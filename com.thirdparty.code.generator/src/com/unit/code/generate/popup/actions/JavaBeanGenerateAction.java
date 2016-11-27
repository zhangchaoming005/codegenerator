package com.unit.code.generate.popup.actions;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.unit.code.generate.utils.AlertUtil;
import com.unit.code.generate.wizard.JavaBeanGenerateWizard;

public class JavaBeanGenerateAction implements IObjectActionDelegate {

	private Shell shell;

	private IStructuredSelection selection;
	
	private static Map<String, String> JAVA_BEAN_MAP = new HashMap<String, String>();

	@Override
	public void run( IAction arg0 ) {
		JavaBeanGenerateWizard wizard = new JavaBeanGenerateWizard( selection );
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		WizardDialog dialog = new WizardDialog( shell, wizard );
		dialog.create();
		dialog.open();
	}

	@Override
	public void selectionChanged( IAction action, ISelection selection ) {
		this.selection = ( IStructuredSelection )selection;
	}

	@Override
	public void setActivePart( IAction action, IWorkbenchPart targetPart ) {
		shell = targetPart.getSite().getShell();
		AlertUtil.setShell( shell );
	}

	public static void setJavaBean( String className, String classQualifiedName ) {
		JAVA_BEAN_MAP.put( className, classQualifiedName );
	}
	
	public static String getJavaBeanQulifiedName( String className ) {
		return JAVA_BEAN_MAP.get( className );
	}
}
