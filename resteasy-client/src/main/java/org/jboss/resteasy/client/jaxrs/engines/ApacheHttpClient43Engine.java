package org.jboss.resteasy.client.jaxrs.engines;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.jboss.resteasy.client.jaxrs.i18n.LogMessages;

/**
 * An Apache HTTP engine for use with the new Builder Config style.
 *
 * This class will be removed at some time in the future when
 * resteasy switches to using HTTP/2.
 */
@Deprecated
public class ApacheHttpClient43Engine extends ManualClosingApacheHttpClient43EngineAppraisal
{
   public ApacheHttpClient43Engine()
   {
      super();
   }

   public ApacheHttpClient43Engine(final HttpHost defaultProxy)
   {
      super(defaultProxy);
   }

   public ApacheHttpClient43Engine(final HttpClient httpClient)
   {
      super(httpClient);
   }

   public ApacheHttpClient43Engine(final HttpClient httpClient, final boolean closeHttpClient)
   {
      super(httpClient, closeHttpClient);
   }

   /**
    * Support original location of interface HttpContextProvider when using apache 4.3 code.
    *
    * NOTE: A fully qualified classname is required for parameter httpContextProvider
    * in order to maintain backward compatibility for apache 4.3 version of the code.
    * Interface HttpContextProvider has been moved into this class so the apache
    * 4.3 class imports continue to works.
    * Interface class org.jboss.resteasy.client.jaxrs.engines.HttpContextProvider
    * is deprecated class ManualClosingApacheHttpClient43EngineAppraisal.HttpContextProvider
    * should be used instead.
    * @param httpClient
    * @param httpContextProvider
    */
   public ApacheHttpClient43Engine(final HttpClient httpClient,
                                   final org.jboss.resteasy.client.jaxrs.engines.HttpContextProvider httpContextProvider)
   {
      super(httpClient, httpContextProvider);
   }

   /**
    * Support new location of interface HttpContextProvider when using apache 4.3 code.
    *
    * NOTE: In order to support both apache 4.3 and 5.x APIs, interface class
    * org.jboss.resteasy.client.jaxrs.engines.HttpContextProvider has been
    * deprecated and moved into this class.  Class ManualClosingApacheHttpClient43EngineAppraisal.HttpContextProvider
    * should be used instead.
    * @param httpClient
    * @param httpContextProvider
    */
   public ApacheHttpClient43Engine(final HttpClient httpClient,
                                   final ManualClosingApacheHttpClient43EngineAppraisal.HttpContextProvider httpContextProvider)
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
