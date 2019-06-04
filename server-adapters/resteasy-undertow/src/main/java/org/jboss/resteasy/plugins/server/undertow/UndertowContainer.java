package org.jboss.resteasy.plugins.server.undertow;

import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.util.PortProvider;

import javax.ws.rs.core.Application;

public class UndertowContainer {

   protected final PathHandler root = new PathHandler();
   protected final ServletContainer container = ServletContainer.Factory.newInstance();
   protected UndertowJaxrsServerEmbedded undertowJaxrsServer = new UndertowJaxrsServerEmbedded();
   protected DeploymentManager manager;

   private ResteasyDeployment deployment = new ResteasyDeploymentImpl();
   private int port = PortProvider.getPort();
   private String rootResourcePath = "/";
   private Class<? extends Application> application;

   public UndertowContainer() {
   }

   public UndertowContainer setResteasyDeployment(ResteasyDeployment deployment) {
      this.deployment = deployment;
      return this;
   }

   public UndertowContainer setPort(int port) {
      this.port = port;
      return this;
   }

   public UndertowContainer setRootResourcePath(String path) {
      this.rootResourcePath = path;
      return this;
   }

   public UndertowContainer setApplicationClass(Class<? extends Application> application) {
      this.application = application;
      return this;
   }

   public UndertowJaxrsServerEmbedded start() {
      undertowJaxrsServer.start();
      /**
      server = Undertow.builder()
         .addHttpListener(port, "localhost")
         .setHandler(root)
         .build();
      server.start();
      **/
      return undertowJaxrsServer;
   }
}
