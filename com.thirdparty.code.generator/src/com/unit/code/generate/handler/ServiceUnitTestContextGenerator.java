package com.unit.code.generate.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.annotation.Annotation;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.unit.code.generate.constants.AnyValueEnum;
import com.unit.code.generate.constants.FormatConstant;
import com.unit.code.generate.constants.ResultEnum;
import com.unit.code.generate.constants.SourceCodeConstant;
import com.unit.code.generate.constants.UnitTestConstant;
import com.unit.code.generate.constants.VarEnum;
import com.unit.code.generate.domain.MethodMember;
import com.unit.code.generate.popup.actions.UnitTestGenerateAction;
import com.unit.code.generate.utils.CtClassUtils;
import com.unit.code.generate.utils.LogUtil;
import com.unit.code.generate.utils.StringUtils;
import com.unit.code.generate.velocity.UnitTestVelocityEngine;

public class ServiceUnitTestContextGenerator extends AbstractUnitTestContextGenerator {

	private static LogUtil logUtil = LogUtil.getInstance();

	public static AbstractUnitTestContextGenerator generator = null;

	private static final String CONFIG_UNIT_TEST_TEMPLATE_VM = "config/unit_test_template_service.vm";

	private Template unitTestTemplate;

	private VelocityEngine velocityEngine;

	private Map<String, MethodMember> testedMethodMemberMap = new HashMap<String, MethodMember>();

	private List<CtField> autowiredFields = new ArrayList<CtField>();

	private Map<String, String> mockedMethodMap = new HashMap<String, String>();

	private Map<String, String> invokedMethodMap = new HashMap<String, String>();

	private Set<String> privateMethodNames = new HashSet<String>();
	
	private Set<String> uncheckedMethodNames = new HashSet<String>();

	private ServiceUnitTestContextGenerator() {
		this.velocityEngine = UnitTestVelocityEngine.getEngine();
		this.unitTestTemplate = velocityEngine.getTemplate( CONFIG_UNIT_TEST_TEMPLATE_VM );
	}

	public static AbstractUnitTestContextGenerator getInstance() {
		if( generator == null ) {
			generator = new ServiceUnitTestContextGenerator();
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
		this.populateImportClass( ctx, ctClass.getName() );
		ctx.put( UnitTestConstant.TESTED_CLASS_NAME, ctClass.getSimpleName() );
		ctx.put( UnitTestConstant.TESTED_INSTANCE_FIELD, StringUtils.lowerFirstChar( ctClass.getSimpleName() ) );
	}

	private boolean isInjectable( CtField field ) {
		if( field == null ) {
			return false;
		}
		
		FieldInfo fieldInfo = field.getFieldInfo();
		AnnotationsAttribute attribute = ( AnnotationsAttribute )fieldInfo.getAttribute( AnnotationsAttribute.visibleTag );
		if( attribute == null ) {
			return false;
		}
		Annotation[] annotations = attribute.getAnnotations();
		for( Annotation annotation : annotations ) {
			if( annotation.getTypeName().equals( "org.springframework.beans.factory.annotation.Autowired" ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void populateFieldContext( VelocityContext ctx, CtClass ctClass ) {
		autowiredFields.clear();
		CtField[] fields = ctClass.getDeclaredFields();

		List<String> fieldList = new ArrayList<String>();
		List<String> injectableFieldList = new ArrayList<String>();

		for( CtField field : fields ) {
			StringBuilder builder = new StringBuilder();
			try {
				String fieldType = field.getType().getSimpleName();
				String fieldName = field.getName();
				String filedDeclaration = getFieldDeclaration( fieldType, fieldName );
				builder.append( filedDeclaration );
				populateImportClass( ctx, field.getType().getName() );
			} catch( NotFoundException e ) {
				logUtil.logError( "未找到字段" + field.getName() + "的类型", e );
			}

			boolean isInjectable = this.isInjectable( field );
			if( isInjectable ) {
				injectableFieldList.add( builder.toString() );
				autowiredFields.add( field );
			} else {
				fieldList.add( builder.toString() );
			}
		}
		ctx.put( UnitTestConstant.FIELD_LIST, fieldList );
		ctx.put( UnitTestConstant.INJECTABLE_FIELD_LIST, injectableFieldList );
	}

	public void populateTestedMethodContext( VelocityContext ctx, CtClass ctClass ) {
		this.privateMethodNames.clear();
		CtMethod[] methods = ctClass.getDeclaredMethods();
		List<MethodMember> testedMethodMembers = new ArrayList<MethodMember>( methods.length );
		for( CtMethod method : methods ) {
			MethodMember methodMember = new MethodMember();
			methodMember.setMethodName( method.getName() );
			methodMember.setLineNumber( method.getMethodInfo().getLineNumber( 0 ) );
			if( CtClassUtils.isPublic( method ) ) {
				methodMember.setPublic( true );
			} else {
				methodMember.setPublic( false );
				privateMethodNames.add( method.getName() );
			}
			testedMethodMembers.add( methodMember );
		}

		Collections.sort( testedMethodMembers );

		String javaFile = UnitTestGenerateAction.srcJavaPath + File.separator + ctClass.getName().replace( ".", File.separator ) + ".java";
		fillMethodCode( javaFile, testedMethodMembers );

		populateMockedMethdsAndCode();

		populateMockMethodForTestedMethod( testedMethodMembers );

		setSuppressWarnings();
		
		super.populateTestedMethodContext( ctx, ctClass );
	}

	private void setSuppressWarnings() {
		for ( MethodMember methodMember : this.testedMethodMemberMap.values() ) {
			for ( String mockedMethodName : methodMember.getMockMethodNames() ) {
				if ( this.uncheckedMethodNames.contains( mockedMethodName.replaceFirst( "^.*\\.(\\w+)$", "$1" ) )) {
					methodMember.setNeedSuppressWarnings( true );
				}
			}
		}
		
	}

	protected String populateMockContent( VelocityContext ctx, CtMethod method ) {
		String methodName = method.getName();
		MethodMember methodMember = this.testedMethodMemberMap.get( methodName );
		if( methodMember == null ) {
			return "";
		}

		StringBuilder builder = new StringBuilder();
		for( String mockMethod : methodMember.getMockMethodNames() ) {
			String mockCode = this.mockedMethodMap.get( mockMethod );
			if( mockCode == null ) {
				continue;
			}

			builder.append( mockCode ).append( "\n" );
		}

		return builder.toString();
	}

	protected String getVerification( CtMethod testedMethod ) {
		StringBuilder result = new StringBuilder();
		result.append( FormatConstant.DOUBLE_TAB ).append( "new Verifications() {" ).append( "\n" );

		String methodName = testedMethod.getName();
		MethodMember methodMember = this.testedMethodMemberMap.get( methodName );
		if( methodMember == null ) {
			return "";
		}
		for( String mockMethodName : methodMember.getMockMethodNames() ) {
			result.append( FormatConstant.THREE_TAB ).append( "{" ).append( "\n" );
			String invokeCode = invokedMethodMap.get( mockMethodName );
			if( invokeCode != null ) {
				result.append( invokeCode ).append( "\n" );
			}
			result.append( FormatConstant.FOUR_TAB ).append( "times=1" ).append( ";" ).append( "\n" );
			result.append( FormatConstant.THREE_TAB ).append( "}" ).append( "\n" );
		}
		result.append( FormatConstant.DOUBLE_TAB ).append( "};" ).append( "\n" );
		return result.toString();
	}

	@Override
	protected void appendAnnotations( StringBuilder builder, CtMethod ctMethod ) {
		MethodMember methodMember = this.testedMethodMemberMap.get( ctMethod.getName() );
		if( methodMember != null && methodMember.isNeedSuppressWarnings() ) {
			builder.append( FormatConstant.TAB ).append( "@SuppressWarnings(\"unchecked\")" ).append( "\n" );
		}
	}

	/**
	 * 为每个方法填充要mock的方法
	 * @param testedMethodMembers 
	 */
	private void populateMockMethodForTestedMethod( List<MethodMember> testedMethodMembers ) {
		Set<String> mockedMethodNames = mockedMethodMap.keySet();

		testedMethodMemberMap.clear();
		for( MethodMember testedMethodMember : testedMethodMembers ) {
			for( String codeLine : testedMethodMember.getMethodCodes() ) {
				for( String mockedMethodName : mockedMethodNames ) {
					if( codeLine.contains( mockedMethodName + "(" ) ) {
						testedMethodMember.getMockMethodNames().add( mockedMethodName );
					}
				}
			}
			this.testedMethodMemberMap.put( testedMethodMember.getMethodName(), testedMethodMember );
		}

		Pattern pattern = null;
		Matcher matcher = null;

		for( MethodMember testedMethodMember : testedMethodMembers ) {
			if( !testedMethodMember.isPublic() ) {
				continue;
			}

			for( String codeLine : testedMethodMember.getMethodCodes() ) {
				for( String privateMethodName : privateMethodNames ) {
					if( codeLine.contains( "this." + privateMethodName + "(" ) ) {
						testedMethodMember.getMockMethodNames().addAll( this.getMockedMethods( privateMethodName ) );
						continue;
					}

					pattern = Pattern.compile( "\\s" + privateMethodName + "\\(" );
					matcher = pattern.matcher( codeLine );
					if( matcher.lookingAt() ) {
						testedMethodMember.getMockMethodNames().addAll( this.getMockedMethods( privateMethodName ) );
					}
				}
			}
		}
	}

	private List<String> getMockedMethods( String testedMethod ) {
		if( testedMethodMemberMap.get( testedMethod ) == null ) {
			return new ArrayList<String>( 0 );
		}

		return testedMethodMemberMap.get( testedMethod ).getMockMethodNames();
	}

	public Template getUnitTestTemplate() {
		return unitTestTemplate;
	}

	private void populateMockedMethdsAndCode() {
		mockedMethodMap.clear();
		invokedMethodMap.clear();
		uncheckedMethodNames.clear();
		try {
			if( this.autowiredFields.isEmpty() ) {
				return;
			}
			for( CtField field : autowiredFields ) {
				String fieldType = field.getType().getName();
				String fieldName = field.getName();
				CtClass fieldClass = UnitTestGenerateAction.classPool.get( fieldType );
				CtMethod[] methods = fieldClass.getDeclaredMethods();
				for( CtMethod method : methods ) {
					String mockMethod = fieldName + "." + method.getName();
					if( CtClassUtils.isPublic( method ) && !isReturnVoid( method ) ) {
						mockedMethodMap.put( fieldName + "." + method.getName(), this.generateMockCode( mockMethod, method ) );
						invokedMethodMap.put( fieldName + "." + method.getName(), generateInvokeForVerification( mockMethod, method ) );
					}
				}
			}
		} catch( NotFoundException e ) {
			e.printStackTrace();
		}
	}

	private String generateInvokeForVerification( String mockMethod, CtMethod method ) {
		StringBuilder builder = new StringBuilder();
		builder.append( FormatConstant.FOUR_TAB ).append( mockMethod ).append( "(" );
		builder.append( generateParamsWithAnyValues( method ) );
		builder.append( ");" );
		return builder.toString();
	}

	/**
	 * 生成anyInt、anyLong形式的参数列表
	 * @param method
	 * @return
	 */
	private String generateParamsWithAnyValues( CtMethod method ) {
		StringBuilder builder = new StringBuilder( "(" );
		boolean needSuppressWarnings = false;
		try {
			CtClass[] parameterTypes;
			parameterTypes = method.getParameterTypes();
			for( CtClass parameterType : parameterTypes ) {
				String[] varTypeMembers = parameterType.getName().split( "\\." );
				String varTypeInEnum = varTypeMembers[ varTypeMembers.length - 1 ].toLowerCase();
				AnyValueEnum e = AnyValueEnum.getValueEnumByVarType( varTypeInEnum );
				builder.append( "," );
				if( e != null ) {
					builder.append( e.getVarValue() );
				} else {
					builder.append( " ( " ).append( parameterType.getSimpleName() ).append( " )" ).append( "any" );
					if ( !parameterType.getSimpleName().contains( "[" ) && !parameterType.getSimpleName().contains( "]" )) {
						needSuppressWarnings = true;
					}
				}
			}
			builder.append( ")" );
		} catch( NotFoundException e1 ) {
			e1.printStackTrace();
		}

		if( needSuppressWarnings ) {
			uncheckedMethodNames.add( method.getName() );
		}

		return builder.toString().replace( "(,", "(" );
	}

	private String generateMockCode( String mockMethod, CtMethod method ) {
		StringBuilder builder = new StringBuilder();
		builder.append( FormatConstant.DOUBLE_TAB ).append( "new NonStrictExpectations() {" ).append( "\n" );
		builder.append( FormatConstant.THREE_TAB ).append( "{" ).append( "\n" );
		builder.append( FormatConstant.FOUR_TAB ).append( mockMethod ).append( generateMockedInvoke( method ) ).append( ";\n" );
		builder.append( FormatConstant.FOUR_TAB ).append( generateMockResult( method ) ).append( ";\n" );
		builder.append( FormatConstant.THREE_TAB ).append( "}" ).append( "\n" );
		builder.append( FormatConstant.DOUBLE_TAB ).append( "}" ).append( ";" );
		return builder.toString();
	}

	private String generateMockResult( CtMethod method ) {
		StringBuilder builder = new StringBuilder( "result = " );
		String returnType = getSimpleReturnType( method );
		ResultEnum resultEnum = ResultEnum.getVarEnumByVarType( returnType );
		if( resultEnum != null ) {
			builder.append( resultEnum.getVarValue() );
		} else {
			builder.append( " new " + returnType + "()" );
		}

		return builder.toString();
	}

	private String generateMockedInvoke( CtMethod method ) {
		StringBuilder builder = new StringBuilder( "(" );
		try {
			CtClass[] parameterTypes;
			parameterTypes = method.getParameterTypes();
			for( CtClass parameterType : parameterTypes ) {
				String[] varTypeMembers = parameterType.getName().split( "\\." );
				String varTypeInEnum = varTypeMembers[ varTypeMembers.length - 1 ].toLowerCase();
				VarEnum e = VarEnum.getVarEnumByVarType( varTypeInEnum );
				builder.append( "," );
				if( e != null ) {
					builder.append( e.getVarValue() );
				} else {
					builder.append( " (" ).append( parameterType.getSimpleName() ).append( ") " ).append( "any" );
				}
			}
			builder.append( ")" );
		} catch( NotFoundException e1 ) {
			e1.printStackTrace();
		}
		return builder.toString().replace( "(,", "(" );
	}

	/**
	 * 读取源码，并将源码填充到方法对象中
	 * @param javaFile
	 * @param methodMembers
	 */
	private void fillMethodCode( String javaFile, List<MethodMember> methodMembers ) {
		InputStreamReader reader = null;
		BufferedReader bufferedReader = null;
		List<String> allCodeLines = new ArrayList<String>();

		try {
			reader = new InputStreamReader( new FileInputStream( javaFile ), "UTF-8" );
			bufferedReader = new BufferedReader( reader );
			String codeLine = null;
			while( ( codeLine = bufferedReader.readLine() ) != null ) {
				allCodeLines.add( codeLine );
			}
		} catch( UnsupportedEncodingException e ) {
			e.printStackTrace();
		} catch( FileNotFoundException e ) {
			e.printStackTrace();
		} catch( IOException e ) {
			e.printStackTrace();
		} finally {
			if( reader != null ) {
				try {
					reader.close();
				} catch( IOException e ) {
					e.printStackTrace();
				}
			}
			if( bufferedReader != null ) {
				try {
					bufferedReader.close();
				} catch( IOException e ) {
					e.printStackTrace();
				}
			}
		}

		for( int i = 0; i < methodMembers.size(); i++ ) {
			MethodMember current = methodMembers.get( i );
			int startLineIndex = current.getLineNumber() - 1;

			int endLineIndex;
			if( i == ( methodMembers.size() - 1 ) ) {
				endLineIndex = allCodeLines.size() - 1;
			} else {
				MethodMember next = methodMembers.get( i + 1 );
				endLineIndex = next.getLineNumber() - 3;
			}

			List<String> methodCodes = new ArrayList<String>();
			for( int lineIndex = startLineIndex; lineIndex < endLineIndex; lineIndex++ ) {
				String lineCode = allCodeLines.get( lineIndex ).trim();

				if( lineCode.isEmpty() ) {
					continue;
				}

				if( lineCode.startsWith( "}" ) || lineCode.startsWith( "/" ) || lineCode.startsWith( "*" ) ) {
					continue;
				}
				methodCodes.add( lineCode );
			}
			current.setMethodCodes( methodCodes );
		}
	}

}
