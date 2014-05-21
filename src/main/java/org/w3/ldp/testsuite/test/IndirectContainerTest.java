package org.w3.ldp.testsuite.test;

import static org.testng.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpStatus;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.Implementation;
import org.w3.ldp.testsuite.annotations.Reference;
import org.w3.ldp.testsuite.annotations.Status;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

import org.w3.ldp.testsuite.vocab.LDP;

public class IndirectContainerTest extends CommonContainerTest {
    private String indirectContainer;

	@Parameters("indirectContainer")
	public IndirectContainerTest(@Optional String indirectContainer) {
		this.indirectContainer = indirectContainer;
	}

	@BeforeClass(alwaysRun = true)
	public void hasIndirectContainer() {
		if (indirectContainer == null) {
			throw new SkipException(
					"No indirectContainer parameter provided in testng.xml. Skipping ldp:IndirectContainer tests.");
		}
	}

	// TODO implement tests, signatures are from LDP spec
	@Test(groups = { MUST }, description = "LDP servers exposing LDPCs MUST advertise their "
			+ "LDP support by exposing a HTTP Link header with a "
			+ "target URI matching the type of container (see below) "
			+ "the server supports, and a link relation type of type "
			+ "(that is, rel='type') in all responses to requests made "
			+ "to the LDPC's HTTP Request-URI.")
	@Reference(uri = LdpTestSuite.SPEC_URI + "#ldpc-linktypehdr")
	@Status(status = Status.APPROVED)
	@Implementation(implementation = Implementation.IMPLEMENTED)
	public void testContainerSupportsHttpLinkHeader() throws URISyntaxException {
		Response response = RestAssured.given().header(ACCEPT, TEXT_TURTLE)
				.expect().statusCode(HttpStatus.SC_OK).when()
				.get(new URI(indirectContainer));
		assertTrue(
				hasLinkHeader(response, LDP.IndirectContainer.stringValue(),
						LINK_REL_TYPE),
				"LDP DirectContainers must advertise their LDP support by exposing a HTTP Link header with a URI matching <"
						+ LDP.IndirectContainer.stringValue()
						+ "> and rel='type'");
	}

	@Test(groups = { MUST }, enabled = false, description = "Each LDP Indirect Container MUST also be a conforming "
			+ "LDP Direct Container in section 5.4 Direct along "
			+ "the following restrictions.")
	@Reference(uri = LdpTestSuite.SPEC_URI + "#ldpic-are-ldpcs")
	@Status(status = Status.PENDING)
	@Implementation(implementation = Implementation.NOT_IMPLEMENTED)
	public void testCreateIndirectContainer() {

	}

	@Test(groups = { MUST }, enabled = false, description = "LDP Indirect Containers MUST contain exactly one "
			+ "triple whose subject is the LDPC URI, whose predicate "
			+ "is ldp:insertedContentRelation, and whose object ICR "
			+ "describes how the member-derived-URI in the container's "
			+ "membership triples is chosen.")
	@Reference(uri = LdpTestSuite.SPEC_URI + "#ldpic-indirectmbr")
	@Status(status = Status.PENDING)
	@Implementation(implementation = Implementation.NOT_IMPLEMENTED)
	public void testContainsLdpcUri() {

	}

	@Test(groups = { MUST }, enabled = false, description = "LDPCs whose ldp:insertedContentRelation triple has an "
			+ "object other than ldp:MemberSubject and that create new "
			+ "resources MUST add a triple to the container whose subject is "
			+ "the container's URI, whose predicate is ldp:contains, and whose "
			+ "object is the newly created resource's URI (which will be "
			+ "different from the member-derived URI in this case). This "
			+ "ldp:contains triple can be the only link from the container to the "
			+ "newly created resource in certain cases.")
	@Reference(uri = LdpTestSuite.SPEC_URI + "#ldpic-post-indirectmbrrel")
	@Status(status = Status.PENDING)
	@Implementation(implementation = Implementation.NOT_IMPLEMENTED)
	public void testPostResource() {

	}

	@Override
	protected String getResourceUri() {
		return indirectContainer;
	}

}
