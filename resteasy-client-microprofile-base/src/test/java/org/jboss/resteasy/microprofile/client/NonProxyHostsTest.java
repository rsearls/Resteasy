package org.jboss.resteasy.microprofile.client;

import org.junit.Test;
import org.junit.Assert;

public class NonProxyHostsTest {
    private static final String srcHosts = "foo.*|foo*|full.foo.net|*.foo.com|foo.*.com|foo*.*bar.c*om";
    @Test
    public void startAsteriskTest () {
        RestClientBuilderImpl builder = new RestClientBuilderImpl();
        Assert.assertTrue(builder.checkHostValidity(srcHosts, "should.succeed.foo.com"));
        Assert.assertFalse(builder.checkHostValidity(srcHosts, "should.fail.foo.dom"));
    }

    @Test
    public void endAsteriskTest() {
        RestClientBuilderImpl builder = new RestClientBuilderImpl();
        Assert.assertTrue(builder.checkHostValidity(srcHosts, "foo.189-succeed"));
        Assert.assertTrue(builder.checkHostValidity(srcHosts, "foomania.should.succeed"));
        Assert.assertFalse(builder.checkHostValidity(srcHosts, "fo.omania.should.fail"));
    }

    @Test
    public void middleAsteriskTest() {
        RestClientBuilderImpl builder = new RestClientBuilderImpl();
        Assert.assertTrue(builder.checkHostValidity(srcHosts, "foo.yes.thing.com"));
        Assert.assertTrue(builder.checkHostValidity(srcHosts, "fooish.boobar.custom"));
        Assert.assertFalse(builder.checkHostValidity(srcHosts, "moo.cowbar.custom"));
    }

    @Test
    public void exactNameTest() {
        RestClientBuilderImpl builder = new RestClientBuilderImpl();
        Assert.assertTrue(builder.checkHostValidity(srcHosts, "full.foo.net"));
        Assert.assertFalse(builder.checkHostValidity(srcHosts, "full.foo.mat"));
    }

    @Test
    public void defaultNonProxyHostTest() {
        RestClientBuilderImpl builder = new RestClientBuilderImpl();
        String nonProxyHostStr = "localhost|127.*|[::1]";
        Assert.assertTrue(builder.checkHostValidity(nonProxyHostStr, "localhost"));
        Assert.assertFalse(builder.checkHostValidity(nonProxyHostStr, "localhosts"));

        Assert.assertTrue(builder.checkHostValidity(nonProxyHostStr, "127.143.44.01"));
        Assert.assertTrue(builder.checkHostValidity(nonProxyHostStr, "127.other.part"));
        Assert.assertFalse(builder.checkHostValidity(nonProxyHostStr, "1277.143.44.01"));
        Assert.assertFalse(builder.checkHostValidity(nonProxyHostStr, "12.743.44.01"));
    }
}
