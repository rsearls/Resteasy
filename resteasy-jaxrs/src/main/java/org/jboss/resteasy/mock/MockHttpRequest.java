package org.jboss.resteasy.mock;

import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.specimpl.HttpHeadersImpl;
import org.jboss.resteasy.specimpl.UriInfoImpl;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.util.HttpHeaderNames;
import org.jboss.resteasy.util.ReadFromStream;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MockHttpRequest implements HttpRequest
{
   protected HttpHeadersImpl httpHeaders;
   protected InputStream inputStream;
   protected UriInfo uri;
   protected String httpMethod;
   protected List<PathSegment> preProcessedSegments;


   protected MockHttpRequest()
   {
   }

   protected static MockHttpRequest initWithUri(String uri) throws URISyntaxException
   {
      URI absoluteUri = new URI(uri);
      MockHttpRequest request = new MockHttpRequest();
      request.httpHeaders = new HttpHeadersImpl();
      request.httpHeaders.setAcceptableLanguages(new ArrayList<String>());
      request.httpHeaders.setAcceptableMediaTypes(new ArrayList<MediaType>());
      request.httpHeaders.setCookies(new HashMap<String, Cookie>());
      request.httpHeaders.setRequestHeaders(new Headers<String>());
      request.uri = new UriInfoImpl(absoluteUri, absoluteUri.getPath(), absoluteUri.getQuery());
      request.preProcessedSegments = request.uri.getPathSegments(false);
      return request;

   }

   public static MockHttpRequest get(String uri) throws URISyntaxException
   {
      MockHttpRequest request = initWithUri(uri);
      request.httpMethod = "GET";
      return request;
   }

   public static MockHttpRequest post(String uri) throws URISyntaxException
   {
      MockHttpRequest request = initWithUri(uri);
      request.httpMethod = "POST";
      return request;
   }

   public static MockHttpRequest put(String uri) throws URISyntaxException
   {
      MockHttpRequest request = initWithUri(uri);
      request.httpMethod = "PUT";
      return request;
   }

   public static MockHttpRequest delete(String uri) throws URISyntaxException
   {
      MockHttpRequest request = initWithUri(uri);
      request.httpMethod = "DELETE";
      return request;
   }

   public static MockHttpRequest head(String uri) throws URISyntaxException
   {
      MockHttpRequest request = initWithUri(uri);
      request.httpMethod = "HEAD";
      return request;
   }

   public static MockHttpRequest deepCopy(HttpRequest request) throws IOException
   {
      MockHttpRequest mock = new MockHttpRequest();
      mock.uri = request.getUri();
      mock.httpHeaders = (HttpHeadersImpl) request.getHttpHeaders();
      mock.httpMethod = request.getHttpMethod();
      mock.inputStream = new ByteArrayInputStream(ReadFromStream.readFromStream(1024, request.getInputStream()));
      mock.preProcessedSegments = request.getPreProcessedSegments();
      return mock;
   }


   public MockHttpRequest header(String name, String value)
   {
      httpHeaders.getRequestHeaders().add(name, value);
      return this;
   }

   public MockHttpRequest accept(String type)
   {
      httpHeaders.getRequestHeaders().add(HttpHeaderNames.ACCEPT, type);
      httpHeaders.getAcceptableMediaTypes().add(MediaType.valueOf(type));
      return this;
   }

   public MockHttpRequest language(String language)
   {
      httpHeaders.getRequestHeaders().add(HttpHeaderNames.ACCEPT_LANGUAGE, language);
      httpHeaders.getAcceptableLanguages().add(language);
      return this;
   }

   public MockHttpRequest cookie(String name, String value)
   {
      Cookie cookie = new Cookie(name, value);
      httpHeaders.getCookies().put(name, cookie);
      return this;
   }

   public MockHttpRequest contentType(String type)
   {
      httpHeaders.getRequestHeaders().add(HttpHeaderNames.CONTENT_TYPE, type);
      httpHeaders.setMediaType(MediaType.valueOf(type));
      return this;
   }

   public MockHttpRequest content(byte[] bytes)
   {
      inputStream = new ByteArrayInputStream(bytes);
      return this;
   }

   public MockHttpRequest content(InputStream stream)
   {
      inputStream = stream;
      return this;
   }

   public HttpHeaders getHttpHeaders()
   {
      return httpHeaders;
   }

   public InputStream getInputStream()
   {
      return inputStream;
   }

   public UriInfo getUri()
   {
      return uri;
   }

   public String getHttpMethod()
   {
      return httpMethod;
   }

   public List<PathSegment> getPreProcessedSegments()
   {
      return preProcessedSegments;
   }

   public void setPreProcessedSegments(List<PathSegment> segments)
   {
      this.preProcessedSegments = segments;
   }

}
