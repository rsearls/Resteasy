package org.jboss.resteasy.test.core.smoke.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;

/**
 * Created by rsearls on 10/16/17.
 */
//@Path("/parent")
@Path("/")
public interface IntfA {
   @GET
   @Path("/report/states")
   @Produces( MediaType.TEXT_HTML)
   public String getReport(@Context UriInfo uriInfo);
}
