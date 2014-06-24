package org.w3.ldp.testsuite.reporter;

import java.util.Arrays;

import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

public class LdpTestListener extends TestListenerAdapter {

	private static final String FAIL = "Failed";
	private static final String SKIP = "Skipped";
	private static final String PASSED = "Passed";

	@Override
	public void onTestFailure(ITestResult tr) {
		log(tr, FAIL);
	}

	@Override
	public void onTestSkipped(ITestResult tr) {
		log(tr, SKIP);
	}

	@Override
	public void onTestSuccess(ITestResult tr) {
		log(tr, PASSED);
	}

	private void log(ITestResult tr, String status) {
		System.out.printf(
				"%-45s %-17s %-8s %-15s %8s%n",
				tr.getName(),
				tr.getTestClass().getRealClass().getSimpleName()
						.replaceAll("Test", ""), status,
				Arrays.toString(tr.getMethod().getGroups()),
				(tr.getEndMillis() - tr.getStartMillis()) + "ms");
	}
}
