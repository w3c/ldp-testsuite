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

    private static String server = null;

    @BeforeSuite
    @Parameters({"ldp.server"})
    public void beforeClass(String server) throws Exception {
        System.out.println("BEFORE CLASS: " + server);
        this.server = server;
        Assumes.assumeThat("server is required", server != null);
        //TODO: REST-assure initialization,
        //      and then move it to an abstract test case
    }

    @Test
    public void testParameter() {
        System.out.println("Test: " + server);
    }

}
