package org.w3.ldp.testsuite.test;

import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.core.Link;

import org.apache.commons.lang3.StringUtils;
import org.apache.marmotta.commons.vocabulary.LDP;
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
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.vocabulary.DC_11;
import com.hp.hpl.jena.vocabulary.RDF;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

public abstract class LdpTest implements HttpHeaders, MediaTypes, LdpPreferences {

	/**
	 * Alternate content to use on POST requests
	 */
	private static Model postModel;

	/**
	 * Builds a model from a turtle representation in a file
	 * @param path
	 */
	protected Model readModel(String path) {
		Model model = null;
		if (path != null) {
			model = ModelFactory.createDefaultModel();
			InputStream  inputStream = getClass().getClassLoader().getResourceAsStream(path);

			String fakeUri = "http://w3c.github.io/ldp-testsuite/fakesubject";
			// Even though null relative URIs are used in the resource representation file,
			// the resulting model doesn't keep them intact. They are changed to "file://..." if
			// an empty string is passed as base to this method.
			model.read(inputStream, fakeUri, "TURTLE");

			// At this point, the model should contain a resource named
			// "http://w3c.github.io/ldp-testsuite/fakesubject" if
			// there was a null relative URI in the resource representation
			// file.
			Resource subject = model.getResource(fakeUri);
			if (subject != null) {
				ResourceUtils.renameResource(subject, "");
			}

			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return model;
	}

	/**
	 * Initialization of generic resource model. This will run only once
	 * at the beginning of the test suite, so postModel static field
	 * will be assigned once too.
	 * @param postTtl
	 */
	@BeforeSuite(alwaysRun = true)
	@Parameters("postTtl")
	public void setPostContent(@Optional String postTtl) {
		postModel = this.readModel(postTtl);
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
	 * A grouping of tests that may not need to run as part of the regular
	 * TestNG runs.  Though by including it, it will allow for the generation
	 * via various reporters.
	 */
	public static final String MANUAL = "MANUAL";

	/**
	 * A Linked Data Platform Non-RDF Source (LDP-NR). An LDPR whose state
	 * is not represented in RDF. These are binary or text documents that do not
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
	 * Build a base RestAssured {@link com.jayway.restassured.specification.RequestSpecification}.
	 *
	 * @return RestAssured Request Specification
	 */
	protected abstract RequestSpecification buildBaseRequestSpecification();

	public Model getAsModel(String uri) {
		return getResourceAsModel(uri, TEXT_TURTLE);
	}

	public Model getResourceAsModel(String uri, String mediaType) {
		return buildBaseRequestSpecification()
				.header(ACCEPT, mediaType)
			.expect()
				.statusCode(isSuccessful())
			.when()
				.get(uri).as(Model.class, new RdfObjectMapper(uri));
	}

	protected Model getDefaultModel() {
		Model model = ModelFactory.createDefaultModel();
		Resource resource = model.createResource("",
				model.createResource("http://example.com/ns#Bug"));
		resource.addProperty(RDF.type, model.createResource(LDP.RDFSource.stringValue()));
		resource.addProperty(
				model.createProperty("http://example.com/ns#severity"), "High");
		resource.addProperty(DC_11.title, "Another bug to test.");
		resource.addProperty(DC_11.description, "Issues that need to be fixed.");

		return model;
	}

	protected Model postContent() {
		return postModel != null? postModel : getDefaultModel();
	}

	/**
	 * Are there any restrictions on content when creating resources? This is
	 * assumed to be true if POST content was provided using the {@code postTtl}
	 * test parameter.
	 *
	 * <p>
	 * This method is used for
	 * {@link CommonContainerTest#testRelativeUriResolutionPost()}.
	 * </p>
	 *
	 * @return true if there are restrictions on what triples are allowed; false
	 *         if the server allows most any RDF
	 * @see #setPostContent(String)
	 * @see RdfSourceTest#restrictionsOnTestResourceContent()
	 */
	protected boolean restrictionsOnPostContent() {
		return postModel != null;
	}

	/**
	 * Tests if a Link response header with the expected URI and relation is
	 * present in an HTTP response. Does not resolve relative URIs. If a Link
	 * URI might be relative, use {@link #containsLinkHeader(String, String,
	 * String, Response)}.
	 *
	 * @param expectedUri
	 *            the expected URI
	 * @param expectedRel
	 *            the expected link relation (rel)
	 * @param response
	 *            the HTTP response
	 * @see <a href="http://tools.ietf.org/html/rfc5988">RFC 5988</a>
	 * @see #getFirstLinkForRelation(String, String, Response)
	 */
	protected boolean containsLinkHeader(String expectedUri, String expectedRel, Response response) {
		return containsLinkHeader(expectedUri, expectedRel, null, response);
	}

	/**
	 * Tests if a Link response header with the expected URI and relation is
	 * present in an HTTP response. Resolves relative URIs against the request
	 * URI if necessary.
	 *
	 * @param expectedLinkUri
	 *            the expected URI
	 * @param expectedRel
	 *            the expected link relation (rel)
	 * @param requestUri
	 *            the HTTP request URI (for resolving relative URIs)
	 * @param response
	 *            the HTTP response
	 * @see <a href="http://tools.ietf.org/html/rfc5988">RFC 5988</a>
	 * @see #getFirstLinkForRelation(String, String, Response)
	 */
	protected boolean containsLinkHeader(String expectedLinkUri, String expectedRel, String requestUri, Response response) {
		List<Header> linkHeaders = response.getHeaders().getList(LINK);
		for (Header linkHeader : linkHeaders) {
			for (String s : linkHeader.getValue().split(",")) {
				Link nextLink = new LinkDelegate().fromString(s);
				if (expectedRel.equals(nextLink.getRel())) {
					String actualLinkUri = resolveIfRelative(requestUri, nextLink.getUri());
					if (expectedLinkUri.equals(actualLinkUri)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Gets the first link from {@code response} with link relation {@code rel}.
	 * Resolves relative URIs against the request URI if necessary.
	 *
	 * @param rel
	 *            the expected link relation
	 * @param requestUri
	 *            the HTTP request URI (for resolving relative URIs)
	 * @param response
	 *            the HTTP response
	 * @return the first link or {@code null} if none was found
	 * @see <a href="http://tools.ietf.org/html/rfc5988">RFC 5988</a>
	 * @see #containsLinkHeader(String, String, Response)
	 */
	protected String getFirstLinkForRelation(String rel, String requestUri, Response response) {
		List<Header> linkHeaders = response.getHeaders().getList(LINK);
		for (Header header : linkHeaders) {
			for (String s : header.getValue().split(",")) {
				Link l = new LinkDelegate().fromString(s);
				if (rel.equals(l.getRel())) {
					return resolveIfRelative(requestUri, l.getUri());
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
	 *			  the HTTP response
	 * @return true if and only if the response contains the expected
	 *		   <code>Preference-Applied</code> header
	 */
	protected boolean isPreferenceApplied(Response response) {
		List<Header> preferenceAppliedHeaders = response.getHeaders().getList(PREFERNCE_APPLIED);
		for (Header h : preferenceAppliedHeaders) {
			// Handle optional whitespace, quoted preference token values, and
			// other tokens in the Preference-Applied response header.
			if (h.getValue().matches("(^|[ ;])return *= *\"?representation\"?($|[ ;])")) {
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
		return "return=representation; " + name + "=\"" + StringUtils.join(values, " ") + "\"";
	}

	/**
	 * Resolves a URI if it's a relative path.
	 *
	 * @param base
	 *            the base URI to use
	 * @param toResolve
	 *            a URI that might be relative
	 * @return the resolved URI
	 * @throws URISyntaxException
	 *             on bad URIs (but relative URIs are OK)
	 */
	public static String resolveIfRelative(String base, String toResolve) {
		try {
			// The URI constructor accepts relative paths
			return resolveIfRelative(base, new URI(toResolve));
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	protected static String resolveIfRelative(String base, URI toResolve) {
		if (toResolve.isAbsolute()) {
			return toResolve.toString();
		}

		try {
			return new URI(base).resolve(toResolve).toString();
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
