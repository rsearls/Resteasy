package org.jboss.resteasy.test.resource.resource;

import javax.annotation.Priority;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Priority(5100)
@Provider
public class ContextResolver4A implements ContextResolver<String> {
   public String getContext(Class<?> type) {
      if (type.equals(float.class)) {
         return "4A";
      }
      return null;
   }
}
