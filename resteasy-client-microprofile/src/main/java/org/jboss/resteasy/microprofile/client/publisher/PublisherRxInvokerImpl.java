package org.jboss.resteasy.microprofile.client.publisher;

import org.reactivestreams.Publisher;

import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;

/**
 *
 */
public class PublisherRxInvokerImpl implements PublisherRxInvoker {
    private final CompletionStageRxInvoker completionStageRxInvoker;
    private final PublisherProvider publisherProvider;

    public PublisherRxInvokerImpl(final CompletionStageRxInvoker completionStageRxInvoker) {
        this.completionStageRxInvoker = completionStageRxInvoker;
        this.publisherProvider = new PublisherProvider();
    }

    @Override
    public Publisher<Response> method(String name) {
        return (Publisher<Response>) publisherProvider.fromCompletionStage(
                completionStageRxInvoker.method(name));
    }

    @Override
    public <T> Publisher<T> method(String name, GenericType<T> responseType) {
        return (Publisher<T>) publisherProvider.fromCompletionStage(
                completionStageRxInvoker.method(name, responseType));
    }

    public <T> Publisher<T> method(String name, Class<T> responseType) {
        return (Publisher<T>) publisherProvider.fromCompletionStage(
                completionStageRxInvoker.method(name, responseType));
    }

    @Override
    public Publisher<Response> method(String name, Entity<?> entity) {
        return (Publisher<Response>) publisherProvider.fromCompletionStage(
                completionStageRxInvoker.method(name, entity));
    }

    @Override
    public <T> Publisher<T> method(String name, Entity<?> entity, Class<T> responseType) {
        return (Publisher<T>) publisherProvider.fromCompletionStage(
                completionStageRxInvoker.method(name, entity, responseType));
    }

    @Override
    public <T> Publisher<T> method(String name, Entity<?> entity, GenericType<T> responseType) {
        return (Publisher<T>) publisherProvider.fromCompletionStage(
                completionStageRxInvoker.method(name, entity, responseType));
    }
}
