package com.unit.code.generate.constants;

public enum AnyValueEnum {
	ByteValueEnum("byte", "anyByte"),
	ShortValueEnum("short", "anyShort"),
	IntValueEnum("int", "anyInt"),
	IntegerValueEnum("integer", "anyInt"),
	LongValueEnum("long", "anyLong"),
	FloatValueEnum("float", "anyFloat"),
	DoubleValueEnum("double", "anyDouble"),
	BooleanEnums("float", "anyBoolean"),
	CharEnums("char", "anyChar"),
	StringEnums("string", "anyString");

	private String varType;

	private String varValue;

	AnyValueEnum( String varType, String varValue ) {
		this.varType = varType;
		this.varValue = varValue;
	}

	public String getVarType() {
		return varType;
	}

	public void setVarType( String varType ) {
		this.varType = varType;
	}

	public String getVarValue() {
		return varValue;
	}

	public void setVarValue( String varValue ) {
		this.varValue = varValue;
	}

	public static AnyValueEnum getValueEnumByVarType( String varType ) {
		if( varType == null ) {
			return null;
		}

		for( AnyValueEnum e : AnyValueEnum.values() ) {
			if( e.getVarType().equals( varType ) ) {
				return e;
			}
		}

		return null;
	}

}
