package org.w3.ldp.testsuite;

import org.apache.commons.cli.*;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * LDP Test Suite Command-Line Interface
 *
 * @author Sergio Fernández
 */
public class CLI {

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("server")
                .withDescription("server url to run the test suite")
                .hasArg()
                .isRequired()
                .create());
        options.addOption(OptionBuilder.withLongOpt("help")
                .withDescription("print this help")
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

        //TODO: actual test suite execution
        System.out.println("not yet implemented");

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
