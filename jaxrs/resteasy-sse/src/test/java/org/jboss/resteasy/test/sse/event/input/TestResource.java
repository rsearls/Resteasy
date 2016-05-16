package org.jboss.resteasy.test.sse.event.input;

import java.lang.annotation.Annotation;
import java.util.Collections;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.sse.InboundSseEvent;
import org.jboss.resteasy.sse.InboundSseEventImpl;
import org.jboss.resteasy.sse.SseFeature;


@Path("eventIn")
public class TestResource {
   private static final MultivaluedHashMap<String, String> headers;
   static {
      headers = new MultivaluedHashMap<String, String>();
      headers.put("Content-Type", Collections.singletonList("text/event-stream"));
   }

   @GET
   @Produces(SseFeature.SERVER_SENT_EVENTS)
   public InboundSseEvent getMessage() {

      InboundSseEventImpl eventInput = new InboundSseEventImpl.Builder(
         new Annotation[0],
         MediaType.valueOf(SseFeature.SERVER_SENT_EVENTS),
         headers
      ).name("simple-msg-get").write("Hello SSE World".getBytes()).build();

      return eventInput;
   }


   @POST
   @Produces (SseFeature.SERVER_SENT_EVENTS)
   public InboundSseEvent getEventInForPost(String msg) {

      InboundSseEventImpl eventInput = new InboundSseEventImpl.Builder(
         new Annotation[0],
         MediaType.valueOf(SseFeature.SERVER_SENT_EVENTS),
         headers
      ).name("simple-msg-post").write(msg.getBytes()).build();

      return eventInput;
   }
}