package org.w3.ldp.testsuite.exception;

public class SkipClientTestException extends SkipException {

	private static final long serialVersionUID = 1L;

	public SkipClientTestException(String test, String skipMessage, boolean skipLog) {
		super(test, "Skipping test. This is a client requirement, not a server requirement.", skipLog);
	}

}
