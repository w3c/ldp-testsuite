package org.w3.ldp.testsuite.test;

import com.jayway.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.annotations.SpecTest.STATUS;
import org.w3.ldp.testsuite.exception.SkipException;
import org.w3.ldp.testsuite.vocab.LDP;

import java.io.IOException;

import static org.testng.Assert.assertTrue;
import static org.w3.ldp.testsuite.http.HttpHeaders.LINK_REL_TYPE;

public class BasicContainerTest extends CommonContainerTest {

	private String basicContainer;

	@Parameters({"basicContainer", "auth"})
	public BasicContainerTest(@Optional String basicContainer, @Optional String auth) throws IOException {
		super(auth);
		this.basicContainer = basicContainer;
	}

	@Test(
			groups = {MUST},
			description = "Each LDP Basic Container MUST also be a "
					+ "conforming LDP Container in section 5.2 Container "
					+ "along with the following restrictions in this section.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpbc-are-ldpcs",
			testMethod = METHOD.INDIRECT,
			approval = STATUS.WG_APPROVED,
			coveredByTests = {CommonContainerTest.class},
			coveredByGroups = {MUST})
	public void testConformsBcLdpContainer() {
		throw new org.testng.SkipException("Covered indirectly by the MUST tests defined in CommonContainerTest class");
	}

	@BeforeClass(alwaysRun = true)
	public void hasBasicContainer() {
		if (basicContainer == null) {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					"No basicContainer parameter provided in testng.xml. Skipping ldp:basicContainer tests.",
					skipLog);
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
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "DirectContainerTest.testHttpLinkHeader and "
					+ "IndirectContainerTest.testContainerSupportsHttpLinkHeader "
					+ "covers the rest.")
	public void testContainerSupportsHttpLinkHeader() {
		Response response = buildBaseRequestSpecification().get(basicContainer);
		assertTrue(
				containsLinkHeader(
						basicContainer,
						LINK_REL_TYPE,
						LDP.BasicContainer.stringValue(),
						basicContainer,
						response
				),
				"LDP BasicContainers must advertise their LDP support by exposing " +
						"a HTTP Link header with a URI matching <"
						+ LDP.BasicContainer.stringValue() + "> and rel='type'"
		);
	}

	@Override
	protected String getResourceUri() {
		return basicContainer;
	}

}
