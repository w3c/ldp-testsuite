package org.w3.ldp.testsuite.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.testng.Assert.assertTrue;
import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;

import org.apache.http.HttpStatus;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.exception.SkipMethodNotAllowedException;
import org.w3.ldp.testsuite.http.HttpMethod;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;
import org.w3.ldp.testsuite.vocab.LDP;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.DCTerms;
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
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-http", testMethod = METHOD.AUTOMATED)
    public void testIsHttp11Server() throws URISyntaxException {
        RestAssured.expect().statusLine(containsString("HTTP/1.1")).when().head(new URI(getResourceUri()));
    }

    @Test(
            enabled = false,
            groups = {MAY},
            description = "LDP servers MAY host a mixture of LDPRs, "
                    + "LDP-RSs and LDP-NRs. For example, it is common "
                    + "for LDP servers to need to host binary or text "
                    + "resources that do not have useful RDF representations.")
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-binary", testMethod = METHOD.NOT_IMPLEMENTED)
    public void testOtherMediaTypes() {

    }

    @Test(
            groups = {MUST},
            description = "LDP server responses MUST use entity tags "
                    + "(either weak or strong ones) as response "
                    + "ETag header values.")
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-etags", testMethod = METHOD.AUTOMATED)
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
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-etags", testMethod = METHOD.AUTOMATED)
    public void testETagHeadersHead() throws URISyntaxException {
        // GET requests
        RestAssured.given().header(ACCEPT, TEXT_TURTLE)
                .expect().statusCode(isSuccessful()).header(ETAG, notNullValue())
                .when().head(new URI(getResourceUri()));
    }

    @Test(
    		groups = {MUST},
            description = "LDP servers exposing LDPRs MUST advertise "
                    + "their LDP support by exposing a HTTP Link header "
                    + "with a target URI of http://www.w3.org/ns/ldp#Resource, "
                    + "and a link relation type of type (that is, rel='type') "
                    + "in all responses to requests made to the LDPR's "
                    + "HTTP Request-URI.")
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-linktypehdr", testMethod = METHOD.AUTOMATED)
    public void testLdpLinkHeader() throws URISyntaxException {
        Response response = RestAssured.given().header(ACCEPT, TEXT_TURTLE)
                .expect().statusCode(isSuccessful())
                .when().get(new URI(getResourceUri()));
        assertTrue(
                hasLinkHeader(response, LDP.Resource.stringValue(), LINK_REL_TYPE),
                "4.2.1.4 LDP servers exposing LDPRs must advertise their LDP support by exposing a HTTP Link header "
                        + "with a target URI of http://www.w3.org/ns/ldp#Resource, and a link relation type of type (that is, "
                        + "rel='type') in all responses to requests made to the LDPR's HTTP Request-URI. Actual: "
                        + response.getHeader(LINK)
        );
    }

    @Test(
            groups = {MUST},
            description = "LDP servers MUST assign the default base-URI "
                    + "for [RFC3987] relative-URI resolution to be the HTTP "
                    + "Request-URI when the resource already exists, and to "
                    + "the URI of the created resource when the request results "
                    + "in the creation of a new resource.")
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-defbaseuri", testMethod = METHOD.AUTOMATED)
    public void testRelativeUriResolutionPut() throws URISyntaxException {
    	skipIfMethodNotAllowed(HttpMethod.PUT);

    	String resourceUri = getResourceUri();
    	Response response = RestAssured.given().header(ACCEPT, TEXT_TURTLE)
    			.expect().statusCode(isSuccessful()).header(ETAG, notNullValue())
    			.when().get(new URI(resourceUri));
    	
    	String eTag = response.getHeader(ETAG);
    	Model model = response.as(Model.class, new RdfObjectMapper("")); // relative URI

    	// Update a property
    	updateResource(model.getResource(""));
    	
    	// Put the resource back using relative URIs.
    	RestAssured
                .given().contentType(TEXT_TURTLE).header(IF_MATCH, eTag)
                	.body(model, new RdfObjectMapper("")) // relative URI
                .expect().statusCode(isSuccessful())
                .when().put(new URI(resourceUri));
    	
    	// Get the resource again to verify its content.
    	model = RestAssured.given().header(ACCEPT, TEXT_TURTLE)
    			.expect().statusCode(isSuccessful())
    			.when().get(new URI(resourceUri)).as(Model.class, new RdfObjectMapper(resourceUri));
    	
    	// Verify the change.
    	verifyUpdatedResource(model.getResource(resourceUri));
    }

    @Test(
            enabled = false,
            groups = {MUST},
            description = "LDP servers MUST publish any constraints on LDP clients’ "
                    + "ability to create or update LDPRs, by adding a Link header "
                    + "with rel='describedby' [RFC5988] to all responses to requests "
                    + "which fail due to violation of those constraints.")
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-pubclireqs", testMethod = METHOD.NOT_IMPLEMENTED)
    public void testPublishConstraints() {

    }

    @Test(
            groups = {MUST},
            description = "LDP servers MUST support the HTTP GET Method for LDPRs")
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-get-must", testMethod = METHOD.AUTOMATED)
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
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-get-options", testMethod = METHOD.AUTOMATED)
    public void testGetResponseHeaders() throws URISyntaxException {
        ResponseSpecification expectResponse = RestAssured.expect();
        expectResponse.header(ALLOW, notNullValue());

        // Some headers are expected depending on OPTIONS
        if (supports(HttpMethod.PATCH)) {
            expectResponse.header(ACCEPT_PATCH, notNullValue());
        }

        if (supports(HttpMethod.POST)) {
            expectResponse.header(ACCEPT_POST, notNullValue());
        }

        expectResponse.when().get(new URI(getResourceUri()));
    }

    @Test(
            groups = {MUST},
            description = "If a HTTP PUT is accepted on an existing resource, "
                    + "LDP servers MUST replace the entire persistent state of "
                    + "the identified resource with the entity representation "
                    + "in the body of the request.")
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-put-replaceall", testMethod = METHOD.AUTOMATED)
    public void testPutReplacesResource() throws URISyntaxException {
    	skipIfMethodNotAllowed(HttpMethod.PUT);
    	
    	if (restrictionsOnContent()) {
    		throw new SkipException("Skipping test because there are restrictions on PUT content for this resource");
    	}
    	
    	// TODO: Is there a better way to test this requirement?
    	String resourceUri = getResourceUri();
    	Response response = RestAssured.given().header(ACCEPT, TEXT_TURTLE)
    			.expect().statusCode(isSuccessful()).header(ETAG, notNullValue())
    			.when().get(new URI(resourceUri));
    	
    	String eTag = response.getHeader(ETAG);
    	Model originalModel = response.as(Model.class, new RdfObjectMapper(resourceUri));

    	// Replace the resource with something different.
    	Model differentContent = ModelFactory.createDefaultModel();
    	final String UPDATED_TITLE = "This resources content has been replaced";
		differentContent.add(differentContent.getResource(resourceUri),
		        DCTerms.title, UPDATED_TITLE);
    	RestAssured
                .given().contentType(TEXT_TURTLE).header(IF_MATCH, eTag)
                	.body(differentContent, new RdfObjectMapper(resourceUri)) // relative URI
                .expect().statusCode(isSuccessful())
                .when().put(new URI(resourceUri));
    	
    	// Get the resource again to see what's there.
    	response = RestAssured.given().header(ACCEPT, TEXT_TURTLE)
    			.expect().statusCode(isSuccessful()).header(ETAG, notNullValue())
    			.when().get(new URI(resourceUri));
    	eTag = response.getHeader(ETAG);
    	Model updatedModel = response.as(Model.class, new RdfObjectMapper(resourceUri));
    	
    	// Validate the updated resource content. The LDP server is allowed to add in
    	// some triples (for instance, dcterms:lastModified), so we can't just compare
    	// that it's exactly what we posted. Let's make sure not all of the triples
    	// from the original resource are there since we've completely replaced it,
    	// however. Also check that the title is as expected.
    	Resource updatedResource = updatedModel.getResource(resourceUri);
    	assertTrue(updatedResource.hasProperty(DCTerms.title, UPDATED_TITLE), "Expected updated resource to have title: " + UPDATED_TITLE);
    	boolean hasDifferentProperties = false;
    	Resource originalResource = originalModel.getResource(resourceUri);
    	StmtIterator iter = originalResource.listProperties();
    	while (iter.hasNext()) {
    		Statement s = iter.next();
    		if (!updatedResource.hasProperty(s.getPredicate())) {
    			hasDifferentProperties = true;
    		}
    	}
    	assertTrue(hasDifferentProperties, "The updated resource has the same properties as the original. Was it really replaced?");
    	
    	// Replace the resource with its original content to clean up.
    	RestAssured
                .given().contentType(TEXT_TURTLE).header(IF_MATCH, eTag)
                	.body(originalModel, new RdfObjectMapper(resourceUri)) // relative URI
                .expect().statusCode(isSuccessful())
                .when().put(new URI(resourceUri));
    }

    @Test(
            groups = {SHOULD},
            description = "LDP servers SHOULD allow clients to update resources "
                    + "without requiring detailed knowledge of server-specific "
                    + "constraints. This is a consequence of the requirement to "
                    + "enable simple creation and modification of LDPRs.")
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-put-simpleupdate", testMethod = METHOD.AUTOMATED)
    public void testAllowUpdateResources() throws URISyntaxException {
    	skipIfMethodNotAllowed(HttpMethod.PUT);

    	String resourceUri = getResourceUri();
    	Response response = RestAssured.given().header(ACCEPT, TEXT_TURTLE)
    			.expect().statusCode(isSuccessful()).header(ETAG, notNullValue())
    			.when().get(new URI(resourceUri));
    	
    	String eTag = response.getHeader(ETAG);
    	Model model = response.as(Model.class, new RdfObjectMapper(resourceUri));

    	// Update a property
    	updateResource(model.getResource(resourceUri));
    	
    	RestAssured
                .given().contentType(TEXT_TURTLE).header(IF_MATCH, eTag)
                	.body(model, new RdfObjectMapper(resourceUri))
                .expect().statusCode(isSuccessful())
                .when().put(new URI(resourceUri));
    	
    	// Get the resource again to verify its content.
    	model = RestAssured.given().header(ACCEPT, TEXT_TURTLE)
    			.expect().statusCode(isSuccessful())
    			.when().get(new URI(resourceUri)).as(Model.class, new RdfObjectMapper(resourceUri));
    	
    	// Verify the change.
    	verifyUpdatedResource(model.getResource(resourceUri));
    }

    @Test(
            enabled = false,
            groups = {MUST},
            description = "If an otherwise valid HTTP PUT request is received that "
                    + "attempts to change properties the server does not allow "
                    + "clients to modify, LDP servers MUST respond with a 4xx range "
                    + "status code (typically 409 Conflict)")
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-put-servermanagedprops", testMethod = METHOD.NOT_IMPLEMENTED)
    public void testPutReadOnlyProperties4xxStatus() {

    }

    @Test(
            enabled = false,
            groups = {SHOULD},
            dependsOnMethods = {"testInvalidPutPropertiesNotAllowed"},
            description = "LDP servers SHOULD provide a corresponding response body containing "
                    + "information about which properties could not be persisted. The "
                    + "format of the 4xx response body is not constrained by LDP.")
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-put-servermanagedprops", testMethod = METHOD.NOT_IMPLEMENTED)
    public void test4xxErrorHasResponseBody() {

    }

    @Test(
            enabled = false,
            groups = {MUST},
            description = "If an otherwise valid HTTP PUT request is received that "
                    + "contains properties the server chooses not to persist, "
                    + "e.g. unknown content, LDP servers MUST respond with an "
                    + "appropriate 4xx range status code [HTTP11].")
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-put-failed", testMethod = METHOD.NOT_IMPLEMENTED)
    public void testPutPropertiesNotPersisted() {

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
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-put-failed", testMethod = METHOD.NOT_IMPLEMENTED)
    public void testResponsePropertiesNotPersisted() {

    }

    @Test(
            groups = {SHOULD},
            description = "LDP clients SHOULD use the HTTP If-Match header and HTTP ETags "
                    + "to ensure it isn’t modifying a resource that has changed since the "
                    + "client last retrieved its representation. LDP servers SHOULD require "
                    + "the HTTP If-Match header and HTTP ETags to detect collisions.")
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-put-precond", testMethod = METHOD.AUTOMATED)
    public void testPutRequiresIfMatch() throws URISyntaxException {
    	skipIfMethodNotAllowed(HttpMethod.PUT);

    	String resourceUri = getResourceUri();
    	Model model = RestAssured
    			.given()
    				.header(ACCEPT, TEXT_TURTLE)
    			.expect()
    				.statusCode(isSuccessful())
    				.header(ETAG, notNullValue())
    			.when()
    				.get(new URI(resourceUri)).as(Model.class, new RdfObjectMapper(resourceUri));

    	RestAssured
                .given()
                	.contentType(TEXT_TURTLE)
                	.body(model, new RdfObjectMapper(resourceUri))
                .expect()
                	.statusCode(not(isSuccessful()))
                .when()
                	.put(new URI(resourceUri));
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
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-put-precond", testMethod = METHOD.AUTOMATED)
    public void testConditionFailedStatusCode() throws URISyntaxException {
    	skipIfMethodNotAllowed(HttpMethod.PUT);

    	String resourceUri = getResourceUri();
    	Model model = RestAssured
    		.given()
    			.header(ACCEPT, TEXT_TURTLE)
    		.expect()
    			.statusCode(isSuccessful()).header(ETAG, notNullValue())
    		.when()
    			.get(resourceUri).as(Model.class, new RdfObjectMapper(resourceUri));

    	RestAssured
            .given()
                    .contentType(TEXT_TURTLE)
                    .header(IF_MATCH, "These aren't the ETags you're looking for.")
                    .body(model, new RdfObjectMapper(resourceUri))
            .expect()
                    .statusCode(HttpStatus.SC_PRECONDITION_FAILED)
            .when()
                    .put(resourceUri);
    }

    @Test(
            groups = {MUST},
            dependsOnMethods = {"testPutRequiresIfMatch"},
            description = "LDP servers MUST respond with status code 412 "
                    + "(Condition Failed) if ETags fail to match when there "
                    + "are no other errors with the request [HTTP11]. LDP "
                    + "servers that require conditional requests MUST respond "
                    + "with status code 428 (Precondition Required) when the "
                    + "absence of a precondition is the only reason for rejecting "
                    + "the request [RFC6585].")
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-put-precond", testMethod = METHOD.AUTOMATED)
    public void testPreconditionRequiredStatusCode() throws URISyntaxException {
    	skipIfMethodNotAllowed(HttpMethod.PUT);

    	String resourceUri = getResourceUri();
    	Model model = RestAssured
    			.given()
    				.header(ACCEPT, TEXT_TURTLE)
    			.expect()
    				.statusCode(isSuccessful())
    				.header(ETAG, notNullValue())
    			.when()
    				.get(new URI(resourceUri)).as(Model.class, new RdfObjectMapper(resourceUri));

    	RestAssured
                .given()
                	.contentType(TEXT_TURTLE)
                	.body(model, new RdfObjectMapper(resourceUri))
                .expect()
                	.statusCode(428)
                .when()
                	.put(new URI(resourceUri));
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
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-put-precond", testMethod = METHOD.NOT_IMPLEMENTED)
    public void testPutBadETag() throws URISyntaxException {
    	skipIfMethodNotAllowed(HttpMethod.PUT);

    	String resourceUri = getResourceUri();
    	Model model = RestAssured
    			.given()
    				.header(ACCEPT, TEXT_TURTLE)
    			.expect()
    				.statusCode(isSuccessful()).header(ETAG, notNullValue())
    			.when()
    				.get(new URI(resourceUri)).as(Model.class, new RdfObjectMapper(resourceUri));

    	RestAssured
                .given().contentType(TEXT_TURTLE)
                	.header(ETAG, "This is not the ETag you're looking for") // bad ETag value
                	.body(model, new RdfObjectMapper(resourceUri))
                .expect()
                	.statusCode(HttpStatus.SC_PRECONDITION_FAILED)
                .when()
                	.put(new URI(resourceUri));
    }

    @Test(
            // enabled = false,
            groups = {MUST},
            description = "LDP servers MUST support the HTTP HEAD method. ")
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-head-must", testMethod = METHOD.AUTOMATED)
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
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-patch-acceptpatch", testMethod = METHOD.AUTOMATED)
    public void testAcceptPatchHeader() throws URISyntaxException {
        skipIfMethodNotAllowed(HttpMethod.PATCH);

        RestAssured
                .expect().statusCode(isSuccessful()).header(ACCEPT_PATCH, notNullValue())
                .when().options(new URI(getResourceUri()));
    }

    @Test(
            // enabled = false,
            groups = {MUST},
            description = "LDP servers MUST support the HTTP OPTIONS method. ")
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-options-must", testMethod = METHOD.AUTOMATED)
    public void testOptions() {
        RestAssured.expect().statusCode(isSuccessful()).when().options(getResourceUri());
    }

    @Test(
            // enabled = false,
            groups = {MUST},
            description = "LDP servers MUST indicate their support for HTTP Methods "
                    + "by responding to a HTTP OPTIONS request on the LDPR’s URL "
                    + "with the HTTP Method tokens in the HTTP response header Allow. ")
    @SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "ldpr-options-allow", testMethod = METHOD.AUTOMATED)
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

    // Update a resource then later test if the updates were applied (i.e., on a subsequent GET).
    // These methods could be overwritten by subclasses.
    private final static String TITLE_FOR_UPDATE = "LDP Test Suite: This resource has been updated... " + System.currentTimeMillis();
    
    /**
	 * Update a resource then later test if the updates were applied (i.e., on a
	 * subsequent GET). These methods could be overwritten by subclasses.
	 * 
	 * @see #verifyUpdatedResource(Resource)
	 */
    protected void updateResource(Resource r) {
    	// Set a title.
    	r.removeAll(DCTerms.title);
    	r.addProperty(DCTerms.title, TITLE_FOR_UPDATE);
    }
    
    /**
	 * Update a resource then later test if the updates were applied (i.e., on a
	 * subsequent GET). These methods could be overwritten by subclasses.
	 * 
	 * @see #updateResource(Resource)
	 */
    protected void verifyUpdatedResource(Resource r) {
    	// Test the resource has the title we set.
    	r.hasProperty(DCTerms.title, TITLE_FOR_UPDATE);
    }
 
	/**
	 * Are there any restrictions on resource content? This should be true for
	 * LDP containers since PUT is not allowed to modify containment triples.
	 * 
	 * @return true if there are restrictions on what triples are allowed; false
	 *         if the entire resource can be replaced with any triples
	 * @see #testPutReplacesResource
	 * @see CommonContainerTest#testRejectPutModifyingContainmentTriples
	 */
    protected boolean restrictionsOnContent() {
    	return false;
    }
}
