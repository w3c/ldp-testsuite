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
import org.w3.ldp.testsuite.LdpConstants;
import org.w3.ldp.testsuite.annotations.Reference;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

public class BasicContainerTest extends CommonContainerTest {
	private String basicContainer;

	@Parameters("basicContainer")
	public BasicContainerTest(@Optional String basicContainer) {
		this.basicContainer = basicContainer;
	}
	
	@BeforeClass(alwaysRun = true)
	public void hasBasicContainer() {
		if (basicContainer == null) {
			throw new SkipException("No basicContainer parameter provided in testng.xml. Skipping ldp:basicContainer tests.");
		}
	}

	@Test(
			groups = { MUST }, 
			description = "LDP servers exposing LDPCs MUST advertise their "
							+ "LDP support by exposing a HTTP Link header with a "
							+ "target URI matching the type of container (see below) "
							+ "the server supports, and a link relation type of type "
							+ "(that is, rel='type') in all responses to requests made "
							+ "to the LDPC's HTTP Request-URI.")
	@Reference(uri = SPEC_URI + "#ldpc-linktypehdr")
	public void testContainerSupportsHttpLinkHeader() throws URISyntaxException {
		Response response = RestAssured.given().header(ACCEPT, TEXT_TURTLE)
				.expect().statusCode(HttpStatus.SC_OK).when()
				.get(new URI(basicContainer));
		assertTrue(
				hasLinkHeader(response, TYPE_BASIC_CONTAINER, LINK_REL_TYPE),
				"LDP BasicContainers must advertise their LDP support by exposing a HTTP Link header with a URI matching <" + TYPE_BASIC_CONTAINER + "> and rel='type'");
	}

	@Test(
			groups = { MUST }, 
			description = "Each LDP Basic Container MUST also be a "
							+ "conforming LDP Container in section 5.2 Container "
							+ "along the following restrictions in this section.")
	@Reference(uri = SPEC_URI + "#ldpbc-are-ldpcs")
	public void testContainerTypeIsBasicContainer() throws URISyntaxException {
		// FIXME: We're just testing the RDF type here. We're not really testing the requirement.
		Model containerModel = getAsModel(basicContainer);
		Resource container = containerModel.getResource(basicContainer);
		assertTrue(container.hasProperty(RDF.type, LdpConstants.TYPE_BASIC_CONTAINER),
				"Could not locate LDP BasicContainer rdf:type for <"+basicContainer+">");
	}

	@Override
    protected String getResourceUri() {
	    return basicContainer;
    }
}
