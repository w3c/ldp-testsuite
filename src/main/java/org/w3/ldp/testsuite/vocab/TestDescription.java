package org.w3.ldp.testsuite.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
//import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class TestDescription {

	private static String td = "http://www.w3.org/2006/03/test-description#";

	public final static Property reviewStatus = property(td + "reviewStatus");
	public final static Property approved = property(td + "approved");
	public final static Property unreviewed = property(td + "unreviewed");
	public final static Property rejected = property(td + "rejected");
	
	public final static Property specificationReference = property(td + "specificationReference");
	public final static Property includesText = property(td + "includesText");
	
	public final static Resource Excerpt = resource(td + "Excerpt");

	protected static final Property property(String name) {
		return ResourceFactory.createProperty(name);
	}
	
	protected static final Resource resource(String name) {
		return ResourceFactory.createResource(name);
	}
}
