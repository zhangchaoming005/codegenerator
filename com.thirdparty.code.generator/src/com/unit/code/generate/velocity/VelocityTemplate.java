package com.unit.code.generate.velocity;

public class VelocityTemplate {

	private static VelocityTemplate velocityTemplate;

	public void getInstance() {
		if( velocityTemplate == null ) {
			synchronized( velocityTemplate ) {
				velocityTemplate = new VelocityTemplate();
			}
		}
	}

}
