package org.jboss.resteasy.plugins.server.undertow;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.plugins.server.embedded.EmbeddedJaxrsServer;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.util.PortProvider;

import javax.servlet.ServletException;

public class UndertowJaxrsServerEmbedded  implements EmbeddedJaxrsServer {
   private String hostname = "localhost";
   private int port = PortProvider.getPort();
   private String rootResourcePath = "";
   private DeploymentInfo di = null;
   private ResteasyDeployment deployment = new ResteasyDeploymentImpl();

   protected final PathHandler root = new PathHandler();
   protected final ServletContainer container = ServletContainer.Factory.newInstance();
   protected Undertow server = null;
   protected DeploymentManager manager;

   @Override
   public void setRootResourcePath(String rootResourcePath) {

   }

   @Override
   public ResteasyDeployment getDeployment() {
      return deployment;
   }

   @Override
   public void setDeployment(ResteasyDeployment deployment){
      this.deployment = deployment;
   }

   @Override
   public void setSecurityDomain(SecurityDomain sc) {
      // no-op
   }

   @Override
   public void start() {
      if (server == null)
      {
         server = Undertow.builder()
            .addHttpListener(port, hostname)
            .setHandler(root)
            .build();
      }
   }

   @Override
   public void stop() {
      if (server != null)
      {
         server.stop();
         server = null;
      }
   }

   public void deploy(DeploymentInfo builder)
   {
      manager = container.addDeployment(builder);
      manager.deploy();

      try
      {
         root.addPrefixPath(builder.getContextPath(), manager.start());
      }
      catch (ServletException e)
      {
         throw new RuntimeException(e);
      }

   }

   public DeploymentInfo getDeploymentInfo(ResteasyDeployment deployment) {
      String mapping = "/";      // resteasy.servlet.mapping.prefix
      String prefix = validateMappingPrefix(mapping);
         /**
         String mapping = "/";      // resteasy.servlet.mapping.prefix
         if (mapping == null) mapping = "/";
         if (!mapping.startsWith("/")) mapping = "/" + mapping;
         if (!mapping.endsWith("/")) mapping += "/";
         mapping = mapping + "*";
         String prefix = null;
         if (!mapping.equals("/*")) {
            prefix = mapping.substring(0, mapping.length() - 2);
         }
         ***/
      ServletInfo resteasyServlet = new ServletInfo("ResteasyServlet",
         HttpServlet30Dispatcher.class);
      resteasyServlet.setAsyncSupported(true)
         .setLoadOnStartup(1)
         .addMapping(mapping);
      if (prefix != null) {
         resteasyServlet.addInitParam("resteasy.servlet.mapping.prefix", prefix);
      }

      return  new DeploymentInfo()
         .addServletContextAttribute(ResteasyDeployment.class.getName(), deployment)
         .addServlet(resteasyServlet);
   }

   public String getHostname() {
      return hostname;
   }
   public void setHostname(String hostname) {
      this.hostname = hostname;
   }
   public int getPort() {
      return port;
   }
   public void setPort(int port) {
      this.port = port;
   }

   public DeploymentManager getManager() {
      return manager;
   }


   private String validateMappingPrefix(String mapping) {
      if (mapping == null) {
         mapping = "/";
      }
      if (!mapping.startsWith("/")) {
         mapping = "/" + mapping;
      }
      if (!mapping.endsWith("/")) {
         mapping += "/";
      }
      mapping = mapping + "*";
      String prefix = null;
      if (!mapping.equals("/*")) {
         prefix = mapping.substring(0, mapping.length() - 2);
      }
      return prefix;
   }
}
