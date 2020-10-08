package org.jboss.resteasy.microprofile.client.publisher;

import org.jboss.resteasy.plugins.providers.sse.SseConstants;
import org.jboss.resteasy.plugins.providers.sse.SseEventInputImpl;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.sse.InboundSseEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Provider
public class InboundSseEventProvider implements MessageBodyReader<InboundSseEvent>,
        MessageBodyWriter<InboundSseEvent> {
    @Override
    public boolean isReadable(Class<?>type, Type genericType, Annotation[] annotations,
                              MediaType mediaType){
        return InboundSseEvent.class.isAssignableFrom(type)
                && MediaType.SERVER_SENT_EVENTS_TYPE.isCompatible(mediaType);
    }
    @Override
    public InboundSseEvent readFrom(Class<InboundSseEvent>type, Type genericType,
                          Annotation[] annotations, MediaType mediaType,
                          MultivaluedMap<String,String> httpHeaders,
                          InputStream entityStream)throws IOException, WebApplicationException {

        int cnt = entityStream.available();
        byte[] byteBuffer = new byte[cnt];
        ((ByteArrayInputStream)entityStream).read(byteBuffer, 0 ,cnt);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(byteBuffer);
        out.write(SseConstants.DOUBLE_EOL);
        ByteArrayInputStream inStream = new ByteArrayInputStream(out.toByteArray());

        SseEventInputImpl sseEventInputImpl = new SseEventInputImpl(annotations,
                mediaType, mediaType, httpHeaders, inStream);

        InboundSseEvent inboundSseEvent = sseEventInputImpl.read();
        inStream.close();
        return inboundSseEvent;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
                               MediaType mediaType) {
        return InboundSseEvent.class.isAssignableFrom(type);
                //&& MediaType.SERVER_SENT_EVENTS.equalsIgnoreCase(mediaType.toString());
    }
    @Override
    public void writeTo(InboundSseEvent event,Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        Charset charset = StandardCharsets.UTF_8;
        if (event.getComment() != null)
        {
            for (final String comment : event.getComment().split("\n"))
            {
                entityStream.write(SseConstants.COMMENT_LEAD);
                entityStream.write(comment.getBytes(charset));
                entityStream.write(SseConstants.EOL);
            }
        }
        if (event.getName() != null)
        {
            entityStream.write(SseConstants.NAME_LEAD);
            entityStream.write(event.getName().getBytes(charset));
            entityStream.write(SseConstants.EOL);
        }
        if (event.getId() != null)
        {
            entityStream.write(SseConstants.ID_LEAD);
            entityStream.write(event.getId().getBytes(charset));
            entityStream.write(SseConstants.EOL);
        }
        if (event.getReconnectDelay() > -1)
        {
            entityStream.write(SseConstants.RETRY_LEAD);
            entityStream.write(Long.toString(event.getReconnectDelay()).getBytes(StandardCharsets.UTF_8));
            entityStream.write(SseConstants.EOL);
        }
        if (event.readData() != null) {
            entityStream.write(SseConstants.DATA_LEAD);
            entityStream.write(event.readData().getBytes(charset));
            entityStream.write(SseConstants.EOL);
        }
        //entityStream.flush();
    }
}
