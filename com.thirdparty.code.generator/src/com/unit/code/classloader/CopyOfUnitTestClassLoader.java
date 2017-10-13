package com.unit.code.classloader;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import com.unit.code.generate.utils.StringUtils;

public class CopyOfUnitTestClassLoader extends ClassLoader {
	private String buildPath = "";

	private Set<String> preLoadedJarFiles = new HashSet<String>();

	private Set<String> loadedClassNames = new HashSet<String>();

	@SuppressWarnings("rawtypes")
	public Class defineClassByName(String name, byte[] b, int offset, int len) {
		Class clazz = super.defineClass(name, b, offset, len);
		return clazz;
	}

	public CopyOfUnitTestClassLoader() {
		super();
	}

	public void preLoadClasses() {
		File directory = new File(this.buildPath);
		this.loadAllClassesFromBuildPath(directory);
		this.loadJarFiles();
	}

	private void loadAllClassesFromBuildPath(File directory) {
		if (directory.exists() && directory.isDirectory()) {
			File[] subFiles = directory.listFiles();
			for (File subFile : subFiles) {
				if (subFile.isDirectory()) {
					this.loadAllClassesFromBuildPath(subFile);
				} else if (subFile.getName().endsWith(".class")) {
					String className = this.getClassNameFromFileName(subFile);
					try {
						this.loadClass(className, subFile.getAbsolutePath());
						this.loadedClassNames.add(className);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	@SuppressWarnings({ "rawtypes" })
	private Class loadClass(String classNameIncludingPackage, String classFilePath) throws ClassNotFoundException,
			Exception {
		Class clazz = null;
		if (!StringUtils.isEmpty(classNameIncludingPackage)) {
			clazz = this.getClassByFile(classFilePath);
		}
		return clazz;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class cls = null;
		cls = findLoadedClass(name);
		if ( cls != null ) {
			return cls;
		}
		if (!this.loadedClassNames.contains(name)) {
			cls = getSystemClassLoader().loadClass(name);
		}
		if (cls == null) {
			throw new ClassNotFoundException(name);
		}
		if (resolve) {
			resolveClass(cls);
		}

		return cls;
	}

	private void loadJarFiles() {
		try {
			// 包路径定义
			URL urls = new URL("file:/E:/eclipse4/runtime-EclipseApplication/test/lib/spring-beans-3.2.3.RELEASE.jar");
			// URL url1 = new
			// URL("file:/E:/eclipse4/runtime-EclipseApplication/test/lib/spring-core-3.2.3.RELEASE.jar");
			// GetPI.class
			URLClassLoader urlLoader = (URLClassLoader) getSystemClassLoader();
			Class<URLClassLoader> sysclass = URLClassLoader.class;
			Method method = sysclass.getDeclaredMethod("addURL", new Class[] { URL.class });
			method.setAccessible(true);
			method.invoke(urlLoader, urls);
			URL urls1 = new URL("file:/D:/eclipse4/ws/com.unit.code.generate/lib/javassist-3.18.0-ga.jar");
			method.invoke(urlLoader, urls1);

			// method.invoke(urlLoader, url1);

			// Class.forName("org.springframework.beans.propertyeditors.CustomDateEditor");
			// urlLoader.loadClass("org.springframework.beans.factory.annotation.Autowired");
		} catch (Exception exp) {
			exp.printStackTrace();
		}
	}

	private String getClassNameFromFileName(File subFile) {
		String classFilePath = subFile.getAbsolutePath();
		if (StringUtils.isEmpty(classFilePath)) {
			return null;
		}

		String classNameIncludingPackage = classFilePath.replace(this.buildPath, "").replace(".class", "")
				.replace(File.separator, ".").replaceFirst("^.", "");
		return classNameIncludingPackage;
	}

	@SuppressWarnings({ "rawtypes", "finally" })
	public Class getClassByFile(String fileName) throws Exception {
		File classFile = new File(fileName);
		// 一般的class文件通常都小于100k，如果现实情况超出这个范围可以放大长度
		byte bytes[] = new byte[102400];
		FileInputStream fis = null;
		Class clazz = null;
		try {
			fis = new FileInputStream(classFile);
			int j = 0;
			while (true) {
				int i = fis.read(bytes);
				if (i == -1)
					break;
				j += i;
			}
			clazz = defineClassByName(null, bytes, 0, j);
			// this.loadedClassMap.put(clazz.getName(), clazz);
			// this.modifyTimeMap.put(clazz.getName(),
			// this.getClassLastModifyTime(fileName));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			fis.close();
			return clazz;
		}
	}

	public void setBuildPath(String buildPath) {
		this.buildPath = buildPath;
	}

	public void addJar(String jarFile) {
		this.preLoadedJarFiles.add(jarFile);
	}

	@SuppressWarnings("rawtypes")
	public String loadMethodInfo(String className, String methodName) {
		if (StringUtils.isEmpty(className) || StringUtils.isEmpty(methodName)) {
			return null;
		}
		// this.loadClass("javassist.ClassPool").getClassLoader();
		try {
//			Class pooClazz = this.loadClass("javassist.ClassPool"); pooClazz.getClassLoader().getParent();
//			Method getDefaultPoolMethod = pooClazz.getDeclaredMethod("getDefault");
//			ClassPool.getDefault().get("com.mm.test.BusinessClass");
//			Object pooClazzInstance = pooClazz.newInstance();
//			Object poolInstance = getDefaultPoolMethod.invoke(pooClazz, new Object[0]);
//			Method getCtClassMethod = pooClazz.getMethod("get", String.class);
//			Object ctClass = getCtClassMethod.invoke(poolInstance, className); poolInstance.getClass().getClassLoader();
//			Object pooClassInstance = method.invoke(pooClazz, new Object[0]);
//			Object ctxClassInstance = getClassMethod.invoke("", args)
//			Method getClassMethod = pooClazz.getDeclaredMethod(pooClazzInstance, String.class);
//			Object ctxClass = getClassMethod.invoke(pooClassInstance, className);

//			System.out.println(111);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		return null;
	}

}
