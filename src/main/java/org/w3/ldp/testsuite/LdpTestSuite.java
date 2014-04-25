package org.w3.ldp.testsuite;

import org.apache.commons.cli.*;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.xml.XmlPackage;
import org.testng.xml.XmlSuite;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LDP Test Suite Command-Line Interface, a wrapper
 * to {@link org.testng.TestNG}
 *
 * @author Sergio Fernández
 */
public class LdpTestSuite {

    public static final String NAME = "LDP Test Suite";
    public static final String PARAM_SERVER = "ldp.server";

    private final String server;

    private final TestNG testng;

    public LdpTestSuite(String server) {
        this.server = server;
        //see: http://testng.org/doc/documentation-main.html#running-testng-programmatically

        XmlSuite suite = new XmlSuite();
        suite.setName(NAME);
        List<XmlPackage> packages = new ArrayList<>();
        packages.add(new XmlPackage(this.getClass().getPackage().getName())); //FIXME: does not work
        suite.setPackages(packages);
        //TODO: dynamic groups
        Map<String,String> parameters = new HashMap<>();
        parameters.put(PARAM_SERVER, server); //FIXME: does not work
        suite.setParameters(parameters);

        testng = new TestNG();
        testng.setCommandLineSuite(suite);
        testng.setTestClasses(new Class[] { GenericTests.class }); //TODO

        TestListenerAdapter tla = new TestListenerAdapter();
        testng.addListener(tla);
    }

    public void run() {
        testng.run();
    }

    private int getStatus() {
        return testng.getStatus();
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

        try {
            ldpTestSuite.run();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            //e.printStackTrace();
            printUsage(options);
        }
        System.exit(ldpTestSuite.getStatus());
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        System.out.println();
        formatter.printHelp("java -jar ldp-testsuite.jar", options);
        System.out.println();
        System.exit(-1);
    }

}
