package org.jboss.resteasy.sse;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import org.jboss.resteasy.sse.FactoryFinder;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventInput;
import javax.ws.rs.sse.SseEventSource;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jboss.resteasy.resteasy_sse.i18n.LogMessages;
import org.jboss.resteasy.resteasy_sse.i18n.Messages;
import org.jboss.resteasy.util.ThreadFactoryBuilder;
import org.jboss.resteasy.sse.SseFeature;

/**
 * User: rsearls
 * Date: 5/5/16
 */
public class ResteasySseEventSource implements SseEventSource {

    /**
     * Default SSE {@link SseEventSource} reconnect delay value in milliseconds.
     *
     * @since 2.3
     */
    public static final long RECONNECT_DEFAULT = 500;

    private enum State {
        READY("ready"),
        OPEN("open"),
        CLOSED("closed");

        private String description;

        private State(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * SSE streaming resource target.
     */
    private final WebTarget target;
    /**
     * Default reconnect delay.
     */
    private final long reconnectDelay;
    /**
     * Flag indicating if the persistent HTTP connections should be disabled.
     */
    private final boolean disableKeepAlive;
    /**
     * Incoming SSE event processing task executor.
     */
    private final ScheduledExecutorService executor;
    /**
     * Event source internal state.
     */
    private final AtomicReference<State> state = new AtomicReference<>(State.READY);
    /**
     * List of all listeners not bound to receive only events of a particular name.
     */
    private final List<SseEventSource.Listener> unboundListeners = new CopyOnWriteArrayList<>();
    /**
     * A map of listeners bound to receive only events of a particular name.
     */
    private final ConcurrentMap<String, List<SseEventSource.Listener>> boundListeners = new ConcurrentHashMap<>();


    /*************************************************************************/
    /******************** interface Listener method impl *********************/
    /**
     * {@code SseEventSource} listener that can be registered to listen for newly received
     * {@link InboundSseEvent} notifications.
     */
    /**
     * Called when a new {@link InboundSseEvent} is received by an event source.
     *
     * @param event received inbound SSE event.
     */
    public void onEvent(InboundSseEvent event) {
        //todo fill out
    }


    /*************************************************************************/
    /********************* abstract class Builder impl ***********************/
    /**
     * JAX-RS {@link SseEventSource} builder class.
     * <p>
     * Event source builder provides methods that let you conveniently configure and subsequently build
     * a new {@code EventSource} instance. You can obtain a new event source builder instance using
     * a static {@link SseEventSource#target(javax.ws.rs.client.WebTarget) EventSource.target(endpoint)} factory method.
     * <p>
     * For example:
     * <pre>
     * EventSource es = EventSource.target(endpoint).named("my source")
     *                             .reconnectingEvery(5, SECONDS)
     *                             .open();
     * </pre>
     * </p>
     */
    static class Builder extends SseEventSource.Builder {

        /**
         * Name of the property identifying the {@link SseEventSource.Builder} implementation
         * to be returned from {@link SseEventSource.Builder#newBuilder()}.
         */
        public static final String JAXRS_DEFAULT_SSE_BUILDER_PROPERTY =
            "javax.ws.rs.sse.SseEventSource.Builder";
        /**
         * Default SSE event source builder implementation class name.
         */
        private static final String JAXRS_DEFAULT_SSE_BUILDER =
            "org.jboss.resteasy.sse.ResteasySseEventSource$Builder";


        private WebTarget endpoint;
        private long reconnect = ResteasySseEventSource.RECONNECT_DEFAULT;
        private String name = null;
        private boolean disableKeepAlive = true;

        /**
         * List of all listeners not bound to receive only events of a particular name.
         */
        private final List<SseEventSource.Listener> bUnboundListeners = new CopyOnWriteArrayList<>();
        /**
         * A map of listeners bound to receive only events of a particular name.
         */
        private final ConcurrentMap<String, List<SseEventSource.Listener>> bBoundListeners = new ConcurrentHashMap<>();


        /**
         * Allows custom implementations to extend the SSE event source builder class.
         */
        protected Builder() {
        }

        /**
         * Create a new SSE event source instance using the default implementation class provided by the JAX-RS
         * implementation provider.
         *
         * @return new SSE event source builder instance.
         */
        static Builder newBuilder() {

            try {
                Object delegate = FactoryFinder.find(JAXRS_DEFAULT_SSE_BUILDER_PROPERTY,
                    JAXRS_DEFAULT_SSE_BUILDER);
                if (!(delegate instanceof Builder)) {
                    Class pClass = Builder.class;
                    String classnameAsResource = pClass.getName().replace('.', '/') + ".class";
                    ClassLoader loader = pClass.getClassLoader();
                    if (loader == null) {
                        loader = ClassLoader.getSystemClassLoader();
                    }
                    URL targetTypeURL = loader.getResource(classnameAsResource);
                    throw new LinkageError(Messages.MESSAGES.attemptToCastClassError(
                       classnameAsResource, targetTypeURL.toString()));
                }
                return (Builder) delegate;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        protected Builder target(WebTarget endpoint) {
            // todo add code
            this.endpoint = endpoint;
            return this;
        }

        /**
         * Set a custom name for the event source.
         * <p>
         * At present, custom event source name is mainly useful to be able to distinguish different event source
         * event processing threads from one another. If not set, a default name will be generated using the
         * SSE endpoint URI.
         * </p>
         *
         * @param name custom event source name.
         * @return updated event source builder instance.
         */
        public Builder named(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the initial reconnect delay to be used by the event source.
         * <p>
         * Note that this value may be later overridden by the SSE endpoint using either a {@code retry} SSE event field
         * or <tt>HTTP 503 + {@value javax.ws.rs.core.HttpHeaders#RETRY_AFTER}</tt> mechanism as described
         * in the {@link SseEventSource} javadoc.
         * </p>
         *
         * @param delay the default time to wait before attempting to recover from a connection loss.
         * @param unit  time unit of the reconnect delay parameter.
         * @return updated event source builder instance.
         */
        public Builder reconnectingEvery(long delay, TimeUnit unit) {
            reconnect = unit.toMillis(delay);
            return this;
        }

        /**
         * Register new {@link SseEventSource.Listener event listener} to receive all streamed {@link InboundSseEvent SSE events}.
         *
         * @param listener event listener to be registered with the event source.
         * @see #register(SseEventSource.Listener, String, String...)
         */
        public Builder register(SseEventSource.Listener listener) {
            return register(listener, null);
        }

        /**
         * Add name-bound {@link SseEventSource.Listener event listener} which will be called only for incoming SSE
         * {@link InboundSseEvent events} whose {@link InboundSseEvent#getName() name} is equal to the specified
         * name(s).
         *
         * @param listener   event listener to register with this event source.
         * @param eventName  inbound event name.
         * @param eventNames additional event names.
         * @see #register(SseEventSource.Listener)
         */
        public Builder register(SseEventSource.Listener listener, String eventName, String... eventNames) {

            if (eventName == null) {
                bUnboundListeners.add(listener);
            } else {
                addBoundListener(eventName, listener);

                if (eventNames != null) {
                    for (String name : eventNames) {
                        addBoundListener(name, listener);
                    }
                }
            }
            return this;
        }

        private void addBoundListener(final String name, final SseEventSource.Listener listener) {
            List<SseEventSource.Listener> listeners = bBoundListeners.putIfAbsent(name,
               new CopyOnWriteArrayList<>(Collections.singleton(listener)));
            if (listeners != null) {
                // alas, new listener collection registration conflict:
                // need to add the new listener to the existing listener collection
                listeners.add(listener);
            }
        }

        /**
         * Build new SSE event source pointing at a SSE streaming {@link WebTarget web target}.
         * <p>
         * The returned event source is ready, but not {@link SseEventSource#open() connected} to the SSE endpoint.
         * It is expected that you will manually invoke its {@link #open()} method once you are ready to start
         * receiving SSE events. In case you want to build an event source instance that is already connected
         * to the SSE endpoint, use the event source builder {@link #open()} method instead.
         * </p>
         * <p>
         * Once the event source is open, the incoming events are processed by the event source in an
         * asynchronous task that runs in an internal single-threaded {@link ScheduledExecutorService
         * scheduled executor service}.
         * </p>
         *
         * @return new event source instance, ready to be connected to the SSE endpoint.
         * @see #open()
         */
        public SseEventSource build() {
            return new ResteasySseEventSource(endpoint, name, reconnect,
               disableKeepAlive, false, bUnboundListeners, bBoundListeners);
        }

        /**
         * Build new SSE event source pointing at a SSE streaming {@link WebTarget web target}.
         * <p>
         * The returned event source is already {@link SseEventSource#open() connected} to the SSE endpoint
         * and is processing any new incoming events. In case you want to build an event source instance
         * that is already ready, but not automatically connected to the SSE endpoint, use the event source
         * builder {@link #build()} method instead.
         * </p>
         * <p>
         * The incoming events are processed by the event source in an asynchronous task that runs in an
         * internal single-threaded {@link ScheduledExecutorService scheduled executor service}.
         * </p>
         *
         * @return new event source instance, already connected to the SSE endpoint.
         * @see #build()
         */
        public SseEventSource open() {
            final SseEventSource source = new ResteasySseEventSource(endpoint,
               name, reconnect, disableKeepAlive, false, bUnboundListeners, bBoundListeners);
            source.open();
            return source;
        }
    }

    /**
     * Create new SSE event source pointing at a SSE streaming {@link WebTarget web target}.
     *
     * This constructor is performs the same series of actions as a call to:
     * <pre>
     * if (open) {
     *     EventSource.target(endpoint).open();
     * } else {
     *     EventSource.target(endpoint).build();
     * }</pre>
     * <p>
     * If the supplied {@code open} flag is {@code true}, the created event source instance automatically
     * {@link #open opens a connection} to the supplied SSE streaming web target and starts processing incoming
     * {@link InboundSseEvent events}.
     * Otherwise, if the {@code open} flag is set to {@code false}, the created event source instance
     * is not automatically connected to the web target. In this case it is expected that the user who
     * created the event source will manually invoke its {@link #open()} method.
     * </p>
     * <p>
     * Once the event source is open, the incoming events are processed by the event source in an
     * asynchronous task that runs in an internal single-threaded {@link ScheduledExecutorService
     * scheduled executor service}.
     * </p>
     *
     * @param endpoint SSE streaming endpoint. Must not be {@code null}.
     * @param open     if {@code true}, the event source will immediately connect to the SSE endpoint,
     *                 if {@code false}, the connection will not be established until {@link #open()} method is
     *                 called explicitly on the event stream.
     * @throws NullPointerException in case the supplied web target is {@code null}.
     */
    public ResteasySseEventSource(final WebTarget endpoint, final boolean open) {
        this(endpoint, null, RECONNECT_DEFAULT, true, open, null, null);
    }

    private ResteasySseEventSource(final WebTarget target,
                        final String name,
                        final long reconnectDelay,
                        final boolean disableKeepAlive,
                        final boolean open,
                        final List<SseEventSource.Listener> unboundListeners,
                        final ConcurrentMap<String, List<SseEventSource.Listener>> boundListeners) {
        if (target == null) {
            throw new NullPointerException(Messages.MESSAGES.webTargeIsNull());
        }
        this.target = SseFeature.register(target); // todo figure out what to do with this.
        this.reconnectDelay = reconnectDelay;
        this.disableKeepAlive = disableKeepAlive;

        if (unboundListeners != null && !unboundListeners.isEmpty()) {
            this.unboundListeners.addAll(unboundListeners);
        }

        if (boundListeners != null && !boundListeners.isEmpty()) {
            this.boundListeners.putAll(boundListeners);
        }

        final String esName = (name == null) ? createDefaultName(target) : name;
        this.executor = Executors.newSingleThreadScheduledExecutor(
           new ThreadFactoryBuilder().setNameFormat(esName + "-%d").setDaemon(true).build());

        if (open) {
            open();
        }
    }

    private static String createDefaultName(WebTarget target) {
        return String.format("resteasy-sse-event-source-[%s]", target.getUri().toASCIIString());
    }

    /***************************************************************************/
    /******************* interface SseEventSource method impl ******************/

    /**
     * Create a new {@link SseEventSource.Builder event source builder} that provides convenient way how to
     * configure and fine-tune various aspects of a newly prepared event source instance.
     *
     * @param endpoint SSE streaming endpoint. Must not be {@code null}.
     * @return a builder of a new event source instance pointing at the specified SSE streaming endpoint.
     * @throws NullPointerException in case the supplied web target is {@code null}.
     */
    public static Builder target(WebTarget endpoint) {
        return Builder.newBuilder().target(endpoint);
    }

    /**
     * Open the connection to the supplied SSE underlying {@link WebTarget web target} and start processing incoming
     * {@link InboundSseEvent events}.
     *
     * @throws IllegalStateException in case the event source has already been opened earlier.
     */
    public void open() {
        if (!state.compareAndSet(State.READY, State.OPEN)) {
            switch (state.get()) {
                case OPEN:
                    throw new IllegalStateException(Messages.MESSAGES.eventSourceAlreadyConnected());
                case CLOSED:
                    throw new IllegalStateException(Messages.MESSAGES.eventSourceAlreadyClosed());
            }
        }

        EventProcessor processor = new EventProcessor(reconnectDelay, null);
        executor.submit(processor);

        // return only after the first request to the SSE endpoint has been made
        processor.awaitFirstContact();
    }

    /**
     * Check if this event source instance has already been {@link #open() opened}.
     *
     * @return {@code true} if this event source is open, {@code false} otherwise.
     */
    public boolean isOpen() {
        return State.OPEN == state.get();
    }

    /**
     * Close this event source.
     * <p>
     * The method will wait up to 5 seconds for the internal event processing tasks to complete.
     */
    public /*default*/ void close() {
        close(5, TimeUnit.SECONDS);
    }

    /**
     * Close this event source and wait for the internal event processing task to complete
     * for up to the specified amount of wait time.
     * <p>
     * The method blocks until the event processing task has completed execution after a shutdown
     * request, or until the timeout occurs, or the current thread is interrupted, whichever happens
     * first.
     * </p>
     * <p>
     * In case the waiting for the event processing task has been interrupted, this method restores
     * the {@link Thread#interrupted() interrupt} flag on the thread before returning {@code false}.
     * </p>
     *
     * @param timeout the maximum time to wait.
     * @param unit    the time unit of the timeout argument.
     * @return {@code true} if this executor terminated and {@code false} if the timeout elapsed
     * before termination or the termination was interrupted.
     */
    public boolean close(final long timeout, final TimeUnit unit) {
        shutdown();
        try {
            if (!executor.awaitTermination(timeout, unit)) {
                LogMessages.LOGGER.eventSourceTimeoutError(target.getUri().toString());
                return false;
            }
        } catch (InterruptedException e) {
            LogMessages.LOGGER.eventShutdownInterruptedError(target.getUri().toString());
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }

    private void shutdown() {
        if (state.getAndSet(State.CLOSED) != State.CLOSED) {
            // shut down only if has not been shut down before
            LogMessages.LOGGER.shuttingDownEventProcessingDebug();
            executor.shutdownNow();
        }
    }

    /**************************************************************************/
    /********************** EventProcessor class impl *************************/
    /**
     * Private event processor task responsible for connecting to the SSE stream and processing
     * incoming SSE events as well as handling any connection issues.
     */
    private class EventProcessor implements Runnable, /*EventListener*/ SseEventSource.Listener {

        /**
         * Open connection response arrival synchronization latch.
         */
        private final CountDownLatch firstContactSignal;
        /**
         * Last received event id.
         */
        private String lastEventId;
        /**
         * Re-connect delay.
         */
        private long reconnectDelay;

        public EventProcessor(final long reconnectDelay, final String lastEventId) {
            /**
             * Synchronization barrier used to signal that the initial contact with SSE endpoint
             * has been made.
             */
            this.firstContactSignal = new CountDownLatch(1);

            this.reconnectDelay = reconnectDelay;
            this.lastEventId = lastEventId;
        }

        private EventProcessor(final EventProcessor that) {
            this.firstContactSignal = null;

            this.reconnectDelay = that.reconnectDelay;
            this.lastEventId = that.lastEventId;
        }

        @Override
        public void run() {
            LogMessages.LOGGER.listenerTaskStartedDebug();

            SseEventInput eventInput = null;
            try {
                try {
                    final Invocation.Builder request = prepareHandshakeRequest();
                    if (state.get() == State.OPEN) { // attempt to connect only if even source is open
                        LogMessages.LOGGER.connectingDebug();
                        eventInput = request.get(SseEventInput.class);
                        LogMessages.LOGGER.connectedDebug();
                    }
                } finally {
                    if (firstContactSignal != null) {
                        // release the signal regardless of event source state or connection request outcome
                        firstContactSignal.countDown();
                    }
                }

                final Thread execThread = Thread.currentThread();

                while (state.get() == State.OPEN && !execThread.isInterrupted()) {
                    if (eventInput == null || eventInput.isClosed()) {
                        LogMessages.LOGGER.connectionLostSchedulingReconnectDebug(reconnectDelay);
                        scheduleReconnect(reconnectDelay);
                        break;
                    } else {
                        this.onEvent(eventInput.read());
                    }
                }
            } catch (ServiceUnavailableException ex) {
                LogMessages.LOGGER.retreivedHTTP503Debug();
                long delay = reconnectDelay;
                if (ex.hasRetryAfter()) {
                    LogMessages.LOGGER.recoveringFromHTTP503Debug();
                    final Date requestTime = new Date();
                    delay = ex.getRetryTime(requestTime).getTime() - requestTime.getTime();
                    delay = (delay > 0) ? delay : 0;
                }

                LogMessages.LOGGER.recoveringFromHTTP503SchedulingReconnecDebug(delay);
                scheduleReconnect(delay);
            } catch (Exception ex) {
                //if (LOGGER.isLoggable(CONNECTION_ERROR_LEVEL)) {
                    LogMessages.LOGGER.unableToConnectClosingSourceError(target.getUri().toASCIIString(), ex);
                //}
                // if we're here, an unrecoverable error has occurred - just turn off the lights...
                ResteasySseEventSource.this.shutdown();
            } finally {
                if (eventInput != null && !eventInput.isClosed()) {
                    try {
                        eventInput.close();
                    } catch (IOException e) {
                        LogMessages.LOGGER.closingEventInputError(e);
                    }
                }
                LogMessages.LOGGER.listenerTaksFinishedDebug();
            }
        }

        /**
         * Called by the event source when an inbound event is received.
         *
         * This listener aggregator method is responsible for invoking {TODO fix this @link EventSource#onEvent(InboundSseEvent)}
         * method on the owning event source as well as for notifying all registered {@link Listener event listeners}.
         *
         * @param event incoming {@link InboundSseEvent inbound event}.
         */
        public void onEvent(final InboundSseEvent event) {
            if (event == null) {
                return;
            }

            LogMessages.LOGGER.newEventReceivedDebug();

            if (event.getId() != null) {
                lastEventId = event.getId();
            }
            if (event.isReconnectDelaySet()) {
                reconnectDelay = event.getReconnectDelay();
            }

            notify((Listener)ResteasySseEventSource.this, event);
            //notify(ResteasySseEventSource.Listener.this, event);
            notify(unboundListeners, event);

            final String eventName = event.getName();
            if (eventName != null) {
                final List<SseEventSource.Listener> eventListeners = boundListeners.get(eventName);
                if (eventListeners != null) {
                    notify(eventListeners, event);
                }
            }
        }

        private void notify(final Collection<SseEventSource.Listener> listeners, final InboundSseEvent event) {
            for (SseEventSource.Listener listener : listeners) {
                notify(listener, event);
            }
        }

        private void notify(final SseEventSource.Listener listener, final InboundSseEvent event) {
            try {
                listener.onEvent(event);
            } catch (Exception ex) {
                //if (LOGGER.isLoggable(Level.FINE)) {
                    LogMessages.LOGGER.eventNotificationInListenerFailedError(listener.getClass().getName(), ex);
                //}
            }
        }

        /**
         * Schedule a new event processor task to reconnect after the specified {@code delay} [milliseconds].
         *
         * If the {@code delay} is zero or negative, the new reconnect task will be scheduled immediately.
         * The {@code reconnectDelay} and {@code lastEventId} field values are propagated into the newly
         * scheduled task.
         * <p>
         * The method will silently abort in case the event source is not {TODO fix this @link EventSource#isOpen() open}.
         * </p>
         *
         * @param delay specifies the amount of time [milliseconds] to wait before attempting a reconnect.
         *              If zero or negative, the new reconnect task will be scheduled immediately.
         */
        private void scheduleReconnect(final long delay) {
            final State s = state.get();
            if (s != State.OPEN) {
                LogMessages.LOGGER.abortingReconnectDebug(s.getDescription());
                return;
            }

            // propagate the current reconnectDelay, but schedule based on the delay parameter
            final EventProcessor processor = new EventProcessor(this);
            if (delay > 0) {
                executor.schedule(processor, delay, TimeUnit.MILLISECONDS);
            } else {
                executor.submit(processor);
            }
        }

        private Invocation.Builder prepareHandshakeRequest() {
            final Invocation.Builder request = target.request(SseFeature.SERVER_SENT_EVENTS_TYPE);
            if (lastEventId != null && !lastEventId.isEmpty()) {
                request.header(SseFeature.LAST_EVENT_ID_HEADER, lastEventId);
            }
            if (disableKeepAlive) {
                request.header("Connection", "close");
            }
            return request;
        }

        /**
         * Await the initial contact with the SSE endpoint.
         */
        public void awaitFirstContact() {
            LogMessages.LOGGER.waitingFirstContactSignalDebug();
            try {
                if (firstContactSignal == null) {
                    return;
                }

                try {
                    firstContactSignal.await();
                } catch (InterruptedException ex) {
                    LogMessages.LOGGER.eventSourceOpenConnectionInterruptedError(ex);
                    Thread.currentThread().interrupt();
                }
            } finally {
                LogMessages.LOGGER.firstContactSignalReleasedDebug();
            }
        }
    }
}
