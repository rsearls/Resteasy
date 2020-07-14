package org.jboss.resteasy.test.microprofile.restclient.resource;

import io.reactivex.Flowable;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.Stream;
import org.jboss.resteasy.test.rx.resource.Thing;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RegisterRestClient(baseUri ="http://localhost:8080/sseService_service")
@Path("/sseService")
@Singleton
public interface SsePublisherMPServiceIntf {

    @GET
    @Path("pubString")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    Publisher<String> pubString();

    @GET
    @Path("get/thing")
    @Produces(MediaType.APPLICATION_JSON)
    @Stream
    Flowable<Thing> getThing();

    @GET
    @Path("observableString")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    //Observable<String> observableString();
    Publisher<String> observableString();
}
