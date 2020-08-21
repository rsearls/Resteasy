package org.jboss.resteasy.plugins.providers.sse;

import org.reactivestreams.Subscription;

public class MpTypeSafeSubscription implements Subscription {

    private final MpSubscription sseSubscription;

    MpTypeSafeSubscription(final MpSubscription sseSubscription) {
        this.sseSubscription = sseSubscription;
    }

    @Override
    public void request(long n) {
        if (n < 1) {
            throw new IllegalArgumentException("Only postive values are valid - passed-in " + n);
        }
        sseSubscription.request(n);
    }

    @Override
    public void cancel() {
        sseSubscription.cancel();
    }
}
