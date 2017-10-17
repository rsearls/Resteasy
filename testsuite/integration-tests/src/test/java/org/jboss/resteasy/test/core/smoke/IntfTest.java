package org.jboss.resteasy.test.core.smoke;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.test.core.smoke.resource.IntfA;
import org.jboss.resteasy.test.core.smoke.resource.IntfAResourse;
import org.jboss.resteasy.test.core.smoke.resource.IntfB;
import org.jboss.resteasy.test.core.smoke.resource.IntfBResource;
import org.junit.Ignore;
import org.junit.runner.RunWith;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by rsearls on 10/16/17.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class IntfTest {

   static ResteasyClient client;

   @Deployment
   public static Archive<?> deployLocatingResource() {
      WebArchive war = TestUtil.prepareArchive(IntfTest.class.getSimpleName());
      war.addClass(IntfA.class);
      war.addClass(IntfB.class);
      return TestUtil.finishContainerPrepare(war, null,
              IntfAResourse.class, IntfBResource.class,
              IntfTest.class);
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, IntfTest.class.getSimpleName());
   }

   @Before
   public void init() {
      client = new ResteasyClientBuilder().build();
   }

   @After
   public void after() throws Exception {
      client.close();
   }

   @Test
   public void testReportIntfA() throws Exception {
      ResteasyWebTarget rwt = client.target(generateURL("/report/states"));
      //ResteasyWebTarget rwt = client.target(generateURL("/parent/report/states"));
      String s = rwt.request().get(String.class);
      Assert.assertEquals("Wrong client answer.", "IntfA getReport called", s);
   }

   /**
    * @tpTestDetails Check result from resource with multiple interfaces.
    * @tpSince RESTEasy 3.0.16
    */
   @Ignore
   @Test
   public void testProxyReportIntfA() throws Exception {
      // this is the one with the reported failure.

      IntfA proxy = client.target(generateURL("/report/states")).proxy(IntfA.class);
      //IntfA proxy = client.target(generateURL("/parent")).proxyBuilder(IntfA.class).build();
      String s = proxy.getReport(null);
      Assert.assertEquals("Wrong client answer.", "IntfA getReport called", s);
   }
/**
   @Ignore
   @Test
   public void testProxyDataIntfB() throws Exception {
      IntfB proxy = client.target(generateURL("/report/states")).proxy(IntfB.class);
      String s = proxy.getData("my input");
      Assert.assertEquals("Wrong client answer.", "IntfB getData called", s);
   }
   **/
}
