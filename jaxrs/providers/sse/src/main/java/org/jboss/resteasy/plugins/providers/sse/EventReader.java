package org.jboss.resteasy.plugins.providers.sse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EventReader
{
   private final byte[] END = "\r\n\r\n".getBytes();

   public EventReader() {

   }

   public byte[] read(final InputStream in) throws IOException {
       final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
       int data;
       int pos = 0;
       while ((data = in.read()) != -1) {
            byte b = (byte)data;
            if (b == END[pos]) {
                pos++;
            } else {
               pos = 0;
            }
            buffer.write(b);
            if (pos >= END.length) {
               return buffer.toByteArray();
            }
       }
       return null;
   }   
}
