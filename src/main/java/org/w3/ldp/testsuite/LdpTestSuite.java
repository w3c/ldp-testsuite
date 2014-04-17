package org.w3.ldp.testsuite;

import org.apache.commons.cli.*;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;

import java.net.URI;

/**
 * LDP Test Suite Command-Line Interface, a wrapper
 * to {@link org.testng.TestNG}
 *
 * @author Sergio Fernández
 */
public class LdpTestSuite {

    private final String server;

    private final TestNG testng;

    public LdpTestSuite(String server) {
        this.server = server;
        //see: http://testng.org/doc/documentation-main.html#running-testng-programmatically

        testng = new TestNG();
        testng.setDefaultSuiteName("LDP Test Suite");
        //TODO: dynamically set the parameter 'ldp.server'

        testng.setTestClasses(new Class[] { GenericTests.class }); //TODO
        //XmlSuite suite = new XmlSuite();
        //suite.setFileName("testng.xml");
        //suite.onParameterElement("server", server);
        //testng.setCommandLineSuite(suite);

        TestListenerAdapter tla = new TestListenerAdapter();
        testng.addListener(tla);
    }

    public void run() {
        testng.run();
    }

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("server")
                .withDescription("server url to run the test suite")
                .hasArg()
                .isRequired()
                .create());
        options.addOption(OptionBuilder.withLongOpt("help")
                .withDescription("print usage help")
                .create());

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
            if (!"http".equals(uri.getScheme())) { throw new IllegalArgumentException("non-http uri"); }
            //TODO: check it is alive
        } catch (Exception e) {
            System.err.println("ERROR: invalid server uri, " + e.getLocalizedMessage());
            printUsage(options);
        }

        //actual test suite execution
        LdpTestSuite ldpTestSuite = new LdpTestSuite(server);
        ldpTestSuite.run();

        System.exit(0);
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        System.out.println();
        formatter.printHelp("java -jar ldp-testsuite.jar", options);
        System.out.println();
        System.exit(-1);
    }

}
