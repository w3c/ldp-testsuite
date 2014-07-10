package org.w3.ldp.testsuite;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BuildProperties {
	private static final Properties properties = new Properties();

	static {
		InputStream inputStream = BuildProperties.class.getResourceAsStream("/build.properties");
		try {
			properties.load(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String getRevision() {
		return properties.getProperty("commit");
	}
}
