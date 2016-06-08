package org.jboss.resteasy.plugins.providers.sse;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventInput;
import javax.ws.rs.sse.SseEventSource;

public class SseEventSourceImpl implements SseEventSource
{

   public static final long RECONNECT_DEFAULT = 500;

   private enum State {
      READY, OPEN, CLOSED
   }

   private WebTarget target = null;

   private final long reconnectDelay;

   private final boolean disableKeepAlive;

   private final ScheduledExecutorService executor;

   private final AtomicReference<State> state = new AtomicReference<>(State.READY);

   private final List<Listener> unboundListeners = new CopyOnWriteArrayList<>();

   private final ConcurrentMap<String, List<Listener>> boundListeners = new ConcurrentHashMap<>();

  

   public static class SourceBuilder extends Builder
   {

      private WebTarget endpoint = null;

      private long reconnect = 500;

      private String name = null;

      private boolean disableKeepAlive = false;

      public SourceBuilder(final WebTarget endpoint)
      {
         this.endpoint = endpoint;
      }

      public Builder named(String name)
      {
         this.name = name;
         return this;
      }

      public SseEventSource build()
      {
         return new SseEventSourceImpl(endpoint, name, reconnect, disableKeepAlive, false);
      }

      public SseEventSource open()
      {
         //TODO: why this api is required ? build can create SseEventSource and this can be invoked against SseEventSource
         final SseEventSource source = new SseEventSourceImpl(endpoint, name, reconnect, disableKeepAlive, false);
         source.open();
         return source;
      }

      @Override
      public Builder target(WebTarget endpoint)
      {
         return new SourceBuilder(endpoint);
      }

      @Override
      public Builder register(Listener listener)
      {
         //TODO: this api should be revised
         return this;
      }

      @Override
      public Builder register(Listener listener, String eventName, String... eventNames)
      {
         //TODO: this api should be revised
         return this;
      }
      @Override
      public Builder reconnectingEvery(long delay, TimeUnit unit)
      {
         reconnect = unit.toMillis(delay);
         return this;
      }
   }

   public SseEventSourceImpl(final WebTarget endpoint)
   {
      this(endpoint, true);
   }

   public SseEventSourceImpl(final WebTarget endpoint, final boolean open)
   {
      this(endpoint, null, RECONNECT_DEFAULT, true, open);
   }

   private SseEventSourceImpl(final WebTarget target, final String name, final long reconnectDelay,
         final boolean disableKeepAlive, final boolean open)
   {
      if (target == null)
      {
         throw new NullPointerException("WebTarget is 'null'.");
      }
      this.target = target;
      this.reconnectDelay = reconnectDelay;
      this.disableKeepAlive = disableKeepAlive;

      final String esName = (name == null) ? createDefaultName(target) : name;
      this.executor = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());

      if (open)
      {
         open();
      }
   }

   private static class DaemonThreadFactory implements ThreadFactory
   {

      private static final AtomicInteger poolNumber = new AtomicInteger(1);

      private final ThreadGroup group;

      private final AtomicInteger threadNumber = new AtomicInteger(1);

      private final String namePrefix;

      DaemonThreadFactory()
      {
         SecurityManager s = System.getSecurityManager();
         group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
         namePrefix = "resteasy-sse-eventsource" + poolNumber.getAndIncrement() + "-thread-";
      }

      public Thread newThread(Runnable r)
      {
         Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
         t.setDaemon(true);
         return t;
      }
   }

   private static String createDefaultName(WebTarget target)
   {
      return String.format("sse-event-source(%s)", target.getUri().toASCIIString());
   }

   @Override
   public void open()
   {
      if (!state.compareAndSet(State.READY, State.OPEN))
      {
         switch (state.get())
         {
            case OPEN :
               throw new IllegalStateException("Event source is already opened");
            case CLOSED :
               throw new IllegalStateException("Event source is already closed");
         }
      }

      EventHandler processor = new EventHandler(reconnectDelay, null);
      executor.submit(processor);

      // return only after the first request to the SSE endpoint has been made
      processor.awaitFirstContact();
   }

   public boolean isOpen()
   {
      return state.get() == State.OPEN;
   }

   public void register(final Listener listener)
   {
      register(listener, null);
   }

   public void register(final Listener listener, final String eventName, final String... eventNames)
   {
      if (eventName == null)
      {
         unboundListeners.add(listener);
      }
      else
      {
         addBoundListener(eventName, listener);

         if (eventNames != null)
         {
            for (String name : eventNames)
            {
               addBoundListener(name, listener);
            }
         }
      }
   }

   private void addBoundListener(final String name, final Listener listener)
   {
      List<Listener> listeners = boundListeners.putIfAbsent(name,
            new CopyOnWriteArrayList<>(Collections.singleton(listener)));
      if (listeners != null)
      {
         listeners.add(listener);
      }
   }

   @Override
   public void close()
   {
      close(5, TimeUnit.SECONDS);
   }

   @Override
   public boolean close(final long timeout, final TimeUnit unit)
   {
      shutdown();
      try
      {
         if (!executor.awaitTermination(timeout, unit))
         {
            return false;
         }
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
         return false;
      }
      return true;
   }

   private void shutdown()
   {
      if (state.getAndSet(State.CLOSED) != State.CLOSED)
      {
         //TODO:log 
         executor.shutdownNow();
      }
   }

   private class EventHandler implements Runnable, Listener
   {

      private final CountDownLatch firstContactSignal;

      private String lastEventId;

      /**
       * Re-connect delay.
       */
      private long reconnectDelay;

      public EventHandler(final long reconnectDelay, final String lastEventId)
      {
         /**
          * Synchronization barrier used to signal that the initial contact with SSE endpoint
          * has been made.
          */
         this.firstContactSignal = new CountDownLatch(1);

         this.reconnectDelay = reconnectDelay;
         this.lastEventId = lastEventId;
      }

      private EventHandler(final EventHandler that)
      {
         this.firstContactSignal = null;

         this.reconnectDelay = that.reconnectDelay;
         this.lastEventId = that.lastEventId;
      }

      @Override
      public void run()
      {
         SseEventInput eventInput = null;
         try
         {
            try
            {
               final Invocation.Builder request = prepareHandshakeRequest();
               if (state.get() == State.OPEN)
               { // attempt to connect only if even source is open
                  eventInput = request.get(SseEventInput.class);

               }
            } catch (Exception e) {
               //TODO:handle this
            }
            finally
            {
               if (firstContactSignal != null)
               {
                  firstContactSignal.countDown();
               }
            }

            final Thread execThread = Thread.currentThread();

            while (state.get() == State.OPEN && !execThread.isInterrupted())
            {
               if (eventInput == null || eventInput.isClosed())
               {
                  scheduleReconnect(reconnectDelay);
                  break;
               }
               else
               {  
            	  InboundSseEvent event = eventInput.read();
            	  //TODO:remove this check 
                  if (event != null)
                  {
                     this.onEvent(event);
                  }
               }
            }
         }
         catch (ServiceUnavailableException ex)
         {

            long delay = reconnectDelay;
            if (ex.hasRetryAfter())
            {
               final Date requestTime = new Date();
               delay = ex.getRetryTime(requestTime).getTime() - requestTime.getTime();
               delay = (delay > 0) ? delay : 0;
            }
            scheduleReconnect(delay);
         }
         catch (Exception ex)
         {
            ex.printStackTrace();
            shutdown();
         }
         finally
         {
            if (eventInput != null && !eventInput.isClosed())
            {
               try
               {
                  eventInput.close();
               }
               catch (IOException e)
               {
                  //
               }
            }
         }
      }

      private void scheduleReconnect(final long delay)
      {
         final State s = state.get();
         if (s != State.OPEN)
         {
            return;
         }

         final EventHandler processor = new EventHandler(this);
         if (delay > 0)
         {
            executor.schedule(processor, delay, TimeUnit.MILLISECONDS);
         }
         else
         {
            executor.submit(processor);
         }
      }

      private Invocation.Builder prepareHandshakeRequest()
      {
         final Invocation.Builder request = target.request(SseConstants.SERVER_SENT_EVENTS_TYPE);
         if (lastEventId != null && !lastEventId.isEmpty())
         {
            request.header(SseConstants.LAST_EVENT_ID_HEADER, lastEventId);
         }
         if (disableKeepAlive)
         {
            request.header("Connection", "close");
         }
         return request;
      }

      public void awaitFirstContact()
      {
         try
         {
            if (firstContactSignal == null)
            {
               return;
            }

            try
            {
               firstContactSignal.await();
            }
            catch (InterruptedException ex)
            {
               Thread.currentThread().interrupt();
            }
         }
         finally
         {
           //
         }
      }

      @Override
      public void onEvent(final InboundSseEvent event) {
          if (event == null) {
              return;
          }
          if (event.getId() != null) {
              lastEventId = event.getId();
          }
          if (event.isReconnectDelaySet()) {
              reconnectDelay = event.getReconnectDelay();
          }         
          final String eventName = event.getName();
          if (eventName != null) {
              final List<Listener> eventListeners = boundListeners.get(eventName);
              if (eventListeners != null) {
                  notify(eventListeners, event);
              }
          }
          notify(unboundListeners, event);
      }   

      private void notify(final Collection<Listener> listeners, final InboundSseEvent event) {
         if (listeners != null)
         {
            for (Listener listener : listeners)
            {
               notify(listener, event);
            }
         }
      }

      private void notify(final Listener listener, final InboundSseEvent event) {
          try {
              listener.onEvent(event);
          } catch (Exception ex) {
              //TODO:Log
          }
      }
   }
}
