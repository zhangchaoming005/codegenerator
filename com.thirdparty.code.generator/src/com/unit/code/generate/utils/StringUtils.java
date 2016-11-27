package com.unit.code.generate.utils;

public class StringUtils {

	public static boolean isEmpty( String s ) {
		if( s == null || s.isEmpty() ) {
			return true;
		}
		return false;
	}

	public static String lowerFirstChar( String source ) {
		if( isEmpty( source ) ) {
			return "";
		}
		String s = source.substring( 0, 1 ).toLowerCase();
		return s + source.substring( 1 );
	}

	public static String upperFirstChar( String source ) {
		if( isEmpty( source ) ) {
			return "";
		}
		String s = source.substring( 0, 1 ).toUpperCase();
		return s + source.substring( 1 );
	}

}
