package com.unit.code.generate.wizard;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (mpe).
 */

public class BusinessGenerateWizardPage extends WizardPage {

	private Text fileText;

	private Set<String> dbKeys = new HashSet<String>();

	private Combo combo;

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public BusinessGenerateWizardPage( ) {
		super( "wizardPage" );
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl( Composite parent ) {
		Composite container = new Composite( parent, SWT.NULL );
		GridLayout layout = new GridLayout();
		container.setLayout( layout );
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		Label label = new Label( container, SWT.NULL );
		label.setText( "&选择数据库：" );

		GridData gd = new GridData( GridData.FILL_HORIZONTAL );
		this.combo = new Combo( container, SWT.READ_ONLY ); // 定义一个只读的下拉框
		combo.setLayoutData( gd );
		for( String dbKey : dbKeys ) {
			combo.add( dbKey );
		}

		label = new Label( container, SWT.NULL );
		label.setText( "&数据表名称:" );

		fileText = new Text( container, SWT.BORDER | SWT.SINGLE );
		gd = new GridData( GridData.FILL_HORIZONTAL );
		fileText.setLayoutData( gd );

		setControl( container );
	}

	public Set<String> getDbKeys() {
		return dbKeys;
	}

	public void setDbKeys( Set<String> dbKeys ) {
		this.dbKeys = dbKeys;
	}

	public String getSelectedDbKey() {
		return this.combo.getText();
	}

	public String getTableName() {
		return this.fileText.getText();
	}
}