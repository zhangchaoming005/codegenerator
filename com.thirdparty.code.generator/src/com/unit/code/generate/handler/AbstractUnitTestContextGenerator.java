package com.unit.code.generate.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import com.unit.code.generate.constants.FormatConstant;
import com.unit.code.generate.constants.UnitTestConstant;
import com.unit.code.generate.constants.VarEnum;
import com.unit.code.generate.utils.CtClassUtils;
import com.unit.code.generate.utils.LogUtil;
import com.unit.code.generate.utils.StringUtils;

public abstract class AbstractUnitTestContextGenerator {

	private static LogUtil logUtil = LogUtil.getInstance();

	public abstract VelocityContext generate( CtClass ctClass );

	public abstract Template getUnitTestTemplate();

	public abstract void populateFieldContext( VelocityContext ctx, CtClass ctClass );

	@SuppressWarnings( "unchecked" )
	protected void populateSetupMethodContext( VelocityContext ctx ) {
		Object fieldListObject = ctx.get( UnitTestConstant.FIELD_LIST );
		if( fieldListObject != null ) {
			StringBuilder builder = new StringBuilder();
			builder.append( FormatConstant.TAB ).append( "@BeforeMethod" ).append( "\n" );
			builder.append( FormatConstant.TAB ).append( "public void setup() {" ).append( "\n" );
			List<String> fieldList = ( List<String> )fieldListObject;
			for( String field : fieldList ) {
				if( field.endsWith( "Mapper" ) ) {
					continue;
				}
				String[] fieldMembers = field.replaceAll( "[ 	]{2,}", " " ).replaceFirst( ";$", "" ).split( "\\s" );
				if( fieldMembers.length == 3 ) {
					String newInstance = getNewInstance( fieldMembers[ 1 ], fieldMembers[ 2 ] );
					builder.append( FormatConstant.DOUBLE_TAB ).append( newInstance ).append( "\n" );
				}
			}
			builder.append( FormatConstant.TAB ).append( "}" );
			ctx.put( "setup_list", builder.toString() );
		}
	}

	protected String getFieldDeclaration( String fieldType, String fieldName ) {
		StringBuilder filedDeclaration = new StringBuilder();
		filedDeclaration.append( "private" );
		filedDeclaration.append( "  " );
		filedDeclaration.append( fieldType );
		filedDeclaration.append( " " );
		filedDeclaration.append( fieldName );
		filedDeclaration.append( ";" );
		return filedDeclaration.toString();
	}

	protected String getNewInstance( String varType, String varName ) {
		if( varType == null || varName == null ) {
			return "";
		}

		String[] varTypeMembers = varType.split( "\\." );
		String varTypeInEnum = varTypeMembers[ varTypeMembers.length - 1 ].toLowerCase();
		VarEnum e = VarEnum.getVarEnumByVarType( varTypeInEnum );
		if( e == null ) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		builder.append( varName ).append( " = " ).append( e.getVarValue() ).append( ";" );

		return builder.toString();

	}

	protected void populateTestedMethodContext( VelocityContext ctx, CtClass ctClass ) {
		CtMethod[] methods = ctClass.getDeclaredMethods();
		List<String> testedMethodContents = new ArrayList<String>();
		List<String> methodNames = new ArrayList<String>();

		for( CtMethod method : methods ) {
			if( !CtClassUtils.isPublic( method ) ) {
				continue;
			}

			StringBuilder builder = constructTestContent( ctx, methodNames, method );
			testedMethodContents.add( builder.toString() );
			if( !this.isReturnVoid( method ) ) {
				String className = this.getReturnTypeClassFromMethod( method );
				if( className != null && !className.startsWith( "java.lang." ) ) {
					populateImportClass( ctx, className );
				}
			}
			methodNames.add( method.getName() );
		}

		ctx.put( "test_methods", testedMethodContents );
	}

	private StringBuilder constructTestContent( VelocityContext ctx, List<String> methodNames, CtMethod ctMethod ) {
		StringBuilder builder = new StringBuilder();
		appendAnnotations( builder, ctMethod );
		builder.append( FormatConstant.TAB ).append( "@Test" );
		builder.append( "\n" );
		generateTestMethodName( ctMethod.getName(), builder, methodNames );
		builder.append( "\n" );
		builder.append( populateMockContent( ctx, ctMethod ) );
		builder.append( populateInvokeContext( ctx, ctMethod ) );
		builder.append( "\n" );
		if( isReturnVoid( ctMethod ) ) {
			populateImportClass( ctx, "mockit.Verifications" );
			builder.append( getVerification( ctMethod ) );
		} else {
			populateImportClass( ctx, "org.testng.Assert" );
			builder.append( getAssertion( ctMethod ) );
		}
		builder.append( "\n" );
		builder.append( FormatConstant.TAB ).append( "}" );
		builder.append( "\n" );
		return builder;
	}

	protected void appendAnnotations( StringBuilder builder, CtMethod ctMethod ) {
		
	}

	protected String populateMockContent( VelocityContext ctx, CtMethod method ) {
		return "";
	}

	protected void generateTestMethodName( String orginalMethodName, StringBuilder builder, List<String> methodNames ) {
		int count = 0;
		for( String methodName : methodNames ) {
			if( methodName.equals( orginalMethodName ) ) {
				count++;
			}
		}
		String methodSuffix = "";
		if( count > 0 ) {
			methodSuffix = methodSuffix + count;
		}
		builder.append( FormatConstant.TAB ).append( "public void test" )
				.append( StringUtils.upperFirstChar( orginalMethodName ) ).append( methodSuffix ).append( "() {" );
	}

	@SuppressWarnings( "unchecked" )
	protected String populateInvokeContext( VelocityContext ctx, CtMethod ctMethod ) {
		StringBuilder result = new StringBuilder();
		result.append( FormatConstant.DOUBLE_TAB );
		if( !isReturnVoid( ctMethod ) ) {
			result.append( this.getSimpleReturnType( ctMethod ) );
			result.append( " result = " );
		}
		result.append( ctx.get( UnitTestConstant.TESTED_INSTANCE_FIELD ) ).append( "." ).append( ctMethod.getName() );
		result.append( "(" );
		Map<String, String> paramMap = new HashMap<String, String>();
		List<String> paramList = new ArrayList<String>();
		this.fillParamInfos( ctMethod, paramList, paramMap );
		String params = getParams( paramList );
		result.append( params );
		result.append( ");" );

		List<String> currentFieldList = ( List<String> )ctx.get( UnitTestConstant.FIELD_LIST );
		if( currentFieldList == null ) {
			currentFieldList = new ArrayList<String>();
			ctx.put( UnitTestConstant.FIELD_LIST, currentFieldList );
		}

		String currentFields = Arrays.toString( currentFieldList.toArray() ).replace( "\n", "" );
		for( Entry<String, String> entry : paramMap.entrySet() ) {
			String paramName = entry.getKey();
			String regexp = "^.+" + "[ ;]" + paramName + "[ ;]" + ".*$";
			if( !currentFields.matches( regexp ) ) {
				currentFieldList.add( this.getFieldDeclaration( entry.getValue(), entry.getKey() ) );
			}
		}
		return result.toString();
	}

	private String getParams( List<String> paramNameList ) {
		StringBuilder params = new StringBuilder();
		for( String paramName : paramNameList ) {
			params.append( paramName ).append( "," );
		}
		return params.toString().replaceFirst( ",$", "" );
	}

	protected boolean isReturnVoid( CtMethod method ) {
		return getSimpleReturnType( method ).equals( "void" );
	}

	protected String getAssertion( CtMethod method ) {
		StringBuilder builder = new StringBuilder();

		builder.append( FormatConstant.DOUBLE_TAB ).append( "Assert.assertNotEquals(" ).append( " result, null );" );
		return builder.toString();
	}

	protected String getVerification( CtMethod ctMethod ) {
		StringBuilder result = new StringBuilder();
		result.append( FormatConstant.DOUBLE_TAB ).append( "new Verifications() {" ).append( "\n" );
		result.append( FormatConstant.THREE_TAB ).append( "{" ).append( "\n" );
		result.append( FormatConstant.FOUR_TAB ).append( "times=1" ).append( ";" ).append( "\n" );
		result.append( FormatConstant.THREE_TAB ).append( "}" ).append( "\n" );
		result.append( FormatConstant.DOUBLE_TAB ).append( "};" ).append( "\n" );
		return result.toString();
	}

	@SuppressWarnings( "unchecked" )
	protected void populateImportClass( VelocityContext ctx, String className ) {
		if( className == null || className.isEmpty() || !className.contains( "." ) ) {
			return;
		}
		Set<String> importClasses = ( Set<String> )ctx.get( UnitTestConstant.IMPORT_TESTED_CLASS );
		if( importClasses == null ) {
			importClasses = new HashSet<String>();
			ctx.put( UnitTestConstant.IMPORT_TESTED_CLASS, importClasses );
		}

		importClasses.add( className );
	}

	private void fillParamInfos( CtMethod ctMethod, List<String> paramNameList, Map<String, String> paramMap ) {
		if( ctMethod == null ) {
			return;
		}
		MethodInfo methodInfo = ctMethod.getMethodInfo();
		CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
		LocalVariableAttribute attr = ( LocalVariableAttribute )codeAttribute.getAttribute( LocalVariableAttribute.tag );
		if( attr == null ) {
			return;
		}
		try {
			int size = ctMethod.getParameterTypes().length;
			int pos = Modifier.isStatic( ctMethod.getModifiers() ) ? 0 : 1;
			for( int i = 0; i < size; i++ ) {
				String className = ctMethod.getParameterTypes()[ i ].getName();
				String paramName = attr.variableName( i + pos );
				paramNameList.add( paramName );
				paramMap.put( paramName, className );
			}
		} catch( NotFoundException e ) {
			logUtil.logError( "未找到方法" + ctMethod.getName() + "的参数列表", e );
		}

	}

	protected String getReturnTypeClassFromMethod( CtMethod method ) {
		try {
			String className = method.getReturnType().getName();
			return className;
		} catch( NotFoundException e ) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected String getSimpleReturnType( CtMethod method ) {
		String result = "Object";
		try {
			result = method.getReturnType().getSimpleName();
		} catch( NotFoundException e ) {
			logUtil.logError( "方法" + method.getName() + "未找到返回值类型", e );
		}
		return result;
	}	
	
	protected String getFieldTypeName( CtField field ) {
		String result = "";
		try {
			result = field.getType().getName();
		} catch( NotFoundException e ) {
			e.printStackTrace();
		}
		return result;
	}

	protected String getSimpleFieldTypeName( CtField field ) {
		String result = "";
		try {
			result = field.getType().getSimpleName();
		} catch( NotFoundException e ) {
			e.printStackTrace();
		}
		return result;
	}
}
