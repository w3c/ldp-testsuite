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

		html.h2().content("Overall Specification Requirements Coverage");
		
		generateStatusGraph();
		writeStatusLegend();
		
		generateImplmntGraph();
		writeImplmntLegend();
		
		html.br();
		writeGraphDescription();

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
		html.span(class_("chartStart"));
		html.label(class_("label")).b().write("Test Case Status by Specification Requirements")._b()._label();
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
		html.span(class_("chartStart"));
		html.label(class_("label")).b().write("Test Case Status by Specification Requirements")._b()._label();
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
		html.br();
		writeTestClassTable(rdfSourceTest);
		html.br();

		html.b().a(href("#" + bcTest.getCanonicalName()))
				.content(bcTest.getCanonicalName())._b();
		html.br();
		writeTestClassTable(bcTest);
		html.br();

		html.b().a(href("#" + commonContainerTest.getCanonicalName()))
				.content(commonContainerTest.getCanonicalName())._b();
		html.br();
		writeTestClassTable(commonContainerTest);
		html.br();

		html.b().a(href("#" + commonResourceTest.getCanonicalName()))
				.content(commonResourceTest.getCanonicalName())._b();
		html.br();
		writeTestClassTable(commonResourceTest);
		html.br();

		html.b().a(href("#" + indirectContainerTest.getCanonicalName()))
				.content(indirectContainerTest.getCanonicalName())._b();
		html.br();
		writeTestClassTable(indirectContainerTest);
		html.br();

		html.b().a(href("#" + directContianerTest.getCanonicalName()))
				.content(directContianerTest.getCanonicalName())._b();
		html.br();
		writeTestClassTable(directContianerTest);
		html.br();

		html.b().a(href("#" + nonRdfSourceTest.getCanonicalName()))
				.content(nonRdfSourceTest.getCanonicalName())._b();
		html.br();
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
		html.span(class_("chartStart"));
		html.label(class_("label")).b().write("Test Case Status")._b()._label();
		html.div(class_("smallBar").id(classType.getSimpleName() + "_statusbar"))
				._div();
		writeStatusSmallLegend();

		html.span(class_("chartStart"));
		html.label(class_("label")).b().write("Test Case Implementation")._b()._label();
		html.div(class_("smallBar").id(classType.getSimpleName() + "_implmtbar"))
				._div();
		writeImplmntSmallLegend();

		acquireTestInfo(classType);

	}

	private static <T> void acquireTestInfo(Class<T> classType)
			throws IOException {
		int total = 0, must = 0, should = 0, may = 0;
		
		int auto = 0;
		int[] autoReq = { 0, 0, 0 }, unimReq = { 0, 0, 0 }, clientReq = { 0, 0,	0 }, manReq = { 0, 0, 0 };
		int unimpl = 0;
		int client = 0;
		int manual = 0;
		
		int approve = 0;
		int[] apprReq = { 0, 0, 0 }, pendReq = { 0, 0, 0 }, extReq = { 0, 0, 0 }, 
				depreReq = {0, 0, 0 }, clariReq = { 0, 0, 0 };
		int pend = 0;
		int extend = 0;
		int depre = 0;
		int clarify = 0;

		Method[] methods = classType.getDeclaredMethods();

		for (Method method : methods) {
			if (method.isAnnotationPresent(SpecTest.class)
					&& method.isAnnotationPresent(Test.class)) {
				total++;
				SpecTest testLdp = method.getAnnotation(SpecTest.class);
				Test test = method.getAnnotation(Test.class);
				String group = Arrays.toString(test.groups());
				if (group.contains("MUST"))
					must++;
				if (group.contains("SHOULD"))
					should++;
				if (group.contains("MAY"))
					may++;
				switch (testLdp.testMethod()) {
				case AUTOMATED:
					auto++;
					if (group.contains("MUST"))
						autoReq[0] = ++autoReq[0];
					if (group.contains("SHOULD"))
						autoReq[1] = ++autoReq[1];
					if (group.contains("MAY"))
						autoReq[2] = ++autoReq[2];
					break;
				case NOT_IMPLEMENTED:
					unimpl++;
					if (group.contains("MUST"))
						unimReq[0] = ++unimReq[0];
					if (group.contains("SHOULD"))
						unimReq[1] = ++unimReq[1];
					if (group.contains("MAY"))
						unimReq[2] = ++unimReq[2];
					break;
				case MANUAL:
					if (group.contains("MUST"))
						manReq[0] = ++manReq[0];
					if (group.contains("SHOULD"))
						manReq[1] = ++manReq[1];
					if (group.contains("MAY"))
						manReq[2] = ++manReq[2];
					manual++;
					break;
				case CLIENT_ONLY:
					client++;
					if (group.contains("MUST"))
						clientReq[0] = ++clientReq[0];
					if (group.contains("SHOULD"))
						clientReq[1] = ++clientReq[1];
					if (group.contains("MAY"))
						clientReq[2] = ++clientReq[2];
					break;
				default:
					break;
				}
				switch (testLdp.approval()) {
				case WG_PENDING:
					pend++;
					if (group.contains("MUST"))
						pendReq[0] = ++pendReq[0];
					if (group.contains("SHOULD"))
						pendReq[1] = ++pendReq[1];
					if (group.contains("MAY"))
						pendReq[2] = ++pendReq[2];
					break;
				case WG_APPROVED:
					approve++;
					if (group.contains("MUST"))
						apprReq[0] = ++apprReq[0];
					if (group.contains("SHOULD"))
						apprReq[1] = ++apprReq[1];
					if (group.contains("MAY"))
						apprReq[2] = ++apprReq[2];
					break;
				case WG_EXTENSION:
					extend++;
					if (group.contains("MUST"))
						extReq[0] = ++extReq[0];
					if (group.contains("SHOULD"))
						extReq[0] = ++extReq[0];
					if (group.contains("MAY"))
						extReq[1] = ++extReq[1];
					break;
				case WG_DEPRECATED:
					depre++;
					if (group.contains("MUST"))
						depreReq[0] = ++depreReq[0];
					if (group.contains("SHOULD"))
						depreReq[1] = ++depreReq[1];
					if (group.contains("MAY"))
						depreReq[2] = ++depreReq[2];
					break;
				case WG_CLARIFICATION:
					clarify++;
					if (group.contains("MUST"))
						clariReq[0] = ++clariReq[0];
					if (group.contains("SHOULD"))
						clariReq[1] = ++clariReq[1];
					if (group.contains("MAY"))
						clariReq[2] = ++clariReq[2];
					break;
				default:
					break;
				}
			}
		}

		// write information into Grafico bar charts
		writeStatusGraph(classType, apprReq, pendReq, extReq, depreReq, clariReq);
		writeImplementationGraph(classType, autoReq, unimReq, clientReq, manReq);

		html.table(class_("classes"));

		html.tr().th().content("Total Tests");
		html.th().content("Test Case Information");
		html.th().content("Status");
		html.th().content("Implementation");
		html._tr();

		html.tr().td().content(total + "");
		html.td();
		if (must > 0)
			html.b().write("MUST: ")._b().write(must + "   ");
		if (should > 0)
			html.b().write("SHOULD: ")._b().write(should + "   ");
		if (may > 0)
			html.b().write("MAY: ")._b().write(may + "   ");
		html.br().br();

		html._td().td();
		if(approve > 0)
			html.b().write("APPROVED: ")._b().write("" + approve).br();
		if(pend > 0)
			html.b().write("PENDING: ")._b().write("" + pend).br();
		if (extend > 0)
			html.b().write("EXTENDS: ")._b().write("" + extend).br();
		if (depre > 0)
			html.b().write("DEPRECATED: ")._b().write("" + depre).br();
		if(clarify > 0)
			html.b().write("CLARIFICATION: ")._b().write("" + clarify).br();		

		html._td().td();
		if (auto > 0)
			html.b().write("AUTOMATED: ")._b().write("" + auto).br();
		if (unimpl > 0)
			html.b().write("UNIMPLEMENTED: ")._b().write("" + unimpl).br();
		if (client > 0)
			html.b().write("CLIENT ONLY: ")._b().write("" + client).br();
		if (manual > 0)
			html.b().write("MANUAL: ")._b().write("" + manual).br();

		html._td();

		html._tr();
		html._table();

	}

	private static <T> void writeStatusGraph(Class<T> classType, int[] apprReq, int[] pendReq, int[] extReq, int[] depreReq, int[] clariReq)
			throws IOException {
		graphs.write("<script>");
		graphs.write("Event.observe(window, 'load', function() {");

		graphs.write("var " + classType.getSimpleName() + "_statusbar"
				+ " = new Grafico.StackedBarGraph($('" + classType.getSimpleName()
				+ "_statusbar'),{");
		graphs.write("approved: [" + apprReq[0] + ", " + apprReq[1] + ", " + apprReq[2] + " ],");
		graphs.write("pending: [" + pendReq[0] + ", " + pendReq[1] + ", " + pendReq[2] + " ],");
		graphs.write("extends: [" + extReq[0] + ", " + extReq[1] + ", " + extReq[2] + " ],");
		graphs.write("deprecated: [" + depreReq[0] + ", " + depreReq[1] + ", " + depreReq[2] + " ],");
		graphs.write("clarify: [" + clariReq[0] + ", " + clariReq[1] + ", " + clariReq[2] + "] },");
		graphs.write("{ labels: [ \"MUST\", \"SHOULD\", \"MAY\" ],");
		graphs.write("colors: { approved: '#501cbf', pending:'#bf1c76', extends: '#bf571c', deprecated: '#606060', client: '#1cbf95' },");
		graphs.write("hover_color: \"#ccccff\",");
		graphs.write("datalabels: { ");
		graphs.write("approved: [ \"" + apprReq[0] + " Approved\", \"" + apprReq[1] + " Approved\", \"" + apprReq[2] + " Approved\" ],");
		graphs.write("pending: [ \"" + pendReq[0] + " Pending\", \"" + pendReq[1] + " Pending\", \"" + pendReq[2] + " Pending\" ],");
		graphs.write("extends: [ \"" + extReq[0] + " Extends\", \"" + extReq[1] + " Extends\", \"" + extReq[2] + " Extends\" ],");
		graphs.write("deprecated: [ \"" + depreReq[0] + " Deprecated\", \"" + depreReq[1] + " Deprecated\", \"" + depreReq[2] + " Deprecated\" ],");
		graphs.write("clarify: [ \"" + clariReq[0] + " Clarification\", \"" + clariReq[1] + " Clarification\", \"" + clariReq[2] + " Clarification\" ] },");

		graphs.write(" }); });");

		graphs.write("</script>");
		
		
	}
	
	private static <T> void writeImplementationGraph(Class<T> classType,
			int[] autoReq, int[] unimReq, int[] clientReq, int[] manReq) {
		graphs.write("<script>");
		graphs.write("Event.observe(window, 'load', function() {");

		graphs.write("var " + classType.getSimpleName() + "_implmtbar"
				+ " = new Grafico.StackedBarGraph($('" + classType.getSimpleName()
				+ "_implmtbar'), {");
		graphs.write("automated: [" + autoReq[0] + ", " + autoReq[1] + ", " + autoReq[2] + " ],");
		graphs.write("unimplemented: [" + unimReq[0] + ", " + unimReq[1] + ", " + unimReq[2] + " ],");
		graphs.write("client: [" + clientReq[0] + ", " + clientReq[1] + ", " + clientReq[2] + " ],");
		graphs.write("manual: [" + manReq[0] + ", " + manReq[1] + ", " + manReq[2] + "] },");
		graphs.write("{ labels: [ \"MUST\", \"SHOULD\", \"MAY\" ],");
		graphs.write("colors: { automated: '#0099cc', unimplemented:'#bf1c21', client: '#bcbf1c', manual: '#3300cc' },");
		graphs.write("hover_color: \"#ccccff\",");
		graphs.write("datalabels: { ");
		graphs.write("automated: [ \"" + autoReq[0] + " Automated\", \"" + autoReq[1] + " Automated\", \"" + autoReq[2] + " Automated\" ],");
		graphs.write("unimplemented: [ \"" + unimReq[0] + " Unimplemented\", \"" + unimReq[1] + " Unimplemented\", \"" + unimReq[2] + " Unimplemented\" ],");
		graphs.write("client: [ \"" + clientReq[0] + " Client-Based\", \"" + clientReq[1] + " Client-Based\", \"" + clientReq[2] + " Client-Based\" ],");
		graphs.write("manual: [ \"" + manReq[0] + " Manual\", \"" + manReq[1] + " Manual\", \"" + manReq[2] + " Manual\" ] },");

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

		html._span();
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
		
		html._span();
	}

	private static void writeStatusSmallLegend() throws IOException {
		html.write("<svg id=\"graphLegend\" width=\"150\" height=\"225\">", NO_ESCAPE);
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"0\" style=\"fill:#501cbf\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"13\" fill=\"black\">Approved Tests</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"20\" style=\"fill:#bf1c76\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"33\" fill=\"black\">Pending Tests</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"40\" style=\"fill:#bf571c\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"53\" fill=\"black\">Extending Tests</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"60\" style=\"fill:#606060\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"73\" fill=\"black\">Deprecated Tests</text>", NO_ESCAPE);	
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"80\" style=\"fill:#1cbf95\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"93\" fill=\"black\">Client Tests</text>", NO_ESCAPE);
		
		html.write("</svg>");
		
		html._span();
	}

	private static void writeImplmntSmallLegend() throws IOException {
		html.write("<svg id=\"graphLegend\" width=\"175\" height=\"225\">", NO_ESCAPE);
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"0\" style=\"fill:#0099cc\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"13\" fill=\"black\">Automated Tests</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"20\" style=\"fill:#bf1c21\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"33\" fill=\"black\">Unimplemented Tests</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"40\" style=\"fill:#bcbf1c\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"53\" fill=\"black\">Client-Based Tests</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"60\" style=\"fill:#3300cc\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"73\" fill=\"black\">Manual Tests</text>", NO_ESCAPE);		
		
		html.write("</svg>");
		
		html._span();
	}
	private static void writeGraphDescription() throws IOException {
		html.h3().content("Description of the Chart Information");
		
		html.h4().content("Test Status");
		html.ul();
		html.li().b().write("Approved: ")._b().write("WG_APPROVED - working group has approved this test case")._li();
		html.li().b().write("Pending: ")._b().write("WG_PENDING (default) - no official recommendation from the WG supporting the specification being tested by this test suite")._li();
		html.li().b().write("Extends: ")._b().write("WG_EXTENSION - valuable test case but not part of the WG approved set")._li();
		html.li().b().write("Deprecated: ")._b().write("WG_DEPRECATED - no longer recommended by WG")._li();
		html.li().b().write("Clarification: ")._b().write("WG_CLARIFICATION - requires further clarification from the working group")._li();
		html._ul();
		
		html.h4().content("Test Implementation");
		html.ul();
		html.li().b().write("Automated: ")._b().write("AUTOMATED - implementation complete")._li();
		html.li().b().write("Not Implemented: ")._b().write("NOT_IMPLEMENTED (default) - possible to implement, just not done")._li();
		html.li().b().write("Client-Based: ")._b().write("CLIENT_ONLY - test is only client-side, this test suite doesn't test it")._li();
		html.li().b().write("Manual: ")._b().write("MANUAL - server test but not automated")._li();
		html._ul();
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
