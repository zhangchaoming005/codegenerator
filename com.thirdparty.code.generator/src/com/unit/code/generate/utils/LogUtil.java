package com.unit.code.generate.utils;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;

import com.unit.code.generate.Activator;

public class LogUtil {

	private static LogUtil instance = null;

	private ILog logger = null;

	private LogUtil() {
		logger = Activator.getDefault().getLog();
	}

	public static LogUtil getInstance() {
		if( instance == null ) {
			instance = new LogUtil();
		}

		return instance;
	}

	public void log( int severity, String message, Throwable exception ) {
		logger.log( new Status( severity, Activator.PLUGIN_ID, Status.OK, message, exception ) );
	}

	public void logCancel( String message, Throwable exception ) {
		logger.log( new Status( Status.CANCEL, Activator.PLUGIN_ID, Status.OK, message, exception ) );
	}

	public void logError( String message, Throwable exception ) {
		logger.log( new Status( Status.ERROR, Activator.PLUGIN_ID, Status.OK, message, exception ) );
	}

	public void logInfo( String message, Throwable exception ) {
		logger.log( new Status( Status.INFO, Activator.PLUGIN_ID, Status.OK, message, exception ) );
	}

	public void logOk( String message, Throwable exception ) {
		logger.log( new Status( Status.OK, Activator.PLUGIN_ID, Status.OK, message, exception ) );
	}

	public void logWarning( String message, Throwable exception ) {
		logger.log( new Status( Status.WARNING, Activator.PLUGIN_ID, Status.OK, message, exception ) );
	}
}
