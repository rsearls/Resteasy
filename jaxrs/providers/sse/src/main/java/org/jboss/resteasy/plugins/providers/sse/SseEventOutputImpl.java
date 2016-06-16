package org.jboss.resteasy.plugins.providers.sse;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventOutput;

import org.jboss.resteasy.plugins.server.servlet.Servlet3AsyncHttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.HttpHeaderNames;


public class SseEventOutputImpl extends GenericType<OutboundSseEvent> implements SseEventOutput 
{
   private MessageBodyWriter<OutboundSseEvent> writer = null;
   private Servlet3AsyncHttpRequest request;
   private HttpServletResponse response;
   private boolean closed;
   private static final byte[] END = "\r\n\r\n".getBytes();
   public SseEventOutputImpl(final MessageBodyWriter<OutboundSseEvent> writer) {
      Object req = ResteasyProviderFactory.getContextData(org.jboss.resteasy.spi.HttpRequest.class);
      if (!(req instanceof Servlet3AsyncHttpRequest)) {
          throw new javax.ws.rs.ServerErrorException("Sse feature requries HttpServlet30Dispatcher", Status.INTERNAL_SERVER_ERROR);
      }
      request = (Servlet3AsyncHttpRequest)req;
      
      this.writer = writer; 
      if (!request.getAsyncContext().isSuspended()) {
         request.getAsyncContext().suspend();
      }

      response =  ResteasyProviderFactory.getContextData(HttpServletResponse.class);
      response.setHeader(HttpHeaderNames.CONTENT_TYPE, MediaType.SERVER_SENT_EVENTS);
   }
  
   @Override
   public void close() throws IOException
   {
      //TODO：look at if this is enough
      if (request.getAsyncContext().isSuspended() && request.getAsyncContext().getAsyncResponse() != null) {
         if (request.getAsyncContext().isSuspended()) {
            request.getAsyncContext().getAsyncResponse().resume(null);
         }
      }
      closed = true;
   }

   @Override
   public void write(OutboundSseEvent event) throws IOException
   { 
      java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();      
      writer.writeTo(event, event.getClass(), null, new Annotation [] {}, event.getMediaType(), null, bout);
      response.getOutputStream().write(bout.toByteArray());
      response.getOutputStream().write(END);
      response.flushBuffer();
   }

   @Override
   public boolean isClosed()
   {
      return closed;
   }
}
