package org.jboss.resteasy.test.microprofile.restclient;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.subscribers.DefaultSubscriber;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.microprofile.client.RestClientBuilderImpl;
import org.jboss.resteasy.test.microprofile.restclient.resource.SimplestService;
import org.jboss.resteasy.test.microprofile.restclient.resource.SimplestServiceIntf;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(Arquillian.class)
@RunAsClient
public class SimplestTest {

    private static SimplestServiceIntf simplestServiceIntf;
    private static CountDownLatch latch;
    private static AtomicInteger errors;

    private static final String WAR_SERVICE = SimplestTest.class.getSimpleName();
    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = TestUtil.prepareArchive(WAR_SERVICE);
        war.addClasses(SimplestService.class);
        war.setManifest(new StringAsset("Manifest-Version: 1.0\n"
                + "Dependencies: org.jboss.resteasy.resteasy-rxjava2 services, org.reactivestreams\n"));
        return TestUtil.finishContainerPrepare(war, null, null);
    }

    private static String generateURL(String path, String deployName) {
        return PortProviderUtil.generateURL(path, deployName);
    }

    //////////////////////////////////////////////////////////////////////////////
    @BeforeClass
    public static void beforeClass() throws Exception {
        RestClientBuilderImpl builder = new RestClientBuilderImpl();
        simplestServiceIntf = builder.baseUri(URI.create(generateURL("", WAR_SERVICE)))
                .build(SimplestServiceIntf.class);
    }

    //////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("unchecked")
    @Test
    public void testGetStrings() throws Exception {
        latch = new CountDownLatch(1);
        errors = new AtomicInteger(0);
        ArrayList<String> stringEventList = new ArrayList<String>();
        List<String> xStringEventList =  new ArrayList<String>();
        xStringEventList.add("one");
        xStringEventList.add("two");
        xStringEventList.add("three");

        Publisher<String> publisher = simplestServiceIntf.getStrings();
        publisher.subscribe(new DefaultSubscriber<String>(){
            public void onNext(String var1) {
                try {
                    stringEventList.add(var1);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    onError(e);
                }
            }

            @Override
            public void onError(Throwable var1) {
                errors.incrementAndGet();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        boolean waitResult = latch.await(30, TimeUnit.SECONDS);
        Assert.assertTrue("Waiting for event to be delivered has timed out.", waitResult);
        Assert.assertEquals(0, errors.get());
    }
}
