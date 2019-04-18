package org.jboss.resteasy.statistics;

import org.jboss.resteasy.spi.statistics.MethodStatisticsLogger;

public class MethodStatisticsLoggerImpl implements MethodStatisticsLogger {
   private long invocationCnt = 0;
   private long failureCnt = 0;
   private long totalExecutionTime = 0;
   private int lastResponseCode = 0;

   @Override
   public long timestamp(){
      return System.nanoTime();
   }

   @Override
   public void duration(final long fromTimestamp) {
      // invocation count and execution time are related.
      // Set them together so avgExecutionTime will be calculated correctly
      synchronized (this)
      {
         ++invocationCnt;
         totalExecutionTime += (System.nanoTime() - fromTimestamp);
      }
   }

   @Override
   public void incFailureCnt() {
      ++failureCnt;
   }

   @Override
   public void setLastResponseCode(int code) {
      lastResponseCode = code;
   }

   @Override
   public void reset() {
      synchronized (this)
      {
         invocationCnt = 0;
         failureCnt = 0;
         lastResponseCode = 0;
         totalExecutionTime = 0;
      }
   }

   @Override
   public long getInvocationCnt() {
      return invocationCnt;
   }

   @Override
   public long getFailedInvocationCnt() {
      return failureCnt;
   }

   @Override
   public int getLastResponseCode() {
      return lastResponseCode;
   }

   @Override
   public long getAvgExecutionTime() {
      long avgExecTime;

      try
      {
         synchronized (this)
         {
            avgExecTime = totalExecutionTime / invocationCnt;
         }
      } catch (Exception e) {
         avgExecTime = -1;
      }
      return avgExecTime;
   }

   @Override
   public long getTotalExecutionTime() {
      return totalExecutionTime;
   }
}
