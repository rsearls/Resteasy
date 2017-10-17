package org.jboss.resteasy.test.core.smoke.resource;

/**
 * Created by rsearls on 10/16/17.
 */
public class IntfBResource implements IntfB {

   public String getData(String query) {
      return "IntfB getData called";
   }
}
