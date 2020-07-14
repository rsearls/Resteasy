package org.jboss.resteasy.test.microprofile.restclient.resource;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import org.jboss.resteasy.annotations.Stream;
import org.jboss.resteasy.test.rx.resource.Bytes;
import org.jboss.resteasy.test.rx.resource.Thing;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("theService")
public class ObservableResourceService {

   @GET
   @Path("get/string")
   @Produces(MediaType.TEXT_PLAIN)
   @Stream
   public Observable<String> get() {
      return buildObservableString("x", 3);
   }

   @GET
   @Path("get/thing")
   @Produces(MediaType.APPLICATION_JSON)
   @Stream
   public Observable<Thing> getThing() {
      return buildObservableThing("x", 3);
   }

   @GET
   @Path("get/thing/list")
   @Produces(MediaType.APPLICATION_JSON)
   @Stream
   public Observable<List<Thing>> getThingList() {
      return buildObservableThingList("x", 2, 3);
   }

   @GET
   @Path("get/bytes")
   @Produces(MediaType.APPLICATION_OCTET_STREAM)
   @Stream
   public Observable<byte[]> getBytes() {
      return buildObservableBytes(3);
   }

   static <T> Observable<String> buildObservableString(String s, int n) {
      return Observable.create(
         new ObservableOnSubscribe<String>() {

            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
               for (int i = 0; i < n; i++)   {
                  emitter.onNext(s);
               }
               emitter.onComplete();
            }
         });
   }

   static Observable<Thing> buildObservableThing(String s, int n) {
      return Observable.create(
         new ObservableOnSubscribe<Thing>() {

            @Override
            public void subscribe(ObservableEmitter<Thing> emitter) throws Exception {
               for (int i = 0; i < n; i++) {
                  emitter.onNext(new Thing(s));
               }
               emitter.onComplete();
            }
         });
   }

   static Observable<List<Thing>> buildObservableThingList(String s, int listSize, int elementSize) {
      return Observable.create(
         new ObservableOnSubscribe<List<Thing>>() {

            @Override
            public void subscribe(ObservableEmitter<List<Thing>> emitter) throws Exception {
               for (int i = 0; i < listSize; i++) {
                  List<Thing> list = new ArrayList<Thing>();
                  for (int j = 0; j < elementSize; j++) {
                     list.add(new Thing(s));
                  }
                  emitter.onNext(list);
               }
               emitter.onComplete();
            }
         });
   }

   static Observable<byte[]> buildObservableBytes(int n) {
      return Observable.create(
         new ObservableOnSubscribe<byte[]>() {

            @Override
            public void subscribe(ObservableEmitter<byte[]> emitter) throws Exception {
               for (int i = 0; i < n; i++) {
                  emitter.onNext(Bytes.BYTES);
               }
               emitter.onComplete();
            }
         });
   }
}
