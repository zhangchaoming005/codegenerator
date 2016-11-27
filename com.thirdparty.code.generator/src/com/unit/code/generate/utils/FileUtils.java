package com.unit.code.generate.utils;

import java.io.File;

public class FileUtils {

	public static void main( String[] args ) {
		String path = "E:\\eclipse4\\runtime-EclipseApplication\\test\\bin\\com\\meizu\\test\\A.java";

		validateDir( path );
	}

	public static String getDirFromPath( String path ) {
		if( path == null ) {
			return null;
		}

		String dir = path.replaceFirst( "[^/\\\\]+?$", "" );
		return dir;
	}

	public static void validateDir( String path ) {
		String dir = getDirFromPath( path );
		File file = new File( dir );
		if( !file.exists() || !file.isDirectory() ) {
			file.mkdirs();
		}
	}

}
