package org.jboss.resteasy.test.rest31.loadservices;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.test.core.basic.ApplicationPropertiesConfigPropertyApplicationInjectionTest;
import org.jboss.resteasy.test.rest31.loadservices.resource.AppDynamicFeature;
import org.jboss.resteasy.test.rest31.loadservices.resource.AppDynamicFeatureApplication;
import org.jboss.resteasy.test.rest31.loadservices.resource.AppDynamicFeatureResource;
import org.jboss.resteasy.test.rest31.loadservices.resource.AppFeature;
import org.jboss.resteasy.test.rest31.loadservices.resource.Rest31DynamicFeature;
import org.jboss.resteasy.test.rest31.loadservices.resource.Rest31Feature;
import org.jboss.resteasy.test.rest31.loadservices.resource.Rest31MessageBodyReader;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(Arquillian.class)
@RunAsClient
public class AppDynamicFeatureTest {
    static Client client;

    @Deployment
    public static Archive<?> deploySimpleResource() {
        JavaArchive dynamicFeatureServiceJar = ShrinkWrap.create(JavaArchive.class, "dynamicFeatureService.jar");
        dynamicFeatureServiceJar.addClass(Rest31DynamicFeature.class);
        dynamicFeatureServiceJar.addAsServiceProvider(jakarta.ws.rs.container.DynamicFeature.class,
                Rest31DynamicFeature.class);
        dynamicFeatureServiceJar.addClass(Rest31MessageBodyReader.class);
        dynamicFeatureServiceJar.addAsServiceProvider(jakarta.ws.rs.ext.Providers.class,
                Rest31MessageBodyReader.class);

        JavaArchive featureServiceJar = ShrinkWrap.create(JavaArchive.class, "featureService.jar");
        featureServiceJar.addClass(Rest31Feature.class);
        featureServiceJar.addAsServiceProvider(jakarta.ws.rs.core.Feature.class, Rest31Feature.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class,
                ApplicationPropertiesConfigPropertyApplicationInjectionTest.class.getSimpleName() + ".war");
        war.addClasses(AppDynamicFeatureApplication.class,
                AppDynamicFeatureResource.class,
                AppDynamicFeature.class,
                AppFeature.class);
        war.addAsWebInfResource(AppDynamicFeatureTest.class.getPackage(),
                "AppDynamicFeatureWed.xml", "web.xml");

        war.addAsLibrary(dynamicFeatureServiceJar);
        war.addAsLibrary(featureServiceJar);

        return war;
    }

    @BeforeClass
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path,
                ApplicationPropertiesConfigPropertyApplicationInjectionTest.class.getSimpleName());
    }

    @Test
    /**
     * DynamicFeature and Feature services must not be present
     */
    public void testExcludedFeatures() {
        WebTarget target = client.target(generateURL("/checkServiceClasses"));
        String response = target.request().get(String.class);
        Assert.assertEquals("The property is not found in the deployment", "success", response);
    }

    @Test
    /**
     * DynamicFeature and Feature services must not be present
     */
    public void testIncludedFeatures() {
        WebTarget target = client.target(generateURL("/checkAppClasses"));
        String response = target.request().get(String.class);
        Assert.assertEquals("The property is not found in the deployment", "success", response);
    }
}
