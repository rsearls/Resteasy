package org.jboss.resteasy.plugins.providers.sse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class ChunkReader
{
   private final byte[] delimiter;

   public ChunkReader() {
       delimiter = Arrays.copyOf("\r\n".getBytes(),  "\r\n".getBytes().length);
   }

   public byte[] readChunk(final InputStream in) throws IOException {
       final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
       byte[] delimiterBuffer = new byte[delimiter.length];

       int data;
       int dPos;
       do {
           dPos = 0;
           while ((data = in.read()) != -1) {
               final byte b = (byte) data;

               // last read byte is part of the chunk delimiter
               if (b == delimiter[dPos]) {
                   delimiterBuffer[dPos++] = b;
                   if (dPos == delimiter.length) {
                       // found chunk delimiter
                       break;
                   }
               } else if (dPos > 0) {
                   delimiterBuffer[dPos] = b;

                   int matched = matchTail(delimiterBuffer, 1, dPos, delimiter);
                   if (matched == 0) {
                       // flush delimiter buffer
                       buffer.write(delimiterBuffer, 0, dPos);
                       buffer.write(b);
                       dPos = 0;
                   } else if (matched == delimiter.length) {
                       // found chunk delimiter
                       break;
                   } else {
                       // one or more elements of a previous buffered delimiter
                       // are parts of a current buffered delimiter
                       buffer.write(delimiterBuffer, 0, dPos + 1 - matched);
                       dPos = matched;
                   }
               } else {
                   buffer.write(b);
               }
           }

       } while (data != -1 && buffer.size() == 0); // skip an empty chunk

       if (dPos > 0 && dPos != delimiter.length) {
           // flush the delimiter buffer, if not empty - parsing finished in the middle of a potential delimiter sequence
           buffer.write(delimiterBuffer, 0, dPos);
       }

       return (buffer.size() > 0) ? buffer.toByteArray() : null;
   }

   private static int matchTail(byte[] buffer, int offset, int length, byte[] pattern) {
       outer:
       for (int i = 0; i < length; i++) {
           final int tailLength = length - i;
           for (int j = 0; j < tailLength; j++) {
               if (buffer[offset + i + j] != pattern[j]) {
                   // mismatch - continue with shorter tail
                   continue outer;
               }
           }

           // found the longest matching tail
           return tailLength;
       }
       return 0;
   }
}
