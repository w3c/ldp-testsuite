package org.w3.ldp.testsuite.reporter;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.jena.JenaJSONLD;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.vocabulary.DOAP;
import com.hp.hpl.jena.vocabulary.DCTerms;

import org.testng.*;
import org.testng.internal.Utils;
import org.testng.xml.XmlSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.vocab.Earl;

import java.io.*;
import java.util.*;

/**
 * Earl Reporter for the LDP Test Suite. Takes in the results of the test suite
 * and reports the information to a Turtle file and a JSON-LD file, both of
 * which contains Earl vocabulary.
 */
public class LdpEarlReporter implements IReporter {

	private BufferedWriter writerTurtle;
	private BufferedWriter writerJson;
	private Model model;

	private static final String LDPT_NAME = "http://w3c.github.io/ldp-testsuite#";

	private static final String PASS = "TEST PASSED";
	private static final String FAIL = "TEST FAILED";
	private static final String SKIP = "TEST SKIPPED";
	private static final String TURTLE = "TURTLE";
	private static final String JSON_LD = "JSON-LD";
	private static final String outputDir = "report"; // directory for results

	// private static final String DIRECT_TEST = "DirectContainerTest";
	// private static final String MEMBER_TEST = "MemberResourceTest";
	// private static final String BASIC_TEST = "BasicContainerTest";
	// private static final String INDIRECT_TEST = "IndirectContainerTest";
	//
	// private static String direct;
	// private static String member;
	// private static String basic;
	// private static String indirect;

	private static String softwareTitle;
	private static String subjectDev;
	private static String homepage;
	private static String subjectName;
	private static String refPage;
	private static String language;

	static {
		JenaJSONLD.init();
	}

	@Override
	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites,
			String outputDirectory) {
		try {
			createWriter(outputDir);
		} catch (IOException e) {
		}
		createModel();
		createAssertions(suites);
		write();
		try {
			endWriter();
		} catch (IOException e) {
		}
	}

	private void createAssertions(List<ISuite> suites) {
		for (ISuite suite : suites) {
			// Acquire parameters
			// direct = suite.getParameter("directContainer");
			// member = suite.getParameter("memberResource");
			// basic = suite.getParameter("basicContainer");
			// indirect = suite.getParameter("indirectContainer");

			homepage = suite.getParameter("homepage");
			refPage = suite.getParameter("referencePage");
			subjectName = suite.getParameter("subjectName");

			softwareTitle = suite.getParameter("software");
			subjectDev = suite.getParameter("developer");
			language = suite.getParameter("language");

			// Make the Assertor Resource
			Resource assertor = model.createResource(refPage, Earl.Assertor);
			assertor.addProperty(DOAP.description, suite.getName());

			/* Software Resource */
			Resource softResource = model
					.createResource(refPage, Earl.Software);
			if (softwareTitle != null)
				model.createResource(null, softResource);

			/* Add properties to the Test Subject Resource */

			Resource subjectResource = model.createResource(refPage,
					Earl.TestSubject);
			// String testClass = result.getTestClass().getName();

			if (homepage != null)
				subjectResource.addProperty(DOAP.homepage, homepage);

			if (subjectName != null)
				subjectResource.addProperty(DOAP.name, subjectName);

			if (subjectDev != null) {
				subjectResource.addProperty(DOAP.developer, subjectDev);
				// TODO acquire and add the information about the developer.
			}
			if (language != null)
				subjectResource
						.addProperty(DOAP.programming_language, language);
			model.createResource(null, subjectResource);

			Map<String, ISuiteResult> tests = suite.getResults();

			for (ISuiteResult results : tests.values()) {
				ITestContext testContext = results.getTestContext();
				getResultProperties(testContext.getFailedTests(), FAIL);
				getResultProperties(testContext.getSkippedTests(), SKIP);
				getResultProperties(testContext.getPassedTests(), PASS);
			}

		}

	}

	private void getResultProperties(IResultMap tests, String status) {
		for (ITestResult result : tests.getAllResults()) {
			makeResultResource(result, status);
		}
	}

	private void makeResultResource(ITestResult result, String status) {
		Resource assertionResource;

		String declaringClass = result.getTestClass().getName();
		declaringClass = declaringClass.substring(declaringClass
				.lastIndexOf(".") + 1);
		if (refPage != null) {
			String uri = refPage + "#" + declaringClass + "."
					+ result.getName();
			assertionResource = model.createResource(uri, Earl.Assertion);
		} else
			assertionResource = model.createResource(null, Earl.Assertion);

		// Resource caseResource = model.createResource(null, Earl.TestCase);
		Resource resultResource = model.createResource(null, Earl.TestResult);

		assertionResource.addProperty(Earl.testSubject, refPage);

		assertionResource.addProperty(
				Earl.test,
				ResourceFactory.createProperty(LDPT_NAME + declaringClass + "."
						+ result.getName()));

		/* Test Result Resource */
		resultResource.addProperty(DCTerms.title, status);
		switch (status) {
		case FAIL:
			resultResource.addProperty(Earl.outcome, Earl.fail);
			break;
		case PASS:
			resultResource.addProperty(Earl.outcome, Earl.pass);
			break;
		case SKIP:
			resultResource.addProperty(Earl.outcome, Earl.skip);
			break;
		default:
			break;
		}

		if (result.getThrowable() != null) {
			createExceptionProperty(result.getThrowable(), resultResource);
		}

		if (result.getMethod().getConstructorOrMethod().getMethod()
				.getAnnotation(SpecTest.class) != null) {

			SpecTest test = result.getMethod().getConstructorOrMethod()
					.getMethod().getAnnotation(SpecTest.class);
			METHOD type = test.testMethod();

			switch (type) {
			case AUTOMATED:
				assertionResource.addProperty(Earl.mode, Earl.automatic);
				break;
			case MANUAL:
				assertionResource.addProperty(Earl.mode, Earl.manual);
				break;
			case NOT_IMPLEMENTED:
				assertionResource.addProperty(Earl.mode, Earl.notTested);
				break;
			case CLIENT_ONLY:
				assertionResource.addProperty(Earl.mode, Earl.notTested);
				break;
			default:
				assertionResource.addProperty(Earl.mode, Earl.notTested);
				break;
			}
		}

		assertionResource.addProperty(Earl.assertedBy, refPage);

		/*
		 * Add the above resources to the Assertion Resource
		 */
		assertionResource.addProperty(Earl.testResult, resultResource);

	}

	private void createExceptionProperty(Throwable thrown, Resource resource) {
		if (thrown.getClass().getName().contains(SKIP))
			resource.addProperty(DCTerms.description, thrown.getMessage());
		else
			resource.addLiteral(DCTerms.description,
					Utils.stackTrace(thrown, false)[0]);
	}

	private void createWriter(String directory) throws IOException {
		writerTurtle = null;
		writerJson = null;
		new File(directory).mkdirs();
		writerTurtle = new BufferedWriter(new FileWriter(directory
				+ "/ldp-testsuite-execution-report-earl.ttl"));
		writerJson = new BufferedWriter(new FileWriter(directory
				+ "/ldp-testsuite-execution-report-earl.jsonld", false));

	}

	private void write() {
		model.write(writerTurtle, TURTLE);

		StringWriter sw = new StringWriter();
		model.write(sw, JSON_LD);
		try {

			Object jsonObject = JsonUtils.fromString(sw.toString());

			HashMap<String, String> context = new HashMap<String, String>();
			// Customise context
			context.put("dcterms", "http://purl.org/dc/terms/");
			context.put("earl", "http://www.w3.org/ns/earl#");
			context.put("foaf", "http://xmlns.com/foaf/0.1/");
			context.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

			// Create an instance of JsonLdOptions with the standard JSON-LD
			// options (will just be default for now)
			JsonLdOptions options = new JsonLdOptions();
			Object compact = JsonLdProcessor.compact(jsonObject, context,
					options);

			writerJson.write(JsonUtils.toPrettyString(compact));
		} catch (IOException | JsonLdError e) {
			e.printStackTrace();
		}

	}

	private void endWriter() throws IOException {
		writerTurtle.flush();
		writerTurtle.close();

		writerJson.flush();
		writerTurtle.close();
	}

	private void createModel() {
		model = ModelFactory.createDefaultModel();
		writePrefixes();
	}

	private void writePrefixes() {
		model.setNsPrefix("doap", "http://usefulinc.com/ns/doap#");
		model.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
		model.setNsPrefix("earl", "http://www.w3.org/ns/earl#");
		model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		model.setNsPrefix("mf",
				"http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#");
		model.setNsPrefix("rdft", "http://www.w3.org/ns/rdftest#");
		model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
		model.setNsPrefix("ldpt", "http://w3c.github.io/ldp-testsuite#");
	}

}
