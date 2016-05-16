package org.jboss.resteasy.sse.event.inbound;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

//@ApplicationPath("/")
@Provider
public class TestPullModelEventsApplication extends Application {
   public Set<Class<?>> getClasses()
   {
      HashSet<Class<?>> classes = new HashSet<Class<?>>();
      classes.add(TestPullModelEventsResource.class);
      return classes;
   }
}