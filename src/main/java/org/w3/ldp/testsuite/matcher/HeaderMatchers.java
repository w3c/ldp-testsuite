/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *	   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.w3.ldp.testsuite.matcher;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jboss.resteasy.plugins.delegates.LinkDelegate;
import org.w3.ldp.testsuite.http.MediaTypes;

import javax.ws.rs.core.Link;

/**
 * Matcher collection to work with HttpHeaders.
 */
public class HeaderMatchers {

	private static final Pattern TURTLE_REGEX = Pattern.compile("^" + MediaTypes.TEXT_TURTLE + "\\s*(;|$)");
	/**
	 * Regular expression matching valid ETag values.
	 *
	 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.19">HTTP 1.1: Section 14.19 - ETag</a>
	 */
	public final static String ETAG_REGEX = "^(W/)?\"([^\"]|\\\\\")*\"$";

	public static Matcher<String> headerPresent() {
		return new BaseMatcher<String>() {
			@Override
			public boolean matches(Object item) {
				return item != null && StringUtils.isNotBlank(item.toString());
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("set");
			}
		};
	}

	public static Matcher<String> headerNotPresent() {
		return new BaseMatcher<String>() {
			@Override
			public boolean matches(Object item) {
				return item == null || StringUtils.isBlank(item.toString());
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("absent or empty");
			}
		};
	}

	public static Matcher<String> isLink(String uri, String rel) {
		final Link expected = Link.fromUri(uri).rel(rel).build();
		return new CustomTypeSafeMatcher<String>(String.format("a Link-Header to <%s> with rel='%s'", uri, rel)) {
			@Override
			protected boolean matchesSafely(String item) {
				return expected.equals(new LinkDelegate().fromString(item));
			}
		};
	}


	public static Matcher<String> isValidEntityTag() {
		return new CustomTypeSafeMatcher<String>("a valid EntityTag value as defined in RFC2616 section 14.19 (did you quote the value?)") {
			/**
			 * Checks that the ETag value is present and valid as defined in RFC2616
			 *
			 * @param item
			 *			  the header value
			 * @return true only if the ETag is valid
			 *
			 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.19">HTTP 1.1: Section 14.19 - ETag</a>
			 */
			@Override
			protected boolean matchesSafely(String item) {
				return item.trim().matches(ETAG_REGEX);
			}
		};
	}

	/**
	 * Matcher testing a Content-Type response header's compatibility with
	 * JSON-LD (expects application/ld+json or application/json).
	 *
	 * @return the matcher
	 */
	public static Matcher<String> isJsonLdCompatibleContentType() {
		return new CustomTypeSafeMatcher<String>("application/ld+json or application/json") {
			@Override
			protected boolean matchesSafely(String item) {
				return item.equals(MediaTypes.APPLICATION_LD_JSON) || item.equals(MediaTypes.APPLICATION_JSON);
			}
		};
	}

	/**
	 * Matcher testing a Content-Type response header's compatibility with
	 * Turtle (expects text/turtle).
	 *
	 * @return the matcher
	 */
	public static Matcher<String> isTurtleCompatibleContentType() {
		return new CustomTypeSafeMatcher<String>("text/turtle") {
			@Override
			protected boolean matchesSafely(String item) {
				return MediaTypes.TEXT_TURTLE.equals(item)
						|| TURTLE_REGEX.matcher(item).find();
			}
		};
	}
}
