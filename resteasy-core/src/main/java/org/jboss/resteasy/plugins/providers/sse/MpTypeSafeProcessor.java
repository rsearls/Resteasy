package org.jboss.resteasy.plugins.providers.sse;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.sse.InboundSseEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MpTypeSafeProcessor<T> implements Processor<InboundSseEvent, T> {

    private MpSubscription incomingSubscription;
    private final MpPublisher ssePublisher;
    private final List<Subscriber<? super T>> subscribers = new LinkedList<>();
    private final AtomicBoolean isClosed = new AtomicBoolean();
    private final GenericType<?> type;
    private final AtomicBoolean isSubscribed = new AtomicBoolean();

    public MpTypeSafeProcessor(final GenericType<T> type, final MpPublisher ssePublisher) {
        this.type = type;
        this.ssePublisher = ssePublisher;
    }

    @Override
    public void onSubscribe(Subscription s) {
        incomingSubscription = (MpSubscription) s;
        //LOG.finest("onSubscribe " + s);
    }

    @Override
    public void onNext(InboundSseEvent t) {
        //LOG.entering(SseTypeSafeProcessor.class.getName(), "onNext", t);
        if (incomingSubscription == null) {
            throw new IllegalStateException("not subscribed");
        }
        if (!isClosed.get()) {
            @SuppressWarnings("unchecked")
            T data = (T) t.readData(type);
            for (Subscriber<? super T> subscriber : subscribers) {
                subscriber.onNext(data);
            }
        }
        //LOG.exiting(SseTypeSafeProcessor.class.getName(), "onNext");
    }

    @Override
    public void onError(Throwable t) {
        if (isClosed.compareAndSet(false, true)) {
            for (Subscriber<? super T> subscriber : subscribers) {
                subscriber.onError(t);
            }
        }
    }

    @Override
    public void onComplete() {
        if (isClosed.compareAndSet(false, true)) {
            for (Subscriber<? super T> subscriber : subscribers) {
                subscriber.onComplete();
            }
        }
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        //LOG.finest("subscribe " + s);
        subscribers.add(s);
        if (isSubscribed.compareAndSet(false, true)) {
            ssePublisher.subscribe(this);
        }
        if (incomingSubscription == null) {
            throw new IllegalStateException();
        }
        s.onSubscribe(new MpTypeSafeSubscription(incomingSubscription));
    }
}
