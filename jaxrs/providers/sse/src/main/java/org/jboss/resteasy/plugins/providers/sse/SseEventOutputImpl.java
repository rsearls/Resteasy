package org.jboss.resteasy.plugins.providers.sse;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;

import javax.servlet.AsyncContext;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventOutput;

import org.jboss.resteasy.plugins.server.servlet.Servlet3AsyncHttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.HttpHeaderNames;


public class SseEventOutputImpl extends GenericType<OutboundSseEvent> implements SseEventOutput 
{
   private OutputStream outputStream;
   private MessageBodyWriter<OutboundSseEvent> writer = null;
   private Servlet3AsyncHttpRequest request;
   private HttpServletResponse response;
   private AsyncContext asynContext;
   private boolean closed;

   public SseEventOutputImpl(final MessageBodyWriter<OutboundSseEvent> writer) {
      Object req = ResteasyProviderFactory.getInstance().getContextData(org.jboss.resteasy.spi.HttpRequest.class);
      if (!(req instanceof Servlet3AsyncHttpRequest)) {
          throw new javax.ws.rs.ServerErrorException("Sse feature requries HttpServlet30Dispatcher", Status.INTERNAL_SERVER_ERROR);
      }
      request = (Servlet3AsyncHttpRequest)req;
      
      this.writer = writer; 
      if (!request.getAsyncContext().isSuspended()) {
         request.getAsyncContext().suspend();
      }
      response =  ResteasyProviderFactory.getInstance().getContextData(HttpServletResponse.class);
      response.setHeader(HttpHeaderNames.CONTENT_TYPE, SseContextImpl.SERVER_SENT_EVENTS);
   }
  
   @Override
   public void close() throws IOException
   {
      request.getAsyncContext().getAsyncResponse().cancel();
      closed = true;
      //TODO: look at need to do flush again?
      
   }

   @Override
   public void write(OutboundSseEvent event) throws IOException
   {
      
      writer.writeTo(event, event.getClass(), null, new Annotation [] {}, event.getMediaType(), null, response.getOutputStream());
      response.getOutputStream().flush();

   }

   @Override
   public boolean isClosed()
   {
      // TODO Auto-generated method stub
      return closed;
   }
}
