package org.w3.ldp.testsuite.test;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.testng.annotations.BeforeClass;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.hamcrest.Matchers.notNullValue;
import static org.testng.Assert.*;
import static org.w3.ldp.testsuite.http.HttpHeaders.*;
import static org.w3.ldp.testsuite.http.LdpPreferences.PREFER_MEMBERSHIP;
import static org.w3.ldp.testsuite.http.LdpPreferences.PREFER_MINIMAL_CONTAINER;
import static org.w3.ldp.testsuite.http.MediaTypes.TEXT_TURTLE;
import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

public class DirectContainerTest extends CommonContainerTest {
	private String directContainer;

	@Parameters({"directContainer", "auth"})
	public DirectContainerTest(@Optional String directContainer, @Optional String auth) throws IOException {
		super(auth);
		this.directContainer = directContainer;
	}

	@BeforeClass(alwaysRun = true)
	public void hasDirectContainer() {
		if (directContainer == null) {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					"No directContainer parameter provided in testng.xml. Skipping ldp:DirectContainer tests.",
					skipLog);
		}
	}

	// MUST: 4.2.8.2 LDP servers must indicate their support for HTTP Methods by
	// responding to a HTTP OPTIONS request on the LDPR's URL with the HTTP
	// Method tokens in the HTTP response header Allow.

	// MUST: 5.2.1.2 The representation of an LDPC may have an rdf:type of only
	// one of ldp:Container for Linked Data Platform Container.

	@Test(
			groups = {MUST},
			description = "LDP servers exposing LDPCs MUST advertise their "
					+ "LDP support by exposing a HTTP Link header with a "
					+ "target URI matching the type of container (see below) "
					+ "the server supports, and a link relation type of type "
					+ "(that is, rel='type') in all responses to requests made "
					+ "to the LDPC's HTTP Request-URI.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-linktypehdr",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "IndirectContainerTest.testContainerSupportsHttpLinkHeader and "
					+ "BasicContainerTest.testContainerSupportsHttpLinkHeader "
					+ "covers the rest.")
	public void testHttpLinkHeader() {
		Response response = buildBaseRequestSpecification().header(ACCEPT, TEXT_TURTLE)
				.expect().statusCode(HttpStatus.SC_OK).when()
				.get(directContainer);
		assertTrue(
				containsLinkHeader(
						directContainer,
						LINK_REL_TYPE,
						LDP.DirectContainer.stringValue(),
						directContainer,
						response
				),
				"LDP DirectContainers must advertise their LDP support by exposing a HTTP Link header with a URI matching <" + LDP.DirectContainer.stringValue() + "> and rel='type'");
	}

	@Test(
			groups = {SHOULD, "ldpMember"},
			description = "LDP Direct Containers SHOULD use the ldp:member predicate "
					+ "as an LDPC's membership predicate if there is no obvious "
					+ "predicate from an application vocabulary to use.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpdc-mbrpred",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testUseMemberPredicate() throws URISyntaxException {
		Model containerModel = getAsModel(directContainer);
		Resource container = containerModel.getResource(directContainer);
		if (container.hasProperty(containerModel.createProperty(LDP.isMemberOfRelation.stringValue()))) {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					"This test does not apply to containers using the ldp:isMemberOfRelation membership pattern.",
					skipLog);
		}
		Resource hasMemberRelation = container.getPropertyResourceValue(containerModel.createProperty(LDP.hasMemberRelation.stringValue()));
		assertEquals(LDP.member.stringValue(), hasMemberRelation.getURI(), "LDP Direct Containers should use the ldp:member predicate if "
				+ "there is no obvious predicate from the application vocabulary. You can disable this test using the 'testLdpMember' parameter in testng.xml.");
	}

	@Test(
			groups = {MUST},
			description = "Each LDP Direct Container representation MUST contain exactly "
					+ "one triple whose subject is the LDPC URI, whose predicate is the "
					+ "ldp:membershipResource, and whose object is the LDPC's membership-"
					+ "constant-URI. Commonly the LDPC's URI is the membership-constant-URI,"
					+ " but LDP does not require this.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpdc-containres",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testMemberResourceTriple() throws URISyntaxException {
		Model containerModel = getAsModel(directContainer);
		Resource container = containerModel.getResource(directContainer);
		Resource membershipResource = container.getPropertyResourceValue(containerModel.createProperty(LDP.membershipResource.stringValue()));
		assertNotNull(membershipResource);
	}

	@Test(
			groups = {MUST},
			description = "Each LDP Direct Container representation must contain exactly "
					+ "one triple whose subject is the LDPC URI, and whose predicate "
					+ "is either ldp:hasMemberRelation or ldp:isMemberOfRelation. "
					+ "The object of the triple is constrained by other sections, "
					+ "such as ldp:hasMemberRelation or ldp:isMemberOfRelation, "
					+ "based on the membership triple pattern used by the container.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpdc-containtriples",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testMemberRelationOrIsMemberOfRelationTripleExists() throws URISyntaxException {
		Model containerModel = getAsModel(directContainer);
		Resource container = containerModel.getResource(directContainer);
		Resource hasMemberRelation = container.getPropertyResourceValue(containerModel.createProperty(LDP.hasMemberRelation.stringValue()));
		Resource isMemberOfRelation = container.getPropertyResourceValue(containerModel.createProperty(LDP.isMemberOfRelation.stringValue()));
		if (hasMemberRelation == null) {
			assertNotNull(isMemberOfRelation, "LDP DirectContainer must have either ldp:hasMemberRelation or ldp:isMemberOfRelation");
		} else {
			assertNull(isMemberOfRelation, "LDP DirectContainer cannot have both ldp:hasMemberRelation and ldp:isMemberOfRelation");
		}
	}

	@Test(
			groups = {MUST},
			description = "LDP Direct Containers MUST behave as if they have a "
					+ "(LDPC URI, ldp:insertedContentRelation , ldp:MemberSubject)"
					+ " triple, but LDP imposes no requirement to materialize such "
					+ "a triple in the LDP-DC representation.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpdc-indirectmbr-basic",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testActAsIfInsertedContentRelationTripleExists() {
		// TODO: This should probably just treat this as an indirect test, see issue #206
		testPostResourceUpdatesTriples();
	}

	@Test(
			groups = {MUST},
			description = "When a successful HTTP POST request to an LDPC results "
					+ "in the creation of an LDPR, the LDPC MUST update its "
					+ "membership triples to reflect that addition, and the resulting "
					+ "membership triple MUST be consistent with any LDP-defined "
					+ "predicates it exposes.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpdc-post-createdmbr-member",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testPostResourceUpdatesTriples() {
		skipIfMethodNotAllowed(HttpMethod.POST);

		Model model = postContent();
		Response postResponse = buildBaseRequestSpecification().contentType(TEXT_TURTLE).body(model, new RdfObjectMapper())
				.expect().statusCode(HttpStatus.SC_CREATED).header(LOCATION, notNullValue())
				.when().post(directContainer);

		String location = postResponse.getHeader(LOCATION);
		try {
			Response getResponse = buildBaseRequestSpecification()
						.header(ACCEPT, TEXT_TURTLE)
						.header(PREFER, include(PREFER_MEMBERSHIP)) // request all membership triples
					.expect()
						.statusCode(isSuccessful())
					.when()
						.get(directContainer);
			Model containerModel = getResponse.as(Model.class, new RdfObjectMapper(directContainer));
			Resource container = containerModel.getResource(directContainer);
			Resource membershipResource = container.getPropertyResourceValue(containerModel.createProperty(LDP.membershipResource.stringValue()));
			Resource hasMemberRelation = container.getPropertyResourceValue(containerModel.createProperty(LDP.hasMemberRelation.stringValue()));
			assertNotNull(membershipResource);

			if (hasMemberRelation != null) {
				// Make sure the resource is a member of the container.
				assertTrue(membershipResource.hasProperty(containerModel.createProperty(hasMemberRelation.getURI()), containerModel.createResource(location)));
			}
		} finally {
			// Delete the resource to clean up.
			buildBaseRequestSpecification().delete(location);
		}
	}

	@Test(
			groups = {MUST},
			description = "When an LDPR identified by the object of a membership "
					+ "triple which was originally created by the LDP-DC is deleted, "
					+ "the LDPC server MUST also remove the corresponding membership triple.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpdc-del-contremovesmbrtriple",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testDeleteResourceUpdatesTriples() {
		skipIfMethodNotAllowed(HttpMethod.POST);

		// Create a resource.
		Model model = postContent();
		Response postResponse = buildBaseRequestSpecification()
				.contentType(TEXT_TURTLE)
				.body(model, new RdfObjectMapper())
			.expect()
				.statusCode(HttpStatus.SC_CREATED)
				.header(LOCATION, HeaderMatchers.headerPresent())
			.when()
				.post(directContainer);

		String location = postResponse.getHeader(LOCATION);

		// Track whether we've deleted the resource, so we can clean up properly on test failures.
		boolean deleted = false;

		try {
			// Test the membership triple
			Response getResponse = buildBaseRequestSpecification()
					.header(ACCEPT, TEXT_TURTLE)
					.header(PREFER, include(PREFER_MEMBERSHIP)) // request all membership triple regardless of membership patterns
				.expect()
					.statusCode(isSuccessful())
				.when()
					.get(directContainer);
			Model containerModel = getResponse.as(Model.class, new RdfObjectMapper(directContainer));

			Resource container = containerModel.getResource(directContainer);
			Resource membershipResource = container.getPropertyResourceValue(containerModel.createProperty(LDP.membershipResource.stringValue()));
			Resource hasMemberRelation = container.getPropertyResourceValue(containerModel.createProperty(LDP.hasMemberRelation.stringValue()));
			Resource isMemberOfRelation = null;
			assertNotNull(membershipResource, MSG_MBRRES_NOTFOUND);

			// First verify the membership triples exist
			if (hasMemberRelation != null) {
				assertTrue(membershipResource.hasProperty(containerModel.createProperty(hasMemberRelation.getURI()), containerModel.getResource(location)),
						"The LDPC server must have a corresponding membership triple when an LDPR is added (hasMemberRelation).");
			} else {
				// Not if membership triple is not of form: (container, membership predicate, member), it may be the inverse.
				isMemberOfRelation = container.getPropertyResourceValue(containerModel.createProperty(LDP.isMemberOfRelation.stringValue()));
				// Check the container for the triple.
				if (!containerModel.contains(containerModel.getResource(location), containerModel.createProperty(isMemberOfRelation.getURI()), membershipResource)) {
					List<Header> preferenceAppliedHeaders = getResponse.getHeaders().getList(PREFERNCE_APPLIED);
					assertFalse(
							!preferenceAppliedHeaders.isEmpty() && hasReturnRepresentation(preferenceAppliedHeaders),
							"Server responded with Preference-Applied header for including membership triples, but membership triple is missing.");
				}

				// Check the resource has the triple as well.
				Model memberResourceModel = getAsModel(location);
				assertTrue(memberResourceModel.contains(memberResourceModel.getResource(location), memberResourceModel.createProperty(isMemberOfRelation.getURI()), membershipResource),
						"The LDPC server must have a corresponding membership triple when an LDPR is added (isMemberOfRelation).");
			}

			// Delete the resource
			deleted = true;
			buildBaseRequestSpecification().expect().statusCode(isSuccessful()).when().delete(location);

			// Get the updated membership resource
			getResponse = buildBaseRequestSpecification()
					.header(ACCEPT, TEXT_TURTLE)
					.header(PREFER, include(PREFER_MEMBERSHIP)) // request all membership triple regardless of membership patterns
				.expect()
					.statusCode(isSuccessful())
				.when()
					.get(directContainer);
			containerModel = getResponse.as(Model.class, new RdfObjectMapper(directContainer));
			membershipResource = containerModel.getResource(membershipResource.getURI());

			// Now verify the membership triples DON"T exist
			if (hasMemberRelation != null) {
				assertFalse(membershipResource.hasProperty(containerModel.createProperty(hasMemberRelation.getURI()), containerModel.getResource(location)),
						"The LDPC server must remove the corresponding membership triple when an LDPR is deleted (hasMemberRelation).");
			} else {
				// Not if membership triple is not of form: (container, membership predicate, member), it may be the inverse.
				isMemberOfRelation = container.getPropertyResourceValue(containerModel.createProperty(LDP.isMemberOfRelation.stringValue()));
				assertFalse(containerModel.contains(containerModel.getResource(location), containerModel.createProperty(isMemberOfRelation.getURI()), membershipResource),
						"The LDPC server must remove the corresponding membership triple when an LDPR is deleted (isMemberOfRelation).");
			}
		} finally {
			// If an assertion failed before we could delete the resource, clean up now.
			if (!deleted) {
				buildBaseRequestSpecification().delete(location);
			}
		}
	}

	@Test(
			groups = {MUST},
			description = "Each LDP Direct Container MUST also be a "
					+ "conforming LDP Container in section 5.2 "
					+ "Container along the following restrictions.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpdc-are-ldpcs",
			testMethod = METHOD.INDIRECT,
			approval = STATUS.WG_APPROVED,
			coveredByTests = {CommonContainerTest.class},
			coveredByGroups = {MUST})
	public void testConformsDcLdpContainer() {
		throw new org.testng.SkipException("Covered indirectly by the MUST tests defined in CommonContainerTest class");
	}

	private boolean hasMembershipTriples(Model containerModel) {
		Resource container = containerModel.getResource(directContainer);
		Resource membershipResource = container.getPropertyResourceValue(containerModel.createProperty(LDP.membershipResource.stringValue()));
		Resource hasMemberRelation = container.getPropertyResourceValue(containerModel.createProperty(LDP.hasMemberRelation.stringValue()));
		assertNotNull(membershipResource, MSG_MBRRES_NOTFOUND);

		// First verify the membership triples exist
		if (hasMemberRelation != null) {
			return membershipResource.hasProperty(containerModel.createProperty(hasMemberRelation.getURI()));
		}

		// Not if membership triple is not of form: (container, membership predicate, member), it may be the inverse.
		Resource isMemberOfRelation = container.getPropertyResourceValue(containerModel.createProperty(LDP.isMemberOfRelation.stringValue()));
		return containerModel.contains(null, containerModel.createProperty(isMemberOfRelation.getURI()), membershipResource);
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
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. ")
	public void testPreferMembershipTriples() {
		Response response;
		Model model;

		// Ask for membership triples.
		response = buildBaseRequestSpecification()
					.header(ACCEPT, TEXT_TURTLE)
					.header(PREFER, include(PREFER_MEMBERSHIP)) // request all membership triples
				.expect()
					.statusCode(isSuccessful())
				.when()
					.get(directContainer);
		model = response.as(Model.class, new RdfObjectMapper(directContainer));

		// Assumes the container is not empty.
		checkPreferenceAppliedHeader(response);
		assertTrue(hasMembershipTriples(model), "Container does not have membership triples");

		// Ask for a minimal container.
		response = buildBaseRequestSpecification()
					.header(ACCEPT, TEXT_TURTLE)
					.header(PREFER, include(PREFER_MINIMAL_CONTAINER)) // request no membership triples
				.expect()
					.statusCode(isSuccessful())
				.when()
					.get(directContainer);
		model = response.as(Model.class, new RdfObjectMapper(directContainer));

		checkPreferenceAppliedHeader(response);
		assertFalse(hasMembershipTriples(model), "Container has membership triples when minimal container was requested");

		// Ask to omit membership.
		response = buildBaseRequestSpecification()
					.header(ACCEPT, TEXT_TURTLE)
					.header(PREFER, omit(PREFER_MEMBERSHIP)) // request no membership triples
				.expect()
					.statusCode(isSuccessful())
				.when()
					.get(directContainer);
		model = response.as(Model.class, new RdfObjectMapper(directContainer));

		checkPreferenceAppliedHeader(response);
		assertFalse(hasMembershipTriples(model), "Container has membership triples when client requested server omit them");

		// Ask for a minimal container, but include membership. (Example from spec.)
		response = buildBaseRequestSpecification()
					.header(ACCEPT, TEXT_TURTLE)
					.header(PREFER, include(PREFER_MINIMAL_CONTAINER, PREFER_MEMBERSHIP))
				.expect()
					.statusCode(isSuccessful())
				.when()
					.get(directContainer);
		model = response.as(Model.class, new RdfObjectMapper(directContainer));

		// Assumes the container is not empty.
		checkPreferenceAppliedHeader(response);
		assertTrue(hasMembershipTriples(model), "Container does not have membership triples");
		assertFalse(model.contains(model.getResource(directContainer), model.createProperty(LDP.contains.stringValue())),
				"Container has containment triples when minimal container was requested");
	}

	@Override
	protected String getResourceUri() {
		return directContainer;
	}

}
