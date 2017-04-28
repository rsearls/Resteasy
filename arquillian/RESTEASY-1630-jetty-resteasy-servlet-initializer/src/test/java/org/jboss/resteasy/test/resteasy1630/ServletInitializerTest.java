package org.jboss.resteasy.test.resteasy1630;

import java.net.URI;
import javax.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.resteasy1630.TestApplication;
import org.jboss.resteasy.resteasy1630.TestResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * User: rsearls
 * Date: 4/24/17
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ServletInitializerTest {

    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "RESTEASY-1630-two.war")
            .addClasses(TestApplication.class)
            .addClasses(TestResource.class)
            .addAsWebInfResource("web.xml");
        return war;
    }

    @ArquillianResource
    URI baseUri;

    /**
     * App declares files via the web.xml
     * @throws Exception
     */
    @Test
    public void testEndpoint() throws Exception {
        Response response = ResteasyClientBuilder.newClient()
            .target(baseUri.toString() + "test/17").request().get();
        System.out.println("Status: " + response.getStatus());
        String entity = response.readEntity(String.class);
        System.out.println("Result: " + entity);
        assertEquals(200, response.getStatus());
        Assert.assertEquals("17", entity);
    }
}
