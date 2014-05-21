package org.w3.ldp.testsuite.test;

import static org.testng.Assert.assertTrue;
import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpStatus;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.Implementation;
import org.w3.ldp.testsuite.annotations.Reference;
import org.w3.ldp.testsuite.annotations.Status;
import org.w3.ldp.testsuite.exception.SkipClientTestException;
import org.w3.ldp.testsuite.exception.SkipNotTestableException;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.jayway.restassured.RestAssured;

/**
 * Tests all RDF source LDP resources, including containers and member resources.
 */
public abstract class RdfSourceTest extends CommonResourceTest {
    @Test(
            groups = {MUST},
            description = "LDP servers MUST provide an RDF representation "
                    + "for LDP-RSs. The HTTP Request-URI of the LDP-RS is "
                    + "typically the subject of most triples in the response.")
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldprs-gen-rdf")
	@Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.IMPLEMENTED)
    public void testGetResource() throws URISyntaxException {
        // Make sure we can get the resource itself and the response is
        // valid RDF. Turtle is a required media type, so this request
        // should succeed for all LDP-RS.
        RestAssured.given().header(ACCEPT, TEXT_TURTLE)
                .expect().statusCode(HttpStatus.SC_OK).contentType(TEXT_TURTLE)
                .when().get(new URI(getResourceUri())).as(Model.class, new RdfObjectMapper(getResourceUri()));
    }

    @Test(
            groups = {SHOULD},
            description = "LDP-RSs representations SHOULD have at least one "
                    + "rdf:type set explicitly. This makes the representations"
                    + " much more useful to client applications that don’t "
                    + "support inferencing.")
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldprs-gen-atleast1rdftype")
    @Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.IMPLEMENTED)
    public void testContainsRdfType() throws URISyntaxException {
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
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldprs-gen-reusevocab")
	@Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.NOT_IMPLEMENTED, manual = true)
    public void testReUseVocabularies() {
        throw new SkipNotTestableException();
    }

    @Test(
            groups = {SHOULD},
            description = "LDP-RSs predicates SHOULD use standard vocabularies such "
                    + "as Dublin Core [DC-TERMS], RDF [rdf11-concepts] and RDF "
                    + "Schema [rdf-schema], whenever possible.")
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldprs-gen-reusevocabsuchas")
	@Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.NOT_IMPLEMENTED, manual = true)
    public void testUseStandardVocabularies() throws URISyntaxException {
        throw new SkipNotTestableException();
    }

    @Test(
            groups = {MUST},
            description = "In the absence of special knowledge of the application "
                    + "or domain, LDP clients MUST assume that any LDP-RS can "
                    + "have multiple values for rdf:type.")
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldp-cli-multitype")
	@Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.NOT_IMPLEMENTED, clientOnly = true)
    public void testAllowMultipleRdfTypes() {
        throw new SkipClientTestException();
    }

    @Test(
            groups = {MUST},
            description = "In the absence of special knowledge of the "
                    + "application or domain, LDP clients MUST assume "
                    + "that the rdf:type values of a given LDP-RS can "
                    + "change over time.")
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldpr-cli-typeschange")
	@Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.NOT_IMPLEMENTED, clientOnly = true)
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
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldpr-cli-openpreds")
	@Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.NOT_IMPLEMENTED, clientOnly = true)
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
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldprs-gen-noinferencing")
	@Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.NOT_IMPLEMENTED, isTestable = false, manual = true)
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
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldpr-cli-preservetriples")
	@Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.NOT_IMPLEMENTED, isTestable = false, manual = true)
    public void testGetResourcePreservesTriples() throws URISyntaxException {
        throw new SkipNotTestableException();
    }

    @Test(
            groups = {MUST},
            description = "LDP clients MUST be capable of processing responses "
                    + "formed by an LDP server that ignores hints, including "
                    + "LDP-defined hints.")
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldpr-cli-hints-ignorable")
	@Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.NOT_IMPLEMENTED, isTestable = false, manual = true)
    public void testAllowResponsesFromServer() {
        throw new SkipNotTestableException();
    }

    @Test(
            groups = {MUST},
            description = "LDP servers MUST provide a text/turtle representation "
                    + "of the requested LDP-RS [turtle].")
    @Reference(uri = LdpTestSuite.SPEC_URI + "#ldprs-get-turtle")
	@Status(status = Status.APPROVED)
    @Implementation(implementation = Implementation.IMPLEMENTED)
    public void testGetResourceTurtle() throws URISyntaxException {
        RestAssured.given().header(ACCEPT, TEXT_TURTLE)
                .expect().statusCode(isSuccessful()).contentType(TEXT_TURTLE)
                .when().get(new URI(getResourceUri())).as(Model.class, new RdfObjectMapper(getResourceUri()));
    }

}
