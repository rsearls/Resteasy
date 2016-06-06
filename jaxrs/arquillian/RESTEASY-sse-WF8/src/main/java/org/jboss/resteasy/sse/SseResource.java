package org.jboss.resteasy.sse;

import java.io.IOException;

import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.sse.SseContext;
import javax.ws.rs.sse.SseEventOutput;

import org.jboss.resteasy.plugins.providers.sse.SseContextImpl;

@Path("server-sent-events")
@Singleton
public class SseResource {

	private final Object outputLock = new Object();
	private SseEventOutput sseEventOutput;
	//@Inject
	private SseContext sseContext = new SseContextImpl();

	//@Inject
	public SseResource() {
		//this.sseContext = sseContext;
	}

	@GET
	@Produces(SseContextImpl.SERVER_SENT_EVENTS)
	public SseEventOutput getMessageQueue() {
		synchronized (outputLock) {
			if (sseEventOutput != null) {
				throw new IllegalStateException("Event output already served.");
			}

			sseEventOutput = sseContext.newOutput();
		}

		return sseEventOutput;
	}

	@POST
	public void addMessage(final String message) throws IOException {
		sseEventOutput.write(sseContext.newEvent().name("custom-message")
				.data(String.class, message).build());
	}

	@DELETE
	public void close() throws IOException {
		synchronized (outputLock) {
			sseEventOutput.close();
			sseEventOutput = sseContext.newOutput();
		}
	}

	@GET
	@Path("domains/{id}")
	@Produces(SseContextImpl.SERVER_SENT_EVENTS)
	public SseEventOutput startDomain(@PathParam("id") final String id) {
		final SseEventOutput output = sseContext.newOutput();

		new Thread() {
			public void run() {
				try {
					output.write(sseContext
							.newEvent()
							.name("domain-progress")
							.data(String.class,
									"starting domain " + id + " ...").build());
					Thread.sleep(200);
					output.write(sseContext.newEvent().name("domain-progress")
							.data("50%").build());
					Thread.sleep(200);
					output.write(sseContext.newEvent().name("domain-progress")
							.data("60%").build());
					Thread.sleep(200);
					output.write(sseContext.newEvent().name("domain-progress")
							.data("70%").build());
					Thread.sleep(200);
					output.write(sseContext.newEvent().name("domain-progress")
							.data("99%").build());
					Thread.sleep(200);
					output.write(sseContext.newEvent().name("domain-progress")
							.data("Done.").build());
					output.close();

				} catch (final InterruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();

		return output;
	}
}