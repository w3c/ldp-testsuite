package org.w3.ldp.testsuite.test;

import static org.hamcrest.core.IsNot.not;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpStatus;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.http.HttpMethod;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.exception.SkipClientTestException;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

import org.w3.ldp.testsuite.vocab.LDP;

/**
 * Common tests for all LDP container types.
 */
public abstract class CommonContainerTest extends RdfSourceTest {

	public static final String MSG_LOC_NOTFOUND = "Location header missing after POST create.";
	public static final String MSG_MBRRES_NOTFOUND = "Unable to locate object in triple with predicate ldp:membershipResource.";

	@Test(
			groups = { MAY }, 
			description = "LDP servers MAY choose to allow the creation of new "
					+ "resources using HTTP PUT.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-put-create", 
			testMethod = METHOD.AUTOMATED)
	public void testPutToCreate() throws URISyntaxException {
		String location = putToCreate();
		RestAssured.expect().statusCode(isSuccessful()).when()
				.delete(new URI(location));
	}

	@Test(
			enabled = false, 
			groups = { MUST }, 
			description = "LDP servers MUST assign the default base-URI "
					+ "for [RFC3987] relative-URI resolution to be the HTTP "
					+ "Request-URI when the resource already exists, and to "
					+ "the URI of the created resource when the request results "
					+ "in the creation of a new resource.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-defbaseuri", 
			testMethod = METHOD.NOT_IMPLEMENTED)
	public void testRelativeUriResolutionPost() {

	}

	@Test(
			groups = { SHOULD }, 
			description = "LDPC representations SHOULD NOT use RDF container "
					+ "types rdf:Bag, rdf:Seq or rdf:List.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-nordfcontainertypes", 
			testMethod = METHOD.AUTOMATED)
	public void testNoRdfBagSeqOrList() throws URISyntaxException {
		Model containerModel = getAsModel(getResourceUri());
		assertFalse(containerModel.listResourcesWithProperty(RDF.type, RDF.Bag)
				.hasNext(), "LDPC representations should not use rdf:Bag");
		assertFalse(containerModel.listResourcesWithProperty(RDF.type, RDF.Seq)
				.hasNext(), "LDPC representations should not use rdf:Seq");
		assertFalse(containerModel
				.listResourcesWithProperty(RDF.type, RDF.List).hasNext(),
				"LDPC representations should not use rdf:List");
	}

	@Test(
			groups = { SHOULD }, 
			enabled = false, // not implemented
			description = "LDP servers SHOULD respect all of a client's LDP-defined "
					+ "hints, for example which subsets of LDP-defined state the "
					+ "client is interested in processing, to influence the set of "
					+ "triples returned in representations of an LDPC, particularly for "
					+ "large LDPCs. See also [LDP-PAGING].")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-prefer", 
			testMethod = METHOD.NOT_IMPLEMENTED)
	public void testClientHints() {

	}

	@Test(
			groups = { SHOULD }, 
			description = "LDPC clients SHOULD create member resources by "
					+ "submitting a representation as the entity body of the "
					+ "HTTP POST to a known LDPC.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-created201", 
			testMethod = METHOD.CLIENT_ONLY)
	public void testClientPostToCreate() {
		throw new SkipClientTestException();
	}

	@Test(
			groups = { MUST },
			description = "If the resource was created successfully, LDP servers MUST "
					+ "respond with status code 201 (Created) and the Location "
					+ "header set to the new resource’s URL. Clients shall not "
					+ "expect any representation in the response entity body on "
					+ "a 201 (Created) response.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-created201", 
			testMethod = METHOD.AUTOMATED)
	public void testPostResponseStatusAndLocation() throws URISyntaxException {
		skipIfMethodNotAllowed(HttpMethod.POST);

		Model model = postContent();
		Response postResponse = RestAssured.given().contentType(TEXT_TURTLE)
				.body(model, new RdfObjectMapper()).expect()
				.statusCode(HttpStatus.SC_CREATED).when()
				.post(new URI(getResourceUri()));

		String location = postResponse.getHeader(LOCATION);
		assertNotNull(location, MSG_LOC_NOTFOUND);

		RestAssured.expect().statusCode(isSuccessful()).when()
				.delete(new URI(location));
	}

	@Test(
			groups = { MUST }, 
			description = "When a successful HTTP POST request to an LDPC results "
					+ "in the creation of an LDPR, a containment triple MUST be "
					+ "added to the state of LDPC.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-createdmbr-contains", 
			testMethod = METHOD.AUTOMATED)
	public void testPostContainer() throws URISyntaxException {
		skipIfMethodNotAllowed(HttpMethod.POST);

		Model model = postContent();
		String containerUri = getResourceUri();
		Response postResponse = RestAssured.given().contentType(TEXT_TURTLE)
				.body(model, new RdfObjectMapper()).expect()
				.statusCode(HttpStatus.SC_CREATED).when()
				.post(new URI(getResourceUri()));

		String location = postResponse.getHeader(LOCATION);
		assertNotNull(location, MSG_LOC_NOTFOUND);

		Model containerModel = getAsModel(containerUri);
		Resource container = containerModel.getResource(containerUri);

		assertTrue(
				container.hasProperty(containerModel
						.createProperty(LDP.contains.stringValue()),
						containerModel.getResource(location)),
				"Container <"
						+ containerUri
						+ "> does not have a containment triple for newly created resource <"
						+ location + ">.");

		RestAssured.expect().statusCode(isSuccessful()).when()
				.delete(new URI(location));
	}

	@Test(
			groups = { MUST }, 
			enabled = false, // not implemented
			description = "LDP servers that successfully create a resource from a "
					+ "RDF representation in the request entity body MUST honor the "
					+ "client's requested interaction model(s). The created resource "
					+ "can be thought of as an RDF named graph [rdf11-concepts]. If any "
					+ "model cannot be honored, the server MUST fail the request.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-createrdf", 
			testMethod = METHOD.NOT_IMPLEMENTED)
	public void testRequestedInteractionModel() {
		skipIfMethodNotAllowed(HttpMethod.POST);

		// ...
	}

	@Test(
			groups = { MUST }, 
			description = "LDP servers MUST accept a request entity body with a "
					+ "request header of Content-Type with value of text/turtle [turtle].")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-turtle", 
			testMethod = METHOD.AUTOMATED)
	public void testAcceptTurtle() throws URISyntaxException {
		skipIfMethodNotAllowed(HttpMethod.POST);

		Model model = postContent();
		Response postResponse = RestAssured.given().contentType(TEXT_TURTLE)
				.body(model, new RdfObjectMapper()).expect()
				.statusCode(HttpStatus.SC_CREATED).when()
				.post(new URI(getResourceUri()));

		// Delete the resource to clean up.
		String location = postResponse.getHeader(LOCATION);
		if (location != null) {
			RestAssured.expect().statusCode(isSuccessful()).when()
					.delete(new URI(location));
		}
	}

	@Test(
			groups = { SHOULD }, 
			enabled = false, 
			description = "LDP servers SHOULD use the Content-Type request header to "
					+ "determine the representation format when the request has an "
					+ "entity body.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-contenttype",
			testMethod = METHOD.NOT_IMPLEMENTED)
	public void testContentTypeHeader() throws URISyntaxException {
		skipIfMethodNotAllowed(HttpMethod.POST);

		// TODO: Determine how to best test this.
	}

	@Test(
			groups = { MUST }, 
			description = "In RDF representations, LDP servers MUST interpret the null relative "
					+ "URI for the subject of triples in the LDPR representation in the request "
					+ "entity body as referring to the entity in the request body. Commonly, that "
					+ "entity is the model for the “to be created” LDPR, so triples whose subject "
					+ "is the null relative URI will usually result in triples in the created "
					+ "resource whose subject is the created resource.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-rdfnullrel", 
			testMethod = METHOD.AUTOMATED)
	public void testNullRelativeUri() throws URISyntaxException {
		skipIfMethodNotAllowed(HttpMethod.POST);

		Model requestModel = ModelFactory.createDefaultModel();
		Resource resource = requestModel.createResource("");
		String identifier = UUID.randomUUID().toString();
		resource.addProperty(DCTerms.identifier, identifier);

		Response postResponse = RestAssured.given().contentType(TEXT_TURTLE)
				.body(requestModel, new RdfObjectMapper(getResourceUri()))
				.expect().statusCode(HttpStatus.SC_CREATED).when()
				.post(new URI(getResourceUri()));

		String location = postResponse.getHeader(LOCATION);
		assertNotNull(location, MSG_LOC_NOTFOUND);

		// Get the resource to check that the resource with a null relative URI
		// was assigned the URI in the Location response header.
		Model responseModel = getAsModel(location);
		Resource created = responseModel.getResource(location);

		// TODO: Is this the best test? It's possible a server will assign its
		// own dcterms:identifier.
		assertTrue(
				created.hasProperty(DCTerms.identifier, identifier),
				"The resource created with URI <"
						+ location
						+ "> does not have the dcterms:identifier as the resource POSTed using the null relative URI.");

		// Delete the resource to clean up.
		RestAssured.expect().statusCode(isSuccessful()).when()
				.delete(new URI(location));
	}

	@Test(
			groups = { SHOULD }, 
			enabled = false, // not implemented
			description = "LDP servers SHOULD assign the URI for the resource to be created "
					+ "using server application specific rules in the absence of a client hint.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-serverassignuri", 
			testMethod = METHOD.NOT_IMPLEMENTED)
	public void testAssignUri() {
		skipIfMethodNotAllowed(HttpMethod.POST);

		// ...
	}

	@Test(
			groups = { SHOULD }, 
			description = "LDP servers SHOULD allow clients to create new resources without "
					+ "requiring detailed knowledge of application-specific constraints. This "
					+ "is a consequence of the requirement to enable simple creation and "
					+ "modification of LDPRs. LDP servers expose these application-specific "
					+ "constraints as described in section 4.2.1 General.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-mincontraints", 
			testMethod = METHOD.AUTOMATED)
	public void testCreateWithoutConstraints() throws URISyntaxException {
		skipIfMethodNotAllowed(HttpMethod.POST);

		// Create a resource with one statement (dcterms:identifier).
		Model requestModel = ModelFactory.createDefaultModel();
		Resource resource = requestModel.createResource("");
		String identifier = UUID.randomUUID().toString();
		resource.addProperty(DCTerms.identifier, identifier);

		Response postResponse = RestAssured.given().contentType(TEXT_TURTLE)
				.body(requestModel, new RdfObjectMapper()).expect()
				.statusCode(HttpStatus.SC_CREATED).when()
				.post(new URI(getResourceUri()));

		// Delete the resource to clean up.
		String location = postResponse.getHeader(LOCATION);
		if (location != null) {
			RestAssured.expect().statusCode(isSuccessful()).when()
					.delete(new URI(location));
		}

	}

	@Test(
			groups = { SHOULD },
			description = "LDP servers that allow member creation via POST SHOULD NOT re-use URIs.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-dontreuseuris", 
			testMethod = METHOD.AUTOMATED)
	public void testRestrictUriReUseSlug() throws URISyntaxException {
		testRestrictUriReUse("uritest");
	}

	@Test(
			groups = { SHOULD }, 
			description = "LDP servers that allow member creation via POST SHOULD NOT re-use URIs.")
	@SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-dontreuseuris", testMethod = METHOD.AUTOMATED)
	public void testRestrictUriReUseNoSlug() throws URISyntaxException {
		testRestrictUriReUse(null);
	}

	@Test(
			groups = { MAY }, 
			enabled = false, // not implemented
			description = "Upon successful creation of an LDP-NR (HTTP status code of 201-Created "
					+ "and URI indicated by Location response header), LDP servers MAY create an "
					+ "associated LDP-RS to contain data about the newly created LDP-NR.")
	@SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-createbinlinkmetahdr", testMethod = METHOD.NOT_IMPLEMENTED)
	public void testCreateAssociatedRdfSource() {

	}

	@Test(
			groups = { MUST }, 
			enabled = false, // not implemented
			dependsOnMethods = { "testCreateAssociatedRdfSource" }, 
			description = "If an LDPC server creates this associated LDP-RS it MUST indicate "
					+ "its location on the HTTP response using the HTTP Link response header "
					+ "with link relation describedby and href to be the URI of the associated "
					+ "LDP-RS resource [RFC5988].")
	@SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-createbinlinkmetahdr", testMethod = METHOD.NOT_IMPLEMENTED)
	public void testAssociatedRdfSourceLinkResponseHeader() {

	}

	@Test(
			groups = { MUST }, 
			description = "LDP servers that support POST MUST include an Accept-Post "
					+ "response header on HTTP OPTIONS responses, listing post document "
					+ "media type(s) supported by the server.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-acceptposthdr", 
			testMethod = METHOD.AUTOMATED)
	public void testAcceptPostResponseHeader() throws URISyntaxException {
		skipIfMethodNotAllowed(HttpMethod.POST);

		Response optionsResponse = RestAssured.expect()
				.statusCode(isSuccessful()).when()
				.options(new URI(getResourceUri()));
		assertNotNull(
				optionsResponse.getHeader(ACCEPT_POST),
				"The HTTP OPTIONS response on container <"
						+ getResourceUri()
						+ "> did not include an Accept-Post response header, but it lists POST in its Allow response header.");
	}

	@Test(
			groups = { SHOULD }, 
			description = "LDP servers SHOULD NOT allow HTTP PUT to update an LDPC’s "
					+ "containment triples; if the server receives such a request, it "
					+ "SHOULD respond with a 409 (Conflict) status code.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-put-mbrprops", 
			testMethod = METHOD.AUTOMATED)
	public void testRejectPutModifyingContainmentTriples() {
		String containerUri = getResourceUri();
    	Response response = RestAssured
    			.given().header(ACCEPT, TEXT_TURTLE)
    			.expect().statusCode(isSuccessful())
    			.when().get(containerUri);
    	String eTag = response.getHeader(ETAG);
    	Model model = response.as(Model.class, new RdfObjectMapper(containerUri));
    	
    	// Try to modify the ldp:contains triple.
    	Resource containerResource = model.getResource(containerUri);
    	containerResource.addProperty(model.createProperty(LDP.contains.stringValue()),
    			model.createResource("#" + System.currentTimeMillis()));

    	RequestSpecification putRequest = RestAssured.given().contentType(TEXT_TURTLE);
    	if (eTag != null) {
    		putRequest.header(IF_MATCH, eTag);
    	}
    	putRequest.body(model, new RdfObjectMapper(containerUri))
    		.expect().statusCode(not(isSuccessful()))
    		.when().put(containerUri);
	}

	@Test(
			groups = { SHOULD }, 
			dependsOnMethods = { "testPutToCreate" }, 
			description = "LDP servers that allow LDPR creation via PUT SHOULD NOT "
					+ "re-use URIs. For RDF representations (LDP-RSs),the created "
					+ "resource can be thought of as an RDF named graph [rdf11-concepts].")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-put-create", 
			testMethod = METHOD.AUTOMATED)
	public void testRestrictPutReUseUri() throws URISyntaxException {
		String location = putToCreate();
		URI uri = new URI(location);

		// Delete the resource.
    	RestAssured
    		.expect()
    			.statusCode(isSuccessful())
    		.when()
    			.delete(uri);

		// Try to put to the same URI again. It should fail.
    	Model content = postContent();
    	RestAssured
    		.given()
    			.contentType(TEXT_TURTLE)
    			.body(content, new RdfObjectMapper(location))
    		.expect()
    			.statusCode(not(isSuccessful()))
    		.when()
    			.put(uri);
	}

	@Test(
			groups = { MUST }, 
			description = "When an LDPR identified by the object of a containment triple "
					+ "is deleted, the LDPC server MUST also remove the LDPR from the "
					+ "containing LDPC by removing the corresponding containment triple.")
	@SpecTest(specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-del-contremovesconttriple", testMethod = METHOD.AUTOMATED)
	public void testDeleteRemovesContainmentTriple() throws URISyntaxException {
		skipIfMethodNotAllowed(HttpMethod.POST);

		Model model = postContent();
		Response postResponse = RestAssured.given().contentType(TEXT_TURTLE)
				.body(model, new RdfObjectMapper()).expect()
				.statusCode(HttpStatus.SC_CREATED).when()
				.post(new URI(getResourceUri()));

		// POST support is optional. Only test delete if the POST succeeded.
		if (postResponse.getStatusCode() != HttpStatus.SC_CREATED) {
			throw new SkipException("HTTP POST failed with status "
					+ postResponse.getStatusCode());
		}

		String location = postResponse.getHeader(LOCATION);
		assertNotNull(location, MSG_LOC_NOTFOUND);

		// TODO: Check if delete is supported on location.....

		// Delete the resource
		RestAssured.expect().statusCode(isSuccessful()).when()
				.delete(new URI(location));

		// Test the membership triple
		Model containerModel = getAsModel(getResourceUri());
		Resource container = containerModel.getResource(getResourceUri());

		assertFalse(
				container.hasProperty(containerModel
						.createProperty(LDP.contains.stringValue()),
						containerModel.getResource(location)),
				"The LDPC server must remove the corresponding containment triple when an LDPR is deleted.");
	}

	@Test(
			groups = { MUST }, 
			enabled = false, // not implemented
			description = "When an LDPR identified by the object of a containment triple "
					+ "is deleted, and the LDPC server created an associated LDP-RS "
					+ "(see the LDPC POST section), the LDPC server MUST also remove the "
				+ "associated LDP-RS it created.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-del-contremovescontres", 
			testMethod = METHOD.NOT_IMPLEMENTED)
	public void testDeleteContainerAssociatedResource() {

	}

	@Test(
			groups = { SHOULD }, 
			description = "LDP servers are RECOMMENDED to support HTTP PATCH as the "
					+ "preferred method for updating an LDPC's empty-container triples. ")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-patch-req", 
			testMethod = METHOD.AUTOMATED)
	public void testPatchMethod() {
		assertTrue(
				supports(HttpMethod.PATCH),
				"Container <"
						+ getResourceUri()
						+ "> has not advertised PATCH support through its HTTP OPTIONS response.");

		// TODO: Actually try to patch the containment triples.
	}

	@Test(
			groups = { MUST },
			enabled = false, // not implemented
			description = "When an LDPC server creates an LDP-NR (for example, one "
					+ "whose representation was HTTP POSTed to the LDPC) the LDP "
					+ "server might create an associated LDP-RS to contain data about "
					+ "the non-LDPR (see LDPC POST section). For LDP-NRs that have this "
					+ "associated LDP-RS, an LDPC server MUST provide an HTTP Link "
					+ "header whose target URI is the associated LDP-RS, and whose link "
					+ "relation type is describedby [RFC5988].")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-options-linkmetahdr", 
			testMethod = METHOD.NOT_IMPLEMENTED)
	public void testProvideLinkHeaderAssociatedRdfSource() {

	}

	protected boolean restrictionsOnContent() {
		return true;
	}

	/**
	 * Tests that LDP servers do not reuse URIs after deleting resources.
	 * 
	 * @param slug
	 *            the slug header for the request or null if no slug
	 * 
	 * @throws URISyntaxException
	 *             on bad URIs
	 * 
	 * @see #testRestrictUriReUseSlug()
	 * @see #testRestrictUriReUseNoSlug()
	 */
	private void testRestrictUriReUse(String slug) throws URISyntaxException {
		skipIfMethodNotAllowed(HttpMethod.POST);

		// POST two resources with the same Slug header and content to make sure
		// they have different URIs.
		Model content = postContent();
		String loc1 = post(content, slug);

		// TODO: Test if DELETE is supported before trying to delete the
		// resource.
		// Delete the resource to make sure the server doesn't reuse the URI
		// below.
		RestAssured.expect().statusCode(isSuccessful()).when()
				.delete(new URI(loc1));

		String loc2 = post(content, slug);
		assertNotEquals(loc1, loc2, "Server reused URIs for POSTed resources.");
	}

	private String post(Model content, String slug) throws URISyntaxException {
		RequestSpecification spec = RestAssured.given()
				.contentType(TEXT_TURTLE);
		if (slug != null) {
			spec.header(SLUG, slug);
		}
		Response post = spec.body(content, new RdfObjectMapper()).expect()
				.statusCode(HttpStatus.SC_CREATED).when()
				.post(new URI(getResourceUri()));
		String location = post.getHeader(LOCATION);
		assertNotNull(location, MSG_LOC_NOTFOUND);

		return location;
	}

	/**
	 * Attempts to create a new resource using PUT.
	 * 
	 * @return the location of the created resource
	 * @see #testPutToCreate()
	 * @see #testRestrictPutReUseUri()
	 */
	protected String putToCreate() {
		// Build a unique URI for the PUT request.
		URI target = UriBuilder.fromUri(getResourceUri())
				.path(UUID.randomUUID().toString()).build();
		Model model = postContent();
		Response response = RestAssured.given().contentType(TEXT_TURTLE)
				.body(model, new RdfObjectMapper("")).expect()
				.statusCode(HttpStatus.SC_CREATED).when().put(target);

		String location = response.getHeader(LOCATION);
		if (location == null) {
			return target.toString();
		}

		return location;
	}

}
