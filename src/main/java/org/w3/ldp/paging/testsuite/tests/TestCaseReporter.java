package org.w3.ldp.paging.testsuite.tests;

import java.io.IOException;

import org.w3.ldp.testsuite.reporter.LdpTestCaseReporter;

public class TestCaseReporter extends LdpTestCaseReporter {

	public static void main(String[] args) throws IOException, SecurityException{
		@SuppressWarnings("rawtypes")
		Class[] classes = {PagingTest.class};
		LdpTestCaseReporter reporter = new LdpTestCaseReporter(classes, PagingTest.PAGING, PagingTest.SPEC_URI);
		reporter.generateReport("paging");
	}

}
