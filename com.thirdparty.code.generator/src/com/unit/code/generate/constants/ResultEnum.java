package com.unit.code.generate.constants;

public enum ResultEnum {
	BasicByteVarEnum("byte", "1"),
	BasicByteArrayVarEnum("byte[]", "new byte[] {1}"),

	ByteVarEnum("Byte", "1"),
	ByteArrayVarEnum("Byte[]", "new Byte[] {1}"),
	
	BasicShortVarEnums("short", "10"),
	BasicShortArrayVarEnums("short[]", "new short[] {10}"),

	ShortVarEnums("Short", "10"),
	ShortArrayVarEnums("Short[]", "new Short[] {10}"),
	
	BasicIntVarEnums("int", "100"),
	BasicIntArrayVarEnums("int[]", "new int[] {100}"),

	IntegerVarEnums("Integer", "100"),
	IntegerArrayVarEnums("Integer[]", "new Integer[] {100}"),

	BasicLongVarEnums("long", "1000L"),
	BasicLongArrayVarEnums("long[]", "new long[] {1000L}"),

	LongVarEnums("Long", "1000L"),
	LongArrayVarEnums("Long[]", "new Long[] {1000L}"),

	BasicFloatVarEnums("float", "1.0"),
	BasicFloatArrayVarEnums("float[]", "new float[] {1.0}"),

	FloatVarEnums("Float", "1.0"),
	FloatArrayVarEnums("Float[]", "new Float[] {1.0}"),

	BasicDoubleVarEnums("double", "1.0d"),
	BasicDoubleArrayVarEnums("double[]", "new double[] {1.0d}"),

	DoubleVarEnums("Double", "1.0d"),
	DoubleArrayVarEnums("Double[]", "new Double[] {1.0d}"),

	BasicBooleanEnums("boolean", "false"),
	BasicBooleanArrayEnums("boolean[]", "new boolean[] {false}"),

	BooleanEnums("Boolean", "false"),
	BooleanArrayEnums("Boolean[]", "new Boolean[] {false}"),

	BasicCharEnums("char", "'a'"),
	BasicCharArrayEnums("char[]", "new char[] {'a'}"),

	StringEnums("String", "\"test\""),
	StringArrayEnums("String[]", "new String[] {\"test\"}"),

	ListEnums("List", "new ArrayList()"),
	SetEnums("Set", "new HashSet()"),
	MapEnums("Map", "new HashMap()"),
	ObjectEnums("Object", "new Object()");

	private String varType;

	private String varValue;

	ResultEnum( String varType, String varValue ) {
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

	public static ResultEnum getVarEnumByVarType( String varType ) {
		if( varType == null ) {
			return null;
		}

		for( ResultEnum e : ResultEnum.values() ) {
			if( e.getVarType().equals( varType ) ) {
				return e;
			}
		}

		return null;
	}

}
