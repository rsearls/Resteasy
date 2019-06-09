package org.jboss.resteasy.plugins.server.embedded;

import org.jboss.resteasy.spi.ResteasyDeployment;

public interface EMBEDDEDJaxrsServer <T> {
   T deploy();
   T start();
   void stop();
   ResteasyDeployment getDeployment();
   T setDeployment(ResteasyDeployment deployment);
   //int getPort();
   T setPort(int port);
   //String getHostname();
   T setHostname(String hostname);
   //String getContextPath();
   //T setContextPath(String contextPath);
    T setRootResourcePath(String rootResourcePath);
    T setSecurityDomain(SecurityDomain sc);
}
