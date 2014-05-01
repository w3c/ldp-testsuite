package org.w3.ldp.testsuite;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.w3.ldp.testsuite.reporter.LdpTestListener;

/**
 * LDP Test Suite Command-Line Interface, a wrapper
 * to {@link org.testng.TestNG}
 *
 * @author Sergio Fernández
 * @author Steve Speicher
 * @author Samuel Padgett
 */
public class LdpTestSuite {

    public static final String NAME = "LDP Test Suite";

    private final TestNG testng;

    enum ContainerType { BASIC, DIRECT, INDIRECT };

    public LdpTestSuite(String server, ContainerType type) {
        //see: http://testng.org/doc/documentation-main.html#running-testng-programmatically

        testng = new TestNG();

        TestListenerAdapter tla = new LdpTestListener();
        testng.addListener(tla);

        // create XmlSuite instance
        XmlSuite testsuite = new XmlSuite();
        testsuite.setName(NAME);

        // provide included/excluded groups
        //TODO: dynamic groups
        testsuite.addIncludedGroup("MUST");
        testsuite.addIncludedGroup("SHOULD");
        testsuite.addIncludedGroup("MAY");
        testsuite.addIncludedGroup("ldpMember");

        // create XmlTest instance
        XmlTest test = new XmlTest(testsuite);
        test.setName("W3C Linked Data Platform Tests");

        // Add any parameters that you want to set to the Test.

        // Add classes we want to test
        List<XmlClass> classes = new ArrayList<XmlClass>();

        Map<String, String> parameters = new HashMap<>();
        switch (type) {
            case BASIC:
                classes.add(new XmlClass("org.w3.ldp.testsuite.test.BasicContainerTest"));
                parameters.put("basicContainer", server);
                break;
            case DIRECT:
                classes.add(new XmlClass("org.w3.ldp.testsuite.test.DirectContainerTest"));
                parameters.put("directContainer", server);
                break;
            case INDIRECT:
                classes.add(new XmlClass("org.w3.ldp.testsuite.test.IndirectContainerTest"));
                parameters.put("indirectContainer", server);
                break;
        }
        classes.add(new XmlClass("org.w3.ldp.testsuite.test.MemberResourceTest"));

        test.setXmlClasses(classes);

        List<XmlTest> tests = new ArrayList<XmlTest>();
        tests.add(test);

        testsuite.setParameters(parameters);
        testsuite.setTests(tests);

        List<XmlSuite> suites = new ArrayList<XmlSuite>();
        suites.add(testsuite);

        // provide our reporter and listener
        testng.setXmlSuites(suites);
    }

    public void run() {
        testng.run();
    }

    private int getStatus() {
        return testng.getStatus();
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("server")
                .withDescription("server url to run the test suite")
                .hasArg()
                .isRequired()
                .create());
        options.addOption(OptionBuilder.withLongOpt("server")
                .withDescription("server url to run the test suite")
                .hasArg()
                .isRequired()
                .create());
        options.addOption(OptionBuilder.withLongOpt("help")
                .withDescription("print usage help")
                .create());

        OptionGroup containerType = new OptionGroup();
        containerType.addOption(OptionBuilder.withLongOpt("basic").withDescription("server url is a basic container").create());
        containerType.addOption(OptionBuilder.withLongOpt("direct").withDescription("server url is a direct container").create());
        containerType.addOption(OptionBuilder.withLongOpt("indirect").withDescription("server url is an indirect container").create());
        containerType.setRequired(true);

        options.addOptionGroup(containerType);

        CommandLineParser parser = new BasicParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("ERROR: " + e.getLocalizedMessage());
            printUsage(options);
        }

        if (cmd.hasOption("help")) {
            printUsage(options);
        }

        String server = cmd.getOptionValue("server");
        try {
            URI uri = new URI(server);
            if (!"http".equals(uri.getScheme())) {
                throw new IllegalArgumentException("non-http uri");
            }
            //TODO: check it is alive
        } catch (Exception e) {
            System.err.println("ERROR: invalid server uri, " + e.getLocalizedMessage());
            printUsage(options);
        }

        //actual test suite execution
        ContainerType type = getSelectedType(cmd);
        LdpTestSuite ldpTestSuite = new LdpTestSuite(server, type);

        try {
            ldpTestSuite.run();
        } catch (Exception e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            System.err.println("ERROR: " + cause.getMessage());
            //e.printStackTrace();
            printUsage(options);
        }

        System.exit(ldpTestSuite.getStatus());
    }

    private static ContainerType getSelectedType(CommandLine cmd) {
        if (cmd.hasOption("direct")) {
            return ContainerType.DIRECT;
        }

        if (cmd.hasOption("indirect")) {
            return ContainerType.INDIRECT;
        }

        return ContainerType.BASIC;
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        System.out.println();
        formatter.printHelp("java -jar ldp-testsuite.jar", options);
        System.out.println();
        System.exit(-1);
    }
    
}
