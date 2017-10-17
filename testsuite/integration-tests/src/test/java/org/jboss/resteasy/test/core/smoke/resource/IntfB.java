package org.jboss.resteasy.test.core.smoke.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

/**
 * Created by rsearls on 10/16/17.
 */
@Path("/report")
public interface IntfB {
   @POST
   @Path("/states")
   @Consumes(MediaType.APPLICATION_XML)
   @Produces( MediaType.APPLICATION_JSON)
      //ReportData getData(WebQuery query);
   public String getData(String query);
}
