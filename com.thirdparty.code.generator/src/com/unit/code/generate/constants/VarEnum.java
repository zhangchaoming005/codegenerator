package com.unit.code.generate.constants;

public enum VarEnum {
	ByteVarEnum("byte", "1"),
	ShortVarEnum("short", "10"),
	IntVarEnum("int", "100"),
	IntegerVarEnum("integer", "100"),
	LongVarEnum("long", "1000L"),
	FloatVarEnum("float", "1.0"),
	DoubleVarEnum("double", "1.0d"),
	BooleanEnum("boolean", "false"),
	CharEnum("char", "'a'"),
	StringEnum("string", "\"test\"");

	private String varType;

	private String varValue;

	VarEnum( String varType, String varValue ) {
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

	public static VarEnum getVarEnumByVarType( String varType ) {
		if( varType == null ) {
			return null;
		}

		for( VarEnum e : VarEnum.values() ) {
			if( e.getVarType().equals( varType ) ) {
				return e;
			}
		}

		return null;
	}

}
