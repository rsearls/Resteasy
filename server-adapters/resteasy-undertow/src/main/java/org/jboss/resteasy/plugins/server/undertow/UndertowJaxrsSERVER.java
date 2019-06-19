package org.jboss.resteasy.plugins.server.undertow;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.plugins.server.embedded.EMBEDDEDJaxrsServer;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.util.EmbeddedServerHelper;
import org.jboss.resteasy.util.PortProvider;

import javax.servlet.ServletException;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static io.undertow.servlet.Servlets.servlet;


/**
 * Wrapper around Undertow to make resteasy deployments easier
 * Each ResteasyDeployment or jaxrs Application is deployed under its own web deployment (WAR)
 *
 * You may also deploy after the server has started.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UndertowJaxrsSERVER implements EMBEDDEDJaxrsServer<UndertowJaxrsSERVER>
{
   protected final PathHandler root = new PathHandler();
   protected final ServletContainer container = ServletContainer.Factory.newInstance();
   protected Undertow server;
   protected DeploymentManager manager;
   protected Map<String, String> contextParams;
   protected Map<String, String> initParams;

   private ResteasyDeployment resteasyDeployment;
   private int port = PortProvider.getPort();
   private String hostname = "localhost";
   private String rootResourcePath;
   private EmbeddedServerHelper serverHelper = new EmbeddedServerHelper();

   @Override
   public UndertowJaxrsSERVER deploy() {
      serverHelper.checkDeployment(resteasyDeployment);
 /***
      if (resteasyDeployment == null) {
         throw new IllegalArgumentException("A ResteasyDeployment object required");
      }
***/
      //  ResteasyDeployment deployment = checkAppDeployment(resteasyDeployment);
      ResteasyDeployment deployment = resteasyDeployment;  // tmp
      return deploy(deployment, serverHelper.checkContextPath(rootResourcePath),
         resteasyDeployment.getClass().getClassLoader());
   }

   @Override
   public UndertowJaxrsSERVER start()
   {
      server = Undertow.builder()
         .addHttpListener(port, hostname)
         .setHandler(root)
         .build();
      server.start();
      return this;
   }

   @Override
   public void stop()
   {
      server.stop();
   }

   public ResteasyDeployment getDeployment() {
      if (resteasyDeployment == null) {
         resteasyDeployment = new ResteasyDeploymentImpl();
      }
      return resteasyDeployment;
   }
   @Override
   public UndertowJaxrsSERVER setDeployment(ResteasyDeployment resteasyDeployment) {
      this.resteasyDeployment = resteasyDeployment;
      return this;
   }

   @Override
   public UndertowJaxrsSERVER setPort(int port) {
      this.port = port;
      return this;
   }

   @Override
   public UndertowJaxrsSERVER setHostname(String hostname) {
      this.hostname = hostname;
      return this;
   }

   @Override
   public UndertowJaxrsSERVER setRootResourcePath(String rootResourcePath) {
      this.rootResourcePath = rootResourcePath;
      return this;
   }

   @Override
   public UndertowJaxrsSERVER setSecurityDomain(SecurityDomain sc) {
      // no-op; does not apply to undertow setup
      return this;
   }
/****
   // private ResteasyDeployment checkAppDeployment(ResteasyDeployment deployment) {
   private String checkAppDeployment(ResteasyDeployment deployment) {

      ResteasyDeployment appDeployment = deployment;

      ApplicationPath appPath = null;
      if (deployment.getApplicationClass() != null)
      {
         try
         {
            Class clazz = Class.forName(deployment.getApplicationClass());
            appPath = (ApplicationPath) clazz.getAnnotation(ApplicationPath.class);

         } catch (ClassNotFoundException e)
         {
            // todo how to handle
         }
      } else if (deployment.getApplication() != null)
      {
         appPath = deployment.getApplication().getClass().getAnnotation(ApplicationPath.class);
      }

      String aPath = null;
      if (appPath != null){
         aPath = appPath.value();
      }
      return aPath;
   }
*****/

   /*************************************************************************/

   public UndertowJaxrsSERVER deploy(Application application) {
      ResteasyDeployment deployment = new ResteasyDeploymentImpl();
      deployment.setApplication(application);
      return deploy(deployment,
         serverHelper.checkAppPath(application.getClass().getAnnotation(ApplicationPath.class)),
         application.getClass().getClassLoader());
   }

   public UndertowJaxrsSERVER deploy(Class<? extends Application> application) {
      ResteasyDeployment deployment = new ResteasyDeploymentImpl();
      deployment.setApplicationClass(application.getName());
      return deploy(deployment,
         serverHelper.checkAppPath(application.getAnnotation(ApplicationPath.class)),
         deployment.getClass().getClassLoader());
   }

   public UndertowJaxrsSERVER deploy(Class<? extends Application> application,
                                     String contextPath) {
      ResteasyDeployment deployment = new ResteasyDeploymentImpl();
      deployment.setApplicationClass(application.getName());
      return deploy(deployment, serverHelper.checkContextPath(contextPath),
         deployment.getClass().getClassLoader());
   }

   private UndertowJaxrsSERVER deploy(ResteasyDeployment deployment,
                                     String contextPath, ClassLoader clazzLoader) {
      DeploymentInfo di = undertowDeployment(deployment);
      di.setClassLoader(clazzLoader);
      di.setContextPath(contextPath);
      di.setDeploymentName("Resteasy" + contextPath);

      if (contextParams != null)
      {
         for (Entry<String, String> e : contextParams.entrySet())
         {
            di.addInitParameter(e.getKey(), e.getValue());
         }
      }
      if (initParams != null)
      {
         ServletInfo servletInfo = di.getServlets().get("ResteasyServlet");
         for (Entry<String, String> e : initParams.entrySet())
         {
            servletInfo.addInitParam(e.getKey(), e.getValue());
         }
      }
      return deploy(di);
   }

   /**
    * Creates a web deployment for your ResteasyDeployent so you can set up things like security constraints
    * You'd call this method, add your servlet security constraints, then call deploy(DeploymentInfo)
    *
    * Note, only one ResteasyDeployment can be applied per DeploymentInfo
    * ResteasyServlet is mapped to mapping + "/*"
    *
    * Example:
    *
    * DeploymentInfo di = server.undertowDeployment(resteasyDeployment, "rest");
    * di.setDeploymentName("MyDeployment")
    * di.setContextRoot("root");
    * server.deploy(di);
    *
    * @param deployment
    * @param mappingPrefix resteasy.servlet.mapping.prefix
    * @return must be deployed by calling deploy(DeploymentInfo), also does not set context path or deployment name
    */
   public DeploymentInfo undertowDeployment(ResteasyDeployment deployment, String mappingPrefix)
   {
      String mapping = serverHelper.checkContextPath(mappingPrefix);
      if (!mapping.endsWith("/")) {
         mapping += "/";
      }
      mapping = mapping + "*";

      ServletInfo resteasyServlet = servlet("ResteasyServlet", HttpServlet30Dispatcher.class)
              .setAsyncSupported(true)
              .setLoadOnStartup(1)
              .addMapping(mapping);

      if (!mapping.equals("/*")) {
         String prefix = mapping.substring(0, mapping.length() - 2);
         resteasyServlet.addInitParam("resteasy.servlet.mapping.prefix", prefix);
      }

      return  new DeploymentInfo()
              .addServletContextAttribute(ResteasyDeployment.class.getName(), deployment)
              .addServlet(resteasyServlet);
   }

   public DeploymentInfo undertowDeployment(ResteasyDeployment deployment)
   {
      return undertowDeployment(deployment, serverHelper.checkAppDeployment(deployment));
   }

   public DeploymentInfo undertowDeployment(Class<? extends Application> application)
   {
      ResteasyDeployment deployment = new ResteasyDeploymentImpl();
      deployment.setApplicationClass(application.getName());
      DeploymentInfo di = undertowDeployment (deployment,
         serverHelper.checkAppPath(application.getAnnotation(ApplicationPath.class)));
      di.setClassLoader(application.getClassLoader());
      return di;
   }

   /**
    * Maps a path prefix to a resource handler to allow serving resources other
    * than the JAX-RS endpoints.
    * For example, this can be used for serving static resources like web pages
    * or API documentation that might be deployed with the REST application server.
    *
    * @param path
    * @param handler
    */
   public void addResourcePrefixPath(String path, ResourceHandler handler)
   {
      root.addPrefixPath(path, handler);
   }

   public UndertowJaxrsSERVER deploy(ResteasyDeployment deployment)
   {
      return deploy(deployment, serverHelper.checkContextPath(rootResourcePath),
         deployment.getClass().getClassLoader());
   }

   /**
    * Adds an arbitrary web deployment to underlying Undertow server.
    * This is for your own deployments
    *
    * @param builder
    * @return
    */
   public UndertowJaxrsSERVER deploy(DeploymentInfo builder)
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
      return this;
   }

   public UndertowJaxrsSERVER start(Undertow.Builder builder)
   {
      server = builder.setHandler(root).build();
      server.start();
      return this;
   }

   public DeploymentManager getManager() {
      return manager;
   }

   public Map<String, String> getContextParams() {
      if (contextParams == null) {
         contextParams = new HashMap<>();
      }
      return contextParams;
   }

   public UndertowJaxrsSERVER setContextParams(Map<String, String> contextParams) {
      this.contextParams = contextParams;
      return this;
   }

   public Map<String, String> getInitParams () {
      if (initParams == null) {
         initParams = new HashMap<>();
      }
      return initParams;
   }

   public UndertowJaxrsSERVER setInitParams(Map<String, String> initParams) {
      this.initParams = initParams;
      return this;
   }
/***
   private String checkAppPath(ApplicationPath appPath) {
      if (appPath != null) {
         return appPath.value();
      }
      return "/";
   }

   private String checkContextPath(String contextPath) {
      if (contextPath == null) {
         return "/";
      } else if (!contextPath.startsWith("/")) {
         return "/" + contextPath;
      }
      return contextPath;
   }
*****/
}
