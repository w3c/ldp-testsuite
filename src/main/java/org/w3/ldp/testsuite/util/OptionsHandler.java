package org.w3.ldp.testsuite.util;

import java.util.Map;

import org.apache.commons.cli.CommandLine;

public class OptionsHandler {
	
	private final Map<String, String> options;
	private final CommandLine cmd;
	
	public OptionsHandler(final Map<String, String> options) {
		this.options = options;
		this.cmd = null;
	}
	
	public OptionsHandler(final CommandLine cmd) {
		this.cmd = cmd;
		this.options = null;
	}
	
	public boolean hasOption(String name) {
		if (options == null) {
			return cmd.hasOption(name);
		}
		
		return options.containsKey(name);
	}
	
	public String getOptionValue(String name) {
		if (options == null) {
			return cmd.getOptionValue(name);
		}
		
		return options.get(name);
	}
	
	public String[] getOptionValues(String name) {
		if (options == null) {
			return cmd.getOptionValues(name);
		}
		
		String[] values = options.get(name).split(",");
		for (int i = 0; i < values.length; i++) {
			values[i] = values[i].trim();
		}
		
		return values;
	}

}
