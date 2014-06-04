package org.w3.ldp.testsuite.reporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.openrdf.model.vocabulary.EARL;
import org.testng.IReporter;
import org.testng.IResultMap;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.internal.Utils;
import org.testng.xml.XmlSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
//import org.w3.ldp.testsuite.annotations.Reference;
import org.w3.ldp.testsuite.vocab.Earl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;

/**
 * Earl Reporter for the LDP Test Suite. Takes in the results of the test suite
 * and reports the information to a Turtle file, which contains Earl vocabulary.
 */
public class LdpEarlReporter implements IReporter {

	private BufferedWriter writer;
	private Model model;

	private static final String PASS = "TEST PASSED";
	private static final String FAIL = "TEST FAILED";
	private static final String SKIP = "TEST SKIPPED";
	private static final String TURTLE = "TURTLE";
	private static final String outputDir = "report"; // directory where results
														// will go

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

			Map<String, ISuiteResult> tests = suite.getResults();

			for (ISuiteResult results : tests.values()) {
				ITestContext testContext = results.getTestContext();
				getResultProperties(testContext.getFailedTests(), FAIL,
						suite.getName());
				getResultProperties(testContext.getSkippedTests(), SKIP,
						suite.getName());
				getResultProperties(testContext.getPassedTests(), PASS,
						suite.getName());
			}

		}

	}

	private void getResultProperties(IResultMap tests, String status,
			String name) {
		for (ITestResult result : tests.getAllResults()) {
			makeResultResource(result, status, name);
		}
	}

	private void makeResultResource(ITestResult result, String status,
			String name) {
		Resource assertionResource = model.createResource(null, Earl.Assertion);
		Resource caseResource = model.createResource(null, Earl.TestCase);
		Resource subjectResource = model.createResource(null, Earl.TestSubject);
		Resource resultResource = model.createResource(null, Earl.TestResult);
		Resource assertorResource = model.createResource(null, Earl.Assertor);
		Resource modeResource = model.createResource(EARL.AUTOMATIC.toString(),
				Earl.TestMode);

		/* Add properties to the Test Subject Resource */
		String declaringClass = result.getTestClass().getName();
		subjectResource.addProperty(DCTerms.description, "Declaring Class: "
				+ declaringClass);

		subjectResource.addProperty(DCTerms.title, result.getName());

		/* Assertor Resource */
		assertorResource.addProperty(DCTerms.title, name);

		/* Test Criterion/Test Case Resource */
		caseResource.addProperty(DCTerms.description, (result.getMethod()
				.getDescription() != null ? result.getMethod().getDescription()
				: "No Description available"));

		String groups = groups(result.getMethod().getGroups());

		caseResource.addProperty(DCTerms.subject, "Groups: " + groups);
		if (result.getMethod().getConstructorOrMethod().getMethod()
				.getAnnotation(SpecTest.class).specRefUri() != null) {
			caseResource.addProperty(DCTerms.relation,
					result.getMethod().getConstructorOrMethod().getMethod()
							.getAnnotation(SpecTest.class).specRefUri());
		}

		/* Test Result Resource */
		long time = result.getEndMillis() - result.getStartMillis();
		resultResource.addLiteral(DCTerms.date, time + " Msec");
		resultResource.addProperty(DCTerms.title, status);

		if (result.getThrowable() != null) {
			createExceptionProperty(result.getThrowable(), resultResource);
		}

		/* Add the above resources to the Assertion Resource */
		assertionResource.addProperty(Earl.test, caseResource);
		assertionResource.addProperty(Earl.testResult, resultResource);
		assertionResource.addProperty(Earl.testSubject, subjectResource);
		assertionResource.addProperty(Earl.assertedBy, assertorResource);
		assertionResource.addProperty(Earl.mode, modeResource);
	}

	private void createExceptionProperty(Throwable thrown, Resource resource) {
		if (thrown.getClass().getName().contains(SKIP))
			resource.addProperty(Earl.outcome, thrown.getMessage());
		else
			resource.addLiteral(Earl.outcome,
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
		writer = null;
		new File(directory).mkdirs();
		writer = new BufferedWriter(new FileWriter(directory
				+ "/EarlTestSuiteReport.ttl"));
	}

	private void write() {
		model.write(writer, TURTLE);
	}

	private void endWriter() throws IOException {
		writer.flush();
		writer.close();
	}

	private void createModel() {
		model = ModelFactory.createDefaultModel();
	}

}
