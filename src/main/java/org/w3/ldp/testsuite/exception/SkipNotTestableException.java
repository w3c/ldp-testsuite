package org.w3.ldp.testsuite.exception;

import org.testng.SkipException;

public class SkipNotTestableException extends SkipException {
	private static final long serialVersionUID = 1L;

	public SkipNotTestableException() {
		super("This requirement or recommendation must be tested manually. It is difficult or impossible to write automated tests for and is not part of the testsuite.");
	}
}
