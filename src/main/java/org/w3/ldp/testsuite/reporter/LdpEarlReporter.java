package org.w3.ldp.testsuite.reporter;

import java.io.IOException;
import java.util.GregorianCalendar;
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
import org.w3.ldp.testsuite.vocab.LDP;

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
public class LdpEarlReporter extends AbstractEarlReporter implements IReporter {

	private static final String PASS = "TEST PASSED";
	private static final String FAIL = "TEST FAILED";
	private static final String SKIP = "TEST SKIPPED";

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
	private static String assertor;
	private static String language;
	private static String mailBox;
	private static String description;
	
	private static Property ranAsClass = ResourceFactory
			.createProperty(LDP.LDPT_NAMESPACE + "ranAsClass");

	@Override
	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites,
			String outputDirectory) {
		try {
			createWriter(OUTPUT_DIR);
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
			assertor = suite.getParameter("assertor");
			subjectName = suite.getParameter("subjectname");

			softwareTitle = suite.getParameter("software");
			subjectDev = suite.getParameter("developer");
			language = suite.getParameter("language");

			mailBox = suite.getParameter("mbox");
			description = suite.getParameter("description");

			// Make the Assertor Resource
			Resource assertorRes = model.createResource(assertor);
			assertorRes.addProperty(RDF.type, Earl.Assertor);
			
			if (description != null)
				assertorRes.addProperty(DOAP.description, description);

			/* Developer Resource (Person) */
			Resource personResource = model.createResource(null, FOAF.Person);
			if (mailBox != null && subjectDev != null) {
				personResource.addProperty(FOAF.mbox, mailBox); // FIXME: Add in
																// the mailto
				personResource.addProperty(FOAF.name, subjectDev);
			}

			assertorRes.addProperty(DOAP.developer, personResource);

			/* Software Resource */
			Resource softResource = model
					.createResource(assertor, Earl.Software);
			if (softwareTitle != null)
				model.createResource(null, softResource);

			/* Add properties to the Test Subject Resource */

			Resource subjectResource = model.createResource(assertor,
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
		String className = result.getTestClass().getName();
		className = className.substring(className
				.lastIndexOf(".") + 1);
		
		Resource assertionResource = model.createResource(null, Earl.Assertion);

		Resource resultResource = model.createResource(null, Earl.TestResult);
		
		Resource subjectResource = model.getResource(assertor);

		assertionResource.addProperty(Earl.testSubject, subjectResource);

		assertionResource.addProperty(
				Earl.test,
				model.getResource(createTestCaseURL(className, result.getName())));

		/* Test Result Resource */
		switch (status) {
		case FAIL:
			resultResource.addProperty(Earl.outcome, Earl.failed);
			break;
		case PASS:
			resultResource.addProperty(Earl.outcome, Earl.passed);
			break;
		case SKIP:
			resultResource.addProperty(Earl.outcome, Earl.untested);
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

	@Override
    protected String getFilename() {
	    return "ldp-testsuite-execution-report-earl";
    }
}
