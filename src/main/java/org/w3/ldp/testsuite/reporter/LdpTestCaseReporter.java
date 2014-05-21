package org.w3.ldp.testsuite.reporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.StringResource;

import static org.rendersnake.HtmlAttributesFactory.*;

import org.testng.annotations.Test;
import org.w3.ldp.testsuite.test.BasicContainerTest;
import org.w3.ldp.testsuite.test.CommonContainerTest;
import org.w3.ldp.testsuite.test.CommonResourceTest;
import org.w3.ldp.testsuite.test.DirectContainerTest;
import org.w3.ldp.testsuite.test.IndirectContainerTest;
import org.w3.ldp.testsuite.test.NonRDFSourceTest;
import org.w3.ldp.testsuite.test.RdfSourceTest;
import org.w3.ldp.testsuite.annotations.Implementation;
import org.w3.ldp.testsuite.annotations.Reference;
import org.w3.ldp.testsuite.annotations.Status;

public class LdpTestCaseReporter {

	private static HtmlCanvas html;

	private static boolean initialRead;

	private static Set<String> refURI = new HashSet<String>();

	private static ArrayList<String> clients = new ArrayList<String>();
	private static ArrayList<String> manuals = new ArrayList<String>();

	private static int totalTests = 0;
	private static int totalImplemented = 0;
	private static int unimplemented = 0;
	private static int coverage = 0;
	private static int clientTest = 0;

	private static int manual = 0;
	private static int disabled = 0;

	private static int must = 0;
	private static int should = 0;
	private static int may = 0;

	private static int reqImpl = 0;

	private static int mustImpl = 0;
	private static int shouldImpl = 0;
	private static int mayImpl = 0;

	private static int refNotImpl = 0;
	private static int mustNotImpl = 0;
	private static int mayNotImpl = 0;
	private static int shouldNotImpl = 0;

	private static int pending = 0;
	private static int approved = 0;

	private static Class<BasicContainerTest> bcTest = BasicContainerTest.class;
	private static Class<RdfSourceTest> rdfSourceTest = RdfSourceTest.class;
	private static Class<IndirectContainerTest> indirectContainerTest = IndirectContainerTest.class;
	private static Class<DirectContainerTest> directContienerTest = DirectContainerTest.class;
	private static Class<CommonContainerTest> commonContainerTest = CommonContainerTest.class;
	private static Class<CommonResourceTest> commonResourceTest = CommonResourceTest.class;
	private static Class<NonRDFSourceTest> nonRdfSourceTest = NonRDFSourceTest.class;

	public static void main(String[] args) throws IOException {
		initialRead = false;
		makeReport();

		createWriter("report", html.toHtml());
	}

	private static void makeReport() throws IOException {
		html = new HtmlCanvas();
		html.html().head();
		writeCss();
		html._head().body().title().content("Test Cases Report");

		html.h1().content("LDP Test Suite: Test Cases Report");

		createSummaryReport();
		toTop();

		acquireTestCases(rdfSourceTest);
		acquireTestCases(bcTest);
		acquireTestCases(commonContainerTest);
		acquireTestCases(commonResourceTest);
		acquireTestCases(nonRdfSourceTest);
		acquireTestCases(indirectContainerTest);
		acquireTestCases(directContienerTest);
	}

	private static void createSummaryReport() throws IOException {

		firstRead();

		initialRead = true;
		html.h2().content("Summary of Test Methods");
		html.table(class_("summary"));
		html.tr().th().content("Total Tests");
		html.th().content("Overall Coverage");
		html.th().content("Unimplemented Methods");
		html._tr();

		html.tr();
		html.td().b().write("" + totalTests)._b()
				.write(" Total Tests Running Against Specifications");
		html.br().b().write(pending + " ")._b().write("Tests pending, ").b()
				.write(approved + " ")._b().write("Tests approved");
		html.ul();
		html.li().b().write("" + coverage)._b().write(" Requirements Covered")
				._li();
		html.ul().li().b().write("" + must)._b().write(" MUST")._li();
		html.li().b().write("" + should)._b().write(" SHOULD")._li();
		html.li().b().write("" + may)._b().write(" MAY")._li()._ul();
		html._ul();

		html._td();

		html.td();

		html.b().write(totalImplemented + "/" + totalTests)._b()
				.write(" of Total Tests Implemented");

		html.ul();
		html.li().b().write(reqImpl + " ")._b()
				.write("Requirements Implemented")._li();
		html.ul();
		html.li().b().write(mustImpl + "/" + must)._b()
				.write(" of MUST Tests Implemented")._li();
		html.li().b().write(shouldImpl + "/" + should)._b()
				.write(" of SHOULD Tests Implemented")._li();
		html.li().b().write(mayImpl + "/" + may)._b()
				.write(" of MAY Tests Implemented")._li();
		html._ul();
		html._ul();

		html._td();

		// html.td().content(totalImplemented + " Tests");
		html.td();
		html.b().write(unimplemented + " ")._b().write("of the Total Tests");
		html.ul();

		html.li().b().write(disabled + " ")._b()
				.write("of the Total Tests not enabled")._li();
		html.li().b().write(clientTest + " ")._b().write("of the Total are ")
				.a(href("#clientTests")).write("Client-Based Tests")._a()._li();
		html.li().b().write(manual + " ")._b()
				.write("of the Total must be Tested ").a(href("#manualTests"))
				.write("Manually")._a()._li();
		html._ul();

		html.write("From the Total, ");

		html.ul();
		html.li().b().write(refNotImpl + " ")._b()
				.write("Requirements not Implemented")._li();
		html.ul();
		html.li().b().write(mustNotImpl + " ")._b().write("MUST")._li();
		html.li().b().write(shouldNotImpl + " ")._b().write("SHOULD")._li();
		html.li().b().write(mayNotImpl + " ")._b().write("MAY")._li();
		html._ul();
		html._ul()._td();
		html._tr();
		html._table();

		generateListOfTestCases();

	}

	private static void firstRead() throws IOException {
		acquireTestCases(rdfSourceTest);
		acquireTestCases(bcTest);
		acquireTestCases(commonContainerTest);
		acquireTestCases(commonResourceTest);
		acquireTestCases(nonRdfSourceTest);
		acquireTestCases(indirectContainerTest);
		acquireTestCases(directContienerTest);
	}

	private static void generateListOfTestCases() throws IOException {
		html.h2().content("Implemented Test Classes");
		html.ul();
		html.li().a(href("#" + rdfSourceTest.getCanonicalName()))
				.content(rdfSourceTest.getCanonicalName())._li();
		html.li().a(href("#" + bcTest.getCanonicalName()))
				.content(bcTest.getCanonicalName())._li();
		html.li().a(href("#" + commonContainerTest.getCanonicalName()))
				.content(commonContainerTest.getCanonicalName())._li();
		html.li().a(href("#" + commonResourceTest.getCanonicalName()))
				.content(commonResourceTest.getCanonicalName())._li();
		html.li().a(href("#" + indirectContainerTest.getCanonicalName()))
				.content(indirectContainerTest.getCanonicalName())._li();
		html.li().a(href("#" + directContienerTest.getCanonicalName()))
				.content(directContienerTest.getCanonicalName())._li();
		html.li().a(href("#" + nonRdfSourceTest.getCanonicalName()))
				.content(nonRdfSourceTest.getCanonicalName())._li();
		html._ul();

		html.h2().a(name("manualTests"))
				.content("Tests that Must be Tested Manually")._h2();
		generateList(manuals);
		html.h2().a(name("clientTests")).content("Client-Based Test Cases")
				._h2();
		generateList(clients);

	}

	private static void generateList(ArrayList<String> list) throws IOException {
		html.ul();
		for (int i = 0; i < list.size(); i++) {
			html.li().a(href("#" + list.get(i))).write(list.get(i))._a()._li();
		}
		html._ul();
	}

	private static <T> void acquireTestCases(Class<T> classType)
			throws IOException {
		String name = classType.getCanonicalName();
		if (initialRead)
			html.h2().a(name(name)).write("Test Class: " + name)._a()._h2();
		Method[] bcMethods = classType.getDeclaredMethods();

		if (initialRead)
			html.ul();
		for (Method method : bcMethods) {
			if (method.isAnnotationPresent(Test.class)) {
				if (!initialRead) {
					totalTests++;
					generateInformation(method, name);
				} else {
					html.li().b().a(name(method.getName()))
							.write(method.getName() + ": ")._a()._b();
					generateInformation(method, name);
					html._li();
				}
			}
		}
		if (initialRead) {
			html._ul();
			toTop();
		}
	}

	private static void generateInformation(Method method, String name)
			throws IOException {

		Reference ref = null;
		Test test = null;
		Status status = null;
		Implementation implmnt = null;

		if (initialRead) {
			html.table(class_("annotation"));
			html.tr().th().content("Annotation Type");
			html.th().content("Information")._tr();
		}

		if (method.getAnnotation(Test.class) != null) {
			test = method.getAnnotation(Test.class);

			if (!initialRead) {
				if (!test.enabled())
					disabled++;
			} else {
				html.tr().td()
						.content(test.annotationType().getCanonicalName());
				html.td();
				html.b().write("Description: ")._b().write(test.description());
				html.br().b().write("Groups: ")._b()
						.write(Arrays.toString(test.groups()));
				html.br().b().write("Enabled: ")._b()
						.write("" + test.enabled())._td();
				html._tr();
			}

		}

		if (method.getAnnotation(Implementation.class) != null) {
			implmnt = method.getAnnotation(Implementation.class);
			if (!initialRead) {
				if (implmnt.implementation().equals(Implementation.IMPLEMENTED)
						&& !implmnt.clientOnly() && implmnt.isTestable()
						&& (test != null && test.enabled())) {
					totalImplemented++;
				}
				if (implmnt.implementation().equals(
						Implementation.NOT_IMPLEMENTED)
						|| implmnt.clientOnly()
						|| !implmnt.isTestable()
						|| (test != null && !test.enabled())) {
					unimplemented++;
				}
				if (implmnt.clientOnly()) {
					clientTest++;
					clients.add(method.getName());
				}
				if (implmnt.manual()) {
					manual++;
					manuals.add(method.getName());
				}
			} else {
				html.tr().td()
						.content(implmnt.annotationType().getCanonicalName());
				html.br().td().b().write("Implementation: ")._b()
						.write(implmnt.implementation());
				html.br().b().write("Manual Test: ")._b()
						.write("" + implmnt.manual());
				html.br().b().write("Client Only: ")._b()
						.write("" + implmnt.clientOnly());
				html._td();
				html._tr();
			}
		}

		if (method.getAnnotation(Reference.class) != null) {
			ref = method.getAnnotation(Reference.class);
			if (!initialRead) {
				if (implmnt != null && test != null) {
					if (!refURI.contains(ref.uri())) {
						coverage++;
						String group = Arrays.toString(test.groups());
						if (group.contains("MUST"))
							must++;
						if (group.contains("SHOULD"))
							should++;
						if (group.contains("MAY"))
							may++;

						refURI.add(ref.uri());
						if (implmnt.implementation().equals(
								Implementation.IMPLEMENTED)) {
							reqImpl++;
							if (group.contains("MUST"))
								mustImpl++;
							if (group.contains("SHOULD"))
								shouldImpl++;
							if (group.contains("MAY"))
								mayImpl++;

						}
						if (implmnt.implementation().equals(
								Implementation.NOT_IMPLEMENTED)) {
							refNotImpl++;

							if (group.contains("MUST"))
								mustNotImpl++;
							if (group.contains("SHOULD"))
								shouldNotImpl++;
							if (group.contains("MAY"))
								mayNotImpl++;
						}

					}
				}
			} else {
				html.tr().td().content(ref.annotationType().getCanonicalName());
				html.td().b().write("Reference URI: ")._b().a(href(ref.uri()))
						.write(ref.uri())._a()._td();
				html._tr();
			}
		}

		if (method.getAnnotation(Status.class) != null) {
			status = method.getAnnotation(Status.class);
			if (!initialRead) {
				if (status.status().equals(Status.PENDING))
					pending++;
				if (status.status().equals(Status.APPROVED))
					approved++;
			} else {
				html.tr().td()
						.content(status.annotationType().getCanonicalName());
				html.td().b().write("Status: ")._b().write(status.status())
						._td();
				html._tr();
			}
		}

		if (initialRead) {
			html._table();
			toTestClass(name);
		}
	}

	private static void toTop() throws IOException {
		html.p(class_("totop")).a(href("#top")).content("Back to Top")._p();
	}

	private static void toTestClass(String name) throws IOException {
		html.p(class_("totest")).a(href("#" + name))
				.content("Back to Main Test Class")._p();
	}

	private static void writeCss() throws IOException {

		html.style().write(StringResource.get("testCaseStyle.css"), NO_ESCAPE)
				._style();
	}

	private static void createWriter(String directory, String output) {
		BufferedWriter writer = null;
		new File(directory).mkdirs();
		try {
			writer = new BufferedWriter(new FileWriter(directory
					+ "/LdpTestCasesHtmlReport.html"));
			writer.write(output);

		} catch (IOException e) {
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
			}
		}
	}

}
