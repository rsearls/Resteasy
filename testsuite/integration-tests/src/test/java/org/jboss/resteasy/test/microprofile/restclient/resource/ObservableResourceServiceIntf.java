package org.jboss.resteasy.test.microprofile.restclient.resource;

import io.reactivex.Observable;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.Stream;
import org.jboss.resteasy.test.rx.resource.Thing;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@RegisterRestClient(baseUri ="http://localhost:8080/ObservableTest")
@Path("/theService")
@Singleton
public interface ObservableResourceServiceIntf {
    @GET
    @Path("get/string")
    @Produces(MediaType.TEXT_PLAIN)
    @Stream
    Observable<String> get();

    @GET
    @Path("get/thing")
    @Produces(MediaType.APPLICATION_JSON)
    @Stream
    Observable<Thing> getThing();

    @GET
    @Path("get/thing/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Stream
    Observable<List<Thing>> getThingList();

    @GET
    @Path("get/bytes")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Stream
    Observable<byte[]> getBytes();
}
