package org.w3.ldp.testsuite.vocab;

import org.openrdf.model.vocabulary.EARL;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class Earl {

	/* List of Earl Resources */
	public final static Resource TestResult = resource(EARL.TESTRESULT
			.toString());
	public final static Resource TestCase = resource(EARL.NAMESPACE
			+ "TestCase");
	public final static Resource TestSubject = resource(EARL.TEST_SUBJECT
			.toString());
	public final static Resource Assertor = resource(EARL.ASSERTOR.toString());
	public final static Resource Assertion = resource(EARL.ASSERTION.toString());
	public final static Resource TestMode = resource(EARL.MODE.toString());

	/* List of Earl Properties */
	public final static Property testResult = property(EARL.RESULT.toString());
	public final static Property testSubject = property(EARL.SUBJECT.toString());

	public final static Property outcome = property(EARL.OUTCOME.toString());
	public final static Property failed = property(EARL.FAIL.toString());
	public final static Property passed = property(EARL.PASS.toString());
	public final static Property skipped = property(EARL.NAMESPACE + "skip");

	public final static Property time = property(EARL.NAMESPACE + "time");
	public final static Property test = property(EARL.TEST.toString());

	public final static Property software = property(EARL.ASSERTOR.toString());

	public final static Property mode = property(EARL.MODE.toString());
	public final static Property auto = property(EARL.AUTOMATIC.toString());

	public final static Property assertedBy = property(EARL.ASSERTEDBY
			.toString());

	protected static final Property property(String name) {
		return ResourceFactory.createProperty(name);
	}

	protected static final Resource resource(String name) {
		return ResourceFactory.createResource(name);
	}

}
