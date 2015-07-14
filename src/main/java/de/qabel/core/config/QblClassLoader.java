package de.qabel.core.config;


import java.net.URL;
import java.net.URLClassLoader;

public class QblClassLoader extends URLClassLoader {
	public QblClassLoader(ClassLoader classLoader) {
		super(new URL[0], classLoader);
	}

	@Override
	public void addURL(URL url) {
		super.addURL(url);
	}
}
