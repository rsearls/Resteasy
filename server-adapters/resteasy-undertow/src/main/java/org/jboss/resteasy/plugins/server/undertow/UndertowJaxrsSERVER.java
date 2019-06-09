package org.jboss.resteasy.plugins.server.undertow;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;

import org.jboss.resteasy.plugins.server.embedded.EMBEDDEDJaxrsServer;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.util.PortProvider;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;

import javax.servlet.ServletException;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import static io.undertow.servlet.Servlets.servlet;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


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

   @Override
   public UndertowJaxrsSERVER deploy() {

      if (resteasyDeployment == null) {
         throw new IllegalArgumentException("A ResteasyDeployment object required");
      }

      ResteasyDeployment deployment = checkAppDeployment(resteasyDeployment);
      return deploy(deployment, checkContextPath(rootResourcePath),
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
      // obsolete method
      return null;
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
      // no-op
      return this;
   }


   private ResteasyDeployment checkAppDeployment(ResteasyDeployment deployment) {

      ResteasyDeployment appDeployment = deployment;
      // Check if user started ResteasyDeployment.  Check of Application
      // Undertow needs ResteasyDeployment not to be started when Application class specified.
      if (appDeployment.getRegistry() == null)
      {
         boolean isApplicationClassPresent = false;
         ApplicationPath appPath = null;
         if (deployment.getApplicationClass() != null) {
            isApplicationClassPresent = true;
            try
            {
               Class clazz = Class.forName(deployment.getApplicationClass());
               appPath = (ApplicationPath)clazz.getAnnotation(ApplicationPath.class);

            } catch (ClassNotFoundException e) {
               // todo how to handle
            }
         } else if (deployment.getApplication() != null) {
            isApplicationClassPresent = true;
            appPath = deployment.getApplication().getClass().getAnnotation(ApplicationPath.class);
         }

         if (isApplicationClassPresent)
         {
            // Applications require a ResteasyDeployment that has not been started
            appDeployment = new ResteasyDeploymentImpl();
            appDeployment.merge(deployment);
            appDeployment.setApplicationClass(deployment.getApplicationClass());
            appDeployment.setApplication(deployment.getApplication());

            setRootResourcePath(appPath.value());
         }
      }
      return appDeployment;
   }


   /*************************************************************************/
   // checkAppPath
   private String checkAppPath(ApplicationPath appPath) {
      if (appPath != null) {
         return appPath.value();
      }
      return "/";
   }

   // checkContextPath
   private String checkContextPath(String contextPath) {
      if (contextPath == null) {
         return "/";
      } else if (!contextPath.startsWith("/")) {
         return "/" + contextPath;
      }
      return contextPath;
   }

   public UndertowJaxrsSERVER deploy(Application application) {
      ResteasyDeployment deployment = new ResteasyDeploymentImpl();
      deployment.setApplication(application);
      return deploy(deployment,
         checkAppPath(application.getClass().getAnnotation(ApplicationPath.class)),
         application.getClass().getClassLoader());
   }

   public UndertowJaxrsSERVER deploy(Class<? extends Application> application) {
      ResteasyDeployment deployment = new ResteasyDeploymentImpl();
      deployment.setApplicationClass(application.getName());
      return deploy(deployment,
         checkAppPath(application.getAnnotation(ApplicationPath.class)),
         deployment.getClass().getClassLoader());
   }

   public UndertowJaxrsSERVER deploy(Class<? extends Application> application,
                                     String contextPath) {
      ResteasyDeployment deployment = new ResteasyDeploymentImpl();
      deployment.setApplicationClass(application.getName());
      return deploy(deployment, checkContextPath(contextPath),
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
    * @param mapping resteasy.servlet.mapping.prefix
    * @return must be deployed by calling deploy(DeploymentInfo), also does not set context path or deployment name
    */
   public DeploymentInfo undertowDeployment(ResteasyDeployment deployment, String mappingPrefix)
   {
      String mapping = checkContextPath(mappingPrefix);
      //if (mapping == null) mapping = "/";
      //if (!mapping.startsWith("/")) mapping = "/" + mapping;
      if (!mapping.endsWith("/")) mapping += "/";
      mapping = mapping + "*";
      /** moved ... rls
      String prefix = null;
      if (!mapping.equals("/*")) prefix = mapping.substring(0, mapping.length() - 2);
      **/
      ServletInfo resteasyServlet = servlet("ResteasyServlet", HttpServlet30Dispatcher.class)
              .setAsyncSupported(true)
              .setLoadOnStartup(1)
              .addMapping(mapping);

      if (!mapping.equals("/*")) {
         String prefix = mapping.substring(0, mapping.length() - 2);
         resteasyServlet.addInitParam("resteasy.servlet.mapping.prefix", prefix);
      }
      //if (prefix != null) resteasyServlet.addInitParam("resteasy.servlet.mapping.prefix", prefix);

      return  new DeploymentInfo()
              .addServletContextAttribute(ResteasyDeployment.class.getName(), deployment)
              .addServlet(resteasyServlet);
   }

   /**
    * Creates a web deployment for your ResteasyDeployent so you can set up things like security constraints
    * You'd call this method, add your servlet security constraints, then call deploy(DeploymentInfo)
    *
    * Note, only one ResteasyDeployment can be applied per DeploymentInfo.  Resteasy servlet is mapped to "/*"
    *
    * @param deployment
    * @return
    */
   public DeploymentInfo undertowDeployment(ResteasyDeployment deployment)
   {
      return undertowDeployment(deployment, "/");
   }

   /**
    * Creates a web deployment for the jaxrs Application.  Will ignore any @ApplicationPath annotation.
    *
    * @param application
    * @param mapping resteasy.servlet.mapping.prefix
    * @return
    */
   /****
   public DeploymentInfo undertowDeploymentORIG(Class<? extends Application> application, String mapping)
   {
      ResteasyDeployment deployment = new ResteasyDeploymentImpl();
      deployment.setApplicationClass(application.getName());
      DeploymentInfo di = undertowDeployment(deployment, mapping);
      di.setClassLoader(application.getClassLoader());
      return di;
   }
**********/

   /**
    * Creates a web deployment for the jaxrs Application.  Will bind the resteasy.servlet.mapping.prefix
    * to @ApplicationPath if it exists, otherwise "/".
    *
    * @param application
    * @return
    */
   /******
   public DeploymentInfo undertowDeploymentORIG(Class<? extends Application> application)
   {
      ApplicationPath appPath = application.getAnnotation(ApplicationPath.class);
      String path = "/";
      if (appPath != null) path = appPath.value();
      return undertowDeployment(application, path);
   }
***********/
   public DeploymentInfo undertowDeployment(Class<? extends Application> application)
   {
      ResteasyDeployment deployment = new ResteasyDeploymentImpl();
      deployment.setApplicationClass(application.getName());
      DeploymentInfo di = undertowDeployment (deployment,
         checkAppPath(application.getAnnotation(ApplicationPath.class)));
      di.setClassLoader(application.getClassLoader());
      return di;
   }

   /**
    * Maps a path prefix to a resource handler to allow serving resources other than the JAX-RS endpoints.
    * For example, this can be used for serving static resources like web pages or API documentation that might
    * be deployed with the REST application server.
    *
    * @param path
    * @param handler
    */
   public void addResourcePrefixPath(String path, ResourceHandler handler)
   {
      root.addPrefixPath(path, handler);
   }

   /**
    * Creates a web deployment under "/"
    *
    * @param deployment
    * @return
    */
   /*********
   public UndertowJaxrsSERVER deployORIG(ResteasyDeployment deployment)
   {
      return deploy(deployment, "/");
   }
***********/
   public UndertowJaxrsSERVER deploy(ResteasyDeployment deployment)
   {
      //return deploy(deployment, "/", deployment.getClass().getClassLoader());
      return deploy(deployment, checkContextPath(rootResourcePath),
         deployment.getClass().getClassLoader());
   }

   /**
    * Creates a web deployment under contextPath
    *
    * @param deployment
    * @param contextPath
    * @return
    */
   /****
   public UndertowJaxrsSERVER deployORIG(ResteasyDeployment deployment, String contextPath)
   {
      return deploy(deployment, contextPath, null, null);
   }
****/
   /*****
   public UndertowJaxrsSERVER deployORIG(ResteasyDeployment deployment, String contextPath, Map<String, String> contextParams, Map<String, String> initParams)
   {
      if (contextPath == null) contextPath = "/";
      if (!contextPath.startsWith("/")) contextPath = "/" + contextPath;
      DeploymentInfo builder = undertowDeployment(deployment);
      builder.setContextPath(contextPath);
      builder.setDeploymentName("Resteasy" + contextPath);
      builder.setClassLoader(deployment.getApplication().getClass().getClassLoader());
      if (contextParams != null)
      {
         for (Entry<String, String> e : contextParams.entrySet())
         {
            builder.addInitParameter(e.getKey(), e.getValue());
         }
      }
      if (initParams != null)
      {
         ServletInfo servletInfo = builder.getServlets().get("ResteasyServlet");
         for (Entry<String, String> e : initParams.entrySet())
         {
            servletInfo.addInitParam(e.getKey(), e.getValue());
         }
      }
      return deploy(builder);
   }
***********/
   /**
    * Creates a web deployment for the jaxrs Application.  Will bind the contextPath
    * to @ApplicationPath if it exists, otherwise "/".
    *
    * @param application
    * @return
    */
   /******
   public UndertowJaxrsSERVER deployORIG(Class<? extends Application> application)
   {
      ApplicationPath appPath = application.getAnnotation(ApplicationPath.class);
      String path = "/";
      if (appPath != null) path = appPath.value();
      return deploy(application, path);
   }
*********/
   /**
    * Creates a web deployment for the jaxrs Application.  Will ignore any @ApplicationPath annotation.
    *
    * @param application
    * @param contextPath
    * @return
    */
   /********
   public UndertowJaxrsSERVER deployORIG(Class<? extends Application> application, String contextPath)
   {
      if (contextPath == null) contextPath = "/";
      if (!contextPath.startsWith("/")) contextPath = "/" + contextPath;
      ResteasyDeployment deployment = new ResteasyDeploymentImpl();
      deployment.setApplicationClass(application.getName());
      DeploymentInfo di = undertowDeployment(deployment);
      di.setClassLoader(application.getClassLoader());
      di.setContextPath(contextPath);
      di.setDeploymentName("Resteasy" + contextPath);
      return deploy(di);
   }
***********/
   /**
    * Creates a web deployment for the jaxrs Application.  Will bind the contextPath
    * to @ApplicationPath if it exists, otherwise "/".
    *
    * @param application
    * @return
    */
   /********
   public UndertowJaxrsSERVER deployORIG(Application application)
   {
      ApplicationPath appPath = application.getClass().getAnnotation(ApplicationPath.class);
      String path = "/";
      if (appPath != null) path = appPath.value();
      return deploy(application, path);
   }
***********/
   /**
    * Creates a web deployment for the jaxrs Application.  Will ignore any @ApplicationPath annotation.
    *
    * @param application
    * @param contextPath
    * @return
    */
   /*****
   public UndertowJaxrsSERVER deployORIG(Application application, String contextPath)
   {
      if (contextPath == null) contextPath = "/";
      if (!contextPath.startsWith("/")) contextPath = "/" + contextPath;
      ResteasyDeployment deployment = new ResteasyDeploymentImpl();
      deployment.setApplication(application);
      DeploymentInfo di = undertowDeployment(deployment);
      di.setClassLoader(application.getClass().getClassLoader());
      di.setContextPath(contextPath);
      di.setDeploymentName("Resteasy" + contextPath);
      return deploy(di);
   }
********/

   /**
    * Adds an arbitrary web deployment to underlying Undertow server.  This is for your own deployments
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
}
