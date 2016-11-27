package com.unit.code.generate.wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.velocity.VelocityContext;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.unit.code.generate.constants.FormatConstant;
import com.unit.code.generate.constants.SourceCodeConstant;
import com.unit.code.generate.domain.ColumnInfo;
import com.unit.code.generate.domain.PropertyInfo;
import com.unit.code.generate.popup.actions.JavaBeanGenerateAction;
import com.unit.code.generate.utils.AlertUtil;
import com.unit.code.generate.utils.DbUtils;
import com.unit.code.generate.utils.JavaProjectUtils;
import com.unit.code.generate.utils.StringUtils;
import com.unit.code.generate.velocity.VelocityUtils;

public class JavaBeanGenerateWizard extends AbstractBusinessGenerateWizard {

	private static final String CONFIG_JAVA_BEAN_TEMPLATE_VM = "config/java_bean_template.vm";

	private String className;

	private String filePath;

	public JavaBeanGenerateWizard( IStructuredSelection selection ) {
		super( selection );
	}

	protected void initialize() {
		super.initialize();
		this.velocityTemplate = this.velocityEngine.getTemplate( CONFIG_JAVA_BEAN_TEMPLATE_VM );
	}

	@Override
	public void addPages() {
		super.addPages();
		page.setTitle( "生成Java Bean代码" );
		page.setDescription( "该向导生成某个表的Java Bean" );
	}

	String generateFilePath() {
		return this.packagePath + File.separator + this.className + ".java";
	}

	VelocityContext generateVelocityContext() {
		VelocityContext ctx = new VelocityContext();
		this.className = DbUtils.getClassNameByTableName( this.tableName );
		ctx.put( SourceCodeConstant.PACKAGE_NAME, this.currentPackageName );
		ctx.put( SourceCodeConstant.CLASS_NAME, this.className );

		List<String> fields = new ArrayList<String>();
		List<String> methods = new ArrayList<String>();
		Set<String> importClasses = new HashSet<String>();

		for( ColumnInfo columnInfo : this.columnInfos ) {
			PropertyInfo propertyInfo = DbUtils.generatePropertyInfoByColumnInfo( columnInfo );
			fields.add( generateFileDeclarationByPropertyInfo( propertyInfo ) );
			methods.add( generateGetterMethodByPropertyInfo( propertyInfo ) );
			methods.add( generateSetterMethodByPropertyInfo( propertyInfo ) );
			if( propertyInfo.getRequiredClass() != null ) {
				importClasses.add( propertyInfo.getRequiredClass() + ";" );
			}
		}
		ctx.put( SourceCodeConstant.DECLARE_FIELDS, fields );
		ctx.put( SourceCodeConstant.DECLARE_METHODS, methods );
		ctx.put( SourceCodeConstant.IMPORT_CLASSES, importClasses );

		JavaBeanGenerateAction.setJavaBean( this.className, this.currentPackageName + "." + this.className );
		return ctx;
	}

	@Override
	public boolean generateFiles() throws Exception {
		VelocityContext velocityContext = this.generateVelocityContext();
		this.filePath = this.generateFilePath();

		VelocityUtils.merge( velocityTemplate, velocityContext, filePath );

		this.currentProject.getProject().getFolder( "src/main" ).refreshLocal( IResource.DEPTH_INFINITE, null );
		AlertUtil.alert( "提示", "Java Bean已生成" );
		return true;
	}
	
	@Override
	public void openFiles() throws Exception {
		JavaProjectUtils.openProjectFile( this.currentProject.getProject(), this.filePath );
	}	

	private String generateSetterMethodByPropertyInfo( PropertyInfo propertyInfo ) {
		StringBuilder builder = new StringBuilder( "\n" );
		builder.append( FormatConstant.TAB ).append( "public void " )
				.append( generateSetterMethdName( propertyInfo.getPropertyName() ) ).append( "( " ).append( propertyInfo.getPropertyType() )
				.append( FormatConstant.SPACE ).append( propertyInfo.getPropertyName() ).append( " ) {" ).append( "\n" )
				.append( FormatConstant.DOUBLE_TAB ).append( "this." ).append( propertyInfo.getPropertyName() ).append( " = " )
				.append( propertyInfo.getPropertyName() ).append( ";\n" )
				.append( FormatConstant.TAB ).append( "}" );
		return builder.toString();
	}

	private Object generateSetterMethdName( String propertyName ) {
		return "set" + StringUtils.upperFirstChar( propertyName );
	}

	private String generateGetterMethodByPropertyInfo( PropertyInfo propertyInfo ) {
		StringBuilder builder = new StringBuilder( "\n" );
		builder.append( FormatConstant.TAB ).append( "public " ).append( propertyInfo.getPropertyType() ).append( FormatConstant.SPACE )
				.append( generateGetterMethdName( propertyInfo.getPropertyName() ) ).append( "( ) {" ).append( "\n" )
				.append( FormatConstant.DOUBLE_TAB ).append( "return " ).append( propertyInfo.getPropertyName() ).append( ";\n" )
				.append( FormatConstant.TAB ).append( "}" );
		return builder.toString();
	}

	private String generateGetterMethdName( String propertyName ) {
		return "get" + StringUtils.upperFirstChar( propertyName );
	}

	private String generateFileDeclarationByPropertyInfo( PropertyInfo propertyInfo ) {
		StringBuilder builder = new StringBuilder( "\n" );
		builder.append( FormatConstant.TAB ).append( "/**" ).append( FormatConstant.SPACE ).append( "\n" ).
				append( FormatConstant.TAB ).append( " * " ).append( propertyInfo.getComment() ).append( "\n" ).
				append( FormatConstant.TAB ).append( " */" ).append( "\n" ).
				append( FormatConstant.TAB ).append( "private" ).append( FormatConstant.SPACE ).
				append( propertyInfo.getPropertyType() ).append( FormatConstant.SPACE ).
				append( propertyInfo.getPropertyName() ).append( FormatConstant.SEMICOLON );
		return builder.toString();
	}

}
