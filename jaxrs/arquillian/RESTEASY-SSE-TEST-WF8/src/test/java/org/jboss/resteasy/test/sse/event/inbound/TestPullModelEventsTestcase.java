package org.jboss.resteasy.test.sse.event.inbound;

import org.junit.runner.RunWith;
import org.junit.Test;
import junit.framework.Assert;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.sse.event.inbound.TestPullModelEventsApplication;
import org.jboss.resteasy.sse.event.inbound.TestPullModelEventsResource;
import org.jboss.resteasy.sse.SseFeature;
import javax.ws.rs.sse.SseEventInput;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import java.io.File;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.client.ClientResponse;
import javax.ws.rs.sse.InboundSseEvent;
import org.jboss.resteasy.sse.OutboundSseEventImpl;

/**
 * User: rsearls
 * Date: 5/11/16
 */
@RunWith(Arquillian.class)
public class TestPullModelEventsTestcase {

   private static final String APP_NAME = "PullModelEvents";
   private static final String PATH = "events";
   private static final String TEST_URL = "http://localhost:8080/" + APP_NAME + "/" + PATH;

   @Deployment
   public static Archive<?> createDeployment() {
      WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
      war.addClasses(org.jboss.resteasy.sse.event.inbound.TestPullModelEventsApplication.class,
         org.jboss.resteasy.sse.event.inbound.TestPullModelEventsResource.class)
         .addAsWebInfResource("sse/event/web.xml", "web.xml")
         .addAsWebInfResource("sse/event/jboss-deployment-structure.xml", "jboss-deployment-structure.xml")
         .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      System.out.println(war.toString(true));
      // rls start debug
      //File wFile = new File("/tmp/xx/PullModelEvents.war");
      //war.as(ZipExporter.class).exportTo(wFile, true);
      // rls end debug
      return war;
   }

   @Test
   public void getOutboundMsg() throws Exception {

      ClientRequest request = new ClientRequest(TEST_URL + "/out");
      ClientResponse<?> response = request.get();
      SseEventInput eventInput = response.getEntity(SseEventInput.class);
      System.out.println("##status: " + response.getStatus());
      Assert.assertEquals(200, response.getStatus());

      while (!eventInput.isClosed()) {
         final InboundSseEvent inboundSseEvent = eventInput.read();
         if (inboundSseEvent == null) {
            // connection was closed
            Assert.fail("Returned InboundSseEvent should not be null.");
         }

         String data = inboundSseEvent.readData(String.class);
         System.out.println("result: " +inboundSseEvent.getName() + ";" + data);
         Assert.assertEquals("Hello SSE World", data);
         eventInput.close();
      }

   }

}
