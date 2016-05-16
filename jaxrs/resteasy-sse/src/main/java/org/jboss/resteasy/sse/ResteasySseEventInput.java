package org.jboss.resteasy.sse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEvent;
import javax.ws.rs.sse.SseEventInput;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.jboss.resteasy.resteasy_sse.i18n.LogMessages;
import org.jboss.resteasy.resteasy_sse.i18n.Messages;

/**
 * User: rsearls
 * Date: 5/5/16
 */
public class ResteasySseEventInput implements SseEventInput {

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private InputStream inputStream;

    private final Providers providers;
    private final Annotation[] annotations;
    private final MediaType mediaType;
    private final MultivaluedMap<String, String> headers;

    public static class Builder {

        private final Providers bProviders;
        private final Annotation[] annotations;
        private final MediaType mediaType;
        private final MultivaluedMap<String, String> headers;
        private final InputStream dummyStream = new ByteArrayInputStream(new byte[128]);

        public Builder(Providers bProviders,
                       Annotation[] annotations,
                       MediaType mediaType,
                       MultivaluedMap<String, String> headers) {

            this.bProviders = bProviders;
            this.annotations = annotations;
            this.mediaType = mediaType;
            this.headers = headers;
            //this.dataStream = new ByteArrayOutputStream();
        }

        public ResteasySseEventInput build() {
            return new ResteasySseEventInput (bProviders,
               annotations,
               mediaType,
               headers,
               dummyStream);
        }
    }
    /**
    public  ResteasySseEventInput (InputStream inputStream) {
        this.inputStream = inputStream;

    }
    **/
    public  ResteasySseEventInput (final Providers providers,
                                   final Annotation[] annotations,
                                   final MediaType mediaType,
                                   final MultivaluedMap<String, String> headers,
                                   final InputStream inputStream) {
        this.providers = providers;
        this.annotations = annotations;
        this.mediaType = mediaType;
        this.headers = headers;
        this.inputStream = inputStream;

    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (final IOException e) {
                    LogMessages.LOGGER.closingResponseInputStreamError(e);
                }
            }
        }
    }

    /**
     * Check if the chunked input has been closed.
     *
     * @return {@code true} if this chunked input has been closed, {@code false} otherwise.
     */
    @Override
    public boolean isClosed() {
        return false;  //TODO turn back on   //return closed.get();
    }

    /**
     * Read next SSE event from the response stream and convert it to a Java instance of {@link InboundSseEvent} type.
     * The method returns {@code null} if the underlying entity input stream has been closed (either implicitly or explicitly
     * by calling the {@link #close()} method).
     * <p>
     * Note: This operation is not thread-safe and has to be explicitly synchronized in case it is used from
     * multiple threads.
     * </p>
     *
     * @return next streamed event or {@code null} if the underlying entity input stream has been closed while reading
     * the next event data.
     * @throws IllegalStateException in case this chunked input has been closed.
     */
    @Override
    public InboundSseEvent read() throws IllegalStateException {
        /**
        if (closed.get()) {
            throw new IllegalStateException(Messages.MESSAGES.inputSourceAlreadyClosed());
        }
        **/
        InboundSseEventImpl.Builder builder = new InboundSseEventImpl.Builder(null,null,null)
           .name("Dummy-InboundSseEvent")
           .id("999")
           .commentLine("under construction");
        return builder.build(); //todo fix this.
    }

}
