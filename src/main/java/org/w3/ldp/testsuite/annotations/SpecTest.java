package org.w3.ldp.testsuite.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SpecTest {

	/**
	 * WG_PENDING (default) - no official recommendation from the WG supporting
	 * the specification being tested by this test suite.
	 * WG_APPROVED - working group has approved this test case
	 * WG_CLARIFICATION - requires further clarification from the working group
	 * WG_DEPRECATED - no longer recommended by WG
	 * WG_EXTENSION - valuable test case but not part of the WG approved set
	 */
	public static enum STATUS {
		WG_PENDING, WG_APPROVED, WG_DEPRECATED, WG_EXTENSION, WG_CLARIFICATION
	}

	;

	/**
	 * The URI of the spec
	 */
	public String specRefUri() default "No Specification URI";

	/**
	 * The status of the test case, pending or approved
	 */
	public STATUS approval() default STATUS.WG_PENDING;

	/**
	 * Many of these values are used for reporting purposes and have these
	 * meanings:
	 * <p/>
	 * NOT_IMPLEMENTED (default) - possible to implement, just not done
	 * AUTOMATED - implementation complete
	 * MANUAL - server test but not automated
	 * CLIENT_ONLY - test is only client-side, this test suite doesn't test it.
	 */
	public static enum METHOD {
		NOT_IMPLEMENTED, AUTOMATED, MANUAL, CLIENT_ONLY
	};

	/**
	 * Whether the test case itself has been implemented or not
	 */
	public METHOD testMethod() default METHOD.NOT_IMPLEMENTED;

	/**
	 * Whether further comment that can be useful
	 */
	public String comment() default "";

}
