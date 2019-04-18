package org.jboss.resteasy.statistics;

import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.spi.statistics.MethodStatisticsLogger;
import org.jboss.resteasy.spi.statistics.StatisticsController;

import java.util.ArrayList;
import java.util.List;

public class StatisticsControllerImpl implements StatisticsController {

   // When statistics collection is OFF use the no-op's methods.
   public static final MethodStatisticsLogger EMPTY = new MethodStatisticsLogger() {
      // use all no-op methods
   };

   boolean isEnabled = true; // false;
   List<ResourceInvoker> registry = new ArrayList<>();

   @Override
   public void register(ResourceInvoker invoker) {
      if(isEnabled) {
         invoker.setMethodStatisticsLogger(new MethodStatisticsLoggerImpl());
      }
      registry.add(invoker);
   }

   @Override
   public void setEnabled(boolean b) {
      if (isEnabled == b)
      {
         return;
      }

      isEnabled = b;
      if (isEnabled) {
         for (ResourceInvoker invoker : registry) {
            invoker.setMethodStatisticsLogger(new MethodStatisticsLoggerImpl());
         }
      } else {
         for (ResourceInvoker invoker : registry) {
            invoker.setMethodStatisticsLogger(EMPTY);
         }
      }
   }

   @Override
   public void reset() {
      for (ResourceInvoker invoker : registry) {
         invoker.getMethodStatisticsLogger().reset();
      }
   }

   @Override
   public String getStatistics() {
      if (isEnabled) {
        return getStats();
      }
      return null;
   }

   private String getStats() {
      StringBuilder sb = new StringBuilder();
      for(ResourceInvoker invoker : registry) {
         MethodStatisticsLogger msl = invoker.getMethodStatisticsLogger();
         sb.append(invoker.getMethod().getName() + "\n");
         long cnt = msl.getInvocationCnt();
         sb.append("    invocation cnt: " + cnt + "\n");
         if (cnt > 0) {
            sb.append("    invocation failure cnt: " + msl.getFailedInvocationCnt() + "\n");
            sb.append("    average execution time: " + msl.getAvgExecutionTime() + "\n");
            sb.append("    total execution time: " + msl.getTotalExecutionTime() + "\n");
         }
         //sb.append("\n");
      }
      return sb.toString();
   }
}
