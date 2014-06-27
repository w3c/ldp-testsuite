package org.w3.ldp.testsuite.http;

import org.w3.ldp.testsuite.vocab.LDP;

/**
 * LDP Constants for use with the Prefer header.
 * 
 * @see <a href="https://dvcs.w3.org/hg/ldpwg/raw-file/default/ldp.html#prefer-parameters">LDP 1.0: Prefer Parameters</a>
 */
public interface LdpPreferences {
	public static final String PREFERENCE_INCLUDE = "include";
	public static final String PREFERENCE_OMIT = "omit";
	public static final String PREFER_MINIMAL_CONTAINER = LDP.NAMESPACE + "PreferMinimalContainer";
	public static final String PREFER_CONTAINMENT = LDP.NAMESPACE + "PreferContainment";
	public static final String PREFER_MEMBERSHIP = LDP.NAMESPACE + "PreferMembership";
}
