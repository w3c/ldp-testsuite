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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.rendersnake.HtmlAttributesFactory.*;

public class LdpTestCaseReporter {

    private static HtmlCanvas html;

    private static boolean initialRead;

    private static Set<String> refURI = new HashSet<String>();

    private static ArrayList<String> clients = new ArrayList<String>();
    private static ArrayList<String> manuals = new ArrayList<String>();

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

    private static Class<BasicContainerTest> bcTest = BasicContainerTest.class;
    private static Class<RdfSourceTest> rdfSourceTest = RdfSourceTest.class;
    private static Class<IndirectContainerTest> indirectContainerTest = IndirectContainerTest.class;
    private static Class<DirectContainerTest> directContianerTest = DirectContainerTest.class;
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
        acquireTestCases(directContianerTest);
    }

    private static void createSummaryReport() throws IOException {

        firstRead();

        initialRead = true;
        html.h2().content("Summary of Test Methods");
        html.table(class_("summary"));
        html.tr().th().content("Totals");
        html.th().content("Coverage");
        html.th().content("Not Implemented");
        html._tr();

        html.tr();
        html.td().b().write("" + totalTests)._b()
                .write(" Total Tests");
        html.ul().li().b().write(approved + " ")._b().write("WG Approved")._li();
        html.li().b().write(pending + " ")._b().write("Approval Pending")._li();
        html.li().b().write(extended + " ")._b().write("Extension")._li();
        html.li().b().write(deprecated + " ")._b().write("No longer valid")._li();
        html._ul();
        // TODO: Add "awaiting approval": !not-impl && pending
        // TODO: Add link to to grouping of kinds of tests (approved or not).  Perhaps a sortable table?
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
        html.li().b().write(reqImpl + " ")._b()
                .write("Requirements Automated")._li();
        html.ul();
        html.li().b().write(mustImpl + " / " + must)._b()
                .write(" MUST")._li();
        html.li().b().write(shouldImpl + " / " + should)._b()
                .write(" SHOULD")._li();
        html.li().b().write(mayImpl + " / " + may)._b()
                .write(" MAY")._li();
        html._ul();
        html._ul();

        html._td();

        // html.td().content(totalImplemented + " Tests");
        html.td();
        html.b().write((unimplemented + clientTest + manual) + " ")._b().write("of the Total Tests");
        html.ul();

        html.li().b().write(unimplemented + " ")._b()
                .write("of the Tests yet to be Coded")._li();
        /* TODO: Determine if disabled is really valuable or not
		html.li().b().write(disabled + " ")._b().write("of the Tests not enabled")._li(); */
        html.li().b().write(clientTest + " ")._b().write("of the Total are ")
                .a(href("#clientTests")).write("Client-Based Tests")._a()._li();
        html.li().b().write(manual + " ")._b()
                .write("of the Total must be ").a(href("#manualTests"))
                .write("Tested Manually")._a()._li();
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
        // FIXME: change to a table, with methods called
        html.h2().content("Implemented Test Classes");

        // html.table(class_("classes"));
        // html.tr().th().content("Test Class Name");
        // html.th(class_("none")).content("Test Cases");
        // html._tr();
        // // html.tr().td().b().write("Test Name")._b()._td();
        // // html.td().b().write("Info")._b()._td();
        // // html._tr();
        //
        // writeCaseRow(rdfSourceTest);
        // writeCaseRow(bcTest);
        // writeCaseRow(commonContainerTest);
        // writeCaseRow(commonResourceTest);
        // writeCaseRow(indirectContainerTest);
        // writeCaseRow(directContianerTest);
        // writeCaseRow(nonRdfSourceTest);
        //
        // html._table();

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
        html.li().a(href("#" + directContianerTest.getCanonicalName()))
                .content(directContianerTest.getCanonicalName())._li();
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

    private static <T> void writeCaseRow(Class<T> classType) throws IOException {
        // TODO Auto-generated method stub
        html.tr();
        html.td();
        html.a(href("#" + classType.getCanonicalName()))
                .content(classType.getCanonicalName())._td();
        writeCellInfo(classType.getDeclaredMethods());
        html._tr();
    }

    private static void writeCellInfo(Method[] declaredMethods)
            throws IOException {
        // TODO Auto-generated method stub
        // html.tr(colspan(declaredMethods.length + ""));
        // for (Method method : declaredMethods) {
        // html.tr().td().content("als")._tr();
        // html.tr().td().content("ba")._tr();
        // }
        html.td();

        for (Method method : declaredMethods) {
            if (method.isAnnotationPresent(Test.class))
                html.a(href("#" + method.getName())).write(method.getName())
                        ._a().br();
            else
                html.write(method.getName()).br();
        }

        html._td();
        // html._tr();
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
        SpecTest testLdp = null;
        Test test = null;
        if (method.getAnnotation(SpecTest.class) != null
                && method.getAnnotation(Test.class) != null) {
            testLdp = method.getAnnotation(SpecTest.class);
            test = method.getAnnotation(Test.class);

            if (!initialRead) {

                METHOD methodStatus = testLdp.testMethod();
                if (!test.enabled())
                    disabled++;
                if (methodStatus.equals(METHOD.AUTOMATED) && test.enabled())
                    automated++;
                if (methodStatus.equals(METHOD.NOT_IMPLEMENTED))
                    unimplemented++;
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

                    String group = Arrays.toString(test.groups());
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
                        break;
                    case WG_APPROVED:
                        approved++;
                        break;
                    case WG_EXTENSION:
                        extended++;
                        break;
                    case WG_DEPRECATED:
                        deprecated++;
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
