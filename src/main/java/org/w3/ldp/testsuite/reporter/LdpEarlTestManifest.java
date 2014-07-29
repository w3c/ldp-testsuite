package org.w3.ldp.testsuite.reporter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.vocab.LDP;
import org.w3.ldp.testsuite.vocab.TestDescription;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.vocabulary.EARL;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.TestManifest;

public class LdpEarlTestManifest extends AbstractEarlReporter {

	private static final Property declaredInClass = ResourceFactory
			.createProperty(LDP.LDPT_NAMESPACE + "declaredInClass");
	private static final Property declaredTestCase = ResourceFactory
			.createProperty(LDP.LDPT_NAMESPACE + "declaredTestCase");
	private static final Property conformanceLevel = ResourceFactory
			.createProperty(LDP.LDPT_NAMESPACE + "conformanceLevel");

	/**
	 * A link to the published Javadoc for the test case (which in turn links to
	 * the source code).
	 */
	private static final Property documentation = ResourceFactory
			.createProperty(LDP.LDPT_NAMESPACE + "documentation");

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
			.createProperty(LDP.LDPT_NAMESPACE + "testMethod");

	/**
	 * @see SpecTest.METHOD#AUTOMATED
	 */
	private static final Resource automated = ResourceFactory
			.createResource(LDP.LDPT_NAMESPACE + "automated");

	/**
	 * @see SpecTest.METHOD#MANUAL
	 */
	private static final Resource manual = ResourceFactory
			.createResource(LDP.LDPT_NAMESPACE + "manual");

	/**
	 * @see SpecTest.METHOD#CLIENT_ONLY
	 */
	private static final Resource clientOnly = ResourceFactory
			.createResource(LDP.LDPT_NAMESPACE + "clientOnly");

	/**
	 * @see SpecTest.METHOD#NOT_IMPLEMENTED
	 */
	private static final Resource notImplemented = ResourceFactory
			.createResource(LDP.LDPT_NAMESPACE + "notImplemented");
	
	/**
	 * @see SpecTest.steps
	 */
	private static final Property steps = ResourceFactory
			.createProperty(LDP.LDPT_NAMESPACE + "steps");
	
	/**
	 * List of GROUPS to include in reporting
	 */
	private static List<String> conformanceLevels = new ArrayList<String>();
	
	public void setConformanceLevels(List<String> list){
		conformanceLevels = list;
	}

	public void generate(Map<Class<?>, String> classes, String title) {
		try {
			createWriter(OUTPUT_DIR, title);
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

	private void writeManifest(ArrayList<Resource> testcases, String localName, String label, String description) {
		if (testcases.size() == 0) return;

		Resource manifest = model.createResource(LDP.LDPT_NAMESPACE + localName+"Manifest",
				TestManifest.Manifest);
		manifest.addProperty(DC.title, label);
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

	private Resource generateInformation(Method method, String className,
			ArrayList<ArrayList<Resource>> conformanceClasses) {
		SpecTest testLdp = null;
		Test test = null;
		if (method.getAnnotation(SpecTest.class) != null
				&& method.getAnnotation(Test.class) != null) {
			testLdp = method.getAnnotation(SpecTest.class);
			String testCaseName = createTestCaseName(className, method.getName());
			
			// Client only tests should be managed in separate EARL manifest
			if (testLdp.testMethod() == METHOD.CLIENT_ONLY) {
				System.err.println("Wrongly received CLIENT_ONLY test for "+testCaseName+
						". Client-only tests should be defined in separate RDF manifest file.");
			}

			test = method.getAnnotation(Test.class);
			String allGroups = groups(test.groups());

			Calendar cal = GregorianCalendar.getInstance();
			Literal date = model.createTypedLiteral(cal);


			String testCaseDeclaringName = createTestCaseName(method.getDeclaringClass().getCanonicalName(), method.getName());
			String testCaseURL = LDP.LDPT_NAMESPACE + testCaseName;
			String testCaseDeclaringURL = LDP.LDPT_NAMESPACE + testCaseDeclaringName;

			Resource testCaseResource = model.createResource(testCaseURL);
			testCaseResource.addProperty(RDF.type, EARL.TestCase);
			testCaseResource.addProperty(RDFS.label, testCaseName);
			testCaseResource.addProperty(TestManifest.name, testCaseName);
			testCaseResource.addProperty(DCTerms.date, date);

			testCaseResource.addProperty(RDFS.comment, test.description());
			if (allGroups != null)
				testCaseResource.addProperty(DCTerms.subject, allGroups);
			
			boolean added = false;		
			if (testLdp.approval() != SpecTest.STATUS.WG_EXTENSION) {
				for (String group: test.groups()) {
					group = group.trim();
					if (conformanceLevels.contains(group))  {
						testCaseResource.addProperty(conformanceLevel, model.createResource(LDP.LDPT_NAMESPACE + group));
						conformanceClasses.get(LdpTestCaseReporter.getConformanceIndex(group)).add(testCaseResource);
						added = true;
					}
				}
			}
			// If not in a conformance group, add to general other bucket
			if (!added) {
				conformanceClasses.get(LdpTestCaseReporter.OTHER).add(testCaseResource);
			}
			 
			String[] stepsArr = testLdp.steps();
			if (stepsArr != null && stepsArr.length > 0) {
				ArrayList<Literal> arr = new ArrayList<Literal>();
				for (String s: stepsArr) {
					arr.add(model.createLiteral(s));
				}
				RDFList l = model.createList(arr.iterator());
				testCaseResource.addProperty(steps, l);
			}

			// Leave action property only to make earl-report happy
			testCaseResource.addProperty(TestManifest.action, "");

			switch (testLdp.approval()) {
			case WG_APPROVED:
				testCaseResource.addProperty(TestDescription.reviewStatus, TestDescription.approved);
				break;
			case WG_PENDING:
				testCaseResource.addProperty(TestDescription.reviewStatus, TestDescription.unreviewed);
				break;
			default:
				testCaseResource.addProperty(TestDescription.reviewStatus, TestDescription.unreviewed);
				break;
			}

			testCaseResource.addProperty(declaredInClass, className);
			testCaseResource.addProperty(declaredTestCase, model.createResource(testCaseDeclaringURL));
			Resource specRef = null;
			if (testLdp.specRefUri() != null) {
				specRef = model.createResource(testLdp.specRefUri());
				testCaseResource.addProperty(RDFS.seeAlso, specRef);
			}

			if (test.description() != null && test.description().length() > 0) {
				Resource excerpt = model.createResource(TestDescription.Excerpt);
				excerpt.addLiteral(TestDescription.includesText, test.description());
				if (specRef != null) {
					excerpt.addProperty(RDFS.seeAlso, specRef);
				}
				testCaseResource.addProperty(TestDescription.specificationReference, excerpt);
			}

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

			testCaseResource.addProperty(documentation,
					model.createResource(ReportUtils.getJavadocLink(method)));

			return testCaseResource;
		}
		return null;
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
