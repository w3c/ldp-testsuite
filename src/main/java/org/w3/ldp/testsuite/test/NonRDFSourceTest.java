package org.w3.ldp.testsuite.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.w3.ldp.testsuite.matcher.HttpStatusNotFoundOrGoneMatcher.isNotFoundOrGone;
import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.marmotta.commons.util.HashUtils;
import org.apache.marmotta.commons.vocabulary.LDP;
import org.openrdf.model.URI;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.annotations.SpecTest.STATUS;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;
import org.w3.ldp.testsuite.matcher.HeaderMatchers;

import com.hp.hpl.jena.rdf.model.Model;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;

/**
 * Tests Non-RDF Source LDP resources.
 */
public class NonRDFSourceTest extends CommonResourceTest {

    private final String container;
    private final URI containerType;
    /** Resource for CommonResourceTest */
    private final String nonRdfSource;

    @Parameters({ "basicContainer", "directContainer", "indirectContainer"})
    public NonRDFSourceTest(@Optional String basicContainer, @Optional String directContainer, @Optional String indirectContainer) throws IOException {
        if (StringUtils.isNotBlank(basicContainer)) {
            container = basicContainer;
            containerType = LDP.BasicContainer;
        } else if (StringUtils.isNotBlank(directContainer)) {
            container = directContainer;
            containerType = LDP.DirectContainer;
        } else if (StringUtils.isNotBlank(indirectContainer)) {
            container = indirectContainer;
            containerType = LDP.IndirectContainer;
        } else {
            throw new SkipException("No root container provided in testng.xml. Skipping LDP Non-RDF Source (LDP-NR) tests.");
        }
        
        final String slug = "test",
                file = slug + ".png",
                mimeType = "image/png";

        // Create a resource to use for CommonResourceTest.
        Response response = postNonRDFSource(slug, file, mimeType);
        nonRdfSource = response.getHeader(LOCATION);
    }

    @AfterClass(alwaysRun = true)
    public void deleteTestResource() {
        RestAssured.delete(nonRdfSource);
    }

    @Override
    protected String getResourceUri() {
        return nonRdfSource;
    }

    @Test(
            groups = {MAY, NR},
            description = "LDP servers may accept an HTTP POST of non-RDF " +
                    "representations (LDP-NRs) for creation of any kind of " +
                    "resource, for example binary resources.")
    @SpecTest(
            specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-createbins",
            testMethod = METHOD.AUTOMATED,
            approval = STATUS.WG_APPROVED)
    public void testPostNonRDFSource() throws IOException {
        // Test constants
        final String slug = "test",
                file = slug + ".png",
                mimeType = "image/png";

        // Make sure we can post binary resources
        Response response = postNonRDFSource(slug, file, mimeType);
        List<Header> links = response.headers().getList("Link");
        Assert.assertTrue(containsLinkHeader(containerType.stringValue(), "type", links));
        RestAssured.delete(response.getHeader(LOCATION));
    }

    @Test(
            groups = {MAY, NR},
            description = "LDP servers may accept an HTTP POST of non-RDF " +
                    "representations (LDP-NRs) for creation of any kind of " +
                    "resource, for example binary resources.")
    @SpecTest(
            specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-createbins",
            testMethod = METHOD.AUTOMATED,
            approval = STATUS.WG_PENDING)
    public void testPostResourceAndGetFromContainer() throws IOException {
        // Test constants
        final String slug = "test",
                file = slug + ".png",
                mimeType = "image/png";

        // Make sure we can post binary resources
        Response response = postNonRDFSource(slug, file, mimeType);
        List<Header> links = response.headers().getList("Link");
        Assert.assertTrue(containsLinkHeader(containerType.stringValue(), "type", links));

        // Check the container contains the new resource
        Model model = RestAssured
            .given()
                .header(ACCEPT, TEXT_TURTLE)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .contentType(TEXT_TURTLE)
            .get(container)
                .body().as(Model.class, new RdfObjectMapper(container));
        assertTrue(model.contains(model.createResource(container), model.createProperty(LDP.contains.stringValue()), model.createResource(response.getHeader(LOCATION))));

        RestAssured.delete(response.getHeader(LOCATION));
    }

    @Test(
            groups = {MAY, NR},
            description = "LDP servers may host a mixture of LDP-RSs and LDP-NRs. " +
                    "For example, it is common for LDP servers to need to host binary " +
                    "or text resources that do not have useful RDF representations.")
    @SpecTest(
            specRefUri = LdpTestSuite.SPEC_URI + "#ldpr-gen-binary",
            testMethod = METHOD.AUTOMATED,
            approval = STATUS.WG_APPROVED)
    public void testPostResourceGetBinary() throws IOException {
        // Test constants
        final String slug = "test",
                file = slug + ".png",
                mimeType = "image/png";

        // Make sure we can post binary resources
        Response response = postNonRDFSource(slug, file, mimeType);
        List<Header> links = response.headers().getList("Link");
        Assert.assertTrue(containsLinkHeader(containerType.stringValue(), "type", links));

        // And then check we get the binary back
        final String expectedMD5 = HashUtils.md5sum(NonRDFSourceTest.class.getResourceAsStream("/" + file));
        final byte[] binary = RestAssured
            .given()
                .header(ACCEPT, mimeType)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .contentType(mimeType)
                .header(ETAG, HeaderMatchers.isValidEntityTag())
            .when()
                .get(response.getHeader(LOCATION))
                .body().asByteArray();
        assertEquals(expectedMD5, HashUtils.md5sum(binary), "md5sum");

        RestAssured.delete(response.getHeader(LOCATION));
    }

    @Test(
            groups = {MAY, NR},
            description = "Each LDP Non-RDF Source must also be a conforming LDP Resource. " +
                    "LDP Non-RDF Sources may not be able to fully express their state using RDF.")
    @SpecTest(
            specRefUri = LdpTestSuite.SPEC_URI + "#ldpnr-are-ldpr",
            testMethod = METHOD.AUTOMATED,
            approval = STATUS.WG_APPROVED)
    public void testPostResourceGetMetadataAndBinary() throws IOException {
        // Test constants
        final String slug = "test",
                file = slug + ".png",
                mimeType = "image/png";

        // Make sure we can post binary resources
        Response response = postNonRDFSource(slug, file, mimeType);

        String location = response.getHeader(LOCATION);
        List<Header> links = response.getHeaders().getList(LINK);
        String describedBy = getFirstLinkForRelation("describedby", links);
        Assert.assertNotNull(describedBy, "Expected Link response header with relation 'describedby'");
        Assert.assertTrue(containsLinkHeader(containerType.stringValue(), "type", links));

        // And then check we get the metadata of back
        /* Model model = */ RestAssured
            .given()
                .header(ACCEPT, TEXT_TURTLE)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .contentType(TEXT_TURTLE)
                .header(ETAG, HeaderMatchers.isValidEntityTag())
            .when()
                .get(describedBy)
                .as(Model.class, new RdfObjectMapper(describedBy));

        // And the binary too
        final String expectedMD5 = HashUtils.md5sum(NonRDFSourceTest.class.getResourceAsStream("/" + file));
        final byte[] binary = RestAssured
            .given()
                .header(ACCEPT, mimeType)
            .expect()
                .statusCode(HttpStatus.SC_OK)
                .contentType(mimeType)
                .header(ETAG, HeaderMatchers.isValidEntityTag())
            .when()
                .get(location)
                .body().asByteArray();
        assertEquals(expectedMD5, HashUtils.md5sum(binary), "md5sum");
        
        RestAssured.delete(location);
    }

    @Test(
            groups = {MAY, NR},
            description = "LDP servers exposing an LDP Non-RDF Source may advertise this by exposing " +
                    "a HTTP Link header with a target URI of http://www.w3.org/ns/ldp#NonRDFSource, and " +
                    "a link relation type of type (that is, rel='type') in responses to requests made to " +
                    "the LDP-NR's HTTP Request-URI.")
    @SpecTest(
            specRefUri = LdpTestSuite.SPEC_URI + "#ldpnr-type",
            testMethod = METHOD.AUTOMATED,
            approval = STATUS.WG_APPROVED)
    public void testPostResourceAndCheckLink() throws IOException {
        // Test constants
        final String slug = "test",
                file = slug + ".png",
                mimeType = "image/png";

        // Make sure we can post binary resources
        Response response = postNonRDFSource(slug, file, mimeType);
        List<Header> links = response.headers().getList(LINK);
        Assert.assertTrue(containsLinkHeader(containerType.stringValue(), "type", links));

        // And then check the link when requesting the LDP-NR
        List<Header> linksNR = RestAssured
            .expect()
                .statusCode(isSuccessful())
                .header(ETAG, HeaderMatchers.isValidEntityTag())
            .when()
                .get(response.getHeader(LOCATION))
                .headers().getList(LINK);
        Assert.assertTrue(containsLinkHeader(LDP.NonRDFSource.stringValue(), "type", linksNR));

        RestAssured.delete(response.header(LOCATION));
    }

    @Test(
            groups = {MAY, NR},
            description = "Upon successful creation of an LDP-NR (HTTP status code of 201-Created and " +
                    "URI indicated by Location response header), LDP servers may create an associated " +
                    "LDP-RS to contain data about the newly created LDP-NR. If a LDP server creates " +
                    "this associated LDP-RS it must indicate its location on the HTTP response using " +
                    "the HTTP Link response header with link relation describedby and href to be the " +
                    "URI of the associated LDP-RS resource.")
    @SpecTest(
            specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-createbinlinkmetahdr",
            testMethod = METHOD.AUTOMATED,
            approval = STATUS.WG_APPROVED)
    public void testPostResourceAndCheckAssociatedResource() throws IOException {
        // Test constants
        final String slug = "test",
                file = slug + ".png",
                mimeType = "image/png";

        // Make sure we can post binary resources
        Response response = postNonRDFSource(slug, file, mimeType);
        List<Header> links = response.headers().getList(LINK);
        String describedBy = getFirstLinkForRelation("describedby", links);
        Assert.assertNotNull(describedBy, "Expected Link response header with relation 'describedby'");
        Assert.assertTrue(containsLinkHeader(containerType.stringValue(), "type", links));
        String location = response.getHeader(LOCATION);

        // Check the link when requesting the LDP-NS
        List<Header> linksNR = RestAssured
            .expect()
                .statusCode(isSuccessful())
                .header(ETAG, HeaderMatchers.headerPresent())
            .when()
                .get(location)
                .headers().getList("Link");
        Assert.assertTrue(containsLinkHeader(describedBy, "describedby", linksNR));

        // And then check the associated LDP-RS is actually there
        RestAssured
            .given()
                .header(ACCEPT, TEXT_TURTLE)
            .expect()
                .statusCode(isSuccessful())
                .contentType(TEXT_TURTLE)
                .header(ETAG, HeaderMatchers.isValidEntityTag())
            .when()
                .get(describedBy);

        RestAssured.delete(location);
    }

    @Test(
            groups = {MUST, NR},
            description = "When a contained LDPR is deleted, and the LDPC server created an"+
                    "associated LDP-RS (see the LDPC POST section), the LDPC server must also"+
                    "delete the associated LDP-RS it created.",
            dependsOnMethods = "testPostResourceAndCheckAssociatedResource")
    @SpecTest(
            specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-del-contremovescontres",
            testMethod = METHOD.AUTOMATED,
            approval = STATUS.WG_PENDING)
    public void testDeleteNonRDFSourceDeletesAssociatedResource() throws IOException {
        // Test constants
        final String slug = "test",
                file = slug + ".png",
                mimeType = "image/png";

        // Make sure we can post binary resources
        Response postResponse = postNonRDFSource(slug, file, mimeType);
        List<Header> links = postResponse.headers().getList(LINK);
        String describedBy = getFirstLinkForRelation("describedby", links);
        Assert.assertNotNull(describedBy, "Expected Link response header with relation 'describedby'");
        String location = postResponse.getHeader(LOCATION);

        // And then check the associated LDP-RS is actually there
        RestAssured
            .given()
                .header(ACCEPT, TEXT_TURTLE)
            .expect()
                .statusCode(isSuccessful())
                .contentType(TEXT_TURTLE)
            .when()
                .get(describedBy);

        // Delete the LDP-NR.
        RestAssured
            .expect()
                .statusCode(isSuccessful())
            .when()
                .delete(location);

        // Check that the associated LDP-RS is also deleted.
        RestAssured
            .given()
                .header(ACCEPT, TEXT_TURTLE)
            .expect()
                .statusCode(isNotFoundOrGone())
            .when()
                .get(describedBy);
    }

    protected Response postNonRDFSource(String slug, String file, String mimeType) throws IOException {
        // Make sure we can post binary resources
        return RestAssured
            .given()
                .header(SLUG, slug)
                .body(IOUtils.toByteArray(getClass().getResourceAsStream("/" + file)))
                .contentType(mimeType)
            .expect()
                .statusCode(HttpStatus.SC_CREATED)
                .header(LOCATION, HeaderMatchers.headerPresent())
            .when()
                .post(container);
    }

    @Test(
            groups = {MUST, NR},
            description = "When responding to requests whose request-URI is a LDP-NR with an"+
                    "associated LDP-RS, a LDPC server must provide the same HTTP Link response"+
                    "header as is required in the create response",
            dependsOnMethods = "testPostResourceAndCheckAssociatedResource")
    @SpecTest(
            specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-options-linkmetahdr",
            testMethod = METHOD.AUTOMATED,
            approval = STATUS.WG_PENDING)
    public void testOptionsHasSameLinkHeader() throws IOException {
        // Test constants
        final String slug = "test",
                file = slug + ".png",
                mimeType = "image/png";

        // Make sure we can post binary resources
        Response postResponse = postNonRDFSource(slug, file, mimeType);

        String location = postResponse.getHeader(LOCATION);
        List<Header> links = postResponse.getHeaders().getList(LINK);
        String describedBy = getFirstLinkForRelation("describedby", links);
        Assert.assertNotNull(describedBy, "Expected Link response header with relation 'describedby' for LDP-NR POST request");

        // Check the Link headers on an HTTP OPTIONS for the LDP-NR
        List<Header> linksOPTIONS = RestAssured
            .expect()
                .statusCode(isSuccessful())
            .when()
                .options(location)
                .getHeaders().getList(LINK);
        Assert.assertTrue(containsLinkHeader(describedBy, "describedby", linksOPTIONS),
                "Expected Link response header with relation 'describedby' and URI <"
                        + describedBy + "> for LDP-NR OPTIONS request");
        
        RestAssured.delete(location);
    }
}
