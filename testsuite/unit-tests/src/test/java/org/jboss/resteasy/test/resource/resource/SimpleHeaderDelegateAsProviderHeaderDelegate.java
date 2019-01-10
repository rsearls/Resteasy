package org.jboss.resteasy.test.resource.resource;

import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

public class SimpleHeaderDelegateAsProviderHeaderDelegate implements
        HeaderDelegate<SimpleHeaderDelegateAsProviderHeader> {

    @Override
    public SimpleHeaderDelegateAsProviderHeader fromString(String value) {
        throw new RuntimeException("Force error");
    }

    @Override
    public String toString(SimpleHeaderDelegateAsProviderHeader value) {
        return "toString:" + value.getMajor() + ";" + value.getMinor();
    }
}
