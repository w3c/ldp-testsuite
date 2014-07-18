package org.w3.ldp.testsuite.test;

import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
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

import java.io.IOException;

import static org.testng.Assert.assertTrue;

public class BasicContainerTest extends CommonContainerTest {

	private String basicContainer;

	@Parameters({"basicContainer", "auth"})
	public BasicContainerTest(@Optional String basicContainer, @Optional String auth) throws IOException {
		super(auth);
		this.basicContainer = basicContainer;
	}

	@BeforeClass(alwaysRun = true)
	public void hasBasicContainer() {
		if (basicContainer == null) {
			throw new SkipException(
					"No basicContainer parameter provided in testng.xml. Skipping ldp:basicContainer tests.");
		}
	}

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
			approval = STATUS.WG_APPROVED)
	public void testContainerSupportsHttpLinkHeader() {
		Response response = buildBaseRequestSpecification().header(ACCEPT, TEXT_TURTLE)
				.expect().statusCode(HttpStatus.SC_OK).when()
				.get(basicContainer);
		assertTrue(
				containsLinkHeader(LDP.BasicContainer.stringValue(), LINK_REL_TYPE,
						response),
				"LDP BasicContainers must advertise their LDP support by exposing a HTTP Link header with a URI matching <"
						+ LDP.BasicContainer.stringValue() + "> and rel='type'"
		);
	}

	@Override
	protected String getResourceUri() {
		return basicContainer;
	}

}
