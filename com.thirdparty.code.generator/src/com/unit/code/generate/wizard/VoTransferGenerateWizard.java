package com.unit.code.generate.wizard;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.unit.code.generate.constants.FormatConstant;
import com.unit.code.generate.constants.SourceCodeConstant;
import com.unit.code.generate.domain.ColumnInfo;
import com.unit.code.generate.domain.PropertyInfo;
import com.unit.code.generate.popup.actions.VoTransferGenerateAction;
import com.unit.code.generate.utils.AlertUtil;
import com.unit.code.generate.utils.DbUtils;
import com.unit.code.generate.utils.JavaProjectUtils;
import com.unit.code.generate.utils.StringUtils;
import com.unit.code.generate.velocity.VelocityUtils;

public class VoTransferGenerateWizard extends AbstractBusinessGenerateWizard {

	private static final String CONFIG_VO_TEMPLATE_VM = "config/java_bean_template.vm";
	
	private static final String CONFIG_TRANSFER_TEMPLATE_VM = "config/transfer_template.vm";

	private String voClassName;
	
	private String boClassName;
	
	private String transferClassName;

	private String voFilePath;
	
	private String transferFilePath;
	
	private Template voTemplate;
	
	private Template transferTemplate;

	public VoTransferGenerateWizard( IStructuredSelection selection ) {
		super( selection );
	}

	protected void initialize() {
		super.initialize();
		this.voTemplate = this.velocityEngine.getTemplate( CONFIG_VO_TEMPLATE_VM );
		this.transferTemplate = this.velocityEngine.getTemplate( CONFIG_TRANSFER_TEMPLATE_VM );
	}

	@Override
	public void addPages() {
		super.addPages();
		page.setTitle( "生成Java Bean代码" );
		page.setDescription( "该向导生成某个表的Java Bean" );
	}

	String generateVoFilePath() {
		return this.packagePath + File.separator + this.voClassName + ".java";
	}
	
	String generateTransferFilePath() {
		return this.packagePath + File.separator + this.transferClassName + ".java";
	}

	VelocityContext generateVoVelocityContext() {
		VelocityContext ctx = new VelocityContext();
		this.voClassName = DbUtils.getClassNameByTableName( this.tableName ) + "VO";
		ctx.put( SourceCodeConstant.PACKAGE_NAME, this.currentPackageName );
		ctx.put( SourceCodeConstant.CLASS_NAME, this.voClassName );

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

		VoTransferGenerateAction.setJavaBean( this.voClassName, this.currentPackageName + "." + this.voClassName );
		return ctx;
	}

	VelocityContext generateTransferVelocityContext() {
		VelocityContext ctx = new VelocityContext();
		this.transferClassName = DbUtils.getClassNameByTableName( this.tableName ) + "Transfer";
		this.boClassName = DbUtils.getClassNameByTableName( this.tableName );
		ctx.put( SourceCodeConstant.PACKAGE_NAME, this.currentPackageName );
		ctx.put( SourceCodeConstant.CLASS_NAME, this.transferClassName );

		List<String> methods = new ArrayList<String>();
		Set<String> importClasses = new HashSet<String>();
		importClasses.add( "import java.util.ArrayList;" );
		importClasses.add( "import java.util.List;" );

		methods.add( generateTransferBo2VoMethod( ) );
		methods.add( generateTransferVo2BoMethod( ) );
		methods.add( generateTransferBo2VoListMethod( ) );
		methods.add( generateTransferVo2BoListMethod( ) );

		ctx.put( SourceCodeConstant.DECLARE_METHODS, methods );
		ctx.put( SourceCodeConstant.IMPORT_CLASSES, importClasses );

		VoTransferGenerateAction.setJavaBean( this.transferClassName, this.currentPackageName + "." + this.transferClassName );
		return ctx;
	}
	
	@Override
	public boolean generateFiles() throws Exception {
		generateVoFile();
		generateTransferFile();

		this.currentProject.getProject().getFolder( "src/main" ).refreshLocal( IResource.DEPTH_INFINITE, null );
		AlertUtil.alert( "提示", "VO、Transfer已生成" );
		return true;
	}

	private void generateVoFile() {
		VelocityContext velocityContext = this.generateVoVelocityContext();
		this.voFilePath = this.generateVoFilePath();

		VelocityUtils.merge( voTemplate, velocityContext, voFilePath );
	}
	
	private void generateTransferFile() {
		VelocityContext velocityContext = this.generateTransferVelocityContext();
		this.transferFilePath = this.generateTransferFilePath();
		
		VelocityUtils.merge( transferTemplate, velocityContext, transferFilePath );
	}
	
	@Override
	public void openFiles() throws Exception {
		JavaProjectUtils.openProjectFile( this.currentProject.getProject(), this.voFilePath );
		JavaProjectUtils.openProjectFile( this.currentProject.getProject(), this.transferFilePath );
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

	private String generateTransferBo2VoMethod( ) {
		String fromClass = this.boClassName;
		String toClass = this.voClassName;
		String fromInstance = "bo";
		String toInstance = "vo";
		String methodName = "transferBO2VO";
		
		return generateTransferMethod( fromClass, toClass, fromInstance, toInstance, methodName );
	}
	
	private String generateTransferBo2VoListMethod( ) {
		String fromClass = this.boClassName;
		String toClass = this.voClassName;
		String fromInstance = "bos";
		String methodName = "transferBO2VO";
		
		return generateTransferListMethod( fromClass, toClass, fromInstance, methodName );
	}
	
	private String generateTransferVo2BoMethod( ) {
		String fromClass = this.voClassName;
		String toClass = this.boClassName;
		String fromInstance = "vo";
		String toInstance = "bo";
		String methodName = "transferVO2BO";
		
		return generateTransferMethod( fromClass, toClass, fromInstance, toInstance, methodName );
	}

	private String generateTransferVo2BoListMethod( ) {
		String fromClass = this.voClassName;
		String toClass = this.boClassName;
		String fromInstance = "vos";
		String methodName = "transferVO2BO";
		
		return generateTransferListMethod( fromClass, toClass, fromInstance, methodName );
	}
	
	private String generateTransferListMethod( String fromClass, String toClass, String fromInstance, String methodName ) {
		String fromListClass = "List<" + fromClass + ">" ;
		String toListClass = "List<" + toClass + ">" ;
		String toArrayListClass = "ArrayList<" + toClass + ">";
		String toInstance = "result";
		
		StringBuilder builder = new StringBuilder( "\n" );
		builder.append( FormatConstant.TAB ).append( "public static ").append( toListClass )
		    .append( FormatConstant.SPACE ).append( methodName ).append( "( " ).append( fromListClass )
		    .append( FormatConstant.SPACE ).append( fromInstance ).append( FormatConstant.SPACE ).append( ") {" ).append( "\n" );
		
		builder.append( FormatConstant.DOUBLE_TAB ).append("if ( ").append( fromInstance ).append(" == null || ").append( fromInstance ).append( ".isEmpty() ) {").append( "\n" );
		builder.append( FormatConstant.THREE_TAB ).append( "return new ").append( toArrayListClass ).append("();" ).append( "\n" );
		builder.append( FormatConstant.DOUBLE_TAB ).append( "}" ).append( "\n" );
		builder.append( "\n" );
		
		builder.append( FormatConstant.DOUBLE_TAB ).append( toListClass ).append(FormatConstant.SPACE).append( toInstance )
		    .append( " = " ).append( "new " ).append( toArrayListClass ).append( "();" ).append( "\n" );
		
		builder.append( FormatConstant.DOUBLE_TAB ).append( "for (" ).append( fromClass ).append( FormatConstant.SPACE ).append( fromInstance.substring( 0, 2 ) )
		    .append( " : " ).append( fromInstance ).append( " ) " ).append( " {" ).append( "\n" );
		builder.append( FormatConstant.THREE_TAB ).append( toInstance ).append( ".add( " )
		    .append( methodName ).append( "( " ).append( fromInstance.substring( 0, 2 ) ).append(" ) ); ").append( "\n" );
		builder.append( FormatConstant.DOUBLE_TAB ).append( "}" ).append("\n");
		
		builder.append( FormatConstant.DOUBLE_TAB ).append( "return " ).append( toInstance ).append( " ;" ).append( "\n" );
		builder.append( FormatConstant.TAB ).append( "}" );
		return builder.toString();
	}
	
	private String generateTransferMethod( String fromClass, String toClass, String fromInstance, String toInstance, String methodName ) {
		StringBuilder builder = new StringBuilder( "\n" );
		builder.append( FormatConstant.TAB ).append( "public static ").append( toClass ).append( FormatConstant.SPACE )
		.append( methodName ).append("( " ).append( fromClass )
		.append( FormatConstant.SPACE ).append( fromInstance ).append( " ) {" ).append( "\n" );
		
		builder.append( FormatConstant.DOUBLE_TAB ).append( "if ( " ).append( fromInstance ).append( " == null ) {" ).append( "\n" );
		builder.append( FormatConstant.THREE_TAB ).append( "return null;" ).append( "\n" );
		builder.append( FormatConstant.DOUBLE_TAB ).append( "}\n\n" );
		
		builder.append( FormatConstant.DOUBLE_TAB ).append( toClass ).append( FormatConstant.SPACE ).append( toInstance ).append( " = new " ).append( toClass ).append( "();" ).append( "\n" );

		for( ColumnInfo columnInfo : this.columnInfos ) {
			PropertyInfo propertyInfo = DbUtils.generatePropertyInfoByColumnInfo( columnInfo );
			builder.append( FormatConstant.DOUBLE_TAB ).append( toInstance ).append( "." ).append( generateSetterMethdName( propertyInfo.getPropertyName() ) )
				.append( "(").append( FormatConstant.SPACE ).append( fromInstance ).append( "." ).append( generateGetterMethodName( propertyInfo.getPropertyName()) ).append( "()" )
				.append( " );" ).append( "\n" );
		}
		
		builder.append( FormatConstant.DOUBLE_TAB ).append( "return ").append( toInstance ).append(";" ).append( "\n" );
		
		builder.append( FormatConstant.TAB ).append( "}" );
		return builder.toString();
	}
	
	private String generateGetterMethodByPropertyInfo( PropertyInfo propertyInfo ) {
		StringBuilder builder = new StringBuilder( "\n" );
		builder.append( FormatConstant.TAB ).append( "public " ).append( propertyInfo.getPropertyType() ).append( FormatConstant.SPACE )
		.append( generateGetterMethodName( propertyInfo.getPropertyName() ) ).append( "( ) {" ).append( "\n" )
		.append( FormatConstant.DOUBLE_TAB ).append( "return " ).append( propertyInfo.getPropertyName() ).append( ";\n" )
		.append( FormatConstant.TAB ).append( "}" );
		return builder.toString();
	}

	private String generateGetterMethodName( String propertyName ) {
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
