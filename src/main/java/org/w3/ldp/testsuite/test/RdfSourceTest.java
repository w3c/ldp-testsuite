package org.w3.ldp.testsuite.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.w3.ldp.testsuite.matcher.HeaderMatchers.isValidEntityTag;
import static org.w3.ldp.testsuite.matcher.HttpStatus4xxRangeMatcher.is4xxRange;
import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.HttpStatus;
import org.testng.SkipException;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.annotations.SpecTest.STATUS;
import org.w3.ldp.testsuite.exception.SkipClientTestException;
import org.w3.ldp.testsuite.exception.SkipNotTestableException;
import org.w3.ldp.testsuite.http.HttpMethod;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;
import org.w3.ldp.testsuite.matcher.HeaderMatchers;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.jayway.restassured.response.Response;

/**
 * Tests all RDF source LDP resources, including containers and member resources.
 */
public abstract class RdfSourceTest extends CommonResourceTest {

	private static final String MSG_NO_READ_ONLY_PROPERTY = "Skipping test because we have no read-only properties to PUT."
			+ " Server-managed properties are specified using the \"read-only-prop\" command-line parameter.";
	private static final String MSG_PUT_RESTRICTIONS = "Skipping test because there are restrictions on PUT content for this resource. "
			+ "The requirement needs to be tested manually.";

	private static final String UNKNOWN_PROPERTY = "http://example.com/ns#comment";

	/** A relative URI to use for testing. */
	protected static final String RELATIVE_URI = "relatedResource";

	@Parameters("auth")
	public RdfSourceTest(@Optional String auth) throws IOException {
		super(auth);
	}

	@Test(
			groups = {MUST},
			description = "LDP servers MUST assign the default base-URI "
					+ "for [RFC3987] relative-URI resolution to be the HTTP "
					+ "Request-URI when the resource already exists, and to "
					+ "the URI of the created resource when the request results "
					+ "in the creation of a new resource.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-defbaseuri",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_PENDING)
	public void testRelativeUriResolutionPut() {
		skipIfMethodNotAllowed(HttpMethod.PUT);

		if (restrictionsOnTestResourceContent()) {
			throw new SkipException(MSG_PUT_RESTRICTIONS);
		}

		String resourceUri = getResourceUri();
		Response response = buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
			.expect()
				.statusCode(isSuccessful())
				.header(ETAG, isValidEntityTag())
			.when()
				.get(resourceUri);

		String eTag = response.getHeader(ETAG);
		Model model = response.as(Model.class, new RdfObjectMapper(resourceUri));

		// Add a statement with a relative URI.
		model.getResource(resourceUri).addProperty(DCTerms.relation, model.getResource(RELATIVE_URI));

		// Put the resource back using relative URIs.
		Response put = buildBaseRequestSpecification()
				.contentType(TEXT_TURTLE).header(IF_MATCH, eTag)
				.body(model, new RdfObjectMapper("")) // keep URIs relative
				.when().put(resourceUri);
		if (!isSuccessful().matches(put.getStatusCode())) {
			throw new SkipException("Cannot verify relative URI resolution because the PUT request failed. Skipping test.");
		}

		// Get the resource again to verify its content.
		model = buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
			.expect()
				.statusCode(isSuccessful())
			.when()
				.get(resourceUri).as(Model.class, new RdfObjectMapper(resourceUri));

		// Verify the change.
		String relationAbsoluteUri = resolveIfRelative(resourceUri, RELATIVE_URI);
		assertTrue(
				model.contains(
						model.getResource(resourceUri),
						DCTerms.relation,
						model.getResource(relationAbsoluteUri)
				),
				"Response does not have expected triple: <" + resourceUri + "> dcterms:relation <" + relationAbsoluteUri + ">."
		);
	}

	@Test(
			groups = {MUST},
			description = "If a HTTP PUT is accepted on an existing resource, "
					+ "LDP servers MUST replace the entire persistent state of "
					+ "the identified resource with the entity representation "
					+ "in the body of the request.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-put-replaceall",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testPutReplacesResource() {
		skipIfMethodNotAllowed(HttpMethod.PUT);

		if (restrictionsOnTestResourceContent()) {
			throw new SkipException(MSG_PUT_RESTRICTIONS);
		}

		// TODO: Is there a better way to test this requirement?
		String resourceUri = getResourceUri();
		Response response = buildBaseRequestSpecification()
						.header(ACCEPT, TEXT_TURTLE)
				.expect()
						.statusCode(isSuccessful())
						.header(ETAG, isValidEntityTag())
				.when()
						.get(resourceUri);

		String eTag = response.getHeader(ETAG);
		Model originalModel = response.as(Model.class, new RdfObjectMapper(resourceUri));

		// Replace the resource with something different.
		Model differentContent = ModelFactory.createDefaultModel();
		final String UPDATED_TITLE = "This resources content has been replaced";
		differentContent.add(differentContent.getResource(resourceUri), DCTerms.title, UPDATED_TITLE);

		response = buildBaseRequestSpecification()
					.contentType(TEXT_TURTLE)
					.header(IF_MATCH, eTag)
					.body(differentContent, new RdfObjectMapper(resourceUri)) // relative URI
				.when()
					.put(resourceUri);
		if (!isSuccessful().matches(response.getStatusCode())) {
			throw new SkipException("Skipping test because the PUT failed. The server may have restrictions on its content.");
		}

		// Get the resource again to see what's there.
		response = buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
			.expect()
				.statusCode(isSuccessful())
				.header(ETAG, isValidEntityTag())
			.when()
				.get(resourceUri);
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
		buildBaseRequestSpecification()
				.contentType(TEXT_TURTLE).header(IF_MATCH, eTag)
				.body(originalModel, new RdfObjectMapper(resourceUri)) // relative URI
				.expect().statusCode(isSuccessful())
				.when().put(resourceUri);
	}

	@Override
	@Test(
			groups = {MUST},
			description = "LDP servers MUST provide an RDF representation "
					+ "for LDP-RSs. The HTTP Request-URI of the LDP-RS is "
					+ "typically the subject of most triples in the response.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-gen-rdf",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testGetResource() {
		// Make sure we can get the resource itself and the response is
		// valid RDF. Turtle is a required media type, so this request
		// should succeed for all LDP-RS.
		buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
				.expect().statusCode(HttpStatus.SC_OK).contentType(TEXT_TURTLE)
				.when().get(getResourceUri()).as(Model.class, new RdfObjectMapper(getResourceUri()));
	}

	@Test(
			groups = {SHOULD},
			description = "LDP-RSs representations SHOULD have at least one "
					+ "rdf:type set explicitly. This makes the representations"
					+ " much more useful to client applications that don’t "
					+ "support inferencing.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-gen-atleast1rdftype",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testContainsRdfType() {
		Model containerModel = getAsModel(getResourceUri());
		Resource r = containerModel.getResource(getResourceUri());
		assertTrue(r.hasProperty(RDF.type), "LDP-RS representation has no explicit rdf:type");
	}

	@Test(
			groups = {SHOULD},
			description = "LDP-RSs SHOULD reuse existing vocabularies instead of "
					+ "creating their own duplicate vocabulary terms. In addition "
					+ "to this general rule, some specific cases are covered by "
					+ "other conformance rules.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-gen-reusevocab",
			testMethod = METHOD.MANUAL,
			approval = STATUS.WG_APPROVED)
	public void testReUseVocabularies() {
		throw new SkipNotTestableException();
	}

	@Test(
			groups = {SHOULD},
			description = "LDP-RSs predicates SHOULD use standard vocabularies such "
					+ "as Dublin Core [DC-TERMS], RDF [rdf11-concepts] and RDF "
					+ "Schema [rdf-schema], whenever possible.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-gen-reusevocabsuchas",
			testMethod = METHOD.MANUAL,
			approval = STATUS.WG_APPROVED)
	public void testUseStandardVocabularies() throws URISyntaxException {
		// TODO: Consider ideas for testUseStandardVocabularies (see comment)
		/* Possible ideas:
		   fetch resource, look for known vocabulary term
		   URIs.  Also can look at total number of terms and have a threshold
		   of differences, say 100 terms and < 5 standard predicates would be odd.
		   Also could look for similar/like short predicate short names or even
		   look for owl:sameAs.
		*/
		throw new SkipNotTestableException();
	}

	@Test(
			groups = {MUST},
			description = "In the absence of special knowledge of the application "
					+ "or domain, LDP clients MUST assume that any LDP-RS can "
					+ "have multiple values for rdf:type.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldp-cli-multitype",
			testMethod = METHOD.CLIENT_ONLY,
			approval = STATUS.WG_APPROVED)
	public void testAllowMultipleRdfTypes() {
		throw new SkipClientTestException();
	}

	@Test(
			groups = {MUST},
			description = "In the absence of special knowledge of the "
					+ "application or domain, LDP clients MUST assume "
					+ "that the rdf:type values of a given LDP-RS can "
					+ "change over time.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-cli-typeschange",
			testMethod = METHOD.CLIENT_ONLY,
			approval = STATUS.WG_APPROVED)
	public void testChangeRdfTypeValue() {
		throw new SkipClientTestException();
	}

	@Test(
			groups = {SHOULD},
			description = "LDP clients SHOULD always assume that the set "
					+ "of predicates for a LDP-RS of a particular type at "
					+ "an arbitrary server is open, in the sense that different "
					+ "resources of the same type may not all have the same set "
					+ "of predicates in their triples, and the set of predicates "
					+ "that are used in the state of any one LDP-RS is not limited "
					+ "to any pre-defined set.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-cli-openpreds",
			testMethod = METHOD.CLIENT_ONLY,
			approval = STATUS.WG_APPROVED)
	public void testServerOpen() {
		throw new SkipClientTestException();
	}

	@Test(
			groups = {MUST},
			description = "LDP servers MUST NOT require LDP clients to implement inferencing "
					+ "in order to recognize the subset of content defined by LDP. Other "
					+ "specifications built on top of LDP may require clients to implement "
					+ "inferencing [rdf11-concepts]. The practical implication is that all "
					+ "content defined by LDP must be explicitly represented, unless noted "
					+ "otherwise within this document.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-gen-noinferencing",
			testMethod = METHOD.MANUAL,
			approval = STATUS.WG_APPROVED)
	public void testRestrictClientInference() {
		throw new SkipNotTestableException();
	}

	@Test(
			groups = {MUST},
			description = "A LDP client MUST preserve all triples retrieved "
					+ "from an LDP-RS using HTTP GET that it doesn’t change "
					+ "whether it understands the predicates or not, when its "
					+ "intent is to perform an update using HTTP PUT. The use of "
					+ "HTTP PATCH instead of HTTP PUT for update avoids this "
					+ "burden for clients [RFC5789]. ")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-cli-preservetriples",
			testMethod = METHOD.MANUAL,
			approval = STATUS.WG_APPROVED)
	public void testGetResourcePreservesTriples() throws URISyntaxException {
		throw new SkipNotTestableException();
	}

	@Test(
			groups = {MUST},
			description = "LDP clients MUST be capable of processing responses "
					+ "formed by an LDP server that ignores hints, including "
					+ "LDP-defined hints.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-cli-hints-ignorable",
			testMethod = METHOD.MANUAL,
			approval = STATUS.WG_APPROVED)
	public void testAllowResponsesFromServer() {
		throw new SkipNotTestableException();
	}

	@Test(
			groups = {MUST},
			description = "LDP servers must provide a text/turtle representation "
					+ "of the requested LDP-RS whenever HTTP content negotiation "
					+ "does not force another outcome [turtle]. In other words, if "
					+ "the server receives a GET request whose Request-URI "
					+ "identifies a LDP-RS, and either text/turtle has the highest "
					+ "relative quality factor (q= value) in the Accept request "
					+ "header or that header is absent, then an LDP server has to "
					+ "respond with Turtle.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-get-turtle",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testGetResourceAcceptTurtle() {
		// Accept: text/turtle
		buildBaseRequestSpecification().header(ACCEPT, TEXT_TURTLE)
				.expect().statusCode(isSuccessful()).contentType(TEXT_TURTLE)
				.when().get(getResourceUri()).as(Model.class, new RdfObjectMapper(getResourceUri()));

		// No Accept header
		buildBaseRequestSpecification()
				.expect().statusCode(isSuccessful()).contentType(TEXT_TURTLE)
				.when().get(getResourceUri()).as(Model.class, new RdfObjectMapper(getResourceUri()));

		// Wildcard
		buildBaseRequestSpecification().header(ACCEPT, "*/*")
				.expect().statusCode(isSuccessful()).contentType(TEXT_TURTLE)
				.when().get(getResourceUri()).as(Model.class, new RdfObjectMapper(getResourceUri()));

		// Accept: text/*
		buildBaseRequestSpecification().header(ACCEPT, "text/*")
				.expect().statusCode(isSuccessful()).contentType(TEXT_TURTLE)
				.when().get(getResourceUri()).as(Model.class, new RdfObjectMapper(getResourceUri()));

		// More complicated Accept header
		buildBaseRequestSpecification().header(ACCEPT, "text/turtle;q=0.9,application/json;q=0.8")
				.expect().statusCode(isSuccessful()).contentType(TEXT_TURTLE)
				.when().get(getResourceUri()).as(Model.class, new RdfObjectMapper(getResourceUri()));
	}

	/**
	 * This is a client-only test. Server tests are covered by
	 * {@link CommonContainerTest#testPreferContainmentTriples()} and
	 * {@link DirectContainerTest#testPreferMembershipTriples()}.
	 */
	@Test(
			enabled = false,
			groups = {MAY},
			description = "LDP clients MAY provide LDP-defined hints that allow servers "
					+ "to optimize the content of responses. section 7.2 Preferences on "
					+ "the Prefer Request Header defines hints that apply to LDP-RSs. ")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-cli-can-hint",
			testMethod = METHOD.CLIENT_ONLY,
			approval = STATUS.WG_PENDING)
	public void testClientMayProvideHints() {
		throw new SkipClientTestException();
	}

	@Test(
			groups = {SHOULD},
			description = "LDP servers SHOULD offer a application/ld+json representation"
					+ " of the requested LDP-RS [JSON-LD]. ")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-get-jsonld",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_PENDING)
	public void testJsonLdRepresentation() throws IOException, JsonLdError {
		Response response = buildBaseRequestSpecification()
				.header(ACCEPT, "application/ld+json, application/json;q=0.5")
				.expect()
				.statusCode(isSuccessful())
				.contentType(HeaderMatchers.isJsonLdCompatibleContentType())
				.when()
				.get(getResourceUri());

		// Make sure it parses as JSON-LD.
		Object json = JsonUtils.fromInputStream(response.asInputStream());
		JsonLdProcessor.toRDF(json); // throws JsonLdError if not valid
	}

	@Test(
			groups = {MUST},
			description = "LDP servers MUST publish any constraints on LDP clients’ "
					+ "ability to create or update LDPRs, by adding a Link header "
					+ "with rel='describedby' [RFC5988] to all responses to requests "
					+ "which fail due to violation of those constraints.")
	@Parameters({ "readOnlyProp" })
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-pubclireqs",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_PENDING)
	public void testPublishConstraintsReadOnlyProp(@Optional String readOnlyProp) {
		skipIfMethodNotAllowed(HttpMethod.PUT);

		if (readOnlyProp == null) {
			throw new SkipException(MSG_NO_READ_ONLY_PROPERTY);
		}

		expectPut4xxDescriedBy(readOnlyProp);
	}

	@Test(
			groups = {MUST},
			description = "LDP servers MUST publish any constraints on LDP clients’ "
					+ "ability to create or update LDPRs, by adding a Link header "
					+ "with rel='describedby' [RFC5988] to all responses to requests "
					+ "which fail due to violation of those constraints.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-pubclireqs",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_PENDING)
	public void testPublishConstraintsUnknownProp() {
		skipIfMethodNotAllowed(HttpMethod.PUT);
		expectPut4xxDescriedBy(UNKNOWN_PROPERTY);
	}


	@Test(
			enabled = false,
			groups = {MUST},
			description = "If an otherwise valid HTTP PUT request is received that "
					+ "attempts to change properties the server does not allow "
					+ "clients to modify, LDP servers MUST respond with a 4xx range "
					+ "status code (typically 409 Conflict)")
	@Parameters("readOnlyProp")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-put-servermanagedprops",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_PENDING)
	public void testPutReadOnlyProperties4xxStatus(@Optional String readOnlyProp) {
		skipIfMethodNotAllowed(HttpMethod.PUT);

		if (readOnlyProp == null) {
			throw new SkipException(MSG_NO_READ_ONLY_PROPERTY);
		}

		expectPut4xxStatus(readOnlyProp);
	}

	@Test(
			groups = {SHOULD},
			description = "LDP servers SHOULD provide a corresponding response body containing "
					+ "information about which properties could not be persisted. The "
					+ "format of the 4xx response body is not constrained by LDP.")
	@Parameters("readOnlyProp")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-put-servermanagedprops",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_PENDING)
	public void test4xxErrorHasResponseBody(@Optional String readOnlyProp) {
		skipIfMethodNotAllowed(HttpMethod.PUT);

		if (readOnlyProp == null) {
			throw new SkipException(MSG_NO_READ_ONLY_PROPERTY);
		}

		expectPut4xxResponseBody(readOnlyProp);
	}

	@Test(
			groups = {MUST},
			description = "If an otherwise valid HTTP PUT request is received that "
					+ "contains properties the server chooses not to persist, "
					+ "e.g. unknown content, LDP servers MUST respond with an "
					+ "appropriate 4xx range status code [HTTP11].")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-put-failed",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_PENDING)
	public void testPutPropertiesNotPersisted() {
		skipIfMethodNotAllowed(HttpMethod.PUT);
		expectPut4xxStatus(UNKNOWN_PROPERTY);
	}

	@Test(
			groups = {SHOULD},
			description = "LDP servers SHOULD provide a corresponding response body containing "
					+ "information about which properties could not be persisted. The "
					+ "format of the 4xx response body is not constrained by LDP. LDP "
					+ "servers expose these application-specific constraints as described "
					+ "in section 4.2.1 General.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-put-failed",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_PENDING)
	public void testResponsePropertiesNotPersisted() {
		skipIfMethodNotAllowed(HttpMethod.PUT);
		expectPut4xxResponseBody(UNKNOWN_PROPERTY);
	}

	protected void modifyProperty(Model m, String resourceUri, String property) {
		Resource r = m.getResource(resourceUri);
		Property p = m.createProperty(property);
		r.removeAll(p);
		// Don't sweat the value or datatype since we expect the PUT to fail anyway.
		r.addProperty(p, "modified");
	}

	protected void expectPut4xxDescriedBy(String invalidProp) {
		Response putResponse = expectPut4xxStatus(invalidProp);
		String describedby = getFirstLinkForRelation(LINK_REL_DESCRIBEDBY, getResourceUri(), putResponse);
		assertNotNull(describedby, "Response did not contain a Link header with rel=\"describedby\"");

		// Make sure we can GET the describedby link.
		buildBaseRequestSpecification()
			.expect()
				.statusCode(isSuccessful())
			.when()
				.get(describedby);
	}

	protected Response expectPut4xxStatus(String invalidProp) {
		// Get the resource.
		String resourceUri = getResourceUri();
		Response getResponse = buildBaseRequestSpecification()
			.expect()
				.statusCode(isSuccessful())
				.header(ETAG, isValidEntityTag())
			.when()
				.get(resourceUri);

		String eTag = getResponse.getHeader(ETAG);
		Model m = getResponse.as(Model.class, new RdfObjectMapper(resourceUri));
		modifyProperty(m, resourceUri, invalidProp);

		Response putResponse = buildBaseRequestSpecification()
				.contentType(TEXT_TURTLE)
				.header(IF_MATCH, eTag)
				.body(m, new RdfObjectMapper(resourceUri))
			.when()
				.put(resourceUri);
		if (isSuccessful().matches(putResponse.getStatusCode())) {
			throw new SkipException("Skipping test because PUT request was successful.");
		}

		assertThat(putResponse.statusCode(), is4xxRange());

		return putResponse;
	}

	protected void expectPut4xxResponseBody(String invalidProp) {
		Response putResponse = expectPut4xxStatus(invalidProp);
		assertThat(putResponse.body().asString(), not(isEmptyOrNullString()));
	}

	/**
	 * Are there any restrictions on the content of the resource being tested
	 * (i.e., the resource returned by {@link #getResourceUri()}). Should be
	 * true for LDP containers since PUT can't directly modify containment
	 * triples.
	 *
	 * <p>
	 * This method is used for {@link #testPutReplacesResource()} and
	 * {@link #testRelativeUriResolutionPut()}.
	 * </p>
	 *
	 * @return true if there are restrictions on what triples are allowed; false
	 *         otherwise
	 * @see #setPostContent(String)
	 * @see #restrictionsOnPostContent()
	 */
	protected boolean restrictionsOnTestResourceContent() {
		// Should be the same as POST content unless this is a container.
		// (Overridden by subclasses as necessary.)
		return restrictionsOnPostContent();
	}
}
