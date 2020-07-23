package org.jboss.resteasy.test.microprofile.restclient.resource;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import org.reactivestreams.Publisher;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("theService")
public class SimplestService {
    @GET
    @Path("strings")
    @Produces({MediaType.SERVER_SENT_EVENTS})
    public Publisher<String> getStrings() {
        return Flowable.create(
                new FlowableOnSubscribe<String>() {
                    @Override
                    public void subscribe(FlowableEmitter<String> emitter) throws Exception {
                        emitter.onNext("one");
                        emitter.onNext("two");
                        emitter.onNext("three");
                        emitter.onComplete();
                    }
                },
                BackpressureStrategy.BUFFER);
    }
}
