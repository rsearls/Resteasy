package org.jboss.resteasy.test.rest31.loadservices.resource;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;

import java.io.InputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class Rest31MessageBodyReader implements MessageBodyReader<Reader>  {
    public boolean isReadable(Class<?> type, Type genericType,
                              Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    public Reader readFrom(Class<Reader> type, Type genericType,
                      Annotation[] annotations, MediaType mediaType,
                      MultivaluedMap<String, String> httpHeaders,
                      InputStream entityStream) throws java.io.IOException, jakarta.ws.rs.WebApplicationException {
        return null;
    }
}
