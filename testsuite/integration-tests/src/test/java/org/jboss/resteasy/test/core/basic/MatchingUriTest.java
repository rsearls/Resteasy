package org.jboss.resteasy.test.core.basic;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
//import org.jboss.resteasy.test.core.basic.resource.MatchingUriResource;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by rsearls on 10/10/17.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MatchingUriTest {
/**/
   //@Path("matchIt")
   @Path("/")
   public static class MatchingUriResource {

      @GET
      @Path("{id}")
      public String m1(@PathParam("id") String a) {
         return "M1 was called. id is: " + a;
      }

      @GET
      @Path("aa")
      @Produces({ "application/custom+json;qs=0.5" })
      public String m2() {
         return "M2 was called (application/custom+json;qs=0.5)";
      }

      @GET
      @Path("ab")
      @Produces({ "application/custom+json;q=0.5" })
      public String m3() {
         return "M3 was called (application/custom+json;q=0.5)";
      }
   }
/**/
   private static Client client;
   private static final String DEP = "MatchingUriTest";

   @Deployment
   public static Archive<?> deploy() {
      WebArchive war = TestUtil.prepareArchive(DEP);
      //war.addClass(MatchingUriResource.class);
      return TestUtil.finishContainerPrepare(war, null, MatchingUriResource.class);
   }

   @BeforeClass
   public static void setup() {
      client = ClientBuilder.newClient();
   }

   @AfterClass
   public static void cleanup() {
      client.close();
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, MatchingUriTest.class.getSimpleName());
   }

   //@Ignore
   @Test
   public void testSecondCase() throws Exception {
      //Invocation.Builder request = client.target(generateURL("/matchIt/aa")).request();
      Invocation.Builder request = client.target(generateURL("/aa")).request();
      Response response = request.get();
      String result = response.readEntity(String.class);

      //request = client.target(generateURL("/matchIt/ab")).request();
      request = client.target(generateURL("/ab")).request();
      response = request.get();
      result = response.readEntity(String.class);

      String stop = "h";
   }
}
