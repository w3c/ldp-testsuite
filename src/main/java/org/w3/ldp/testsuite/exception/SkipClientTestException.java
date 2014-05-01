package org.w3.ldp.testsuite.exception;

import org.testng.SkipException;

public class SkipClientTestException extends SkipException {
    private static final long serialVersionUID = 1L;

    public SkipClientTestException() {
        super("Skipping test. This is a client requirement, not a server requirement.");
    }
}
