package org.w3.ldp.testsuite.reporter;

import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

public class LdpTestListener extends TestListenerAdapter {

    private static final String FAIL = "Method Failed ";
    private static final String SKIP = "Method Skipped";
    private static final String PASSED = "Method Passed";

    @Override
    public void onTestFailure(ITestResult tr) {
        log(tr.getName(), FAIL, (tr.getEndMillis() - tr.getStartMillis())
                + " Msec");
    }

    @Override
    public void onTestSkipped(ITestResult tr) {
        log(tr.getName(), SKIP, (tr.getEndMillis() - tr.getStartMillis())
                + " Msec");
    }

    @Override
    public void onTestSuccess(ITestResult tr) {
        log(tr.getName(), PASSED, (tr.getEndMillis() - tr.getStartMillis())
                + " Msec");
    }

    private void log(String string, String status, String time) {
        System.out.printf("%-55s %-15s %15s %n", string, status, time);
    }

}
