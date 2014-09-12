package org.w3.ldp.testsuite.exception;

import org.w3.ldp.testsuite.http.HttpMethod;

import java.io.PrintWriter;

public class SkipMethodNotAllowedException extends SkipException {
	private static final long serialVersionUID = 1L;

	public SkipMethodNotAllowedException(String test, HttpMethod method, PrintWriter skipLog) {
		super(test, "Skipping test since method " + method.getName() +
				" is not allowed. This HTTP method is needed for this test.", skipLog);
	}

	public SkipMethodNotAllowedException(String test, String uri, HttpMethod method, PrintWriter skipLog) {
		super(test, "Skipping test since <" + uri + "> has not advertised " + method.getName() +
				" support through its HTTP OPTIONS response. This HTTP method is needed for this test.", skipLog);
	}

}
