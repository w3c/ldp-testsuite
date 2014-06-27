package org.w3.ldp.testsuite.data;

import org.testng.annotations.DataProvider;
import org.w3.ldp.testsuite.http.MediaTypes;

public class MediaTypeDataProvider implements MediaTypes {
	public final static String NAME = "mediaTypes";

	@DataProvider(name = NAME)
	public static Object[][] createData() {
		// TODO: Make the supported media types configurable.
		return new Object[][]{{TEXT_TURTLE}, {APPLICATION_RDF_XML}};
	}
}
