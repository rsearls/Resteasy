package org.jboss.resteasy.test.microprofile.restclient.resource;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.test.rx.resource.Thing;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@RegisterRestClient(baseUri ="http://localhost:8080/PublisherTest")
@Path("/theService")
@Singleton
public interface PublisherResourceServiceIntf {
    @GET
    @Path("get/string")
    @Produces(MediaType.TEXT_PLAIN)
    Publisher<String> get();

    @GET
    @Path("get/thing")
    @Produces(MediaType.APPLICATION_JSON)
    Publisher<Thing> getThing();

    @GET
    @Path("get/thing/list")
    @Produces(MediaType.APPLICATION_JSON)
    Publisher<List<Thing>> getThingList();

    @GET
    @Path("get/bytes")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Publisher<byte[]> getBytes();

}
