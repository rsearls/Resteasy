package org.jboss.resteasy.sse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: rsearls
 * Date: 5/11/16
 */
final class  FactoryFinder {  // TODO  is there a different way to do this??

   private static final Logger LOGGER = Logger.getLogger(FactoryFinder.class.getName());

   private FactoryFinder() {
      // prevents instantiation
   }

   static ClassLoader getContextClassLoader() {
      return AccessController.doPrivileged(
         new PrivilegedAction<ClassLoader>() {

            @Override
            public ClassLoader run() {
               ClassLoader cl = null;
               try {
                  cl = Thread.currentThread().getContextClassLoader();
               } catch (SecurityException ex) {
                  LOGGER.log(
                     Level.WARNING,
                     "Unable to get context classloader instance.",
                     ex);
               }
               return cl;
            }
         });
   }

   /**
    * Creates an instance of the specified class using the specified
    * {@code ClassLoader} object.
    *
    * @param className   name of the class to be instantiated.
    * @param classLoader class loader to be used.
    * @return instance of the specified class.
    * @throws ClassNotFoundException if the given class could not be found
    *                                or could not be instantiated.
    */
   private static Object newInstance(final String className, final ClassLoader classLoader) throws ClassNotFoundException {
      try {
         Class spiClass;
         if (classLoader == null) {
            spiClass = Class.forName(className);
         } else {
            try {
               spiClass = Class.forName(className, false, classLoader);
            } catch (ClassNotFoundException ex) {
               LOGGER.log(
                  Level.FINE,
                  "Unable to load provider class " + className
                     + " using custom classloader " + classLoader.getClass().getName()
                     + " trying again with current classloader.",
                  ex);
               spiClass = Class.forName(className);
            }
         }
         return spiClass.newInstance();
      } catch (ClassNotFoundException x) {
         throw x;
      } catch (Exception x) {
         throw new ClassNotFoundException("Provider " + className + " could not be instantiated: " + x, x);
      }
   }

   /**
    * Finds the implementation {@code Class} object for the given
    * factory name, or if that fails, finds the {@code Class} object
    * for the given fallback class name. The arguments supplied MUST be
    * used in order. If using the first argument is successful, the second
    * one will not be used.
    * <p>
    * This method is package private so that this code can be shared.
    * </p>
    *
    * @param factoryId         the name of the factory to find, which is
    *                          a system property.
    * @param fallbackClassName the implementation class name, which is
    *                          to be used only if nothing else.
    *                          is found; {@code null} to indicate that
    *                          there is no fallback class name.
    * @return the {@code Class} object of the specified message factory;
    *         may not be {@code null}.
    * @throws ClassNotFoundException if the given class could not be found
    *                                or could not be instantiated.
    */
   static Object find(final String factoryId, final String fallbackClassName) throws ClassNotFoundException {
      ClassLoader classLoader = getContextClassLoader();

      String serviceId = "META-INF/services/" + factoryId;
      // try to find services in CLASSPATH
      BufferedReader reader = null;
      try {
         InputStream is;
         if (classLoader == null) {
            is = ClassLoader.getSystemResourceAsStream(serviceId);
         } else {
            is = classLoader.getResourceAsStream(serviceId);
         }

         if (is != null) {
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            String factoryClassName = reader.readLine();
            if (factoryClassName != null && !"".equals(factoryClassName)) {
               return newInstance(factoryClassName, classLoader);
            }
         }
      } catch (Exception ex) {
         LOGGER.log(Level.FINER, "Failed to load service " + factoryId + " from " + serviceId, ex);
      } finally {
         if (reader != null) {
            try {
               reader.close();
            } catch (IOException ex) {
               LOGGER.log(Level.FINER, String.format("Error closing %s file.", serviceId), ex);
            }
         }
      }


      // try to read from $java.home/lib/jaxrs.properties
      FileInputStream inputStream = null;
      String configFile = null;
      try {
         String javah = System.getProperty("java.home");
         configFile = javah + File.separator + "lib" + File.separator + "jaxrs.properties";
         File f = new File(configFile);
         if (f.exists()) {
            Properties props = new Properties();
            inputStream = new FileInputStream(f);
            props.load(inputStream);
            String factoryClassName = props.getProperty(factoryId);
            return newInstance(factoryClassName, classLoader);
         }
      } catch (Exception ex) {
         LOGGER.log(Level.FINER, "Failed to load service " + factoryId
            + " from $java.home/lib/jaxrs.properties", ex);
      } finally {
         if (inputStream != null) {
            try {
               inputStream.close();
            } catch (IOException ex) {
               LOGGER.log(Level.FINER, String.format("Error closing %s file.", configFile), ex);
            }
         }
      }

      // Use the system property
      try {
         String systemProp = System.getProperty(factoryId);
         if (systemProp != null) {
            return newInstance(systemProp, classLoader);
         }
      } catch (SecurityException se) {
         LOGGER.log(Level.FINER, "Failed to load service " + factoryId
            + " from a system property", se);
      }

      if (fallbackClassName == null) {
         throw new ClassNotFoundException(
            "Provider for " + factoryId + " cannot be found", null);
      }

      return newInstance(fallbackClassName, classLoader);
   }
}
