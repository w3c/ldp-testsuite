package org.w3.ldp.testsuite;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
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
import org.apache.commons.lang3.StringUtils;
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

import com.jayway.restassured.RestAssured;

/**
 * LDP Test Suite Command-Line Interface, a wrapper to {@link org.testng.TestNG}
 *
 * @author Sergio Fernández
 * @author Steve Speicher
 * @author Samuel Padgett
 */
public class LdpTestSuite {

	public static final String NAME = "LDP Test Suite";
	public static final String SPEC_URI = "http://www.w3.org/TR/ldp";
	public static final String OUTPUT_DIR = "report";

	static final String[] EARLDEPEDENTARGS = {"software", "developer", "language", "homepage", "assertor", "shortname"};

	private final TestNG testng;

	private final List<XmlClass> classList; // for test types to add in

	private final String reportTitle;

	private String outputDir;

	enum ContainerType {
		BASIC, DIRECT, INDIRECT
	}

	/**
	 * Initialize the test suite with options as a map
	 *
	 * @param options map of options
	 */
	public LdpTestSuite(final Map<String, String> options) {
		this(new OptionsHandler(options));
	}

	/**
	 * Initialize the test suite with options as the row command-line input
	 *
	 * @param cmd command-line options
	 */
	public LdpTestSuite(final CommandLine cmd) {
		this(new OptionsHandler(cmd));
	}

	/**
	 * Initialize the test suite with options as the row command-line input
	 *
	 * @param cmd command-line options
	 * @param reportTitle report title
	 */
	public LdpTestSuite(final CommandLine cmd, String reportTitle) {
		this(new OptionsHandler(cmd), reportTitle);
	}

	private LdpTestSuite(OptionsHandler optionsHandler) {
		this(optionsHandler, null);
	}

	private LdpTestSuite(OptionsHandler optionsHandler, String reportTitle) {
		// see: http://testng.org/doc/documentation-main.html#running-testng-programmatically
		testng = new TestNG();
		this.reportTitle = reportTitle;
		this.outputDir = OUTPUT_DIR;
		this.classList = new ArrayList<>();
		this.setupSuite(optionsHandler);
	}

	public void checkUriScheme(String uri) throws URISyntaxException {
		String scheme = new URI(uri).getScheme();
		if (!"http".equals(scheme) && !"https".equals(scheme)) {
			throw new IllegalArgumentException("non-http uri");
		}
	}

	private void setupSuite(OptionsHandler options) {

		testng.setDefaultSuiteName(NAME);

		// create XmlSuite instance
		XmlSuite testsuite = new XmlSuite();
		testsuite.setName(NAME);

		// provide included/excluded groups
		// get groups to include
		final String[] includedGroups;
		if(options.hasOption("includedGroups")) {
			includedGroups = options.getOptionValues("includedGroups");
			for(String group : includedGroups){
				testsuite.addIncludedGroup(group);
			}
		} else{
			testsuite.addIncludedGroup(LdpTest.MUST);
			testsuite.addIncludedGroup(LdpTest.SHOULD);
			testsuite.addIncludedGroup(LdpTest.MAY);
		}
		// get groups to exclude
		final String[] excludedGroups;
		if(options.hasOption("excludedGroups")){
			excludedGroups = options.getOptionValues("excludedGroups");
			for(String group : excludedGroups){
				testsuite.addExcludedGroup(group);
			}
		}

		// create XmlTest instance
		XmlTest test = new XmlTest(testsuite);
		test.setName("W3C Linked Data Platform Tests");

		// Add any parameters that you want to set to the Test.
		// Test suite parameters
		final Map<String, String> parameters = new HashMap<>();

		if (options.hasOption("output")) {
			final String output = options.getOptionValue("output");
			outputDir = output + File.separator + LdpTestSuite.OUTPUT_DIR;
			testng.setOutputDirectory(output + File.separator + TestNG.DEFAULT_OUTPUTDIR);
		}
		parameters.put("output", outputDir);

		final String server;
		if (options.hasOption("server")) {
			server = options.getOptionValue("server");
			if (StringUtils.startsWith(server, "https:")) { // allow self-signed certificates for development servers
				RestAssured.useRelaxedHTTPSValidation();
			}
			try {
				checkUriScheme(server);
			} catch (Exception e) {
				throw new IllegalArgumentException("ERROR: invalid server uri, " + e.getLocalizedMessage());
			}
		} else {
			throw new IllegalArgumentException("ERROR: missing server uri");
		}

		// Listener injection from options
		final String[] listeners;
		if (options.hasOption("listeners")) {
			listeners = options.getOptionValues("listeners");
			for (String listener : listeners) {
				try {
					Class<?> listenerCl = Class.forName(listener);
					Object instance = listenerCl.newInstance();
					testng.addListener(instance);
				} catch (ClassNotFoundException e) {
					throw new IllegalArgumentException("ERROR: invalid listener class name, " + e.getLocalizedMessage());
				} catch (InstantiationException | IllegalAccessException e) {
					throw new IllegalArgumentException("ERROR: problem while creating listener, " + e.getLocalizedMessage());
				}
			}
		}

		// Add method enabler (Annotation Transformer)
		testng.addListener(new MethodEnabler());

		testng.addListener(new LdpTestListener());
		LdpHtmlReporter reporter = new LdpHtmlReporter();
		if (StringUtils.isNotBlank(reportTitle)) {
			reporter.setTitle(reportTitle);
		}
		reporter.setOutputDirectory(outputDir);
		testng.addListener(reporter);

		if (options.hasOption("earl")) {
			LdpEarlReporter earlReport = new LdpEarlReporter();
			if (StringUtils.isNotBlank(reportTitle)) {
				earlReport.setTitle(reportTitle);
			}
			earlReport.setOutputDirectory(outputDir);
			testng.addListener(earlReport);

			// required --earl args
			for (String arg: EARLDEPEDENTARGS) {
				if (options.hasOptionWithValue(arg))
					parameters.put(arg, options.getOptionValue(arg));
				else
					printEarlUsage(arg);
			}

			// optional --earl args
			if (options.hasOptionWithValue("mbox")) {
				parameters.put("mbox", options.getOptionValue("mbox"));
			}

		}

		if (options.hasOptionWithValue("cont-res")) {
			final String containerAsResource = options.getOptionValue("cont-res");
			try {
				checkUriScheme(containerAsResource);
			} catch (Exception e) {
				throw new IllegalArgumentException("ERROR: invalid containerAsResource uri, " + e.getLocalizedMessage());
			}
			parameters.put("containerAsResource", containerAsResource);
		}

		if (options.hasOption("read-only-prop")) {
			parameters.put("readOnlyProp", options.getOptionValue("read-only-prop"));
		}

		if (options.hasOption("relative-uri")) {
			parameters.put("relativeUri", options.getOptionValue("relative-uri"));
		}

		if (options.hasOptionWithValue("auth")) {
			final String auth = options.getOptionValue("auth");
			if (auth.contains(":")) {
					String[] split = auth.split(":");
					if (split.length == 2 && StringUtils.isNotBlank(split[0]) && StringUtils.isNotBlank(split[1])) {
						parameters.put("auth", auth);
				} else {
					throw new IllegalArgumentException("ERROR: invalid basic authentication credentials");
				}
			} else {
				throw new IllegalArgumentException("ERROR: invalid basic authentication credentials");
			}
		}

		final String postTtl;
		if (options.hasOption("postTtl")) {
			postTtl = options.getOptionValue("postTtl");
			parameters.put("postTtl", postTtl);
		}

		final String memberTtl;
		if (options.hasOption("memberTtl")) {
			memberTtl = options.getOptionValue("memberTtl");
			parameters.put("memberTtl", memberTtl);
		}

		final String memberResource;
		if (options.hasOption("memberResource")) {
			memberResource = options.getOptionValue("memberResource");
			parameters.put("memberResource", memberResource);
		}

		ContainerType type = getSelectedType(options);
		switch (type) {
			case BASIC:
				classList.add(new XmlClass("org.w3.ldp.testsuite.test.BasicContainerTest"));
				parameters.put("basicContainer", server);
				break;
			case DIRECT:
				classList.add(new XmlClass("org.w3.ldp.testsuite.test.DirectContainerTest"));
				parameters.put("directContainer", server);
				break;
			case INDIRECT:
				classList.add(new XmlClass("org.w3.ldp.testsuite.test.IndirectContainerTest"));
				parameters.put("indirectContainer", server);
				break;
		}

		classList.add(new XmlClass("org.w3.ldp.testsuite.test.MemberResourceTest"));
		testsuite.addIncludedGroup("ldpMember");

		if (options.hasOption("non-rdf")) {
			classList.add(new XmlClass("org.w3.ldp.testsuite.test.NonRDFSourceTest"));
		}

		if (options.hasOption("httpLogging")) {
			parameters.put("httpLogging", "true");
		}

		if (options.hasOption("skipLogging")) {
			parameters.put("skipLogging", "true");
		}

		test.setXmlClasses(classList);

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

	private ContainerType getSelectedType(OptionsHandler options) {
		if (options.hasOption("direct")) {
			return ContainerType.DIRECT;
		} else if (options.hasOption("indirect")) {
			return ContainerType.INDIRECT;
		} else {
			return ContainerType.BASIC;
		}
	}

	public void addTestClass(String klass) {
		addTestClass(new XmlClass(klass));
	}

	public void addTestClass(XmlClass klass) {
		this.classList.add(klass);
	}

	public void addTestClasses(Collection<XmlClass> classes) {
		this.classList.addAll(classes);
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

	public String getOutputDir() {
		return outputDir;
	}

	public static CommandLine getCommandLine(Options options, String[] args){
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
		return cmd;
	}

	public static void executeTestSuite(String[] args, Options options, String reportTitle) {
		executeTestSuite(args, options, reportTitle, Collections.<XmlClass>emptyList());
	}
	public static void executeTestSuite(String[] args, Options options, String reportTitle, List<XmlClass> classes) {
		// actual test suite execution
		try {
			CommandLine cmd = LdpTestSuite.getCommandLine(options, args);
			LdpTestSuite ldpTestSuite = new LdpTestSuite(cmd, reportTitle);
			ldpTestSuite.addTestClasses(classes);
			ldpTestSuite.run();
			System.exit(ldpTestSuite.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
			Throwable cause = ExceptionUtils.getRootCause(e);
			System.err.println("ERROR: " + (cause != null ? cause.getMessage() : e.getMessage()));
			printUsage(options);
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

	private static void printEarlUsage(String missingArg) {
		System.out.println("--earl missing arg: "+missingArg);
		System.out.println("Required additional args:");
		for (String arg: EARLDEPEDENTARGS) {
			System.out.println("\t--"+arg);
		}
		System.exit(1);
	}

	@SuppressWarnings("static-access")
	public static OptionGroup addCommonOptions() {
		OptionGroup common = new OptionGroup();
		common.addOption(OptionBuilder.withLongOpt("server")
				.withDescription("server url to run the test suite").hasArg()
				.withArgName("server").isRequired().create());

		common.addOption(OptionBuilder.withLongOpt("auth")
				.withDescription("server basic authentication credentials following the syntax username:password").hasArg()
				.withArgName("username:password").create());

		common.addOption(OptionBuilder.withLongOpt("earl")
				.withDescription("General EARL ttl file").withArgName("earl")
				.isRequired(false).create());

		common.addOption(OptionBuilder.withLongOpt("includedGroups")
				.withDescription("test groups to run, separated by a space").hasArgs()
				.withArgName("includedGroups").isRequired(false)
				.create());

		common.addOption(OptionBuilder.withLongOpt("excludedGroups")
				.withDescription("test groups to not run, separated by a space").hasArgs()
				.withArgName("excludedGroups").isRequired(false)
				.create());

		common.addOption(OptionBuilder.withLongOpt("test")
				.withDescription("which tests to run (* is a wildcard)")
				.hasArgs().withArgName("test names")
				.create());

		common.addOption(OptionBuilder.withLongOpt("output")
				.withDescription("output directory (current directory by default)")
				.hasArgs().withArgName("output")
				.isRequired(false).create());

		common.addOption(OptionBuilder.withLongOpt("httpLogging")
				.withDescription("log HTTP requests and responses on validation failures")
				.isRequired(false).create());

		common.addOption(OptionBuilder.withLongOpt("skipLogging")
				.withDescription("log skip test messages")
				.isRequired(false).create());

		common.addOption(OptionBuilder.withLongOpt("help")
				.withDescription("prints this usage help")
				.isRequired(false).create());
		return common;
	}

	@SuppressWarnings("static-access")
	public static OptionGroup addEarlOptions() {
		OptionGroup earl = new OptionGroup();
		// --earl dependent values
		earl.addOption(OptionBuilder
				.withLongOpt("software")
				.withDescription(
						"title of the software test suite runs on: required with --earl")
				.hasArg().withArgName("software").isRequired(false).create());

		earl.addOption(OptionBuilder
				.withLongOpt("developer")
				.withDescription(
						"the name of the software developer: required with --earl")
				.hasArg().withArgName("dev-name").isRequired(false).create());

		earl.addOption(OptionBuilder
				.withLongOpt("mbox")
				.withDescription(
						"email of the sofware developer: optional with --earl")
				.hasArg().withArgName("mbox").isRequired(false).create());

		earl.addOption(OptionBuilder
				.withLongOpt("language")
				.withDescription(
						"primary programming language of the software: required with --earl")
				.hasArg().withArgName("language").isRequired(false).create());

		earl.addOption(OptionBuilder
				.withLongOpt("homepage")
				.withDescription(
						"the homepage of the suite runs against: required with --earl")
				.hasArg().withArgName("homepage").isRequired(false).create());

		earl.addOption(OptionBuilder
				.withLongOpt("assertor")
				.withDescription(
						"the URL of the person or agent that asserts the results: required with --earl")
				.hasArg().withArgName("assertor").isRequired(false).create());

		earl.addOption(OptionBuilder.withLongOpt("shortname")
				.withDescription("a simple short name: required with --earl")
				.hasArg().withArgName("shortname").isRequired(false).create());
		// end of --earl dependent values
		return earl;
	}

}
