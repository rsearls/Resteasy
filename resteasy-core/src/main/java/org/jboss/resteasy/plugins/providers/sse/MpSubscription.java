package org.jboss.resteasy.plugins.providers.sse;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.ws.rs.sse.InboundSseEvent;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MpSubscription implements Subscription  {

    private final MpPublisher publisher;
    private final Subscriber<? super InboundSseEvent> subscriber;
    private final AtomicLong requested = new AtomicLong();
    private final AtomicLong delivered = new AtomicLong();
    private final AtomicBoolean completed = new AtomicBoolean();
    private final AtomicBoolean canceled = new AtomicBoolean();
    //CHECKSTYLE:OFF
    private final LinkedList<InboundSseEvent> buffer = new LinkedList<>(); //NOPMD
    //CHECKSTYLE:ON
    private final AtomicInteger bufferSize = new AtomicInteger(256);


    public MpSubscription(final MpPublisher publisher, final Subscriber<? super InboundSseEvent> subscriber) {
        this.publisher = publisher;
        this.subscriber = subscriber;
    }

    @Override
    public void request(long n) {
        if (canceled.get()) {
            return;
        }
        if (n < 1) {
            fireError(new IllegalArgumentException("Only positive values may be requested - passed-in " + n));
            return;
        }
        requested.addAndGet(n);
        synchronized (buffer) {
            InboundSseEvent bufferedEvent = null;
            synchronized (delivered) {
                while (delivered.get() < requested.get()
                        && (bufferedEvent = buffer.pollFirst()) != null) {

                    subscriber.onNext(bufferedEvent);
                    delivered.incrementAndGet();
                }
            }
        }
    }

    @Override
    public void cancel() {
        canceled.set(true);
        publisher.removeSubscription(this);
    }

    void fireSubscribe() {
        subscriber.onSubscribe(this);
    }

    void fireEvent(InboundSseEvent event) {
        if (completed.get() || canceled.get()) {
            return;
        }
        delivered.updateAndGet(l -> {
            if (l < requested.get()) {
                subscriber.onNext(event);
                return l + 1;
            } else {
                buffer(event);
            }
            return l;
        });

        fireCompleteIfReady();
    }

    void fireCompleteIfReady() {
        if (completed.get() && buffer.isEmpty()) {
            subscriber.onComplete();
        }
    }

    void fireError(Throwable t) {
        if (completed.compareAndSet(false, true)) {
            subscriber.onError(t);
        }
    }

    void setBufferSize(int newSize) {
        bufferSize.set(newSize);
    }

    private void buffer(InboundSseEvent event) {
        synchronized (buffer) {
            buffer.addLast(event);
            if (buffer.size() > bufferSize.get()) {
                buffer.removeFirst();
            }
        }
    }

    static boolean isActive(MpSubscription subscription) {
        return !subscription.completed.get() && !subscription.canceled.get();
    }

    void complete() {
        completed.set(true);
        fireCompleteIfReady();
    }
}
