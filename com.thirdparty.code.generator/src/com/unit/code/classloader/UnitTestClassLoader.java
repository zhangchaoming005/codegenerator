package com.unit.code.classloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UnitTestClassLoader extends ClassLoader {
	private String buildPath = "";

	private Map<String, Class> loadedClassMap = new ConcurrentHashMap<String, Class>();

	private Map<String, Long> modifyTimeMap = new ConcurrentHashMap<String, Long>();

	@SuppressWarnings("rawtypes")
	public Class defineClassByName(String name, byte[] b, int offset, int len) {
		Class clazz = super.defineClass(name, b, offset, len);
		return clazz;
	}

	public UnitTestClassLoader() {
		super(UnitTestClassLoader.class.getClassLoader());
	}

	public Class loadClass00(String name, boolean resolve) {
		Long lastModifiedTime = this.modifyTimeMap.get(name);
		if (lastModifiedTime == null) {
			return null;
		}

		String filePath = this.getClassCompletePath(name);
		Long currentModifiedTime = this.getClassLastModifyTime(filePath);
		if (currentModifiedTime > lastModifiedTime) {
			return null;
		}

		return this.loadedClassMap.get(name);
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
			this.loadedClassMap.put(clazz.getName(), clazz);
			this.modifyTimeMap.put(clazz.getName(), this.getClassLastModifyTime(fileName));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			fis.close();
			return clazz;
		}
	}

	private long getClassLastModifyTime(String path) {
		File file = new File(path);
		if (!file.exists()) {
			throw new RuntimeException(new FileNotFoundException(path));
		}
		return file.lastModified();
	}

	private String getClassCompletePath(String name) {
		String path = this.buildPath + File.separator + name.replace(".", File.separator) + ".class";
		return path;
	}

	public String getBuildPath() {
		return buildPath;
	}

	public void setBuildPath(String buildPath) {
		this.buildPath = buildPath;
	}

}
