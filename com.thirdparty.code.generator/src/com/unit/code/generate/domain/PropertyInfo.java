package com.unit.code.generate.domain;

import com.unit.code.generate.constants.FormatConstant;

public class PropertyInfo {

	private String propertyName;

	private String propertyType;

	private String modifiers;

	private String annotation;

	private String comment;

	private String requiredClass;

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName( String propertyName ) {
		this.propertyName = propertyName;
	}

	public String getPropertyType() {
		return propertyType;
	}

	public void setPropertyType( String propertyType ) {
		this.propertyType = propertyType;
	}

	public String getModifiers() {
		return modifiers;
	}

	public void setModifiers( String modifiers ) {
		this.modifiers = modifiers;
	}

	public String getAnnotation() {
		return annotation;
	}

	public void setAnnotation( String annotation ) {
		this.annotation = annotation;
	}

	public String getComment() {
		return comment;
	}

	public void setComment( String comment ) {
		this.comment = comment;
	}

	public String getRequiredClass() {
		return requiredClass;
	}

	public void setRequiredClass( String requiredClass ) {
		this.requiredClass = requiredClass;
	}

	public String toString() {
		return this.formatWithMargin( "" );
	}

	public String formatWithMargin( String margin ) {
		StringBuilder builder = new StringBuilder();
		if( this.annotation != null ) {
			builder.append( margin ).append( this.annotation ).append( "\n" );
		}

		builder.append( margin );
		if ( this.modifiers !=null ) {
			builder.append( this.modifiers ).append( FormatConstant.SPACE );
		}
		builder.append( this.propertyType ).append( FormatConstant.SPACE ).append( this.propertyName ).append( ";" );
		return builder.toString();
	}
}
