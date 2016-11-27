package com.unit.code.generate.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.CtClass;
import javassist.CtField;
import javassist.NotFoundException;
import javassist.bytecode.ConstPool;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.unit.code.generate.constants.SourceCodeConstant;
import com.unit.code.generate.constants.UnitTestConstant;
import com.unit.code.generate.domain.DbInfo;
import com.unit.code.generate.popup.actions.UnitTestGenerateAction;
import com.unit.code.generate.utils.AlertUtil;
import com.unit.code.generate.utils.CtClassUtils;
import com.unit.code.generate.utils.LogUtil;
import com.unit.code.generate.utils.MapUtils;
import com.unit.code.generate.utils.StringUtils;
import com.unit.code.generate.velocity.UnitTestVelocityEngine;

public class DaoUnitTestContextGenerator extends AbstractUnitTestContextGenerator {

	private static final String DB_TX_MANAGER_ANNOTATION = "db_tx_manager_annotation";

	private static final String DB_PREFIX = "db_prefix";

	private static LogUtil logUtil = LogUtil.getInstance();

	public static AbstractUnitTestContextGenerator generator = null;

	private static final String CONFIG_UNIT_TEST_TEMPLATE_VM = "config/unit_test_template_dao.vm";

	private Template unitTestTemplate;

	private VelocityEngine velocityEngine;

	private DaoUnitTestContextGenerator() {
		this.velocityEngine = UnitTestVelocityEngine.getEngine();
		this.unitTestTemplate = this.velocityEngine.getTemplate( CONFIG_UNIT_TEST_TEMPLATE_VM );
	}

	public static AbstractUnitTestContextGenerator getInstance() {
		if( generator == null ) {
			generator = new DaoUnitTestContextGenerator();
		}

		return generator;
	}

	@Override
	public VelocityContext generate( CtClass ctClass ) {
		VelocityContext ctx = new VelocityContext();

		ctx.put( SourceCodeConstant.PACKAGE_NAME, ctClass.getName().replace( "." + ctClass.getSimpleName(), "" ) );

		populateTestedClassContext( ctx, ctClass );

		populateFieldContext( ctx, ctClass );

		populateTestedMethodContext( ctx, ctClass );

		populateSetupMethodContext( ctx );
		return ctx;
	}

	private void populateTestedClassContext( VelocityContext ctx, CtClass ctClass ) {
		populateImportClass( ctx, ctClass.getName() );
		populateTestBase( ctx, ctClass );
		ctx.put( UnitTestConstant.TESTED_CLASS_NAME, ctClass.getSimpleName() );
		ctx.put( UnitTestConstant.TESTED_INSTANCE_FIELD, StringUtils.lowerFirstChar( ctClass.getSimpleName().replaceFirst( "DaoImpl", "Dao" ) ) );
	}

	private void populateTestBase( VelocityContext ctx, CtClass ctClass ) {
		String basePackage = ctClass.getPackageName().replaceFirst( "dao.impl", "" );
		this.populateImportClass( ctx, basePackage + "TestBase" );
	}

	@Override
	public void populateFieldContext( VelocityContext ctx, CtClass ctClass ) {
		CtField[] fields = ctClass.getDeclaredFields();

		List<String> fieldList = new ArrayList<String>();

		for( CtField field : fields ) {
			StringBuilder builder = new StringBuilder();
			try {
				String simpleFieldType = this.getSimpleFieldTypeName( field );
				if( simpleFieldType.endsWith( "Mapper" ) ) {
					String packageName = field.getType().getPackageName();
					String className = field.getType().getName();
					populateDbInfo( ctx, packageName, className );
				} else if ( ctx.get( DB_PREFIX ) == null 
						&& CtClassUtils.isStatic( field )
						&& field.getName().equalsIgnoreCase( "NAMESPACE" ) ) {
					String mapperConstantValue = this.getMapperClassNameFromConstant( ctClass );
					if ( mapperConstantValue != null ) {
						String className = mapperConstantValue.replaceFirst( "\\.$", "" );
						String packageName = className.replaceFirst( "\\.[^\\.]+Mapper$", "" );
						populateDbInfo( ctx, packageName, className );
					}
				} else {
					String fieldName = field.getName();
					String filedDeclaration = getFieldDeclaration( simpleFieldType, fieldName );
					builder.append( filedDeclaration );
					fieldList.add( builder.toString() );
					populateImportClass( ctx, this.getFieldTypeName( field ) );
				}

			} catch( NotFoundException e ) {
				logUtil.logError( "未找到字段" + field.getName() + "的类型", e );
			}

		}
		ctx.put( UnitTestConstant.FIELD_LIST, fieldList );
	}

	private String getMapperClassNameFromConstant( CtClass ctClass ) {
		ConstPool constPool = ctClass.getClassFile().getConstPool();
		int size = constPool.getSize();
		for ( int i=1; i<size; i++ ) {
			String constValue = getUtf8Info( constPool, i );
			if ( constValue != null && constValue.endsWith( "Mapper." )) {
				return constValue;
			}
		}
		return null;
	}
	
	private String getUtf8Info( ConstPool constPool, int i) {
		String constValue = null;
		try {
			constValue = constPool.getUtf8Info( i );
		} catch(Exception e ) {
			return null;
		}
		
		return constValue;
	}

	private void populateDbInfo( VelocityContext ctx, String packageName, String className ) {
		DbInfo dbInfo = UnitTestGenerateAction.mybatisMapperDbInfoMap.get( packageName );
		if( dbInfo == null ) {
			AlertUtil.alert( "提示",
					"未发现当前Dao对应的数据库信息，请检查spring配置文件（xxx-datasource.xml或者xxx-context）中的mybatis配置（basePackage），是否包含了当前package！\n生成单元测试不会含有insert_sql和delete_sql" );
		}
		populateTxManager( ctx, dbInfo );
		populateDbPrefix( ctx, dbInfo );
		if( StringUtils.isEmpty( dbInfo.getUrl() ) || StringUtils.isEmpty( dbInfo.getUserName() ) ) {
			AlertUtil.alert( "提示",
					"数据库连接信息不完整，请检查spring配置文件（xxx-datasource或者xxx-context）中的数据源配置，变量名称是否与test4j.properties中相同" );
		}
		populateInsertSql( ctx, className, dbInfo );
	}

	private void populateTxManager( VelocityContext ctx, DbInfo dbInfo ) {
		if( dbInfo != null && dbInfo.getTxManager() != null && dbInfo.getTxManager().length() > 0 ) {
			ctx.put( DB_TX_MANAGER_ANNOTATION, "@Transactional( value = TransactionMode.ROLLBACK, transactionManagerName =\"" + dbInfo.getTxManager() + "\")" );
		} else {
			AlertUtil.alert( "提示",
					"未发现当前Dao对应的数据源含有事务管理器（DataSourceTransactionManager）信息，可以检查spring配置文件（xxx-datasource.xml或者xxx-context）确认是否必要！" );
			ctx.put( DB_TX_MANAGER_ANNOTATION, "@Transactional( value=TransactionMode.DISABLED )" );
		}
	}

	private void populateInsertSql( VelocityContext ctx, String className, DbInfo dbInfo ) {
		if( dbInfo == null ) {
			return;
		}

		String mapperPath = UnitTestGenerateAction.srcResourcePath + File.separator + className.replace( ".", File.separator ) + ".xml";
		File file = new File( mapperPath );
		if( !file.exists() || !file.isFile() ) {
			return;
		}

		String tableName = getTableNameFromMapper( file );
		if( StringUtils.isEmpty( tableName ) ) {
			return;
		}

		this.generateInsertAndDeleteSql( dbInfo, tableName, ctx );
	}

	private void generateInsertAndDeleteSql( DbInfo dbInfo, String tableName, VelocityContext ctx ) {
		String driver = dbInfo.getDriverClassName();
		String url = dbInfo.getUrl();
		String username = dbInfo.getUserName();
		String password = dbInfo.getPassword();

		StringBuilder insertBuilder = new StringBuilder();
		StringBuilder deleteBuilder = new StringBuilder();
		Connection conn = null;
		ResultSet rs = null;

		try {
			Class.forName( driver );

			DriverManager.setLoginTimeout( 1 );
			conn = DriverManager.getConnection( url, username, password );

			List<String> primaryKeyColumns = getPrimaryKeyColumns( tableName, conn );

			boolean fidIsUniquePrimaryKey = false;
			Statement statement = conn.createStatement();
			String sql = "select * from " + tableName + " limit 1";
			if( primaryKeyColumns.isEmpty() ) {
				AlertUtil.alert( "表" + tableName, "该表可能缺失主键!" );
			} else if( !primaryKeyColumns.get( 0 ).equals( "FID" ) || primaryKeyColumns.size() > 1 ) {
				AlertUtil.alert( "表" + tableName, "该表的主键可能不是FID，或者是其他联合主键，请考虑是否符合规范!" );
			} else {
				fidIsUniquePrimaryKey = true;
				sql = "select * from " + tableName + " order by FID desc limit 1";
			}

			rs = statement.executeQuery( sql );

			ResultSetMetaData metadata = rs.getMetaData();
			if( primaryKeyColumns.isEmpty() ) {
				generateIdentifierColumns( primaryKeyColumns, metadata );
			}

			Map<String, String> column2ValueMap = new HashMap<String, String>();
			for( int i = 1; i <= metadata.getColumnCount(); i++ ) {
				String columnName = metadata.getColumnName( i );
				if( fidIsUniquePrimaryKey && columnName.equals( "FID" ) ) {
					String fid = getFidValue( rs );
					column2ValueMap.put( columnName, fid );
				} else {
					int columnType = metadata.getColumnType( i );
					column2ValueMap.put( columnName, this.getColumnValue( columnType, columnName ) );
				}
			}

			insertBuilder.append( "INSERT INTO " ).append( tableName ).append( "(" );
			for( int i = 1; i <= metadata.getColumnCount(); i++ ) {
				String columnName = metadata.getColumnName( i );
				if( i == 1 ) {
					insertBuilder.append( columnName );
				} else {
					insertBuilder.append( "," ).append( columnName );
				}
			}

			insertBuilder.append( ") VALUES (" );
			for( int i = 1; i <= metadata.getColumnCount(); i++ ) {
				String value = column2ValueMap.get( metadata.getColumnName( i ) );
				if( i == 1 ) {
					insertBuilder.append( value );
				} else {
					insertBuilder.append( "," ).append( value );
				}
			}
			insertBuilder.append( ")" );

			deleteBuilder.append( "DELETE FROM " ).append( tableName ).append( " WHERE " );
			for( int i = 0; i < primaryKeyColumns.size(); i++ ) {
				String columnName = primaryKeyColumns.get( i );
				String columnValue = column2ValueMap.get( columnName );
				if( i == 0 ) {
					deleteBuilder.append( columnName ).append( "=" ).append( columnValue );
				} else {
					deleteBuilder.append( " AND " ).append( columnName ).append( "=" ).append( columnValue );
				}
			}

		} catch( ClassNotFoundException e ) {
			e.printStackTrace();
		} catch( SQLException e ) {
			AlertUtil.alert( "提示", "SQL异常：" + e.getMessage() );
			e.printStackTrace();
		} catch( Exception e ) {
			e.printStackTrace();
		} finally {
			if( rs != null ) {
				try {
					rs.close();
				} catch( SQLException e ) {
					e.printStackTrace();
				}
			}
			if( conn != null ) {
				try {
					conn.close();
				} catch( SQLException e ) {
					e.printStackTrace();
				}
			}
		}

		ctx.put( "insert_sql", insertBuilder.toString() );
		ctx.put( "delete_sql", deleteBuilder.toString() );
	}

	private String getFidValue( ResultSet rs ) throws SQLException {
		Long fid = 1L;
		if( rs.next() ) {
			fid = rs.getLong( "FID" ) + 1;
		}
		return fid.toString();
	}

	private void generateIdentifierColumns( List<String> primaryKeyColumns, ResultSetMetaData metadata ) throws SQLException {
		for( int i = 1; i <= metadata.getColumnCount(); i++ ) {
			primaryKeyColumns.add( metadata.getColumnName( i ) );
		}
	}

	private List<String> getPrimaryKeyColumns( String tableName, Connection conn ) throws SQLException {
		PreparedStatement primaryKeyStatement = conn
				.prepareStatement( "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_NAME = ? AND TABLE_SCHEMA = ? AND COLUMN_KEY = 'PRI'" );
		primaryKeyStatement.setString( 1, tableName );
		primaryKeyStatement.setString( 2, conn.getCatalog() );
		ResultSet primaryKeyResultSet = primaryKeyStatement.executeQuery();
		List<String> primaryKeyColumnNames = new ArrayList<String>();
		while( primaryKeyResultSet.next() ) {
			primaryKeyColumnNames.add( primaryKeyResultSet.getString( 1 ) );
		}

		return primaryKeyColumnNames;
	}

	private String getColumnValue( int columnType, String columnName ) {
		String result = null;
		switch( columnType ) {
		case java.sql.Types.BIT:
			result = "1";
			break;
		case java.sql.Types.TINYINT:
			result = "2";
			break;
		case java.sql.Types.SMALLINT:
			result = "4";
			break;
		case java.sql.Types.INTEGER:
			result = "10";
			break;
		case java.sql.Types.BIGINT:
			result = "100";
			break;
		case java.sql.Types.DECIMAL:
			result = "12.5";
			break;
		case java.sql.Types.CHAR:
			result = "'C'";
			break;
		case java.sql.Types.VARCHAR:
			result = "'" + columnName + "'";
			break;
		case java.sql.Types.DATE:
			result = "'2015-10-10 00:00:00'";
			break;
		case java.sql.Types.TIME:
			result = "'2015-10-10 11:11:11'";
			break;
		case java.sql.Types.TIMESTAMP:
			result = "'2015-10-10 12:30:30'";
			break;
		default:
			result = "''";
		}
		return result;
	}

	private String getTableNameFromMapper( File file ) {
		BufferedReader reader = null;
		InputStream inputStream = null;
		List<String> tableNames = new ArrayList<String>();
		try {
			inputStream = new FileInputStream( file );
			InputStreamReader inputReader = new InputStreamReader( inputStream, "UTF-8" );
			reader = new BufferedReader( inputReader );
			String lineData = null;
			while( ( lineData = reader.readLine() ) != null ) {
				if( lineData.contains( "T_" ) ) {
					tableNames.addAll( getTableFromStr( lineData ) );
				}
			}
		} catch( FileNotFoundException e ) {
			e.printStackTrace();
		} catch( UnsupportedEncodingException e ) {
			e.printStackTrace();
		} catch( IOException e ) {
			e.printStackTrace();
		} finally {
			if( inputStream != null ) {
				try {
					inputStream.close();
				} catch( IOException e ) {
					e.printStackTrace();
				}
			}

			if( reader != null ) {
				try {
					reader.close();
				} catch( IOException e ) {
					e.printStackTrace();
				}
			}
		}

		String result = getMaxOccured( tableNames );
		return result;
	}

	private String getMaxOccured( List<String> strList ) {
		if( strList.size() == 0 ) {
			return null;
		}

		if( strList.size() == 1 ) {
			return strList.get( 0 );
		}

		Map<String, Integer> counterMap = new HashMap<String, Integer>();
		for( String str : strList ) {
			if( counterMap.get( str ) == null ) {
				counterMap.put( str, 1 );
			} else {
				counterMap.put( str, counterMap.get( str ) + 1 );
			}
		}

		List<Map.Entry<String, Integer>> sortedList = MapUtils.sortMapByValue( counterMap );
		String result = sortedList.get( sortedList.size() - 1 ).getKey();
		return result;
	}

	private List<String> getTableFromStr( String s ) {
		if( StringUtils.isEmpty( s ) ) {
			return null;
		}

		String[] pieces = s.split( "\\s" );
		List<String> tableNames = new ArrayList<String>();
		for( String piece : pieces ) {
			if( piece.startsWith( "T_" ) ) {
				tableNames.add( piece );
			}
		}
		return tableNames;
	}

	private void populateDbPrefix( VelocityContext ctx, DbInfo dbInfo ) {
		if( dbInfo != null ) {
			ctx.put( DB_PREFIX, dbInfo.getPrefix() );
		}
	}

	public Template getUnitTestTemplate() {
		return unitTestTemplate;
	}

}
