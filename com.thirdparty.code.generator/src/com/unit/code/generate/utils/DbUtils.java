package com.unit.code.generate.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.unit.code.generate.constants.MysqlType2JavaTypeEnum;
import com.unit.code.generate.domain.ColumnInfo;
import com.unit.code.generate.domain.DbInfo;
import com.unit.code.generate.domain.PropertyInfo;

public class DbUtils {

	public static Map<String, DbInfo> populateTest4jDbInfo( String dbProperties ) {
		Map<String, DbInfo> dbInfoMap = new HashMap<String, DbInfo>();

		File test4jFile = new File( dbProperties );
		if( !test4jFile.exists() || !test4jFile.isFile() ) {
			return Collections.unmodifiableMap( dbInfoMap );
		}

		Properties properties = new Properties();
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream( test4jFile );
			properties.load( inputStream );
			inputStream.close();
		} catch( Exception e ) {
			return Collections.unmodifiableMap( dbInfoMap );
		} finally {
			if( inputStream != null ) {
				try {
					inputStream.close();
				} catch( IOException e ) {
					e.printStackTrace();
				}
			}
		}

		if( properties.size() == 0 ) {
			return Collections.unmodifiableMap( dbInfoMap );
		}

		for( Entry<Object, Object> entry : properties.entrySet() ) {
			Object key = entry.getKey();
			Object value = entry.getValue();
			if( key == null || StringUtils.isEmpty( key.toString() ) || value == null || StringUtils.isEmpty( value.toString() ) ) {
				continue;
			}

			String keyString = key.toString();
			if( ( keyString.toLowerCase().contains( "url" )
					|| keyString.toLowerCase().contains( "username" )
					|| keyString.toLowerCase().contains( "password" ) ) && keyString.toLowerCase().contains( "database" ) ) {
				String valueString = entry.getValue().toString();
				pushDbInfo( keyString, valueString, dbInfoMap );
			}
		}
		return Collections.unmodifiableMap( dbInfoMap );

	}

	private static void pushDbInfo( String keyString, String valueString, Map<String, DbInfo> dbInfoMap ) {
		String dbPrefix = getDbPrefix( keyString );
		if ( dbPrefix.length() == 0 ) {
			dbPrefix ="default";
		}
		DbInfo dbInfo = dbInfoMap.get( dbPrefix );
		if( dbInfo == null ) {
			dbInfoMap.put( dbPrefix, new DbInfo( dbPrefix ) );
			dbInfo = dbInfoMap.get( dbPrefix );
		}

		if( keyString.toLowerCase().endsWith( "url" ) ) {
			dbInfo.setUrl( valueString );
		}

		if( keyString.toLowerCase().endsWith( "username" ) ) {
			dbInfo.setUserName( valueString );
		}
		if( keyString.toLowerCase().endsWith( "password" ) ) {
			dbInfo.setPassword( valueString );
		}
	}
	
	public static String generatePropertyNameWithColumnName( String columnName ) {
		columnName = columnName.toUpperCase().replaceFirst( "(\\w{2,})(ID)$", "$1_$2" );
		String[] pieces = columnName.split( "_" );

		StringBuilder builder = new StringBuilder();
		for( int i = 0; i < pieces.length; i++ ) {
			if( i == 0 ) {
				builder.append( pieces[ i ].toLowerCase() );
			} else {
				builder.append( StringUtils.upperFirstChar( pieces[ i ].toLowerCase() ) );
			}
		}

		return builder.toString();
	}	
	
	public static String getClassNameByTableName( String tableName ) {
		String[] pieces = tableName.split( "_" );
		StringBuilder builder = new StringBuilder();

		if( pieces.length > 2 && pieces[ 0 ].equals( "T" ) ) {
			for( int i = 2; i < pieces.length; i++ ) {
				builder.append( StringUtils.upperFirstChar( pieces[ i ].toLowerCase() ) );
			}
		} else {
			for( int i = 0; i < pieces.length; i++ ) {
				builder.append( StringUtils.upperFirstChar( pieces[ i ].toLowerCase() ) );
			}
		}

		return builder.toString();
	}	
	
	public static String generateAliasForTable( String tableName ) {
		String[] pieces = tableName.split( "_" );
		StringBuilder builder = new StringBuilder();
		
		if( pieces.length > 2 ) {
			for( int i = 2; i < pieces.length; i++ ) {
				builder.append( pieces[ i ].substring( 0, 1 ).toUpperCase() );
			}
		} else {
			builder.append( pieces[ pieces.length - 1 ].substring( 0, 1 ).toUpperCase() );
		}
		
		return builder.toString();
	}	
	
	public static PropertyInfo generatePropertyInfoByColumnInfo( ColumnInfo columnInfo ) {
		PropertyInfo propertyInfo = new PropertyInfo();
		propertyInfo.setPropertyName( DbUtils.generatePropertyNameWithColumnName( columnInfo.getColumnName() ) );
		propertyInfo.setComment( columnInfo.getComment() );
		
		MysqlType2JavaTypeEnum mysqlType2JavaTypeEnum = MysqlType2JavaTypeEnum.getEnumByMysqlType( columnInfo.getColumnType() );
		if( mysqlType2JavaTypeEnum != null ) {
			propertyInfo.setPropertyType( mysqlType2JavaTypeEnum.getJavaType() );
			if( !mysqlType2JavaTypeEnum.getImportClass().isEmpty() ) {
				propertyInfo.setRequiredClass( mysqlType2JavaTypeEnum.getImportClass() );
			}
		}

		return propertyInfo;
	}	
	
	public static String getDbPrefix( String dbExpression ) {
		if( dbExpression.startsWith( "database." ) ) {
			return "default";
		}
		return dbExpression.replaceFirst( "\\.database.*$", " " ).trim();
	}
}
