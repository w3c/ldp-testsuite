package org.w3.ldp.testsuite.matcher;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class HttpStatusSuccessMatcher extends TypeSafeMatcher<Integer> {

    @Override
    public void describeTo(Description d) {
        d.appendText("successful status code");
    }

    @Override
    protected boolean matchesSafely(Integer status) {
        return status >= 200 && status <= 209;
    }

    @Factory
    public static Matcher<Integer> isSuccessful() {
        return new HttpStatusSuccessMatcher();
    }

}
