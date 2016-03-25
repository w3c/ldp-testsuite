package org.w3.ldp.testsuite.test;

import org.apache.jena.rdf.model.Model;
import com.jayway.restassured.response.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.marmotta.commons.util.HashUtils;
import org.apache.marmotta.commons.vocabulary.LDP;
import org.testng.Assert;
import org.testng.annotations.*;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.annotations.SpecTest.STATUS;
import org.w3.ldp.testsuite.exception.SkipException;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;
import org.w3.ldp.testsuite.matcher.HeaderMatchers;

import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.w3.ldp.testsuite.http.HttpHeaders.*;
import static org.w3.ldp.testsuite.http.MediaTypes.TEXT_TURTLE;
import static org.w3.ldp.testsuite.matcher.HttpStatusNotFoundOrGoneMatcher.isNotFoundOrGone;
import static org.w3.ldp.testsuite.matcher.HttpStatusSuccessMatcher.isSuccessful;

/**
 * Tests Non-RDF Source LDP resources.
 */
public class NonRDFSourceTest extends CommonResourceTest {
	private final static String SETUP_ERROR = "ERROR: Could not create test resource for NonRDFSourceTest. Skipping tests.";

	private String container;
	/** Resource for CommonResourceTest */
	private String nonRdfSource;

	@Parameters("auth")
	public NonRDFSourceTest(@Optional String auth) throws IOException {
		super(auth);
	}

	@Parameters({ "basicContainer", "directContainer", "indirectContainer" })
	@BeforeSuite(alwaysRun = true)
	public void createTestResource(@Optional String basicContainer, @Optional String directContainer, @Optional String indirectContainer) {
		if (StringUtils.isNotBlank(basicContainer)) {
			container = basicContainer;
		} else if (StringUtils.isNotBlank(directContainer)) {
			container = directContainer;
		} else if (StringUtils.isNotBlank(indirectContainer)) {
			container = indirectContainer;
		} else {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					"No root container provided in testng.xml. Skipping LDP Non-RDF Source (LDP-NR) tests.",
					skipLog);
		}

		final String slug = "non-rdf-source",
				file = "test.png",
				mimeType = "image/png";

		// Create a resource to use for CommonResourceTest.
		try {
			Response response = buildBaseRequestSpecification()
					.header(SLUG, slug)
					.body(IOUtils.toByteArray(getClass().getResourceAsStream("/" + file)))
					.contentType(mimeType)
					.post(container);
			if (response.getStatusCode() != HttpStatus.SC_CREATED) {
				System.err.println(SETUP_ERROR);
				System.err.println("POST failed with status code: " + response.getStatusCode());
				System.err.println();
				return;
			}

			nonRdfSource = response.getHeader(LOCATION);
			if (nonRdfSource == null) {
				System.err.println(SETUP_ERROR);
				System.err.println("Location response header missing");
				System.err.println();
				return;
			}
		} catch (Exception e) {
			System.err.println(SETUP_ERROR);
			e.printStackTrace();
		}
	}

	@AfterSuite(alwaysRun = true)
	public void deleteTestResource() {
		if (nonRdfSource != null) {
			buildBaseRequestSpecification().delete(nonRdfSource);
		}
	}

	@Override
	protected String getResourceUri() {
		if (nonRdfSource == null) {
			throw new SkipException(Thread.currentThread().getStackTrace()[2].getMethodName(),
					"Skipping test because test resource is null.", skipLog);
		}

		return nonRdfSource;
	}

	@Test(
			groups = {MAY},
			description = "LDP servers may accept an HTTP POST of non-RDF " +
					"representations (LDP-NRs) for creation of any kind of " +
					"resource, for example binary resources.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-createbins",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testPostResourceAndGetFromContainer covers the rest.")
	public void testPostNonRDFSource() throws IOException {
		// Test constants
		final String slug = "test",
				file = slug + ".png",
				mimeType = "image/png";

		// Make sure we can post binary resources
		Response response = postNonRDFSource(slug, file, mimeType);
		buildBaseRequestSpecification().delete(response.getHeader(LOCATION));
	}

	@Test(
			groups = {MAY},
			description = "LDP servers may accept an HTTP POST of non-RDF " +
					"representations (LDP-NRs) for creation of any kind of " +
					"resource, for example binary resources.")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-post-createbins",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED,
			comment = "Covers only part of the specification requirement. "
					+ "testPostNonRDFSource covers the rest.")
	public void testPostResourceAndGetFromContainer() throws IOException {
		// Test constants
		final String slug = "test",
				file = slug + ".png",
				mimeType = "image/png";

		// Make sure we can post binary resources
		Response response = postNonRDFSource(slug, file, mimeType);
		try {
			// Check the container contains the new resource
			Model model = buildBaseRequestSpecification()
					.header(ACCEPT, TEXT_TURTLE)
				.expect()
					.statusCode(HttpStatus.SC_OK)
					.contentType(HeaderMatchers.isTurtleCompatibleContentType())
				.get(container)
					.body().as(Model.class, new RdfObjectMapper(container));

			assertTrue(model.contains(model.createResource(container), model.createProperty(LDP.contains.stringValue()), model.createResource(response.getHeader(LOCATION))));
		} finally {
			buildBaseRequestSpecification().delete(response.getHeader(LOCATION));
		}
	}

	@Test(
			groups = {MAY},
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
		try {
			// And then check we get the binary back
			final String expectedMD5 = HashUtils.md5sum(NonRDFSourceTest.class.getResourceAsStream("/" + file));
			final byte[] binary = buildBaseRequestSpecification()
					.header(ACCEPT, mimeType)
				.expect()
					.statusCode(HttpStatus.SC_OK)
					.contentType(mimeType)
					.header(ETAG, HeaderMatchers.isValidEntityTag())
				.when()
					.get(response.getHeader(LOCATION))
					.body().asByteArray();
			assertEquals(expectedMD5, HashUtils.md5sum(binary), "md5sum");
		} finally {
			buildBaseRequestSpecification().delete(response.getHeader(LOCATION));
		}
	}

	@Test(
			groups = {MAY},
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

		try {
			String associatedRdfSource = getFirstLinkForRelation(location, LINK_REL_DESCRIBEDBY, container, response);
			Assert.assertNotNull(associatedRdfSource, "No Link response header with relation \"describedby\" " +
					"and anchor parameter matching the newly-created resource URI");

			// And then check we get the metadata of back
			buildBaseRequestSpecification()
					.header(ACCEPT, TEXT_TURTLE)
				.expect()
					.statusCode(HttpStatus.SC_OK)
					.contentType(HeaderMatchers.isTurtleCompatibleContentType())
					.header(ETAG, HeaderMatchers.isValidEntityTag())
				.when()
					.get(associatedRdfSource)
					.as(Model.class, new RdfObjectMapper(associatedRdfSource));

			// And the binary too
			final String expectedMD5 = HashUtils.md5sum(NonRDFSourceTest.class.getResourceAsStream("/" + file));
			final byte[] binary = buildBaseRequestSpecification()
					.header(ACCEPT, mimeType)
				.expect()
					.statusCode(HttpStatus.SC_OK)
					.contentType(mimeType)
					.header(ETAG, HeaderMatchers.isValidEntityTag())
				.when()
					.get(location)
					.body().asByteArray();
			assertEquals(expectedMD5, HashUtils.md5sum(binary), "md5sum");
		} finally {
			buildBaseRequestSpecification().delete(location);
		}
	}

	@Test(
			groups = {MAY},
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
		Response postResponse = postNonRDFSource(slug, file, mimeType);
		final String location = postResponse.getHeader(LOCATION);

		try {
			// And then check the link when requesting the LDP-NR
			Response getResponse = buildBaseRequestSpecification()
				.expect()
					.statusCode(isSuccessful())
					.header(ETAG, HeaderMatchers.isValidEntityTag())
				.when()
					.get(location);
			Assert.assertTrue(containsLinkHeader(
					location,
					LINK_REL_TYPE,
					LDP.NonRDFSource.stringValue(),
					location,
					getResponse
			));
		} finally {
			buildBaseRequestSpecification().delete(postResponse.header(LOCATION));
		}
	}

	@Test(
			groups = {MAY},
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
		Response postResponse = postNonRDFSource(slug, file, mimeType);
		String location = postResponse.getHeader(LOCATION);
		try {
			String associatedRdfSource = getFirstLinkForRelation(location, LINK_REL_DESCRIBEDBY, location, postResponse);
			Assert.assertNotNull(associatedRdfSource, "No Link response header with relation \"describedby\" " +
					"and anchor parameter matching the newly-created resource URI");

			// Check the link when requesting the LDP-NS
			Response getResponse = buildBaseRequestSpecification()
				.expect()
					.statusCode(isSuccessful())
					.header(ETAG, HeaderMatchers.headerPresent())
				.when()
					.get(location);
			Assert.assertTrue(containsLinkHeader(
					location,
					LINK_REL_DESCRIBEDBY,
					associatedRdfSource,
					location,
					getResponse
			));

			// And then check the associated LDP-RS is actually there
			buildBaseRequestSpecification()
					.header(ACCEPT, TEXT_TURTLE)
				.expect()
					.statusCode(isSuccessful())
					.contentType(HeaderMatchers.isTurtleCompatibleContentType())
					.header(ETAG, HeaderMatchers.isValidEntityTag())
				.when()
					.get(associatedRdfSource);
		} finally {
			buildBaseRequestSpecification().delete(location);
		}
	}

	@Test(
			groups = {MUST},
			description = "When a contained LDPR is deleted, and the LDPC server created an"+
					"associated LDP-RS (see the LDPC POST section), the LDPC server must also"+
					"delete the associated LDP-RS it created.",
			dependsOnMethods = "testPostResourceAndCheckAssociatedResource")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-del-contremovescontres",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testDeleteNonRDFSourceDeletesAssociatedResource() throws IOException {
		// Test constants
		final String slug = "test",
				file = slug + ".png",
				mimeType = "image/png";

		// Make sure we can post binary resources
		Response postResponse = postNonRDFSource(slug, file, mimeType);
		String location = postResponse.getHeader(LOCATION);
		boolean deleted = false;

		try {
			String associatedRdfSource = getFirstLinkForRelation(location, LINK_REL_DESCRIBEDBY, container, postResponse);
			Assert.assertNotNull(associatedRdfSource, "No Link response header with relation \"describedby\" " +
					"and anchor parameter matching the newly-created resource URI");

			// And then check the associated LDP-RS is actually there
			buildBaseRequestSpecification()
					.header(ACCEPT, TEXT_TURTLE)
				.expect()
					.statusCode(isSuccessful())
					.contentType(HeaderMatchers.isTurtleCompatibleContentType())
				.when()
					.get(associatedRdfSource);

			// Delete the LDP-NR.
			deleted = true;
			buildBaseRequestSpecification()
				.expect()
					.statusCode(isSuccessful())
				.when()
					.delete(location);

			// Check that the associated LDP-RS is also deleted.
			buildBaseRequestSpecification()
					.header(ACCEPT, TEXT_TURTLE)
				.expect()
					.statusCode(isNotFoundOrGone())
				.when()
					.get(associatedRdfSource);
		} finally {
			// Clean up if an assertion failed before we could delete the resource.
			if (!deleted) {
				buildBaseRequestSpecification().delete(location);
			}
		}
	}

	protected Response postNonRDFSource(String slug, String file, String mimeType) throws IOException {
		// Make sure we can post binary resources
		return buildBaseRequestSpecification()
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
			groups = {MUST},
			description = "When responding to requests whose request-URI is a LDP-NR with an"+
					"associated LDP-RS, a LDPC server must provide the same HTTP Link response"+
					"header as is required in the create response",
			dependsOnMethods = "testPostResourceAndCheckAssociatedResource")
	@SpecTest(
			specRefUri = LdpTestSuite.SPEC_URI + "#ldpc-options-linkmetahdr",
			testMethod = METHOD.AUTOMATED,
			approval = STATUS.WG_APPROVED)
	public void testOptionsHasSameLinkHeader() throws IOException {
		// Test constants
		final String slug = "test",
				file = slug + ".png",
				mimeType = "image/png";

		// Make sure we can post binary resources
		Response postResponse = postNonRDFSource(slug, file, mimeType);
		String location = postResponse.getHeader(LOCATION);

		try {
			String associatedRdfSource = getFirstLinkForRelation(location, LINK_REL_DESCRIBEDBY, container, postResponse);
			Assert.assertNotNull(associatedRdfSource, "No Link response header with relation \"describedby\" " +
					"and anchor parameter matching the newly-created resource URI");

			// Check the Link headers on an HTTP OPTIONS for the LDP-NR
			Response optionsResponse = buildBaseRequestSpecification()
				.expect()
					.statusCode(isSuccessful())
				.when()
					.options(location);
			Assert.assertTrue(containsLinkHeader(
						location,
						LINK_REL_DESCRIBEDBY,
						associatedRdfSource,
						location,
						optionsResponse
					),
					"No Link response header with relation \"describedby\" and URI <"
							+ associatedRdfSource + "> for LDP-NR OPTIONS request");
		} finally {
			buildBaseRequestSpecification().delete(location);
		}
	}

}
