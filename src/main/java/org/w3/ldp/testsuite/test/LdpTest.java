package org.w3.ldp.testsuite.test;

import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.ws.rs.core.Link;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.plugins.delegates.LinkDelegate;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.w3.ldp.testsuite.http.HttpHeaders;
import org.w3.ldp.testsuite.http.LdpPreferences;
import org.w3.ldp.testsuite.http.MediaTypes;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;

public abstract class LdpTest implements HttpHeaders, MediaTypes,
        LdpPreferences {

    /**
     * Alternate content to use on POST requests
     */
    private static Model postModel;

    /**
     * Alternate content to use on member resource initialization
     * {@link org.w3.ldp.testsuite.test.MemberResourceTest}
     */
    private static Model memberModel;

    private Model readModel(String path) {

        Model model = null;
        if (path != null) {
            model = ModelFactory.createDefaultModel();
            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream(path);

            model.read(inputStream, "", "TURTLE");
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return model;
    }

    /**
     * Initialization of member and generic resource models. This will run only once
     * at the beginning of the test suite, so postModel and memberModel static fields 
     * will be assigned once too. 
     * @param postTtl
     * @param memberTtl
     */
    @BeforeSuite(alwaysRun = true)
    @Parameters({ "postTtl", "memberTtl" })
    public void setPostContent(@Optional String postTtl,
            @Optional String memberTtl) {

        postModel = this.readModel(postTtl);
        memberModel = this.readModel(memberTtl);
    }

    /**
     * An absolute requirement of the specification.
     *
     * @see <a href="https://www.ietf.org/rfc/rfc2119.txt">RFC 2119</a>
     */
    public static final String MUST = "MUST";

    /**
     * There may exist valid reasons in particular circumstances to ignore a
     * particular item, but the full implications must be understood and
     * carefully weighed before choosing a different course.
     *
     * @see <a href="https://www.ietf.org/rfc/rfc2119.txt">RFC 2119</a>
     */
    public static final String SHOULD = "SHOULD";

    /**
     * An item is truly optional. One vendor may choose to include the item
     * because a particular marketplace requires it or because the vendor feels
     * that it enhances the product while another vendor may omit the same item.
     *
     * @see <a href="https://www.ietf.org/rfc/rfc2119.txt">RFC 2119</a>
     */
    public static final String MAY = "MAY";

    /**
     * A Linked Data Platform Non-RDF Source (LDP-NR). An LDPR whose state is
     * not represented in RDF. These are binary or text documents that do not
     * have useful RDF representations.
     *
     * @see <a href="http://www.w3.org/TR/ldp/#terms">LDP Terminology</a>
     */
    public static final String NR = "NON-RDF";

    private static boolean warnings = false;

    public static boolean getWarnings() {
        return warnings;
    }

    /**
     * Tests if a Link response header with the expected URI and relation is
     * present in an HTTP response.
     *
     * @param uri
     *            the expected URI
     * @param linkRelation
     *            the expected link relation (rel)
     * @param response
     *            the HTTP response
     * @see <a href="http://tools.ietf.org/html/rfc5988">RFC 5988</a>
     */
    protected boolean containsLinkHeader(String uri, String linkRelation,
            Response response) {
        return containsLinkHeader(uri, linkRelation, response.getHeaders()
                .getList(LINK));
    }

    public Model getAsModel(String uri) {
        return getResourceAsModel(uri, TEXT_TURTLE);
    }

    public Model getResourceAsModel(String uri, String mediaType) {
        return RestAssured.given().header(ACCEPT, mediaType).expect()
                .statusCode(isSuccessful()).when().get(uri)
                .as(Model.class, new RdfObjectMapper(uri));
    }

    private Model postBug() {
        Model model = ModelFactory.createDefaultModel();
        Resource resource = model.createResource("",
                model.createResource("http://example.com/ns#Bug"));
        resource.addProperty(
                model.createProperty("http://example.com/ns#severity"), "High");
        resource.addProperty(DC.title, "Another bug to test.");
        resource.addProperty(DC.description, "Issues that need to be fixed.");

        return model;
    }

    protected Model postContent() {
        if (postModel != null) {
            return postModel;
        }

        return postBug();
    }

    protected Model postMember() {
        if (memberModel != null) {
            return memberModel;
        }

        return postBug();
    }

    /**
     * Check if the header is contained in the headers list (because RestAssured
     * only checks the FIRST header)
     *
     * @param header
     *            header to look for
     * @param headers
     *            list of headers
     * @return header is contained
     */
    protected boolean containsLinkHeader(Header header, List<Header> headers) {
        for (Header h : headers) {
            if (header.equals(h)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the link is contained in the headers list (becase RestAssured
     * only checks the FIRST header)
     *
     * @param link
     *            header to look for
     * @param headers
     *            list of headers
     * @return link is contained
     */
    protected boolean containsLinkHeader(Link link, List<Header> headers) {
        for (Header header : headers) {
            for (String s : header.getValue().split(",")) {
                Link l = new LinkDelegate().fromString(s);
                if (link.equals(l)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the link is contained in the headers list (becase RestAssured
     * only checks the FIRST header)
     *
     * @param uri
     *            link uri
     * @param rel
     *            link rel
     * @param headers
     *            list of headers
     * @return link is contained
     */
    protected boolean containsLinkHeader(String uri, String rel,
            List<Header> headers) {
        return containsLinkHeader(Link.fromUri(uri).rel(rel).build(), headers);
    }

    protected String getFirstLinkForRelation(String rel, List<Header> headers) {
        for (Header header : headers) {
            for (String s : header.getValue().split(",")) {
                Link l = new LinkDelegate().fromString(s);
                if (rel.equals(l.getRel())) {
                    return l.getUri().toString();
                }
            }
        }
        return null;
    }

    /**
     * Checks the response for a
     * <code>Preference-Applied: return=representation</code> response header.
     * 
     * @param response
     *            the HTTP response
     * @return true if and only if the response contains the expected
     *         <code>Preference-Applied</code> header
     */
    protected boolean isPreferenceApplied(Response response) {
        List<Header> preferenceAppliedHeaders = response.getHeaders().getList(
                PREFERNCE_APPLIED);
        for (Header h : preferenceAppliedHeaders) {
            // Handle optional whitespace, quoted preference token values, and
            // other tokens in the Preference-Applied response header.
            if (h.getValue().matches(
                    "(^|[ ;])return *= *\"?representation\"?($|[ ;])")) {
                return true;
            }
        }

        return false;
    }

    public static String include(String... preferences) {
        return ldpPreference(PREFERENCE_INCLUDE, preferences);
    }

    public static String omit(String... preferences) {
        return ldpPreference(PREFERENCE_OMIT, preferences);
    }

    private static String ldpPreference(String name, String... values) {
        return "return=representation; " + name + "=\""
                + StringUtils.join(values, " ") + "\"";
    }
}
