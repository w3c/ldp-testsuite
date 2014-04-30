package org.w3.ldp.testsuite.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.testng.Assert.assertTrue;
import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.HttpMethod;
import org.w3.ldp.testsuite.annotations.Reference;
import org.w3.ldp.testsuite.exception.SkipMethodNotAllowedException;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.ResponseSpecification;

/**
 * Common tests for all LDP resources, RDF source and non-RDF source.
 */
public abstract class CommonResourceTest extends LdpTest {
	
	private HashSet<String> options = new HashSet<String>();

	protected abstract String getResourceUri();
	
	@BeforeClass(alwaysRun = true)
    public void determineOptions() throws URISyntaxException {
    	String uri = getResourceUri();
    
    	if (uri != null) {
    		// Use HTTP OPTIONS, which MUST be supported by LDP servers, to determine what methods are supported on this container.
    		Response optionsResponse = RestAssured.options(new URI(uri));
    		String allow = optionsResponse.header(ALLOW);
    		if (allow != null) {
    			String[] methods = allow.split("\\s*,\\s*");
    			for (String method : methods) {
    				options.add(method);
    			}
    		}
    	}
    }

	@Test(
			groups = {MUST}, 
			description = "LDP servers MUST at least be"
							+ " HTTP/1.1 conformant servers [HTTP11].")
	@Reference(uri = SPEC_URI + "#ldpr-gen-http")
	public void testIsHttp11Server() throws URISyntaxException{
		RestAssured.expect().statusLine(containsString("HTTP/1.1")).when().head(new URI(getResourceUri()));
	}
	
	@Test(
			enabled = false,
			groups = {MAY}, 
			description = "LDP servers MAY host a mixture of LDPRs, "
							+ "LDP-RSs and LDP-NRs. For example, it is common "
							+ "for LDP servers to need to host binary or text "
							+ "resources that do not have useful RDF representations.")
	@Reference(uri = SPEC_URI + "#ldpr-gen-binary")
	public void testOtherMediaTypes(){
		
	}
	
	@Test(
			groups = {MUST},
			description = "LDP server responses MUST use entity tags "
							+ "(either weak or strong ones) as response "
							+ "ETag header values.")
	@Reference(uri = SPEC_URI + "#ldpr-gen-etags")
	public void testETagHeadersGet() throws URISyntaxException {
		// GET requests
		RestAssured.given().header(ACCEPT, TEXT_TURTLE)
				.expect().statusCode(isSuccessful()).header(ETAG, notNullValue())
				.when().get(new URI(getResourceUri()));
	}

	@Test(
			groups = {MUST},
			description = "LDP server responses MUST use entity tags "
							+ "(either weak or strong ones) as response "
							+ "ETag header values.")
	@Reference(uri = SPEC_URI + "#ldpr-gen-etags")
	public void testETagHeadersHead() throws URISyntaxException {
		// GET requests
		RestAssured.given().header(ACCEPT, TEXT_TURTLE)
				.expect().statusCode(isSuccessful()).header(ETAG, notNullValue())
				.when().head(new URI(getResourceUri()));
	}

	@Test(
			description = "LDP servers exposing LDPRs MUST advertise "
							+ "their LDP support by exposing a HTTP Link header "
							+ "with a target URI of http://www.w3.org/ns/ldp#Resource, "
							+ "and a link relation type of type (that is, rel='type') "
							+ "in all responses to requests made to the LDPR's "
							+ "HTTP Request-URI.")
	@Reference(uri = SPEC_URI + "#ldpr-gen-linktypehdr")
	public void testLdpLinkHeader() throws URISyntaxException{
		Response response = RestAssured.given().header(ACCEPT, TEXT_TURTLE)
				.expect().statusCode(isSuccessful())
				.when().get(new URI(getResourceUri()));
		assertTrue(
				hasLinkHeader(response, TYPE_RESOURCE, LINK_REL_TYPE),
				"4.2.1.4 LDP servers exposing LDPRs must advertise their LDP support by exposing a HTTP Link header "
						+ "with a target URI of http://www.w3.org/ns/ldp#Resource, and a link relation type of type (that is, "
						+ "rel='type') in all responses to requests made to the LDPR's HTTP Request-URI. Actual: "
						+ response.getHeader(LINK));
	}
	
	@Test(
			enabled = false,
			groups = {MUST},
			description = "LDP servers MUST assign the default base-URI "
							+ "for [RFC3987] relative-URI resolution to be the HTTP "
							+ "Request-URI when the resource already exists, and to "
							+ "the URI of the created resource when the request results "
							+ "in the creation of a new resource.")
	@Reference(uri = SPEC_URI + "#ldpr-gen-defbaseuri")
	public void testRelativeUriResolutionPut(){
		// ...
	}
	
	@Test(
			enabled = false,
			groups = {MUST},
			description = "LDP servers MUST publish any constraints on LDP clients’ "
							+ "ability to create or update LDPRs, by adding a Link header "
							+ "with rel='describedby' [RFC5988] to all responses to requests "
							+ "which fail due to violation of those constraints.")
	@Reference(uri = SPEC_URI + "#ldpr-gen-pubclireqs")
	public void testPublishConstraints(){
		
	}
	
	@Test(
			// enabled = false,
			groups = {MUST}, 
			description = "LDP servers MUST support the HTTP GET Method for LDPRs")
	@Reference(uri = SPEC_URI + "#ldpr-get-must")
	public void testGetResource() throws URISyntaxException {
		assertTrue(supports(HttpMethod.GET), "HTTP GET is not listed in the Allow response header on HTTP OPTIONS requests for resource <" + getResourceUri() + ">");
		RestAssured
		    .expect().statusCode(isSuccessful())
		    .when().get(new URI(getResourceUri()));
	}
	
	@Test(
			groups = {MUST},
			description = "LDP servers MUST support the HTTP response headers "
							+ "defined in section 4.2.8 HTTP OPTIONS. ")
	@Reference(uri = SPEC_URI + "#ldpr-get-options")
	public void testGetResponseHeaders() throws URISyntaxException{
		ResponseSpecification expectResponse = RestAssured.expect();
		expectResponse.header(ALLOW, notNullValue());
		
		// Some headers are expected depending on OPTIONS
		if (supports(HttpMethod.PATCH)) {
			expectResponse.header(ACCEPT_PATCH, notNullValue());
		}

		if (supports(HttpMethod.POST)){
			expectResponse.header(ACCEPT_POST, notNullValue());
		}

		expectResponse.when().get(new URI(getResourceUri()));
	}
	
	/** Use of Http PUT method is optional */
	@Test(
			enabled = false,
			groups = {MUST}, 
			description = "If a HTTP PUT is accepted on an existing resource, "
							+ "LDP servers MUST replace the entire persistent state of "
							+ "the identified resource with the entity representation "
							+ "in the body of the request.")
	@Reference(uri = SPEC_URI + "#ldpr-put-replaceall")
	public void testReplaceExisitngResource(){
		
	}
	
	@Test(
			enabled = false,
			groups = {SHOULD}, 
			description = "LDP servers SHOULD allow clients to update resources "
							+ "without requiring detailed knowledge of server-specific "
							+ "constraints. This is a consequence of the requirement to "
							+ "enable simple creation and modification of LDPRs.")
	@Reference(uri = SPEC_URI + "#ldpr-put-simpleupdate")
	public void testAllowUpdateResources(){
		
	}
	
	@Test(
			enabled = false,
			groups = {MUST},
			description = "If an otherwise valid HTTP PUT request is received that "
							+ "attempts to change properties the server does not allow "
							+ "clients to modify, LDP servers MUST respond with a 4xx range "
							+ "status code (typically 409 Conflict)")
	@Reference(uri = SPEC_URI + "#ldprs-put-servermanagedprops")
	public void testPutReadOnlyProperties4xxStatus(){
		
	}
	
	@Test( 
			enabled = false,
			groups = {SHOULD}, 
			dependsOnMethods = {"testInvalidPutPropertiesNotAllowed"}, 
			description = "LDP servers SHOULD provide a corresponding response body containing "
							+ "information about which properties could not be persisted. The "
							+ "format of the 4xx response body is not constrained by LDP.")
	@Reference(uri = SPEC_URI + "#ldprs-put-servermanagedprops")
	public void test4xxErrorHasResponseBody(){
		
	}
	
	@Test(
			enabled = false,
			groups = {MUST},
			description = "If an otherwise valid HTTP PUT request is received that "
							+ "contains properties the server chooses not to persist, "
							+ "e.g. unknown content, LDP servers MUST respond with an "
							+ "appropriate 4xx range status code [HTTP11].")
	@Reference(uri = SPEC_URI + "#ldprs-put-failed")
	public void testPutPropertiesNotPersisted(){
		
	}
	
	@Test( 
			enabled = false,
			groups = {SHOULD}, 
			dependsOnMethods = {"testInvalidPutPropertiesNotPersisted"}, 
			description = "LDP servers SHOULD provide a corresponding response body containing "
							+ "information about which properties could not be persisted. The "
							+ "format of the 4xx response body is not constrained by LDP. LDP "
							+ "servers expose these application-specific constraints as described "
							+ "in section 4.2.1 General.")
	@Reference(uri = SPEC_URI + "#ldprs-put-failed")
	public void testResponsePropertiesNotPersisted(){
		
	}
	
	@Test(
			enabled = false,
			groups = {SHOULD}, 
			description = "LDP clients SHOULD use the HTTP If-Match header and HTTP ETags "
							+ "to ensure it isn’t modifying a resource that has changed since the "
							+ "client last retrieved its representation. LDP servers SHOULD require "
							+ "the HTTP If-Match header and HTTP ETags to detect collisions.")
	@Reference(uri = SPEC_URI + "#ldpr-put-precond")
	public void testUseHttpIfMatchHeaderAndETags(){
		
	}
	
	@Test( 
			enabled = false,
			groups = {MUST}, 
			dependsOnMethods = {"testUseHttpIfMatchHeaderAndETags"}, 
			description = "LDP servers MUST respond with status code 412 "
							+ "(Condition Failed) if ETags fail to match when there "
							+ "are no other errors with the request [HTTP11]. LDP "
							+ "servers that require conditional requests MUST respond "
							+ "with status code 428 (Precondition Required) when the "
							+ "absence of a precondition is the only reason for rejecting "
							+ "the request [RFC6585].")
	@Reference(uri = SPEC_URI + "#ldpr-put-precond")
	public void testResponseOnFailedRequests(){
		
	}
	
	/** Use of Http PUT method is optional */
	@Test(
			enabled = false,
			groups = {MAY}, 
			description = "LDP servers MAY choose to allow the creation of new "
							+ "resources using HTTP PUT.")
	@Reference(uri = SPEC_URI + "#ldpr-put-create")
	public void testPut(){
	}
	
	@Test(
			// enabled = false,
			groups = {MUST},
			description = "LDP servers MUST support the HTTP HEAD method. ")
	@Reference(uri = SPEC_URI + "#ldpr-head-must")
	public void testHead() {
		assertTrue(supports(HttpMethod.HEAD), "HTTP HEAD is not listed in the Allow response header on HTTP OPTIONS requests for resource <" + getResourceUri() + ">");
		RestAssured.expect().statusCode(isSuccessful()).when().head(getResourceUri());
	}
	
	@Test(
			groups = {MUST},
			description = "LDP servers that support PATCH MUST include an "
							+ "Accept-Patch HTTP response header [RFC5789] on HTTP "
							+ "OPTIONS requests, listing patch document media type(s) "
							+ "supported by the server. ")
	@Reference(uri = SPEC_URI + "#ldpr-patch-acceptpatch")
	public void testAcceptPatchHeader() throws URISyntaxException{
		skipIfMethodNotAllowed(HttpMethod.PATCH);

		RestAssured
				.expect().statusCode(isSuccessful()).header(ACCEPT_PATCH, notNullValue())
				.when().options(new URI(getResourceUri()));
	}

	@Test(
			// enabled = false,
			groups = {MUST},
			description = "LDP servers MUST support the HTTP OPTIONS method. ")
	@Reference(uri = SPEC_URI + "#ldpr-options-must")
	public void testOptions() {
		RestAssured.expect().statusCode(isSuccessful()).when().options(getResourceUri());
	}
	
	@Test(
			// enabled = false,
			groups = {MUST},
			description = "LDP servers MUST indicate their support for HTTP Methods "
							+ "by responding to a HTTP OPTIONS request on the LDPR’s URL "
							+ "with the HTTP Method tokens in the HTTP response header Allow. ")
	@Reference(uri = SPEC_URI +"ldpr-options-allow")
	public void testOptionsAllowHeader() throws URISyntaxException {
		URI uri = new URI(getResourceUri());
		RestAssured.expect().statusCode(isSuccessful()).header(ALLOW, notNullValue()).when().options(uri);
	}

	protected boolean supports(HttpMethod method) {
		return options.contains(method.getName());
	}

	protected void skipIfMethodNotAllowed(HttpMethod method) {
		if (!supports(method)) {
			throw new SkipMethodNotAllowedException(getResourceUri(), method);
		}
	}
}