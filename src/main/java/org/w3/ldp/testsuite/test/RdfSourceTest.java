package org.w3.ldp.testsuite.test;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.apache.marmotta.commons.vocabulary.LDP;
import org.jboss.resteasy.spi.Failure;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.annotations.SpecTest.STATUS;
import org.w3.ldp.testsuite.exception.SkipException;
import org.w3.ldp.testsuite.exception.SkipNotTestableException;
import org.w3.ldp.testsuite.http.HttpMethod;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;
import org.w3.ldp.testsuite.matcher.HeaderMatchers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.testng.Assert.*;
import static org.w3.ldp.testsuite.http.HttpHeaders.*;
import static org.w3.ldp.testsuite.http.MediaTypes.TEXT_TURTLE;
import static org.w3.ldp.testsuite.matcher.HeaderMatchers.isValidEntityTag;
import static org.w3.ldp.testsuite.matcher.HttpStatus4xxRangeMatcher.is4xxRange;
import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

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
	@Parameters("relativeUri")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-defbaseuri",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testRelativeUriResolutionPut(@Optional String relativeUri) {
		skipIfMethodNotAllowed(HttpMethod.PUT);

		if (restrictionsOnTestResourceContent()) {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					MSG_PUT_RESTRICTIONS, skipLog);
		}

		if (relativeUri == null) {
			relativeUri = RELATIVE_URI;
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
		getPrimaryTopic(model, resourceUri).addProperty(DCTerms.relation, model.getResource(relativeUri));

		// Put the resource back using relative URIs.
		Response put = buildBaseRequestSpecification()
				.contentType(TEXT_TURTLE).header(IF_MATCH, eTag)
				.body(model, new RdfObjectMapper("")) // keep URIs relative
				.when().put(resourceUri);
		if (!isSuccessful().matches(put.getStatusCode())) {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					"Cannot verify relative URI resolution because the PUT request failed. Skipping test.",
					skipLog);
		}

		// Get the resource again to verify its content.
		model = buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
			.expect()
				.statusCode(isSuccessful())
			.when()
				.get(resourceUri).as(Model.class, new RdfObjectMapper(resourceUri));

		// Verify the change.
		String relationAbsoluteUri = resolveIfRelative(resourceUri, relativeUri);
		assertTrue(
				model.contains(
						getPrimaryTopic(model, resourceUri),
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
		putReplaceResource(true);
	}

	@Test(
			groups = {MUST},
			description = "LDP servers SHOULD allow clients to update resources "
					+ "without requiring detailed knowledge of server-specific "
					+ "constraints. This is a consequence of the requirement to "
					+ "enable simple creation and modification of LDPRs.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-put-simpleupdate",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testPutSimpleUpdate() {
		putReplaceResource(false);
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
				.expect().statusCode(HttpStatus.SC_OK).contentType(HeaderMatchers.isTurtleCompatibleContentType())
				.when().get(getResourceUri()).as(Model.class, new RdfObjectMapper(getResourceUri()));
	}

	@Test(
			groups = {SHOULD},
			description = "LDP-RSs representations SHOULD have at least one "
					+ "rdf:type set explicitly. This makes the representations "
					+ "much more useful to client applications that don’t "
					+ "support inferencing.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-gen-atleast1rdftype",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testContainsRdfType() {
		Model containerModel = getAsModel(getResourceUri());
		Resource r = getPrimaryTopic(containerModel, getResourceUri());
		assertTrue(r.hasProperty(RDF.type), "LDP-RS representation has no explicit rdf:type");
	}

	@Test(
			groups = {MAY},
			description = "The representation of a LDP-RS MAY have an rdf:type "
					+ "of ldp:RDFSource for Linked Data Platform RDF Source.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-rdftype",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testTypeRdfSource() {
		Model containerModel = getAsModel(getResourceUri());
		Resource r = containerModel.getResource(getResourceUri());
		assertTrue(
				r.hasProperty(
						RDF.type,
						containerModel.createResource(LDP.RDFSource.stringValue())
				),
				"LDP-RS representation does not have rdf:type ldp:RDFSource");
	}

	@Test(
			groups = {SHOULD, MANUAL},
			description = "LDP-RSs SHOULD reuse existing vocabularies instead of "
					+ "creating their own duplicate vocabulary terms. In addition "
					+ "to this general rule, some specific cases are covered by "
					+ "other conformance rules.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-gen-reusevocab",
			testMethod = METHOD.MANUAL,
			approval = STATUS.WG_APPROVED,
			steps = {"Given a URL for a RDF Source, perform a GET using an RDF content type",
				"Inspect the content to ensure standard terms are used.  For example, if things like "
				+ "ex:label or ex:title are used, instead of DCTERMS or RDFS, then the test "
				+ "should fail."})
	public void testReUseVocabularies() {
		throw new SkipNotTestableException(Thread.currentThread().getStackTrace()[1].getMethodName(), skipLog);
	}

	@Test(
			groups = {SHOULD, MANUAL},
			description = "LDP-RSs predicates SHOULD use standard vocabularies such "
					+ "as Dublin Core [DC-TERMS], RDF [rdf11-concepts] and RDF "
					+ "Schema [rdf-schema], whenever possible.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-gen-reusevocabsuchas",
			testMethod = METHOD.MANUAL,
			approval = STATUS.WG_APPROVED,
			steps = {"Given a URL for a RDF Source, perform a GET using an RDF content type",
					"Inspect the content to ensure standard terms are used.  For example, if things like "
					+ "ex:label or ex:title are used, instead of DCTERMS or RDFS, then the test "
					+ "should fail."})
	public void testUseStandardVocabularies() throws URISyntaxException {
		// TODO: Consider ideas for testUseStandardVocabularies (see comment)
		/* Possible ideas:
		   fetch resource, look for known vocabulary term
		   URIs.  Also can look at total number of terms and have a threshold
		   of differences, say 100 terms and < 5 standard predicates would be odd.
		   Also could look for similar/like short predicate short names or even
		   look for owl:sameAs.
		*/
		throw new SkipNotTestableException(Thread.currentThread().getStackTrace()[1].getMethodName(), skipLog);
	}

	@Test(
			groups = {MUST, MANUAL},
			description = "LDP servers MUST NOT require LDP clients to implement inferencing "
					+ "in order to recognize the subset of content defined by LDP. Other "
					+ "specifications built on top of LDP may require clients to implement "
					+ "inferencing [rdf11-concepts]. The practical implication is that all "
					+ "content defined by LDP must be explicitly represented, unless noted "
					+ "otherwise within this document.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-gen-noinferencing",
			testMethod = METHOD.MANUAL,
			approval = STATUS.WG_APPROVED,
			steps = {"Perform a GET on a URL for the reource requesting an RDF content type",
					"Inspect the results (both headers and content) for missing terms.  Additionally "
					+ "could run an inferencing tool and compare results, seeing if needed information"
					+ "should have been explicitly listed by the server."})
	public void testRestrictClientInference() {
		throw new SkipNotTestableException(Thread.currentThread().getStackTrace()[1].getMethodName(), skipLog);
	}

	@Test(
			groups = {MUST},
			description = "LDP servers must respond with a Turtle representation of the "
					+ "requested LDP-RS when the request includes an Accept header specifying "
					+ "text/turtle, unless HTTP content negotiation requires a different outcome [turtle].")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-get-turtle",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testGetResourceAcceptTurtle() {
		// Accept: text/turtle
		buildBaseRequestSpecification().header(ACCEPT, TEXT_TURTLE)
				.expect().statusCode(isSuccessful()).contentType(HeaderMatchers.isTurtleCompatibleContentType())
				.when().get(getResourceUri()).as(Model.class, new RdfObjectMapper(getResourceUri()));

		// More complicated Accept header
		buildBaseRequestSpecification().header(ACCEPT, "text/turtle;q=0.9,application/json;q=0.8")
				.expect().statusCode(isSuccessful()).contentType(HeaderMatchers.isTurtleCompatibleContentType())
				.when().get(getResourceUri()).as(Model.class, new RdfObjectMapper(getResourceUri()));
	}
	
	@Test(
			groups = {SHOULD},
			description = "LDP servers should respond with a text/turtle representation of the "
					+ "requested LDP-RS whenever the Accept request header is absent [turtle].")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-get-conneg",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testGetResourceAsTurtleNoAccept() {
		// No Accept header
		buildBaseRequestSpecification()
				.expect().statusCode(isSuccessful()).contentType(HeaderMatchers.isTurtleCompatibleContentType())
				.when().get(getResourceUri()).as(Model.class, new RdfObjectMapper(getResourceUri()));
	}

	@Test(
			groups = {MUST},
			description = " LDP servers must respond with a application/ld+json "
					+ "representation of the requested LDP-RS when the request "
					+ "includes an Accept header, unless content negotiation or "
					+ "Turtle support requires a different outcome [JSON-LD].")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-get-jsonld",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
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
					+ "with rel='http://www.w3.org/ns/ldp#constrainedBy' [RFC5988] "
					+ "to all responses to requests which fail due to violation of "
					+ "those constraints.")
	@Parameters({ "readOnlyProp" })
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-pubclireqs",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testPublishConstraintsUnknownProp covers the rest.")
	public void testPublishConstraintsReadOnlyProp(@Optional String readOnlyProp) {
		skipIfMethodNotAllowed(HttpMethod.PUT);

		if (readOnlyProp == null) {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					MSG_NO_READ_ONLY_PROPERTY, skipLog);
		}

		expectPut4xxConstrainedBy(readOnlyProp);
	}

	@Test(
			groups = {MUST},
			description = "LDP servers MUST publish any constraints on LDP clients’ "
					+ "ability to create or update LDPRs, by adding a Link header "
					+ "with rel='http://www.w3.org/ns/ldp#constrainedBy' [RFC5988] "
					+ "to all responses to requests which fail due to violation of "
					+ "those constraints.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-pubclireqs",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testPublishConstraintsReadOnlyProp covers the rest.")
	public void testPublishConstraintsUnknownProp() {
		skipIfMethodNotAllowed(HttpMethod.PUT);
		expectPut4xxConstrainedBy(UNKNOWN_PROPERTY);
	}


	@Test(
			groups = {MUST},
			description = "If an otherwise valid HTTP PUT request is received that "
					+ "attempts to change properties the server does not allow "
					+ "clients to modify, LDP servers MUST respond with a 4xx range "
					+ "status code (typically 409 Conflict)")
	@Parameters("readOnlyProp")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-put-servermanagedprops",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "test4xxErrorHasResponseBody covers the rest.")
	public void testPutReadOnlyProperties4xxStatus(@Optional String readOnlyProp) {
		skipIfMethodNotAllowed(HttpMethod.PUT);

		if (readOnlyProp == null) {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					MSG_NO_READ_ONLY_PROPERTY, skipLog);
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
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testPutReadOnlyProperties4xxStatus covers the rest.")
	public void test4xxErrorHasResponseBody(@Optional String readOnlyProp) {
		skipIfMethodNotAllowed(HttpMethod.PUT);

		if (readOnlyProp == null) {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					MSG_NO_READ_ONLY_PROPERTY, skipLog);
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
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testResponsePropertiesNotPersisted covers the rest.")
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
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testPutPropertiesNotPersisted covers the rest.")
	public void testResponsePropertiesNotPersisted() {
		skipIfMethodNotAllowed(HttpMethod.PUT);
		expectPut4xxResponseBody(UNKNOWN_PROPERTY);
	}

	@Test(
			groups = {MUST},
			description = "Each LDP RDF Source MUST also be a conforming LDP "
					+ "Resource as defined in section 4.2 Resource, along "
					+ "with the restrictions in this section.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-are-ldpr",
			testMethod = METHOD.INDIRECT,
			approval = STATUS.WG_APPROVED,
			coveredByTests = {CommonResourceTest.class},
			coveredByGroups = {MUST})
	public void testConformsRdfSourceLdpResource() {
		throw new org.testng.SkipException("Covered indirectly by the MUST tests defined in CommonResourceTest class");
	}

	protected void modifyProperty(Model m, String resourceUri, String property) {
		Resource r = getPrimaryTopic(m, resourceUri);
		Property p = m.createProperty(property);
		r.removeAll(p);
		// Don't sweat the value or datatype since we expect the PUT to fail anyway.
		r.addProperty(p, "modified");
	}

	protected void expectPut4xxConstrainedBy(String invalidProp) {
		Response putResponse = expectPut4xxStatus(invalidProp);
		final String uri = getResourceUri();
		String constrainedBy = getFirstLinkForRelation(uri, LINK_REL_CONSTRAINEDBY, uri, putResponse);
		assertNotNull(constrainedBy, "Response did not contain a Link header with rel=\"http://www.w3.org/ns/ldp#constrainedBy\"");

		try {
			final URI linkUri = new URI(constrainedBy);

			if (linkUri.getScheme().startsWith("http")) {
				// Make sure we can GET the constrainedBy link.
				buildBaseRequestSpecification()
						.expect()
							.statusCode(isSuccessful())
						.when()
							.get(constrainedBy);
			}
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	protected Response expectPut4xxStatus(String invalidProp) {
		// Get the resource.
		String resourceUri = getResourceUri();
		Response getResponse = buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
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
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					"Skipping test because PUT request was successful.", skipLog);
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
	 * {@link #testRelativeUriResolutionPut(String)}.
	 * </p>
	 *
	 * @return true if there are restrictions on what triples are allowed; false
	 *         otherwise

	 * @see #restrictionsOnPostContent()
	 */
	protected boolean restrictionsOnTestResourceContent() {
		// Should be the same as POST content unless this is a container.
		// (Overridden by subclasses as necessary.)
		return restrictionsOnPostContent();
	}

	protected void putReplaceResource(boolean continueOnError) {
		skipIfMethodNotAllowed(HttpMethod.PUT);

		if (restrictionsOnTestResourceContent()) {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					MSG_PUT_RESTRICTIONS, skipLog);
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
		Model originalModel = response.as(Model.class, new RdfObjectMapper(resourceUri));
		Resource resource = getPrimaryTopic(originalModel, resourceUri);

		assertNotNull(resource, "Expected to location resource in response for "+resourceUri);

		// Update the model with updated title
		resource.removeAll(DCTerms.title);
		// Make sure the title is unique
		final String UPDATED_TITLE = "This resources content has been replaced (" + System.currentTimeMillis() + ")";
		originalModel.add(resource, DCTerms.title, UPDATED_TITLE);

		response = buildBaseRequestSpecification()
					.contentType(TEXT_TURTLE)
					.header(IF_MATCH, eTag)
					.body(originalModel, new RdfObjectMapper(resourceUri)) // relative URI
				.when()
					.put(resourceUri);

		if (!isSuccessful().matches(response.getStatusCode())) {
			if (continueOnError) {
				throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
						"Skipping test because the PUT failed. The server may have restrictions on its content.",
						skipLog);
			} else {
				throw new Failure("Unable to do simple update on resource, received code: "+response.getStatusLine());
			}
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

		// Make sure it's the only title (we removed all before PUTting)
		Resource updatedResource = getPrimaryTopic(updatedModel, resourceUri);
		StmtIterator titleProps = updatedResource.listProperties(DCTerms.title);
		int titlePropSize = titleProps.toSet().size();
		assertEquals(titlePropSize, 1, "Updated resource should only contain one dcterms:title changes but instead found "+titlePropSize+" changes");
	}
}
