package com.unit.code.generate.domain;

import java.util.ArrayList;
import java.util.List;

public class MethodMember implements Comparable<MethodMember> {

	private String methodName;

	private boolean isPublic;

	private boolean needSuppressWarnings;

	private int lineNumber;

	private List<String> methodCodes;

	private List<String> mockMethodNames = new ArrayList<String>();

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName( String methodName ) {
		this.methodName = methodName;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic( boolean isPublic ) {
		this.isPublic = isPublic;
	}

	public boolean isNeedSuppressWarnings() {
		return needSuppressWarnings;
	}

	public void setNeedSuppressWarnings( boolean needSuppressWarnings ) {
		this.needSuppressWarnings = needSuppressWarnings;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber( int lineNumber ) {
		this.lineNumber = lineNumber;
	}

	public List<String> getMethodCodes() {
		return methodCodes;
	}

	public void setMethodCodes( List<String> methodCodes ) {
		this.methodCodes = methodCodes;
	}

	public List<String> getMockMethodNames() {
		return mockMethodNames;
	}

	public void setMockMethods( List<String> mockMethodNames ) {
		this.mockMethodNames = mockMethodNames;
	}

	@Override
	public int compareTo( MethodMember member ) {
		return this.lineNumber - member.getLineNumber();
	}

}
