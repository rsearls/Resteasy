package org.jboss.resteasy.sse;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventInput;
import org.jboss.resteasy.resteasy_sse.i18n.Messages;
import org.jboss.resteasy.resteasy_sse.i18n.LogMessages;

/**
 * User: rsearls
 * Date: 5/19/16
 */
public class SseEventInputImpl implements SseEventInput {

   private final AtomicBoolean closed = new AtomicBoolean(false);
   private InputStream inputStream;
   private final InboundSseEvent inboundSseEvent;

   public SseEventInputImpl(InboundSseEvent inboundSseEvent) {

      this.inboundSseEvent = inboundSseEvent;

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

      return closed.get();
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

      if (closed.get()) {
         throw new IllegalStateException(Messages.MESSAGES.inputSourceAlreadyClosed());
      }

      //todo fix this.

      return inboundSseEvent;
   }

}
