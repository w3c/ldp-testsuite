package org.w3.ldp.testsuite.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Implementation {

	public static final String IMPLEMENTED = "implemented";
	public static final String NOT_IMPLEMENTED = "not implemented";

	public String implementation() default NOT_IMPLEMENTED;

	public boolean isTestable() default true;

	public boolean clientOnly() default false;
	
	public boolean manual() default false;
}
