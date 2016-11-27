package com.unit.code.generate.domain;

public class DbInfo {

	private String prefix;

	private String type = "mysql";

	private String url;

	private String userName;

	private String password;

	private String schemaNames;

	private String driverClassName = "com.mysql.jdbc.Driver";

	private boolean onlyTest;

	private String txManager;

	public DbInfo( String prefix ) {
		this.prefix = prefix;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix( String prefix ) {
		this.prefix = prefix;
	}

	public String getType() {
		return type;
	}

	public void setType( String type ) {
		this.type = type;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl( String url ) {
		this.url = url;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName( String userName ) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword( String password ) {
		this.password = password;
	}

	public String getSchemaNames() {
		return schemaNames;
	}

	public void setSchemaNames( String schemaNames ) {
		this.schemaNames = schemaNames;
	}

	public String getDriverClassName() {
		return driverClassName;
	}

	public void setDriverClassName( String driverClassName ) {
		this.driverClassName = driverClassName;
	}

	public boolean isOnlyTest() {
		return onlyTest;
	}

	public void setOnlyTest( boolean onlyTest ) {
		this.onlyTest = onlyTest;
	}

	public String getTxManager() {
		return txManager;
	}

	public void setTxManager( String txManager ) {
		this.txManager = txManager;
	}

}
