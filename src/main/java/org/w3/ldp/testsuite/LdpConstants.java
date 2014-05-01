package org.w3.ldp.testsuite;

public interface LdpConstants {
    public final static String NS = "http://www.w3.org/ns/ldp#";

    /* TYPES */
    public final static String TYPE_CONTAINER = NS + "Container";
    public final static String TYPE_BASIC_CONTAINER = NS + "BasicContainer";
    public final static String TYPE_DIRECT_CONTAINER = NS + "DirectContainer";
    public final static String TYPE_INDIRECT_CONTAINER = NS + "IndirectContainer";
    public final static String TYPE_RESOURCE = NS + "Resource";

    /* PROPERTIES */
    public final static String CONTAINS = NS + "contains";
    public final static String MEMBER = NS + "member";
    public final static String HAS_MEMBER_RELATION = NS + "hasMemberRelation";
    public final static String IS_MEMBER_OF_RELATION = NS + "isMemberOfRelation";
    public final static String MEMBERSHIP_RESOURCE = NS + "membershipResource";

    /* LDP URI */
    public final static String SPEC_URI = "https://dvcs.w3.org/hg/ldpwg/raw-file/default/ldp.html";
}
