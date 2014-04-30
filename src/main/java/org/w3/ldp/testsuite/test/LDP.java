package org.w3.ldp.testsuite.test;

import org.w3.ldp.testsuite.LdpConstants;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class LDP {
	
	public static final Property contains = property(LdpConstants.CONTAINS);
	public static final Property member = property(LdpConstants.MEMBER);
	public static final Property membershipResource = property(LdpConstants.MEMBERSHIP_RESOURCE);
	public static final Property hasMemberRelation = property(LdpConstants.HAS_MEMBER_RELATION);
	public static final Property isMemberOfRelation = property(LdpConstants.IS_MEMBER_OF_RELATION);
	
	public static final Resource Container = resource(LdpConstants.TYPE_CONTAINER);
	public static final Resource BasicContainer = resource(LdpConstants.TYPE_BASIC_CONTAINER);
	public static final Resource DirectContainer = resource(LdpConstants.TYPE_DIRECT_CONTAINER);
	public static final Resource IndirectContainer = resource(LdpConstants.TYPE_INDIRECT_CONTAINER);

    protected static final Resource resource(String name)
    {
    	return ResourceFactory.createResource(name); 
    }

    protected static final Property property(String name)
    { 
    	return ResourceFactory.createProperty(name);
    }

}
