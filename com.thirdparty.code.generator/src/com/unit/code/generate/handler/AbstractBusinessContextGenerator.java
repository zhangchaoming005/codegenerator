package com.unit.code.generate.handler;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;


public abstract class AbstractBusinessContextGenerator {
	public abstract VelocityContext generate( );

	public abstract Template getUnitTestTemplate();
	
}
