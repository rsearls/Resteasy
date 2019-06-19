package org.jboss.resteasy.plugins.server.sun.http;

import com.sun.net.httpserver.HttpServer;
import org.jboss.resteasy.plugins.server.embedded.EMBEDDEDJaxrsServer;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.util.EmbeddedServerHelper;
import org.jboss.resteasy.util.PortProvider;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * com.sun.net.httpserver.HttpServer adapter for Resteasy.  You may instead want to create and manage your own HttpServer.
 * Use the HttpContextBuilder class in this case to build and register a specific HttpContext.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SUNHttpJaxrsServer implements EMBEDDEDJaxrsServer<SUNHttpJaxrsServer>
{
   protected HttpContextBuilder context = new HttpContextBuilder();
   protected HttpServer httpServer;
   protected int configuredPort = PortProvider.getPort(); // orig value 8080;
   protected int runtimePort = -1;
   protected ResteasyDeployment deployment;
   private EmbeddedServerHelper serverHelper = new EmbeddedServerHelper();

   @Override
   public SUNHttpJaxrsServer deploy() {
      // no-op
      return this;
   }

   @Override
   public SUNHttpJaxrsServer start()
   {
      serverHelper.checkDeployment(deployment);
      /***
      if (deployment == null) {
         throw new IllegalArgumentException("A ResteasyDeployment object required");
      } else if (deployment.getRegistry() == null) {
         deployment.start();
      }
      ***/
      setRootResourcePath(serverHelper.checkContextPath(
         serverHelper.checkAppDeployment(deployment)));

      if (httpServer == null)
      {
         try
         {
            httpServer = HttpServer.create(new InetSocketAddress(configuredPort), 10);
            runtimePort = httpServer.getAddress().getPort();
         }
         catch (IOException e)
         {
            throw new RuntimeException(e);
         }
      }
      context.bind(httpServer);
      httpServer.start();
      return this;
   }

   @Override
   public void stop()
   {
      runtimePort = -1;
      httpServer.stop(0);
      context.cleanup();
   }

   @Override
   public ResteasyDeployment getDeployment() {
      if(deployment == null) {
         deployment = context.getDeployment();
      }
      return deployment;
   }

   @Override
   public SUNHttpJaxrsServer setDeployment(ResteasyDeployment deployment)
   {
      this.deployment = deployment;
      this.context.setDeployment(deployment);
      return this;
   }
   /**
    * Value is ignored if HttpServer property is set. Default value is 8080
    *
    * @param port
    */
   @Override
   public SUNHttpJaxrsServer setPort(int port)
   {
      this.configuredPort = port;
      return this;
   }

   /**
    * Gets port number of this HttpServer.
    *
    * @return port number.
    */
   public int getPort()
   {
      return runtimePort > 0 ? runtimePort : configuredPort;
   }

   @Override
   public SUNHttpJaxrsServer setHostname(String hostname) {
      // no-op
      return this;
   }

   /**
    * Setting a security domain will turn on Basic Authentication
    *
    * @param securityDomain
    */
   @Override
   public SUNHttpJaxrsServer setSecurityDomain(SecurityDomain securityDomain)
   {
      this.context.setSecurityDomain(securityDomain);
      return this;
   }


   @Override
   public SUNHttpJaxrsServer setRootResourcePath(String rootResourcePath)
   {
      context.setPath(rootResourcePath);
      return this;
   }


   /**
    * If you do not provide an HttpServer instance, one will be created on startup
    *
    * @param httpServer
    */
   public SUNHttpJaxrsServer setHttpServer(HttpServer httpServer)
   {
      this.httpServer = httpServer;
      return this;
   }

}
