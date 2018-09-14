package org.jboss.resteasy.util;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.DotName;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;

import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.spi.ResteasyDeployment;

import javax.ws.rs.core.Application;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is a prototyping class.  Playing with the best way for resteasy to access
 * the index information.
 */
public class ResteasyIndex {

   private Index index = null;

   public ResteasyIndex() {
   }

   public Index generateIndex(ResteasyDeployment deployment) {
      // gather all the application's classes to be indexed
      Set<String> namesAsStrings = new HashSet<>();
      // this.deployment = deployment;

      namesAsStrings.add(deployment.getApplicationClass());
      Application application = deployment.getApplication();
      if (application != null)
      {
         namesAsStrings.add(application.getClass().getName());
      }

      namesAsStrings.add(deployment.getInjectorFactoryClass());
      InjectorFactory injectorFactory = deployment.getInjectorFactory();
      if (injectorFactory != null) {
         namesAsStrings.add(injectorFactory.getClass().getName());
      }

      namesAsStrings.addAll(deployment.getScannedResourceClasses());
      namesAsStrings.addAll(deployment.getScannedProviderClasses());

      List<String> scannedJndiComponentResourcesList = deployment.getScannedJndiComponentResources();
      for (String resource : scannedJndiComponentResourcesList) {
         String[] config = resource.trim().split(";");
         if (config.length >= 3) {
            namesAsStrings.add(config[1]);
         }
      }

      List<String> jndiComponentResources = deployment.getJndiComponentResources();
      for (String resource : jndiComponentResources) {
         String[] config = resource.trim().split(";");
         if (config.length >= 3) {
            namesAsStrings.add(config[1]);
         }
      }

      namesAsStrings.addAll(deployment.getProviderClasses());

      for (Class clazz : deployment.getActualProviderClasses()) {
         namesAsStrings.add(clazz.getClass().getName());
      }

      for (Object provider : deployment.getProviders())
      {
         namesAsStrings.add(provider.getClass().getName());
      }

      namesAsStrings.addAll(deployment.getResourceClasses());

      for (Class clazz : deployment.getActualResourceClasses()) {
         namesAsStrings.add(clazz.getClass().getName());
      }

      for (ResourceFactory factory: deployment.getResourceFactories()){
         namesAsStrings.add(factory.getClass().getName());
      }

      for (Object resource : deployment.getResources())
      {
         namesAsStrings.add(resource.getClass().getName());
      }

      namesAsStrings.remove(null);
      Indexer indexer = new Indexer();

      for (String s : namesAsStrings)
      {
         String className = getSystemDependentClassFilename(s.trim());
         InputStream inStream = Thread.currentThread().getContextClassLoader()
                 .getResourceAsStream(className);
         if (inStream != null)
         {
            try
            {
               indexer.index(inStream);
            } catch (IOException e) {
               //todo log msg
               System.out.println("Indexer processing failed for file: " + className);
            }
         } else
         {
            //todo log msg
            System.out.println("Class not found in classloader: " + className);
         }
      }

      this.index = indexer.complete();

      testIndex(index); // rls debug
      return index;
   }

   public void getAnnotationInClass(Class providerClass, DotName annotationDotName) {
      List<AnnotationInstance> annotations = index.getAnnotations(annotationDotName);

      DotName classDotName = DotName.createSimple(providerClass.getName());
      ClassInfo classInfo = index.getClassByName(classDotName);
      if (classInfo != null) {
         Map<DotName, List<AnnotationInstance>> annotationMap = classInfo.annotations();
         List<AnnotationInstance> aInstanceList = annotationMap.get((DotName)annotationDotName);
         String xxx = "";
      }
   }

   private void testIndex(Index index) {

      List<AnnotationInstance> annotations =
              index.getAnnotations(JaxrsAnnotations.PROVIDER.getDotName());
      for (AnnotationInstance a : annotations)
      {
         AnnotationValue value = a.value();
         AnnotationTarget target = a.target();
         if (target.kind() == AnnotationTarget.Kind.CLASS) {
            ClassInfo cInfo = target.asClass();
            DotName dotname = cInfo.name();
            String fullname = dotname.toString();
            String here = "";
         }
      }


      String stophere = "";
   }

   /**
    * Create system dependent filename with extension '.class'
    **/
   private String getSystemDependentClassFilename (String name) {
      return name.replace(".", File.separator) + ".class";
   }

}
