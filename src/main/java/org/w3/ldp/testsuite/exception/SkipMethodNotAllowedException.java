package org.w3.ldp.testsuite.exception;

import org.testng.SkipException;
import org.w3.ldp.testsuite.http.HttpMethod;

public class SkipMethodNotAllowedException extends SkipException {
    private static final long serialVersionUID = 1L;

    public SkipMethodNotAllowedException(HttpMethod method) {
        super("Skipping test since method " + method.getName() +
                " is not allowed. This HTTP method is needed for this test.");
    }

    public SkipMethodNotAllowedException(String uri, HttpMethod method) {
        super("Skipping test since <" + uri + "> has not advertised " + method.getName() +
                " support through its HTTP OPTIONS response. This HTTP method is needed for this test.");
    }
}
