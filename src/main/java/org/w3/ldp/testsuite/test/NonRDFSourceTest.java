package org.w3.ldp.testsuite.test;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.annotations.SpecTest.STATUS;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;
import org.w3.ldp.testsuite.matcher.HeaderMatchers;

import com.hp.hpl.jena.rdf.model.Model;
import com.jayway.restassured.RestAssured;

/**
 * Tests Non-RDF Source LDP resources.
 */
public abstract class NonRDFSourceTest extends CommonResourceTest {

    @Test(
            groups = {MAY},
            description = "LDP servers may accept an HTTP POST of non-RDF " +
                    "representations (LDP-NRs) for creation of any kind of " +
                    "resource, for example binary resources.")
    @SpecTest(
    		specRefUri = LdpTestSuite.SPEC_URI + "#dfn-ldp-server", 
    		testMethod = METHOD.AUTOMATED,
    		approval   = STATUS.WG_APPROVED)
    public void testPostResource() throws IOException {
        // Make sure we can post binary resources
        RestAssured
            .given()
                .header("Slug", "test")
                .body(IOUtils.toByteArray(getClass().getResourceAsStream("/test.png")))
                .contentType("image/png")
            .expect()
                .statusCode(201)
                .header("Location", getResourceUri() + "test.png")
                .header("Link", CoreMatchers.anyOf( //TODO: RestAssured only checks the FIRST header...
                                HeaderMatchers.isLink(getResourceUri(), "describedby"),
                                //HeaderMatchers.isLink(LdpWebService.LDP_SERVER_CONSTRAINTS, "describedby"),
                                HeaderMatchers.isLink(org.w3.ldp.testsuite.vocab.LDP.BasicContainer.stringValue(), "type"))
                )
            .when()
                .post(getResourceUri());


        RestAssured.given().header(ACCEPT, TEXT_TURTLE)
                .expect().statusCode(HttpStatus.SC_OK).contentType(TEXT_TURTLE)
                .when().get(getResourceUri()).as(Model.class, new RdfObjectMapper(getResourceUri()));
    }

    //TODO: still refactoring tests from Marmotta

}
