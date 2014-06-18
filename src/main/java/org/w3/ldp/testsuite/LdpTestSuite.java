package org.w3.ldp.testsuite;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.w3.ldp.testsuite.reporter.LdpEarlReporter;
import org.w3.ldp.testsuite.reporter.LdpHtmlReporter;
import org.w3.ldp.testsuite.reporter.LdpTestListener;
import org.w3.ldp.testsuite.test.LdpTest;
import org.w3.ldp.testsuite.transformer.MethodEnabler;
import org.w3.ldp.testsuite.util.OptionsHandler;

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
	}

	/**
	 * Initialize the test suite with options as a map
	 * 
	 * @param options
	 *      		map of options
	 */
	public LdpTestSuite(final Map<String, String> options) {
		testng = new TestNG();
		this.setupSuite(new OptionsHandler(options));
	}
	
	/**
	 * Initialize the test suite with options as the row command-line input
	 * 
	 * @param cmd
	 *            command-line options
	 */
	public LdpTestSuite(final CommandLine cmd) {
        // see: http://testng.org/doc/documentation-main.html#running-testng-programmatically
		testng = new TestNG();
		this.setupSuite(new OptionsHandler(cmd));
    }
	
	private void setupSuite(OptionsHandler options) {
		testng.setDefaultSuiteName(NAME);
		
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
        if (options.hasOption("server")) {
            server = options.getOptionValue("server");
			try {
				URI uri = new URI(server);
				if (!"http".equals(uri.getScheme())) {
					throw new IllegalArgumentException("non-http uri");
				}
			} catch (Exception e) {
				throw new IllegalArgumentException(
						"ERROR: invalid server uri, " + e.getLocalizedMessage());
			}
		} else {
			throw new IllegalArgumentException("ERROR: missing server uri");
		}

		// Listener injection from options
		final String[] listeners;
		if (options.hasOption("listeners")) {
			listeners = options.getOptionValue("listeners").split(",");

			for (String listener : listeners) {

				try {
					Class<?> listenerCl = Class.forName(listener.trim());
					Object instance = listenerCl.newInstance();
					testng.addListener(instance);
				} catch (ClassNotFoundException e) {
					throw new IllegalArgumentException(
							"ERROR: invalid listener class name, "
									+ e.getLocalizedMessage());
				} catch (InstantiationException | IllegalAccessException e) {
					throw new IllegalArgumentException(
							"ERROR: problem while creating listener, "
									+ e.getLocalizedMessage());
				}
			}
		}
		
		testng.addListener(new LdpTestListener());
		testng.addListener(new LdpEarlReporter());
		testng.addListener(new LdpHtmlReporter());
		
		// Add method enabler (Annotation Transformer)
		testng.addListener(new MethodEnabler());

		
		String softwareTitle = null;
		if (options.hasOption("software"))
			softwareTitle = options.getOptionValue("software");
		String softwareDev = null;
		if (options.hasOption("developer"))
			softwareDev = options.getOptionValue("developer");
		String language = null;
		if (options.hasOption("language"))
			language = options.getOptionValue("language");
		String homepage = null;
		if (options.hasOption("homepage"))
			homepage = options.getOptionValue("homepage");

		// Add classes we want to test
		final List<XmlClass> classes = new ArrayList<>();

		final Map<String, String> parameters = new HashMap<>();

		if (softwareTitle != null)
			parameters.put("software", softwareTitle);
		if (softwareDev != null)
			parameters.put("developer", softwareDev);
		if (language != null)
			parameters.put("language", language);
		if (homepage != null)
			parameters.put("homepage", homepage);

        final ContainerType type = getSelectedType(options);
		switch (type) {
		case BASIC:
			classes.add(new XmlClass(
					"org.w3.ldp.testsuite.test.BasicContainerTest"));
			parameters.put("basicContainer", server);
			break;
		case DIRECT:
			classes.add(new XmlClass(
					"org.w3.ldp.testsuite.test.DirectContainerTest"));
			parameters.put("directContainer", server);
			break;
		case INDIRECT:
			classes.add(new XmlClass(
					"org.w3.ldp.testsuite.test.IndirectContainerTest"));
			parameters.put("indirectContainer", server);
			break;
		}
		
		final String post;
		if (options.hasOption("post")) {
			post = options.getOptionValue("post");
			parameters.put("post", post);
		}

		final String memberResource;
		if (options.hasOption("memberResource")) {
			memberResource = options.getOptionValue("memberResource");
			parameters.put("memberResource", memberResource);
		}

		classes.add(new XmlClass("org.w3.ldp.testsuite.test.MemberResourceTest"));
		testsuite.addIncludedGroup("ldpMember");

        if (options.hasOption("non-rdf")) {
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
		testng.setXmlSuites(suites);
 
        if (options.hasOption("test")) {
            final String[] testNamePatterns = options.getOptionValues("test");
            for (int i = 0; i < testNamePatterns.length; i++) {
                // We support only * as a wildcard character to keep the command line simple.
                // Convert the wildcard pattern into a regex to use internally.
                testNamePatterns[i] = wildcardPatternToRegex(testNamePatterns[i]);
            }

            // Add a method intercepter to filter the list for matching tests.
            testng.addListener(new IMethodInterceptor() {
                @Override
                public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
                    ArrayList<IMethodInstance> toRun = new ArrayList<>();
                    for (IMethodInstance method : methods) {
                        for (String testNamePattern : testNamePatterns) {
                            if (method.getMethod().getMethodName().matches(testNamePattern)) {
                                toRun.add(method);
                            }
                        }
                    }

                    return toRun;
                }
            });
        }
	}
 
    public String wildcardPatternToRegex(String wildcardPattern) {
        // use lookarounds and zero-width matches to include the * delimeter in the result
        String[] tokens = wildcardPattern.split("(?<=\\*)|(?=\\*)");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if ("*".equals(token)) {
                builder.append(".*");
            } else if (token.length() > 0) {
                builder.append(Pattern.quote(token));
            }
        }

        return builder.toString();
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
				.withDescription("server url to run the test suite").hasArg()
				.withArgName("server").isRequired().create());

		options.addOption(OptionBuilder.withLongOpt("software")
				.withDescription("title of the software test suite runs on")
				.hasArg().withArgName("software").isRequired(false).create());

		options.addOption(OptionBuilder.withLongOpt("developer")
				.withDescription("the name of the software developer").hasArg()
				.withArgName("dev-name").isRequired(false).create());

		options.addOption(OptionBuilder
				.withLongOpt("language")
				.withDescription("primary programming language of the software")
				.hasArg().withArgName("language").isRequired(false).create());
		options.addOption(OptionBuilder.withLongOpt("homepage")
				.withDescription("the homepage of the suite runs against")
				.hasArg().withArgName("homepage").isRequired(false).create());

		OptionGroup containerType = new OptionGroup();
		containerType.addOption(OptionBuilder.withLongOpt("basic")
				.withDescription("the server url is a basic container")
				.create());
		containerType.addOption(OptionBuilder.withLongOpt("direct")
				.withDescription("the server url is a direct container")
				.create());
		containerType.addOption(OptionBuilder.withLongOpt("indirect")
				.withDescription("the server url is an indirect container")
				.create());
		containerType.setRequired(true);
		options.addOptionGroup(containerType);

		options.addOption(OptionBuilder.withLongOpt("non-rdf")
				.withDescription("include LDP-NR testing").create());

        options.addOption(OptionBuilder.withLongOpt("test")
                .withDescription("which tests to run (* is a wildcard)")
                .hasArgs().withArgName("test names")
                .create());

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
			System.err.println("ERROR: "
					+ (cause != null ? cause.getMessage() : e.getMessage()));
			printUsage(options);
		}

	}

    private static ContainerType getSelectedType(OptionsHandler options) {
        if (options.hasOption("direct")) {
			return ContainerType.DIRECT;
        } else if (options.hasOption("indirect")) {
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
