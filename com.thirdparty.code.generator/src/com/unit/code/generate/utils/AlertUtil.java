package com.unit.code.generate.utils;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

public class AlertUtil {

	private static Shell shell = null;

	public static void setShell( Shell alertShell ) {
		shell = alertShell;
	}

	public static void alert( String titile, String message ) {
		MessageDialog.openInformation( shell, titile, message );
	}

}
