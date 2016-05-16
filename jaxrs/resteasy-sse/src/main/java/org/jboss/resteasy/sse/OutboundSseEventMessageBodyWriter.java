package org.jboss.resteasy.sse;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.sse.OutboundSseEvent;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.sse.SseFeature;

/**
 * User: rsearls
 * Date: 4/30/16
 */

@Provider
@Produces(MediaType.SERVER_SENT_EVENTS)
public class OutboundSseEventMessageBodyWriter implements MessageBodyWriter<OutboundSseEvent> {

   private static final Charset UTF8 = StandardCharsets.UTF_8;
   private static final byte[] COMMENT_LEAD = ": ".getBytes(UTF8);
   private static final byte[] NAME_LEAD = "event: ".getBytes(UTF8);
   private static final byte[] ID_LEAD = "id: ".getBytes(UTF8);
   private static final byte[] RETRY_LEAD = "retry: ".getBytes(UTF8);
   private static final byte[] DATA_LEAD = "data: ".getBytes(UTF8);
   private static final byte[] MEDIATYPE_LEAD = "mediatype: ".getBytes(UTF8);
   private static final byte[] EOL = {'\n'};

   @Override
   public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
                              final MediaType mediaType) {
      // TODO SseFeature should be changed to MediaType when the jaxrs 2.1 changes become stnd.
      return OutboundSseEvent.class.isAssignableFrom(type);
   }

   @Override
   public long getSize(final OutboundSseEvent incomingEvent,
                       final Class<?> type,
                       final Type genericType,
                       final Annotation[] annotations,
                       final MediaType mediaType) {

      return -1;
   }

   @Override
   @SuppressWarnings("unchecked")
   public void writeTo(final OutboundSseEvent outboundSseEvent,
                       final Class<?> type,
                       final Type genericType,
                       final Annotation[] annotations,
                       final MediaType mediaType,
                       final MultivaluedMap<String, Object> httpHeaders,
                       final OutputStream entityStream) throws IOException, WebApplicationException {

      final Charset charset = getCharset(mediaType);
      if (outboundSseEvent.getComment() != null) {
         for (final String comment : outboundSseEvent.getComment().split("\n")) {
            entityStream.write(COMMENT_LEAD);
            entityStream.write(comment.getBytes(charset));
            entityStream.write(EOL);
         }
      }

      if (outboundSseEvent.getType() != null) {
         if (outboundSseEvent.getName() != null) {
            entityStream.write(NAME_LEAD);
            entityStream.write(outboundSseEvent.getName().getBytes(charset));
            entityStream.write(EOL);
         }
         if (outboundSseEvent.getId() != null) {
            entityStream.write(ID_LEAD);
            entityStream.write(outboundSseEvent.getId().getBytes(charset));
            entityStream.write(EOL);
         }
         if (outboundSseEvent.getReconnectDelay() > SseFeature.RECONNECT_NOT_SET) {
            entityStream.write(RETRY_LEAD);
            entityStream.write(Long.toString(outboundSseEvent.getReconnectDelay()).getBytes(charset));
            entityStream.write(EOL);
         }

         final MediaType eventMediaType =
            outboundSseEvent.getMediaType() == null ? MediaType.TEXT_PLAIN_TYPE : outboundSseEvent.getMediaType();
         /***
         // rls test start
         entityStream.write(MEDIATYPE_LEAD);
         entityStream.write(eventMediaType.toString().getBytes(charset));
         entityStream.write(EOL);
         // rls test end
         **/
         Providers providerFactory = ResteasyProviderFactory.getContextData(Providers.class);
         final MessageBodyWriter messageBodyWriter = providerFactory.getMessageBodyWriter(outboundSseEvent.getType(),
            outboundSseEvent.getGenericType(), annotations, eventMediaType);
         messageBodyWriter.writeTo(
            outboundSseEvent.getData(),
            outboundSseEvent.getType(),
            outboundSseEvent.getGenericType(),
            annotations,
            eventMediaType,
            httpHeaders,
            new OutputStream() {

               private boolean start = true;

               @Override
               public void write(final int i) throws IOException {

                  if (start) {
                     entityStream.write(DATA_LEAD);
                     start = false;
                  }
                  entityStream.write(i);
                  if (i == '\n') {
                     entityStream.write(DATA_LEAD);
                  }
               }
            });
         /**/
         entityStream.write(EOL);
      }
   }

   private Charset getCharset(MediaType m) {

      String name = (m == null) ? null : m.getParameters().get(MediaType.CHARSET_PARAMETER);
      return (name == null) ? UTF8 : Charset.forName(name);
   }
}