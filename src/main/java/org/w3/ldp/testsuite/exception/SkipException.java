package org.w3.ldp.testsuite.exception;

import org.w3.ldp.testsuite.test.LdpTest;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Custom SkipException just to log the skipped tests
 */
public class SkipException extends org.testng.SkipException {

	private static final long serialVersionUID = 1L;

	public SkipException(String test, String skipMessage) {
		super(skipMessage);
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(LdpTest.SKIPPED_LOG, true)))) {
			out.println(String.format("[%s] skipped test %s: %s", new Timestamp(new Date().getTime()), test, skipMessage));
		}catch (IOException e) {}
	}

}
