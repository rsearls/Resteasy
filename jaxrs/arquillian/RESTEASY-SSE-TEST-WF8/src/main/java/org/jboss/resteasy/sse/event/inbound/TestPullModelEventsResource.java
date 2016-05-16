package org.jboss.resteasy.sse.event.inbound;

import java.lang.annotation.Annotation;
import java.util.Collections;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.OutboundSseEvent;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.sse.InboundSseEventImpl;
import org.jboss.resteasy.sse.OutboundSseEventImpl;
import org.jboss.resteasy.sse.SseFeature;


@Path("events")
public class TestPullModelEventsResource {

   @GET
   @Path("out")
   @Produces(SseFeature.SERVER_SENT_EVENTS)
   public OutboundSseEvent getOutbound() {

      OutboundSseEventImpl.Builder eventInput = new OutboundSseEventImpl.Builder();
         eventInput.name("simple-outbound-msg")
         .id("999")
         .comment("comment of simple-outbound-msg")
         .mediaType(MediaType.TEXT_HTML_TYPE)
         .data(new String("Hello SSE World"));

      return eventInput.build();
   }

}