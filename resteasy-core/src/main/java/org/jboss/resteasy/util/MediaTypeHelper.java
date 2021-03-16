package org.jboss.resteasy.util;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings(value = "rawtypes")
public class MediaTypeHelper extends org.jboss.resteasy.reactive.common.util.MediaTypeHelper
{
   @SuppressWarnings(value = "unchecked")
   public static MediaType getConsumes(Class declaring, AccessibleObject method)
   {
      Consumes consume = method.getAnnotation(Consumes.class);
      if (consume == null)
      {
         consume = (Consumes) declaring.getAnnotation(Consumes.class);
         if (consume == null) return null;
      }
      return MediaType.valueOf(consume.value()[0]);
   }

   public static MediaType[] getProduces(Class declaring, Method method)
   {
      return getProduces(declaring, method, null);
   }

   @SuppressWarnings(value = "unchecked")
   public static MediaType[] getProduces(Class declaring, Method method, MediaType defaultProduces)
   {
      Produces consume = method.getAnnotation(Produces.class);
      if (consume == null)
      {
         consume = (Produces) declaring.getAnnotation(Produces.class);
      }
      if (consume == null)
      {
         if (defaultProduces != null)
         {
            return new MediaType[]{defaultProduces};
         } else {
            return null;
         }
      }
      MediaType[] mediaTypes = new MediaType[consume.value().length];
      for(int i = 0; i< consume.value().length;i++){
         mediaTypes[i] = MediaType.valueOf(consume.value()[i]);
      }
      return mediaTypes.length != 0 ? mediaTypes : null;
   }

}
