package com.unit.code.generate.popup.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.unit.code.generate.utils.AlertUtil;
import com.unit.code.generate.wizard.RepositoyrGenerateWizard;


public class RepositoryGenerateAction implements IObjectActionDelegate {

	private Shell shell;

	private IStructuredSelection selection;
	
	@Override
	public void run( IAction arg0 ) {
		RepositoyrGenerateWizard wizard = new RepositoyrGenerateWizard( selection );
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

}
