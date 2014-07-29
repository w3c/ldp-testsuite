package org.w3.ldp.paging.testsuite.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3.ldp.testsuite.reporter.LdpEarlTestManifest;
import org.w3.ldp.testsuite.test.LdpTest;

public class EarlTestManifest {
	/**
	 * List of GROUPS to include in reporting
	 */
	private static final List<String> conformanceLevels = new ArrayList<String>();
	private static Map<Class<?>, String> classes = new HashMap<Class<?>, String>();
	
	public static void main(String[] args) {
		conformanceLevels.add(LdpTest.MUST);
		conformanceLevels.add(LdpTest.SHOULD);
		conformanceLevels.add(LdpTest.MAY);
		conformanceLevels.add(PagingTest.PAGING);
		
		Class<PagingTest> paging = PagingTest.class;
		classes.put(paging, "LdpPaging:Paging Specifications");
		LdpEarlTestManifest manifest = new LdpEarlTestManifest();
		manifest.setConformanceLevels(conformanceLevels);
		manifest.generate(classes, "-paging");
	}

}
