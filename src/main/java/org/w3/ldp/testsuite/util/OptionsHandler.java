package org.w3.ldp.testsuite.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

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

	public boolean hasOptionWithValue(String name) {
		if (options == null) {
			return cmd.hasOption(name) && StringUtils.isNotBlank(cmd.getOptionValue(name));
		}

		return options.containsKey(name) && StringUtils.isNotBlank(options.get(name));
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
