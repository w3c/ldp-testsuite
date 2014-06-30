package org.w3.ldp.testsuite.reporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.testng.annotations.Test;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.test.BasicContainerTest;
import org.w3.ldp.testsuite.test.CommonContainerTest;
import org.w3.ldp.testsuite.test.CommonResourceTest;
import org.w3.ldp.testsuite.test.DirectContainerTest;
import org.w3.ldp.testsuite.test.IndirectContainerTest;
import org.w3.ldp.testsuite.test.NonRDFSourceTest;
import org.w3.ldp.testsuite.test.RdfSourceTest;
import org.w3.ldp.testsuite.vocab.RdfLdp;

import com.github.jsonldjava.jena.JenaJSONLD;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.vocabulary.EARL;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.TestManifest;

public class LdpEarlTestManifest {

	private static BufferedWriter writer;
	private static Model model;

	private static Property declared;


	private static Class<BasicContainerTest> bcTest = BasicContainerTest.class;
	private static Class<RdfSourceTest> rdfSourceTest = RdfSourceTest.class;
	private static Class<IndirectContainerTest> indirectContainerTest = IndirectContainerTest.class;
	private static Class<DirectContainerTest> directContianerTest = DirectContainerTest.class;
	private static Class<CommonContainerTest> commonContainerTest = CommonContainerTest.class;
	private static Class<CommonResourceTest> commonResourceTest = CommonResourceTest.class;
	private static Class<NonRDFSourceTest> nonRdfSourceTest = NonRDFSourceTest.class;

	private static final String LDPT_PREFIX = "ldpt";
	private static final String LDPT_NAME = "http://w3c.github.io/ldp-testsuite#";
	private static final String TURTLE = "TURTLE";

	private static final String outputDir = "report"; // directory where results

	static {
		JenaJSONLD.init();
	}

	public static void main(String[] args) throws IOException {
		try {
			createWriter(outputDir);
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}

		model = ModelFactory.createDefaultModel();
		declared = ResourceFactory
				.createProperty(LDPT_NAME + "declaredInClass");

		writePrefixes();
		ResIterator tests = writeTestClasses();
		writeManifest(tests);

		write();
		try {
			endWriter();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	private static void writeManifest(ResIterator tests) {
		Resource manifest = model.createResource(LDPT_NAME,
				TestManifest.Manifest);
		manifest.addProperty(RDFS.comment, "LDP tests");
		manifest.addProperty(TestManifest.entries, model.createList(tests));
	}

	private static void writePrefixes() {
		model.setNsPrefix("doap", "http://usefulinc.com/ns/doap#");
		model.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
		model.setNsPrefix("earl", "http://www.w3.org/ns/earl#");
		model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		model.setNsPrefix("mf",
				"http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#");
		model.setNsPrefix("rdft", "http://www.w3.org/ns/rdftest#");
		model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
		model.setNsPrefix(LDPT_PREFIX, LDPT_NAME);
	}

	private static <T> void writeInfo(Class<T> testClass) {
		String name = testClass.getCanonicalName();
		Method[] bcMethods = testClass.getDeclaredMethods();
		for (Method method : bcMethods) {
			if (method.isAnnotationPresent(Test.class)) {
				generateInformation(method, name);
			}
		}
	}

	private static void generateInformation(Method method, String name) {
		SpecTest testLdp = null;
		Test test = null;
		if (method.getAnnotation(SpecTest.class) != null
				&& method.getAnnotation(Test.class) != null) {
			testLdp = method.getAnnotation(SpecTest.class);
			test = method.getAnnotation(Test.class);
			String group = groups(test.groups());

			Calendar cal = GregorianCalendar.getInstance();
			Literal date = model.createTypedLiteral(cal);

			String testCaseName = createTestCaseName(name, method.getName());
			String testCaseURL = LDPT_NAME + testCaseName;

			Resource testCaseResource = model.createResource(testCaseURL);
			testCaseResource.addProperty(RDF.type, EARL.TestCase);
			testCaseResource.addProperty(TestManifest.name,testCaseName);
			testCaseResource.addProperty(DCTerms.date, date);

			testCaseResource.addProperty(RDFS.comment, test.description());
			if (group != null)
				testCaseResource.addProperty(DCTerms.subject, group);

			// Leave action property only to make earl-report happy
			testCaseResource.addProperty(TestManifest.action, "");

			switch (testLdp.approval()) {
			case WG_APPROVED:
				testCaseResource.addProperty(RdfLdp.Approval, RdfLdp.approved);
				break;
			case WG_PENDING:
				testCaseResource.addProperty(RdfLdp.Approval, RdfLdp.propopsed);
				break;
			default:
				testCaseResource.addProperty(RdfLdp.Approval, RdfLdp.propopsed);
				break;
			}

			testCaseResource.addProperty(declared, name);
		}
	}

	public static String createTestCaseURL(String className, String methodName) {
		return LDPT_NAME + createTestCaseName(className, methodName);
	}
	
	public static String createTestCaseName(String className, String methodName) {

		className = className.substring(className.lastIndexOf(".") + 1);

		if (className.endsWith("Test")) {
			className = className.substring(0, className.length()-4);
		}
		if (methodName.startsWith("test")) {
			methodName = methodName.substring(4, methodName.length());
		}
		return className + "-" + methodName;
	}

	private static String groups(String[] list) {
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

	private static ResIterator writeTestClasses() {
		writeInfo(rdfSourceTest);
		writeInfo(bcTest);
		writeInfo(commonContainerTest);
		writeInfo(commonResourceTest);
		writeInfo(nonRdfSourceTest);
		writeInfo(indirectContainerTest);
		writeInfo(directContianerTest);
		
		return 	model.listSubjectsWithProperty(RDF.type, EARL.TestCase);
	}

	private static void write() {
		model.write(writer, TURTLE);
	}

	private static void createWriter(String directory) throws IOException {
		writer = null;
		new File(directory).mkdirs();
		writer = new BufferedWriter(new FileWriter(directory
				+ "/ldp-earl-manifest.ttl"));
	}

	private static void endWriter() throws IOException {
		writer.flush();
		writer.close();
	}
}
