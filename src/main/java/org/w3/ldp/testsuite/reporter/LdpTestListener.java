package org.w3.ldp.testsuite.reporter;

import java.util.Arrays;

import org.apache.commons.lang3.text.WordUtils;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import org.testng.internal.Utils;

public class LdpTestListener extends TestListenerAdapter {

	private static final String FAIL = "Failed";
	private static final String SKIP = "Skipped";
	private static final String PASSED = "Passed";

	private long startTime;
	private StringBuffer errors;

	@Override
	public void onStart(ITestContext testContext) {
		startTime = System.currentTimeMillis();
		errors = new StringBuffer();
	}

	@Override
	public void onFinish(ITestContext testContext) {
		long now = System.currentTimeMillis();
		double timeInSeconds = (double) (now - startTime) / 1000;
		System.out.printf("%nTotal Time: %.2fs%n", timeInSeconds);
		
		if (errors.length() != 0) {
			System.err.println(errors);
		}
	}

	@Override
	public void onTestFailure(ITestResult tr) {
		log(tr, FAIL);
		printErrorDetails(tr);
	}

	protected void printErrorDetails(ITestResult tr) {
		errors.append("\n[FAILURE] ");
		errors.append(tr.getTestClass().getRealClass().getSimpleName());
		errors.append(".");
		errors.append(tr.getName());
		errors.append("\n");

		String description = tr.getMethod().getDescription();
		if (description != null) {
			errors.append("\n");
			errors.append(WordUtils.wrap(description, 78));
			errors.append("\n");
		}

		Throwable thrown = tr.getThrowable();
		if (thrown != null) {
			errors.append("\n");
			errors.append(Utils.stackTrace(thrown, false)[0]);
			errors.append("\n");
		}
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
				"%-50s %-17s %-8s %-15s %8s%n",
				tr.getName(),
				tr.getTestClass().getRealClass().getSimpleName()
						.replaceAll("Test", ""), status,
				Arrays.toString(tr.getMethod().getGroups()),
				(tr.getEndMillis() - tr.getStartMillis()) + "ms");
	}
}
