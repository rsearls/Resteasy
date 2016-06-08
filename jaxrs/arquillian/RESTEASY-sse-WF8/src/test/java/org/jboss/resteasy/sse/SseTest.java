package org.jboss.resteasy.sse;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource.Listener;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.plugins.providers.sse.SseEventProvider;
import org.jboss.resteasy.plugins.providers.sse.SseEventSourceImpl;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class SseTest {

	@Deployment(name = "resteasy-sse.war", order = 1)
	public static Archive<?> createTestArchive1() {
		WebArchive war1 = ShrinkWrap
				.create(WebArchive.class, "resteasy-sse.war")
				.setManifest(
						new StringAsset(
								"Manifest-Version: 1.0\n"
										+ "Dependencies: org.jboss.resteasy.resteasy-sse-provider\n"))
				.addClasses(SseResource.class).addClasses(SseApplication.class)
				.addAsWebInfResource("web.xml")
				.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
		System.out.println(war1.toString(true));
		return war1;
	}

	@Test
	public void testSseEvent() throws Exception {
		final Set<String> results = new HashSet<String>();
		final CountDownLatch latch = new CountDownLatch(6);
		WebTarget target = ClientBuilder.newBuilder().register(SseEventProvider.class).newClient().target(
				"http://localhost:9090/resteasy-sse/server-sent-events").path("domains").path("1");
		SseEventSourceImpl.SourceBuilder builder = new SseEventSourceImpl.SourceBuilder(
				target);

		
		SseEventSourceImpl eventSource = (SseEventSourceImpl) builder.build();
		eventSource.register(new Listener() {
			@Override
			public void onEvent(InboundSseEvent event) {
				results.add(event.readData());
				latch.countDown();
			}
		});
		eventSource.open();
		target.request().buildPost(null);
		Assert.assertTrue("Waiting for evet to be delivered has timed out.",
                latch.await(20 , TimeUnit.SECONDS));
		eventSource.close();
		Assert.assertTrue("6 SseInboundEvent expected", results.size() == 6);
		Assert.assertTrue("Expect the last event is Done event", results.toArray(new String[]{})[5].indexOf("Done") > -1);

	}
}
