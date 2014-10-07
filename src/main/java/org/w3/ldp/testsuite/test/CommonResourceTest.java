package org.w3.ldp.testsuite.test;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Headers;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.testng.ITestResult;
import org.testng.annotations.*;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.annotations.SpecTest.STATUS;
import org.w3.ldp.testsuite.exception.SkipException;
import org.w3.ldp.testsuite.exception.SkipMethodNotAllowedException;
import org.w3.ldp.testsuite.exception.SkipNotTestableException;
import org.w3.ldp.testsuite.http.HttpMethod;
import org.w3.ldp.testsuite.vocab.LDP;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jayway.restassured.config.LogConfig.logConfig;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.w3.ldp.testsuite.http.HttpHeaders.*;
import static org.w3.ldp.testsuite.http.MediaTypes.TEXT_TURTLE;
import static org.w3.ldp.testsuite.matcher.HeaderMatchers.isValidEntityTag;
import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

/**
 * Common tests for all LDP resources, RDF source and non-RDF source.
 */
public abstract class CommonResourceTest extends LdpTest {

	private Set<String> options = new HashSet<String>();

	protected Map<String,String> auth;

	protected abstract String getResourceUri();

	@BeforeClass(alwaysRun = true)
	public void determineOptions() {
		String uri = getResourceUri();
		if (StringUtils.isNotBlank(uri)) {
			// Use HTTP OPTIONS, which MUST be supported by LDP servers, to determine what methods are supported on this container.
			Response optionsResponse = buildBaseRequestSpecification().options(uri);
			Headers headers = optionsResponse.getHeaders();
			List<Header> allowHeaders = headers.getList(ALLOW);
			for (Header allowHeader : allowHeaders) {
				String allow = allowHeader.getValue();
				if (allow != null) {
					String[] methods = allow.split("\\s*,\\s*");
					for (String method : methods) {
						options.add(method);
					}
				}
			}
		}
	}

	@AfterMethod(alwaysRun = true)
	public void addFailureToHttpLog(ITestResult result) {
		if (httpLog != null && result.getStatus() == ITestResult.FAILURE) {
			// Add the failure details after the HTTP trace so it's clear what test it belongs to.
			httpLog.println(">>> [FAILURE] Test: " + result.getName());
			Throwable thrown = result.getThrowable();
			if (thrown != null) {
				httpLog.append(thrown.getLocalizedMessage());
				httpLog.println();
			}
			httpLog.println();
		}
	}

	@Parameters("auth")
	public CommonResourceTest(@Optional String auth) throws IOException {
		if (StringUtils.isNotBlank(auth) && auth.contains(":")) {
			String[] split = auth.split(":");
			if (split.length == 2 && StringUtils.isNotBlank(split[0]) && StringUtils.isNotBlank(split[1])) {
				this.auth = ImmutableMap.of("username", split[0], "password", split[1]);
			}
		} else {
			this.auth = null;
		}
	}

	@Override
	protected RequestSpecification buildBaseRequestSpecification() {
		RequestSpecification spec = RestAssured.given();
		if (auth != null) {
			spec.auth().preemptive().basic(auth.get("username"), auth.get("password"));
		}

		if (httpLog != null) {
			spec.config(RestAssured
					.config()
					.logConfig(logConfig()
							.enableLoggingOfRequestAndResponseIfValidationFails()
							.defaultStream(new PrintStream(new WriterOutputStream(httpLog)))
							.enablePrettyPrinting(true)));
		}

		return spec;
	}

	@Test(
			groups = {MUST, MANUAL},
			description = "LDP servers MUST at least be"
					+ " HTTP/1.1 conformant servers [HTTP11].")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-http",
			testMethod = METHOD.MANUAL,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testIsHttp11Server covers the rest.")
	public void testIsHttp11Manual() throws URISyntaxException {
		throw new SkipNotTestableException(Thread.currentThread().getStackTrace()[1].getMethodName(), skipLog);
	}

	@Test(
			groups = {MUST},
			description = "LDP server responses MUST use entity tags "
					+ "(either weak or strong ones) as response "
					+ "ETag header values.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-etags",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testETagHeadersHead covers the rest.")
	public void testETagHeadersGet() {
		// GET requests
		buildBaseRequestSpecification()
			.expect()
				.statusCode(isSuccessful())
				.header(ETAG, isValidEntityTag())
			.when()
				.get(getResourceUri());
	}

	@Test(
			groups = {MUST},
			description = "LDP server responses MUST use entity tags "
					+ "(either weak or strong ones) as response "
					+ "ETag header values.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-etags",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testETagHeadersGet covers the rest.")
	public void testETagHeadersHead() {
		// GET requests
		buildBaseRequestSpecification()
				.expect().statusCode(isSuccessful()).header(ETAG, isValidEntityTag())
				.when().head(getResourceUri());
	}

	@Test(
			groups = {MUST},
			description = "LDP servers exposing LDPRs MUST advertise "
					+ "their LDP support by exposing a HTTP Link header "
					+ "with a target URI of http://www.w3.org/ns/ldp#Resource, "
					+ "and a link relation type of type (that is, rel='type') "
					+ "in all responses to requests made to the LDPR's "
					+ "HTTP Request-URI.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-linktypehdr",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testLdpLinkHeader() {
		final String uri = getResourceUri();
		Response response = buildBaseRequestSpecification()
				.when()
					.get(getResourceUri());
		assertTrue(
				containsLinkHeader(
						uri,
						LINK_REL_TYPE,
						LDP.Resource.stringValue(),
						uri,
						response
				),
				"4.2.1.4 LDP servers exposing LDPRs must advertise their LDP support by exposing a HTTP Link header "
						+ "with a target URI of http://www.w3.org/ns/ldp#Resource, and a link relation type of type (that is, "
						+ "rel='type') in all responses to requests made to the LDPR's HTTP Request-URI. Actual: "
						+ response.getHeader(LINK)
		);
	}

	@Test(
			groups = {MUST},
			description = "LDP servers MUST support the HTTP GET Method for LDPRs")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-get-must",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testGetResource() {
		assertTrue(supports(HttpMethod.GET), "HTTP GET is not listed in the Allow response header on HTTP OPTIONS requests for resource <" + getResourceUri() + ">");
		buildBaseRequestSpecification()
				.expect().statusCode(isSuccessful())
				.when().get(getResourceUri());
	}

	@Test(
			groups = {MUST},
			description = "LDP servers MUST support the HTTP response headers "
					+ "defined in section 4.2.8 HTTP OPTIONS. ")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-get-options",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testGetResponseHeaders() {
		ResponseSpecification expectResponse = buildBaseRequestSpecification().expect();
		expectResponse.header(ALLOW, notNullValue());

		// Some headers are expected depending on OPTIONS
		if (supports(HttpMethod.PATCH)) {
			expectResponse.header(ACCEPT_PATCH, notNullValue());
		}

		if (supports(HttpMethod.POST)) {
			expectResponse.header(ACCEPT_POST, notNullValue());
		}

		expectResponse.when().get(getResourceUri());
	}




	@Test(
			groups = {SHOULD},
			description = "LDP clients SHOULD use the HTTP If-Match header and HTTP ETags "
					+ "to ensure it isn’t modifying a resource that has changed since the "
					+ "client last retrieved its representation. LDP servers SHOULD require "
					+ "the HTTP If-Match header and HTTP ETags to detect collisions.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-put-precond",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testConditionFailedStatusCode, testPreconditionRequiredStatusCode "
					+ "and testPutBadETag covers the rest.")
	public void testPutRequiresIfMatch() throws URISyntaxException {
		skipIfMethodNotAllowed(HttpMethod.PUT);

		String resourceUri = getResourceUri();
		Response response = buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
			.expect()
				.statusCode(isSuccessful())
				.header(ETAG, isValidEntityTag())
			.when()
				.get(resourceUri);

		buildBaseRequestSpecification()
				.contentType(response.getContentType())
				.body(response.asByteArray())
			.expect()
				.statusCode(not(isSuccessful()))
			.when()
				.put(resourceUri);
	}

	@Test(
			groups = {MUST},
			description = "LDP servers MUST respond with status code 412 "
					+ "(Condition Failed) if ETags fail to match when there "
					+ "are no other errors with the request [HTTP11]. LDP "
					+ "servers that require conditional requests MUST respond "
					+ "with status code 428 (Precondition Required) when the "
					+ "absence of a precondition is the only reason for rejecting "
					+ "the request [RFC6585].")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-put-precond",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testPutBadETag, testPreconditionRequiredStatusCode "
					+ "and testPutRequiresIfMatch covers the rest.")
	public void testConditionFailedStatusCode() {
		skipIfMethodNotAllowed(HttpMethod.PUT);

		String resourceUri = getResourceUri();
		Response response = buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
				.expect()
					.statusCode(isSuccessful()).header(ETAG, isValidEntityTag())
				.when()
					.get(resourceUri);
		String contentType = response.getContentType();

		buildBaseRequestSpecification()
					.contentType(contentType)
					.header(IF_MATCH, "\"These aren't the ETags you're looking for.\"")
					.body(response.asByteArray())
				.expect()
					.statusCode(HttpStatus.SC_PRECONDITION_FAILED)
				.when()
					.put(resourceUri);
	}

	@Test(
			groups = {MUST},
			description = "LDP servers MUST respond with status code 412 "
					+ "(Condition Failed) if ETags fail to match when there "
					+ "are no other errors with the request [HTTP11]. LDP "
					+ "servers that require conditional requests MUST respond "
					+ "with status code 428 (Precondition Required) when the "
					+ "absence of a precondition is the only reason for rejecting "
					+ "the request [RFC6585].")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-put-precond",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testConditionFailedStatusCode,  testPutBadETag"
					+ "and testPutRequiresIfMatch covers the rest.")
	public void testPreconditionRequiredStatusCode() {
		skipIfMethodNotAllowed(HttpMethod.PUT);

		String resourceUri = getResourceUri();
		Response getResponse = buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
			.expect()
				.statusCode(isSuccessful())
				.header(ETAG, isValidEntityTag())
			.when()
				.get(resourceUri);

		// Verify that we can successfully PUT the resource WITH an If-Match header.
		Response ifMatchResponse = buildBaseRequestSpecification()
					.header(IF_MATCH, getResponse.getHeader(ETAG))
					.contentType(getResponse.contentType())
					.body(getResponse.asByteArray())
				.when()
					.put(resourceUri);
		if (!isSuccessful().matches(ifMatchResponse.getStatusCode())) {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					"Skipping test because PUT request failed with valid If-Match header.",
					skipLog);
		}

		// Now try WITHOUT the If-Match header. If the result is NOT successful,
		// it should be because the header is missing and we can check the error
		// code.
		Response noIfMatchResponse = buildBaseRequestSpecification()
					.contentType(getResponse.contentType())
					.body(getResponse.asByteArray())
				.when()
					.put(resourceUri);
		if (isSuccessful().matches(noIfMatchResponse.getStatusCode())) {
			// It worked. This server doesn't require If-Match, which is only a
			// SHOULD requirement (see testPutRequiresIfMatch). Skip the test.
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					"Server does not require If-Match header.", skipLog);
		}

		assertEquals(428, noIfMatchResponse.getStatusCode(), "Expected 428 Precondition Required error on PUT request with no If-Match header");
	}

	@Test(
			groups = {MUST},
			description = "LDP servers MUST respond with status code 412 "
					+ "(Condition Failed) if ETags fail to match when there "
					+ "are no other errors with the request [HTTP11]. LDP "
					+ "servers that require conditional requests MUST respond "
					+ "with status code 428 (Precondition Required) when the "
					+ "absence of a precondition is the only reason for rejecting "
					+ "the request [RFC6585].")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-put-precond",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testConditionFailedStatusCode, testPreconditionRequiredStatusCode "
					+ "and testPutRequiresIfMatch covers the rest.")
	public void testPutBadETag() {
		skipIfMethodNotAllowed(HttpMethod.PUT);

		String resourceUri = getResourceUri();
		Response response = buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
			.expect()
				.statusCode(isSuccessful()).header(ETAG, isValidEntityTag())
			.when()
				.get(resourceUri);

		buildBaseRequestSpecification()
				.contentType(response.getContentType())
				.header(IF_MATCH, "\"This is not the ETag you're looking for\"") // bad ETag value
				.body(response.asByteArray())
			.expect()
				.statusCode(HttpStatus.SC_PRECONDITION_FAILED)
			.when()
				.put(resourceUri);
	}

	@Test(
			groups = {MUST},
			description = "LDP servers MUST support the HTTP HEAD method. ")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-head-must",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testHead() {
		assertTrue(supports(HttpMethod.HEAD), "HTTP HEAD is not listed in the Allow response header on HTTP OPTIONS requests for resource <" + getResourceUri() + ">");
		buildBaseRequestSpecification().expect().statusCode(isSuccessful()).when().head(getResourceUri());
	}

	@Test(
			groups = {MUST},
			description = "LDP servers that support PATCH MUST include an "
					+ "Accept-Patch HTTP response header [RFC5789] on HTTP "
					+ "OPTIONS requests, listing patch document media type(s) "
					+ "supported by the server. ")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-patch-acceptpatch",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testAcceptPatchHeader() {
		if (supports(HttpMethod.PATCH)) {
			buildBaseRequestSpecification()
				.expect().statusCode(isSuccessful()).header(ACCEPT_PATCH, notNullValue())
				.when().options(getResourceUri());
		}
	}

	@Test(
			groups = {MUST},
			description = "LDP servers MUST support the HTTP OPTIONS method. ")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-options-must",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testOptions() {
		buildBaseRequestSpecification().expect().statusCode(isSuccessful()).when().options(getResourceUri());
	}

	@Test(
			groups = {MUST},
			description = "LDP servers MUST indicate their support for HTTP Methods "
					+ "by responding to a HTTP OPTIONS request on the LDPR’s URL "
					+ "with the HTTP Method tokens in the HTTP response header Allow. ")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-options-allow",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testOptionsAllowHeader() {
		buildBaseRequestSpecification().expect().statusCode(isSuccessful()).header(ALLOW, notNullValue())
				.when().options(getResourceUri());
	}

	protected boolean supports(HttpMethod method) {
		return options.contains(method.getName());
	}

	protected void skipIfMethodNotAllowed(HttpMethod method) {
		if (!supports(method)) {
			throw new SkipMethodNotAllowedException(Thread.currentThread().getStackTrace()[1].getMethodName(), getResourceUri(), method, skipLog);
		}
	}
}
