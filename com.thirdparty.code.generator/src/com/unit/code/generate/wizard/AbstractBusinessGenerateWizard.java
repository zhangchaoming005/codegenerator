package com.unit.code.generate.wizard;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

import com.unit.code.generate.domain.ColumnInfo;
import com.unit.code.generate.domain.DbInfo;
import com.unit.code.generate.utils.AlertUtil;
import com.unit.code.generate.utils.DbUtils;
import com.unit.code.generate.utils.JavaProjectUtils;
import com.unit.code.generate.velocity.UnitTestVelocityEngine;

/**
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "mpe". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will
 * be able to open it.
 */

public abstract class AbstractBusinessGenerateWizard extends Wizard implements INewWizard {

	protected BusinessGenerateWizardPage page;

	private static final String PATH_SRC_JAVA = File.separator + "src" + File.separator + "main" + File.separator + "java";
	
	private static final String PATH_SRC_RES = File.separator + "src" + File.separator + "main" + File.separator + "resources";

	private static final String PATH_TEST_RES = File.separator + "src" + File.separator + "test" + File.separator + "resources";

	protected IStructuredSelection selection;

	protected IJavaProject currentProject;

	private String projectLocation;

	private String srcJavaLocation;
	
	private String srcResourcesLocation;

	private String testResourceLocation;

	private Map<String, DbInfo> dbInfoMap = new HashMap<String, DbInfo>();

	private Object selectedElement;

	protected VelocityEngine velocityEngine;

	protected Template velocityTemplate;

	protected String dbKey;

	protected String tableName;

	protected String currentPackageName;

	protected String packagePath;

	protected List<ColumnInfo> columnInfos = new ArrayList<ColumnInfo>();
	
	/**
	 * Constructor for SampleNewWizard.
	 */
	public AbstractBusinessGenerateWizard() {
		super();
		setNeedsProgressMonitor( true );
	}

	public AbstractBusinessGenerateWizard( IStructuredSelection selection ) {
		super();
		this.selection = selection;
		setNeedsProgressMonitor( true );

		this.selectedElement = this.selection.getFirstElement();
		if( ( this.selectedElement instanceof CompilationUnit == false )
				&& ( this.selectedElement instanceof PackageFragment == false ) ) {
			AlertUtil.alert( "提示", "所选择的对象不是包或者对象，无法确定生成文件要存放的路径" );
			return;
		}

		initialize();
		if( this.dbInfoMap.isEmpty() ) {
			AlertUtil.alert( "提示", "test4j.properties配置文件不存在，或者配置不正确，请确认！" );
			return;
		}
	}

	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new BusinessGenerateWizardPage( );
		page.setDbKeys( this.dbInfoMap.keySet() );
		addPage( page );
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		this.dbKey = this.page.getSelectedDbKey();
		this.tableName = this.page.getTableName();
		this.columnInfos = this.getColumnInfos( dbKey, tableName );
		
		if( columnInfos.isEmpty() ) {
			AlertUtil.alert( "提示", "选择的数据库或者表不存在，无法生成Java类（表名的大小写必须匹配）" );
			return false;
		}

		boolean generateSuccess = false;
		try {
			generateSuccess = generateFiles();
		} catch( Exception e ) {
			e.printStackTrace();
			AlertUtil.alert( "提示", String.format( "文件生成失败! %s", e.getMessage() ) );
			return false;
		}
		
		if ( !generateSuccess ) {
			return false;
		}
		
		try {
			openFiles();
		} catch( Exception e ) {
			e.printStackTrace();
			AlertUtil.alert( "提示", String.format( "打开文件失败! %s", e.getMessage() ) );
			return false;
		}

		return true;
	}

	abstract boolean generateFiles() throws Exception;
	
	abstract void openFiles() throws Exception;

	protected List<ColumnInfo> getColumnInfos( String dbKey, String tableName ) {
		DbInfo dbInfo = this.dbInfoMap.get( dbKey );
		if( dbInfo == null ) {
			return new ArrayList<ColumnInfo>();
		}

		List<ColumnInfo> result = new ArrayList<ColumnInfo>();
		String driver = dbInfo.getDriverClassName();
		String url = dbInfo.getUrl();
		String username = dbInfo.getUserName();
		String password = dbInfo.getPassword();

		Connection conn = null;
		ResultSet rs = null;

		try {
			Class.forName( driver );
			DriverManager.setLoginTimeout( 1 );
			conn = DriverManager.getConnection( url, username, password );

			PreparedStatement queryStatement = conn
					.prepareStatement( "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT, COLUMN_KEY, EXTRA FROM information_schema.COLUMNS WHERE TABLE_NAME = ? AND TABLE_SCHEMA = ? ORDER BY ORDINAL_POSITION" );
			queryStatement.setString( 1, tableName );
			queryStatement.setString( 2, conn.getCatalog() );
			rs = queryStatement.executeQuery();
			while( rs.next() ) {
				ColumnInfo columnInfo = new ColumnInfo();
				columnInfo.setColumnName( rs.getString( "COLUMN_NAME" ) );
				columnInfo.setColumnType( rs.getString( "DATA_TYPE" ) );
				columnInfo.setComment( rs.getString( "COLUMN_COMMENT" ) );
				columnInfo.setPrimary( ( rs.getString( "COLUMN_KEY" ) != null && rs.getString( "COLUMN_KEY" ).equals( "PRI" ) ) );
				columnInfo.setAutoIncrement( ( rs.getString( "EXTRA" ) != null && rs.getString( "EXTRA" ).contains( "auto_increment" ) ) );
				result.add( columnInfo );
			}
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
		return result;
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init( IWorkbench workbench, IStructuredSelection selection ) {}

	protected void initialize() {

		this.currentProject = JavaProjectUtils.getCurrentProject( ( IJavaElement )this.selectedElement );

		this.velocityEngine = UnitTestVelocityEngine.getEngine();

		setupBasicPath();

		setupDbInfo();

	}

	private void setupDbInfo() {
		String test4jPropertiesPath = this.testResourceLocation + File.separator + "test4j.properties";
		this.dbInfoMap = DbUtils.populateTest4jDbInfo( test4jPropertiesPath );
	}

	private void setupBasicPath() {
		this.projectLocation = this.currentProject.getProject().getLocation().toOSString();
		this.srcJavaLocation = this.projectLocation + PATH_SRC_JAVA;
		this.srcResourcesLocation = this.projectLocation + PATH_SRC_RES;
		this.testResourceLocation = this.projectLocation + PATH_TEST_RES;

		if( selectedElement instanceof CompilationUnit ) {
			CompilationUnit unit = ( CompilationUnit )selectedElement;
			this.currentPackageName = ( ( PackageFragment )unit.getParent() ).getElementName();
		} else {
			this.currentPackageName = ( ( PackageFragment )selectedElement ).getElementName();
		}

		this.packagePath = this.getSrcJavaPath( this.currentPackageName );
	}

	protected String getSrcJavaPath( String packageName ) {
		if( packageName == null ) {
			return null;
		}

		return this.srcJavaLocation + File.separator + packageName.replace( ".", File.separator );
	}
	
	protected String getSrcResPath( String packageName ) {
		if( packageName == null ) {
			return null;
		}
		
		return this.srcResourcesLocation + File.separator + packageName.replace( ".", File.separator );
	}
}