package org.w3.ldp.testsuite.reporter;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.StringResource;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.test.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.rendersnake.HtmlAttributesFactory.*;

public class LdpTestCaseReporter {

	private static HtmlCanvas html;

	private static boolean initialRead;

	private static StringWriter graphs = new StringWriter();

	private static Set<String> refURI = new HashSet<String>();

	private static ArrayList<String> clients = new ArrayList<String>();
	private static ArrayList<String> manuals = new ArrayList<String>();
	private static ArrayList<String> readyToBeApproved = new ArrayList<String>();
	private static ArrayList<String> needCode = new ArrayList<String>();

	private static int totalTests = 0;
	private static int automated = 0;
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

	private static int reqNotImpl = 0;
	private static int mustNotImpl = 0;
	private static int mayNotImpl = 0;
	private static int shouldNotImpl = 0;

	private static int pending = 0;
	private static int approved = 0;
	private static int extended = 0;
	private static int deprecated = 0;
	private static int clarification = 0;
	
	private static Class<BasicContainerTest> bcTest = BasicContainerTest.class;
	private static Class<RdfSourceTest> rdfSourceTest = RdfSourceTest.class;
	private static Class<IndirectContainerTest> indirectContainerTest = IndirectContainerTest.class;
	private static Class<DirectContainerTest> directContianerTest = DirectContainerTest.class;
	private static Class<CommonContainerTest> commonContainerTest = CommonContainerTest.class;
	private static Class<CommonResourceTest> commonResourceTest = CommonResourceTest.class;
	private static Class<NonRDFSourceTest> nonRdfSourceTest = NonRDFSourceTest.class;

	private static int mustpend = 0;
	private static int shouldpend = 0;
	private static int maypend = 0;

	private static int mustapp = 0;
	private static int shouldapp = 0;
	private static int mayapp = 0;

	private static int mustex = 0;
	private static int shouldex = 0;
	private static int mayex = 0;

	private static int mustdep = 0;
	private static int shoulddep = 0;
	private static int maydep = 0;

	private static int notYetMust = 0;
	private static int notYetShould = 0;
	private static int notYetMay = 0;

	public static void main(String[] args) throws IOException {
		initialRead = false;
		makeReport();
		endReport();
		createWriter("report", html.toHtml());
	}

	private static void makeReport() throws IOException {
		html = new HtmlCanvas();
		html.html().head();
		writeCss();
		html.title().content("LDP: Test Cases Coverage Report")._head().body();

		html.h1().content("W3C Linked Data Platform (LDP) Test Suite: Test Cases Coverate Report");
		html.p().a(href("http://www.w3.org/2012/ldp/")).write("See also W3C Linked Data Platform WG")._a()._p();

		createSummaryReport();
		toTop();

		acquireTestCases(rdfSourceTest);
		acquireTestCases(bcTest);
		acquireTestCases(commonContainerTest);
		acquireTestCases(commonResourceTest);
		acquireTestCases(nonRdfSourceTest);
		acquireTestCases(indirectContainerTest);
		acquireTestCases(directContianerTest);
	}

	private static void endReport() throws IOException {
		html.script().content(StringResource.get("/raphael/raphael-min.js"),
				NO_ESCAPE);
		html.script().content(StringResource.get("/prototype/prototype.js"),
				NO_ESCAPE);
		html.script().content(StringResource.get("/grafico/grafico-min.js"),
				NO_ESCAPE);
		html.write(graphs.toString(), NO_ESCAPE);

		html._body()._html();
	}

	private static void createSummaryReport() throws IOException {

		firstRead();

		generateBarGraph();

		initialRead = true;
		html.h2().content("Summary of Test Methods");
		html.table(class_("summary"));
		html.tr().th().content("Totals");
		html.th().content("Coverage");
		html.th().content("Not Implemented");
		html._tr();

		html.tr();
		html.td().b().write("" + totalTests)._b().write(" Total Tests");
		html.ul().li().b().write(approved + " ")._b().write("WG Approved")
				._li();
		html.li().b().write(pending + " ")._b().write("Incomplete")._li();
		html.li().b().write(extended + " ")._b().write("Extension")._li();
		html.li().b().write(deprecated + " ")._b().write("No longer valid")
				._li();
		html.li().b().write(clarification + " ")._b().write("Needs to be clarified with WG")
		._li();
		html._ul();
		
		html.br().b().a(href("#tobeapproved")).write(notYetMust+notYetShould+notYetMay+
				" Ready for WG Approval")._a()._b();

		html._td();

		html.td();

		html.b().write(automated + " / " + totalTests)._b()
				.write(" of Total Tests Automated").br();
		html.b().write(automated + " / " + (automated + unimplemented))._b()
				.write(" of Tests Possible to Automate");
		html.ul();
		html.li().b().write("" + coverage)._b().write(" Requirements Covered")
				._li();
		html.ul().li().b().write("" + must)._b().write(" MUST")._li();
		html.li().b().write("" + should)._b().write(" SHOULD")._li();
		html.li().b().write("" + may)._b().write(" MAY")._li()._ul();
		html._ul();

		html.ul();
		html.li().b().write(reqImpl + " ")._b().write("Requirements Automated")
				._li();
		html.ul();
		html.li().b().write(mustImpl + " / " + must)._b().write(" MUST")._li();
		html.li().b().write(shouldImpl + " / " + should)._b().write(" SHOULD")
				._li();
		html.li().b().write(mayImpl + " / " + may)._b().write(" MAY")._li();
		html._ul();
		html._ul();

		html._td();

		// html.td().content(totalImplemented + " Tests");
		html.td();
		html.b().write((unimplemented + clientTest + manual) + " ")._b()
				.write("of the Total Tests");
		html.ul();

		html.li().b().write(unimplemented + " ")._b()
				.write("of the Tests ").a(href("#needCode")).write("Yet to be Coded")._a()._li();
		/*
		 * TODO: Determine if disabled is really valuable or not
		 * html.li().b().write(disabled +
		 * " ")._b().write("of the Tests not enabled")._li();
		 */
		html.li().b().write(clientTest + " ")._b().write("of the Total are ")
				.a(href("#clientTests")).write("Client-Based Tests")._a()._li();
		html.li().b().write(manual + " ")._b().write("of the Total must be ")
				.a(href("#manualTests")).write("Tested Manually")._a()._li();
		html._ul();

		html.write("From the Total, ");

		html.ul();
		html.li().b().write(reqNotImpl + " ")._b()
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

	private static void generateBarGraph() throws IOException {

		html.h2().content("Overall Specification Requirements Coverage");
		html.div(id("coverage_bar").class_("barChart"))._div();
		graphs.write("<script>");
		graphs.write("Event.observe(window, 'load', function() {");
		graphs.write("var coverage_bar = new Grafico.StackedBarGraph($('coverage_bar'),");
		graphs.write("{ approved: [" + mustapp + "," + shouldapp + ", "
				+ mayapp + "],");
		graphs.write("pending: [" + (mustpend - notYetMust) + ", "
				+ (shouldpend - notYetShould) + ", " + (maypend - notYetMay)
				+ "],");
		graphs.write("autoPend: [" + notYetMust + ", " + notYetShould + ", "
				+ notYetMay + " ],");
		graphs.write("extended: [" + mustex + ", " + shouldex + ", " + mayex
				+ "],");
		graphs.write("deprecated: [" + mustdep + ", " + shoulddep + ", "
				+ maydep + "] },");
		graphs.write("{ labels: [ \"MUST\", \"SHOULD\", \"MAY\" ],");
		graphs.write("colors: { approved: '#2f59bf', pending: '#a2bf2f', autoPend:'#1cbf5c', extended: '#bfa22f', deprecated: '#bf5a2f' },");
		graphs.write("hover_color: \"#ccccff\",");
		graphs.write("datalabels: { approved: [ \"" + mustapp
				+ " Approved\", \"" + shouldapp + " Approved\", \"" + mayapp
				+ " Approved\"],");
		graphs.write("pending: [ \"" + (mustpend - notYetMust)
				+ " Pending\", \"" + (shouldpend - notYetShould)
				+ " Pending\", \"" + (maypend - notYetMay) + " Pending\" ],");
		graphs.write("autoPend: [ \"" + notYetMust + " Awaiting Approval\", \""
				+ notYetShould + " Awaiting Approval\", \"" + notYetMay
				+ " Awaiting Approval\" ],");
		graphs.write("extended: [ \"" + mustex + " Extends\" , \"" + shouldex
				+ " Extends\" , \"" + mayex + " Extends\"],");
		graphs.write("deprecated: [ \"" + mustdep + " Deprecated\" , \""
				+ shoulddep + " Deprecated\" , \"" + maydep
				+ " Deprecated\" ] },");
		graphs.write("}); });");

		graphs.write("</script>");
	}

	private static void firstRead() throws IOException {
		acquireTestCases(rdfSourceTest);
		acquireTestCases(bcTest);
		acquireTestCases(commonContainerTest);
		acquireTestCases(commonResourceTest);
		acquireTestCases(nonRdfSourceTest);
		acquireTestCases(indirectContainerTest);
		acquireTestCases(directContianerTest);
	}

	private static void generateListOfTestCases() throws IOException {
		html.h2(id("tobeapproved")).content("Test Cases Ready for WG Approval");

		if (readyToBeApproved.size() == 0) {
			html.b().write("No test cases awaiting WG approval.")._b().br();
		} else {
			html.p().write("For details of test cases, ").a(href("https://github.com/w3c/ldp-testsuite")).
				write("see source in GitHub")._a()._p();
			html.ul();
			for(String tc: readyToBeApproved) {
				html.li().write(tc)._li();
			}
			html._ul();
		}
		toTop();
		
		html.h2(id("needCode")).content("Test Cases Yet to be Implemented");
		if (needCode.size() == 0)
			html.b().write("No test cases that need to be Implemented")._b()
					.br();
		else {
			html.ul();
			for (String tc : needCode)
				html.li().content(tc);
			html._ul();
		}
		toTop();
		
		html.h2().content("Implemented Test Classes");

		html.b().a(href("#" + rdfSourceTest.getCanonicalName()))
				.content(rdfSourceTest.getCanonicalName())._b();
		writeTestClassTable(rdfSourceTest);
		html.br();

		html.b().a(href("#" + bcTest.getCanonicalName()))
				.content(bcTest.getCanonicalName())._b();
		writeTestClassTable(bcTest);
		html.br();

		html.b().a(href("#" + commonContainerTest.getCanonicalName()))
				.content(commonContainerTest.getCanonicalName())._b();
		writeTestClassTable(commonContainerTest);
		html.br();

		html.b().a(href("#" + commonResourceTest.getCanonicalName()))
				.content(commonResourceTest.getCanonicalName())._b();
		writeTestClassTable(commonResourceTest);
		html.br();

		html.b().a(href("#" + indirectContainerTest.getCanonicalName()))
				.content(indirectContainerTest.getCanonicalName())._b();
		writeTestClassTable(indirectContainerTest);
		html.br();

		html.b().a(href("#" + directContianerTest.getCanonicalName()))
				.content(directContianerTest.getCanonicalName())._b();
		writeTestClassTable(directContianerTest);
		html.br();

		html.b().a(href("#" + nonRdfSourceTest.getCanonicalName()))
				.content(nonRdfSourceTest.getCanonicalName())._b();
		writeTestClassTable(nonRdfSourceTest);

		toTop();
		
		html.h2().a(id("manualTests"))
				.content("Tests that Must be Tested Manually")._h2();
		generateList(manuals);
		html.h2().a(id("clientTests")).content("Client-Based Test Cases")._h2();
		generateList(clients);

	}

	private static <T> void writeTestClassTable(Class<T> classType)
			throws IOException {
		html.div(class_("barChart").id(classType.getSimpleName() + "_bar"))
				._div();

		acquireTestInfo(classType);

	}

	private static <T> void acquireTestInfo(Class<T> classType)
			throws IOException {

		int total = 0;
		int must = 0;
		int should = 0;
		int may = 0;
		int auto = 0;
		// int pend = 0;
		int unimpl = 0;
		int extend = 0;
		int depre = 0;
		int client = 0;
		int manual = 0;

		Method[] methods = classType.getDeclaredMethods();

		for (Method method : methods) {
			if (method.isAnnotationPresent(SpecTest.class)
					&& method.isAnnotationPresent(Test.class)) {
				total++;
				SpecTest testLdp = method.getAnnotation(SpecTest.class);
				Test test = method.getAnnotation(Test.class);
				String group = Arrays.toString(test.groups());
				// if (!test.enabled())
				// disable++;
				if (group.contains("MUST"))
					must++;
				if (group.contains("SHOULD"))
					should++;
				if (group.contains("MAY"))
					may++;
				switch (testLdp.testMethod()) {
				case NOT_IMPLEMENTED:
					unimpl++;
					break;
				case MANUAL:
					manual++;
					break;
				case CLIENT_ONLY:
					client++;
					break;
				default:
					break;
				}
				switch (testLdp.approval()) {
				case WG_APPROVED:
					auto++;
					break;
				case WG_EXTENSION:
					extend++;
					break;
				case WG_DEPRECATED:
					depre++;
					break;
				default:
					break;
				}
			}
		}

		// write information into a Grafico bar chart
		writeResourceGraph(classType, total, unimpl, extend, depre, client,
				manual);

		html.table(class_("classes"));

		html.tr().th().content("Total Tests");
		html.th().content("Test Case Information")._tr();

		html.tr().td().content(total + "");
		html.td();
		if (must > 0)
			html.b().write("MUST: ")._b().write(must + "   ");
		if (should > 0)
			html.b().write("SHOULD: ")._b().write(should + "   ");
		if (may > 0)
			html.b().write("MAY: ")._b().write(may + "   ");
		html.br().br();
		html.ul();
		if (auto > 0)
			html.li().b().write("AUTOMATED: ")._b().write("" + auto)._li();
		if (extend > 0)
			html.li().b().write("EXTENDS: ")._b().write("" + extend)._li();
		if (depre > 0)
			html.li().b().write("DEPRECATED: ")._b().write("" + depre)._li();

		if (unimpl > 0)
			html.li().b().write("UNIMPLEMENTED: ")._b().write("" + unimpl)
					._li();

		if (client > 0)
			html.li().b().write("CLIENT ONLY: ")._b().write("" + client)._li();
		if (manual > 0)
			html.li().b().write("MANUAL: ")._b().write("" + manual)._li();

		html._ul();

		html._td();

		html._tr();
		html._table();

	}

	private static <T> void writeResourceGraph(Class<T> classType, int total,
			int unimpl, int extend, int depre, int client, int manual)
			throws IOException {

		graphs.write("<script>");
		graphs.write("Event.observe(window, 'load', function() {");

		graphs.write("var " + classType.getSimpleName() + "_bar"
				+ " = new Grafico.BarGraph($('" + classType.getSimpleName()
				+ "_bar'),");
		graphs.write("[" + (total - unimpl - extend - depre - client - manual)
				+ ", " + unimpl + ", " + extend + ", " + manual + ", " + client
				+ ", " + depre + "],");
		graphs.write("{ labels: [ \"Automated\", \"Unimplemented\", \"Extends\", \"Manual\", \"Client\", \"Deprecated\" ],");
		graphs.write("color: '#2f59bf',");
		graphs.write("label_rotation: -10,");
		graphs.write("hover_color: \"#ccccff\",");
		graphs.write("datalabels: { one: ["
				+ (total - unimpl - extend - depre - client - manual) + ", "
				+ unimpl + ", " + extend + ", " + manual + ", " + client + ", "
				+ depre + "]}");
		graphs.write(" }); });");

		graphs.write("</script>");
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
			html.h2().a(id(name)).write("Test Class: " + name)._a()._h2();
		Method[] bcMethods = classType.getDeclaredMethods();

		if (initialRead)
			html.ul();
		for (Method method : bcMethods) {
			if (method.isAnnotationPresent(Test.class)) {
				if (!initialRead) {
					generateInformation(method, name);
				} else {
					html.li().b().a(id(method.getName()))
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
		SpecTest testLdp = null;
		Test test = null;
		if (method.getAnnotation(SpecTest.class) != null
				&& method.getAnnotation(Test.class) != null) {
			testLdp = method.getAnnotation(SpecTest.class);
			test = method.getAnnotation(Test.class);

			if (!initialRead) {
				totalTests++;

				METHOD methodStatus = testLdp.testMethod();
				String group = Arrays.toString(test.groups());
				if (!test.enabled())
					disabled++;
				if (methodStatus.equals(METHOD.AUTOMATED))
					automated++;
				if (methodStatus.equals(METHOD.NOT_IMPLEMENTED)){
					unimplemented++;
					needCode.add(method.getName());
				}
				if (methodStatus.equals(METHOD.CLIENT_ONLY)) {
					clientTest++;
					clients.add(method.getName());
				}
				if (methodStatus.equals(METHOD.MANUAL)) {
					manual++;
					manuals.add(method.getName());
				}
				if (!refURI.contains(testLdp.specRefUri())) {
					refURI.add(testLdp.specRefUri());
					coverage++;

					if (group.contains("MUST"))
						must++;
					if (group.contains("SHOULD"))
						should++;
					if (group.contains("MAY"))
						may++;

					if (methodStatus.equals(METHOD.AUTOMATED)) {
						reqImpl++;
						if (group.contains("MUST"))
							mustImpl++;
						if (group.contains("SHOULD"))
							shouldImpl++;
						if (group.contains("MAY"))
							mayImpl++;
					}
					if (methodStatus.equals(METHOD.NOT_IMPLEMENTED)) {
						reqNotImpl++;
						if (group.contains("MUST"))
							mustNotImpl++;
						if (group.contains("SHOULD"))
							shouldNotImpl++;
						if (group.contains("MAY"))
							mayNotImpl++;
					}
				}
				switch (testLdp.approval()) {
				case WG_PENDING:
					pending++;
					if (group.contains("MUST")) {
						if (testLdp.testMethod() !=
								SpecTest.METHOD.NOT_IMPLEMENTED) {
							notYetMust++;
							readyToBeApproved.add(method.getName());
						} else
							mustpend++;
					}
					if (group.contains("SHOULD")) {
						if (testLdp.testMethod() !=
								SpecTest.METHOD.NOT_IMPLEMENTED) {
							notYetShould++;
							readyToBeApproved.add(method.getName());
						} else
							shouldpend++;
					}
					if (group.contains("MAY")) {
						if (testLdp.testMethod() !=
								SpecTest.METHOD.NOT_IMPLEMENTED) {
							notYetMay++;
							readyToBeApproved.add(method.getName());
						} else
							maypend++;
					}
					break;
				case WG_APPROVED:
					approved++;
					if (group.contains("MUST"))
						mustapp++;
					if (group.contains("SHOULD"))
						shouldapp++;
					if (group.contains("MAY"))
						mayapp++;
					break;
				case WG_EXTENSION:
					extended++;
					if (group.contains("MUST"))
						mustex++;
					if (group.contains("SHOULD"))
						shouldex++;
					if (group.contains("MAY"))
						mayex++;
					break;
				case WG_DEPRECATED:
					deprecated++;
					if (group.contains("MUST"))
						mustdep++;
					if (group.contains("SHOULD"))
						shoulddep++;
					if (group.contains("MAY"))
						maydep++;
					break;
				case WG_CLARIFICATION:
					clarification++;
					break;
				default:
					break;
				}
			} else {

				html.div(class_("pad-left"));
				html.b().write("Description: ")._b().write(test.description());
				html.br().b().write("Reference URI: ")._b()
						.a(href(testLdp.specRefUri()))
						.write(testLdp.specRefUri())._a();
				html.br().b().write("Groups: ")._b()
						.write(Arrays.toString(test.groups()));
				html.br().b().write("Status: ")._b()
						.write(testLdp.approval().toString());
				html.br().b().write("Test Case Implementation: ")._b()
						.write("" + testLdp.testMethod());
				html.br().b().write("Enabled: ")._b()
						.write("" + test.enabled());

				html._div();
				toTestClass(name);
			}
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
					+ "/ldp-testsuite-coverage-report.html"));
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
