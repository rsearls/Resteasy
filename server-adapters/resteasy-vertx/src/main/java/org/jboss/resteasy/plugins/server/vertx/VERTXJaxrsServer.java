package org.jboss.resteasy.plugins.server.vertx;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.plugins.server.embedded.EMBEDDEDJaxrsServer;
import org.jboss.resteasy.util.PortProvider;

/**
 * An HTTP server that sends back the content of the received HTTP request
 * in a pretty plaintext form.
 *
 * @author Andy Taylor (andy.taylor@jboss.org)
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 * @author Norman Maurer
 * @author Julien Viet
 * @version $Rev: 2080 $, $Date: 2010-01-26 18:04:19 +0900 (Tue, 26 Jan 2010) $
 */
public class VERTXJaxrsServer implements EMBEDDEDJaxrsServer<VERTXJaxrsServer>
{
   private static final ConcurrentMap<String, Helper> deploymentMap = new ConcurrentHashMap<>();
   protected VertxOptions vertxOptions = new VertxOptions();
   protected Vertx vertx;
   protected HttpServerOptions serverOptions = new HttpServerOptions();
   protected VertxResteasyDeployment deployment;
   protected String root = "";
   protected SecurityDomain domain;
   private String deploymentID;
   // default no idle timeout.

   public VERTXJaxrsServer() {
      // provide default port
      serverOptions.setPort(PortProvider.getPort());
   }

   @Override
   public VERTXJaxrsServer deploy() {
      // no-op
      return this;
   }

   @SuppressWarnings("unchecked")
   @Override
   public VERTXJaxrsServer start()
   {
      vertx = Vertx.vertx(vertxOptions);
      deployment.start();
      String key = UUID.randomUUID().toString();
      deploymentMap.put(key, new Helper(root, serverOptions, deployment, domain));
      // Configure the server.
      CompletableFuture<String> fut = new CompletableFuture<>();
      DeploymentOptions deploymentOptions = new DeploymentOptions()
         .setInstances(vertxOptions.getEventLoopPoolSize())
         .setConfig(new JsonObject().put("helper", key));
      vertx.deployVerticle(Verticle.class.getName(), deploymentOptions, ar -> {
         deploymentMap.remove(key);
         if (ar.succeeded())
         {
            fut.complete(ar.result());
         } else
         {
            fut.completeExceptionally(ar.cause());
         }
      });
      try
      {
         deploymentID = fut.get(60, TimeUnit.SECONDS);
      } catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
      } catch (ExecutionException e)
      {
         throw new RuntimeException(e.getCause());
      } catch (TimeoutException e)
      {
         throw new RuntimeException(e);
      }
      return this;
   }

   @Override
   public void stop()
   {
      if (deploymentID != null)
      {
         CompletableFuture<Void> fut = new CompletableFuture<>();
         vertx.close(ar ->
         {
            fut.complete(null);
         });
         deploymentID = null;
         try
         {
            fut.get(10, TimeUnit.SECONDS);
         } catch (InterruptedException e)
         {
            Thread.currentThread().interrupt();
         } catch (Exception ignore)
         {
         }
      }
   }


   @Override
   public ResteasyDeployment getDeployment() {
      if(deployment == null) {
         deployment = new VertxResteasyDeployment();
         deployment.start();
      }
      return deployment;
   }

   @Override
   public VERTXJaxrsServer setDeployment(ResteasyDeployment deployment)
   {
      this.deployment = (VertxResteasyDeployment) deployment;
      return this;
   }

   @Override
   public VERTXJaxrsServer setPort(int port)
   {
      serverOptions.setPort(port);
      return this;
   }

   public int getPort()
   {
      return serverOptions.getPort();
   }

   @Override
   public VERTXJaxrsServer setHostname(String hostname)
   {
      serverOptions.setHost(hostname);
      return this;
   }

   public String getHostname()
   {
      return serverOptions.getHost();
   }

   @Override
   public VERTXJaxrsServer setRootResourcePath(String rootResourcePath)
   {
      root = rootResourcePath;
      if (root != null && root.equals("/")) root = "";
      return this;
   }

   @Override
   public VERTXJaxrsServer setSecurityDomain(SecurityDomain sc)
   {
      this.domain = sc;
      return this;
   }


   public VertxOptions getVertxOptions()
   {
      return vertxOptions;
   }

   /**
    * Set {@link io.vertx.core.VertxOptions}.
    *
    * @param options the {@link io.vertx.core.VertxOptions}.
    * @see Vertx#vertx(VertxOptions)
    */
   public VERTXJaxrsServer setVertxOptions(VertxOptions options)
   {
      this.vertxOptions = options;
      return this;
   }

   /**
    * Set {@link io.vertx.core.http.HttpServerOptions}.
    *
    * @param options the {@link io.vertx.core.http.HttpServerOptions}.
    * @see Vertx#createHttpServer(HttpServerOptions)
    */
   public VERTXJaxrsServer setServerOptions(HttpServerOptions options)
   {
      this.serverOptions = options;
      return this;
   }

   public HttpServerOptions getServerOptions()
   {
      return serverOptions;
   }



   private static class Helper
   {
      final String root;
      final HttpServerOptions serverOptions;
      final ResteasyDeployment deployment;
      final SecurityDomain domain;

      Helper(final String root, final HttpServerOptions serverOptions, final ResteasyDeployment deployment, final SecurityDomain domain)
      {
         this.root = root;
         this.serverOptions = serverOptions;
         this.deployment = deployment;
         this.domain = domain;
      }

      public Handler<HttpServerRequest> createHandler(Vertx vertx)
      {
         return new VertxRequestHandler(vertx, deployment, root, domain);
      }
   }

   public static class Verticle extends AbstractVerticle
   {

      protected HttpServer server;

      @Override
      public void start(Future<Void> startFuture) throws Exception
      {
         Helper helper = deploymentMap.get(config().getString("helper"));
         server = vertx.createHttpServer(helper.serverOptions);
         server.requestHandler(new VertxRequestHandler(vertx, helper.deployment, helper.root, helper.domain));
         server.listen(ar -> {
            if (ar.succeeded())
            {
               startFuture.complete();
            } else
            {
               startFuture.fail(ar.cause());
            }
         });
      }
   }
}
