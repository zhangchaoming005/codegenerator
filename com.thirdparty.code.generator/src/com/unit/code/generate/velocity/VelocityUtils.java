package com.unit.code.generate.velocity;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import com.unit.code.generate.utils.FileUtils;

public class VelocityUtils {

	public static void merge( Template template, VelocityContext ctx, String path ) {
		PrintWriter writer = null;
		FileUtils.validateDir( path );
		try {
			writer = new PrintWriter( path, "UTF-8" );
			template.merge( ctx, writer );
			writer.flush();
		} catch( FileNotFoundException e ) {
			e.printStackTrace();
		} catch( UnsupportedEncodingException e ) {
			e.printStackTrace();
		} finally {
			writer.close();
		}
	}

}
