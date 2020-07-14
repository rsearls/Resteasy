package org.jboss.resteasy.test.microprofile.restclient;

import org.jboss.resteasy.test.rx.resource.Thing;
import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.resteasy.microprofile.client.RestClientBuilderImpl;
import org.jboss.resteasy.test.microprofile.restclient.resource.SsePublisherMPService;
import org.jboss.resteasy.test.microprofile.restclient.resource.SsePublisherMPServiceIntf;
import org.jboss.resteasy.test.rx.resource.Thing;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@RunWith(Arquillian.class)
@RunAsClient
public class SsePublisherMPTest {
    protected static final Logger LOG = Logger.getLogger(SsePublisherMPTest.class.getName());
    private static final String WAR_SERVICE = "sseService_service";

    @Deployment(name=WAR_SERVICE)
    public static Archive<?> serviceDeploy() {
        WebArchive war = TestUtil.prepareArchive(WAR_SERVICE);
        war.addClasses(SsePublisherMPService.class,
                Thing.class);
        war.setManifest(new StringAsset("Manifest-Version: 1.0\n"
                + "Dependencies: org.jboss.resteasy.resteasy-rxjava2 services, org.reactivestreams\n"));
        return TestUtil.finishContainerPrepare(war, null, null);
    }

    private static String generateURL(String path, String deployName) {
        return PortProviderUtil.generateURL(path, deployName);
    }

    @Ignore
    @Test
    public void observableStringTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger errors = new AtomicInteger(0);
        ArrayList<String> stringList = new ArrayList<String>();
        List<String> xStringList = new ArrayList<String>();
        xStringList.add("x");
        xStringList.add("x");
        xStringList.add("x");

        RestClientBuilderImpl builder = new RestClientBuilderImpl();
        SsePublisherMPServiceIntf ssePublisherMPServiceIntf = builder
                .baseUri(URI.create(generateURL("", WAR_SERVICE)))
                .build(SsePublisherMPServiceIntf.class);
        /**
        Observable<String> observable = ssePublisherMPServiceIntf.observableString();
        observable.subscribe(
                (String o) -> stringList.add(o),
                (Throwable t) -> errors.incrementAndGet(),
                () -> latch.countDown());
        **/
        /**/
        Publisher<String> observable = ssePublisherMPServiceIntf.observableString();
        observable.subscribe(
                new Subscriber<String>() {
                    Subscription subscription;

                    @Override
                    public void onSubscribe(Subscription s) {
                        subscription = s;
                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(String var1) {
                        stringList.add(var1);
                    }

                    @Override
                    public void onError(Throwable var1) {
                        errors.incrementAndGet();
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                }
        );
        /***/
        boolean waitResult = latch.await(30, TimeUnit.SECONDS);
        Assert.assertTrue("Waiting for event to be delivered has timed out.", waitResult);
        Assert.assertEquals(0, errors.get());
        Assert.assertEquals(xStringList, stringList);
    }

    @Ignore
    @Test
    public void pubStringTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger errors = new AtomicInteger(0);
        ArrayList<String> stringList = new ArrayList<String>();
        List<String> xStringList = new ArrayList<String>();
        xStringList.add("one");
        xStringList.add( "two");

        RestClientBuilderImpl builder = new RestClientBuilderImpl();
        SsePublisherMPServiceIntf ssePublisherMPServiceIntf = builder
                .baseUri(URI.create(generateURL("", WAR_SERVICE)))
                .build(SsePublisherMPServiceIntf.class);


        Publisher<String> publisher = ssePublisherMPServiceIntf.pubString();
        publisher.subscribe(
                new FlowableSubscriber<String>() {
                    Subscription subscription;

                    @Override
                    public void onSubscribe(Subscription s) {
                        subscription = s;
                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(String var1) {
                        stringList.add(var1);
                    }

                    @Override
                    public void onError(Throwable var1) {
                        errors.incrementAndGet();
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                }
        );

        boolean waitResult = latch.await(30, TimeUnit.SECONDS);
        Assert.assertTrue("Waiting for event to be delivered has timed out.", waitResult);
        Assert.assertEquals(0, errors.get());
        Assert.assertEquals(xStringList, stringList);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetThing() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger errors = new AtomicInteger(0);
        ArrayList<String> stringList = new ArrayList<String>();
        ArrayList<Thing>  thingList = new ArrayList<Thing>();
        List<Thing>  xThingList =  new ArrayList<Thing>();
        for (int i = 0; i < 3; i++) {xThingList.add(new Thing("x"));}

        RestClientBuilderImpl builder = new RestClientBuilderImpl();
        SsePublisherMPServiceIntf ssePublisherMPServiceIntf = builder
                .baseUri(URI.create(generateURL("", WAR_SERVICE)))
                .build(SsePublisherMPServiceIntf.class);

        Flowable<Thing> flowable = ssePublisherMPServiceIntf.getThing();
                flowable.subscribe(
                (Thing o) -> thingList.add(o),
                (Throwable t) -> errors.incrementAndGet(),
                () -> latch.countDown());
        /**
        Publisher<String> publisher = ssePublisherMPServiceIntf.getThing();
        publisher.subscribe(
                new FlowableSubscriber<String>() {
                    Subscription subscription;

                    @Override
                    public void onSubscribe(Subscription s) {
                        subscription = s;
                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(String var1) {
                        stringList.add(var1);
                    }

                    @Override
                    public void onError(Throwable var1) {
                        errors.incrementAndGet();
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                }
        );
        **/
        boolean waitResult = latch.await(30, TimeUnit.SECONDS);
        Assert.assertTrue("Waiting for event to be delivered has timed out.", waitResult);
        Assert.assertEquals(0, errors.get());
        Assert.assertEquals(xThingList, thingList);
    }

    @Ignore
    @Test
    public void flowStringTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger errors = new AtomicInteger(0);
        ArrayList<String> stringList = new ArrayList<String>();
        List<String> xStringList = new ArrayList<String>();
        xStringList.add("one");
        xStringList.add( "two");

        RestClientBuilderImpl builder = new RestClientBuilderImpl();
        SsePublisherMPServiceIntf ssePublisherMPServiceIntf = builder
                .baseUri(URI.create(generateURL("", WAR_SERVICE)))
                .build(SsePublisherMPServiceIntf.class);

      Flowable<String> flowable = (Flowable<String>)ssePublisherMPServiceIntf.pubString();
      flowable.subscribe(
                (String o) -> {
                    String subOnNext="";
                    stringList.add(o);},
                (Throwable t) -> {
                    String enter="";
                    errors.incrementAndGet();},
                () -> {
                    String cntIt="";
                    latch.countDown();}
        );

        boolean waitResult = latch.await(30, TimeUnit.SECONDS);
        Assert.assertTrue("Waiting for event to be delivered has timed out.", waitResult);
        Assert.assertEquals(0, errors.get());
        Assert.assertEquals(xStringList, stringList);
    }

    /**********
     Publisher<String> publisher = ssePublisherMPServiceIntf.pubString();
     publisher.subscribe(
     new FlowableSubscriber<String>() {
     Subscription subscription;

     @Override
     public void onSubscribe(Subscription s) {
     subscription = s;
     // subscription.request(1);
     subscription.request(Long.MAX_VALUE);
     }

     @Override
     public void onNext(String var1) {
     stringList.add(var1);
     //subscription.request(1);
     }

     @Override
     public void onError(Throwable var1) {
     errors.incrementAndGet();
     }

     @Override
     public void onComplete() {
     latch.countDown();
     }
     }
     );
     **********/
}
