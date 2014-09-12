package org.w3.ldp.testsuite.exception;

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
		this(test, skipMessage, null);
	}

	public SkipException(String test, String skipMessage, PrintWriter skipLog) {
		super(skipMessage);
		if (skipLog != null) {
			skipLog.println(String.format("[%s] skipped test %s: %s", df.format(new Date()), test, skipMessage));
		}
	}

}
