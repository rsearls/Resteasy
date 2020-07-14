package org.jboss.resteasy.test.microprofile.restclient.resource;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import org.jboss.resteasy.annotations.Stream;
import org.jboss.resteasy.test.rx.resource.Thing;
import org.reactivestreams.Publisher;
import org.jboss.resteasy.test.rx.resource.Thing;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/sseService")
public class SsePublisherMPService {

    @GET
    @Path("pubString")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Publisher<String> pubString() {
        return Flowable.fromArray("one", "two");
    }

    @GET
    @Path("flowString")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Flowable<String> flowString() {
        return Flowable.fromArray("one", "two");
    }

    @GET
    @Path("get/thing")
    @Produces(MediaType.APPLICATION_JSON)
    @Stream
    public Flowable<Thing> getThing() {
        return buildFlowableThing("x", 3);
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

    /////////////////////////////////////////////////////////////////

    @GET
    @Path("observableString")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    /**/
    public Publisher<String> observableString() {
        Observable<String> observable = buildObservableString("x", 3);
        observable.toFlowable(BackpressureStrategy.BUFFER);
       return (Publisher)observable;
    }
    /**/
    /**
    public Observable<String> observableString() {
        return buildObservableString("x", 3);
    }
    **/
    static <T> Observable<String> buildObservableString(String s, int n) {
        return Observable.create(
                new ObservableOnSubscribe<String>() {

                    @Override
                    public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                        for (int i = 0; i < n; i++) {
                            emitter.onNext(s);
                        }
                        emitter.onComplete();
                    }
                });
    }

}
