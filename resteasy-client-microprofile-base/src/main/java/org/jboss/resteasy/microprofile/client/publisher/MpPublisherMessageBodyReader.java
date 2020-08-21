package org.jboss.resteasy.microprofile.client.publisher;

import org.jboss.resteasy.plugins.providers.sse.MpPublisher;
import org.jboss.resteasy.plugins.providers.sse.MpTypeSafeProcessor;
import org.reactivestreams.Publisher;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.sse.InboundSseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Provider
@Consumes(MediaType.SERVER_SENT_EVENTS)
public class MpPublisherMessageBodyReader implements MessageBodyReader<Publisher<?>> {

    @Context
    protected Providers providers;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Publisher.class.isAssignableFrom(type) && MediaType.SERVER_SENT_EVENTS_TYPE.isCompatible(mediaType);
    }

    @Override
    public Publisher<?> readFrom(Class<Publisher<?>> type, Type genericType,
                                 Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
                                 InputStream entityStream) throws IOException, WebApplicationException {
        // todo get real one
        ExecutorService executor = Executors.newSingleThreadExecutor();
        MpPublisher publisher = new MpPublisher(entityStream, executor, providers,
                annotations, mediaType, httpHeaders);
        MessageBodyReader reader = providers.getMessageBodyReader(type, genericType, annotations, mediaType);   // todo rls debug
        if (genericType instanceof ParameterizedType) {
            Type typeArgument = ((ParameterizedType)genericType).getActualTypeArguments()[0];
            if (typeArgument.equals(InboundSseEvent.class)) {
                publisher.useTwo();
                return publisher;
            }

            return new MpTypeSafeProcessor<Object>(new GenericType<Object>(typeArgument), publisher);
        }

        return null;
    }
}
