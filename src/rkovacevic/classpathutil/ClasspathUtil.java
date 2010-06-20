package rkovacevic.classpathutil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility methods for searching classpath resources for <code>Class</code> objects.
 * 
 * @author Robert Kovacevic <robert.kovacevic1@gmail.com>
 * @version 1.0.1 - 25. 03. 2010.
 * 
 */
public final class ClasspathUtil {

	private ClasspathUtil() {
		// static class
	}

	/**
	 * Finds all concrete (non abstract and non interface) <code>Class</code> objects in a specific package,
	 * that implement given interface.
	 * 
	 * @param packageName search in this package
	 * @param interFace search for this interface
	 * @return <code>Set</code> of <code>Class</code> objects that implement the interface
	 * @throws IOException
	 */
	public static Set<Class<?>> getConcreteClassesWithInterface(String packageName, Class<?> interFace) throws IOException {
		Set<Class<?>> classes = new HashSet<Class<?>>();

		if (!interFace.isInterface()) {
			return classes;
		}
		
		for (Class<?> c : getAllClasses(packageName)) {
			if (!Modifier.isAbstract(c.getModifiers()) && !c.isInterface()) {
				if (implementsInterface(c, interFace)) {
					classes.add(c);
				}
			}
		}

		return classes;
	}
	
	public static boolean implementsInterface(Class<?> klass, Class<?> interFace) {
		if (interFace == null || !interFace.isInterface() || klass == null) {
			return false;
		}
		
		for (Class<?> i: klass.getInterfaces()) {
			
			if (isInterfaceEqual(i, interFace)) {
				return true;
			}
		}
		
		Class<?> superClass = klass.getSuperclass();
		if (superClass != null) {
			return implementsInterface(superClass, interFace);
		}
		
		return false;
	}
	
	public static boolean isInterfaceEqual(Class<?> interface1, Class<?> interface2) {
		if (interface1.equals(interface2)) {
			return true;
		}
		
		Class<?> superClass = interface1.getSuperclass();
		if (superClass != null) {
			return isInterfaceEqual(superClass, interface2);
		}
		
		return false;
	}

	/**
	 * Iterates through all resources in current thread's <code>ClassLoader</code> and searches for
	 * <i>.class</i> files, then attempts to parse the class name from file name, and find the 
	 * <code>Class</code> object. <br>
	 * Searches only through <i>.jar</i> files and extracted <i>.class</i> files, so, for example, 
	 * it can't search the java.* package.
	 * 
	 * @param packageName search in this package
	 * @return <code>Set</code> of found <code>Class</code> objects
	 * @throws IOException
	 */
	public static Set<Class<?>> getAllClasses(String packageName) throws IOException {
		Set<Class<?>> classes = new HashSet<Class<?>>();

		if (packageName == null || packageName.length() == 0) {
			return classes;
		}

		for (URL resource : Collections.list(getClassLoader().getResources(packageName.replace('.', '/')))) {
			if (resource.getProtocol().equalsIgnoreCase("jar")) {
				classes.addAll(getClassesInJarResource(resource, packageName));
			} else if (resource.getProtocol().equalsIgnoreCase("file")) {
				classes.addAll(getClassesInFileResource(new File(URLDecoder.decode(resource.getPath(), "UTF-8")), packageName));
			}
		}

		return classes;
	}

	private static Set<Class<?>> getClassesInJarResource(URL resource, String packageName) throws IOException {
		Set<Class<?>> classes = new HashSet<Class<?>>();

		JarURLConnection conn = (JarURLConnection) resource.openConnection();
		JarFile jar = conn.getJarFile();

		for (JarEntry entry : Collections.list(jar.entries())) {
			String n = entry.getName();
			if (n.startsWith(packageName.replace('.', '/')) && n.endsWith(".class")) {
				if (n.contains("$")) {
					if (isAnonymous(n)) {
						return classes;
					}
				}
				String className = n.replace("/", ".").substring(0, n.length() - 6);
				try {
					Class<?> klass = getClassForName(className);
					if (!Modifier.isPrivate(klass.getModifiers())) {
						classes.add(klass);
					}
				} catch (ClassNotFoundException e) {
					//
				}
			}
		}

		return classes;
	}

	private static Set<Class<?>> getClassesInFileResource(File file, String packageName) throws IOException {
		Set<Class<?>> classes = new HashSet<Class<?>>();

		if (file.exists()) {
			if (file.isDirectory()) {
				for (File f : file.listFiles()) {
					classes.addAll(getClassesInFileResource(f, packageName));
				}
			} else {
				String fileName = file.getAbsolutePath();
				if (fileName.endsWith(".class")) {
					if (fileName.contains("$")) {
						String innerName = fileName.substring(fileName.lastIndexOf("$") + 1, fileName.lastIndexOf("."));
						if (isAnonymous(innerName)) {
							// Ignore anonymous inner classes
							return classes;
						}
					}
					// removes the .class extension
					fileName = fileName.substring(0, fileName.length() - 6);
					fileName = fileName.replace(getSystemFileSeperator(), ".");
					fileName = fileName.substring(fileName.indexOf(packageName));

					try {
						Class<?> klass = getClassForName(fileName);
						if (!Modifier.isPrivate(klass.getModifiers())) {
							classes.add(klass);
						}
					} catch (ClassNotFoundException e) {
						//
					}
				}
			}
		}

		return classes;
	}

	private static Class<?> getClassForName(String className) throws ClassNotFoundException {
		try {
			Class<?> c = Class.forName(className);
			return c;
		} catch (Exception exception) {
			throw new ClassNotFoundException("Exception getting class for name: " + className + " - " + exception.getMessage());
		} catch (Error error) {
			throw new ClassNotFoundException("Error getting class for name: " + className + " - " + error.getMessage());
		}
	}

	private static ClassLoader getClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	private static String getSystemFileSeperator() {
		String p = System.getProperty("file.separator");
		if (p == null) {
			p = "/";
		}
		return p;
	}
	
	private static boolean isAnonymous(String className) {
		return className.matches("\\d*");
	}

}
