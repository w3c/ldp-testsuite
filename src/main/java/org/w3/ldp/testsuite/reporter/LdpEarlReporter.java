package org.w3.ldp.testsuite.reporter;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.jena.JenaJSONLD;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
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

	private static final String PASS = "TEST PASSED";
	private static final String FAIL = "TEST FAILED";
	private static final String SKIP = "TEST SKIPPED";
	private static final String TURTLE = "TURTLE";
	private static final String JSON_LD = "JSON-LD";
	private static final String outputDir = "report"; // directory where results
	// will go

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
			subjectName = suite.getParameter("subjectName");

			softwareTitle = suite.getParameter("software");
			subjectDev = suite.getParameter("developer");
			language = suite.getParameter("language");

			// Make the Assertor Resource
			Resource assertor = model.createResource(null, Earl.Assertor);
			assertor.addProperty(DCTerms.title, suite.getName());

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
		Resource assertionResource = model.createResource(null, Earl.Assertion);
		Resource caseResource = model.createResource(null, Earl.TestCase);
		Resource subjectResource = model.createResource(null, Earl.TestSubject);
		Resource resultResource = model.createResource(null, Earl.TestResult);
		Resource softResource = model.createResource(null, Earl.Software);
		// Resource modeResource = null;

		/* Add properties to the Test Subject Resource */

		// String testClass = result.getTestClass().getName();

		if (homepage != null)
			subjectResource.addProperty(DOAP.homepage, homepage);

		if (subjectName != null)
			subjectResource.addProperty(DOAP.name, subjectName);

		if (subjectDev != null)
			subjectResource.addProperty(DOAP.developer, subjectDev);
		if (language != null)
			subjectResource.addProperty(DOAP.programming_language, language);

		/* Software Resource */
		if (softwareTitle != null)
			softResource.addProperty(DCTerms.title, softwareTitle);

		/* Test Criterion/Test Case Resource */
		String declaringClass = result.getTestClass().getName();

		caseResource.addProperty(DCTerms.description, "Declaring Class: "
				+ declaringClass
				+ " - "
				+ (result.getMethod().getDescription() != null ? result
						.getMethod().getDescription()
						: "No Description available"));

		String groups = groups(result.getMethod().getGroups());

		caseResource.addProperty(DCTerms.subject, "Groups: " + groups);

		Calendar cal = GregorianCalendar.getInstance();
		Literal value = model.createTypedLiteral(cal);
		caseResource.addProperty(DCTerms.date, value);

		/* Test Result Resource */

		// long time = result.getEndMillis() - result.getStartMillis();
		// resultResource.addProperty(Earl.time, time + " Msec");

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
				assertionResource.addProperty(Earl.mode,
						model.createResource(Earl.Automatic));
				break;
			case MANUAL:
				assertionResource.addProperty(Earl.mode,
						model.createResource(Earl.Manual));
				break;
			case NOT_IMPLEMENTED:
				assertionResource.addProperty(Earl.mode,
						model.createResource(Earl.NotTested));
				break;
			case CLIENT_ONLY:
				assertionResource.addProperty(Earl.mode,
						model.createResource(Earl.NotTested));
				break;
			default:
				assertionResource.addProperty(Earl.mode,
						model.createResource(Earl.NotTested));
				break;
			}
			// modeResource.addProperty(DCTerms.title, type.toString());
			// modeResource.addProperty(DCTerms.description, result.getName());
		}

		// setProperty(Earl.mode, model.createResource(Earl.Automatic));

		/*
		 * Add the above resources to the Assertion Resource
		 */
		assertionResource.addProperty(Earl.test, caseResource);
		assertionResource.addProperty(Earl.testSubject, subjectResource);
		assertionResource.addProperty(Earl.testResult, resultResource);
		assertionResource.addProperty(Earl.assertedBy, softResource);

	}

	private void createExceptionProperty(Throwable thrown, Resource resource) {
		if (thrown.getClass().getName().contains(SKIP))
			resource.addProperty(DCTerms.description, thrown.getMessage());
		else
			resource.addLiteral(DCTerms.description,
					Utils.stackTrace(thrown, false)[0]);
	}

	private String groups(String[] list) {
		if (list.length == 0)
			return null;
		String retList = "";
		for (int i = 0; i < list.length; i++) {
			if (i == list.length - 1)
				retList += list[i];
			else
				retList += list[i] + ", ";
		}
		return retList;
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
	}

}
