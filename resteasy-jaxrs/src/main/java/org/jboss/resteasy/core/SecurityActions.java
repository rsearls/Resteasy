package org.jboss.resteasy.core;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * User: rsearls
 * Date: 4/11/17
 */
class SecurityActions
{
    /**
     * Get context classloader.
     *
     * @return the current context classloader
     */
    static ClassLoader getContextClassLoader()
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null)
        {
            return Thread.currentThread().getContextClassLoader();
        }
        else
        {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run()
                {
                    return Thread.currentThread().getContextClassLoader();
                }
            });
        }
    }

    /**
     * Set context classloader.
     *
     * @param classLoader the classloader
     */
    static void setContextClassLoader(final ClassLoader classLoader)
    {
        if (System.getSecurityManager() == null)
        {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
        else
        {
            AccessController.doPrivileged(new PrivilegedAction<Object>()
            {
                public Object run()
                {
                    Thread.currentThread().setContextClassLoader(classLoader);
                    return null;
                }
            });
        }
    }

}
