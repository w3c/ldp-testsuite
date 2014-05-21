package org.w3.ldp.testsuite.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Status {

	public static final String APPROVED = "approved";
	public static final String PENDING = "pending";
	public static final String DEPRECATED = "deprecated";
	public static final String EXTENSION = "extension";

	String status() default PENDING;

}
