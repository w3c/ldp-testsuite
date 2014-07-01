package org.w3.ldp.testsuite.reporter;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.StringResource;
import org.testng.annotations.Test;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;
import org.w3.ldp.testsuite.annotations.SpecTest.STATUS;
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

	private static int mustImpl = 0;
	private static int shouldImpl = 0;
	private static int mayImpl = 0;

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

	private static int mustex = 0;
	private static int shouldex = 0;
	private static int mayex = 0;

	private static int mustdep = 0;
	private static int shoulddep = 0;
	private static int maydep = 0;

	private static int mustPend = 0;
	private static int shouldPend = 0;
	private static int mayPend = 0;

	private static int mustMan;
	private static int shouldMan;
	private static int mayMan;

	private static int mustClient;
	private static int shouldClient;
	private static int mayClient;

	private static int mayAppr;
	private static int shouldAppr;
	private static int mustAppr;

	public static void main(String[] args) throws IOException {
		initialRead = false;
		firstRead();
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

		generateStatusGraph();
		writeStatusLegend();

		// TODO divide charts based on status and implementation
		generateImplmntGraph(); // TODO
		writeImplmntLegend(); // TODO

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

		html.br().b().a(href("#tobeapproved")).write(mustPend + shouldPend + mayPend 
				+ " Ready for WG Approval")._a()._b();

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
		html.li().b().write((mustImpl+shouldImpl+mayImpl) + " ")._b().write("Requirements Automated")
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

		html.li().b().write(unimplemented + " ")._b().write("of the Tests ")
				.a(href("#needCode")).write("Yet to be Coded")._a()._li();
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
		html.li().b().write((mustNotImpl + shouldNotImpl + mayNotImpl) + " ")
				._b().write("Requirements not Implemented")._li();
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

	private static void generateStatusGraph() throws IOException {

		html.h2().content("Overall Specification Requirements Coverage");
		html.div(id("status_bar").class_("barChart"))._div();
		graphs.write("<script>");
		graphs.write("Event.observe(window, 'load', function() {");
		graphs.write("var status_bar = new Grafico.StackedBarGraph($('status_bar'), {");
		graphs.write("approved: [ " + mustAppr + ", " + shouldAppr + ", "
				+ mayAppr + "],");
		graphs.write("pending: [" + mustPend + ", " + shouldPend + ", "
				+ mayPend + " ],");
		graphs.write("extended: [" + mustex + ", " + shouldex + ", " + mayex
				+ "],");
		graphs.write("deprecated: [" + mustdep + ", " + shoulddep + ", "
				+ maydep + "] },");
		graphs.write("{ labels: [ \"MUST\", \"SHOULD\", \"MAY\" ],");
		graphs.write("colors: { approved: '#a2bf2f', pending:'#1cbfbb', extended: '#bfa22f', deprecated: '#606060' },");
		graphs.write("hover_color: \"#ccccff\",");
		graphs.write("datalabels: { ");
		graphs.write("approved: [ \"" + mustAppr + " Approved\", \""
				+ shouldAppr + " Approved\", \"" + mayAppr + " Approved\" ],");
		graphs.write("pending: [ \"" + mustPend + " Pending\", \""
				+ shouldPend + " Pending\", \"" + mayPend
				+ " Pending\" ],");
		graphs.write("extended: [ \"" + mustex + " Extends\" , \"" + shouldex
				+ " Extends\" , \"" + mayex + " Extends\"],");
		graphs.write("deprecated: [ \"" + mustdep + " Deprecated\" , \""
				+ shoulddep + " Deprecated\" , \"" + maydep
				+ " Deprecated\" ] },");
		graphs.write("}); });");

		graphs.write("</script>");
	}

	private static void generateImplmntGraph() throws IOException {
		// TODO Auto-generated method stub
		html.div(id("implmnt_bar").class_("barChart"))._div();
		graphs.write("<script>");
		graphs.write("Event.observe(window, 'load', function() {");
		graphs.write("var implmnt_bar = new Grafico.StackedBarGraph($('implmnt_bar'), {");
		graphs.write("automated: [ " + mustImpl + ", " + shouldImpl + ", "
				+ mayImpl + "],");
		graphs.write("unimplemented: [" + mustNotImpl + ", " + shouldNotImpl
				+ ", " + mayNotImpl + " ],");
		graphs.write("client: [" + mustClient + ", " + shouldClient + ", "
				+ mayClient + " ],");
		graphs.write("manual: [" + mustMan + ", " + shouldMan + ", " + mayMan
				+ " ],");
		graphs.write("},");
		graphs.write("{ labels: [ \"MUST\", \"SHOULD\", \"MAY\" ],");
		graphs.write("colors: { automated: '#0099cc', unimplemented: '#bf1c56', client: '#8d1cbf', manual: '#3300cc' },");
		graphs.write("hover_color: \"#ccccff\",");
		graphs.write("datalabels: { ");
		graphs.write("automated: [ \"" + mustImpl + " Automated\", \""
				+ shouldImpl + " Automated\", \"" + mayImpl
				+ " Automated\" ],");
		graphs.write("unimplemented: [ \"" + mustNotImpl
				+ " Unimplemented\", \"" + shouldNotImpl
				+ " Unimplemented\", \"" + mayNotImpl + " Unimplemented\" ],");
		graphs.write("client: [ \"" + mustClient + " Client-Based\", \""
				+ shouldClient + " Client-Based\", \"" + mayClient
				+ " Client-Based\" ],");
		graphs.write("manual: [ \"" + mustMan + " Manual\", \"" + shouldMan
				+ " Manual\", \"" + mayMan + " Manual\" ],");
		graphs.write(" },");
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
			html.p().write("For details of test cases, ")
					.a(href("https://github.com/w3c/ldp-testsuite"))
					.write("see source in GitHub")._a()._p();
			html.ul();
			for (String tc : readyToBeApproved) {
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
				STATUS testApproval = testLdp.approval();
				String group = Arrays.toString(test.groups());
				if (!refURI.contains(testLdp.specRefUri())) { // for just the requirement testing
					refURI.add(testLdp.specRefUri());
					coverage++;
					if (group.contains("MUST")) {
						must++;
						switch (methodStatus) {
						case AUTOMATED:
							automated++;
							mustImpl++;
							if (testApproval.equals(STATUS.WG_PENDING))
								readyToBeApproved.add(method.getName());
							break;
						case CLIENT_ONLY:
							clients.add(method.getName());
							clientTest++;
							mustClient++;
							break;
						case MANUAL:
							manuals.add(method.getName());
							manual++;
							mustMan++;
							break;
						case NOT_IMPLEMENTED:
							mustNotImpl++;
							unimplemented++;
							needCode.add(method.getName());
							break;
						default:
							break;
						}
						switch (testApproval) {
						case WG_APPROVED:
							mustAppr++;
							approved++;
							break;
						case WG_CLARIFICATION:
							clarification++;
							break;
						case WG_DEPRECATED:
							deprecated++;
							mustdep++;
							break;
						case WG_EXTENSION:
							extended++;
							mustex++;
							break;
						case WG_PENDING:
							mustPend++;
							pending++;
						default:
							break;
						}

					}
					if (group.contains("SHOULD")) {
						should++;
						switch (methodStatus) {
						case AUTOMATED:
							automated++;
							shouldImpl++;
							if (testApproval.equals(STATUS.WG_PENDING))
								readyToBeApproved.add(method.getName());
							break;
						case CLIENT_ONLY:
							clients.add(method.getName());
							clientTest++;
							shouldClient++;
							break;
						case MANUAL:
							manuals.add(method.getName());
							manual++;
							shouldMan++;
							break;
						case NOT_IMPLEMENTED:
							shouldNotImpl++;
							unimplemented++;
							needCode.add(method.getName());
							break;
						default:
							break;
						}
						switch (testApproval) {
						case WG_APPROVED:
							shouldAppr++;
							approved++;
							break;
						case WG_CLARIFICATION:
							clarification++;
							break;
						case WG_DEPRECATED:
							deprecated++;
							shoulddep++;
							break;
						case WG_EXTENSION:
							extended++;
							shouldex++;
							break;
						case WG_PENDING:
							shouldPend++;
							pending++;
						default:
							break;
						}
					}
					if (group.contains("MAY")) {
						may++;
						switch (methodStatus) {
						case AUTOMATED:
							automated++;
							mayImpl++;
							if (testApproval.equals(STATUS.WG_PENDING))
								readyToBeApproved.add(method.getName());
							break;
						case CLIENT_ONLY:
							clients.add(method.getName());
							clientTest++;
							mayClient++;
							break;
						case MANUAL:
							manuals.add(method.getName());
							manual++;
							mayMan++;
							break;
						case NOT_IMPLEMENTED:
							mayNotImpl++;
							unimplemented++;
							needCode.add(method.getName());
							break;
						default:
							break;
						}
						switch (testApproval) {
						case WG_APPROVED:
							mayAppr++;
							approved++;
							break;
						case WG_CLARIFICATION:
							clarification++;
							break;
						case WG_DEPRECATED:
							deprecated++;
							maydep++;
							break;
						case WG_EXTENSION:
							extended++;
							mayex++;
							break;
						case WG_PENDING:
							mayPend++;
							pending++;
						default:
							break;
						}
					}
				} else { // for all the other test cases
					switch (methodStatus) {
					case AUTOMATED:
						automated++;
						if (testApproval.equals(STATUS.WG_APPROVED))
						if (testApproval.equals(STATUS.WG_PENDING)) {
							readyToBeApproved.add(method.getName());
						}
						break;
					case CLIENT_ONLY:
						clients.add(method.getName());
						clientTest++;
						break;
					case MANUAL:
						manuals.add(method.getName());
						manual++;
						break;
					case NOT_IMPLEMENTED:
						unimplemented++;
						needCode.add(method.getName());
						break;
					default:
						break;
					}
					switch (testApproval) {
					case WG_APPROVED:
						approved++;
						break;
					case WG_CLARIFICATION:
						clarification++;
						break;
					case WG_DEPRECATED:
						deprecated++;
						break;
					case WG_EXTENSION:
						extended++;
						break;
					case WG_PENDING:
						pending++;
					default:
						break;
					}
				}

			} else {
				// write in the test information
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
	
	private static void writeStatusLegend() throws IOException {
		html.write("<svg id=\"graphLegend\" width=\"200\" height=\"225\">", NO_ESCAPE);
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"0\" style=\"fill:#a2bf2f\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"13\" fill=\"black\">Approved Tests</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"20\" style=\"fill:#1cbfbb\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"33\" fill=\"black\">Pending</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"40\" style=\"fill:#bfa22f\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"53\" fill=\"black\">Extending Tests</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"60\" style=\"fill:#606060 \"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"73\" fill=\"black\">Deprecated Tests</text>", NO_ESCAPE);
		
		
		html.write("</svg>");
	}
	
	private static void writeImplmntLegend() throws IOException {
		html.write("<svg id=\"graphLegend\" width=\"200\" height=\"225\">", NO_ESCAPE);
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"0\" style=\"fill:#0099cc\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"13\" fill=\"black\">Automated Tests</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"20\" style=\"fill:#bf1c56\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"33\" fill=\"black\">Unimplemented Tests</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"40\" style=\"fill:#8d1cbf\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"53\" fill=\"black\">Client-Based Tests</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"60\" style=\"fill:#3300cc\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"73\" fill=\"black\">Manual Tests</text>", NO_ESCAPE);		
		
		html.write("</svg>");
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
