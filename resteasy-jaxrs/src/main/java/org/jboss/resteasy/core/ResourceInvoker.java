package org.jboss.resteasy.core;

import org.jboss.resteasy.specimpl.BuiltResponse;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;

import java.lang.reflect.Method;
import java.security.PrivilegedActionException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ResourceInvoker
{
   BuiltResponse invoke(HttpRequest request, HttpResponse response) throws PrivilegedActionException;
   BuiltResponse invoke(HttpRequest request, HttpResponse response, Object target) throws PrivilegedActionException;

   Method getMethod();
}
