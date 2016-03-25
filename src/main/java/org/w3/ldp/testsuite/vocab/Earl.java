package org.w3.ldp.testsuite.vocab;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.openrdf.model.vocabulary.EARL;

public class Earl {

	/* List of Earl Resources */
	public final static Resource TestResult = resource(EARL.TESTRESULT
			.toString());
	public final static Resource TestCase = resource(EARL.NAMESPACE
			+ "TestCase");

	public final static Resource TestSubject = resource(EARL.TEST_SUBJECT
			.toString());
	public final static Resource Assertion = resource(EARL.ASSERTION.toString());

	public final static Resource Assertor = resource(EARL.ASSERTOR.toString());

	public final static Resource Software = resource(EARL.SOFTWARE.toString());

	/* List of Earl Properties */
	public final static Property automatic = property(EARL.AUTOMATIC.toString());
	public final static Property manual = property(EARL.MANUAL.toString());
	public final static Property notTested = property(EARL.NOTTESTED.toString());

	public final static Property testResult = property(EARL.RESULT.toString());
	public final static Property testSubject = property(EARL.SUBJECT.toString());

	public final static Property outcome = property(EARL.OUTCOME.toString());

	public final static Property test = property(EARL.TEST.toString());

	public final static Property mode = property(EARL.MODE.toString());
	public final static Property auto = property(EARL.AUTOMATIC.toString());

	public final static Property assertedBy = property(EARL.ASSERTEDBY
			.toString());

	// Note, this string values from org.openrdf...EARL are wrong compared to
	// the spec at http://www.w3.org/TR/EARL10-Schema/#OutcomeValue
	public final static Property passed = property(EARL.NAMESPACE + "passed");
	public final static Property failed = property(EARL.NAMESPACE + "failed");
	public final static Property untested = property(EARL.NAMESPACE + "untested");
	public final static Property inapplicable = property(EARL.NAMESPACE + "inapplicable");
	public final static Property cantTess = property(EARL.NAMESPACE + "cantTell");
	
	protected static final Property property(String name) {
		return ResourceFactory.createProperty(name);
	}

	protected static final Resource resource(String name) {
		return ResourceFactory.createResource(name);
	}

}
