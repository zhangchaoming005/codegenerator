package com.unit.code.generate.domain;

public class ColumnInfo {

	private String columnName;

	private String columnType;

	private String comment;

	private boolean autoIncrement;

	private boolean primary;

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName( String columnName ) {
		this.columnName = columnName;
	}

	public String getColumnType() {
		return columnType;
	}

	public void setColumnType( String columnType ) {
		this.columnType = columnType;
	}

	public String getComment() {
		return comment;
	}

	public void setComment( String comment ) {
		this.comment = comment;
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary( boolean primary ) {
		this.primary = primary;
	}

	public boolean isAutoIncrement() {
		return autoIncrement;
	}

	public void setAutoIncrement( boolean autoIncrement ) {
		this.autoIncrement = autoIncrement;
	}

}
