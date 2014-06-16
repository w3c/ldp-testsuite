package org.w3.ldp.testsuite.vocab;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
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

    public final static Resource Automatic = resource(EARL.AUTOMATIC.toString());
    public final static Resource Manual = resource(EARL.MANUAL.toString());
    public final static Resource NotTested = resource(EARL.NOTTESTED.toString());
    public final static Resource Software = resource(EARL.SOFTWARE.toString());

    /* List of Earl Properties */
    public final static Property testResult = property(EARL.RESULT.toString());
    public final static Property testSubject = property(EARL.SUBJECT.toString());

    public final static Property outcome = property(EARL.OUTCOME.toString());

    public final static Property test = property(EARL.TEST.toString());

    public final static Property mode = property(EARL.MODE.toString());
    public final static Property auto = property(EARL.AUTOMATIC.toString());

    public final static Property assertedBy = property(EARL.ASSERTEDBY
            .toString());

    public final static Property pass = property(EARL.PASS.toString());
    public final static Property fail = property(EARL.FAIL.toString());
    public final static Property skip = property(EARL.NAMESPACE + "untested");

    protected static final Property property(String name) {
        return ResourceFactory.createProperty(name);
    }

    protected static final Resource resource(String name) {
        return ResourceFactory.createResource(name);
    }

}
