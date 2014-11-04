package org.w3.ldp.testsuite;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class RunLdpTestSuite {

	private static Options options = new Options();
	
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(Level.OFF);

		options.addOptionGroup(LdpTestSuite.addCommonOptions());
		options.addOptionGroup(LdpTestSuite.addEarlOptions());
		addContainerOptions();

		addNonRdfOption();

		addContResOption();
		addReadOnlyOption();
		addRelativeUriOption();

		LdpTestSuite.executeTestSuite(args, options, "ldp-testsuite");
	}
	
	@SuppressWarnings("static-access")
	private static void addContainerOptions() {
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
	}
	
	@SuppressWarnings("static-access")
	private static void addReadOnlyOption() {
		options.addOption(OptionBuilder.withLongOpt("read-only-prop")
				.withDescription("a read-only property to test error conditions")
				.hasArg().withArgName("uri")
				.create());
	}

	@SuppressWarnings("static-access")
	private static void addRelativeUriOption() {
		options.addOption(OptionBuilder.withLongOpt("relative-uri")
				.withDescription("a relative uri to test relative uri resolution")
				.hasArg().withArgName("uri")
				.create());
	}

	@SuppressWarnings("static-access")
	private static void addContResOption() {
		options.addOption(OptionBuilder.withLongOpt("cont-res")
				.withDescription("url of a container with interaction model of a resource").hasArg()
				.withArgName("cont-res").create());
	}

	@SuppressWarnings("static-access")
	private static void addNonRdfOption() {
		options.addOption(OptionBuilder.withLongOpt("non-rdf")
				.withDescription("include LDP-NR testing").create());
	}

}
