package org.jboss.resteasy.client.jaxrs.engines;

import org.apache.http.protocol.HttpContext;

/**
 * This interface was created to be used by ManualClosingApacheHttpClient43Engine
 * (i.e. the apache 4.x version of httpclient and core archives).
 *
 * This interface has been moved into class ManualClosingApacheHttpClient43Engine
 * and into class ManualClosingApacheHttpClient5Engine.
 * It is the preferred location from which to reference it.
 *
 * This interface will be removed at sometime in the future.
 */
@Deprecated
public interface HttpContextProvider
{
   HttpContext getContext();
}
