package org.jboss.resteasy.test.resource.resource;

import javax.annotation.Priority;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Priority(5100)   // should run after default priority 5000
@Provider
@Produces("text/plain")
public class ContextResolver5A implements ContextResolver<String> {
   public String getContext(Class<?> type) {
      if (type.equals(float.class))
      {
         return "5A";
      }
      return null;
   }
}
