package org.w3.ldp.testsuite.reporter;

import java.lang.reflect.Method;

public class ReportUtils {
	public static final String JAVADOC_BASE_URI = "http://w3c.github.io/ldp-testsuite/api/java/";

	public static String getJavadocLink(Method method) {
		final String path =
				method.getDeclaringClass().getCanonicalName().replace(".", "/") + ".html";
		final String hash = "#" + method.getName() + "()";
		return JAVADOC_BASE_URI + path + hash;
	}
}
