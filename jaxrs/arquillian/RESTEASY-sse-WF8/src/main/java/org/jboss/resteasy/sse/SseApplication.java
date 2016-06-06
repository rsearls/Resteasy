package org.jboss.resteasy.sse;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

@Provider
public class SseApplication extends Application
{
   public Set<Class<?>> getClasses()
   {
      HashSet<Class<?>> classes = new HashSet<Class<?>>();
      classes.add(SseResource.class);
      return classes;
   }
}
