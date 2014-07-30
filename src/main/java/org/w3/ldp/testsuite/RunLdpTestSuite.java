package org.w3.ldp.testsuite;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.testng.xml.XmlClass;
import org.w3.ldp.testsuite.LdpTestSuite.ContainerType;
import org.w3.ldp.testsuite.util.OptionsHandler;

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

		// Add classes we want to test
		final List<XmlClass> classes = new ArrayList<>();
		
		ArrayList<String> params = new ArrayList<>();

		ContainerType type = getSelectedType(new OptionsHandler(LdpTestSuite
				.getCommandLine(options, args, null)));
		switch (type) {
		case BASIC:
			classes.add(new XmlClass("org.w3.ldp.testsuite.test.BasicContainerTest"));
			params.add("basicContainer");
			break;
		case DIRECT:
			classes.add(new XmlClass("org.w3.ldp.testsuite.test.DirectContainerTest"));
			params.add("directContainer");
			break;
		case INDIRECT:
			classes.add(new XmlClass("org.w3.ldp.testsuite.test.IndirectContainerTest"));
			params.add("indirectContainer");
			break;
		}
		LdpTestSuite.addParameter(params);
		LdpTestSuite.executeTestSuite(LdpTestSuite.getCommandLine(options, args, classes), options, "ldp-testsuite");
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
