package org.jboss.resteasy.sse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEvent;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * User: rsearls
 * Date: 5/4/16
 */
public class InboundSseEventImpl implements InboundSseEvent {

    private static final GenericType<String> STRING_AS_GENERIC_TYPE = new GenericType<>(String.class);

    private final String name;
    private final String id;
    private final String comment;
    private final byte[] data;
    private final long reconnectDelay;
    private final Providers providers;
    private final Annotation[] annotations;
    private final MediaType mediaType;
    private final MultivaluedMap<String, String> headers;


    //TODO make this thread-safe
    /**
     * Inbound event builder. This implementation is not thread-safe.
     */
    public static class Builder {

        private String id;
        private String name;
        private long reconnectDelay = SseEvent.RECONNECT_NOT_SET;
        private final ByteArrayOutputStream dataStream;
        private final Annotation[] annotations;
        private final MediaType mediaType;
        private final MultivaluedMap<String, String> headers;
        private final StringBuilder commentBuilder;

        /**
         * Create new inbound event builder.
         *
         * @param annotations annotations attached to the Java type to be read. Used for
         *                    {@link javax.ws.rs.ext.MessageBodyReader} lookup.
         * @param mediaType   media type of the SSE event data.
         *                    Used for {@link javax.ws.rs.ext.MessageBodyReader} lookup.
         * @param headers     response headers. Used for {@link javax.ws.rs.ext.MessageBodyWriter} lookup.
         */
        public Builder(
                       Annotation[] annotations,
                       MediaType mediaType,
                       MultivaluedMap<String, String> headers
                       ) {
            this.annotations = annotations;
            this.mediaType = mediaType;
            this.headers = headers;

            this.commentBuilder = new StringBuilder();
            this.dataStream = new ByteArrayOutputStream();
        }

        /**
         * Get event identifier.
         * <p>
         * Contains value of SSE {@code "id"} field. This field is optional. Method may return {@code null}, if the event
         * identifier is not specified.
         * </p>
         *
         * @return event id.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Set inbound event name.
         * <p/>
         * Value of the received SSE {@code "event"} field.
         *
         * @param name {@code "event"} field value.
         * @return updated builder instance.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Add a comment line to the event.
         * <p>
         * The comment line will be added to the received SSE event comment as a new line in the comment field.
         * If the comment line parameter is {@code null}, the call will be ignored.
         * </p>
         *
         * @param commentLine comment line to be added to the event comment.
         * @return updated builder instance.
         * @since 2.21
         */
        public Builder commentLine(final CharSequence commentLine) {
            if (commentLine != null) {
                commentBuilder.append(commentLine).append('\n');
            }

            return this;
        }

        /**
         * Set reconnection delay (in milliseconds) that indicates how long the event receiver should wait
         * before attempting to reconnect in case a connection to SSE event source is lost.
         * <p>
         * Value of the received SSE {@code "retry"} field.
         * </p>
         *
         * @param milliseconds reconnection delay in milliseconds. Negative values un-set the reconnection delay.
         * @return updated builder instance.
         * @since 2.3
         */
        public Builder reconnectDelay(long milliseconds) {
            if (milliseconds < 0) {
                milliseconds = SseEvent.RECONNECT_NOT_SET;
            }
            this.reconnectDelay = milliseconds;
            return this;
        }

        /**
         * Add more inbound event data.
         *
         * @param data byte array containing data stored in the incoming event.
         * @return updated builder instance.
         */
        public Builder write(byte[] data) {
            if (data == null || data.length == 0) {
                return this;
            }

            try {
                this.dataStream.write(data);
            } catch (IOException ex) {
                // ignore - this is not possible with ByteArrayOutputStream
            }
            return this;
        }

        /**
         * Build a new inbound event instance using the supplied data.
         *
         * @return new inbound event instance.
         */
        public InboundSseEventImpl build() {
            Providers providerFactory = ResteasyProviderFactory.getContextData(Providers.class);

            return new InboundSseEventImpl(
                name,
                id,
                commentBuilder.length() > 0 ? commentBuilder.substring(0, commentBuilder.length() - 1) : null,
                reconnectDelay,
                dataStream.toByteArray(),
                providerFactory,
                annotations,
                ((mediaType == null)? new MediaType(null,null) : mediaType),
                headers);
        }
    }

    private InboundSseEventImpl(final String name,
                                final String id,
                                final String comment,
                                final long reconnectDelay,
                                final byte[] data,
                                final Providers providers,
                                final Annotation[] annotations,
                                final MediaType mediaType,
                                final MultivaluedMap<String, String> headers) {
        this.name = name;
        this.id = id;
        this.comment = comment;
        this.reconnectDelay = reconnectDelay;
        this.data = stripLastLineBreak(data);
        this.providers = providers;
        this.annotations = annotations;
        this.mediaType = mediaType;
        this.headers = headers;
    }




    /**
     * Check if the event is empty (i.e. does not contain any data).
     *
     * @return {@code true} if current instance does not contain any data, {@code false} otherwise.
     */
    @Override
    public boolean isEmpty(){
        return data.length == 0;
    }

    /**
     * Get the original event data as {@link String}.
     *
     * @return event data de-serialized into a string.
     * @throws javax.ws.rs.ProcessingException when provided type can't be read. The thrown exception wraps the original cause.
     */
    @Override
    public String readData() {
        return readData(STRING_AS_GENERIC_TYPE, null);
    }

    /**
     * Read event data as a given Java type.
     *
     * @param type Java type to be used for event data de-serialization.
     * @return event data de-serialized as an instance of a given type.
     * @throws javax.ws.rs.ProcessingException when provided type can't be read. The thrown exception wraps the original cause.
     */
    @Override
    public <T> T readData(Class<T> type) {
        return readData(new GenericType<T>(type), null);
    }

    /**
     * Read event data as a given generic type.
     *
     * @param type generic type to be used for event data de-serialization.
     * @return event data de-serialized as an instance of a given type.
     * @throws javax.ws.rs.ProcessingException when provided type can't be read. The thrown exception wraps the original cause.
     */
    @Override
    public <T> T readData(GenericType<T> type) {
        return readData(type, null);
    }

    /**
     * Read event data as a given Java type.
     *
     * @param messageType Java type to be used for event data de-serialization.
     * @param mediaType   {@link MediaType media type} to be used for event data de-serialization.
     * @return event data de-serialized as an instance of a given type.
     * @throws javax.ws.rs.ProcessingException when provided type can't be read. The thrown exception wraps the original cause.
     */
    @Override
    public <T> T readData(Class<T> messageType, MediaType mediaType) {
        return readData(new GenericType<T>(messageType), mediaType);
    }

    /**
     * Read event data as a given generic type.
     *
     * @param type      generic type to be used for event data de-serialization.
     * @param mediaType {@link MediaType media type} to be used for event data de-serialization.
     * @return event data de-serialized as an instance of a given type.
     * @throws javax.ws.rs.ProcessingException when provided type can't be read. The thrown exception wraps the original cause.
     */
    @Override
    public <T> T readData(GenericType<T> type, MediaType mediaType) {

        final MediaType effectiveMediaType = mediaType == null ? this.mediaType : mediaType;
        final MessageBodyReader reader = providers.getMessageBodyReader(
           type.getRawType(), type.getType(), annotations, effectiveMediaType);

        if (reader == null) {
            throw new IllegalStateException("Message body reader not found for the SSE event data");
        }
        return readAndCast(type, effectiveMediaType, reader);
    }


    @SuppressWarnings("unchecked")
    private <T> T readAndCast(GenericType<T> type, MediaType effectiveMediaType, MessageBodyReader reader) {
        try {
            return (T) reader.readFrom(
                type.getRawType(),
                type.getType(),
                annotations,
                effectiveMediaType,
                headers,
                new ByteArrayInputStream(data));
        } catch (IOException ex) {
            throw new ProcessingException(ex);
        }
    }

    /**
     * String last line break from data. (Last line-break should not be considered as part of received data).
     *
     * @param data data
     * @return updated byte array.
     */
    private static byte[] stripLastLineBreak(final byte[] data) {

        if (data.length > 0 && data[data.length - 1] == '\n') {
            return Arrays.copyOf(data, data.length - 1);
        }

        return data;
    }



    /*************************** SseEvent ****************************************/
    /**
     * Get event identifier.
     * <p>
     * Contains value of SSE {@code "id"} field. This field is optional. Method may return {@code null}, if the event
     * identifier is not specified.
     * </p>
     *
     * @return event id.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Get event name.
     * <p>
     * Contains value of SSE {@code "event"} field. This field is optional. Method may return {@code null}, if the event
     * name is not specified.
     * </p>
     *
     * @return event name, or {@code null} if not set.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Get a comment string that accompanies the event.
     * <p>
     * Contains value of the comment associated with SSE event. This field is optional. Method may return {@code null},
     * if the event comment is not specified.
     * </p>
     *
     * @return comment associated with the event.
     */
    @Override
    public String getComment() {
        return comment;
    }

    /**
     * Get new connection retry time in milliseconds the event receiver should wait before attempting to
     * reconnect after a connection to the SSE event source is lost.
     * <p>
     * Contains value of SSE {@code "retry"} field. This field is optional. Method returns {@link #RECONNECT_NOT_SET}
     * if no value has been set.
     * </p>
     *
     * @return reconnection delay in milliseconds or {@link #RECONNECT_NOT_SET} if no value has been set.
     */
    @Override
    public long getReconnectDelay() {
        return reconnectDelay;
    }

    /**
     * Check if the connection retry time has been set in the event.
     *
     * @return {@code true} if new reconnection delay has been set in the event, {@code false} otherwise.
     */
    @Override
    public boolean isReconnectDelaySet() {
        return reconnectDelay > SseEvent.RECONNECT_NOT_SET;
    }

}
