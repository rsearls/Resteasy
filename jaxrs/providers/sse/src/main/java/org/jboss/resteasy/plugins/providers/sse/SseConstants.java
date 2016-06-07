package org.jboss.resteasy.plugins.providers.sse;

import java.nio.charset.Charset;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

public class SseConstants
{
   public static final String SERVER_SENT_EVENTS = "text/event-stream";

   public static final MediaType SERVER_SENT_EVENTS_TYPE = MediaType.valueOf(SERVER_SENT_EVENTS);

   public static final String LAST_EVENT_ID_HEADER = "Last-Event-ID";

   public static final GenericType<String> STRING_AS_GENERIC_TYPE = new GenericType<>(String.class);

   public static final Charset UTF8 = Charset.forName("UTF-8");

   public static final byte[] COMMENT_LEAD = ": ".getBytes(UTF8);

   public static final byte[] NAME_LEAD = "event: ".getBytes(UTF8);

   public static final byte[] ID_LEAD = "id: ".getBytes(UTF8);

   public static final byte[] RETRY_LEAD = "retry: ".getBytes(UTF8);

   public static final byte[] DATA_LEAD = "data: ".getBytes(UTF8);

   public static final byte[] EOL = {'\n'};

   public enum State {
      NEW_LINE, COMMENT, FIELD,
   }
}
