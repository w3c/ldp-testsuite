package org.w3.ldp.testsuite.reporter;

import static org.rendersnake.HtmlAttributesFactory.NO_ESCAPE;
import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.href;
import static org.rendersnake.HtmlAttributesFactory.id;
import static org.rendersnake.HtmlAttributesFactory.style;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.StringResource;
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
import org.w3.ldp.testsuite.BuildProperties;
import org.w3.ldp.testsuite.LdpTestSuite;
import org.w3.ldp.testsuite.annotations.SpecTest;
import org.w3.ldp.testsuite.annotations.SpecTest.METHOD;

/**
 * HTML reporter for the LDP test suite. Takes the results of the test methods
 * and displays the information to the user.
 */
public class LdpHtmlReporter implements IReporter {

	private int passed = 0;
	private int failed = 0;
	private int skipped = 0;
	private int mustPass = 0;
	private int mayPass = 0;
	private int shouldPass = 0;

	private int total;
	
	private int mustSkip = 0;
	private int shouldSkip = 0;
	private int maySkip = 0;

	private int mustFailed = 0;
	private int shouldFailed;
	private int mayFailed;

	private IResultMap passedTests;
	private IResultMap failedTests;
	private IResultMap skippedTests;
	
	HashMap<String, Integer> passClasses;
	HashMap<String, Integer> failClasses;
	HashMap<String, Integer> skipClasses;

	private HtmlCanvas html;
	
	private String outputName = "ldp-testsuite";

	private List<ITestNGMethod> indirect = new ArrayList<ITestNGMethod>();
	
	private static StringWriter graphs = new StringWriter();
	
	private static ArrayList<String> colors = new ArrayList<String>(Arrays.asList("#42d992", "#1cbfbb", "#1d0b4e", "#bf1c56"));

	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites,
							   String outputDirectory) {
		try {
			for (ISuite suite : suites) {
				html = new HtmlCanvas();
				html.html().head();

				writeCss();
				html.title().content(LdpTestSuite.NAME + " Report")._head()
						.body();
				html.h1().content(LdpTestSuite.NAME + " Summary");

				// Getting the results for the said suite
				Map<String, ISuiteResult> suiteResults = suite.getResults();

				for (ISuiteResult sr : suiteResults.values()) {

					ITestContext tc = sr.getTestContext();
					passedTests = tc.getPassedTests();
					failedTests = tc.getFailedTests();
					skippedTests = tc.getSkippedTests();
				}
				
				// Initialize variables for charts
				passClasses = getClasses(passedTests);
				failClasses = getClasses(failedTests);
				skipClasses = getClasses(skippedTests);
				
				html.h2().content("Overall Coverage Bar Charts");
				
				html.span(class_("chartStart"));
				html.label(class_("label")).b().write("Test Results by Specification Requirement")._b()._label();
				html.div(class_("barChart").id("overallChart1"))._div(); // svg chart
				writePassFailLegend();
				
				html._span();
				
				html.span(class_("chartStart"));
				html.label(class_("label")).b().write("Test Results by Test Class")._b()._label();
				html.div(class_("barChart").id("resourcesChart"))._div();
				writeTestClassLegend();
				html._span();
				
				html.br();
				generateOverallSummaryReport(suites, "summary");
				displayGroupsInfo(suites);
				displayMethodsSummary(suites);
				toTop();
				generateMethodDetails(suites);

				html.script().content(
						StringResource.get("/raphael/raphael-min.js"), NO_ESCAPE);
				html.script().content(
						StringResource.get("/prototype/prototype.js"), NO_ESCAPE);
				html.script().content(
						StringResource.get("/grafico/grafico-min.js"), NO_ESCAPE);
				
				writeOverallBarChart();
				writeResourcesBarChart();
		
				html.write(graphs.toString(), NO_ESCAPE);
				html._body()._html();

				// send html to a file
				createWriter("report", html.toHtml(), outputName);

				Files.copy(getClass().getResourceAsStream("/testng-reports.css"),
						new File(outputDirectory, "testng-reports.css").toPath(),
						StandardCopyOption.REPLACE_EXISTING);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setTitle(String title) {
		this.outputName = title;
	}

	private void writeCss() throws IOException {

		html.style().write(StringResource.get("reportStyle.css"), NO_ESCAPE)
				._style();
	}

	private void createWriter(String directory, String output, String title) {
		BufferedWriter writer = null;
		new File(directory).mkdirs();
		try {
			writer = new BufferedWriter(new FileWriter(directory
					+ "/" + title + "-execution-report.html"));
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

	private void generateOverallSummaryReport(List<ISuite> suites, String id)
			throws IOException {
		html.table(class_("summary"));
		Date date = new Date();
		for (ISuite suite : suites) {
			// Getting the results for the said suite
			Map<String, ISuiteResult> suiteResults = suite.getResults();

			for (ISuiteResult sr : suiteResults.values()) {

				ITestContext tc = sr.getTestContext();

				Iterator<ITestResult> passedResults = tc.getPassedTests()
						.getAllResults().iterator();
				while (passedResults.hasNext()) {
					ITestResult result = passedResults.next();

					try {
						Method m = result.getMethod().getConstructorOrMethod()
								.getMethod();
						String[] groups = m.getAnnotation(Test.class).groups();
						for (int i = 0; i < groups.length; i++) {
							if (groups[i].equals("MUST"))
								mustPass++;
							if (groups[i].equals("MAY"))
								mayPass++;
							if (groups[i].equals("SHOULD"))
								shouldPass++;

						}
					} catch (SecurityException e) {
						e.printStackTrace();
					}

				}
				Iterator<ITestResult> failedResults = tc.getFailedTests()
						.getAllResults().iterator();
				while (failedResults.hasNext()) {
					ITestResult result = failedResults.next();

					try {
						Method m = result.getMethod().getConstructorOrMethod()
								.getMethod();
						String[] groups = m.getAnnotation(Test.class).groups();
						for (int i = 0; i < groups.length; i++) {
							if (groups[i].equals("MUST"))
								mustFailed++;
							if (groups[i].equals("SHOULD"))
								shouldFailed++;
							if (groups[i].equals("MAY"))
								mayFailed++;
						}
					} catch (SecurityException e) {
						e.printStackTrace();
					}

				}
				Iterator<ITestResult> skippedResults = tc.getSkippedTests()
						.getAllResults().iterator();
				while (skippedResults.hasNext()) {
					ITestResult result = skippedResults.next();

					try {
						Method m = result.getMethod().getConstructorOrMethod()
								.getMethod();
						String[] groups = m.getAnnotation(Test.class).groups();
						for (int i = 0; i < groups.length; i++) {
							if (groups[i].equals("MUST"))
								mustSkip++;
							if (groups[i].equals("MAY"))
								maySkip++;
							if (groups[i].equals("SHOULD"))
								shouldSkip++;

						}
					} catch (SecurityException e) {
						e.printStackTrace();
					}

				}
				passed = tc.getPassedTests().getAllResults().size();
				failed = tc.getFailedTests().getAllResults().size();
				skipped = tc.getSkippedTests().getAllResults().size();
			}
			total = passed + failed + skipped;
			generateSummaryTableStart(date, suite.getName());
			generateSummaryTable();
			html._table();
		}

	}

	private void generateSummaryTableStart(Date date, String suiteName)
			throws IOException {
		html.tr().th().content("Test Suite Name");
		html.th().content("Revision");
		html.th().content("Report Date");
		html.th().content("Skipped Tests");
		html.th().content("Passed (MUST)");
		html.th().content("Passed (MAY)");
		html.th().content("Passed (SHOULD)");
		// html.th().content("Failed (MUST)");
		html._tr();

		html.tr(class_("alt")).td().content(suiteName);
		final String commit = BuildProperties.getRevision();
		if (commit == null) {
			html.td().content("<UNKNOWN>");
		} else {
			html.td().a(href("https://github.com/w3c/ldp-testsuite/commit/" + commit)).content(commit)._td();
		}
		html.td().content(date.toString());
	}

	private void generateSummaryTable()
			throws IOException {
		printCellResult(skipped, total, "");
		printCellResult(mustPass, mustPass + mustFailed, "MUST");
		printCellResult(mayPass, mayPass + mayFailed, "SHOULD");
		printCellResult(shouldPass, shouldPass + shouldFailed, "MAY");
		html._tr();
	}

	private void printCellResult(int value, int total, String requirement) throws IOException {
		if (value == 0)
			html.td().i().write("No tests of this type called")._i()._td();
		else {
			DecimalFormat df = new DecimalFormat("##.##");
			double result = (((double) value / total) * 100);
			String finalTotal = df.format(result);
			html.td().b().write(finalTotal + "% ")._b();
			if(requirement.equals(""))
				html.write("of the total tests (");
			else 
				html.write(requirement + " Requirements (");
			
			html.b().write(value + "/" + total)._b();
			html.write(")")._td();
		}
	}

	private void displayGroupsInfo(List<ISuite> suite) throws IOException {
		for (ISuite testSuite : suite) {
			Map<String, ISuiteResult> tests = testSuite.getResults();
			for (ISuiteResult results : tests.values()) {
				ITestContext overview = results.getTestContext();
				String[] excluded = overview.getExcludedGroups();
				String[] included = overview.getIncludedGroups();
				generateList(included,
						"Included Groups for " + testSuite.getName());
				generateList(excluded,
						"Excluded Groups for " + testSuite.getName());
			}
		}
	}

	private void generateList(String[] list, String title) throws IOException {
		html.h2().write(title)._h2();
		if (list.length == 0) {
			html.div(style("padding-left:2em")).i()
					.content("No groups of this type found in the test suite")
					._div();
		} else {
			html.ul();
			for (String group : list) {
				html.li().content(group);
			}
			html._ul();
		}
	}

	private void displayMethodsSummary(List<ISuite> suites) throws IOException {
		for (ISuite suite : suites) {
			Map<String, ISuiteResult> r = suite.getResults();
			for (ISuiteResult r2 : r.values()) {
				ITestContext testContext = r2.getTestContext();
				makeMethodsList(testContext);
			}
		}
	}

	private void makeMethodsList(ITestContext testContext) throws IOException {
		IResultMap failed = testContext.getFailedTests();
		IResultMap passed = testContext.getPassedTests();
		IResultMap skipped = testContext.getSkippedTests();

		html.h1(class_("center")).content("Methods called");
		html.a(href("#Skipped")).write("Go To Skipped Tests").br()._a();
		html.a(href("#Passed")).write("Go To Passed Tests").br()._a();
		html.a(href("#Indirect")).write("Go To Indirect Tests").br()._a();
		html.br();
		
		makeMethodSummaryTable(failed, "Failed");
		html.br();
		toTop();
		html.br();
		makeMethodSummaryTable(skipped, "Skipped");
		html.br();
		toTop();
		html.br();
		makeMethodSummaryTable(passed, "Passed");
		html.br();
		toTop();
		makeIndirectSummaryTable();
		html.br();

	}

	private void makeMethodSummaryTable(IResultMap tests, String title)
			throws IOException {
		html.table(class_("indented"));
		html.tr().th(class_(title)).a(id((title))).write(title + " Test Cases")._a()._th();
		html.th(class_(title)).content("Groups");
		html.th(class_(title)).content("Test Class");
		html.th(class_(title)).content("Description of Test Method")._tr();
		for (ITestResult result : tests.getAllResults()) {
			ITestNGMethod method = result.getMethod();
			if(method.getConstructorOrMethod().getMethod().getAnnotation(SpecTest.class).testMethod().equals(METHOD.INDIRECT)){
				// do nothing, will add this in a separate table that specifically defines indirect tests
				indirect.add(method);
				
			} else {
				List<String> groups = Arrays.asList(method.getGroups());
				if (groups.contains("MUST") && result.getStatus() == ITestResult.FAILURE) {
					html.tr(class_("critical"));
				} else {
					html.tr();
				}

				html.td()
					.a(href("#" + method.getTestClass().getName() + "_"
							+ method.getMethodName()))
					.write(method.getMethodName(), NO_ESCAPE)._a()._td();
				html.td().content(Arrays.toString(method.getGroups()));
				html.td().content(method.getTestClass().getName());

				html.td().content(
					(method.getDescription() != null ? method.getDescription()
							: "No Description found"));
				html._tr();
			}
		}
		html._table();
	}
	
	private void makeIndirectSummaryTable() throws IOException {
		// TODO implement
		html.table(class_("indented"));
		html.tr().th().a(id(("Indirect"))).write("Indirect Test Cases")._a()._th();
		html.th().content("Overall Test Result");
		html.th().content("Test Class");
		html.th().content("Description of Test Method")._tr();

		for(ITestNGMethod method : indirect){
			html.tr();
			html.td()
				.a(href("#" + method.getTestClass().getName() + "_"
				+ method.getMethodName())).write(method.getMethodName(), NO_ESCAPE)._a()._td();
			ArrayList<String> result = new ArrayList<String>();
			Method m = method.getConstructorOrMethod().getMethod();
			SpecTest spec = m.getAnnotation(SpecTest.class);
			for(Class<?> className : spec.coveredByTests()){
				Method[] classMethod = className.getDeclaredMethods();
				for(Method methodName : classMethod) {
					if(methodName.getAnnotation(Test.class) != null) {
						String group = Arrays.toString(methodName.getAnnotation(Test.class).groups()); 
						for(String groupCover : spec.coveredByGroups()) {
							if(group.contains(groupCover) && !methodName.getName().contains("Conforms")) {
								result.add(findTestResult(methodName.getName()));
							}								
						}
					}
				}
			}
			if(result.size() > 0){
				if(result.contains("failed"))
					html.td(class_("Failed")).content("Failed");
				else if(result.contains("passed") && !result.contains("failed"))
					html.td(class_("Passed")).content("Passed");
				else if(result.contains("skipped") && !result.contains("failed") && !result.contains("passed"))
					html.td(class_("Skipped")).content("Skipped");
			}
			html.td().content(method.getTestClass().getName());
			html.td().content(
					(method.getDescription() != null ? method.getDescription()
							: "No Description found"));
			html._tr();
		}
		
		html._table();
	}

	private void generateMethodDetails(List<ISuite> suites) throws IOException {
		html.h1().content("Test Method Details");
		for (ISuite suite : suites) {
			Map<String, ISuiteResult> r = suite.getResults();
			for (ISuiteResult r2 : r.values()) {
				ITestContext testContext = r2.getTestContext();

				generateDetail(testContext.getFailedTests());
				generateDetail(testContext.getSkippedTests());
				generateDetail(testContext.getPassedTests());
			}
		}
	}

	private void generateDetail(IResultMap tests) throws IOException {
		for (ITestResult m : tests.getAllResults()) {
			ITestNGMethod method = m.getMethod();
			html.h2()
					.a(id(m.getTestClass().getName() + "_"
							+ method.getMethodName()))
					.write(m.getTestClass().getName() + ": "
							+ method.getMethodName())._a()._h2();
			getAdditionalInfo(m, method);
			html.p(class_("indented"))
					.b()
					.write("Description: ")
					._b()
					.write((method.getDescription() != null ? method
							.getDescription()
							: "No description for this test method found"))
					._p();
			String groups = "";
			for (String group : method.getGroups()) {
				groups += group + " ";
			}
			html.p(class_("indented")).b().write("Requirement Level: ")._b()
					.write(groups)._p();

			toTop();
		
		}	

	}

	private void getAdditionalInfo(ITestResult m, ITestNGMethod method)
			throws IOException {
		if (m.getThrowable() != null) {
			Throwable thrown = m.getThrowable();
			String exception = thrown.getClass().getName();
			if (exception.contains("Skip")) {
				createSkipExceptionTable(thrown);
			} else {
				createThrownTable(thrown);
			}
		}

		if (m.getParameters() != null && m.getParameters().length != 0) {
			Object[] params = m.getParameters();
			String parameters = "";
			for (Object p : params) {
				if (p != null)
					parameters += p.toString() + " ";
			}
			html.p(class_("indented")).b().write("Parameters: ")._b()
					.write(parameters)._p();
		}

		if (m.getMethod().getConstructorOrMethod().getMethod()
				.getAnnotation(SpecTest.class) != null) {
			SpecTest testLdp = m.getMethod().getConstructorOrMethod().getMethod()
					.getAnnotation(SpecTest.class);
			if(!testLdp.comment().equals(""))
				html.p(class_("note")).b().write("NOTE: ")._b()
					.write(testLdp.comment())._p();
			if(testLdp.steps().length != 0)
				writeSteps(testLdp.steps(), m.getName());
			if(testLdp.coveredByTests().length > 0 && testLdp.coveredByGroups().length > 0){
				html.p().b().content("Test Case is covered Indirectly by the following:")._p();
				html.ul();
				for(Class<?> className : testLdp.coveredByTests()){
					Method[] classMethod = className.getDeclaredMethods();
					for(Method methodName : classMethod) {
						if(methodName.getAnnotation(Test.class) != null) {
							String group = Arrays.toString(methodName.getAnnotation(Test.class).groups()); 
							for(String groupCover : testLdp.coveredByGroups()) {
								if(group.contains(groupCover) && !methodName.getName().contains("Conforms")) {
									String testCaseName = methodName.getDeclaringClass().getSimpleName();
									html.li().b().write(testCaseName)._b().write("::" +  methodName.getName());
									html.write(" - [Test " + findTestResult(methodName.getName()) + "]");
									html._li();
								}								
							}
						}
					}
				}
				html._ul();
			}
			html.p(class_("indented")).b().write("Reference URI: ")._b()
					.a(href(testLdp.specRefUri())).write(testLdp.specRefUri())._a()._p();
		}
		
	}

	private String  findTestResult(String methodName) {
		Iterator<ITestNGMethod> passed = passedTests.getAllMethods().iterator();
		while(passed.hasNext()){
			ITestNGMethod method = passed.next();
			if(method.getMethodName().equals(methodName)){
				return "passed";
			}
		}
		
		Iterator<ITestNGMethod> skipped = skippedTests.getAllMethods().iterator();
		while(skipped.hasNext()){
			ITestNGMethod method = skipped.next();
			if(method.getMethodName().equals(methodName)){
				return "skipped";
			}
		}
		
		Iterator<ITestNGMethod> failed = failedTests.getAllMethods().iterator();
		while(failed.hasNext()){
			ITestNGMethod method = failed.next();
			if(method.getMethodName().equals(methodName)){
				return "failed";
			}
		}
		
		return null;
		// TODO Auto-generated method stub
		
	}

	private void writeSteps(String[] steps, String title) throws IOException {
		html.p().content("How to Run " + title);
		html.ul();
		for(String step : steps)
			html.li().content(step);
		html._ul();
	}

	private void createSkipExceptionTable(Throwable thrown) throws IOException {
		html.table(class_("indented"));
		html.tr(class_("center")).th(class_("Skipped"))
				.content("[SKIPPED TEST]")._tr();
		html.tr().td().content(thrown.getMessage())._tr();
		html._table();

	}

	private void createThrownTable(Throwable thrown) throws IOException {
		html.table(class_("indented"));
		html.tr(class_("center")).th(class_("Failed")).content("[FAILED TEST]")
				._tr();
		html.tr().td(class_("throw")).content(Utils.stackTrace(thrown, false)[0])._tr();

		html._table();

	}

	private void toTop() throws IOException {
		html.p(class_("totop")).a(href("#top")).content("Back to Top")._p();
	}
	private void writeOverallBarChart() throws IOException {
		graphs.write("<script>");
		graphs.write("Event.observe(window, 'load', function() {");
		graphs.write("var summary_bar = new Grafico.StackedBarGraph($('overallChart1'),");
		graphs.write("{ passed: [ " + mustPass + ", " + shouldPass + ", "
				+ mayPass + " ],");
		graphs.write("failed: [" + mustFailed + ", " + shouldFailed + ", "
				+ mayFailed + " ], ");
		graphs.write("skipped: [" + mustSkip + ", " + shouldSkip + ", " + maySkip + " ]");
		graphs.write("},");
		graphs.write("{ labels: [ \"MUST\", \"SHOULD\", \"MAY\" ],");
		graphs.write("colors: { passed: '#a2bf2f', failed: '#a80000', skipped: '#606060' },");
		graphs.write("hover_color: \"#ccccff\",");

		graphs.write("datalabels: { passed: [ \"" + mustPass + " Passed\", \""
				+ shouldPass + " Passed\", \"" + mayPass + " Passed\"],");
		graphs.write("failed: [ \"" + mustFailed + " Failed\" , \""
				+ shouldFailed + " Failed\" , \"" + mayFailed
				+ " Failed\" ],");
		graphs.write("skipped: [ \"" + mustSkip + " Skipped\", \"" + shouldSkip
				+ " Skipped\", \"" + maySkip + " Skipped\" ]");
		graphs.write("},");

		graphs.write("}); });");
		graphs.write("</script>");
	}

	private void writeResourcesBarChart() throws IOException {

		graphs.write("<script>");
		graphs.write("Event.observe(window, 'load', function() {");
		graphs.write("var resource_bar = new Grafico.StackedBarGraph($('resourcesChart'),");
		writeChartValues(passClasses, failClasses, skipClasses);
		graphs.write("{ labels: [ \"Passed\", \"Failed\", \"Skipped\" ],");
		graphs.write("hover_color: \"#ccccff\",");
		writeChartColors(passClasses);
		writeChartLabels(passClasses, failClasses, skipClasses);
		graphs.write("}); });");
		graphs.write("</script>");
	}
	
	private void writePassFailLegend() throws IOException {
		html.write("<svg id=\"passFailLegend\" width=\"150\" height=\"250\">", NO_ESCAPE);
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"0\" style=\"fill:#a2bf2f\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"13\" fill=\"black\">Passed Tests</text>", NO_ESCAPE);
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"20\" style=\"fill:#a80000\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"33\" fill=\"black\">Failed Tests</text>", NO_ESCAPE);
		html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"40\" style=\"fill:#606060\"/>", NO_ESCAPE);
		html.write("<text x=\"20\" y=\"53\" fill=\"black\">Skipped Tests</text>", NO_ESCAPE);
		html.write("</svg>");
	}
	
	private void writeTestClassLegend() throws IOException {
		html.write("<svg id=\"testClassLegend\" width=\"175\" height=\"250\">", NO_ESCAPE);
		
		Set<String> names = passClasses.keySet();
		Iterator<String> label = names.iterator();
		int textY = 13;
		int rectY = 0;
		int getColor = 0;
		
		while (label.hasNext() && getColor != colors.size()) {
			String className = label.next();
			html.write("<rect width=\"15\" height=\"15\" x=\"0\" y=\"" + rectY
					+ "\" style=\"fill:" + colors.get(getColor) + "\"/>",
					NO_ESCAPE);
			html.write("<text x=\"20\" y=\"" + textY + "\" fill=\"black\">"
					+ className + "</text>", NO_ESCAPE);

			textY += 20;
			rectY += 20;
			getColor++;
		
		}
		html.write("</svg>");
	}
	
	private void writeChartLabels(HashMap<String, Integer> passClasses,
			HashMap<String, Integer> failClasses,
			HashMap<String, Integer> skipClasses) {
		graphs.write("datalabels: {");
		Set<String> names = passClasses.keySet();
		Iterator<String> label = names.iterator();
		while (label.hasNext()) {
			String className = label.next();
			graphs.write(className + ": [ \"" + passClasses.get(className)
					+ " " + className + "\", \"" + failClasses.get(className)
					+ " " + className + "\", \"" + skipClasses.get(className)
					+ " " + className + "\" ]");
			if (label.hasNext())
				graphs.write(",");
		}
		graphs.write("},");
	}
	
	private void writeChartColors(HashMap<String, Integer> passClasses) {
		graphs.write("colors: {");
		Set<String> names = passClasses.keySet();
		Iterator<String> label = names.iterator();
		int getColor = 0;
		while (label.hasNext() && getColor != colors.size()) {
			String className = label.next();

			if (label.hasNext())
				graphs.write(className + ": '" + colors.get(getColor) + "', ");
			else
				graphs.write(className+ ": '" + colors.get(getColor) + "' },");
			getColor++;
		}
	}

	private void writeChartValues(HashMap<String, Integer> passClasses,
			HashMap<String, Integer> failClasses,
			HashMap<String, Integer> skipClasses) {
		graphs.write("{");
		Set<String> names = passClasses.keySet();
		Iterator<String> label = names.iterator();
		while (label.hasNext()) {
			String className = label.next();

			graphs.write(className + ": [" + passClasses.get(className) + ", "
					+ failClasses.get(className) + ", "
					+ skipClasses.get(className) + " ]");
			if (label.hasNext())
				graphs.write(",");
		}
		graphs.write(" },");
	}

	private HashMap<String, Integer> getClasses(IResultMap tests) {
		HashMap<String, Integer> classes = new HashMap<String, Integer>();
		Iterator<ITestResult> results = tests.getAllResults().iterator();
		while (results.hasNext()) {
			String name = results.next().getTestClass().getName().toString();
			name = name.substring(name.lastIndexOf(".") + 1);

			if (!classes.containsKey(name))
				classes.put(name, 1);
			else
				classes.put(name, classes.get(name) + 1);
		}
		return classes;
	}

}
