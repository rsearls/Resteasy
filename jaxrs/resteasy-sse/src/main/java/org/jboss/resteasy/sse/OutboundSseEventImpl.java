package org.jboss.resteasy.sse;

import java.lang.reflect.Type;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEvent;
import org.jboss.resteasy.resteasy_sse.i18n.Messages;

public class OutboundSseEventImpl implements OutboundSseEvent {
    private String id;
    private String name;
    private String comment;
    private long reconnectDelay;
    private Class<?> type;
    private Type genericType;
    private MediaType mediaType;
    private Object data;


    public static class Builder {
        private String id;
        private String name;
        private String comment;
        private long reconnectDelay = SseEvent.RECONNECT_NOT_SET;
        private Class<?> type;
        private Type genericType;
        private MediaType mediaType = new MediaType(null,null);
        private Object data;

        /**
         * Set the event id.
         * <p>
         * Will be send as a value of the SSE {@code "id"} field. This field is optional.
         * </p>
         *
         * @param id event id.
         * @return updated builder instance.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Set event name.
         * <p>
         * Will be send as a value of the SSE {@code "event"} field. This field is optional.
         * </p>
         *
         * @param name event name.
         * @return updated builder instance.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set reconnection delay (in milliseconds) that indicates how long the event receiver should wait
         * before attempting to reconnect in case a connection to SSE event source is lost.
         * <p>
         * Will be send as a value of the SSE {@code "retry"} field. This field is optional.
         * </p>
         * <p>
         * Absence of a value of this field in an {@link OutboundSseEvent} instance
         * is indicated by {@link SseEvent#RECONNECT_NOT_SET} value returned from
         * {@link #getReconnectDelay()}.
         * </p>
         *
         * @param milliseconds reconnection delay in milliseconds. Negative values un-set the reconnection delay.
         * @return updated builder instance.
         */
        public Builder reconnectDelay(long milliseconds) {
            if (milliseconds < 0) {
                milliseconds = SseEvent.RECONNECT_NOT_SET;
            }

            this.reconnectDelay = milliseconds;
            return this;
        }

        /**
         * Set the {@link MediaType media type} of the event data.
         * <p>
         * This information is mandatory. The default value is {@link MediaType#TEXT_PLAIN}.
         * </p>
         *
         * @param mediaType {@link MediaType} of event data. Must not be {@code null}.
         * @return updated builder instance.
         * @throws NullPointerException in case the {@code mediaType} parameter is {@code null}.
         */
        public Builder mediaType(MediaType mediaType) {
            if (mediaType == null) {
                throw new NullPointerException(Messages.MESSAGES.mediaTypeCannotBeNull());
            }

            this.mediaType = mediaType;
            return this;
        }

        /**
         * Set comment string associated with the event.
         * <p>
         * The comment will be serialized with the event, before event data are serialized. If the event
         * does not contain any data, a separate "event" that contains only the comment will be sent.
         * This information is optional, provided the event data are set.
         * <p>
         * Note that multiple invocations of this method result in a previous comment being replaced with a new one.
         * To achieve multi-line comments, a multi-line comment string has to be used.
         * </p>
         *
         * @param comment comment string.
         * @return updated builder instance.
         */
        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        /**
         * Set event data and java type of event data.
         * <p>
         * Type information  will be used for {@link javax.ws.rs.ext.MessageBodyWriter} lookup.
         * <p>
         * Note that multiple invocations of this method result in previous even data being replaced with new one.
         * </p>
         *
         * @param type java type of supplied data. Must not be {@code null}.
         * @param data event data. Must not be {@code null}.
         * @return updated builder instance.
         * @throws NullPointerException in case either {@code type} or {@code data} parameter is {@code null}.
         */
        @SuppressWarnings("rawtypes")
        public Builder data(Class type, Object data) {
            if (type == null) {
                throw new NullPointerException(Messages.MESSAGES.classCannotBeNull());
            }
            if (data == null) {
                throw new NullPointerException(Messages.MESSAGES.dataObjectCannotBeNull());
            }

            this.type = type;
            this.data= data;
            return this;
        }

        /**
         * Set event data and a generic java type of event data.
         * <p>
         * Type information will be used for {@link javax.ws.rs.ext.MessageBodyWriter} lookup.
         * <p>
         * Note that multiple invocations of this method result in previous even data being replaced with new one.
         * </p>
         *
         * @param type generic type of supplied data. Must not be {@code null}.
         * @param data event data. Must not be {@code null}.
         * @return updated builder instance.
         * @throws NullPointerException in case either {@code type} or {@code data} parameter is {@code null}.
         */
        @SuppressWarnings("rawtypes")
        public Builder data(GenericType type, Object data) {
            if (type == null) {
                throw new NullPointerException(Messages.MESSAGES.genericTypeCannotBeNull());
            }
            if (data == null) {
                throw new NullPointerException(Messages.MESSAGES.dataObjectCannotBeNull());
            }

            this.genericType = type.getType();
            this.data= data;
            this.type = data.getClass();
            return this;
        }

        /**
         * Set event data and java type of event data.
         * <p>
         * This is a convenience method that derives the event data type information from the runtime type of
         * the event data. The supplied event data may be represented as {@link javax.ws.rs.core.GenericEntity}.
         * <p>
         * Note that multiple invocations of this method result in previous even data being replaced with new one.
         * </p>
         *
         * @param data event data. Must not be {@code null}.
         * @return updated builder instance.
         * @throws NullPointerException in case the {@code data} parameter is {@code null}.
         */
        public Builder data(Object data) {
            if (data == null) {
                throw new NullPointerException(Messages.MESSAGES.dataObjectCannotBeNull());
            }

            this.data = data;
            this.type = data.getClass();
            return this;
        }

        /**
         * Build {@link OutboundSseEvent}.
         * <p>
         * There are two valid configurations:
         * <ul>
         * <li>if a {@link Builder#comment(String) comment} is set, all other parameters are optional.
         * If event {@link Builder#data(Class, Object) data} and {@link Builder#mediaType(MediaType) media type} is set,
         * event data will be serialized after the comment.</li>
         * <li>if a {@link Builder#comment(String) comment} is not set, at least the event
         * {@link Builder#data(Class, Object) data} must be set. All other parameters are optional.</li>
         * </ul>
         * </p>
         *
         * @return new {@link OutboundSseEvent} instance.
         * @throws IllegalStateException when called with invalid configuration (neither a comment nor event data are set).
         */
        public OutboundSseEvent build() {
            if (comment == null && data == null) {
                throw new IllegalStateException(Messages.MESSAGES.commentOrDataMustBeSet());
            }

            return new OutboundSseEventImpl(
                id,
                name,
                comment,
                reconnectDelay,
                type,
                genericType,
                mediaType,
                data
            );
        }

    }

    OutboundSseEventImpl(String id, String name, String comment, long reconnectDelay,
                         Class<?> type, Type genericType, MediaType mediaType, Object data) {
        this.id = id;
        this.name = name;
        this.comment = comment;
        this.reconnectDelay = reconnectDelay;
        this.type = type;
        this.genericType = genericType;
        this.mediaType = mediaType;
        this.data = data;
    }

    /**
     * Get event identifier.
     * <p>
     * This field is optional. If specified, the value is send as a value of the SSE {@code "id"} field.
     * </p>
     *
     * @return event identifier, or {@code null} if not set.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Get event name.
     * <p>
     * This field is optional. If specified, will be send as a value of the SSE {@code "event"} field.
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
     * If specified, the comment value is sent with the event as one or more SSE comment lines
     * (depending on line breaks in the actual data string), before any actual event data are serialized.
     * If the event instance does not contain any data, a separate "event" that contains only the comment
     * will be sent. Comment information is optional, provided the event data are set.
     * </p>
     *
     * @return comment associated with the event.
     */
    @Override
    public String getComment() {
        return comment;
    }

    /**
     * Get connection retry time in milliseconds the event receiver should wait before attempting to
     * reconnect after a connection to the SSE source is lost.
     * <p>
     * This field is optional. If specified, the value is send as a value of the SSE {@code "retry"} field.
     * </p>
     *
     * @return reconnection delay in milliseconds or {@link SseEvent#RECONNECT_NOT_SET} if no value has been set.
     */
    @Override
    public long getReconnectDelay() {
        if (reconnectDelay < SseEvent.RECONNECT_NOT_SET) {
            return SseEvent.RECONNECT_NOT_SET;
        }
        return reconnectDelay;
    }

    /**
     * Check if the connection retry time has been set in the event.
     *
     * @return {@code true} if reconnection delay in milliseconds has been set in the event, {@code false} otherwise.
     */
    @Override
    public boolean isReconnectDelaySet() {
        return reconnectDelay != SseEvent.RECONNECT_NOT_SET;
    }

    /**
     * Get data type.
     * <p>
     * This information is used to select a proper {@link javax.ws.rs.ext.MessageBodyWriter} to be used for
     * serializing the {@link #getData() event data}.
     * </p>
     *
     * @return data type. May return {@code null}, if the event does not contain any data.
     */
    @Override
    public Class<?> getType() {
        return type;
    }

    /**
     * Get generic data type.
     * <p>
     * This information is used to select a proper {@link javax.ws.rs.ext.MessageBodyWriter} to be used for
     * serializing the {@link #getData() event data}.
     * </p>
     *
     * @return generic data type. May return {@code null}, if the event does not contain any data.
     */
    @Override
    public Type getGenericType() {
        return genericType;
    }

    /**
     * Get {@link MediaType media type} of the event data.
     * <p>
     * This information is used to a select proper {@link javax.ws.rs.ext.MessageBodyWriter} to be used for
     * serializing the {@link #getData() event data}.
     * </p>
     *
     * @return data {@link MediaType}.
     */
    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    /**
     * Get event data.
     * <p>
     * The event data, if specified, are serialized and sent as one or more SSE event {@code "data"} fields
     * (depending on the line breaks in the actual serialized data content). The data are serialized
     * using an available {@link javax.ws.rs.ext.MessageBodyWriter} that is selected based on the event
     * {@link #getType() type}, {@link #getGenericType()} generic type} and {@link #getMediaType()} media type}.
     * </p>
     *
     * @return event data. May return {@code null}, if the event does not contain any data.
     */
    @Override
    public Object getData() {
        return data;
    }
}
