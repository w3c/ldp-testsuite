package org.w3.ldp.paging.testsuite.tests;

import java.io.IOException;

import org.w3.ldp.testsuite.reporter.LdpTestCaseReporter;

public class TestCaseReporter extends LdpTestCaseReporter {

	public static void main(String[] args) throws IOException{
		@SuppressWarnings("rawtypes")
		Class[] classes = {PagingTest.class};
		LdpTestCaseReporter reporter = new LdpTestCaseReporter(classes);
		reporter.generateReport("paging");
	}

}
