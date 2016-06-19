package org.jboss.resteasy.plugins.providers.sse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventInput;
import org.jboss.resteasy.plugins.providers.sse.SseEventSourceImpl.EventHandler;


public class SseEventInputImpl implements SseEventInput
{

   private Annotation[] annotations;

   private MediaType mediaType;

   private MultivaluedMap<String, String> httpHeaders;

   private InputStream inputStream;

   private static EventReader eventReader = new EventReader();

   public SseEventInputImpl(Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
         InputStream inputStream)
   {
      this.annotations = annotations;
      this.mediaType = mediaType;
      this.httpHeaders = httpHeaders;
      this.inputStream = inputStream;
   }

   @Override
   public void close() throws IOException
   {
      //TODO: handle close;
      this.inputStream.close();

   }

   @Override
   public boolean isClosed()
   {
      //TODO: we need a state to log the close state
      return false;
   }

   // rls added start
   private EventHandler parent;
   public void setEventHandler(EventHandler parent) {
      this.parent = parent;
   }
   // rls added end

   @Override
   public InboundSseEvent read() throws IllegalStateException
   {
      byte[] chunk = null;
      try
      {
         chunk = eventReader.read(inputStream);
         if (chunk == null)
         {
            //close();
            return null;
         }
      }
      catch (IOException e1)
      {
         e1.printStackTrace();
      }

      final ByteArrayInputStream entityStream = new ByteArrayInputStream(chunk);
      final ByteArrayOutputStream tokenData = new ByteArrayOutputStream();
      Charset charset = SseConstants.UTF8;
      if (mediaType != null && mediaType.getParameters().get(MediaType.CHARSET_PARAMETER) != null)
      {
         charset = Charset.forName(mediaType.getParameters().get(MediaType.CHARSET_PARAMETER));
      }
      // rls final
      InboundSseEventImpl.Builder eventBuilder = new InboundSseEventImpl.Builder(annotations, mediaType,
            httpHeaders);
      int b = -1;
      SseConstants.State currentState = SseConstants.State.NEW_LINE;
      try
      {
         loop : do
         {
            switch (currentState)
            {
               case NEW_LINE :
                  if (b == '\r')
                  {
                     b = entityStream.read();

                     b = b == '\n' ? entityStream.read() : b;
                  }
                  else
                  {
                     b = entityStream.read();
                  }

                  if (b == '\n' || b == '\r' || b == -1)
                  {
                     break loop;
                  }

                  if (b == ':')
                  {
                     currentState = SseConstants.State.COMMENT;
                  }
                  else
                  {
                     tokenData.write(b);
                     currentState = SseConstants.State.FIELD;
                  }
                  break;
               case COMMENT :
                  // skipping comment data
                  b = readLineUntil(entityStream, '\n', tokenData);
                  final String commentLine = tokenData.toString(charset.toString());
                  tokenData.reset();
                  eventBuilder.commentLine(commentLine.trim());
                  currentState = SseConstants.State.NEW_LINE;
                  break;
               case FIELD :
                  b = readLineUntil(entityStream, ':', tokenData);
                  final String fieldName = tokenData.toString(charset.toString());
                  tokenData.reset();

                  if (b == ':')
                  {
                     do
                     {
                        b = entityStream.read();
                     }
                     while (b == ' ');

                     if (b != '\n' && b != '\r' && b != -1)
                     {
                        tokenData.write(b);
                        b = readLineUntil(entityStream, '\n', tokenData);
                     }
                  }

                  processField(eventBuilder, fieldName, mediaType, tokenData.toByteArray());
                  tokenData.reset();

                  currentState = SseConstants.State.NEW_LINE;

                  // rls added start
                  if ("data".equals(fieldName)) {
                     if (parent != null) {
                        parent.onEvent(eventBuilder.build());
                     }
                     eventBuilder = new InboundSseEventImpl.Builder(annotations, mediaType,
                         httpHeaders);
                  }
                  // rls added end
                  break;
            }
         }

         while (b != -1);
      }
      catch (IOException e)
      {
         throw new IllegalStateException(e);
      }
      return eventBuilder.build();

   }

   private int readLineUntil(final InputStream in, final int delimiter, final OutputStream out) throws IOException
   {
      int b;
      while ((b = in.read()) != -1)
      {
         if (b == delimiter || b == '\n' || b == '\r')
         {
            break;
         }
         else if (out != null)
         {
            out.write(b);
         }
      }

      return b;
   }

   private void processField(final InboundSseEventImpl.Builder inboundEventBuilder, final String name,
         final MediaType mediaType, final byte[] value)
   {
      Charset charset = SseConstants.UTF8;
      if (mediaType != null && mediaType.getParameters().get(MediaType.CHARSET_PARAMETER) != null)
      {
         charset = Charset.forName(mediaType.getParameters().get(MediaType.CHARSET_PARAMETER));
      }
      String valueString = new String(value, charset);
      if (name.startsWith("    "))
      {
         inboundEventBuilder.name(name.substring(4));
      }
      else if ("data".equals(name))
      {
         inboundEventBuilder.write(value);
         inboundEventBuilder.write(SseConstants.EOL);
      }
      else if ("id".equals(name))
      {
         inboundEventBuilder.id(valueString);
      }
      else if ("retry".equals(name))
      {
         try
         {
            inboundEventBuilder.reconnectDelay(Long.parseLong(valueString));
         }
         catch (final NumberFormatException ex)
         {
            //TODO:log
         }
      }
      else
      {
         //TODO:Log
      }
   }

}
