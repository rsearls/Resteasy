package org.jboss.resteasy.plugins.providers.sse;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.sse.InboundSseEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class MpPublisher implements Publisher<InboundSseEvent>  {

    final Executor executor;
    final BufferedReader br;
    final Providers providers;
    final List<MpSubscription> subscriptions = new CopyOnWriteArrayList<>();
    final AtomicBoolean isStarted = new AtomicBoolean(false);
    final Annotation[] annotations;
    final MediaType mediaType;
    final  MultivaluedMap<String, String> httpHeaders;
    boolean isUseTwo = false;

    public MpPublisher(final InputStream is, final Executor executor, final Providers providers,
                       final Annotation[] annotations, final MediaType mediaType,
                       final MultivaluedMap<String, String> httpHeaders) {
        br = new BufferedReader(new InputStreamReader(is));
        this.executor = executor;
        this.providers = providers;
        this.annotations = annotations;
        this.mediaType = mediaType;
        this.httpHeaders = httpHeaders;
    }

    //public void useTwo() { isUseTwo = true; }
    @Override
    public void subscribe(Subscriber<? super InboundSseEvent> subscriber) {
        MpSubscription subscription = new MpSubscription(this, subscriber);
        subscriptions.add(subscription);
        subscription.fireSubscribe();

        if (isUseTwo) {
            startTwo();
        } else {
            start();
        }
    }

    private void start() {
        if (isStarted.compareAndSet(false, true)) {
            executor.execute(() -> {
                try (BufferedReader br2 = br) {
                    InboundSseEventImpl.Builder builder = new InboundSseEventImpl.Builder(annotations, mediaType,
                            httpHeaders);
                    String line = br.readLine();
                    boolean isBuilderEmpty = true;
                    while (line != null && !subscriptions.isEmpty()) {
                        if (line.startsWith("data:")) {
                            builder.write(removeSpace(line.substring(5)).getBytes());
                            isBuilderEmpty = false;
                        } else if (line.startsWith("id:")) {
                            builder.id(removeSpace(line.substring(3)));
                            isBuilderEmpty = false;
                        } else if (line.startsWith("event:")) {
                            builder.name(removeSpace(line.substring(6)));
                            isBuilderEmpty = false;
                        } else if (line.startsWith(":")) {
                            builder.commentLine(removeSpace(line.substring(1)));
                            isBuilderEmpty = false;
                        } else if ("".equals(line)) {
                            if (!isBuilderEmpty) {
                                builder.providers(providers);
                                InboundSseEvent event = builder.build();
                                for (MpSubscription subscription : subscriptions) {
                                    subscription.fireEvent(event);
                                }
                                builder = new InboundSseEventImpl.Builder(annotations, mediaType,
                                        httpHeaders);
                                isBuilderEmpty = true;
                            }
                        }
                        line = br.readLine();
                    }
                    for (MpSubscription subscription : subscriptions) {
                        subscription.complete();
                    }
                } catch (IOException ex) {
                    for (MpSubscription subscription : subscriptions) {
                        subscription.fireError(ex);
                    }
                }
            });
        }
    }

    private void startTwo() {
        if (isStarted.compareAndSet(false, true)) {
            executor.execute(() -> {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (BufferedReader br2 = br) {
                    String line = br.readLine();
                    boolean isBuilderEmpty = true;
                    while (line != null && !subscriptions.isEmpty()) {
                        if (line.startsWith("data:")) {
                            isBuilderEmpty = false;
                            out.write(removeSpace(line.substring(5)).getBytes());
                            out.write(SseConstants.EOL);
                        } else if ("".equals(line)) {
                            if (!isBuilderEmpty) {
                                out.write(SseConstants.EOL);
                                ByteArrayInputStream inStream = new ByteArrayInputStream(out.toByteArray());
                                SseEventInputImpl sseEventInputImpl = new SseEventInputImpl(annotations,
                                        mediaType, mediaType, httpHeaders, inStream);

                                InboundSseEvent event = sseEventInputImpl.read();
                                inStream.close();

                                for (MpSubscription subscription : subscriptions) {
                                    subscription.fireEvent(event);
                                }

                                isBuilderEmpty = true;
                                out.close();
                                out = new ByteArrayOutputStream();
                            }
                        }
                        line = br.readLine();
                    }
                    for (MpSubscription subscription : subscriptions) {
                        subscription.complete();
                    }
                } catch (IOException ex) {
                    for (MpSubscription subscription : subscriptions) {
                        subscription.fireError(ex);
                    }
                }
                finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // todo  What to do???
                    }
                }
            });
        }
    }

    void removeSubscription(MpSubscription subscription) {
        subscriptions.remove(subscription);
    }

    private String removeSpace(String s) {
        if (s != null && s.startsWith(" ")) {
            return s.substring(1);
        }
        return s;
    }
}
