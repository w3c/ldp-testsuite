package org.w3.ldp.testsuite.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Reference {

    /**
     * The URI of the reference
     */
    String uri();
}
