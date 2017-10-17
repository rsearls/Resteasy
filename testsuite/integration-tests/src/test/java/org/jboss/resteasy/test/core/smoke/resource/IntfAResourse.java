package org.jboss.resteasy.test.core.smoke.resource;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

/**
 * Created by rsearls on 10/16/17.
 */
//@Path("/parent")
public class IntfAResourse implements IntfA {
   public String getReport(@Context UriInfo uriInfo) {
      return "IntfA getReport called";
   }
}
