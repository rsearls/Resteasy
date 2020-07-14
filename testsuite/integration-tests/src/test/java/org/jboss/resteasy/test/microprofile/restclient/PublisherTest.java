package org.jboss.resteasy.test.microprofile.restclient;

import io.reactivex.FlowableSubscriber;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.microprofile.client.RestClientBuilderImpl;
import org.jboss.resteasy.test.microprofile.restclient.resource.PublisherResourceService;
import org.jboss.resteasy.test.microprofile.restclient.resource.PublisherResourceServiceIntf;
import org.jboss.resteasy.test.rx.resource.Bytes;
import org.jboss.resteasy.test.rx.resource.Thing;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import javax.ws.rs.core.GenericType;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @tpSubChapter Microprofile-rest-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 4.0
 */
@RunWith(Arquillian.class)
@RunAsClient
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PublisherTest {

   private static PublisherResourceServiceIntf publisherResourceServiceIntf;
   private static CountDownLatch latch;
   private static AtomicInteger errors;

   private static final String WAR_SERVICE = PublisherTest.class.getSimpleName();
   private static final List<String> xStringList = new ArrayList<String>();
   private static final List<String> aStringList = new ArrayList<String>();
   private static final List<Thing>  xThingList =  new ArrayList<Thing>();
   private static final List<Thing>  aThingList =  new ArrayList<Thing>();
   private static final List<List<Thing>> xThingListList = new ArrayList<List<Thing>>();
   private static final List<List<Thing>> aThingListList = new ArrayList<List<Thing>>();

   private static AtomicReference<Object> value = new AtomicReference<Object>();
   private static ArrayList<String> stringList = new ArrayList<String>();
   private static ArrayList<Thing>  thingList = new ArrayList<Thing>();
   private static ArrayList<List<?>> thingListList = new ArrayList<List<?>>();
   private static ArrayList<byte[]> bytesList = new ArrayList<byte[]>();
   private static GenericType<List<Thing>> LIST_OF_THING = new GenericType<List<Thing>>() {};

   static {
      for (int i = 0; i < 3; i++) {xStringList.add("x");}
      for (int i = 0; i < 3; i++) {aStringList.add("a");}
      for (int i = 0; i < 3; i++) {xThingList.add(new Thing("x"));}
      for (int i = 0; i < 3; i++) {aThingList.add(new Thing("a"));}
      for (int i = 0; i < 2; i++) {xThingListList.add(xThingList);}
      for (int i = 0; i < 2; i++) {aThingListList.add(aThingList);}
   }

   @Deployment
   public static Archive<?> deploy() {
      WebArchive war = TestUtil.prepareArchive(WAR_SERVICE);
      war.addClass(Thing.class);
      war.addClass(Bytes.class);
      war.addClass(PublisherResourceService.class);
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
      publisherResourceServiceIntf = builder
              .baseUri(URI.create(generateURL("", WAR_SERVICE)))
              .build(PublisherResourceServiceIntf.class);
   }

   @Before
   public void before() throws Exception {
      stringList.clear();
      thingList.clear();
      thingListList.clear();
      bytesList.clear();
      latch = new CountDownLatch(1);
      errors = new AtomicInteger(0);
      value.set(null);
   }

   //////////////////////////////////////////////////////////////////////////////

   @SuppressWarnings("unchecked")
   @Test
   public void testGet() throws Exception {
      Publisher<String> publisher = publisherResourceServiceIntf.get();
       publisher.subscribe(new MyFlowableSubscriber<String>(
               (String s) -> stringList.add(s),
               errors,
               latch));

      boolean waitResult = latch.await(30, TimeUnit.SECONDS);
      Assert.assertTrue("Waiting for event to be delivered has timed out.", waitResult);
      Assert.assertEquals(0, errors.get());
      Assert.assertEquals(xStringList, stringList);
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testGetThing() throws Exception {
      Publisher<Thing> publisher = publisherResourceServiceIntf.getThing();
      publisher.subscribe(new MyFlowableSubscriber<Thing>(
               (Thing o) -> thingList.add(o),
               errors,
               latch));

      boolean waitResult = latch.await(30, TimeUnit.SECONDS);
      Assert.assertTrue("Waiting for event to be delivered has timed out.", waitResult);
      Assert.assertEquals(0, errors.get());
      Assert.assertEquals(xThingList, thingList);
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testGetThingList() throws Exception {
       Publisher<List<Thing>> publisher = publisherResourceServiceIntf.getThingList();
       publisher.subscribe(new MyFlowableSubscriber<List<Thing>>(
               (List<?> l) -> thingListList.add(l),
               errors,
               latch));

      boolean waitResult = latch.await(30, TimeUnit.SECONDS);
      Assert.assertTrue("Waiting for event to be delivered has timed out.", waitResult);
      Assert.assertEquals(0, errors.get());
      Assert.assertEquals(xThingListList, thingListList);
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testGetBytes() throws Exception {
      Publisher<byte[]> publisher = publisherResourceServiceIntf.getBytes();
      publisher.subscribe(new MyFlowableSubscriber<byte[]>(
              (byte[] b) -> bytesList.add(b),
              errors,
              latch));

      boolean waitResult = latch.await(30, TimeUnit.SECONDS);
      Assert.assertTrue("Waiting for event to be delivered has timed out.", waitResult);
      Assert.assertEquals(0, errors.get());
      Assert.assertEquals(3, bytesList.size());
      for (byte[] b : bytesList) {
         Assert.assertTrue(Arrays.equals(Bytes.BYTES, b));
      }
   }

    /**
     * Convenience class to consolidate the common method functions.
     * @param <T>
     */
    class  MyFlowableSubscriber<T> implements FlowableSubscriber<T> {
       final Consumer<? super T> onNext;
       CountDownLatch latch;
       AtomicInteger errors;
       Subscription subscription;

      MyFlowableSubscriber(final Consumer<? super T> onNext,
                                   final AtomicInteger errors, final CountDownLatch latch) {
           this.onNext = onNext;
           this.errors = errors;
           this.latch = latch;
       }

       @Override
       public void onSubscribe(Subscription s) {
           subscription = s;
           subscription.request(Long.MAX_VALUE);
       }

       public void onNext(T var1) {
           try {
               onNext.accept(var1);
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
   }

}
