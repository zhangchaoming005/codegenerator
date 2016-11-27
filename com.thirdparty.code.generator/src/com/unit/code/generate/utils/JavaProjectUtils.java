package com.unit.code.generate.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

public class JavaProjectUtils {

	public static IJavaProject getCurrentProject( IJavaElement unit ) {
		IJavaElement javaElement = unit.getParent();
		while( javaElement.getParent() != null && !( javaElement.getParent() instanceof IJavaProject ) ) {
			javaElement = javaElement.getParent();
		}

		return ( IJavaProject )javaElement.getParent();
	}

	public static List<String> getReferencedBuildPaths( IJavaProject javaProject ) {
		List<String> result = new ArrayList<String>();
		try {
			IProject[] projects = javaProject.getProject().getReferencedProjects();
			for( IProject project : projects ) {
				if( project.hasNature( JavaCore.NATURE_ID ) ) {
					IJavaProject tmpJavaProject = JavaCore.create( project );
					result.add( JavaProjectUtils.getBuildPath( tmpJavaProject ) );
				}
			}
		} catch( CoreException e ) {
			e.printStackTrace();
		}
		return result;
	}

	public static String formatPath( String buildPath ) {
		if( StringUtils.isEmpty( buildPath ) ) {
			return null;
		}

		String result = buildPath.replace( "\\", File.separator ).replace( "/", File.separator );
		return result;
	}

	public static String getBuildPath( IJavaProject javaProject ) {
		try {
			// javaProject.getReferencedClasspathEntries();
			// javaProject.getRequiredProjectNames();

			String buildPath = javaProject.getProject().getLocation().toOSString()
					+ javaProject.getOutputLocation().toOSString()
							.replace( File.separator + javaProject.getProject().getName(), "" );
			buildPath = JavaProjectUtils.formatPath( buildPath );
			return buildPath;
		} catch( JavaModelException e ) {
			e.printStackTrace();
		}

		return null;
	}

	public static void openProjectFile( IProject project, String filePath ) throws PartInitException {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		filePath = filePath.replace( project.getLocation().toOSString(), "" );
		IFile java_file = project.getFile( new Path( filePath ) );
		IDE.openEditor( page, java_file );
	}
}
