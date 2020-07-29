package org.jboss.resteasy.client.jaxrs.internal;

import org.jboss.resteasy.client.jaxrs.RxInvokerSourceID;
import org.jboss.resteasy.client.jaxrs.RxInvokerSourceIdentity;

import java.util.concurrent.ExecutorService;

import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.RxInvokerProvider;
import javax.ws.rs.client.SyncInvoker;

public class CompletionStageRxInvokerProvider implements RxInvokerProvider<CompletionStageRxInvoker>, RxInvokerSourceIdentity
{
   @Override
   public boolean isRxInvokerSource (RxInvokerSourceID id) {
      return !(RxInvokerSourceID.MP_REST_CLIENT == id);
   }

   @Override
   public boolean isProviderFor(Class<?> clazz) {
      return CompletionStageRxInvoker.class.equals(clazz);
   }

   @Override
   public CompletionStageRxInvoker getRxInvoker(SyncInvoker syncInvoker, ExecutorService executorService) {
      return new CompletionStageRxInvokerImpl(syncInvoker, executorService);
   }
}
