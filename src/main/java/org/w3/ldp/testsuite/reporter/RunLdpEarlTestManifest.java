package org.w3.ldp.testsuite.reporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3.ldp.testsuite.test.BasicContainerTest;
import org.w3.ldp.testsuite.test.DirectContainerTest;
import org.w3.ldp.testsuite.test.IndirectContainerTest;
import org.w3.ldp.testsuite.test.LdpTest;
import org.w3.ldp.testsuite.test.MemberResourceTest;
import org.w3.ldp.testsuite.test.NonRDFSourceTest;

public class RunLdpEarlTestManifest {
	
private static final List<String> conformanceLevels = new ArrayList<String>();
private static Map<Class<?>, String> classes = new HashMap<Class<?>, String>();
	
	public static void main(String[] args) {
		conformanceLevels.add(LdpTest.MUST);
		conformanceLevels.add(LdpTest.SHOULD);
		conformanceLevels.add(LdpTest.MAY);

		Class<BasicContainerTest> bcTest = BasicContainerTest.class;
		Class<IndirectContainerTest> indirectContainerTest = IndirectContainerTest.class;
		Class<DirectContainerTest> directContianerTest = DirectContainerTest.class;
		Class<MemberResourceTest> memberResourceTest = MemberResourceTest.class;
		Class<NonRDFSourceTest> nonRdfSourceTest = NonRDFSourceTest.class;
		
		classes.put(bcTest, "BasicContainer:LDP Basic Container tests.");
		classes.put(nonRdfSourceTest, "Non-RDFSource:LDP Non-RDF Source tests.");
		classes.put(memberResourceTest, "RDFSource:LDP RDF Source tests.");
		classes.put(directContianerTest, "DirectContainer:LDP Direct Container tests.");
		classes.put(indirectContainerTest, "IndirectContainer:LDP Indirect Container tests.");
		
		LdpEarlTestManifest manifest = new LdpEarlTestManifest();
		manifest.setConformanceLevels(conformanceLevels);
		manifest.generate(classes, ""); // gives the default filename for Earl Manifest
	}

}
