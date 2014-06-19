package org.w3.ldp.testsuite.http;

import org.w3.ldp.testsuite.vocab.LDP;

public interface HttpHeaders {
    public static final String ACCEPT = "Accept";
    public static final String ACCEPT_PATCH = "Accept-Patch";
    public static final String ACCEPT_POST = "Accept-Post";
    public static final String ALLOW = "Allow";
    public static final String ETAG = "ETAG";
    public static final String IF_MATCH = "If-Match";
    public static final String LINK = "Link";
    public static final String LINK_REL_TYPE = "type";
    public static final String LOCATION = "Location";
    public static final String PREFER = "Prefer";
    public static final String PREFERENCE_INCLUDE_MEMBERSHIP = "return=representation; include=\"" + LDP.NAMESPACE + "PreferMembership\"";
    public static final String PREFERNCE_APPLIED = "Preference-Applied";
    public static final String SLUG = "Slug";
}
