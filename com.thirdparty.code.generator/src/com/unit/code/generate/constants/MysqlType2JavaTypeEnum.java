package com.unit.code.generate.constants;

public enum MysqlType2JavaTypeEnum {
	TinyIntEnum("tinyint", "Integer", ""),
	SmallIntEnum("smallint", "Integer", ""),
	MediumIntEnum("mediumint", "Integer", ""),
	IntEnum("int", "Integer", ""),
	IntegerEnum("integer", "Integer", ""),
	BigIntEnum("bigint", "Long", ""),
	FloatEnum("float", "Float", ""),
	DoubleEnum("double", "Double", ""),
	DecimalEnum("decimal", "BigDecimal", "import java.util.Date"),

	CharEnum("char", "String", ""),
	VarCharEnum("varchar", "String", ""),
	TextEnum("text", "String", ""),

	DateEnum("date", "Date", "import java.util.Date"),
	TimeEnum("time", "Date", "import java.util.Date"),
	TimeStampEnum("timestamp", "Date", "import java.util.Date"),
	DateTimeEnum("datetime", "Date", "import java.util.Date"), ;

	private String mysqlType;

	private String javaType;

	private String importClass;

	MysqlType2JavaTypeEnum( String mysqlType, String javaType, String importClass ) {
		this.mysqlType = mysqlType;
		this.javaType = javaType;
		this.importClass = importClass;
	}

	public String getMysqlType() {
		return mysqlType;
	}

	public void setMysqlType( String mysqlType ) {
		this.mysqlType = mysqlType;
	}

	public String getJavaType() {
		return javaType;
	}

	public void setJavaType( String javaType ) {
		this.javaType = javaType;
	}

	public String getImportClass() {
		return importClass;
	}

	public void setImportClass( String importClass ) {
		this.importClass = importClass;
	}

	public static MysqlType2JavaTypeEnum getEnumByMysqlType( String mysqlType ) {
		if( mysqlType == null ) {
			return null;
		}

		for( MysqlType2JavaTypeEnum item : MysqlType2JavaTypeEnum.values() ) {
			if( item.getMysqlType().equals( mysqlType ) ) {
				return item;
			}
		}

		return null;
	}

}
