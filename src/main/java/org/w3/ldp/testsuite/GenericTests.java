package org.w3.ldp.testsuite;

import nl.javadude.assumeng.Assumes;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * A Generic Test, just to play around TestNG
 */
public class GenericTests {

    private final String server;

    @Parameters(LdpTestSuite.PARAM_SERVER)
    public GenericTests(String server) {
        this.server = server;
        //TODO: REST-assure initialization,
        //      and then move it to an abstract test
    }

    @BeforeSuite
    public void beforeSuite() throws Exception {
        System.out.println("BEFORE CLASS: " + server);
        Assumes.assumeThat("server is required", server != null);
    }

    @Test
    public void testParameter() {
        System.out.println("Test: " + server);
    }

}
