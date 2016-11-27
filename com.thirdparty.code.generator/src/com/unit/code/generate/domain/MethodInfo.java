package com.unit.code.generate.domain;

import java.util.List;

import com.unit.code.generate.constants.FormatConstant;

public class MethodInfo {

	private String modifiers;

	private String returnType;

	private String name;

	private String annotation;

	private String content;

	private List<ParamInfo> paramList;

	public String getModifiers() {
		return modifiers;
	}

	public void setModifiers( String modifiers ) {
		this.modifiers = modifiers;
	}

	public String getReturnType() {
		return returnType;
	}

	public void setReturnType( String returnType ) {
		this.returnType = returnType;
	}

	public String getName() {
		return name;
	}

	public void setName( String name ) {
		this.name = name;
	}

	public String getAnnotation() {
		return annotation;
	}

	public void setAnnotation( String annotation ) {
		this.annotation = annotation;
	}

	public String getContent() {
		return content;
	}

	public void setContent( String content ) {
		this.content = content;
	}

	public List<ParamInfo> getParamList() {
		return paramList;
	}

	public void setParamList( List<ParamInfo> paramList ) {
		this.paramList = paramList;
	}

	public String getDeclaration() {
		StringBuilder builder = getDeclarationUtilParam();
		if( builder == null ) {
			return null;
		}
		builder.append( getParamAndTypeSeries() ).append( " )" );

		return builder.toString();
	}

	public String getMapperDeclaration() {
		StringBuilder builder = getDeclarationUtilParam();
		if( builder == null ) {
			return null;
		}

		builder.append( getMapperParamAndTypeSeries() ).append( " )" );

		return builder.toString();
	}

	public String getInvokeStatement() {
		StringBuilder builder = new StringBuilder( this.name ).append( "( " );
		builder.append( this.getParamSeries() );
		builder.append( " )" );
		return builder.toString();
	}

	public String toString() {
		StringBuilder builder = new StringBuilder( this.name ).append( "( " );
		builder.append( this.getParamSeries() );
		builder.append( " )" );
		return builder.toString();
	}

	public String toStringWithMargin( String margin ) {
		StringBuilder builder = new StringBuilder( );
		builder.append( margin ).append( this.annotation ).append( "\n" );
		builder.append( margin ).append( this.getDeclaration() ).append( " {\n" );
		builder.append( margin ).append( FormatConstant.TAB ).append( this.getContent() ).append( ";\n" );
		builder.append( margin ).append( "}" );
		return builder.toString();
	}

	private String getParamSeries() {
		if( this.paramList != null && this.paramList.size() == 0 ) {
			return "";
		}

		StringBuilder builder = new StringBuilder();
		for( ParamInfo param : this.paramList ) {
			builder.append( ", " ).append( param.getName() );
		}

		return builder.toString().replaceFirst( "^, ", "" );
	}

	private String getParamAndTypeSeries() {
		if( this.paramList != null && this.paramList.size() == 0 ) {
			return "";
		}

		StringBuilder builder = new StringBuilder();
		for( ParamInfo param : this.paramList ) {
			builder.append( ", " ).append( param.toString() );
		}

		return builder.toString().replaceFirst( "^, ", "" );
	}

	private String getMapperParamAndTypeSeries() {
		if( this.paramList != null && this.paramList.size() == 0 ) {
			return null;
		}

		StringBuilder builder = new StringBuilder();
		for( ParamInfo param : this.paramList ) {
			builder.append( ", " ).append( param.toMapperString() );
		}

		return builder.toString().replaceFirst( "^, ", "" );
	}

	private StringBuilder getDeclarationUtilParam() {
		if( returnType == null || returnType.length() == 0 ) {
			return null;
		}

		StringBuilder builder = new StringBuilder();
		if( modifiers != null && modifiers.length() > 0 ) {
			builder.append( modifiers ).append( FormatConstant.SPACE );
		}

		builder.append( returnType ).append( FormatConstant.SPACE );
		builder.append( name ).append( "( " );
		return builder;
	}

	public static class ParamInfo {

		private String type;

		private String name;

		public ParamInfo() {

		}

		public ParamInfo( String name, String type ) {
			this.name = name;
			this.type = type;
		}

		public String getType() {
			return type;
		}

		public void setType( String type ) {
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public void setName( String name ) {
			this.name = name;
		}

		public String toString() {
			if( this.name == null || this.type == null ) {
				return null;
			}

			return this.type + FormatConstant.SPACE + this.name;
		}

		public String toMapperString() {
			if( this.name == null || this.type == null ) {
				return null;
			}

			StringBuilder builder = new StringBuilder( "@Param( \"" );
			builder.append( name ).append( "\" ) " ).append( type ).append( FormatConstant.SPACE ).append( name );

			return builder.toString();
		}
	}

}
