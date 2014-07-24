package org.w3.ldp.paging.testsuite.tests;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3.ldp.testsuite.LdpTestSuite;

public class RunPagingTest {

	private static Options options = new Options();
	
	public static void main(String[] args){
		Logger.getRootLogger().setLevel(Level.OFF);
		
		options.addOptionGroup(LdpTestSuite.addCommonOptions());
		options.addOptionGroup(LdpTestSuite.addEarlOptions());	
		addPagingOption();

		LdpTestSuite.executeTestSuite(options, args);
	}
	
	@SuppressWarnings("static-access")
	protected static void addPagingOption() {
		options.addOption(OptionBuilder.withLongOpt("paging")
				.withDescription("include paging tests").create());
	}
	
}
