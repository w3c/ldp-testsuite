package org.w3.ldp.testsuite.test;

import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.marmotta.commons.constants.Namespace.FOAF;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.annotations.SpecTest.STATUS;
import org.w3.ldp.testsuite.vocab.LDP;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.jayway.restassured.response.Response;

public class IndirectContainerTest extends CommonContainerTest {
	// Default Inserted Content Relation
	private final static String DEFAULT_ICR = FOAF.primaryTopic;
	
	private String indirectContainer;
	private Property insertedContentRelation;
	
	@Parameters({"indirectContainer", "auth", "insertedContentRelation"})
	public IndirectContainerTest(@Optional String indirectContainer, @Optional String auth, @Optional String insertedContentRelation) throws IOException {
		super(auth);
		this.indirectContainer = indirectContainer;
		this.insertedContentRelation = ResourceFactory.createProperty(insertedContentRelation);
	}

	@BeforeClass(alwaysRun = true)
	public void hasIndirectContainer() {
		if (indirectContainer == null) {
			throw new SkipException(
					"No indirectContainer parameter provided in testng.xml. Skipping ldp:IndirectContainer tests.");
		}
		if(insertedContentRelation == null) {
			insertedContentRelation = ResourceFactory.createProperty(DEFAULT_ICR);
		}
	}
	
	@Override
	protected Model getDefaultModel() {
		Model model = super.getDefaultModel();
		Resource resource = model.getResource("");
		resource.addProperty(insertedContentRelation,
				model.createResource("#me"));

		return model;
	}

	// TODO implement tests, signatures are from LDP spec
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
					+ "DirectContainerTest.testHttpLinkHeader and "
					+ "BasicContainerTest.testContainerSupportsHttpLinkHeader "
					+ "covers the rest.")
	public void testContainerSupportsHttpLinkHeader() {
		Response response = buildBaseRequestSpecification().header(ACCEPT, TEXT_TURTLE)
				.expect().statusCode(HttpStatus.SC_OK).when()
				.get(indirectContainer);
		assertTrue(
				containsLinkHeader(LDP.IndirectContainer.stringValue(), LINK_REL_TYPE,
						response),
				"LDP DirectContainers must advertise their LDP support by exposing a HTTP Link header with a URI matching <"
						+ LDP.IndirectContainer.stringValue()
						+ "> and rel='type'"
		);
	}

	@Test(
			groups = {MUST},
			enabled = false,
			description = "Each LDP Indirect Container MUST also be a conforming "
					+ "LDP Direct Container in section 5.4 Direct along "
					+ "the following restrictions.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpic-are-ldpcs",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testCreateIndirectContainer() {
		// TODO: Impl testCreateIndirectContainer
	}

	@Test(
			groups = {MUST},
			enabled = false,
			description = "LDP Indirect Containers MUST contain exactly one "
					+ "triple whose subject is the LDPC URI, whose predicate "
					+ "is ldp:insertedContentRelation, and whose object ICR "
					+ "describes how the member-derived-URI in the container's "
					+ "membership triples is chosen.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpic-indirectmbr",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testContainsLdpcUri() {
		// TODO: Impl testContainsLdpcUri
	}

	@Test(
			groups = {MUST},
			enabled = false,
			description = "LDPCs whose ldp:insertedContentRelation triple has an "
					+ "object other than ldp:MemberSubject and that create new "
					+ "resources MUST add a triple to the container whose subject is "
					+ "the container's URI, whose predicate is ldp:contains, and whose "
					+ "object is the newly created resource's URI (which will be "
					+ "different from the member-derived URI in this case). This "
					+ "ldp:contains triple can be the only link from the container to the "
					+ "newly created resource in certain cases.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpic-post-indirectmbrrel",
			testMethod = METHOD.NOT_IMPLEMENTED,
			approval = STATUS.WG_PENDING)
	public void testPostResource() {
		// TODO: Impl testPostResource
	}

	@Override
	protected String getResourceUri() {
		return indirectContainer;
	}

}
