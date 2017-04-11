package org.w3.ldp.testsuite.test;

import org.apache.jena.rdf.model.Model;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.w3.ldp.testsuite.exception.SkipException;
import org.w3.ldp.testsuite.mapper.RdfObjectMapper;

import java.io.IOException;

import static org.w3.ldp.testsuite.http.HttpHeaders.LOCATION;
import static org.w3.ldp.testsuite.http.MediaTypes.TEXT_TURTLE;

/**
 * Tests that run on an LDP-RS that is not a container.
 */
public class MemberResourceTest extends RdfSourceTest {
	private static final String SETUP_ERROR = "ERROR: Could not create test resource for MemberResourceTest. Skipping tests.";

	private String container;
	private String memberResource;

	@Parameters("auth")
	public MemberResourceTest(@Optional String auth) throws IOException {
		super(auth);
	}

	/*
	 * Creates a resource to test if there's no memberResource test parameter.
	 */
	@Parameters({"memberResource", "directContainer", "indirectContainer", "basicContainer", "memberTtl"})
	@BeforeSuite(alwaysRun = true)
	public void createTestResource(@Optional String memberResource, @Optional String directContainer,
			@Optional String indirectContainer, @Optional String basicContainer,
			@Optional String memberTtl) {
		// If resource is defined, use that. Otherwise, fall back to creating one from one of the containers.
		if (memberResource != null) {
			this.memberResource = memberResource;
		} else if (directContainer != null) {
			this.container = directContainer;
		} else if (indirectContainer != null) {
			this.container = indirectContainer;
		} else if (basicContainer != null) {
			this.container = basicContainer;
		} else {
			throw new SkipException(Thread.currentThread().getStackTrace()[1].getMethodName(),
					"No memberResource or container parameters defined in testng.xml", skipLog);
		}

		if (this.memberResource == null) {
			try {
				Model model = this.readModel(memberTtl);
				if (model == null) {
					model = this.getDefaultModel();
				}

				Response postResponse = buildBaseRequestSpecification()
						.contentType(TEXT_TURTLE)
						.body(model, new RdfObjectMapper())
						.post(this.container);
				if (postResponse.getStatusCode() != HttpStatus.SC_CREATED) {
					System.err.println(SETUP_ERROR);
					System.err.println("POST failed with status code: " + postResponse.getStatusCode());
					System.err.println();
					return;
				}

				this.memberResource = postResponse.getHeader(LOCATION);
				if (this.memberResource == null) {
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
	}

	@Override
	protected String getResourceUri() {
		if (memberResource == null) {
			throw new SkipException(Thread.currentThread().getStackTrace()[2].getMethodName(),
					"Skipping test because test resource is null.", skipLog);
		}
		return memberResource;
	}

	/*
	 * Deletes the test resource to clean up if it's wasn't provided using the
	 * memberResource test parameter.
	 */
	@AfterSuite(alwaysRun = true)
	public void deleteTestResource() {
		// If container isn't null, we created the resource ourselves. To clean up, delete the resource.
		if (container != null) {
			buildBaseRequestSpecification().delete(memberResource);
		}
	}
}
