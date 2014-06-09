package org.w3.ldp.testsuite;

import java.net.URI;
import java.util.*;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.w3.ldp.testsuite.reporter.LdpEarlReporter;
import org.w3.ldp.testsuite.reporter.LdpHtmlReporter;
import org.w3.ldp.testsuite.reporter.LdpTestListener;
import org.w3.ldp.testsuite.test.LdpTest;
import org.w3.ldp.testsuite.util.CommandLineUtil;

/**
 * LDP Test Suite Command-Line Interface, a wrapper to {@link org.testng.TestNG}
 *
 * @author Sergio Fernández
 * @author Steve Speicher
 * @author Samuel Padgett
 */
public class LdpTestSuite {

    public static final String NAME = "LDP Test Suite";

    public static final String SPEC_URI = "https://dvcs.w3.org/hg/ldpwg/raw-file/default/ldp.html";

    private final TestNG testng;

    enum ContainerType {
        BASIC, DIRECT, INDIRECT
    };

    /**
     * Basic test suite initialization over a server testing basic containers
     *
     * @param server base urtl to the server
     */
    public LdpTestSuite(final String server) {
        this(ImmutableMap.of("server", server, "basic", null));
    }

    /**
     * Initialize the test suite with options as the row command-line input
     *
     * @param cmd command-line options
     */
    public LdpTestSuite(final CommandLine cmd) {
        this(CommandLineUtil.asMap(cmd));
    }

    /**
     * Initialize the test suite with a map of options
     *
     * @param options options
     */
    public LdpTestSuite(final Map<String, String> options) {

        // see: http://testng.org/doc/documentation-main.html#running-testng-programmatically
        testng = new TestNG();
        testng.setDefaultSuiteName(NAME);

        testng.addListener(new LdpTestListener());
        testng.addListener(new LdpEarlReporter());
        testng.addListener(new LdpHtmlReporter());

        // create XmlSuite instance
        XmlSuite testsuite = new XmlSuite();
        testsuite.setName(NAME);

        // provide included/excluded groups
        // TODO: dynamic groups
        testsuite.addIncludedGroup(LdpTest.MUST);
        testsuite.addIncludedGroup(LdpTest.SHOULD);
        testsuite.addIncludedGroup(LdpTest.MAY);

        // create XmlTest instance
        XmlTest test = new XmlTest(testsuite);
        test.setName("W3C Linked Data Platform Tests");

        // Add any parameters that you want to set to the Test.

        final String server;
        if (options.containsKey("server")) {
            server = options.get("server");
            try {
                URI uri = new URI(server);
                if (!"http".equals(uri.getScheme())) {
                    throw new IllegalArgumentException("non-http uri");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("ERROR: invalid server uri, "
                        + e.getLocalizedMessage());
            }
        } else {
            throw new IllegalArgumentException("ERROR: missing server uri");
        }

        // Add classes we want to test
        final List<XmlClass> classes = new ArrayList<>();

        final Map<String, String> parameters = new HashMap<>();
        final ContainerType type = getSelectedType(options);
        switch (type) {
            case BASIC:
                classes.add(new XmlClass( "org.w3.ldp.testsuite.test.BasicContainerTest"));
                parameters.put("basicContainer", server);
                break;
            case DIRECT:
                classes.add(new XmlClass( "org.w3.ldp.testsuite.test.DirectContainerTest"));
                parameters.put("directContainer", server);
                break;
            case INDIRECT:
                classes.add(new XmlClass( "org.w3.ldp.testsuite.test.IndirectContainerTest"));
                parameters.put("indirectContainer", server);
                break;
        }

        classes.add(new XmlClass("org.w3.ldp.testsuite.test.MemberResourceTest"));
        testsuite.addIncludedGroup("ldpMember");

        if (options.containsKey("non-rdf")) {
            classes.add(new XmlClass("org.w3.ldp.testsuite.test.NonRDFSourceTest"));
            testsuite.addIncludedGroup(LdpTest.NR);
        }

        test.setXmlClasses(classes);

        final List<XmlTest> tests = new ArrayList<>();
        tests.add(test);

        testsuite.setParameters(parameters);
        testsuite.setTests(tests);

        final List<XmlSuite> suites = new ArrayList<>();
        suites.add(testsuite);

        // provide our reporter and listener
        testng.setXmlSuites(suites);
    }

    public void run() {
        testng.run();
    }

    public int getStatus() {
        return testng.getStatus();
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        Options options = new Options();

        options.addOption(OptionBuilder.withLongOpt("server")
                .withDescription("server url to run the test suite")
                .hasArg().withArgName("server")
                .isRequired().create());

        OptionGroup containerType = new OptionGroup();
        containerType.addOption(OptionBuilder.withLongOpt("basic")
                .withDescription("the server url is a basic container").create());
        containerType.addOption(OptionBuilder.withLongOpt("direct")
                .withDescription("the server url is a direct container").create());
        containerType.addOption(OptionBuilder.withLongOpt("indirect")
                .withDescription("the server url is an indirect container")
                .create());
        containerType.setRequired(true);
        options.addOptionGroup(containerType);

        options.addOption(OptionBuilder.withLongOpt("non-rdf")
                .withDescription("include LDP-NR testing").create());

        options.addOption(OptionBuilder.withLongOpt("help")
                .withDescription("prints this usage help").create());

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

        // actual test suite execution
        try {
            LdpTestSuite ldpTestSuite = new LdpTestSuite(cmd);
            ldpTestSuite.run();
            System.exit(ldpTestSuite.getStatus());
        } catch (Exception e) {
            // e.printStackTrace();
            Throwable cause = ExceptionUtils.getRootCause(e);
            System.err.println("ERROR: " + (cause != null ? cause.getMessage() : e.getMessage()));
            printUsage(options);
        }

    }

    private static ContainerType getSelectedType(Map<String, String> options) {
        if (options.containsKey("direct")) {
            return ContainerType.DIRECT;
        } else if (options.containsKey("indirect")) {
            return ContainerType.INDIRECT;
        } else {
            return ContainerType.BASIC;
        }
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(new Comparator<Option>() {
            @Override
            public int compare(Option o1, Option o2) {
                if ("server".equals(o1.getLongOpt())) {
                    return -10000;
                } else if ("help".equals(o1.getLongOpt())) {
                    return 10000;
                } else {
                    return o1.getLongOpt().compareTo(o2.getLongOpt());
                }
            }
        });
        System.out.println();
        formatter.printHelp("java -jar ldp-testsuite.jar", options);
        System.out.println();
        System.exit(-1);
    }

}
