package org.jboss.resteasy.sse;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventInput;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.sse.SseFeature;

/**
 * Takes a InboundSseEvent return type and provides a SseEventInput object
 * containing an InboundSseEvent
 *
 * User: rsearls
 * Date: 5/18/16
 */
@Provider
@Consumes(MediaType.SERVER_SENT_EVENTS)
public class SseEventInputMessageBodyReader implements MessageBodyReader<Object> {

   public boolean isReadable(Class<?> type, Type genericType,
                             Annotation[] annotations, MediaType mediaType) {
      // TODO SseFeature should be changed to MediaType when the jaxrs 2.1 changes become stnd.
      return SseEventInput.class.isAssignableFrom(type) && SseFeature.SERVER_SENT_EVENTS_TYPE.isCompatible(mediaType);
   }

   public SseEventInput readFrom(Class<Object> type,
                                 Type genericType,
                                 Annotation[] annotations,
                                 MediaType mediaType,
                                 MultivaluedMap<String, String> httpHeaders,
                                 InputStream entityStream) throws java.io.IOException, javax.ws.rs.WebApplicationException {

      Providers providerFactory = ResteasyProviderFactory.getContextData(Providers.class);
      MessageBodyReader mBodyReader = providerFactory.getMessageBodyReader(
         InboundSseEvent.class, null, new Annotation[0], SseFeature.SERVER_SENT_EVENTS_TYPE);

      InboundSseEvent inboundEvent = (InboundSseEvent)mBodyReader.readFrom(InboundSseEvent.class,
         null, new Annotation[0], null, new MultivaluedHashMap<String, String>(), entityStream);

      final SseEventInputImpl sseEventInput = new SseEventInputImpl(inboundEvent);
      return sseEventInput;
   }
}
