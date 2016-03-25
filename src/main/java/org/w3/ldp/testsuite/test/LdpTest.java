package org.w3.ldp.testsuite.test;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

import org.apache.commons.lang3.StringUtils;
import org.apache.marmotta.commons.vocabulary.LDP;
import org.jboss.resteasy.plugins.delegates.LinkDelegate;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;

import javax.ws.rs.core.Link;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.testng.Assert.assertTrue;
import static org.w3.ldp.testsuite.http.HttpHeaders.*;
import static org.w3.ldp.testsuite.http.LdpPreferences.PREFERENCE_INCLUDE;
import static org.w3.ldp.testsuite.http.LdpPreferences.PREFERENCE_OMIT;
import static org.w3.ldp.testsuite.http.MediaTypes.TEXT_TURTLE;
import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

public abstract class LdpTest {

	public final static String SKIPPED_LOG_FILENAME = "skipped.log";

	public final static String HTTP_LOG_FILENAME = "http.log";
	public final static DateFormat df = DateFormat.getDateTimeInstance();

	public final static String DEFAULT_MODEL_TYPE = "http://example.com/ns#Bug";

	/*
	 * The following properties are marked static because commonSetup() is only called
	 * one time, even if several test classes inherit from LdpTest.
	 */

	/**
	 * Alternate content to use on POST requests
	 */
	private static Model postModel;

	/**
	 * For HTTP details on validation failures
	 */
	protected static PrintWriter httpLog;

	/**
	 * For skipped test logging
	 */
	protected static PrintWriter skipLog;

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
	 *
	 * @param postTtl the resource with Turtle content to use for POST requests
	 * @param httpLogging whether to log HTTP request and response details on errors
	 */
	@BeforeSuite(alwaysRun = true)
	@Parameters({"output", "postTtl", "httpLogging", "skipLogging"})
	public void commonSetup(@Optional String outputDir, @Optional String postTtl, @Optional String httpLogging, @Optional String skipLogging) throws IOException {

		/*
		 * Note: This method is only called one time, even if many classes inherit
		 * from LdpTest. Don't set non-static members here.
		 */

		postModel = readModel(postTtl);

		if (outputDir == null || outputDir.length() == 0)
			outputDir = LdpTestSuite.OUTPUT_DIR;
		
		File dir = new File(outputDir);
		dir.mkdirs();

		if ("true".equals(httpLogging)) {
			File file = new File(dir, HTTP_LOG_FILENAME);
			try {
				httpLog = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
				httpLog.println(String.format("LDP Test Suite: HTTP Log (%s)", df.format(new Date())));
				httpLog.println("---------------------------------------------------");
			} catch (IOException e) {
				System.err.println(String.format("WARNING: Error creating %s for detailed errors", HTTP_LOG_FILENAME));
				e.printStackTrace();
			}
		}

		if ("true".equals(skipLogging)) {
			File file = new File(dir, SKIPPED_LOG_FILENAME);
			try {
				skipLog = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
				skipLog.println(String.format("LDP Test Suite: Skipped Tests Log (%s)", df.format(new Date())));
				skipLog.println("------------------------------------------------------------");
			} catch (IOException e) {
				System.err.println(String.format("WARNING: Error creating %s for detailed errors", SKIPPED_LOG_FILENAME));
				e.printStackTrace();
			}
		}

	}

	@AfterSuite(alwaysRun = true)
	public void commonTearDown() {
		if (httpLog != null) {
			httpLog.println();
			httpLog.flush();
			httpLog.close();
		}
		if (skipLog != null) {
			skipLog.println();
			skipLog.flush();
			skipLog.close();
		}
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
				model.createResource(DEFAULT_MODEL_TYPE));
		resource.addProperty(RDF.type, model.createResource(LDP.RDFSource.stringValue()));
		resource.addProperty(
				model.createProperty("http://example.com/ns#severity"), "High");
		resource.addProperty(DCTerms.title, "Another bug to test.");
		resource.addProperty(DCTerms.description, "Issues that need to be fixed.");

		return model;
	}

	/**
	 * Given the location (URI), locate the appropriate "primary" resource within
	 * the model.  Often models will have many triples with various subjects, which
	 * don't always match the request-URI.  This attempts to resolve to the appropriate
	 * resource for the request-URI.  This method is used to determine which subject
	 * URI should be used to assign new triples to for tests such as PUT.
	 * 
	 * @param model
	 * @param location
	 * @return Resource primary from model
	 */
	protected Resource getPrimaryTopic(Model model, String location) {
		Resource loc = model.getResource(location);
		ResIterator bugs = model.listSubjectsWithProperty(RDF.type, model.createResource(DEFAULT_MODEL_TYPE));
		if (bugs.hasNext()) {
			return bugs.nextResource();
		} else {
			return loc;
		}
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
	 * {@link CommonContainerTest#testRelativeUriResolutionPost(String)}.
	 * </p>
	 *
	 * @return true if there are restrictions on what triples are allowed; false
	 *		   if the server allows most any RDF
	 * @see RdfSourceTest#restrictionsOnTestResourceContent()
	 */
	protected boolean restrictionsOnPostContent() {
		return postModel != null;
	}

	/**
	 * Tests if a Link response header with the expected context URI, link
	 * relation, and target URI is present in an HTTP response. Resolves
	 * relative URIs against the request URI if necessary.
	 *
	 * @param linkContext
	 *            the context of the Link (usually the request URI, but can be
	 *            changed with an anchor parameter)
	 * @param relation
	 *            the expected link relation (rel)
	 * @param linkTarget
	 *            the expected URI
	 * @param requestUri
	 *            the HTTP request URI (for determing a Link's context and
	 *            resolving relative URIs)
	 * @param response
	 *            the HTTP response
	 * @see <a href="http://tools.ietf.org/html/rfc5988">RFC 5988</a>
	 */
	protected boolean containsLinkHeader(
			String linkContext,
			String relation,
			String linkTarget,
			String requestUri,
			Response response) {
		List<Header> linkHeaders = response.getHeaders().getList(LINK);
		for (Header linkHeader : linkHeaders) {
			for (String s : splitLinks(linkHeader)) {
				Link nextLink = new LinkDelegate().fromString(s);
				if (relation.equals(nextLink.getRel())) {
					String actualLinkUri = resolveIfRelative(requestUri, nextLink.getUri());
					if (linkMatchesContext(linkContext, requestUri, nextLink) &&
							linkTarget.equals(actualLinkUri)) {
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
	 * @param linkContext
	 *            the context of the Link (usually the request URI, but can be
	 *            changed with an anchor parameter)
	 * @param relation
	 *            the expected link relation
	 * @param requestUri
	 *            the HTTP request URI (for determing a Link's context and
	 *            resolving relative URIs)
	 * @param response
	 *            the HTTP response
	 * @return the first link or {@code null} if none was found
	 * @see <a href="http://tools.ietf.org/html/rfc5988">RFC 5988</a>
	 */
	protected String getFirstLinkForRelation(String linkContext, String relation, String requestUri, Response response) {
		List<Header> linkHeaders = response.getHeaders().getList(LINK);
		for (Header header : linkHeaders) {
			for (String s : splitLinks(header)) {
				Link l = new LinkDelegate().fromString(s);
				if (relation.equals(l.getRel()) &&
						linkMatchesContext(linkContext, requestUri, l)) {
					return resolveIfRelative(requestUri, l.getUri());
				}
			}
		}

		return null;
	}

	/**
	 * Splits an HTTP Link header that might have multiple links separated by a
	 * comma.
	 *
	 * @param linkHeader
	 *			the link header
	 * @return the list of link-values as defined in RFC 5988 (for example,
	 *		 {@code "<http://example.com/bt/bug432>; rel=related"})
	 * @see <a href="http://tools.ietf.org/html/rfc5988#page-7">RFC 5988: The Link Header Field</a>
	 */
	// LinkDelegate doesn't handle this for us
	protected List<String> splitLinks(Header linkHeader) {
		final ArrayList<String> links = new ArrayList<>();
		final String value = linkHeader.getValue();

		// Track the beginning index for the current link-value.
		int beginIndex = 0;

		// Is the current char inside a URI-Reference?
		boolean inUriRef = false;

		// Split the string on commas, but only if not in a URI-Reference
		// delimited by angle brackets.
		for (int i = 0; i < value.length(); ++i) {
			final char c = value.charAt(i);

			if (c == ',' && !inUriRef) {
				// Found a comma not in a URI-Reference. Split the string.
				final String link = value.substring(beginIndex, i).trim();
				links.add(link);

				// Assign the next begin index for the next link.
				beginIndex = i + 1;
			} else if (c == '<') {
				// Angle brackets are not legal characters in a URI, so they can
				// only be used to mark the start and end of a URI-Reference.
				// See http://tools.ietf.org/html/rfc3986#section-2
				inUriRef = true;
			} else if (c == '>') {
				inUriRef = false;
			}
		}

		// There should be one more link in the string.
		final String link = value.substring(beginIndex, value.length()).trim();
		links.add(link);

		return links;
	}

	private boolean linkMatchesContext(String expectedContext, String requestUri, Link link) {
		String anchor = link.getParams().get("anchor");
		if (anchor == null) {
			anchor = requestUri;
		}

		return anchor.equals(expectedContext);
	}

	/**
	 * Asserts the response has a <code>Preference-Applied:
	 * return=representation</code> response header, but only if at
	 * least one <code>Preference-Applied</code> header is present.
	 *
	 * @param response
	 *			  the HTTP response
	 */
	protected void checkPreferenceAppliedHeader(Response response) {
		List<Header> preferenceAppliedHeaders = response.getHeaders().getList(PREFERNCE_APPLIED);
		if (preferenceAppliedHeaders.isEmpty()) {
			// The header is not mandatory.
			return;
		}

		assertTrue(hasReturnRepresentation(preferenceAppliedHeaders),
				"Server responded with a Preference-Applied header, but it did not contain return=representation");
	}

	protected boolean hasReturnRepresentation(List<Header> preferenceAppliedHeaders) {
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
	 *			  the base URI to use
	 * @param toResolve
	 *			  a URI that might be relative
	 * @return the resolved URI
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
