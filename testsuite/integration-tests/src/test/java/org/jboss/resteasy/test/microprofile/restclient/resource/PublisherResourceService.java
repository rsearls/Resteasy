package org.jboss.resteasy.test.microprofile.restclient.resource;


import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import org.jboss.resteasy.annotations.Stream;
import org.jboss.resteasy.test.rx.resource.Bytes;
import org.jboss.resteasy.test.rx.resource.Thing;
import org.reactivestreams.Publisher;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/theService")
public class PublisherResourceService {

   @GET
   @Path("get/string")
   @Produces(MediaType.TEXT_PLAIN)
   @Stream
   public Publisher<String> get() {
      return buildFlowableString("x", 3);
   }

   @GET
   @Path("get/thing")
   @Produces(MediaType.APPLICATION_JSON)
   @Stream
   public Publisher<Thing> getThing() {
      return buildFlowableThing("x", 3);
   }

   @GET
   @Path("get/thing/list")
   @Produces(MediaType.APPLICATION_JSON)
   @Stream
   public Publisher<List<Thing>> getThingList() {
      return buildFlowableThingList("x", 2, 3);
   }

   @GET
   @Path("get/bytes")
   @Produces(MediaType.APPLICATION_OCTET_STREAM)
   @Stream
   public Publisher<byte[]> getBytes() {
      return buildFlowableBytes(3);
   }

   static <T> Flowable<String> buildFlowableString(String s, int n) {
      return Flowable.create(
         new FlowableOnSubscribe<String>() {

            @Override
            public void subscribe(FlowableEmitter<String> emitter) throws Exception {
               for (int i = 0; i < n; i++)   {
                  emitter.onNext(s);
               }
               emitter.onComplete();
            }
         },
         BackpressureStrategy.BUFFER);
   }

   static Flowable<Thing> buildFlowableThing(String s, int n) {
      return Flowable.create(
         new FlowableOnSubscribe<Thing>() {

            @Override
            public void subscribe(FlowableEmitter<Thing> emitter) throws Exception {
               for (int i = 0; i < n; i++) {
                  emitter.onNext(new Thing(s));
               }
               emitter.onComplete();
            }
         },
         BackpressureStrategy.BUFFER);
   }

   static Flowable<List<Thing>> buildFlowableThingList(String s, int listSize, int elementSize) {
      return Flowable.create(
         new FlowableOnSubscribe<List<Thing>>() {

            @Override
            public void subscribe(FlowableEmitter<List<Thing>> emitter) throws Exception {
               for (int i = 0; i < listSize; i++) {
                  List<Thing> list = new ArrayList<Thing>();
                  for (int j = 0; j < elementSize; j++) {
                     list.add(new Thing(s));
                  }
                  emitter.onNext(list);
               }
               emitter.onComplete();
            }
         },
         BackpressureStrategy.BUFFER);
   }

   static Flowable<byte[]> buildFlowableBytes(int n) {
      return Flowable.create(
         new FlowableOnSubscribe<byte[]>() {

            @Override
            public void subscribe(FlowableEmitter<byte[]> emitter) throws Exception {
               for (int i = 0; i < n; i++) {
                  emitter.onNext(Bytes.BYTES);
               }
               emitter.onComplete();
            }
         },
         BackpressureStrategy.BUFFER);
   }
}
