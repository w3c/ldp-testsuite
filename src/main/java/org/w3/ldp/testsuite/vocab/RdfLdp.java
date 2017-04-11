package org.w3.ldp.testsuite.vocab;

import org.apache.jena.rdf.model.Property;
//import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class RdfLdp {

	private static String rdft = "http://www.w3.org/ns/rdftest#";

	public final static Property Approval = property(rdft + "Approval");

	public final static Property approved = property(rdft + "Approved");
	public final static Property propopsed = property(rdft + "Proposed");

	protected static final Property property(String name) {
		return ResourceFactory.createProperty(name);
	}
}
