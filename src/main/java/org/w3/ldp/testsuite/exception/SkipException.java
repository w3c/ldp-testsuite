package org.w3.ldp.testsuite.exception;

import org.w3.ldp.testsuite.test.LdpTest;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;

/**
 * Custom SkipException, just to log the skipped tests
 */
public class SkipException extends org.testng.SkipException {

	private static final long serialVersionUID = 1L;

	public static final DateFormat df = DateFormat.getDateTimeInstance();

	public SkipException(String test, String skipMessage) {
		this(test, skipMessage, false);
	}

	public SkipException(String test, String skipMessage, boolean skipLog) {
		super(skipMessage);
		System.out.println("skipLog = " + skipLog);
		if (skipLog) {
			try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(LdpTest.SKIPPED_LOG_FILENAME, true)))) {
				out.println(String.format("[%s] skipped test %s: %s", df.format(new Date()), test, skipMessage));
			} catch (IOException e) {
				System.err.println(String.format("Error logging SkipException from %s: %s", test, e.getMessage()));
			}
		}
	}

}
