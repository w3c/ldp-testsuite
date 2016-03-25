package org.w3.ldp.testsuite.reporter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.testng.IReporter;
import org.testng.IResultMap;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import org.testng.internal.Utils;
import org.testng.xml.XmlSuite;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.vocab.Earl;
import org.w3.ldp.testsuite.vocab.LDP;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.DOAP;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;

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

	private static String software;
	private static String developer;
	private static String homepage;
	private static String assertor;
	private static String language;
	private static String mbox;
	private static String description;
	private static String shortname;
	private static ArrayList<String> missingParms = new ArrayList<>();

	private static Property ranAsClass = ResourceFactory
			.createProperty(LDP.LDPT_NAMESPACE + "ranAsClass");

	private static String TITLE = "ldp-testsuite";

	private IResultMap passedTests;
	private IResultMap failedTests;
	private IResultMap skippedTests;

	private String outputDirectory = LdpTestSuite.OUTPUT_DIR;

	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	@Override
	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
		try {
			createWriter(this.outputDirectory, "");
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
		createModel();
		createAssertions(suites);
		write();
		if (missingParms.size() > 0) {
			System.out.print("EARL report missing values for parameters: ");
			boolean first=true;
			String o="";
			for (String p: missingParms) {
				if (first) {
					first = false;
					o = p;
				} else {
					o = o + ", " + p;
				}
			}
			System.out.println(o);
		}
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
			if (homepage == null) missingParms.add("homepage");
			
			assertor = suite.getParameter("assertor");
			if (assertor == null) missingParms.add("assertor");
			
			software = suite.getParameter("software");
			if (software == null) missingParms.add("software");
			
			developer = suite.getParameter("developer");
			if (developer == null) missingParms.add("developer");
			
			language = suite.getParameter("language");
			if (language == null) missingParms.add("language");

			mbox = suite.getParameter("mail");
			if (mbox == null) missingParms.add("mail");

			description = suite.getParameter("description");
			if (description == null) missingParms.add("description");

			shortname = suite.getParameter("shortname");
			if (shortname == null) missingParms.add("shortname");

			// Make the Assertor Resource (the thing doing the testing) 
			Resource assertorRes = model.createResource(assertor);
			assertorRes.addProperty(RDF.type, Earl.Assertor);
			
			// Create the subject resource (the thing being tested)
			Resource subjectResource = model.createResource(homepage,
					Earl.TestSubject);

			if (description != null)
				subjectResource.addProperty(DOAP.description, description);

			/* Developer Resource (Person) */
			Resource personResource = model.createResource(null, FOAF.Person);
			if (mbox != null)
				personResource.addProperty(FOAF.mbox, mbox);
			if(developer != null)
				personResource.addProperty(FOAF.name, developer);

			subjectResource.addProperty(DOAP.developer, personResource);

			/* Software Resource */
			Resource softResource = model
					.createResource(homepage, Earl.Software);
			if (software != null)
				softResource.addProperty(DCTerms.title, software);

			if(shortname != null)
				softResource.addProperty(DOAP.name, shortname);

			/* Add properties to the Test Subject Resource */
			subjectResource.addProperty(RDF.type, DOAP.Project);

			if (homepage != null)
				subjectResource.addProperty(DOAP.homepage, homepage);

			if (language != null)
				subjectResource
						.addProperty(DOAP.programming_language, language);

			Map<String, ISuiteResult> tests = suite.getResults();

			for (ISuiteResult results : tests.values()) {
				ITestContext testContext = results.getTestContext();
				passedTests = testContext.getPassedTests();
				failedTests = testContext.getFailedTests();
				skippedTests = testContext.getSkippedTests();
				getResultProperties(failedTests, FAIL);
				getResultProperties(skippedTests, SKIP);
				getResultProperties(passedTests, PASS);
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

		Resource subjectResource = model.getResource(homepage);
		Resource assertorResource = model.getResource(assertor);

		assertionResource.addProperty(Earl.testSubject, subjectResource);

		assertionResource.addProperty(
				Earl.test,
				model.getResource(createTestCaseURL(className, result.getName())));

		/* Test Result Resource */
		Method method = result.getMethod().getConstructorOrMethod().getMethod();
		if(method.getAnnotation(SpecTest.class) != null){
			SpecTest specTest = method.getAnnotation(SpecTest.class);
			if(specTest.coveredByGroups().length > 0 && specTest.coveredByGroups().length > 0){
				ArrayList<String> testResults = new ArrayList<String>();
				for(Class<?> classVal : specTest.coveredByTests()){
					Method[] classMethod = classVal.getDeclaredMethods();
					for(Method methodName : classMethod) {
						if(methodName.getAnnotation(Test.class) != null) {
							String group = Arrays.toString(methodName.getAnnotation(Test.class).groups());
							for(String groupCover : specTest.coveredByGroups()) {
								if(group.contains(groupCover) && !methodName.getName().contains("Conforms")) {
									testResults.add(findTestResult(methodName.getName()));
								}
							}
						}
					}
				}
				// evaluate the testResults for the Indirect Test
				// if one of the tests fails, then the entire indirect test fails
				// if there are only pass and skipped tests (none failed), then it passes
				// if there are only skipped (none passed or failed), then it is skipped
				if(testResults.size() > 0){
					if(testResults.contains(FAIL))
						status = FAIL;
					else if(testResults.contains(PASS) && !testResults.contains(FAIL))
						status = PASS;
					else if(testResults.contains(SKIP) && !testResults.contains(FAIL) && !testResults.contains(PASS))
						status = SKIP;
				}
			}
		}
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
			case INDIRECT:
				assertionResource.addProperty(Earl.mode, Earl.automatic);
				break;
			default:
				assertionResource.addProperty(Earl.mode, Earl.notTested);
				break;
			}
		}

		assertionResource.addProperty(Earl.assertedBy, assertorResource);
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

	private String  findTestResult(String methodName) {
		Iterator<ITestNGMethod> passed = passedTests.getAllMethods().iterator();
		while(passed.hasNext()){
			ITestNGMethod method = passed.next();
			if(method.getMethodName().equals(methodName)){
				return PASS;
			}
		}

		Iterator<ITestNGMethod> skipped = skippedTests.getAllMethods().iterator();
		while(skipped.hasNext()){
			ITestNGMethod method = skipped.next();
			if(method.getMethodName().equals(methodName)){
				return SKIP;
			}
		}

		Iterator<ITestNGMethod> failed = failedTests.getAllMethods().iterator();
		while(failed.hasNext()){
			ITestNGMethod method = failed.next();
			if(method.getMethodName().equals(methodName)){
				return FAIL;
			}
		}

		return null;
	}

	@Override
    protected String getFilename() {
	    return TITLE + "-execution-report-earl";
    }

	public void setTitle(String title){
		TITLE = title;
	}
}
