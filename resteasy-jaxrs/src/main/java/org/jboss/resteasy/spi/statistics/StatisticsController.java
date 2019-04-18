package org.jboss.resteasy.spi.statistics;

import org.jboss.resteasy.core.ResourceInvoker;

public interface StatisticsController {
   void register(ResourceInvoker invoker);
   void setEnabled(boolean b);
   void reset();
   String getStatistics();
}
