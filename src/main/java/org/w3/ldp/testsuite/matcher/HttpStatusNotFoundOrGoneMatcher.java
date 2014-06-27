package org.w3.ldp.testsuite.matcher;

import org.apache.http.HttpStatus;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class HttpStatusNotFoundOrGoneMatcher extends TypeSafeMatcher<Integer> {

	@Override
	public void describeTo(Description d) {
		d.appendText("404 Not Found or 410 Gone");
	}

	@Override
	protected boolean matchesSafely(Integer status) {
		return status == HttpStatus.SC_NOT_FOUND || status == HttpStatus.SC_GONE;
	}

	@Factory
	public static Matcher<Integer> isNotFoundOrGone() {
		return new HttpStatusNotFoundOrGoneMatcher();
	}
}
