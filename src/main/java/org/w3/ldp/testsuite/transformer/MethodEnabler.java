package org.w3.ldp.testsuite.transformer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

public class MethodEnabler implements IAnnotationTransformer {

	private static Map<String, Boolean> transforms = new HashMap<>();	
	private static boolean defEnabled = true;

	public synchronized static void includeMethod(String name) {
		transforms.put(name, true);		
	}

	public static void excludeMethod(String name) {
		transforms.put(name, false);
	}
	
	public static void setDefault(boolean enabled) {
		defEnabled = enabled;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void transform(ITestAnnotation annotation, Class testClass,
			Constructor testConstructor, Method testMethod) {
		String methodName = testMethod.getName();
		
		if (transforms.containsKey(methodName)) {
			annotation.setEnabled(transforms.get(methodName));
			
			String[] dependencies = annotation.getDependsOnMethods();
			for (String dep : dependencies) {
				transforms.put(dep, true);
			}
		} else {	
			// If it is not in the transforms map and defEnabled is true, 
			// do what its annotation says
			annotation.setEnabled(annotation.getEnabled() && defEnabled);
		}
	}
}
