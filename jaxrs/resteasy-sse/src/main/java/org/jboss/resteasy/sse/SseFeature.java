package org.jboss.resteasy.sse;

import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.SseEvent;

// clone of jersey class
/**
 * A JAX-RS {@link Feature feature} that enables Server-Sent Events support.
 *
 * User: rsearls
 * Date: 5/6/16
 */
public class SseFeature implements Feature {
   /**
    * {@link String} representation of Server sent events media type. ("{@value}").
    */
   public static final String SERVER_SENT_EVENTS = MediaType.SERVER_SENT_EVENTS; //"text/event-stream";
   /**
    * Server sent events media type.
    */
   public static final MediaType SERVER_SENT_EVENTS_TYPE = new MediaType("text", "event-stream");
      //MediaType.SERVER_SENT_EVENTS_TYPE; //MediaType.valueOf(SERVER_SENT_EVENTS);

   /**
    * If {@code true} then {@link org.glassfish.jersey.media.sse.SseFeature SSE Feature} automatic registration is
    * suppressed.
    * <p>
    * Since Jersey 2.8, by default SSE Feature is automatically enabled when the SSE module is on class path.
    * You can override this behavior by setting this property to {@code true}.
    * The value of this property may be specifically overridden on either client or server side by setting the
    * {@link #DISABLE_SSE_CLIENT client} or {@link #DISABLE_SSE_SERVER server} variant of this property.
    * </p>
    * <p>
    * The default value is {@code false}.
    * </p>
    * <p>
    * The name of the configuration property is <tt>{@value}</tt>.
    * </p>
    *
    * @since 2.8
    */
   //@Property
   //public static final String DISABLE_SSE = "jersey.config.media.sse.disable";
   /**
    * Client-side variant of {@link #DISABLE_SSE} property.
    * <p>
    * The default value is {@code false}.
    * </p>
    * <p>
    * The name of the configuration property is <tt>{@value}</tt>.
    * </p>
    *
    * @since 2.8
    */
   //@Property
   //public static final String DISABLE_SSE_CLIENT = "jersey.config.client.media.sse.disable";

   /**
    * Server-side variant of {@link #DISABLE_SSE} property.
    * <p>
    * The default value is {@code false}.
    * </p>
    * <p>
    * The name of the configuration property is <tt>{@value}</tt>.
    * </p>
    *
    * @since 2.8
    */
   //@Property
   //public static final String DISABLE_SSE_SERVER = "jersey.config.server.media.sse.disable";

   /**
    * A "reconnection not set" value for the SSE reconnect delay set via {@code retry} field.
    *
    * @since 2.3
    */
   public static final long RECONNECT_NOT_SET = SseEvent.RECONNECT_NOT_SET;

   /**
    * {@code "Last-Event-ID"} HTTP request header name as defined by
    * <a href="http://www.w3.org/TR/eventsource/#last-event-id">SSE specification</a>.
    *
    * @since 2.3
    */
   public static final String LAST_EVENT_ID_HEADER = HttpHeaders.LAST_EVENT_ID_HEADER; //"Last-Event-ID";


   @Override
   public boolean configure(final FeatureContext context) {
      if (context.getConfiguration().isEnabled(this.getClass())) {
         return false;
      }
      // todo figure out what to do with this
      /**
      switch (context.getConfiguration().getRuntimeType()) {
         case CLIENT:
            context.register(EventInputReader.class);
            context.register(InboundEventReader.class);
            break;
         case SERVER:
            context.register(OutboundEventWriter.class);
            break;
      }
      **/
      return true;
   }

   /**
    * Safely register a {@code SseFeature} in a given configurable context.
    *
    * @param ctx configurable context in which the SSE feature should be registered.
    * @return updated configurable context.
    */
   static <T extends Configurable<T>> T register(final T ctx) {
      if (!ctx.getConfiguration().isRegistered(SseFeature.class)) {
         ctx.register(SseFeature.class);
      }

      return ctx;
   }
}
