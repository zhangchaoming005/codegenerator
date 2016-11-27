package com.unit.code.generate.velocity;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

public class UnitTestVelocityEngine {

	private static VelocityEngine velocityEngine = null;

	public static VelocityEngine getEngine() {
		if( velocityEngine == null ) {
			velocityEngine = new VelocityEngine();
			velocityEngine.setProperty( RuntimeConstants.RESOURCE_LOADER, "classpath" );
			velocityEngine.setProperty( "classpath.resource.loader.class", ClasspathResourceLoader.class.getName() );
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader( VelocityEngine.class.getClassLoader() );
			velocityEngine.init();
			Thread.currentThread().setContextClassLoader( loader );
		}
		return velocityEngine;
	}

}
