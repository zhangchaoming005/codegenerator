package com.unit.code.generate.utils;

import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;


public class CtClassUtils {

	public static boolean isPublic( CtMethod method ) {
		return Modifier.isPublic( method.getModifiers() );
	}
	
	public static boolean isPrivate( CtMethod method ) {
		return Modifier.isPrivate( method.getModifiers() );
	}
	
	public static boolean isPrivate( CtField field ) {
		return Modifier.isPrivate( field.getModifiers() );
	}
	
	public static boolean isStatic( CtField field ) {
		return Modifier.isStatic( field.getModifiers() );
	}
}
