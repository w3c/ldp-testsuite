package org.w3.ldp.testsuite.test;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.annotations.SpecTest.STATUS;
import org.w3.ldp.testsuite.exception.SkipException;
import org.w3.ldp.testsuite.http.HttpMethod;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;
import org.w3.ldp.testsuite.matcher.HeaderMatchers;
import org.w3.ldp.testsuite.vocab.LDP;

import javax.ws.rs.core.UriBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.hamcrest.core.IsNot.not;
import static org.testng.Assert.*;
import static org.w3.ldp.testsuite.http.HttpHeaders.*;
import static org.w3.ldp.testsuite.http.LdpPreferences.PREFER_CONTAINMENT;
import static org.w3.ldp.testsuite.http.LdpPreferences.PREFER_MINIMAL_CONTAINER;
import static org.w3.ldp.testsuite.http.MediaTypes.APPLICATION_LD_JSON;
import static org.w3.ldp.testsuite.http.MediaTypes.TEXT_TURTLE;
import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

/**
 * Common tests for all LDP container types.
 */
public abstract class CommonContainerTest extends RdfSourceTest {

	public static final String MSG_LOC_NOTFOUND = "Location header missing after POST create.";
	public static final String MSG_MBRRES_NOTFOUND = "Unable to locate object in triple with predicate ldp:membershipResource.";

	@Parameters("auth")
	public CommonContainerTest(@Optional String auth) throws IOException {
		super(auth);
	}

	@Test(
			groups = {MAY},
			description = "LDP servers MAY choose to allow the creation of new "
					+ "resources using HTTP PUT.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-put-create",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testPutToCreate() {
		String location = putToCreate();
		buildBaseRequestSpecification().delete(location);
	}

	@Test(
			groups = {MUST},
			description = "LDP servers MUST assign the default base-URI "
					+ "for [RFC3987] relative-URI resolution to be the HTTP "
					+ "Request-URI when the resource already exists, and to "
					+ "the URI of the created resource when the request results "
					+ "in the creation of a new resource.")
	@Parameters("relativeUri")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-defbaseuri",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. ")
	public void testRelativeUriResolutionPost(@Optional String relativeUri) {
		skipIfMethodNotAllowed(HttpMethod.POST);

		if (restrictionsOnPostContent()) {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					"Skipping test because there are restrictions on POST content. "
					+ "The requirement needs to be tested manually.", skipLog);
		}

		if (relativeUri == null) {
			relativeUri = RELATIVE_URI;
		}

		// POST content with a relative URI (other than the null relative URI).
		Model requestModel = postContent();
		Resource r = requestModel.getResource("");
		r.addProperty(DCTerms.relation, requestModel.createResource(relativeUri));

		String containerUri = getResourceUri();
		Response postResponse = buildBaseRequestSpecification()
					.contentType(TEXT_TURTLE)
					.body(requestModel, new RdfObjectMapper())
				.expect()
					.statusCode(HttpStatus.SC_CREATED)
					.header(LOCATION, HeaderMatchers.headerPresent())
				.when()
					.post(containerUri);

		String location = postResponse.getHeader(LOCATION);

		try {
			Response getResponse = buildBaseRequestSpecification()
					.header(ACCEPT, TEXT_TURTLE)
				.expect()
					.statusCode(isSuccessful())
				.when()
					.get(location);

			// Check that the dcterms:relation URI was resolved relative to the
			// URI assigned to the new resource (location).
			Model responseModel = getResponse.as(Model.class, new RdfObjectMapper(location));
			String relationAbsoluteUri = resolveIfRelative(location, relativeUri);
			assertTrue(
					responseModel.contains(
							getPrimaryTopic(responseModel, location),
							DCTerms.relation,
							responseModel.getResource(relationAbsoluteUri)
					),
					"Response does not have expected triple: <" + location + "> dcterms:relation <" + relationAbsoluteUri + ">."
			);
		} finally {
			buildBaseRequestSpecification().delete(location);
		}
	}

	@Test(
			groups = {SHOULD},
			description = "LDPC representations SHOULD NOT use RDF container "
					+ "types rdf:Bag, rdf:Seq or rdf:List.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-nordfcontainertypes",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testNoRdfBagSeqOrList() {
		Model containerModel = getAsModel(getResourceUri());
		assertFalse(containerModel.listResourcesWithProperty(RDF.type, RDF.Bag)
				.hasNext(), "LDPC representations should not use rdf:Bag");
		assertFalse(containerModel.listResourcesWithProperty(RDF.type, RDF.Seq)
				.hasNext(), "LDPC representations should not use rdf:Seq");
		assertFalse(containerModel.listResourcesWithProperty(RDF.type, RDF.List).hasNext(),
				"LDPC representations should not use rdf:List"
		);
	}

	@Test(
			groups = {SHOULD},
			description = "LDP servers SHOULD respect all of a client's LDP-defined "
					+ "hints, for example which subsets of LDP-defined state the "
					+ "client is interested in processing, to influence the set of "
					+ "triples returned in representations of an LDPC, particularly for "
					+ "large LDPCs. See also [LDP-PAGING].")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-prefer",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testPreferContainmentTriples() {
		Response response;
		Model model;
		String containerUri = getResourceUri();

		// Ask for containment triples.
		response = buildBaseRequestSpecification()
					.header(ACCEPT, TEXT_TURTLE)
					.header(PREFER, include(PREFER_CONTAINMENT)) // request all containment triples
				.expect()
					.statusCode(isSuccessful())
				.when()
					.get(containerUri);
		model = response.as(Model.class, new RdfObjectMapper(containerUri));

		checkPreferenceAppliedHeader(response);

		// Assumes the container is not empty.
		assertTrue(model.contains(model.getResource(containerUri), model.createProperty(LDP.contains.stringValue())),
				"Container does not have containment triples");

		// Ask for a minimal container.
		response = buildBaseRequestSpecification()
					.header(ACCEPT, TEXT_TURTLE)
					.header(PREFER, include(PREFER_MINIMAL_CONTAINER)) // request no containment triples
				.expect()
					.statusCode(isSuccessful())
				.when()
					.get(containerUri);
		model = response.as(Model.class, new RdfObjectMapper(containerUri));

		checkPreferenceAppliedHeader(response);
		assertFalse(model.contains(model.getResource(containerUri), model.createProperty(LDP.contains.stringValue())),
				"Container has containment triples when minimal container was requested");

		// Ask to omit containment triples.
		response = buildBaseRequestSpecification()
					.header(ACCEPT, TEXT_TURTLE)
					.header(PREFER, omit(PREFER_CONTAINMENT)) // request no containment triples
				.expect()
					.statusCode(isSuccessful())
				.when()
					.get(containerUri);
		model = response.as(Model.class, new RdfObjectMapper(containerUri));

		checkPreferenceAppliedHeader(response);

		// Assumes the container is not empty.
		assertFalse(model.contains(model.getResource(containerUri), model.createProperty(LDP.contains.stringValue())),
				"Container has containment triples when client requested server omit them");
	}

	@Test(
			groups = {MUST},
			description = "If the resource was created successfully, LDP servers MUST "
					+ "respond with status code 201 (Created) and the Location "
					+ "header set to the new resource’s URL. Clients shall not "
					+ "expect any representation in the response entity body on "
					+ "a 201 (Created) response.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-created201",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testPostResponseStatusAndLocation() throws URISyntaxException {
		skipIfMethodNotAllowed(HttpMethod.POST);

		String location = null;

		try {
			Model model = postContent();
			Response postResponse = buildBaseRequestSpecification().contentType(TEXT_TURTLE)
					.body(model, new RdfObjectMapper()).expect()
					.statusCode(HttpStatus.SC_CREATED).when()
					.post(getResourceUri());

			location = postResponse.getHeader(LOCATION);
			assertNotNull(location, MSG_LOC_NOTFOUND);
		} finally {
			buildBaseRequestSpecification().delete(location);
		}
	}

	@Test(
			groups = {MUST},
			description = "When a successful HTTP POST request to an LDPC results "
					+ "in the creation of an LDPR, a containment triple MUST be "
					+ "added to the state of LDPC.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-createdmbr-contains",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testPostContainer() throws URISyntaxException {
		skipIfMethodNotAllowed(HttpMethod.POST);

		Model model = postContent();
		String containerUri = getResourceUri();
		Response postResponse = buildBaseRequestSpecification()
					.contentType(TEXT_TURTLE)
					.body(model, new RdfObjectMapper())
				.expect()
					.statusCode(HttpStatus.SC_CREATED)
					.header(LOCATION, HeaderMatchers.headerPresent())
				.when()
					.post(containerUri);

		String location = postResponse.getHeader(LOCATION);

		try {
			Response getResponse = buildBaseRequestSpecification()
					.header(ACCEPT, TEXT_TURTLE)
					.header(PREFER, include(PREFER_CONTAINMENT)) // hint to the server that we want containment triples
				.expect()
					.statusCode(isSuccessful())
				.when()
					.get(containerUri);
			Model containerModel = getResponse.as(Model.class, new RdfObjectMapper(containerUri));
			Resource container = containerModel.getResource(containerUri);

			assertTrue(
					container.hasProperty(containerModel
									.createProperty(LDP.contains.stringValue()),
							containerModel.getResource(location)
					),
					"Container <"
							+ containerUri
							+ "> does not have a containment triple for newly created resource <"
							+ location + ">."
			);
		} finally {
			buildBaseRequestSpecification().delete(location);
		}
	}

	@Test(
			groups = {MUST},
			description = "LDP servers that successfully create a resource from a "
					+ "RDF representation in the request entity body MUST honor the "
					+ "client's requested interaction model(s). The created resource "
					+ "can be thought of as an RDF named graph [rdf11-concepts]. If any "
					+ "model cannot be honored, the server MUST fail the request.")
	@Parameters("containerAsResource")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-createrdf",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testRequestedInteractionModelHeaders covers the rest.")
	public void testRequestedInteractionModelCreateNotAllowed(@Optional String containerAsResource) {
		if (containerAsResource == null)
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					"containerAsResource is null", skipLog);

		Model model = postContent();

		// If create is successful, then not acting like a plain ole resource
		Response postResponse = buildBaseRequestSpecification().contentType(TEXT_TURTLE)
				.body(model, new RdfObjectMapper())
				.post(containerAsResource);

		// Cleanup if it actually created something
		String location = postResponse.getHeader(LOCATION);
		if (postResponse.statusCode() == HttpStatus.SC_CREATED && location !=null)
			buildBaseRequestSpecification().delete(location);

		assertNotEquals(postResponse.statusCode(), HttpStatus.SC_CREATED, "Resources with interaction model of only ldp:Resources shouldn't allow container POST-create behavior.");

		// TODO: Possibly parse 'Allow' header to see if POST is wrongly listed
	}

	@Test(
			groups = {MUST},
			description = "LDP servers that successfully create a resource from a "
					+ "RDF representation in the request entity body MUST honor the "
					+ "client's requested interaction model(s). The created resource "
					+ "can be thought of as an RDF named graph [rdf11-concepts]. If any "
					+ "model cannot be honored, the server MUST fail the request.")
	@Parameters("containerAsResource")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-createrdf",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testRequestedInteractionModelCreateNotAllowed covers the rest.")
	public void testRequestedInteractionModelHeaders(@Optional String containerAsResource) {
		if (containerAsResource == null)
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					"containerAsResource is null", skipLog);

		// Ensure we don't get back any of the container types in the rel='type' Link header
		Response response = buildBaseRequestSpecification()
				.expect()
					.statusCode(isSuccessful())
				.when()
					.options(containerAsResource);
		assertFalse(
				containsLinkHeader(
						containerAsResource,
						LINK_REL_TYPE,
						LDP.BasicContainer.stringValue(),
						containerAsResource,
						response) ||
				containsLinkHeader(
						containerAsResource,
						LINK_REL_TYPE,
						LDP.DirectContainer.stringValue(),
						containerAsResource,
						response) ||
				containsLinkHeader(
						containerAsResource,
						LINK_REL_TYPE,
						LDP.IndirectContainer.stringValue(),
						containerAsResource,
						response),
				"Resource wrongly advertising itself as a rel='type' of one of the container types."
		);
	}

	@Test(
			groups = {MUST},
			description = "LDP servers MUST accept a request entity body with a "
					+ "request header of Content-Type with value of text/turtle [turtle].")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-turtle",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testAcceptTurtle() {
		skipIfMethodNotAllowed(HttpMethod.POST);

		Model model = postContent();
		Response postResponse = buildBaseRequestSpecification().contentType(TEXT_TURTLE)
				.body(model, new RdfObjectMapper()).expect()
				.statusCode(HttpStatus.SC_CREATED).when()
				.post(getResourceUri());

		// Delete the resource to clean up.
		String location = postResponse.getHeader(LOCATION);
		if (location != null) {
			buildBaseRequestSpecification().delete(location);
		}
	}

	@Test(
			groups = {SHOULD},
			description = "LDP servers SHOULD use the Content-Type request header to "
					+ "determine the representation format when the request has an "
					+ "entity body.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-contenttype",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testContentTypeHeader() throws URISyntaxException {
		skipIfMethodNotAllowed(HttpMethod.POST);

		// POST Turtle content with a bad Content-Type request header to see what happens.
		Model toPost = postContent();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		toPost.write(out, "TURTLE");
		Response postResponse = buildBaseRequestSpecification()
				.contentType("text/this-is-not-turtle")
				.body(out.toByteArray())
			.when()
				.post(getResourceUri());

		if (postResponse.getStatusCode() == HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE) {
			// If we get an unsupported media type status, we're done.
			return;
		}

		// Otherwise, we still might be OK if the server supports non-RDF source,
		// in which case it might have treated the POST content as binary. Check
		// the response Content-Type if we ask for the new resource.
		assertTrue(postResponse.getStatusCode() == HttpStatus.SC_CREATED ||
						postResponse.getStatusCode() == HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE,
				"Expected either 415 Unsupported Media Type or 201 Created in response to POST");

		String location = postResponse.getHeader(LOCATION);
		assertNotNull(location, "No Location response header on 201 Created response");

		try {
			Response getResponse = buildBaseRequestSpecification()
				.when()
					.get(location)
				.then().assertThat()
					.statusCode(isSuccessful())
					.and().contentType(not(TEXT_TURTLE))
				.extract().response();

			// Also make sure there is no Link header indicating this is an RDF source.
			assertFalse(
					containsLinkHeader(
							location,
							LINK_REL_TYPE,
							LDP.RDFSource.stringValue(),
							location,
							getResponse
					),
					"Server should not respond with RDF source Link header when content was " +
					"created with non-RDF Content-Type");
		} finally {
			// Clean up.
			buildBaseRequestSpecification().delete(location);
		}
	}

	@Test(
			groups = {MUST},
			description = "In RDF representations, LDP servers MUST interpret the null relative "
					+ "URI for the subject of triples in the LDPR representation in the request "
					+ "entity body as referring to the entity in the request body. Commonly, that "
					+ "entity is the model for the “to be created” LDPR, so triples whose subject "
					+ "is the null relative URI will usually result in triples in the created "
					+ "resource whose subject is the created resource.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-rdfnullrel",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testNullRelativeUriPost() throws URISyntaxException {
		skipIfMethodNotAllowed(HttpMethod.POST);

		Model requestModel = postContent();

		// Do not pass a URI to RdfObjectMapper so that it stays as the null
		// relative URI in the request body
		Response postResponse = buildBaseRequestSpecification()
					.contentType(TEXT_TURTLE)
					.body(requestModel, new RdfObjectMapper()) // do not pass a URI
				.expect()
					.statusCode(HttpStatus.SC_CREATED)
				.when()
					.post(getResourceUri());

		String location = postResponse.getHeader(LOCATION);
		assertNotNull(location, MSG_LOC_NOTFOUND);

		try {
			// Get the resource to check that the resource with a null relative URI
			// was assigned the URI in the Location response header.
			Model responseModel = getAsModel(location);

			// Rename the subject of the request model so it matches the received location
			// and intersect both models.
			ResourceUtils.renameResource(
					requestModel.getResource(""), getPrimaryTopic(responseModel, location).getURI());
			Model intersectionModel = requestModel.intersection(responseModel);

			// OK if the result is not empty
			assertTrue(intersectionModel.size() > 0,
					"The resource created with URI <"
							+ location
							+ "> has nothing in common with the resource POSTed using the null relative URI."
			);
		} finally {
			// Delete the resource to clean up.
			buildBaseRequestSpecification().delete(location);
		}
	}

	@Test(
			groups = {SHOULD},
			description = "LDP servers SHOULD assign the URI for the resource to be created "
					+ "using server application specific rules in the absence of a client hint.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-serverassignuri",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testPostNoSlug() {
		skipIfMethodNotAllowed(HttpMethod.POST);

		// POST content with no Slug and see if the server assigns a URI.
		Model model = postContent();
		Response postResponse = buildBaseRequestSpecification()
				.contentType(TEXT_TURTLE)
				.body(model, new RdfObjectMapper())
			 .expect()
				.statusCode(HttpStatus.SC_CREATED)
				.header(LOCATION, HeaderMatchers.headerPresent())
			.when()
				.post(getResourceUri());

		// Delete the resource to clean up.
		buildBaseRequestSpecification().delete(postResponse.getHeader(LOCATION));
	}

	@Test(
			groups = {SHOULD},
			description = "LDP servers SHOULD allow clients to create new resources without "
					+ "requiring detailed knowledge of application-specific constraints. This "
					+ "is a consequence of the requirement to enable simple creation and "
					+ "modification of LDPRs. LDP servers expose these application-specific "
					+ "constraints as described in section 4.2.1 General.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-mincontraints",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testCreateWithoutConstraints() throws URISyntaxException {
		skipIfMethodNotAllowed(HttpMethod.POST);

		// Create a resource with one statement (dcterms:title).
		Model requestModel = ModelFactory.createDefaultModel();
		Resource resource = requestModel.createResource("");
		resource.addProperty(DCTerms.title, "Created by the LDP test suite");

		Response postResponse = buildBaseRequestSpecification()
					.contentType(TEXT_TURTLE)
					.body(requestModel, new RdfObjectMapper())
				.expect()
					.statusCode(HttpStatus.SC_CREATED)
				.when()
					.post(getResourceUri());

		// Delete the resource to clean up.
		String location = postResponse.getHeader(LOCATION);
		if (location != null) {
			buildBaseRequestSpecification().delete(location);
		}

	}

	@Test(
			groups = {SHOULD},
			description = "LDP servers that allow member creation via POST SHOULD NOT re-use URIs.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-dontreuseuris",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testRestrictUriReUseNoSlug covers the rest.")
	public void testRestrictUriReUseSlug() throws URISyntaxException {
		testRestrictUriReUse("uritest");
	}

	@Test(
			groups = {SHOULD},
			description = "LDP servers that allow member creation via POST SHOULD NOT re-use URIs.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-dontreuseuris",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testRestrictUriReUseSlug covers the rest.")
	public void testRestrictUriReUseNoSlug() throws URISyntaxException {
		testRestrictUriReUse(null);
	}

	@Test(
			groups = {MUST},
			description = "LDP servers that support POST MUST include an Accept-Post "
					+ "response header on HTTP OPTIONS responses, listing post document "
					+ "media type(s) supported by the server.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-acceptposthdr",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testAcceptPostResponseHeader() {
		skipIfMethodNotAllowed(HttpMethod.POST);

		Response optionsResponse = buildBaseRequestSpecification().expect()
				.statusCode(isSuccessful()).when()
				.options(getResourceUri());
		assertNotNull(
				optionsResponse.getHeader(ACCEPT_POST),
				"The HTTP OPTIONS response on container <"
						+ getResourceUri()
						+ "> did not include an Accept-Post response header, but it lists POST in its Allow response header."
		);
	}

	@Test(
			groups = {SHOULD},
			description = "LDP servers SHOULD NOT allow HTTP PUT to update an LDPC’s "
					+ "containment triples; if the server receives such a request, it "
					+ "SHOULD respond with a 409 (Conflict) status code.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-put-mbrprops",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testRejectPutModifyingContainmentTriples() {
		String containerUri = getResourceUri();
		Response response = buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
				.expect().statusCode(isSuccessful())
				.when().get(containerUri);
		String eTag = response.getHeader(ETAG);
		Model model = response.as(Model.class, new RdfObjectMapper(containerUri));

		// Try to modify the ldp:contains triple.
		Resource containerResource = model.getResource(containerUri);
		containerResource.addProperty(model.createProperty(LDP.contains.stringValue()),
				model.createResource("#" + System.currentTimeMillis()));

		RequestSpecification putRequest = buildBaseRequestSpecification().contentType(TEXT_TURTLE);
		if (eTag != null) {
			putRequest.header(IF_MATCH, eTag);
		}
		putRequest.body(model, new RdfObjectMapper(containerUri))
				.expect().statusCode(not(isSuccessful()))
				.when().put(containerUri);
	}

	@Test(
			groups = {SHOULD},
			dependsOnMethods = {"testPutToCreate"},
			description = "LDP servers that allow LDPR creation via PUT SHOULD NOT "
					+ "re-use URIs. For RDF representations (LDP-RSs),the created "
					+ "resource can be thought of as an RDF named graph [rdf11-concepts].")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-put-create",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testRestrictPutReUseUri() {
		String location = putToCreate();

		// Delete the resource.
		buildBaseRequestSpecification()
				.expect()
				.statusCode(isSuccessful())
				.when()
				.delete(location);

		// Try to put to the same URI again. It should fail.
		Model content = postContent();
		buildBaseRequestSpecification()
				.given()
				.contentType(TEXT_TURTLE)
				.body(content, new RdfObjectMapper(location))
				.expect()
				.statusCode(not(isSuccessful()))
				.when()
				.put(location);
	}

	@Test(
			groups = {MUST},
			description = "When an LDPR identified by the object of a containment triple "
					+ "is deleted, the LDPC server MUST also remove the LDPR from the "
					+ "containing LDPC by removing the corresponding containment triple.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-del-contremovesconttriple",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testDeleteRemovesContainmentTriple() throws URISyntaxException {
		skipIfMethodNotAllowed(HttpMethod.POST);

		Model model = postContent();
		String containerUri = getResourceUri();
		Response postResponse = buildBaseRequestSpecification()
					.contentType(TEXT_TURTLE)
					.body(model, new RdfObjectMapper())
				.expect()
					.statusCode(HttpStatus.SC_CREATED)
				.when()
					.post(getResourceUri());

		// POST support is optional. Only test delete if the POST succeeded.
		if (postResponse.getStatusCode() != HttpStatus.SC_CREATED) {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					"HTTP POST failed with status " + postResponse.getStatusCode(), skipLog);
		}

		String location = postResponse.getHeader(LOCATION);
		assertNotNull(location, MSG_LOC_NOTFOUND);

		// TODO: Check if delete is supported on location.....

		// Delete the resource
		buildBaseRequestSpecification()
			.expect()
				.statusCode(isSuccessful())
			.when()
				.delete(location);

		// Test the membership triple
		Response getResponse = buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
				.header(PREFER, include(PREFER_CONTAINMENT)) // hint to the server that we want containment triples
			.expect()
				.statusCode(isSuccessful())
			.when()
				.get(containerUri);
		Model containerModel = getResponse.as(Model.class, new RdfObjectMapper(containerUri));
		Resource container = containerModel.getResource(containerUri);

		assertFalse(
				container.hasProperty(containerModel
								.createProperty(LDP.contains.stringValue()),
						containerModel.getResource(location)
				),
				"The LDPC server must remove the corresponding containment triple when an LDPR is deleted."
		);
	}

	@Test(
			groups = {SHOULD},
			description = "LDP servers are RECOMMENDED to support HTTP PATCH as the "
					+ "preferred method for updating an LDPC's empty-container triples. ")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-patch-req",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testPatchMethod() {
		assertTrue(
				supports(HttpMethod.PATCH),
				"Container <"
						+ getResourceUri()
						+ "> has not advertised PATCH support through its HTTP OPTIONS response."
		);

		// TODO: Actually try to patch the containment triples.
	}

	@Test(
			groups = {MAY},
			description = "The representation of a LDPC MAY have an rdf:type "
					+ "of ldp:Container for Linked Data Platform Container. Non-normative "
					+ "note: LDPCs might have additional types, like any LDP-RS. ")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-typecontainer",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testRdfTypeLdpContainer() {
		String container = getResourceUri();
		Model m = buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
			.expect()
				.statusCode(isSuccessful())
			.when()
				.get(container).as(Model.class, new RdfObjectMapper(container));
		assertTrue(m.contains(m.getResource(container), RDF.type, m.getResource(LDP.Container.stringValue())),
				"LDPC does not have an rdf:type of ldp:Container");
	}

	/**
	 * @see <a href="http://tools.ietf.org/html/rfc5023#page-30">RFC 5023 - The Atom Publishing Protocol: 9.7. The Slug Header</a>
	 */
	@Test(
			groups = {MAY},
			description = "LDP servers MAY allow clients to suggest "
					+ "the URI for a resource created through POST, "
					+ "using the HTTP Slug header as defined in [RFC5023]. "
					+ "LDP adds no new requirements to this usage, so its "
					+ "presence functions as a client hint to the server "
					+ "providing a desired string to be incorporated into the "
					+ "server's final choice of resource URI.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-slug",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testServerHonorsSlug() {
		skipIfMethodNotAllowed(HttpMethod.POST);

		// Come up with a (probably) unique slug header. Try to find one that
		// the server will accept unchanged, so avoid special characters or very
		// long strings.
		String slug = RandomStringUtils.randomAlphabetic(6);
		Model content = postContent();
		String location = post(content, slug);

		try {
			// Per RFC 5023, the server may change the slug so this isn't a
			// perfect test. We can at least ignore case when looking at the
			// return location.
			assertTrue(location.toLowerCase().contains(slug.toLowerCase()), "Slug is not part of the return Location");
		} finally {
			// Clean up.
			buildBaseRequestSpecification().delete(location);
		}
	}

	@Test(
			groups = {MUST},
			description = "LDP servers SHOULD accept a request entity "
					+ "body with a request header of Content-Type with "
					+ "value of application/ld+json [JSON-LD].")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-jsonld",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testPostJsonLd() {
		skipIfMethodNotAllowed(HttpMethod.POST);

		// POST content as JSON-LD.
		Model model = postContent();
		Response postResponse = buildBaseRequestSpecification()
				.contentType(APPLICATION_LD_JSON)
				.body(model, new RdfObjectMapper())
			.expect()
				.statusCode(HttpStatus.SC_CREATED)
				.header(LOCATION, HeaderMatchers.headerPresent())
			.when()
				.post(getResourceUri());

		// Get the resource back to make sure it wasn't simply treated as a
		// binary resource. We should be able to get it as text/turtle.
		final String location = postResponse.getHeader(LOCATION);
		Response getResponse = buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
			.expect()
				.statusCode(isSuccessful())
				.contentType(HeaderMatchers.isTurtleCompatibleContentType())
			.when()
				.get(location);
		assertFalse(
				containsLinkHeader(
						location,
						LINK_REL_TYPE,
						LDP.NonRDFSource.stringValue(),
						location,
						getResponse
				),
				"Resources POSTed using JSON-LD should be treated as RDF source");
	}

	@Test(
			groups = {MUST},
			description = "Each Linked Data Platform Container MUST "
					+ "also be a conforming Linked Data Platform RDF Source.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-isldpr",
			testMethod = METHOD.INDIRECT,
			approval = STATUS.WG_APPROVED,
			coveredByTests = {RdfSourceTest.class},
			coveredByGroups = {MUST})
	public void testConformsContainerRdfResource() {
		throw new org.testng.SkipException("Covered indirectly by the MUST tests defined in RdfSourceTest class");
	}

	@Override
	protected boolean restrictionsOnTestResourceContent() {
		// Always true for containers because their containment triples can't be
		// modified via PUT.
		return true;
	}

	/**
	 * Tests that LDP servers do not reuse URIs after deleting resources.
	 *
	 * @param slug the slug header for the request or null if no slug
	 * @throws URISyntaxException on bad URIs
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
		buildBaseRequestSpecification().expect().statusCode(isSuccessful()).when().delete(loc1);

		String loc2 = post(content, slug);
		try {
			assertNotEquals(loc1, loc2, "Server reused URIs for POSTed resources.");
		} finally {
			buildBaseRequestSpecification().delete(loc2);
		}
	}

	private String post(Model content, String slug) {
		RequestSpecification spec = buildBaseRequestSpecification().contentType(TEXT_TURTLE);
		if (slug != null) {
			spec.header(SLUG, slug);
		}
		Response post = spec.body(content, new RdfObjectMapper()).expect()
				.statusCode(HttpStatus.SC_CREATED).when()
				.post(getResourceUri());
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
		Response response = buildBaseRequestSpecification().contentType(TEXT_TURTLE)
				.body(model, new RdfObjectMapper("")).expect()
				.statusCode(HttpStatus.SC_CREATED).when().put(target);

		String location = response.getHeader(LOCATION);
		if (location == null) {
			return target.toString();
		}

		return location;
	}

}
