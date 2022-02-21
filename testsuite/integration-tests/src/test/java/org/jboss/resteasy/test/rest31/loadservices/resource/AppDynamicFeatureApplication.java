package org.jboss.resteasy.test.rest31.loadservices.resource;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ApplicationPath("/")
public class AppDynamicFeatureApplication extends Application {
    private Set<Class<?>> classes = new HashSet<Class<?>>();
    private HashMap<String, Object> propMap = new HashMap<>();

    public AppDynamicFeatureApplication () {
        classes.add(AppDynamicFeature.class);
        classes.add(AppFeature.class);
        classes.add(AppDynamicFeatureResource.class);

        propMap.put("Prop1", "Value1");
        propMap.put("jakarta.ws.rs.loadServices", Boolean.FALSE);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Map<String, Object> getProperties() {
        return propMap;
    }
}
