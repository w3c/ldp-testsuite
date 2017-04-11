package org.w3.ldp.testsuite.reporter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.vocab.LDP;
import org.w3.ldp.testsuite.vocab.TestDescription;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.EARL;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.TestManifest;

public class LdpEarlTestManifest extends AbstractEarlReporter {
	
	private static String namespace = LDP.LDPT_NAMESPACE;

	private static final Property declaredInClass = ResourceFactory
			.createProperty(namespace + "declaredInClass");
	private static final Property declaredTestCase = DCTerms.source;
	private static final Property conformanceLevel = ResourceFactory
			.createProperty(namespace + "conformanceLevel");

	/**
	 * A link to the published Javadoc for the test case (which in turn links to
	 * the source code).
	 */
	private static final Property documentation = ResourceFactory
			.createProperty(namespace + "documentation");

	/**
	 * The test method: {@link #automated}, {@link #manual}, {@link #clientOnly},
	 * or {@link #notImplemented}.
	 *
	 * <p>
	 * earl:mode is inappropriate here since its domain is earl:Assertion.
	 * </p>
	 *
	 * @see SpecTest#testMethod()
	 * @see <a href="http://www.w3.org/TR/EARL10-Schema/#mode">EARL schema -
	 *      earl:mode</a>
	 */
	private static final Property testMethod = ResourceFactory
			.createProperty(namespace + "testMethod");

	/**
	 * @see SpecTest.METHOD#AUTOMATED
	 */
	private static final Resource automated = ResourceFactory
			.createResource(namespace + "automated");

	/**
	 * @see SpecTest.METHOD#MANUAL
	 */
	private static final Resource manual = ResourceFactory
			.createResource(namespace + "manual");

	/**
	 * @see SpecTest.METHOD#CLIENT_ONLY
	 */
	private static final Resource clientOnly = ResourceFactory
			.createResource(namespace + "clientOnly");

	/**
	 * @see SpecTest.METHOD#NOT_IMPLEMENTED
	 */
	private static final Resource notImplemented = ResourceFactory
			.createResource(namespace + "notImplemented");
	/**
	 * @see SpecTest.METHOD#INDIRECT
	 */
	private static final Resource indirect = ResourceFactory
			.createResource(namespace + "indirect");

	/**
	 * @see SpecTest.steps
	 */
	private static final Property steps = ResourceFactory
			.createProperty(namespace + "steps");

	/**
	 * List of GROUPS to include in reporting
	 */
	private static List<String> conformanceLevels = new ArrayList<String>();

	public void setConformanceLevels(List<String> list){
		conformanceLevels = list;
	}

	public void generate(Map<Class<?>, String> classes, String title) {
		try {
			createWriter(LdpTestSuite.OUTPUT_DIR, title);
			System.out.println("Writing test manifest...");
			createModel();
			writeTestClasses(classes);
			write();
			System.out.println("Done!");
			endWriter();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
	
	public void setNamespaceUri(String name){
		namespace = name;
	}

	private void writeManifest(ArrayList<Resource> testcases, String localName, String label, String description) {
		if (testcases.size() == 0) return;

		Resource manifest = model.createResource(namespace + localName+"Manifest",
				TestManifest.Manifest);
		manifest.addProperty(DCTerms.title, label);
		manifest.addProperty(TestManifest.name, label);
		manifest.addProperty(RDFS.comment, description);
		Resource[] ra={};
		RDFList l = model.createList(testcases.toArray(ra));
		manifest.addProperty(TestManifest.entries, l);
	}

	private <T> void writeInfo(Class<T> testClass, String title, String description) {
		ArrayList<ArrayList<Resource>> conformanceClasses = new ArrayList<ArrayList<Resource>>();
		conformanceClasses.add(new ArrayList<Resource>());
		conformanceClasses.add(new ArrayList<Resource>());
		conformanceClasses.add(new ArrayList<Resource>());
		conformanceClasses.add(new ArrayList<Resource>());

		String className = testClass.getCanonicalName();
		Method[] methods = testClass.getMethods();
		for (Method method : methods) {
			if (method.isAnnotationPresent(Test.class)) {
				generateInformation(method, className, conformanceClasses);
			}
		}
		writeManifest(conformanceClasses.get(LdpTestCaseReporter.MUST), title
				+ "-MUST", title + " (MUST)", description
				+ " MUST conformance tests.");
		writeManifest(conformanceClasses.get(LdpTestCaseReporter.SHOULD), title
				+ "-SHOULD", title + " (SHOULD)", description
				+ " SHOULD conformance tests.");
		writeManifest(conformanceClasses.get(LdpTestCaseReporter.MAY), title
				+ "-MAY", title + " (MAY)", description
				+ " MAY conformance tests.");
		writeManifest(conformanceClasses.get(LdpTestCaseReporter.OTHER), title
				+ "-OTHER", title + " (OTHER)", description
				+ " No official conformance status or test case is extension or incomplete.");
	}

	@SuppressWarnings("incomplete-switch")
	private Resource generateInformation(Method method, String className,
			ArrayList<ArrayList<Resource>> conformanceClasses) {
		SpecTest testLdp = null;
		Test test = null;
		if (method.getAnnotation(SpecTest.class) != null
				&& method.getAnnotation(Test.class) != null) {
			testLdp = method.getAnnotation(SpecTest.class);
			test = method.getAnnotation(Test.class);
			if(!testLdp.testMethod().equals(METHOD.INDIRECT)){

				Resource testCaseResource = createResource(className, method, test, testLdp, conformanceClasses);
				testCaseResource.addProperty(RDF.type, EARL.TestCase);
				switch (testLdp.testMethod()) {
				case AUTOMATED:
					testCaseResource.addProperty(testMethod, automated);
					break;
				case MANUAL:
					testCaseResource.addProperty(testMethod, manual);
					break;
				case NOT_IMPLEMENTED:
					testCaseResource.addProperty(testMethod, notImplemented);
					break;
				case CLIENT_ONLY:
					testCaseResource.addProperty(testMethod, clientOnly);
					break;
				}
				return testCaseResource;
			} else { // for Indirect Tests
				Resource indirectResource = createResource(className, method, test, testLdp, conformanceClasses);
				indirectResource.addProperty(RDF.type, EARL.TestRequirement);
				indirectResource.addProperty(testMethod, indirect);
				if(testLdp.coveredByTests().length > 0 && testLdp.coveredByGroups().length > 0) {
					for(Class<?> coverTest : testLdp.coveredByTests()) {
						Method[] classMethod = coverTest.getDeclaredMethods();
						for(Method m : classMethod) {
							if(m.getAnnotation(Test.class) != null) {
								String group = Arrays.toString(m.getAnnotation(Test.class).groups());
								for(String groupCover : testLdp.coveredByGroups()) {
									if(group.contains(groupCover)) {
										String testCaseName = createTestCaseName(m.getDeclaringClass().getCanonicalName(), m.getName());
										String testCaseURL = namespace + testCaseName;
										indirectResource.addProperty(DCTerms.hasPart, testCaseURL);

									}
								}
							}
						}
					}
				}
				return indirectResource;
			}
		}
		return null;
	}

	private Resource createResource(String className, Method method, Test test, SpecTest testLdp,
			ArrayList<ArrayList<Resource>> conformanceClasses) {
		String testCaseName = createTestCaseName(className, method.getName());

		// Client only tests should be managed in separate EARL manifest
		if (testLdp.testMethod() == METHOD.CLIENT_ONLY) {
			System.err.println("Wrongly received CLIENT_ONLY test for "+testCaseName+
					". Client-only tests should be defined in separate RDF manifest file.");
		}

		String allGroups = groups(test.groups());

		Calendar cal = GregorianCalendar.getInstance();
		Literal date = model.createTypedLiteral(cal);


		String testCaseDeclaringName = createTestCaseName(method.getDeclaringClass().getCanonicalName(), method.getName());
		String testCaseURL = namespace + testCaseName;
		String testCaseDeclaringURL = namespace + testCaseDeclaringName;

		Resource resource = model.createResource(testCaseURL);
		resource.addProperty(RDFS.label, testCaseName);
		resource.addProperty(TestManifest.name, testCaseName);
		resource.addProperty(DCTerms.date, date);

		resource.addProperty(RDFS.comment, test.description());
		if (allGroups != null)
			resource.addProperty(DCTerms.subject, allGroups);

		boolean added = false;
		if (testLdp.approval() != SpecTest.STATUS.WG_EXTENSION) {
			for (String group: test.groups()) {
				group = group.trim();
				if (conformanceLevels.contains(group))  {
					resource.addProperty(conformanceLevel, model.createResource(namespace + group));
					conformanceClasses.get(LdpTestCaseReporter.getConformanceIndex(group)).add(resource);
					added = true;
				}
			}
		}
		// If not in a conformance group, add to general other bucket
		if (!added) {
			conformanceClasses.get(LdpTestCaseReporter.OTHER).add(resource);
		}

		String[] stepsArr = testLdp.steps();
		if (stepsArr != null && stepsArr.length > 0) {
			ArrayList<Literal> arr = new ArrayList<Literal>();
			for (String s: stepsArr) {
				arr.add(model.createLiteral(s));
			}
			RDFList l = model.createList(arr.iterator());
			resource.addProperty(steps, l);
		}

		// 	Leave action property only to make earl-report happy
		resource.addProperty(TestManifest.action, "");

		switch (testLdp.approval()) {
		case WG_APPROVED:
			resource.addProperty(TestDescription.reviewStatus, TestDescription.approved);
			break;
		case WG_PENDING:
			resource.addProperty(TestDescription.reviewStatus, TestDescription.unreviewed);
			break;
		default:
			resource.addProperty(TestDescription.reviewStatus, TestDescription.unreviewed);
			break;
		}

		resource.addProperty(declaredInClass, className);
		resource.addProperty(declaredTestCase, model.createResource(testCaseDeclaringURL));
		Resource specRef = null;
		if (testLdp.specRefUri() != null) {
			specRef = model.createResource(testLdp.specRefUri());
			resource.addProperty(RDFS.seeAlso, specRef);
		}

		if (test.description() != null && test.description().length() > 0) {
			Resource excerpt = model.createResource(TestDescription.Excerpt);
			excerpt.addLiteral(TestDescription.includesText, test.description());
			if (specRef != null) {
				excerpt.addProperty(RDFS.seeAlso, specRef);
			}
			resource.addProperty(TestDescription.specificationReference, excerpt);
		}

		resource.addProperty(documentation,
				model.createResource(ReportUtils.getJavadocLink(method)));

		return resource;

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

	private void writeTestClasses(Map<Class<?>, String> classes) {
		// These are put in order so they are presented properly on earl-report
		//		writeInfo(commonResourceTest, testcases);
		//		writeInfo(rdfSourceTest, testcases);
		//		writeInfo(commonContainerTest, testcases);
		Iterator<Class<?>> classNames = classes.keySet().iterator();
		String info = "";
		while(classNames.hasNext()){
			Class<?> classVal = classNames.next();
			info = classes.get(classVal);
			String[] split = info.split(":");
			writeInfo(classVal, split[0], split[1]);
		}
	}

	@Override
    protected String getFilename() {
	    return "ldp-earl-manifest";
    }
}
