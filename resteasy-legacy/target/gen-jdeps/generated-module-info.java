/**
  Mon Oct 30 12:20:37 EDT 2017
  File generated by jModInfoGet
	Analysis of /home/rsearls/j1/Resteasy/resteasy-legacy
		/home/rsearls/j1/Resteasy/resteasy-legacy/target/gen-jdeps/classes.dot
		/home/rsearls/j1/Resteasy/resteasy-legacy/target/classes/META-INF/services/javax.ws.rs.ext.RuntimeDelegate
**/
module resteasy-legacy {
exports org.jboss.resteasy.annotations.interception;
exports org.jboss.resteasy.annotations.legacy;
exports org.jboss.resteasy.client;
exports org.jboss.resteasy.client.cache;
exports org.jboss.resteasy.client.core;
exports org.jboss.resteasy.client.core.executors;
exports org.jboss.resteasy.client.core.extractors;
exports org.jboss.resteasy.client.core.marshallers;
exports org.jboss.resteasy.client.exception;
exports org.jboss.resteasy.client.exception.mapper;
exports org.jboss.resteasy.core.interception;
exports org.jboss.resteasy.logging;
exports org.jboss.resteasy.logging.impl;
exports org.jboss.resteasy.plugins.interceptors.encoding;
exports org.jboss.resteasy.spi.interception;
exports org.jboss.resteasy.spi.old;
requires  java.base;
requires  java.logging;
requires  java.management;
requires  java.xml.ws.annotation;
requires  resteasy-jaxrs;
requires  resteasy-spring;
provides javax.ws.rs.ext.RuntimeDelegate with 
		org.jboss.resteasy.spi.old.ResteasyProviderFactory;

/**
	The module names for these packages are unknown.
	User intervention may be needed.
		requires javax.ws.rs;
		requires javax.ws.rs.client;
		requires javax.ws.rs.container;
		requires javax.ws.rs.ext;
		requires org.apache.commons.io.output;
		requires org.apache.http;
		requires org.apache.http.auth;
		requires org.apache.http.client;
		requires org.apache.http.client.entity;
		requires org.apache.http.client.methods;
		requires org.apache.http.client.params;
		requires org.apache.http.conn;
		requires org.apache.http.cookie;
		requires org.apache.http.entity;
		requires org.apache.http.impl.auth;
		requires org.apache.http.impl.client;
		requires org.apache.http.message;
		requires org.apache.http.params;
		requires org.apache.http.protocol;
		requires org.apache.log4j;
		requires org.slf4j;
**/

}