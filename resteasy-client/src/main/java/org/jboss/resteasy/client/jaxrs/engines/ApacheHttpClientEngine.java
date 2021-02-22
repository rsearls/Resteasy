package org.jboss.resteasy.client.jaxrs.engines;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;

/**
 * Methods added to support new apache 5.x engine.
 * Existing methods deprecated but are maintained for backward compatibility.
 */
public interface ApacheHttpClientEngine extends ClientHttpEngine
{
   /**
    * Enumeration to represent memory units.
    */
   enum MemoryUnit {
      /**
       * Bytes
       */
      BY,
      /**
       * Killo Bytes
       */
      KB,

      /**
       * Mega Bytes
       */
      MB,

      /**
       * Giga Bytes
       */
      GB
   }

   /**
    * Returns the container's default HTTP engine.
    * @return
    */
   @Deprecated
   static ApacheHttpClientEngine create()
   {
      return new ApacheHttpClient43Engine();
   }

   @Deprecated
   static ApacheHttpClientEngine create(
           org.apache.http.impl.client.CloseableHttpClient httpClient)
   {
      return new ApacheHttpClient43Engine(httpClient);
   }

   @Deprecated
   static ApacheHttpClientEngine create(
           org.apache.http.client.HttpClient httpClient, boolean closeHttpClient)
   {
      return new ApacheHttpClient43Engine(httpClient, closeHttpClient);
   }

   ///////////////////////  HTTP/2       ////////////////////////////

   /**
    *
    * @param engVersion Identifier of HTTP engine to create
    * @return
    */
   static ApacheHttpClientEngine create(String engVersion)
   {
      if ("HTTP/2".equals(engVersion)) {
         return new ApacheHttpClient5Engine();
      }
      return new ApacheHttpClient43Engine();
   }

   static ApacheHttpClientEngine create(
           org.apache.hc.client5.http.impl.classic.CloseableHttpClient httpClient)
   {
      return new ApacheHttpClient5Engine(httpClient);
   }

   static ApacheHttpClientEngine create(
           org.apache.hc.client5.http.classic.HttpClient httpClient, boolean closeHttpClient)
   {
      return new ApacheHttpClient5Engine(httpClient, closeHttpClient);
   }
}
