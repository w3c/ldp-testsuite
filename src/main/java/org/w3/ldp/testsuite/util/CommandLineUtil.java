package org.w3.ldp.testsuite.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import java.util.HashMap;
import java.util.Map;

public class CommandLineUtil {

	public static Map<String, String> asMap(CommandLine cmd) {
		Map<String, String> map = new HashMap<>();
		for (Option option : cmd.getOptions()) {
			map.put(option.getLongOpt(), option.getValue());
		}
		return map;
	}

}
