package org.w3.ldp.testsuite.test;

import static org.hamcrest.Matchers.notNullValue;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpStatus;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.http.HttpMethod;
import org.w3.ldp.testsuite.annotations.Implementation;
import org.w3.ldp.testsuite.annotations.Reference;
import org.w3.ldp.testsuite.annotations.Status;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

import org.w3.ldp.testsuite.vocab.LDP;

public class DirectContainerTest extends CommonContainerTest {
    private String directContainer;

    @Parameters("directContainer")
    public DirectContainerTest(@Optional String directContainer) {
        this.directContainer = directContainer;
    }

    @BeforeClass(alwaysRun = true)
    public void hasDirectContainer() {
        if (directContainer == null) {
            throw new SkipException("No directContainer parameter provided in testng.xml. Skipping ldp:DirectContainer tests.");
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
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldpc-linktypehdr")
	@Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.IMPLEMENTED)
    public void testHttpLinkHeader() throws URISyntaxException {
        Response response = RestAssured.given().header(ACCEPT, TEXT_TURTLE)
                .expect().statusCode(HttpStatus.SC_OK).when()
                .get(new URI(directContainer));
        assertTrue(
                hasLinkHeader(response, LDP.DirectContainer.stringValue(), LINK_REL_TYPE),
                "LDP DirectContainers must advertise their LDP support by exposing a HTTP Link header with a URI matching <" + LDP.DirectContainer.stringValue() + "> and rel='type'");
    }

    @Test(
            groups = {SHOULD, "ldpMember"},
            description = "LDP Direct Containers SHOULD use the ldp:member predicate "
                    + "as an LDPC's membership predicate if there is no obvious "
                    + "predicate from an application vocabulary to use.")
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldpdc-mbrpred")
	@Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.IMPLEMENTED)
    public void testUseMemberPredicate() throws URISyntaxException {
        Model containerModel = getAsModel(directContainer);
        Resource container = containerModel.getResource(directContainer);
        Resource hasMemberRelation = container.getPropertyResourceValue(containerModel.createProperty(LDP.hasMemberRelation.stringValue()));
        assertEquals(LDP.member.stringValue(), hasMemberRelation.getURI(), "LDP Direct Containers should use the ldp:member predicate if "
                + "there is no obvious predicate from the application vocabulary. You can disabled this test using the 'testLdpMember' parameter in testng.xml.");
    }

    @Test(
            groups = {MUST},
            description = "Each LDP Direct Container representation MUST contain exactly "
                    + "one triple whose subject is the LDPC URI, whose predicate is the "
                    + "ldp:membershipResource, and whose object is the LDPC's membership-"
                    + "constant-URI. Commonly the LDPC's URI is the membership-constant-URI,"
                    + " but LDP does not require this.")
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldpdc-containres")
	@Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.IMPLEMENTED)
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
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldpdc-containtriples")
	@Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.IMPLEMENTED)
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
            enabled = false, // not implemented
            description = "LDP Direct Containers MUST behave as if they have a "
                    + "(LDPC URI, ldp:insertedContentRelation , ldp:MemberSubject)"
                    + " triple, but LDP imposes no requirement to materialize such "
                    + "a triple in the LDP-DC representation.")
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldpdc-indirectmbr-basic")
	@Status(status = Status.PENDING)
    @Implementation(implementation = Implementation.NOT_IMPLEMENTED)
    public void testActAsIfInsertedContentRelationTripleExists() {

    }

    @Test(
            groups = {MUST},
            description = "When a successful HTTP POST request to an LDPC results "
                    + "in the creation of an LDPR, the LDPC MUST update its "
                    + "membership triples to reflect that addition, and the resulting "
                    + "membership triple MUST be consistent with any LDP-defined "
                    + "predicates it exposes.")
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldpdc-post-createdmbr-member")
	@Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.IMPLEMENTED)
    public void testPostResourceUpdatesTriples() throws URISyntaxException {
        skipIfMethodNotAllowed(HttpMethod.POST);

        Model model = postContent();
        Response postResponse = RestAssured.given().contentType(TEXT_TURTLE).body(model, new RdfObjectMapper())
                .expect().statusCode(HttpStatus.SC_CREATED).header(LOCATION, notNullValue())
                .when().post(new URI(directContainer));

        String location = postResponse.getHeader(LOCATION);
        Model containerModel = getAsModel(directContainer);
        Resource container = containerModel.getResource(directContainer);
        Resource membershipResource = container.getPropertyResourceValue(containerModel.createProperty(LDP.membershipResource.stringValue()));
        Resource hasMemberRelation = container.getPropertyResourceValue(containerModel.createProperty(LDP.hasMemberRelation.stringValue()));
        assertNotNull(membershipResource);

        // Make sure the resource is in the container.
        Resource r = containerModel.createResource(location);
        if (hasMemberRelation != null) {
            assertTrue(membershipResource.hasProperty(containerModel.createProperty(hasMemberRelation.getURI()), r));
        }

        // Delete the resource to clean up.
        RestAssured.expect().statusCode(isSuccessful()).when().delete(new URI(location));
    }

    @Test(
            groups = {MUST},
            description = "When an LDPR identified by the object of a membership "
                    + "triple which was originally created by the LDP-DC is deleted, "
                    + "the LDPC server MUST also remove the corresponding membership triple.")
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldpdc-del-contremovesmbrtriple")
	@Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.IMPLEMENTED)
    public void testDeleteResourceUpdatesTriples() throws URISyntaxException {
        skipIfMethodNotAllowed(HttpMethod.POST);

        Model model = postContent();
        Response postResponse = RestAssured.given().contentType(TEXT_TURTLE).body(model, new RdfObjectMapper())
                .expect().statusCode(HttpStatus.SC_CREATED).header(LOCATION, notNullValue())
                .when().post(new URI(directContainer));

        String location = postResponse.getHeader(LOCATION);

        // Test the membership triple
        Model containerModel = getAsModel(directContainer);
        Resource container = containerModel.getResource(directContainer);
        Resource membershipResource = container.getPropertyResourceValue(containerModel.createProperty(LDP.membershipResource.stringValue()));
        Resource hasMemberRelation = container.getPropertyResourceValue(containerModel.createProperty(LDP.hasMemberRelation.stringValue()));
        Resource isMemberOfRelation = null;
        assertNotNull(membershipResource, MSG_MBRRES_NOTFOUND);
        Model membershipResourceModel = null;
        if (membershipResource.getURI().equals(directContainer)) {
            membershipResourceModel = containerModel;
        } else {
            membershipResourceModel = getAsModel(membershipResource.getURI());
            membershipResource = membershipResourceModel.getResource(membershipResource.getURI());
        }

        // First verify the membership triples exist
        if (hasMemberRelation != null) {
            assertTrue(membershipResource.hasProperty(membershipResourceModel.createProperty(hasMemberRelation.getURI()), membershipResourceModel.getResource(location)),
                    "The LDPC server must have a corresponding membership triple when an LDPR is added (hasMemberRelation).");
        } else {
            // Not if membership triple is not of form: (container, membership predicate, member), it may be the inverse.
            // Check both the membership resource and the member resource for the membership triple
            isMemberOfRelation = container.getPropertyResourceValue(containerModel.createProperty(LDP.isMemberOfRelation.stringValue()));
            int numMembershipTriples = membershipResourceModel.listStatements(membershipResourceModel.getResource(location), membershipResourceModel.createProperty(isMemberOfRelation.getURI()), membershipResource).toSet().size();
            Model memberResourceModel = getAsModel(location);
            assertNotNull(memberResourceModel, "Unable to fetch created member resource to validate membership triples (isMemberOfRelation)	");
            numMembershipTriples += memberResourceModel.listStatements(memberResourceModel.getResource(location), memberResourceModel.createProperty(isMemberOfRelation.getURI()), membershipResource).toSet().size();
            assertTrue(numMembershipTriples > 0, "The LDPC server must have a corresponding membership triple when an LDPR is added (isMemberOfRelation).");
        }

        // Delete the resource
        RestAssured.expect().statusCode(isSuccessful()).when().delete(new URI(location));

        // Get the updated membership resource
        membershipResourceModel = getAsModel(membershipResource.getURI());
        membershipResource = membershipResourceModel.getResource(membershipResource.getURI());

        // Now verify the membership triples DON"T exist
        if (hasMemberRelation != null) {
            assertFalse(membershipResource.hasProperty(membershipResourceModel.createProperty(hasMemberRelation.getURI()), membershipResourceModel.getResource(location)),
                    "The LDPC server must remove the corresponding membership triple when an LDPR is deleted (hasMemberRelation).");
        } else {
            // Not if membership triple is not of form: (container, membership predicate, member), it may be the inverse.
            isMemberOfRelation = container.getPropertyResourceValue(containerModel.createProperty(LDP.isMemberOfRelation.stringValue()));
            assertEquals(containerModel.listStatements(membershipResourceModel.getResource(location), membershipResourceModel.createProperty(isMemberOfRelation.getURI()), membershipResource).toSet().size(),
                    0, "The LDPC server must remove the corresponding membership triple when an LDPR is deleted (isMemberOfRelation).");
        }
    }

    @Override
    protected String getResourceUri() {
        return directContainer;
    }
}
