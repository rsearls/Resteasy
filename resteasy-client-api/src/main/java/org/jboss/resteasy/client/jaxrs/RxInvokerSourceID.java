package org.jboss.resteasy.client.jaxrs;

/**
 * There are 3 modules that support reactiveX methods in resource
 * methods.  There can be competing RxInvokerProviders for a method
 * return type from these modules.  This enum is used to identify from
 * which module the endpoint was called and identify the RxInvokerProvider
 * to be used.
 */
public enum RxInvokerSourceID {
    RXJAVA2,
    REACTOR,
    MP_REST_CLIENT
}
