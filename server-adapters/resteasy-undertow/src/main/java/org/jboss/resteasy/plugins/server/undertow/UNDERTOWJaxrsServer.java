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
public class UNDERTOWJaxrsServer implements EMBEDDEDJaxrsServer<UNDERTOWJaxrsServer>
{
   protected final PathHandler root = new PathHandler();
   protected final ServletContainer container = ServletContainer.Factory.newInstance();
   protected Undertow server;
   protected DeploymentManager manager;
   protected ResteasyDeploymentImpl deployment;
   protected Map<String, String> contextParams;
   protected Map<String, String> initParams;
   protected String resteasy_servlet_mapping_prefix = "/";
   private String contextPath = resteasy_servlet_mapping_prefix;
   private int port = PortProvider.getPort();
   private String hostname = "localhost";

   public UNDERTOWJaxrsServer ORIGdeploy() {
      return  deploy(this.deployment, null);
   }

   public UNDERTOWJaxrsServer deploy() {
      //return  deploy(this.deployment);
      return  deploy(this.deployment, checkApplicationPath(this.deployment));
   }

   // todo usage in UndertowParameterTest
   public UNDERTOWJaxrsServer deploy(ResteasyDeployment restDeployment, String contextPath /*,
                                     Map<String, String> XXcontextParams,
                                     Map<String, String> XXinitParams*/)
   {
      ResteasyDeployment deployment = checkAppDeployment(restDeployment);

      if (contextPath == null) contextPath = "/";
      if (!contextPath.startsWith("/")) contextPath = "/" + contextPath;
      DeploymentInfo builder = undertowDeployment(deployment);
      builder.setContextPath(contextPath);
      builder.setDeploymentName("Resteasy" + contextPath);
      builder.setClassLoader(deployment.getClass().getClassLoader());
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

   /**
    * Adds an arbitrary web deployment to underlying Undertow server.
    * This is for your own deployments
    *
    * @param builder
    * @return
    */
   public UNDERTOWJaxrsServer deploy(DeploymentInfo builder)
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

   @Override
   public UNDERTOWJaxrsServer start()
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

   @Override
   public ResteasyDeployment getDeployment() {
      if(deployment == null) {
         deployment = new ResteasyDeploymentImpl();
         deployment.start();
      }
      return deployment;
   }

   @Override
   public UNDERTOWJaxrsServer setDeployment(ResteasyDeployment deployment) {
      this.deployment = (ResteasyDeploymentImpl) deployment;
      return this;
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
    * DeploymentInfo di = server.undertowDeployment(deployment, "rest");
    * di.setDeploymentName("MyDeployment")
    * di.setContextRoot("root");
    * server.deploy(di);
    *
    * @param deployment
    * @param mapping resteasy.servlet.mapping.prefix
    * @return must be deployed by calling deploy(DeploymentInfo), also does not set context path or deployment name
    */
   // todo used internally
   public DeploymentInfo undertowDeployment(ResteasyDeployment deployment, String mapping)
   {
      if (mapping == null) mapping = "/";
      if (!mapping.startsWith("/")) mapping = "/" + mapping;
      if (!mapping.endsWith("/")) mapping += "/";
      mapping = mapping + "*";
      String prefix = null;
      if (!mapping.equals("/*")) prefix = mapping.substring(0, mapping.length() - 2);
      ServletInfo resteasyServlet = servlet("ResteasyServlet", HttpServlet30Dispatcher.class)
              .setAsyncSupported(true)
              .setLoadOnStartup(1)
              .addMapping(mapping);
      if (prefix != null) resteasyServlet.addInitParam("resteasy.servlet.mapping.prefix", prefix);

      return  new DeploymentInfo()
              .addServletContextAttribute(ResteasyDeployment.class.getName(), deployment)
              .addServlet(
                      resteasyServlet
                         );
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
   // todo used in WadlUndertowConnector
   // todo used internally
   public DeploymentInfo undertowDeployment(ResteasyDeployment deployment)
   {
      return undertowDeployment(deployment, resteasy_servlet_mapping_prefix);
   }

   /**
    * Creates a web deployment for the jaxrs Application.  Will ignore any @ApplicationPath annotation.
    *
    * @param application
    * @param mapping resteasy.servlet.mapping.prefix
    * @return
    */
   /****
    * // todo used internally
   public DeploymentInfo undertowDeployment(Class<? extends Application> application, String mapping)
   {
      ResteasyDeployment deployment = new ResteasyDeploymentImpl();
      deployment.setApplicationClass(application.getName());
      DeploymentInfo di = undertowDeployment(deployment, mapping);
      di.setClassLoader(application.getClassLoader());
      return di;
   }
**************/

   /**
    * Creates a web deployment for the jaxrs Application.  Will bind the resteasy.servlet.mapping.prefix
    * to @ApplicationPath if it exists, otherwise "/".
    *
    * @param application
    * @return
    */
   /***
    * // todo usage in UndertowTest
    * // todo replace ... set app in deployment, setContextPath ...
   public DeploymentInfo undertowDeployment(Class<? extends Application> application)
   {
      ApplicationPath appPath = application.getAnnotation(ApplicationPath.class);
      String path = "/";
      if (appPath != null) path = appPath.value();
      return undertowDeployment(application, path);
   }
********/
   /**
    * Maps a path prefix to a resource handler to allow serving resources other than the JAX-RS endpoints.
    * For example, this can be used for serving static resources like web pages or API documentation that might
    * be deployed with the REST application server.
    *
    * @param path
    * @param handler
    */
   // todo used in UndertowTest .. keep
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
   /***
    * // todo not used
   public UNDERTOWJaxrsServer deploy(ResteasyDeployment deployment)
   {
      return deploy(deployment, "/");
   }
***/
   /**
    * Creates a web deployment under contextPath
    *
    * @param deployment
    * @param contextPath
    * @return
    */
   /**
    * //todo internal use only
   public UNDERTOWJaxrsServer deploy(ResteasyDeployment deployment, String contextPath)
   {
      return deploy(deployment, contextPath, null, null);
   }
***/

   /**
    * Creates a web deployment for the jaxrs Application.  Will bind the contextPath
    * to @ApplicationPath if it exists, otherwise "/".
    *
    * @param application
    * @return
    */
   /*********
    * // todo Used in HeadContentLengthTest
    * // todo Used in UndertowTest
    * // todo replace ... set app in deployment, setContextPath ...
   public UNDERTOWJaxrsServer deploy(Class<? extends Application> application)
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
   /***
    * //todo used in UndertowTest
   public UNDERTOWJaxrsServer deploy(Class<? extends Application> application, String contextPath)
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
******/
   /**
    * Creates a web deployment for the jaxrs Application.  Will bind the contextPath
    * to @ApplicationPath if it exists, otherwise "/".
    *
    * @param application
    * @return
    */
   /**************
    * // todo NO usages found
   public UNDERTOWJaxrsServer deploy(Application application)
   {
      ApplicationPath appPath = application.getClass().getAnnotation(ApplicationPath.class);
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
   /*****
    * // todo used in UndertowTestRunner
   public UNDERTOWJaxrsServer deploy(Application application, String contextPath)
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
*********/

/**
   public String getContextPath() {
      return contextPath;
   }
   **/
   @Override
   //public UNDERTOWJaxrsServer setContextPath(String contextPath) {
   public UNDERTOWJaxrsServer setRootResourcePath(String contextPath) {
      this.contextPath = contextPath;
      return this;
   }
/**
   public String getHostname() {
      return hostname;
   }
   **/
   @Override
   public UNDERTOWJaxrsServer setHostname(String hostname) {
      this.hostname = hostname;
      return this;
   }
   /**
   public int getPort() {
      return port;
   }
   **/
   @Override
   public UNDERTOWJaxrsServer setPort(int port) {
      this.port = port;
      return this;
   }

   @Override
   public UNDERTOWJaxrsServer setSecurityDomain(SecurityDomain sc) {
      // no-op
      return this;
   }


   // todo used in UndertowTestRunner
   public UNDERTOWJaxrsServer start(Undertow.Builder builder)
   {
      server = builder.setHandler(root).build();
      server.start();
      return this;
   }

   // todo used in ApplicationContextTest
   // todo using in AsynchSpringTest
   public DeploymentManager getManager() {
      return manager;
   }

   public Map<String, String> getContextParams() {
      if (contextParams == null) {
         contextParams = new HashMap<>();
      }
      return contextParams;
   }

   public Map<String, String> getInitParams () {
      if (initParams == null) {
         initParams = new HashMap<>();
      }
      return initParams;
   }

   private String checkApplicationPath (ResteasyDeployment deployment) {

      String path = null;
      if (deployment.getApplicationClass() != null) {
         try
         {
            Class clazz = Class.forName(deployment.getApplicationClass());
            ApplicationPath appPath = (ApplicationPath)clazz.getAnnotation(ApplicationPath.class);
            if (appPath != null) {
               // annotation is present and original root is not set
               //setRootResourcePath(appPath.value());
               path = appPath.value();
            }
         } catch (ClassNotFoundException e) {
            // todo how to handle
         }
      } else
         // dynamically set the root path (the user can rewrite it by calling setRootResourcePath)
         if (deployment.getApplication() != null) {
            ApplicationPath appPath = deployment.getApplication().getClass()
               .getAnnotation(ApplicationPath.class);
            if (appPath != null) {
               // annotation is present and original root is not set
               //setRootResourcePath(appPath.value());
               path = appPath.value();
            }
         }
      return path;
   }

   private ResteasyDeployment checkAppDeployment(ResteasyDeployment deployment) {
      ResteasyDeployment appDeployment = deployment;
      if (deployment.getApplication() != null || deployment.getApplicationClass() != null) {
         // Applications require a ResteasyDeployment that has not been started
         appDeployment = new ResteasyDeploymentImpl();
         appDeployment.merge(deployment);
         appDeployment.setApplicationClass(deployment.getApplicationClass());
         appDeployment.setApplication(deployment.getApplication());
      }
      return appDeployment;
   }

}
