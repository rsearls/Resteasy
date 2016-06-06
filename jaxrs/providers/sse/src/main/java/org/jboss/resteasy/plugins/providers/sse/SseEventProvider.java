package org.jboss.resteasy.plugins.providers.sse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.Consumes;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventInput;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

@Provider
@Produces(
{"text/event-stream"})
@Consumes(
{"text/event-stream"})
//TODO:move writeTo to SseEventOutputImpl
public class SseEventProvider implements MessageBodyReader<SseEventInput>, MessageBodyWriter<OutboundSseEvent>
{
   public static final String SERVER_SENT_EVENTS = "text/event-stream";

   public static final MediaType SERVER_SENT_EVENTS_TYPE = MediaType.valueOf(SERVER_SENT_EVENTS);

   private static final Charset UTF8 = Charset.forName("UTF-8");

   private static final byte[] COMMENT_LEAD = ": ".getBytes(UTF8);

   private static final byte[] NAME_LEAD = "event: ".getBytes(UTF8);

   private static final byte[] ID_LEAD = "id: ".getBytes(UTF8);

   private static final byte[] RETRY_LEAD = "retry: ".getBytes(UTF8);

   private static final byte[] DATA_LEAD = "data: ".getBytes(UTF8);

   private static final byte[] EOL =
   {'\n'};

   private enum State {
      NEW_LINE, COMMENT, FIELD,
   }

   @Override
   public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return type.equals(OutboundSseEvent.class) && SERVER_SENT_EVENTS_TYPE.isCompatible(mediaType);
   }

   @Override
   public long getSize(OutboundSseEvent t, Class<?> type, Type genericType, Annotation[] annotations,
         MediaType mediaType)
   {
      return -1;
   }

   @Override
   public void writeTo(OutboundSseEvent event, Class<?> type, Type genericType, Annotation[] annotations,
         MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
         throws IOException, WebApplicationException
   {
      Charset charset = UTF8;
      if (mediaType != null && mediaType.getParameters().get(MediaType.CHARSET_PARAMETER) != null)
      {
         charset = Charset.forName(mediaType.getParameters().get(MediaType.CHARSET_PARAMETER));
      }
      if (event.getComment() != null)
      {
         for (final String comment : event.getComment().split("\n"))
         {
            entityStream.write(COMMENT_LEAD);
            entityStream.write(comment.getBytes(charset));
            entityStream.write(EOL);
         }
      }

      if (event.getType() != null)
      {
         if (event.getName() != null)
         {
            entityStream.write(NAME_LEAD);
            entityStream.write(event.getName().getBytes(charset));
            entityStream.write(EOL);
         }
         if (event.getId() != null)
         {
            entityStream.write(ID_LEAD);
            entityStream.write(event.getId().getBytes(charset));
            entityStream.write(EOL);
         }
         if (event.getReconnectDelay() > -1)
         {
            entityStream.write(RETRY_LEAD);
            entityStream.write(Long.toString(event.getReconnectDelay()).getBytes(charset));
            entityStream.write(EOL);
         }

         if (event.getData() != null)
         {
            Class<?> payloadClass = event.getType();
            Type payloadType = event.getGenericType();
            if (payloadType == null)
            {
               payloadType = payloadClass;
            }

            if (payloadType == null && payloadClass == null)
            {
               payloadType = Object.class;
               payloadClass = Object.class;
            }

            entityStream.write(DATA_LEAD);
            MessageBodyWriter writer = ResteasyProviderFactory.getInstance().getMessageBodyWriter(payloadClass,
                  payloadType, annotations, event.getMediaType());

            if (writer == null)
            {
               throw new InternalServerErrorException("No suitable message body writer for class: "
                     + payloadClass.getName());
            }

            writer.writeTo(event.getData(), payloadClass, payloadType, annotations, event.getMediaType(), httpHeaders,
                  entityStream);
            entityStream.write(EOL);
         }

      }

   }

   @Override
   public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return SERVER_SENT_EVENTS_TYPE.isCompatible(mediaType);
   }

   @Override
   public SseEventInput readFrom(Class<SseEventInput> type, Type genericType, Annotation[] annotations,
         MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException,
         WebApplicationException
   {
     return new SseEventInputImpl(annotations,mediaType, httpHeaders, entityStream);

   }

 

}
