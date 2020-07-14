package org.jboss.resteasy.microprofile.client.publisher;

import javax.ws.rs.client.RxInvokerProvider;
import javax.ws.rs.client.SyncInvoker;
import java.util.concurrent.ExecutorService;

/**
 * Identify (method return type org.reactivestreams.Publisher).  Provide rxjava
 * that will process the data and return a Publisher object.
 * This is class is following the class implementation pattern set in
 * resteasy-rxjava2.
 */
public class PublisherRxInvokerProvider implements RxInvokerProvider<PublisherRxInvoker> {
    @Override
    public boolean isProviderFor(Class<?> clazz) {
        return PublisherRxInvoker.class.equals(clazz);
    }
    @Override
    public PublisherRxInvoker getRxInvoker(SyncInvoker syncInvoker, ExecutorService executorService) {
        return new PublisherRxInvokerImpl(syncInvoker, executorService);
    }
}
