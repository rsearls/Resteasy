package org.jboss.resteasy.client.jaxrs.engines;

import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.client5.http.classic.HttpClient;
import org.jboss.resteasy.client.jaxrs.i18n.LogMessages;

/**
 * An Apache HTTP engine for use with the new Builder Config style.
 */
public class ApacheHttpClient5Engine extends ManualClosingApacheHttpClient5EngineAppraisal
{
   public ApacheHttpClient5Engine()
   {
      super();
   }

   public ApacheHttpClient5Engine(final HttpHost defaultProxy)
   {
      super(defaultProxy);
   }

   public ApacheHttpClient5Engine(final HttpClient httpClient)
   {
      super(httpClient);
   }

   public ApacheHttpClient5Engine(final HttpClient httpClient, final boolean closeHttpClient)
   {
      super(httpClient, closeHttpClient);
   }

   public ApacheHttpClient5Engine(final HttpClient httpClient,
                                  final ManualClosingApacheHttpClient5EngineAppraisal.HttpContextProvider httpContextProvider)
   {
      super(httpClient, httpContextProvider);
   }

   public void finalize() throws Throwable
   {
      if (!isClosed() && allowClosingHttpClient && getHttpClient() != null)
         LogMessages.LOGGER.closingForYou(this.getClass());
      close();
      super.finalize();
   }

}
