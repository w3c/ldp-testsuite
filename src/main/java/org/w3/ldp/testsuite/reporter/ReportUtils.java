package org.w3.ldp.testsuite.reporter;

import java.lang.reflect.Method;

public class ReportUtils {
	public static final String JAVADOC_BASE_URI = "http://w3c.github.io/ldp-testsuite/api/java/";

	/**
	 * Generates a link to the Javadoc hosted on w3c.github.io for the
	 * corresponding test method, which in turn link back to the source. This
	 * can be added to HTML and EARL test reports.
	 *
	 * @param method
	 *            the test method
	 * @return a link to the Javadoc.
	 * @see <a href="http://w3c.github.io/ldp-testsuite/api/java/">Hosted Javadoc</a>
	 */
	public static String getJavadocLink(final Method method) {
		// Example link:
		//   http://w3c.github.io/ldp-testsuite/api/java/org/w3/ldp/testsuite/test/CommonContainerTest.html#testRequestedInteractionModelCreateNotAllowed(java.lang.String)
		final StringBuilder link = new StringBuilder();
		link.append(JAVADOC_BASE_URI);
		link.append(method.getDeclaringClass().getCanonicalName().replace(".", "/"));
		link.append(".html#");
		link.append(method.getName());
		link.append("(");
		boolean first = true;
		for (Class<?> paramType : method.getParameterTypes()) {
			if (!first) {
				link.append(", ");
			}

			link.append(paramType.getCanonicalName());
			first = false;
		}
		link.append(")");

		return link.toString();
	}
}
