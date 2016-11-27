package com.unit.code.generate.popup.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javassist.ClassPool;
import javassist.CtClass;

import org.apache.velocity.VelocityContext;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.unit.code.generate.domain.DbInfo;
import com.unit.code.generate.handler.AbstractUnitTestContextGenerator;
import com.unit.code.generate.handler.DaoUnitTestContextGenerator;
import com.unit.code.generate.handler.ServiceUnitTestContextGenerator;
import com.unit.code.generate.utils.AlertUtil;
import com.unit.code.generate.utils.DbUtils;
import com.unit.code.generate.utils.JavaProjectUtils;
import com.unit.code.generate.utils.LogUtil;
import com.unit.code.generate.utils.StringUtils;
import com.unit.code.generate.velocity.VelocityUtils;

public class UnitTestGenerateAction implements IObjectActionDelegate {

	private static LogUtil logUtil = LogUtil.getInstance();

	private static final String DIR_CONFIG = "config";

	private static final String PROJECT_SUFFIX_WEB = "-Web";

	private static final String PROJECT_SUFFIX_SERVICE = "-service";

	private static final String PATH_SRC_JAVA = "src" + File.separator + "main" + File.separator + "java";

	private static final String PATH_TEST_JAVA = "src" + File.separator + "test" + File.separator + "java";

	private static final String FILE_SUFFIX_JAVA = ".java";

	public static String srcJavaPath;

	public static String srcResourcePath;

	private Shell shell;

	private IStructuredSelection selection;

	private String testJavaPath;

	private String projectName;

	private String buildPath = null;

	public static ClassPool classPool = ClassPool.getDefault();

	public static Map<String, DbInfo> mybatisMapperDbInfoMap = new HashMap<String, DbInfo>();

	public static Map<String, DbInfo> test4jDbInfoMap = new HashMap<String, DbInfo>();

	private String testResourcePath;

	/**
	 * Constructor for Action1.
	 */
	public UnitTestGenerateAction() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart( IAction action, IWorkbenchPart targetPart ) {
		shell = targetPart.getSite().getShell();
		AlertUtil.setShell( shell );
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run( IAction action ) {
		if( this.selection.getFirstElement() instanceof CompilationUnit == false ) {
			AlertUtil.alert( "提示", "请选择Service、ServiceImpl或DaoImpl后缀的Java文件" );
			return;
		}
		CompilationUnit unit = ( CompilationUnit )this.selection.getFirstElement();
		String classNameIncludingPackage = this.getClassNameIncludingPackage( unit );
		if( !classNameIncludingPackage.endsWith( "Service" )
				&& !classNameIncludingPackage.toUpperCase().endsWith( "SERVICEIMPL" )
				&& !classNameIncludingPackage.toUpperCase().endsWith( "DAOIMPL" ) ) {
			MessageDialog.openInformation( shell, "Generate", "只支持对Service、ServiceImpl和DaoImpl后缀 的java类生成单元测试" );
			return;
		}

		IJavaProject javaProject = JavaProjectUtils.getCurrentProject( unit );

		this.projectName = javaProject.getElementName();

		List<String> jarFiles = this.getReferencedJarFiles( javaProject ); // 加载maven依赖的jar文件

		this.loadJarFiles( jarFiles );

		List<String> referencedBuildPaths = JavaProjectUtils.getReferencedBuildPaths( javaProject );
		this.preLoadClasses( referencedBuildPaths );

		this.buildPath = JavaProjectUtils.getBuildPath( javaProject );

		this.preLoadClasses( Arrays.asList( buildPath ) );

		this.getSrcPath( javaProject );

		this.populateTest4jDbInfo();

		this.populateMybatisMapperInfo();

		try {
			CtClass ctClass = classPool.get( classNameIncludingPackage );
			String unitTestFilePath = this.generateUnitTestFile( ctClass );
			MessageDialog.openInformation( shell, "Generate", "生成代码成功");
			javaProject.getProject().getFolder( "src/test" ).refreshLocal( IResource.DEPTH_INFINITE, null );
			JavaProjectUtils.openProjectFile( javaProject.getProject(), unitTestFilePath );
		} catch( Exception e ) {
			logUtil.logError( e.getMessage(), e );
			MessageDialog.openInformation( shell, "Generate", "生成代码失败:" + e.getStackTrace() );
		}

	}

	private void populateTest4jDbInfo() {
		String test4jProperties = this.testResourcePath + File.separator + "test4j.properties";
		test4jDbInfoMap = DbUtils.populateTest4jDbInfo( test4jProperties );
	}

	@SuppressWarnings( "unchecked" )
	private void populateMybatisMapperInfo() {
		String springContextDir = srcResourcePath + File.separator + DIR_CONFIG;
		File[] files = getSpringContextXmlFiles( springContextDir );
		if( ( files == null || files.length == 0 ) && this.projectName.endsWith( PROJECT_SUFFIX_SERVICE ) ) {
			String webProjectName = this.projectName.replaceFirst( PROJECT_SUFFIX_SERVICE + "$", PROJECT_SUFFIX_WEB );
			springContextDir = springContextDir.replace( File.separator + this.projectName + File.separator, File.separator + webProjectName + File.separator );
			files = getSpringContextXmlFiles( springContextDir );
		}

		if( files == null || files.length == 0 ) {
			return;
		}

		File springContextFile = files[ 0 ];

		SAXReader reader = new SAXReader();
		try {
			Document document = reader.read( springContextFile.getAbsolutePath() );
			List<Element> beanElements = document.getRootElement().elements( "bean" );
			Map<String, Element> elementMap = new HashMap<String, Element>();
			List<String> mybatisList = new ArrayList<String>();

			for( Element beanElement : beanElements ) {
				elementMap.put( beanElement.attributeValue( "id" ), beanElement );
				if( beanElement.attribute( "class" ).getText().equals( "org.mybatis.spring.mapper.MapperScannerConfigurer" ) ) {
					mybatisList.add( beanElement.attributeValue( "id" ) );
				}
			}

			if( mybatisList.size() == 0 ) {
				return;
			}

			mybatisMapperDbInfoMap = new HashMap<String, DbInfo>();

			for( String mapper : mybatisList ) {
				Element element = elementMap.get( mapper );
				Element basePackageElement = this.findChildElement( "name", "basePackage", element );
				if( basePackageElement == null ) {
					continue;
				}

				Element sqlSessionFactoryElement = this.findChildElement( "name", "sqlSessionFactoryBeanName", element );
				if( sqlSessionFactoryElement == null ) {
					continue;
				}

				this.getPackageDbInfo( basePackageElement.attributeValue( "value" ), sqlSessionFactoryElement.attributeValue( "value" ), elementMap );
			}
		} catch( DocumentException e ) {
			e.printStackTrace();
		}

		Collections.unmodifiableMap( mybatisMapperDbInfoMap );
	}

	private void getPackageDbInfo( String basePackage, String sqlSessionFactoryBeanName, Map<String, Element> elementMap ) {
		Element sqlSessionFactoryElement = elementMap.get( sqlSessionFactoryBeanName );
		if( sqlSessionFactoryElement == null ) {
			return;
		}

		Element dataSourcePropertyElement = this.findChildElement( "name", "dataSource", sqlSessionFactoryElement );
		if( dataSourcePropertyElement == null ) {
			return;
		}

		String dataSourceName = dataSourcePropertyElement.attributeValue( "ref" );
		Element dataSourceBean = elementMap.get( dataSourceName );
		if( dataSourceBean == null ) {
			return;
		}

		Element usernameElement = this.findChildElement( "name", "username", dataSourceBean );

		if( usernameElement == null ) {
			return;
		}

		String dbInfo = usernameElement.attributeValue( "value" );
		dbInfo = dbInfo.replace( "$", "" ).replace( "{", "" ).replace( "}", "" );
		if( StringUtils.isEmpty( dbInfo ) ) {
			return;
		}

		String dbPrefix = DbUtils.getDbPrefix( dbInfo );
		DbInfo db = test4jDbInfoMap.get( dbPrefix );
		if( db == null ) {
			db = new DbInfo( dbPrefix );
		}

		populateTxManager( dataSourceName, elementMap, db );
		String[] basePackages = basePackage.split( "," );
		for( String basePack : basePackages ) {
			mybatisMapperDbInfoMap.put( basePack.trim(), db );
		}
	}

	private void populateTxManager( String dataSourceName, Map<String, Element> elementMap, DbInfo dbInfo ) {
		for( Entry<String, Element> entry : elementMap.entrySet() ) {
			Element element = entry.getValue();
			if( !element.attributeValue( "class" ).equals( "org.springframework.jdbc.datasource.DataSourceTransactionManager" ) ) {
				continue;
			}
			if( this.findChildElement( "ref", dataSourceName, element ) != null ) {
				dbInfo.setTxManager( entry.getKey() );
			}
		}

	}

	@SuppressWarnings( "unchecked" )
	private Element findChildElement( String attributeName, String attributeValue, Element element ) {
		List<Element> childElements = element.elements();
		for( Element child : childElements ) {
			if( child.attribute( attributeName ) != null && child.attributeValue( attributeName ).equals( attributeValue ) ) {
				return child;
			}
		}
		return null;
	}

	private File[] getSpringContextXmlFiles( String springContextDir ) {
		File dir = new File( springContextDir );
		if( !dir.exists() || !dir.isDirectory() ) {
			return new File[ 0 ];
		}

		File[] files = dir.listFiles( new FilenameFilter() {

			@Override
			public boolean accept( File dir, String name ) {
				return name.endsWith( "datasource.xml" );
			}
		} );

		if( files == null || files.length == 0 ) {
			files = dir.listFiles( new FilenameFilter() {

				@Override
				public boolean accept( File dir, String name ) {
					return name.endsWith( "context.xml" );
				}
			} );

		}
		return files;
	}

	private String generateUnitTestFile( CtClass ctClass ) {
		if( ctClass == null ) {
			MessageDialog.openInformation( shell, "Generate", "class文件为空，生成失败" );
			return "";
		}

		AbstractUnitTestContextGenerator generator = getContextGenerator( ctClass );
		if( generator == null ) {
			MessageDialog.openInformation( shell, "Generate", "只支持对Service和DaoImpl后缀 的java类生成单元测试" );
			return "";
		}
		VelocityContext ctx = generator.generate( ctClass );

		String unitTestFilePath = this.getUnitTestFilePath( ctClass );
		populateClassName( ctx, unitTestFilePath );

		VelocityUtils.merge( generator.getUnitTestTemplate(), ctx, unitTestFilePath );
		return unitTestFilePath;
	}

	private void populateClassName( VelocityContext ctx, String unitTestFilePath ) {
		File file = new File( unitTestFilePath );
		String className = file.getName().replace( ".java", "" );
		ctx.put( "class_name", className );
	}

	private AbstractUnitTestContextGenerator getContextGenerator( CtClass ctClass ) {
		String className = ctClass.getName().toUpperCase();
		AbstractUnitTestContextGenerator generator = null;
		if( className.toUpperCase().endsWith( "DAOIMPL" ) ) {
			generator = DaoUnitTestContextGenerator.getInstance();
		} else {
			generator = ServiceUnitTestContextGenerator.getInstance();
		}

		return generator;
	}

	public void preLoadClasses( List<String> paths ) {
		for( String buildPath : paths ) {
			File directory = new File( buildPath );
			this.loadAllClassesFromBuildPath( directory );
		}
	}

	private void loadAllClassesFromBuildPath( File directory ) {
		if( directory.exists() && directory.isDirectory() ) {
			File[] subFiles = directory.listFiles();
			for( File subFile : subFiles ) {
				if( subFile.isDirectory() ) {
					this.loadAllClassesFromBuildPath( subFile );
				} else if( subFile.getName().endsWith( ".class" ) ) {
					this.loadClass( subFile );
				}
			}
		}
	}

	private void loadClass( File file ) {
		InputStream stream = null;
		try {
			stream = new FileInputStream( file );
			classPool.makeClass( stream );
		} catch( FileNotFoundException e ) {
			logUtil.logError( "未找到文件" + file.getAbsolutePath(), e );
		} catch( IOException e ) {
			logUtil.logError( "加载文件IO错误：" + file.getAbsolutePath(), e );
		} catch( Exception e ) {
			logUtil.logError( "加载文件运行时异常错误：" + file.getAbsolutePath(), e );
		} finally {
			if( stream != null ) {
				try {
					stream.close();
				} catch( IOException e ) {
					logUtil.logError( "关闭文件流错误", e );
				}
			}
		}

	}

	private String getClassNameIncludingPackage( CompilationUnit unit ) {
		String simpleClassName = unit.getElementName().replace( FILE_SUFFIX_JAVA, "" );

		String packageName = ( ( PackageFragment )unit.getParent() ).getElementName();
		return packageName + "." + simpleClassName;
	}

	private String getSrcPath( IJavaProject javaProject ) {
		try {
			IPackageFragmentRoot[] roots = javaProject.getAllPackageFragmentRoots();
			for( IPackageFragmentRoot root : roots ) {
				if( !( root instanceof JarPackageFragmentRoot ) ) {
					String path = root.getPath().toOSString();
					String workspace = javaProject.getProject().getLocation().toOSString();
					if( path.contains( PATH_TEST_JAVA ) && path.contains( javaProject.getPath().toOSString() ) ) {
						this.testJavaPath = workspace + path.replace( "\\" + javaProject.getElementName(), "" );
						this.testResourcePath = new File( this.testJavaPath ).getParent() + File.separator + "resources";
					}
					if( path.contains( PATH_SRC_JAVA ) && path.contains( javaProject.getPath().toOSString() ) ) {
						srcJavaPath = workspace + path.replace( "\\" + javaProject.getElementName(), "" );
						srcResourcePath = new File( srcJavaPath ).getParent() + File.separator + "resources";
					}
				}
			}
		} catch( JavaModelException e ) {
			logUtil.logError( "获取Java源文件路径错误，", e );
		}
		return null;
	}

	private String getUnitTestFilePath( CtClass ctClass ) {
		String packagePath = ctClass.getName().replace( ".", File.separator );
		String filePath = this.testJavaPath + File.separator + packagePath + "Test.java";
		File file = new File( filePath );
		if( file.exists() ) {
			filePath = this.testJavaPath + File.separator + packagePath + "Test1.java";
		}
		return filePath;
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged( IAction action, ISelection selection ) {
		this.selection = ( IStructuredSelection )selection;
	}

	@SuppressWarnings( "static-access" )
	private void loadJarFiles( List<String> jarFiles ) {
		try {
			// 包路径定义
			URLClassLoader urlLoader = ( URLClassLoader )this.getClass().getClassLoader().getSystemClassLoader();
			Class<URLClassLoader> sysclass = URLClassLoader.class;
			Method method = sysclass.getDeclaredMethod( "addURL", new Class[] { URL.class } );
			method.setAccessible( true );
			for( String jarFile : jarFiles ) {
				URL urls = new URL( jarFile );
				method.invoke( urlLoader, urls );
			}
		} catch( Exception exp ) {
			exp.printStackTrace();
		}
	}

	private List<String> getReferencedJarFiles( IJavaProject javaProject ) {
		List<String> referencedJarFiles = new ArrayList<String>();
		try {
			IClasspathEntry[] entries = javaProject.getResolvedClasspath( true );
			for( IClasspathEntry entry : entries ) {
				if( entry.getPath().toOSString().contains( ".m2" ) && entry.getPath().toOSString().contains( "repository" ) ) {
					referencedJarFiles.add( "file:/" + entry.getPath().toOSString() );
				}
			}
		} catch( JavaModelException e ) {
			e.printStackTrace();
		}
		return referencedJarFiles;
	}

}
