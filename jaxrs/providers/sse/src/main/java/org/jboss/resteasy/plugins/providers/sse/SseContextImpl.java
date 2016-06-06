package org.jboss.resteasy.plugins.providers.sse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent.Builder;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseContext;
import javax.ws.rs.sse.SseEventOutput;

public class SseContextImpl implements SseContext
{
  

   public static final String SERVER_SENT_EVENTS = "text/event-stream";

   public static final MediaType SERVER_SENT_EVENTS_TYPE = MediaType.valueOf(SERVER_SENT_EVENTS);
   
   public static final String LAST_EVENT_ID_HEADER = "Last-Event-ID";

   private SseEventProvider writer = new SseEventProvider();

   public SseContextImpl()
   {
      
      
   }

   @Override
   public SseEventOutput newOutput()
   {
      return new SseEventOutputImpl(writer);
   }

   @Override
   public Builder newEvent()
   {
      return new OutboundSseEventImpl.BuilderImpl();
   }

   @Override
   public SseBroadcaster newBroadcaster()
   {
      return new SseBroadcasterImpl();
   }
}
