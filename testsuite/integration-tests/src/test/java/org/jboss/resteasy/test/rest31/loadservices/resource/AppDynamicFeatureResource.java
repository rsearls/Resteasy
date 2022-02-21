package org.jboss.resteasy.test.rest31.loadservices.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import java.util.Set;

@Path("/")
public class AppDynamicFeatureResource {

    @Context
    private Configuration configuration;

    /**
     * Confirm the DynamicFeature and Feature impls identified in the
     * application classes method are present.
     * @return
     */
    @GET
    @Path("/checkServiceClasses")
    public Response getAppClasses() {
        Set<Class<?>> cSet =  configuration.getClasses();
        boolean adf = configuration.getClasses().contains(Rest31DynamicFeature.class);
        boolean af = configuration.getClasses().contains(Rest31Feature.class);

        if (!adf && !af) {
            return Response.ok("success").build();
        }
        return Response.ok("Rest31DynamicFeature = " + adf + "  Rest31Feature = " + af).build();
    }

    /**
     * Confirm the DynamicFeature and Feature impls identified via the
     * service file is not present.
     * @return
     */
    @GET
    @Path("/checkAppClasses")
    public Response getServices() {
        Set<Class<?>> cSet =  configuration.getClasses();
        boolean adf = configuration.getClasses().contains(AppDynamicFeature.class);
        boolean af = configuration.getClasses().contains(AppFeature.class);

        if (adf && af) {
            return Response.ok("success").build();
        }
        return Response.ok("AppDynamicFeature = " + adf + "  AppFeature = " + af).build();
    }
}
