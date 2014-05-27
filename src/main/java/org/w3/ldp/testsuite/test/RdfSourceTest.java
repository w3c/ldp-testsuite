package org.w3.ldp.testsuite.test;

import static org.testng.Assert.assertTrue;
import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpStatus;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.annotations.SpecTest.STATUS;
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
    @SpecTest(
    		specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-gen-rdf", 
    		testMethod = METHOD.AUTOMATED,
    		approval   = STATUS.WG_APPROVED)
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
    @SpecTest(
    		specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-gen-atleast1rdftype", 
    		testMethod = METHOD.AUTOMATED,
    		approval   = STATUS.WG_APPROVED)
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
    @SpecTest(
    		specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-gen-reusevocab", 
    		testMethod = METHOD.MANUAL,
    		approval   = STATUS.WG_APPROVED)
    public void testReUseVocabularies() {
        throw new SkipNotTestableException();
    }

    @Test(
            groups = {SHOULD},
            description = "LDP-RSs predicates SHOULD use standard vocabularies such "
                    + "as Dublin Core [DC-TERMS], RDF [rdf11-concepts] and RDF "
                    + "Schema [rdf-schema], whenever possible.")
    @SpecTest(
    		specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-gen-reusevocabsuchas", 
    		testMethod = METHOD.MANUAL,
    		approval   = STATUS.WG_APPROVED)
    public void testUseStandardVocabularies() throws URISyntaxException {
    	// TODO: Consider ideas for testUseStandardVocabularies (see comment)
    	/* Possible ideas: 
    	   fetch resource, look for known vocabulary term
    	   URIs.  Also can look at total number of terms and have a threshold
    	   of differences, say 100 terms and < 5 standard predicates would be odd.
    	   Also could look for similar/like short predicate short names or even
    	   look for owl:sameAs.
    	    */
        throw new SkipNotTestableException();
    }

    @Test(
            groups = {MUST},
            description = "In the absence of special knowledge of the application "
                    + "or domain, LDP clients MUST assume that any LDP-RS can "
                    + "have multiple values for rdf:type.")
    @SpecTest(
    		specRefUri = LdpTestSuite.SPEC_URI + "#ldp-cli-multitype", 
    		testMethod = METHOD.CLIENT_ONLY,
    		approval   = STATUS.WG_APPROVED)
    public void testAllowMultipleRdfTypes() {
        throw new SkipClientTestException();
    }

    @Test(
            groups = {MUST},
            description = "In the absence of special knowledge of the "
                    + "application or domain, LDP clients MUST assume "
                    + "that the rdf:type values of a given LDP-RS can "
                    + "change over time.")
    @SpecTest(
    		specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-cli-typeschange", 
    		testMethod = METHOD.CLIENT_ONLY,
    		approval   = STATUS.WG_APPROVED)
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
    @SpecTest(
    		specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-cli-openpreds", 
    		testMethod = METHOD.CLIENT_ONLY,
    		approval   = STATUS.WG_APPROVED)
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
    @SpecTest(
    		specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-gen-noinferencing", 
    		testMethod = METHOD.MANUAL,
    		approval   = STATUS.WG_APPROVED)
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
    @SpecTest(
    		specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-cli-preservetriples", 
    		testMethod = METHOD.MANUAL,
    		approval   = STATUS.WG_APPROVED)
    public void testGetResourcePreservesTriples() throws URISyntaxException {
        throw new SkipNotTestableException();
    }

    @Test(
            groups = {MUST},
            description = "LDP clients MUST be capable of processing responses "
                    + "formed by an LDP server that ignores hints, including "
                    + "LDP-defined hints.")
    @SpecTest(
    		specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-cli-hints-ignorable", 
    		testMethod = METHOD.MANUAL,
    		approval   = STATUS.WG_APPROVED)
    public void testAllowResponsesFromServer() {
        throw new SkipNotTestableException();
    }

    @Test(
            groups = {MUST},
            description = "LDP servers MUST provide a text/turtle representation "
                    + "of the requested LDP-RS [turtle].")
    @SpecTest(
    		specRefUri = LdpTestSuite.SPEC_URI + "#ldprs-get-turtle", 
    		testMethod = METHOD.AUTOMATED,
    		approval   = STATUS.WG_APPROVED)
    public void testGetResourceTurtle() throws URISyntaxException {
        RestAssured.given().header(ACCEPT, TEXT_TURTLE)
                .expect().statusCode(isSuccessful()).contentType(TEXT_TURTLE)
                .when().get(new URI(getResourceUri())).as(Model.class, new RdfObjectMapper(getResourceUri()));
    }

}
