package org.jboss.resteasy.specimpl;

import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.interception.AbstractReaderInterceptorContext;
import org.jboss.resteasy.core.interception.ClientReaderInterceptorContext;
import org.jboss.resteasy.plugins.delegates.LocaleDelegate;
import org.jboss.resteasy.plugins.providers.sse.EventInput;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.HeaderValueProcessor;
import org.jboss.resteasy.spi.LinkHeaders;
import org.jboss.resteasy.spi.MarshalledEntity;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.CaseInsensitiveMap;
import org.jboss.resteasy.util.DateUtil;
import org.jboss.resteasy.util.HttpHeaderNames;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.jboss.resteasy.util.InputStreamToByteArray;
import org.jboss.resteasy.util.ReadFromStream;
import org.jboss.resteasy.util.Types;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ReaderInterceptor;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A response object not attached to a client or server invocation.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BuiltResponse extends Response
{
   protected Object entity;
   protected int status = HttpResponseCodes.SC_OK;
   protected String reason = "Unknown Code";
   protected Headers<Object> metadata = new Headers<Object>();
   protected Annotation[] annotations;
   protected Class entityClass;
   protected Type genericType;
   protected HeaderValueProcessor processor;
   protected volatile boolean isClosed;
   protected InputStream is;
   protected byte[] bufferedEntity;
   protected volatile boolean streamRead;
   protected volatile boolean streamFullyRead;

   public BuiltResponse()
   {
   }

   public BuiltResponse(int status, Headers<Object> metadata, Object entity, Annotation[] entityAnnotations)
   {
      this(status, null, metadata, entity, entityAnnotations);
   }

   public BuiltResponse(int status, String reason, Headers<Object> metadata, Object entity, Annotation[] entityAnnotations)
   {
      setEntity(entity);
      this.status = status;
      this.metadata = metadata;
      this.annotations = entityAnnotations;
      if (reason != null) {
         this.reason = reason;
      }
   }

   public Class getEntityClass()
   {
      return entityClass;
   }

   public void setEntityClass(Class entityClass)
   {
      this.entityClass = entityClass;
   }

   protected HeaderValueProcessor getHeaderValueProcessor()
   {
      if (processor != null) return processor;
      return ResteasyProviderFactory.getInstance();
   }

   @Override
   public Object getEntity()
   {
      abortIfClosed();
      return entity;
   }

   @Override
   public int getStatus()
   {
      return status;
   }

   public String getReasonPhrase()
   {
      return reason;
   }

   @Override
   public StatusType getStatusInfo()
   {
      StatusType statusType = Status.fromStatusCode(status);
      if (statusType == null)
      {
         statusType = new StatusType()
         {
            @Override
            public int getStatusCode()
            {
               return status;
            }

            @Override
            public Status.Family getFamily()
            {
               return Status.Family.familyOf(status);
            }

            @Override
            public String getReasonPhrase()
            {
               return reason;
            }
         };
      }
      return statusType;
   }

   @Override
   public MultivaluedMap<String, Object> getMetadata()
   {
      return metadata;
   }

   public void setEntity(Object entity)
   {
      if (entity == null)
      {
         this.entity = null;
         this.genericType = null;
         this.entityClass = null;
      }
      else if (entity instanceof GenericEntity)
      {

         GenericEntity ge = (GenericEntity) entity;
         this.entity = ge.getEntity();
         this.genericType = ge.getType();
         this.entityClass = ge.getRawType();
      }
      else
      {
         this.entity = entity;
         this.entityClass = entity.getClass();
         this.genericType = null;
      }
   }

   public void setStatus(int status)
   {
      this.status = status;
   }

   public void setReasonPhrase(String reason)
   {
      this.reason = reason;
   }

   public void setMetadata(MultivaluedMap<String, Object> metadata)
   {
      this.metadata = new Headers<Object>();
      this.metadata.putAll(metadata);
   }

   public Annotation[] getAnnotations()
   {
      return annotations;
   }

   public void addMethodAnnotations(Annotation[] methodAnnotations)
   {
      List<Annotation> ann = new ArrayList<Annotation>();
      if (annotations != null)
      {
         for (Annotation annotation : annotations)
         {
            ann.add(annotation);
         }
      }
      for (Annotation annotation : methodAnnotations)
      {
         ann.add(annotation);
      }
      annotations = ann.toArray(new Annotation[ann.size()]);
   }


   public void setAnnotations(Annotation[] annotations)
   {
      this.annotations = annotations;
   }

   public Type getGenericType()
   {
      return genericType;
   }

   public void setGenericType(Type genericType)
   {
      this.genericType = genericType;
   }

   @Override
   public <T> T readEntity(Class<T> type, Annotation[] annotations)
   {
      return readEntity(type, null, annotations);
   }

   @SuppressWarnings(value = "unchecked")
   @Override
   public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations)
   {
      return readEntity((Class<T>) entityType.getRawType(), entityType.getType(), annotations);
   }

   @Override
   public <T> T readEntity(Class<T> type)
   {
      return readEntity(type, null, null);
   }


   @SuppressWarnings(value = "unchecked")
   @Override
   public <T> T readEntity(GenericType<T> entityType)
   {
      return readEntity((Class<T>) entityType.getRawType(), entityType.getType(), null);
   }

   public <T> T readEntity(Class<T> type, Type genericType, Annotation[] anns) {
      abortIfClosed();
      if (entity != null) {
         if (type.isInstance((this.entity))) {
            return (T) entity;
         } else if (entity instanceof InputStream) {
            setInputStream((InputStream) entity);
            entity = null;
         } else if (bufferedEntity == null) {
            throw new RuntimeException(Messages.MESSAGES.entityAlreadyRead(entity.getClass()));
         } else {
            entity = null;
         }
      }

      if (entity == null) {
         if (status == HttpResponseCodes.SC_NO_CONTENT) {
            return null;
         }

         try {
            entity = readFrom(type, genericType, getMediaType(), anns);
            if (entity == null || (entity != null
                    && !InputStream.class.isInstance(entity)
                    && !Reader.class.isInstance(entity)
                    && bufferedEntity == null)) {
               try {
                  if (!EventInput.class.isInstance(entity)) {
                     close();
                  }
               } catch (Exception ignored) {
               }
            }
         } catch (RuntimeException e) {
            try {
               close();
            } catch (Exception ignored) {

            }
            throw e;
         }
      }
      return (T) entity;
   }

   // this is synchronized in conjunction with finalize to protect against premature finalize called by the GC
   protected synchronized <T> Object readFrom(Class<T> type, Type genericType,
                                              MediaType media, Annotation[] annotations)
   {
      Type useGeneric = genericType == null ? type : genericType;
      Class<?> useType = type;
      media = media == null ? MediaType.WILDCARD_TYPE : media;
      annotations = annotations == null ? this.annotations : annotations;
      boolean isMarshalledEntity = false;
      if (type.equals(MarshalledEntity.class))
      {
         isMarshalledEntity = true;
         ParameterizedType param = (ParameterizedType) useGeneric;
         useGeneric = param.getActualTypeArguments()[0];
         useType = Types.getRawType(useGeneric);
      }

      Object obj = null;
      try
      {
         InputStream is = getEntityStream();
         if (is == null)
         {
            throw new IllegalStateException(Messages.MESSAGES.inputStreamWasEmpty());
         }
         if (isMarshalledEntity)
         {
            is = new InputStreamToByteArray(is);

         }

         ResteasyProviderFactory providerFactory = ResteasyProviderFactory.getInstance();
         ReaderInterceptor[] readerInterceptors = providerFactory
                 .getServerReaderInterceptorRegistry()
                 .postMatch(null, null);

         final Object finalObj;
         AbstractReaderInterceptorContext context = new ClientReaderInterceptorContext(
                 readerInterceptors, providerFactory, useType,
                 useGeneric, annotations, media, getStringHeaders(), is,
                 new HashMap<String, Object>());

         finalObj = context.proceed();
         obj = finalObj;

         if (isMarshalledEntity)
         {
            InputStreamToByteArray isba = (InputStreamToByteArray) is;
            final byte[] bytes = isba.toByteArray();
            return new MarshalledEntity<Object>()
            {
               @Override
               public byte[] getMarshalledBytes()
               {
                  return bytes;
               }

               @Override
               public Object getEntity()
               {
                  return finalObj;
               }
            };
         }
         else
         {
            return finalObj;
         }

      }
      catch (ProcessingException pe)
      {
         throw pe;
      }
      catch (Exception ex)
      {
         throw new ProcessingException(ex);
      }
   }

   protected InputStream getEntityStream()
   {
      if (bufferedEntity != null) return new ByteArrayInputStream(bufferedEntity);
      if (isClosed()) throw new ProcessingException(Messages.MESSAGES.streamIsClosed());
      InputStream is = getInputStream();
      return is != null ? new InputStreamWrapper(is, this) : null;
   }

   protected void setInputStream(InputStream is)
   {
      this.is = is;
   }

   protected InputStream getInputStream()
   {
      if (is == null && entity != null && entity instanceof InputStream) {
         is = (InputStream) entity;
      }
      return is;
   }

   private static class InputStreamWrapper extends FilterInputStream {

      private BuiltResponse response;

      protected InputStreamWrapper(final InputStream in, final BuiltResponse response) {
         super(in);
         this.response = response;
      }

      public int read() throws IOException
      {
         return checkEOF(super.read());
      }

      public int read(byte[] b) throws IOException
      {
         return checkEOF(super.read(b));
      }

      public int read(byte[] b, int off, int len) throws IOException
      {
         return checkEOF(super.read(b, off, len));
      }

      private int checkEOF(int v)
      {
         response.streamRead=true;
         if (v < 0)
         {
            response.streamFullyRead = true;
         }
         return v;
      }

      @Override
      public void close() throws IOException {
         super.close();
         this.response.close();
      }
   }

   protected void resetEntity()
   {
      entity = null;
      bufferedEntity = null;
      streamFullyRead = false;
   }

   public void releaseConnection() throws IOException
   {
      releaseConnection(false);
   }

   public void releaseConnection(boolean consumeInputStream) throws IOException
   {
      try
      {
         if (is != null)
         {
            if (consumeInputStream)
            {
               while (is.read() > 0)
               {
               }
            }
            is.close();
            is = null;
         }
      }
      catch (IOException e)
      {

      }

   }

   @Override
   public boolean hasEntity()
   {
      abortIfClosed();
      return entity != null;
   }

   @Override
   public boolean bufferEntity()
   {
      abortIfClosed();

      if (bufferedEntity != null) return true;
      if (streamRead) return false;
      InputStream is = getInputStream();
      if (is == null) return false;
      try
      {
         bufferedEntity = ReadFromStream.readFromStream(1024, is);
      }
      catch (IOException e)
      {
         throw new ProcessingException(e);
      }
      finally
      {
         try {
            releaseConnection();
         }
         catch (IOException e) {
            throw new ProcessingException(e);
         }
      }
      return true;
   }

   public boolean isClosed()
   {
      return isClosed;
   }

   public void abortIfClosed() {
      if (bufferedEntity == null) {
         if (isClosed()) {
            throw new IllegalStateException(Messages.MESSAGES.responseIsClosed());
         }
      }
   }

   @Override
   public void close()
   {
      isClosed = true;
   }

   @Override
   public Locale getLanguage()
   {
      Object obj = metadata.getFirst(HttpHeaders.CONTENT_LANGUAGE);
      if (obj == null) return null;
      if (obj instanceof Locale) return (Locale) obj;
      return new LocaleDelegate().fromString(toHeaderString(obj));
   }

   @Override
   public int getLength()
   {
      Object obj = metadata.getFirst(HttpHeaders.CONTENT_LENGTH);
      if (obj == null) return -1;
      if (obj instanceof Integer) return (Integer) obj;
      return Integer.valueOf(toHeaderString(obj));
   }

   @Override
   public MediaType getMediaType()
   {
      Object obj = metadata.getFirst(HttpHeaders.CONTENT_TYPE);
      if (obj instanceof MediaType) return (MediaType) obj;
      if (obj == null) return null;
      return MediaType.valueOf(toHeaderString(obj));
   }

   @Override
   public Map<String, NewCookie> getCookies()
   {
      Map<String, NewCookie> cookies = new HashMap<String, NewCookie>();
      List list = metadata.get(HttpHeaders.SET_COOKIE);
      if (list == null) return cookies;
      for (Object obj : list)
      {
         if (obj instanceof NewCookie)
         {
            NewCookie cookie = (NewCookie)obj;
            cookies.put(cookie.getName(), cookie);
         }
         else
         {
            String str = toHeaderString(obj);
            NewCookie cookie = NewCookie.valueOf(str);
            cookies.put(cookie.getName(), cookie);
         }
      }
      return cookies;
   }

   @Override
   public EntityTag getEntityTag()
   {
      Object d = metadata.getFirst(HttpHeaders.ETAG);
      if (d == null) return null;
      if (d instanceof EntityTag) return (EntityTag) d;
      return EntityTag.valueOf(toHeaderString(d));
   }

   @Override
   public Date getDate()
   {
      Object d = metadata.getFirst(HttpHeaders.DATE);
      if (d == null) return null;
      if (d instanceof Date) return (Date) d;
      return DateUtil.parseDate(d.toString());
   }

   @Override
   public Date getLastModified()
   {
      Object d = metadata.getFirst(HttpHeaders.LAST_MODIFIED);
      if (d == null) return null;
      if (d instanceof Date) return (Date) d;
      return DateUtil.parseDate(d.toString());
   }

   @Override
   public Set<String> getAllowedMethods()
   {
      Set<String> allowedMethods = new HashSet<String>();
      List<Object> allowed = metadata.get("Allow");
      if (allowed == null) return allowedMethods;
      for (Object header : allowed)
      {
         if (header != null && header instanceof String)
         {
            String[] list = ((String)header).split(",");
            for (String str : list)
            {
               if (!"".equals(str.trim()))
               {
                  allowedMethods.add(str.trim().toUpperCase());
               }
            }
         }
         else
         {
            allowedMethods.add(toHeaderString(header).toUpperCase());
         }
      }

      return allowedMethods;
   }

   protected String toHeaderString(Object header)
   {
      if (header instanceof String) return (String)header;
      return getHeaderValueProcessor().toHeaderString(header);
   }

   @Override
   public MultivaluedMap<String, String> getStringHeaders()
   {
      CaseInsensitiveMap<String> map = new CaseInsensitiveMap<String>();
      for (Map.Entry<String, List<Object>> entry : metadata.entrySet())
      {
         for (Object obj : entry.getValue())
         {
            map.add(entry.getKey(), toHeaderString(obj));
         }
      }
      return map;
   }

   @Override
   public String getHeaderString(String name)
   {
      List vals = metadata.get(name);
      if (vals == null) return null;
      StringBuilder builder = new StringBuilder();
      boolean first = true;
      for (Object val : vals)
      {
         if (first) first = false;
         else builder.append(",");
         if (val == null) val = "";
         val = toHeaderString(val);
         if (val == null) val = "";
         builder.append(val);
      }
      return builder.toString();
   }

   @Override
   public URI getLocation()
   {
      Object uri = metadata.getFirst(HttpHeaders.LOCATION);
      if (uri == null) return null;
      if (uri instanceof URI) return (URI)uri;
      String str = null;
      if (uri instanceof String) str = (String)uri;
      else str = toHeaderString(uri);
      return URI.create(str);
   }

   @Override
   public Set<Link> getLinks()
   {
      LinkHeaders linkHeaders = getLinkHeaders();
      Set<Link> links = new HashSet<Link>();
      links.addAll(linkHeaders.getLinks());
      return links;
   }

   protected LinkHeaders getLinkHeaders()
   {
      LinkHeaders linkHeaders = new LinkHeaders();
      linkHeaders.addLinkObjects(metadata, getHeaderValueProcessor());
      return linkHeaders;
   }

   @Override
   public boolean hasLink(String relation)
   {
      return getLinkHeaders().getLinkByRelationship(relation) != null;
   }

   @Override
   public Link getLink(String relation)
   {
      return getLinkHeaders().getLinkByRelationship(relation);
   }

   @Override
   public Link.Builder getLinkBuilder(String relation)
   {
      Link link = getLinkHeaders().getLinkByRelationship(relation);
      if (link == null) return null;
      return Link.fromLink(link);
   }

}
