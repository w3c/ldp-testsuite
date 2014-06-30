package org.w3.ldp.testsuite.reporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.IReporter;
import org.testng.IResultMap;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.internal.Utils;
import org.testng.xml.XmlSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.vocab.Earl;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.jena.JenaJSONLD;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.vocabulary.DOAP;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;

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
	private static String mailBox;
	private static String description;
	
	private static Property ranAsClass = ResourceFactory
			.createProperty(LDPT_NAME + "ranAsClass");

	static {
		JenaJSONLD.init();
	}

	@Override
	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites,
			String outputDirectory) {
		try {
			createWriter(outputDir);
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
		createModel();
		createAssertions(suites);
		write();
		try {
			endWriter();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
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

			mailBox = suite.getParameter("mail");
			description = suite.getParameter("description");

			// Make the Assertor Resource
			Resource assertor = model.createResource(refPage);
			assertor.addProperty(RDF.type, Earl.Assertor);
			
			if (description != null)
				assertor.addProperty(DOAP.description, description);

			/* Developer Resource (Person) */
			Resource personResource = model.createResource(null, FOAF.Person);
			if (mailBox != null && subjectDev != null) {
				personResource.addProperty(FOAF.mbox, mailBox); // FIXME: Add in
																// the mailto
				personResource.addProperty(FOAF.name, subjectDev);
			}

			assertor.addProperty(DOAP.developer, personResource);

			/* Software Resource */
			Resource softResource = model
					.createResource(refPage, Earl.Software);
			if (softwareTitle != null)
				model.createResource(null, softResource);

			/* Add properties to the Test Subject Resource */

			Resource subjectResource = model.createResource(refPage,
					Earl.TestSubject);
			
			subjectResource.addProperty(RDF.type, DOAP.Project);

			if (homepage != null)
				subjectResource.addProperty(DOAP.homepage, homepage);

			if (subjectName != null)
				subjectResource.addProperty(DOAP.name, subjectName);

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
		String declaringClass = result.getMethod().getConstructorOrMethod().getMethod().getDeclaringClass().getName();
		declaringClass = declaringClass.substring(declaringClass
				.lastIndexOf(".") + 1);
		
		Resource assertionResource = model.createResource(null, Earl.Assertion);

		Resource resultResource = model.createResource(null, Earl.TestResult);
		
		Resource subjectResource = model.getResource(refPage);

		assertionResource.addProperty(Earl.testSubject, subjectResource);

		assertionResource.addProperty(
				Earl.test,
				model.getResource(LdpEarlTestManifest.createTestCaseURL(declaringClass, result.getName())));

		/* Test Result Resource */
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

		assertionResource.addProperty(Earl.assertedBy, subjectResource);
		assertionResource.addLiteral(ranAsClass, result.getTestClass().getRealClass().getSimpleName());

		resultResource.addProperty(DCTerms.date, model.createTypedLiteral(GregorianCalendar.getInstance()));
		
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
		model.setNsPrefix("ldpt", LDPT_NAME);
	}

}
