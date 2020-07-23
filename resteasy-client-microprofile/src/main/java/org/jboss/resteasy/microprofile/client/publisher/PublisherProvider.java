package org.jboss.resteasy.microprofile.client.publisher;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import org.jboss.resteasy.spi.AsyncClientResponseProvider;
import org.jboss.resteasy.spi.AsyncResponseProvider;
import org.reactivestreams.Publisher;

import javax.ws.rs.ext.Provider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

@Provider
public class PublisherProvider implements AsyncResponseProvider<Publisher<?>>,
        AsyncClientResponseProvider<Publisher<?>> {

    private static class PublisherAdaptor<T> extends CompletableFuture<T> {
        private Disposable subscription;

        PublisherAdaptor(final Publisher<T> publisher) {
            //todo how to properly handle this for publisher
            this.subscription = new NoOpDisposable(this);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            subscription.dispose();
            return super.cancel(mayInterruptIfRunning);
        }
    }

    private static class NoOpDisposable implements Disposable {
        private CompletableFuture completableFuture;

        NoOpDisposable (final CompletableFuture cf) {
            this.completableFuture = cf;
        }
        @Override
        public void dispose(){}

        @Override
        public boolean isDisposed(){
            return true;
        }
    }

    @Override
    public CompletionStage<?> toCompletionStage(Publisher<?> asyncResponse) {
        return new PublisherAdaptor<>(asyncResponse);
    }

    @Override
    public Publisher<?> fromCompletionStage(CompletionStage<?> completionStage) {
        return Flowable.fromFuture(completionStage.toCompletableFuture());
    }

    @Override
    public Publisher<?> fromCompletionStage(final Supplier<CompletionStage<?>> completionStageSupplier) {
        return Flowable.defer(() -> fromCompletionStage(completionStageSupplier.get()));
    }
}
