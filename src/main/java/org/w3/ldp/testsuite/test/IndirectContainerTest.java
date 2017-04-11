package org.w3.ldp.testsuite.test;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.Statement;
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
import org.w3.ldp.testsuite.http.LdpPreferences;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;
import org.w3.ldp.testsuite.vocab.LDP;

import java.io.IOException;

import static org.hamcrest.Matchers.notNullValue;
import static org.testng.Assert.assertTrue;
import static org.w3.ldp.testsuite.http.HttpHeaders.ACCEPT;
import static org.w3.ldp.testsuite.http.HttpHeaders.LINK_REL_TYPE;
import static org.w3.ldp.testsuite.http.HttpHeaders.LOCATION;
import static org.w3.ldp.testsuite.http.HttpHeaders.PREFER;
import static org.w3.ldp.testsuite.http.LdpPreferences.PREFER_MINIMAL_CONTAINER;
import static org.w3.ldp.testsuite.http.MediaTypes.TEXT_TURTLE;

public class IndirectContainerTest extends CommonContainerTest {

	private String indirectContainer;
	private Property insertedContentRelationProperty = null;

	@Parameters({"indirectContainer", "auth"})
	public IndirectContainerTest(@Optional String indirectContainer, @Optional String auth) throws IOException {
		super(auth);
		this.indirectContainer = indirectContainer;
	}
	
	@BeforeClass(alwaysRun = true)
	public void hasIndirectContainer() {
		if (indirectContainer == null) {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					"No indirectContainer parameter provided in testng.xml. Skipping ldp:IndirectContainer tests.",
					skipLog);
		}
		
		try {
			setInsertedContentRelation();
		} catch(Exception ignore) {}
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
				containsLinkHeader(
						indirectContainer,
						LINK_REL_TYPE,
						LDP.IndirectContainer.stringValue(),
						indirectContainer,
						response
				),
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
	public void testConformsIcLdpContainer() {
		// TODO: Impl testConformsIcLdpContainer
	}

	@Test(
			groups = {MUST},
			enabled = true,
			description = "LDP Indirect Containers MUST contain exactly one "
					+ "triple whose subject is the LDPC URI, whose predicate "
					+ "is ldp:insertedContentRelation, and whose object ICR "
					+ "describes how the member-derived-URI in the container's "
					+ "membership triples is chosen.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpic-indirectmbr",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_PENDING)
	public void testContainerHasInsertedContentRelation() {
		Response getResponse = buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
				.header(PREFER, include(PREFER_MINIMAL_CONTAINER))
				.expect()
					.statusCode(HttpStatus.SC_OK)
				.when()
					.get(indirectContainer);
		Model containerModel = getResponse.as(Model.class, new RdfObjectMapper(indirectContainer));
		Resource container = containerModel.getResource(indirectContainer);
		Property insertedContentRelation = ResourceFactory.createProperty(LDP.insertedContentRelation.stringValue());
		
		assertTrue(
				container.hasProperty(insertedContentRelation),
				"Container <"
						+ indirectContainer
						+ "> does not have a triple with the LDPC URI as the subject and ldp:insertedContentRelation as the predicate."
		);
		
		StmtIterator stmtIterator = container.listProperties(insertedContentRelation);
		RDFNode node = stmtIterator.next().getObject();
		
		assertTrue(
				node.isURIResource(),
				"The property with predicate ldp:insertedContentRelation, doesn't point to an RDF Object."
		);
		
		assertTrue(
				! stmtIterator.hasNext(),
				"Container <"
						+ indirectContainer
						+ "> has more than one triple with the LDPC URI as the subject and ldp:insertedContentRelation as the predicate."
		);
	}

	@Test(
			groups = {MUST},
			enabled = true,
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
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_PENDING)
	public void testPostResource() {
		skipIfMethodNotAllowed(HttpMethod.POST);
		
		if ( insertedContentRelationProperty != null ) {
			if ( insertedContentRelationProperty.getURI().equals(LDP.MemberSubject.stringValue()) ) {
				throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
						"The indirectContainer's ldp:insertedContentRelation triple has ldp:MemberSubject as the object.",
						skipLog);
			}
		}
		
		Model model = postContent();
		Response postResponse = buildBaseRequestSpecification()
				.contentType(TEXT_TURTLE)
				.body(model, new RdfObjectMapper())
				.expect()
					.statusCode(HttpStatus.SC_CREATED)
					.header(LOCATION, notNullValue())
				.when()
					.post(indirectContainer);

		String location = postResponse.getHeader(LOCATION);
		
		Response getResponse = buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
				.header(PREFER, include(LdpPreferences.PREFER_CONTAINMENT))
				.when()
					.get(indirectContainer);
		Model containerModel = getResponse.as(Model.class, new RdfObjectMapper(indirectContainer));
		Resource container = containerModel.getResource(indirectContainer);
		Property contains = ResourceFactory.createProperty(LDP.contains.stringValue());
		
		assertTrue(
				container.hasProperty(contains, containerModel.getResource(location)),
				"The IndirectContainer <"
						+ indirectContainer
						+ "> didn't create a triple with the containerURI as a subject, ldp:contains as the predicate "
						+ "and the resource's newly created URI as the object."
		);
				
	}

	private void setInsertedContentRelation() {
		Response getResponse = buildBaseRequestSpecification()
				.header(ACCEPT, TEXT_TURTLE)
				.header(PREFER, include(LdpPreferences.PREFER_MINIMAL_CONTAINER))
				.when()
					.get(indirectContainer);
		
		Model containerModel = getResponse.as(Model.class, new RdfObjectMapper(indirectContainer));
		Resource container = containerModel.getResource(indirectContainer);
		Property insertedContentRelation = containerModel.getProperty(LDP.insertedContentRelation.stringValue());
		
		if (!container.hasProperty(insertedContentRelation)) return;
		
		Statement statement = container.getProperty(insertedContentRelation);
		RDFNode node = statement.getObject();
		
		if (!node.isURIResource()) return;
		
		String propertyURI = node.asResource().getURI();
		insertedContentRelationProperty = ResourceFactory.createProperty(propertyURI);
	}
	
	@Override
	protected String getResourceUri() {
		return indirectContainer;
	}
	
	@Override
	protected Model getDefaultModel() {
		Model model = super.getDefaultModel();
		
		if (insertedContentRelationProperty == null) return model;
		
		Resource resource = model.getResource("");
		resource.addProperty(insertedContentRelationProperty, model.createResource("#me"));
		return model;
	}

}
