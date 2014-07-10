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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import static org.rendersnake.HtmlAttributesFactory.*;

public class LdpTestCaseReporter {

	private static HtmlCanvas html;

	private static boolean initialRead;

	private static StringWriter graphs = new StringWriter();

	private static Set<String> refURI = new HashSet<String>();

	private static ArrayList<String> clients = new ArrayList<String>();
	private static ArrayList<String> manuals = new ArrayList<String>();
	private static HashMap<String, String> readyToBeApproved = new HashMap<String, String>(); // key==method, value==class
	private static HashMap<String, String> needCode = new HashMap<String, String>(); // key==method, value==class
	
	private static final HashMap<String, String> implmColor = new HashMap<String, String>(); // key==label value==color
	private static final HashMap<String, String> statusColor = new HashMap<String, String>();
	static{
		implmColor.put("automated", "#0099cc");
		implmColor.put("unimplemented", "#bf1c56");
		implmColor.put("client", "#8d1cbf");
		implmColor.put("manual", "#3300cc");
		statusColor.put("approved", "#a2bf2f");
		statusColor.put("pending", "#1cbfbb");
		statusColor.put("extends", "#bfa22f");
		statusColor.put("deprecated", "#606060");
		statusColor.put("clarify", "#1bff95");
	}
	private static final int MUST = 0;
	private static final int SHOULD = 1;
	private static final int MAY = 2;
	
	private static int totalTests = 0;
	private static int automated = 0;

	private static int mustTotal = 0;
	private static int shouldTotal = 0;
	private static int mayTotal = 0;

	private static int[] auto  = {0, 0, 0};
	private static int[] unimplmnt  = {0, 0, 0};

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

	private static int[] extnd = {0, 0, 0};
	private static int[] deprctd  = {0, 0, 0};
	private static int[] pend  = {0, 0, 0};
	private static int[] manual = {0, 0, 0};
	private static int[] client = {0, 0, 0};
	private static int[] approve = {0, 0, 0};	
	private static int[] clarify = {0, 0, 0};

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
		html.script().content(StringResource.get("/raphael/raphael-min.js"), NO_ESCAPE);
		html.script().content(StringResource.get("/prototype/prototype.js"), NO_ESCAPE);
		html.script().content(StringResource.get("/grafico/grafico-min.js"), NO_ESCAPE);
		html.write(graphs.toString(), NO_ESCAPE);

		html._body()._html();
	}

	private static void createSummaryReport() throws IOException {

		initialRead = true;
		html.h2().content("Summary of Test Methods");
		html.table(class_("summary"));
		html.tr().th().content("Totals");
		html.th().content("Coverage");
		html.th().content("Not Implemented");
		html._tr();

		html.tr();
		html.td().b().write("" + totalTests)._b().write(" Total Tests");
		html.ul().li().span(style(writeBlockStyle(statusColor, "approved")))._span()
			.b().write(approved + " ")._b().write("WG Approved")._li();
		html.li().span(style(writeBlockStyle(statusColor, "pending")))._span()
			.b().write(pending + " ")._b().write("Incomplete")._li();
		html.li().span(style(writeBlockStyle(statusColor, "extends")))._span()
			.b().write(extended + " ")._b().write("Extension")._li();
		html.li().span(style(writeBlockStyle(statusColor, "deprecated")))._span()
			.b().write(deprecated + " ")._b().write("No longer valid")._li();
		html.li().span(style(writeBlockStyle(statusColor, "clarify")))._span()
			.b().write(clarification + " ")._b().write("Needs to be clarified with WG")._li();
		html._ul();

		html.br().b().a(href("#tobeapproved")).write(readyToBeApproved.size()
				+ " Ready for WG Approval")._a()._b();
		
		html.br();
		html.span(class_("chartStart"));
		// html.label(class_("label")).b().write("Test Case Implementation for Totals")._b()._label();
		html.div(class_("barChart").id("overall_statusbar"))._div();
		writeStatusGraph("overall", approve, pend, extnd, deprctd, clarify);
		html._span();
		html._td();

		html.td();

		html.b().write(automated + " / " + totalTests)._b()
				.write(" of Total Tests Automated").br();
		html.b().write(automated + " / " + (automated + needCode.size()))._b()
				.write(" of Tests Possible to Automate");
		html.ul();
		html.li().b().write("" + refURI.size())._b().write(" Requirements Covered")
				._li();
		html.ul().li().b().write("" + mustTotal)._b().write(" MUST")._li();
		html.li().b().write("" + shouldTotal)._b().write(" SHOULD")._li();
		html.li().b().write("" + mayTotal)._b().write(" MAY")._li()._ul();
		html._ul();

		html.ul();
		int implemented = getTotal(auto);
		html.li().span(style(writeBlockStyle(implmColor, "automated")))._span()
			.b().write(implemented + " ")._b().write("Requirements Automated")._li();
		html.ul();
		html.li().b().write(auto[MUST] + " / " + mustTotal)._b().write(" MUST")._li();
		html.li().b().write(auto[SHOULD] + " / " + shouldTotal)._b().write(" SHOULD")
				._li();
		html.li().b().write(auto[MAY] + " / " + mayTotal)._b().write(" MAY")._li();
		html._ul();
		html._ul();
		
		html.span(class_("chartStart"));
		// html.label(class_("label")).b().write("Test Case Status for Coverage")._b()._label();
		html.div(class_("barChart").id("overall_implmtbar"))._div();
		writeImplementationGraph("overall", auto, unimplmnt, client, manual);
		html._span();
		html._td();

		// html.td().content(totalImplemented + " Tests");
		html.td();
		html.b().write((needCode.size() + clients.size() + manuals.size()) + " ")._b()
				.write("of the Total Tests");
		html.ul();

		html.li().b().write(needCode.size() + " ")._b().write("of the Tests ")
				.a(href("#needCode")).write("Yet to be Coded")._a()._li();
		/*
		 * TODO: Determine if disabled is really valuable or not
		 * html.li().b().write(disabled +
		 * " ")._b().write("of the Tests not enabled")._li();
		 */
		html.li().span(style(writeBlockStyle(implmColor, "client")))._span()
			.b().write(clients.size() + " ")._b().write("of the Total are ")
			.a(href("#clientTests")).write("Client-Based Tests")._a()._li();
		html.li().span(style(writeBlockStyle(implmColor, "manual")))._span()
			.b().write(manuals.size() + " ")._b().write("of the Total must be ")
			.a(href("#manualTests")).write("Tested Manually")._a()._li();
		html._ul();

		html.write("From the Total, ");

		html.ul();
		int unimplemented = getTotal(unimplmnt);
		html.li().span(style(writeBlockStyle(implmColor, "unimplemented")))._span()
			.b().write(unimplemented + " ")
			._b().write("Requirements not Implemented")._li();
		html.ul();
		html.li().b().write(unimplmnt[MUST] + " ")._b().write("MUST")._li();
		html.li().b().write(unimplmnt[SHOULD] + " ")._b().write("SHOULD")._li();
		html.li().b().write(unimplmnt[MAY] + " ")._b().write("MAY")._li();
		html._ul();
		html._ul()._td();
		html._tr();
		html._table();

		writeGraphDescription();
		generateListOfTestCases();

	}

	private static String writeBlockStyle(HashMap<String, String> list, String string) {
		return "background-color:" + list.get(string) + "; height:15px; width:15px; float:left";
	}

	private static int getTotal(int[] array) {
		int total = 0;
		for(int i = 0; i < array.length; i++)
			total += array[i];
		return total;
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
			iterateTableList(readyToBeApproved);
			html._ul();
		}
		toTop();

		html.h2(id("needCode")).content("Test Cases Yet to be Implemented");
		if (needCode.size() == 0)
			html.b().write("No test cases that need to be Implemented")._b()
					.br();
		else {
			html.ul();
			iterateTableList(needCode);
			html._ul();
		}
		toTop();

		html.h2().content("Implemented Test Classes");

		writeTestTables(rdfSourceTest);
		writeTestTables(bcTest);
		writeTestTables(commonContainerTest);
		writeTestTables(commonResourceTest);
		writeTestTables(indirectContainerTest);
		writeTestTables(directContianerTest);
		writeTestTables(nonRdfSourceTest);

		toTop();

		html.h2().a(id("manualTests"))
				.content("Tests that Must be Tested Manually")._h2();
		generateList(manuals);
		html.h2().a(id("clientTests")).content("Client-Based Test Cases")._h2();
		generateList(clients);

	}

	private static <T> void writeTestTables(Class<T> testClass) throws IOException {
		html.b().a(href("#" + testClass.getCanonicalName()))
				.content(testClass.getCanonicalName())._b();
		html.br();
		writeTestClassTable(testClass);
		html.br();
	}

	private static void iterateTableList(HashMap<String, String> list) throws IOException {
		String className;
		String methodName;
		Iterator<Entry<String, String>> codeIter = list.entrySet().iterator();
		while(codeIter.hasNext()){
			Entry<String, String> entry = codeIter.next();
			className = entry.getValue();
			className = className.substring(className.lastIndexOf(".") + 1);
			methodName = entry.getKey();
			html.li().a(href("#" + methodName)).b().write(className + "::")._b().write(methodName)._a()._li();
		}
	}

	private static <T> void writeTestClassTable(Class<T> classType)
			throws IOException {
		html.span(class_("chartStart"));
		html.label(class_("label")).b().write("Test Case Status")._b()._label();
		html.div(class_("smallBar").id(classType.getSimpleName() + "_statusbar"))._div();
		writeStatusLegend();

		html.span(class_("chartStart"));
		html.label(class_("label")).b().write("Test Case Implementation")._b()._label();
		html.div(class_("smallBar").id(classType.getSimpleName() + "_implmtbar"))._div();
		writeImplmntLegend();

		acquireTestInfo(classType);

	}

	private static <T> void acquireTestInfo(Class<T> classType)
			throws IOException {
		int total = 0, must = 0, should = 0, may = 0;
		
		int[] autoReq = { 0, 0, 0 }, unimReq = { 0, 0, 0 }, clientReq = { 0, 0,	0 }, manReq = { 0, 0, 0 };
		int[] apprReq = { 0, 0, 0 }, pendReq = { 0, 0, 0 }, extReq = { 0, 0, 0 }, 
				depreReq = {0, 0, 0 }, clariReq = { 0, 0, 0 };

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
					if (group.contains("MUST"))
						++autoReq[MUST];
					if (group.contains("SHOULD"))
						++autoReq[SHOULD];
					if (group.contains("MAY"))
						++autoReq[MAY];
					break;
				case NOT_IMPLEMENTED:
					if (group.contains("MUST"))
						++unimReq[MUST];
					if (group.contains("SHOULD"))
						++unimReq[SHOULD];
					if (group.contains("MAY"))
						++unimReq[MAY];
					break;
				case MANUAL:
					if (group.contains("MUST"))
						++manReq[MUST];
					if (group.contains("SHOULD"))
						++manReq[SHOULD];
					if (group.contains("MAY"))
						++manReq[MAY];
					break;
				case CLIENT_ONLY:
					if (group.contains("MUST"))
						++clientReq[MUST];
					if (group.contains("SHOULD"))
						++clientReq[SHOULD];
					if (group.contains("MAY"))
						++clientReq[MAY];
					break;
				default:
					break;
				}
				switch (testLdp.approval()) {
				case WG_PENDING:
					if (group.contains("MUST"))
						++pendReq[MUST];
					if (group.contains("SHOULD"))
						++pendReq[SHOULD];
					if (group.contains("MAY"))
						++pendReq[MAY];
					break;
				case WG_APPROVED:
					if (group.contains("MUST"))
						++apprReq[MUST];
					if (group.contains("SHOULD"))
						++apprReq[SHOULD];
					if (group.contains("MAY"))
						++apprReq[MAY];
					break;
				case WG_EXTENSION:
					if (group.contains("MUST"))
						++extReq[MUST];
					if (group.contains("SHOULD"))
						++extReq[SHOULD];
					if (group.contains("MAY"))
						++extReq[MAY];
					break;
				case WG_DEPRECATED:
					if (group.contains("MUST"))
						++depreReq[MUST];
					if (group.contains("SHOULD"))
						++depreReq[SHOULD];
					if (group.contains("MAY"))
						++depreReq[MAY];
					break;
				case WG_CLARIFICATION:
					if (group.contains("MUST"))
						++clariReq[MUST];
					if (group.contains("SHOULD"))
						++clariReq[SHOULD];
					if (group.contains("MAY"))
						++clariReq[MAY];
					break;
				default:
					break;
				}
			}
		}

		// write information into Grafico bar charts
		writeStatusGraph(classType.getSimpleName(), apprReq, pendReq, extReq, depreReq, clariReq);
		writeImplementationGraph(classType.getSimpleName(), autoReq, unimReq, clientReq, manReq);

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
		int approve = getTotal(apprReq);
		if(approve > 0)
			html.b().write("APPROVED: ")._b().write("" + approve).br();
		int pend = getTotal(pendReq);
		if(pend > 0)
			html.b().write("PENDING: ")._b().write("" + pend).br();
		int extend = getTotal(extReq);
		if (extend > 0)
			html.b().write("EXTENDS: ")._b().write("" + extend).br();
		int depre = getTotal(depreReq);
		if (depre > 0)
			html.b().write("DEPRECATED: ")._b().write("" + depre).br();
		int clarify = getTotal(clariReq);
		if(clarify > 0)
			html.b().write("CLARIFICATION: ")._b().write("" + clarify).br();		

		html._td().td();
		int auto = getTotal(autoReq);
		if (auto > 0)
			html.b().write("AUTOMATED: ")._b().write("" + auto).br();
		int unimpl = getTotal(unimReq);
		if (unimpl > 0)
			html.b().write("UNIMPLEMENTED: ")._b().write("" + unimpl).br();
		int client = getTotal(clientReq);
		if (client > 0)
			html.b().write("CLIENT ONLY: ")._b().write("" + client).br();
		int manual = getTotal(manReq);
		if (manual > 0)
			html.b().write("MANUAL: ")._b().write("" + manual).br();

		html._td();

		html._tr();
		html._table();

	}

	private static void writeStatusGraph(String type, int[] apprReq, int[] pendReq, int[] extReq, int[] depreReq, int[] clariReq)
			throws IOException {
		graphs.write("<script>");
		graphs.write("Event.observe(window, 'load', function() {");

		graphs.write("var " + type + "_statusbar"
				+ " = new Grafico.StackedBarGraph($('" + type
				+ "_statusbar'),{");
		graphs.write("approved: [" + apprReq[0] + ", " + apprReq[1] + ", " + apprReq[2] + " ],");
		graphs.write("pending: [" + pendReq[0] + ", " + pendReq[1] + ", " + pendReq[2] + " ],");
		graphs.write("extends: [" + extReq[0] + ", " + extReq[1] + ", " + extReq[2] + " ],");
		graphs.write("deprecated: [" + depreReq[0] + ", " + depreReq[1] + ", " + depreReq[2] + " ],");
		graphs.write("clarify: [" + clariReq[0] + ", " + clariReq[1] + ", " + clariReq[2] + "] },");
		graphs.write("{ labels: [ \"MUST\", \"SHOULD\", \"MAY\" ],");
		graphs.write("colors: { ");
		writeColors(statusColor);
		graphs.write(" },");
		graphs.write("hover_color: \"#ccccff\",");
		graphs.write("datalabels: { ");
		graphs.write("approved: [ \"" + apprReq[0] + " WG Approved\", \"" + apprReq[1] + " WG Approved\", \"" + apprReq[2] + " WG Approved\" ],");
		graphs.write("pending: [ \"" + pendReq[0] + " WG Pending\", \"" + pendReq[1] + " WG Pending\", \"" + pendReq[2] + " WG Pending\" ],");
		graphs.write("extends: [ \"" + extReq[0] + " WG Extension\", \"" + extReq[1] + " WG Extension\", \"" + extReq[2] + " WG Extension\" ],");
		graphs.write("deprecated: [ \"" + depreReq[0] + " WG Deprecated\", \"" + depreReq[1] + " WG Deprecated\", \"" + depreReq[2] + " WG Deprecated\" ],");
		graphs.write("clarify: [ \"" + clariReq[0] + " WG Clarification\", \"" + clariReq[1] + " WG Clarification\", \"" + clariReq[2] + " WG Clarification\" ] },");

		graphs.write(" }); });");

		graphs.write("</script>");
		
		
	}

	private static void writeImplementationGraph(String classType,
			int[] autoReq, int[] unimReq, int[] clientReq, int[] manReq) {
		graphs.write("<script>");
		graphs.write("Event.observe(window, 'load', function() {");

		graphs.write("var " + classType + "_implmtbar"
				+ " = new Grafico.StackedBarGraph($('" + classType
				+ "_implmtbar'), {");
		graphs.write("automated: [" + autoReq[0] + ", " + autoReq[1] + ", " + autoReq[2] + " ],");
		graphs.write("unimplemented: [" + unimReq[0] + ", " + unimReq[1] + ", " + unimReq[2] + " ],");
		graphs.write("client: [" + clientReq[0] + ", " + clientReq[1] + ", " + clientReq[2] + " ],");
		graphs.write("manual: [" + manReq[0] + ", " + manReq[1] + ", " + manReq[2] + "] },");
		graphs.write("{ labels: [ \"MUST\", \"SHOULD\", \"MAY\" ],");
		graphs.write("colors: { ");
		writeColors(implmColor);
		graphs.write("},");
		graphs.write("hover_color: \"#ccccff\",");
		graphs.write("datalabels: { ");
		graphs.write("automated: [ \"" + autoReq[0] + " Automated\", \"" + autoReq[1] + " Automated\", \"" + autoReq[2] + " Automated\" ],");
		graphs.write("unimplemented: [ \"" + unimReq[0] + " Not Implemented\", \"" + unimReq[1] + " Not Implemented\", \"" + unimReq[2] + " Not Implemented\" ],");
		graphs.write("client: [ \"" + clientReq[0] + " Client Only\", \"" + clientReq[1] + " Client Only\", \"" + clientReq[2] + " Client Only\" ],");
		graphs.write("manual: [ \"" + manReq[0] + " Manual\", \"" + manReq[1] + " Manual\", \"" + manReq[2] + " Manual\" ] },");

		graphs.write(" }); });");

		graphs.write("</script>");
	}
	
	private static void writeColors(HashMap<String, String> colorList) {
		Iterator<Entry<String, String>> codeIter = colorList.entrySet().iterator();
		Entry<String, String> value;
		while(codeIter.hasNext()){
			value = codeIter.next();
				graphs.write(value.getKey() + ": '" + value.getValue() + "' ");
			if(codeIter.hasNext())
				graphs.write(", ");
		}
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
		Method[] declaredMethods = classType.getDeclaredMethods();

		if (initialRead)
			html.ul();
		for (Method method : declaredMethods) {
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
					if (group.contains("MUST")) {
						mustTotal++;
						switch (methodStatus) {
						case AUTOMATED:
							automated++;
							++auto[MUST];
							if (testApproval.equals(STATUS.WG_PENDING))
								readyToBeApproved.put(method.getName(), method.getDeclaringClass().getCanonicalName());
							break;
						case CLIENT_ONLY:
							clients.add(method.getName());
							++client[MUST];
							break;
						case MANUAL:
							manuals.add(method.getName());
							++manual[MUST];
							break;
						case NOT_IMPLEMENTED:
							++unimplmnt[MUST];
							needCode.put(method.getName(), method.getDeclaringClass().getCanonicalName());
							break;
						default:
							break;
						}
						switch (testApproval) {
						case WG_APPROVED:
							++approve[MUST];
							approved++;
							break;
						case WG_CLARIFICATION:
							clarification++;
							++clarify[MUST];
							break;
						case WG_DEPRECATED:
							deprecated++;;
							++deprctd[MUST];
							break;
						case WG_EXTENSION:
							extended++;
							++extnd[MUST];
							break;
						case WG_PENDING:
							++pend[MUST];
							pending++;
						default:
							break;
						}

					}
					if (group.contains("SHOULD")) {
						shouldTotal++;
						switch (methodStatus) {
						case AUTOMATED:
							automated++;
							++auto[SHOULD];
							if (testApproval.equals(STATUS.WG_PENDING))
								readyToBeApproved.put(method.getName(), method.getDeclaringClass().getCanonicalName());
							break;
						case CLIENT_ONLY:
							clients.add(method.getName());
							++client[SHOULD];
							break;
						case MANUAL:
							manuals.add(method.getName());
							++manual[SHOULD];
							break;
						case NOT_IMPLEMENTED:
							++unimplmnt[SHOULD];
							needCode.put(method.getName(), method.getDeclaringClass().getCanonicalName());
							break;
						default:
							break;
						}
						switch (testApproval) {
						case WG_APPROVED:
							++approve[SHOULD];
							approved++;
							break;
						case WG_CLARIFICATION:
							clarification++;
							++clarify[SHOULD];
							break;
						case WG_DEPRECATED:
							deprecated++;
							++deprctd[SHOULD];
							break;
						case WG_EXTENSION:
							extended++;
							++extnd[SHOULD];
							break;
						case WG_PENDING:
							++pend[SHOULD];
							pending++;
						default:
							break;
						}
					}
					if (group.contains("MAY")) {
						mayTotal++;
						switch (methodStatus) {
						case AUTOMATED:
							automated++;
							++auto[MAY];
							if (testApproval.equals(STATUS.WG_PENDING))
								readyToBeApproved.put(method.getName(), method.getDeclaringClass().getCanonicalName());
							break;
						case CLIENT_ONLY:
							clients.add(method.getName());
							++client[MAY];
							break;
						case MANUAL:
							manuals.add(method.getName());
							++manual[MAY];
							break;
						case NOT_IMPLEMENTED:
							++unimplmnt[MAY];
							needCode.put(method.getName(), method.getDeclaringClass().getCanonicalName());
							break;
						default:
							break;
						}
						switch (testApproval) {
						case WG_APPROVED:
							++approve[MAY];
							approved++;
							break;
						case WG_CLARIFICATION:
							clarification++;
							++clarify[MAY];
							break;
						case WG_DEPRECATED:
							deprecated++;
							++deprctd[MAY];
							break;
						case WG_EXTENSION:
							extended++;
							++extnd[MAY];
							break;
						case WG_PENDING:
							++pend[MAY];
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
						if (testApproval.equals(STATUS.WG_PENDING))
							readyToBeApproved.put(method.getName(), method.getDeclaringClass().getCanonicalName());
						break;
					case CLIENT_ONLY:
						clients.add(method.getName());
						break;
					case MANUAL:
						manuals.add(method.getName());
						break;
					case NOT_IMPLEMENTED:
						needCode.put(method.getName(), method.getDeclaringClass().getCanonicalName());
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
		html.write("<svg width=\"200\" height=\"200\">", NO_ESCAPE);
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"0\" style=\"fill:#a2bf2f\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"13\" fill=\"black\">WG Approved</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"20\" style=\"fill:#1cbfbb\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"33\" fill=\"black\">WG Pending</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"40\" style=\"fill:#bfa22f\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"53\" fill=\"black\">WG Extension</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"60\" style=\"fill:#606060 \"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"73\" fill=\"black\">WG Deprecated</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"80\" style=\"fill:#1bff95 \"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"93\" fill=\"black\">WG Clarification</text>", NO_ESCAPE);
		
		html.write("</svg>");

		html._span();
	}
	
	private static void writeImplmntLegend() throws IOException {
		html.write("<svg width=\"200\" height=\"200\">", NO_ESCAPE);
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"0\" style=\"fill:#0099cc\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"13\" fill=\"black\">Automated</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"20\" style=\"fill:#bf1c56\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"33\" fill=\"black\">Not Implemented</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"40\" style=\"fill:#8d1cbf\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"53\" fill=\"black\">Client Only</text>", NO_ESCAPE);
		
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"60\" style=\"fill:#3300cc\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"73\" fill=\"black\">Manual</text>", NO_ESCAPE);		
		
		html.write("</svg>");
		
		html._span();
	}

	private static void writeGraphDescription() throws IOException {
		html.h3().content("Description of the Chart Information");
		
		html.h4().content("Test Status");
		html.ul();
		html.li().b().write("WG Approved")._b().write(" - working group has approved this test case")._li();
		html.li().b().write("WG Pending")._b().write(" (default) - no official recommendation from the WG supporting the specification being tested by this test suite")._li();
		html.li().b().write("WG Extension")._b().write(" - valuable test case but not part of the WG approved set")._li();
		html.li().b().write("WG Deprecated")._b().write(" - no longer recommended by WG")._li();
		html.li().b().write("WG Clarification")._b().write(" - requires further clarification from the working group")._li();
		html._ul();
		
		html.h4().content("Test Implementation");
		html.ul();
		html.li().b().write("Automated")._b().write(" - implementation complete")._li();
		html.li().b().write("Not Implemented")._b().write(" (default) - possible to implement, just not done")._li();
		html.li().b().write("Client Only")._b().write(" - test is only client-side, this test suite doesn't test it")._li();
		html.li().b().write("Manual")._b().write(" - server test but not automated")._li();
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
