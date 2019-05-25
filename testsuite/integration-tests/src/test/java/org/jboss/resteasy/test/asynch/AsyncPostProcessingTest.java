package org.jboss.resteasy.test.asynch;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import javax.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.test.asynch.resource.AsyncPostProcessingMsgBodyWriterInterceptor;
import org.jboss.resteasy.test.asynch.resource.AsyncPostProcessingInterceptor;
import org.jboss.resteasy.test.asynch.resource.AsyncPostProcessingResource;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.utils.PermissionUtil;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.jboss.logging.Logger;

import javax.ws.rs.core.Response;
import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.security.SecurityPermission;
import java.util.PropertyPermission;

/**
 * @tpSubChapter Asynchronous RESTEasy
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-767
 * @tpSince RESTEasy 3.0.16
 */
@RunWith(Arquillian.class)
public class AsyncPostProcessingTest {

   private static Logger logger = Logger.getLogger(AsyncPostProcessingTest.class);
   static ResteasyClient client;

   @Deployment
   public static Archive<?> createTestArchive() {
      WebArchive war =  TestUtil.prepareArchive(AsyncPostProcessingTest.class.getSimpleName());
      war.addClasses(TestUtil.class, PortProviderUtil.class);
      war.addAsWebInfResource(AsyncPostProcessingTest.class.getPackage(), "AsyncPostProcessingTestWeb.xml", "web.xml");
      // rls test start
      //war.addAsManifestResource(AsyncPostProcessingTest.class.getPackage(),
      //   "AsyncPost_jboss-deployment-structure.xml", "jboss-deployment-structure.xml");
      //war.setManifest(new StringAsset("Manifest-Version: 1.0\n"
      //   + "Dependencies: org.jboss.resteasy.resteasy-jaxrs\n"));
      // rls test end

      // Arquillian in the deployment
      war.addAsManifestResource(PermissionUtil.createPermissionsXmlAsset(
            new SecurityPermission("insertProvider"),
            //new FilePermission("/home/rsearls/j1/wildfly/dist/target/wildfly-17.0.0.Beta1-SNAPSHOT/modules/system/layers/base/org/jboss/resteasy/resteasy-jaxrs/main/resteasy-core-4.1.0-SNAPSHOT.jar", "read"),
            //new FilePermission("/home/rsearls/j1/wildfly/dist/target/wildfly-17.0.0.Beta1-SNAPSHOT/modules/system/layers/base/org/jboss/resteasy/resteasy-jaxrs/main/resteasy-client-4.1.0-SNAPSHOT.jar", "read"),
            new ReflectPermission("suppressAccessChecks"),
            new PropertyPermission("arquillian.*", "read"),
            new PropertyPermission("ipv6", "read"),
            new PropertyPermission("node", "read"),
            new PropertyPermission("org.jboss.resteasy.port", "read"),
            new RuntimePermission("accessDeclaredMembers"),
            new RuntimePermission("getenv.RESTEASY_PORT"),
            new SocketPermission(PortProviderUtil.getHost(), "connect,resolve")
      ), "permissions.xml");
      return TestUtil.finishContainerPrepare(war, null, AsyncPostProcessingResource.class,
            AsyncPostProcessingMsgBodyWriterInterceptor.class, AsyncPostProcessingInterceptor.class);
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, AsyncPostProcessingTest.class.getSimpleName());
   }

   @Before
   public void init() {
      client = (ResteasyClient)ClientBuilder.newClient();
   }

   @After
   public void after() throws Exception {
      client.close();
   }

   /**
    * @tpTestDetails Test synchronized request.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testSync() throws Exception {
      reset();
      Response response = client.target(generateURL("/sync")).request().get();
      logger.info("Status: " + response.getStatus());
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      logger.info("TestMessageBodyWriterInterceptor.called: " + AsyncPostProcessingMsgBodyWriterInterceptor.called);
      logger.info("TestPostProcessInterceptor.called: " + AsyncPostProcessingInterceptor.called);
      response.bufferEntity();
      logger.info("returned entity: " + response.readEntity(String.class));
      Assert.assertTrue("AsyncPostProcessingMsgBodyWriterInterceptor interceptor was not called",
            AsyncPostProcessingMsgBodyWriterInterceptor.called);
      Assert.assertTrue("AsyncPostProcessingInterceptor interceptor was not called", AsyncPostProcessingInterceptor.called);
      Assert.assertEquals("Entity has wrong content", "sync", response.readEntity(String.class));
   }

   /**
    * @tpTestDetails Test async request with delay.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testAsyncWithDelay() throws Exception {
      reset();
      Response response = client.target(generateURL("/async/delay")).request().get();
      logger.info("Status: " + response.getStatus());
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      logger.info("TestMessageBodyWriterInterceptor.called: " + AsyncPostProcessingMsgBodyWriterInterceptor.called);
      logger.info("TestPostProcessInterceptor.called: " + AsyncPostProcessingInterceptor.called);
      response.bufferEntity();
      logger.info("returned entity: " + response.readEntity(String.class));
      Assert.assertTrue("AsyncPostProcessingMsgBodyWriterInterceptor interceptor was not called",
            AsyncPostProcessingMsgBodyWriterInterceptor.called);
      Assert.assertTrue("AsyncPostProcessingInterceptor interceptor was not called", AsyncPostProcessingInterceptor.called);
      Assert.assertEquals("Entity has wrong content", "async/delay", response.readEntity(String.class));
   }

   /**
    * @tpTestDetails Test async request without delay.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testAsyncWithNoDelay() throws Exception {
      reset();
      Response response = client.target(generateURL("/async/nodelay")).request().get();
      logger.info("Status: " + response.getStatus());
      Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
      logger.info("TestMessageBodyWriterInterceptor.called: " + AsyncPostProcessingMsgBodyWriterInterceptor.called);
      logger.info("TestPostProcessInterceptor.called: " + AsyncPostProcessingInterceptor.called);
      response.bufferEntity();
      logger.info("returned entity: " + response.readEntity(String.class));
      Assert.assertTrue("AsyncPostProcessingMsgBodyWriterInterceptor interceptor was not called",
            AsyncPostProcessingMsgBodyWriterInterceptor.called);
      Assert.assertTrue("AsyncPostProcessingInterceptor interceptor was not called", AsyncPostProcessingInterceptor.called);
      Assert.assertEquals("Entity has wrong content", "async/nodelay", response.readEntity(String.class));
   }

   private void reset() {
      AsyncPostProcessingMsgBodyWriterInterceptor.called = false;
      AsyncPostProcessingInterceptor.called = false;
   }
}
