package org.jboss.resteasy.core;

import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.spi.NoLogWebApplicationException;
import org.jboss.resteasy.spi.ReaderException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.UnhandledException;
import org.jboss.resteasy.spi.WriterException;
import org.jboss.resteasy.tracing.RESTEasyTracingLogger;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ExceptionHandler
{
   protected ResteasyProviderFactoryImpl providerFactory;
   protected Set<String> unwrappedExceptions = new HashSet<String>();
   protected boolean mapperExecuted;

   public ExceptionHandler(ResteasyProviderFactory providerFactory, Set<String> unwrappedExceptions)
   {
      this.providerFactory = (ResteasyProviderFactoryImpl)providerFactory;
      this.unwrappedExceptions = unwrappedExceptions;
   }

   public boolean isMapperExecuted()
   {
      return mapperExecuted;
   }

   /**
    * If there exists an Exception mapper for exception, execute it, otherwise, do NOT recurse up class hierarchy
    * of exception.
    *
    * @param exception
    * @param logger
    * @return response
    */
   @SuppressWarnings(value = "unchecked")
   protected Response executeExactExceptionMapper(Throwable exception, RESTEasyTracingLogger logger) {
      if (logger == null)
         logger = RESTEasyTracingLogger.empty();

      ExceptionMapper mapper = providerFactory.getExceptionMappers().get(exception.getClass());
      if (mapper == null) return null;
      mapperExecuted = true;
      long timestamp = logger.timestamp("EXCEPTION_MAPPING");
      Response resp = mapper.toResponse(exception);
      logger.logDuration("EXCEPTION_MAPPING", timestamp, mapper, exception, exception.getLocalizedMessage(), resp);
      return resp;
   }

   @Deprecated
   @SuppressWarnings(value = "unchecked")
   public Response executeExactExceptionMapper(Throwable exception) {
      return executeExactExceptionMapper(exception, null);
   }

   @SuppressWarnings(value = "unchecked")
   protected Response executeExceptionMapperForClass(Throwable exception, Class clazz, RESTEasyTracingLogger logger)
   {
      if (logger == null)
         logger = RESTEasyTracingLogger.empty();
      ExceptionMapper mapper = providerFactory.getExceptionMappers().get(clazz);
      if (mapper == null) return null;
      mapperExecuted = true;
      long timestamp = logger.timestamp("EXCEPTION_MAPPING");
      Response resp = mapper.toResponse(exception);
      logger.logDuration("EXCEPTION_MAPPING", timestamp, mapper, exception, exception.getLocalizedMessage(), resp);
      return resp;
   }

   @Deprecated
   @SuppressWarnings(value = "unchecked")
   public Response executeExceptionMapperForClass(Throwable exception, Class clazz)
   {
      return executeExceptionMapperForClass(exception, clazz, null);
   }

   /**
    *
    * @param request
    * @param e
    * @return
    */
   private ExceptionMapper handleApplicationException(HttpRequest request,
                                                      ApplicationException e,
                                                      Throwable[] mappedThrowable)
   {
      mappedThrowable[0] = e;
      // See if there is a mapper for ApplicationException
      ExceptionMapper mapper = providerFactory.getExceptionMappers().get(ApplicationException.class);

      if (mapper == null) {
         mapper = unwrapException(request, e, mappedThrowable);
      }
      return mapper;
   }


   @Deprecated
   @SuppressWarnings(value = "unchecked")
   public Response executeExceptionMapper(Throwable exception)
   {
      return executeExactExceptionMapper(exception, null);
   }

   private ExceptionMapper unwrapException(HttpRequest request, Throwable e, Throwable[] mappedThrowable) {
      ExceptionMapper mapper = null;

      Throwable unwrappedException = e.getCause();
      mappedThrowable[0] = unwrappedException;
         /*
          * If the response property of the exception does not
          * contain an entity and an exception mapping provider
          * (see section 4.4) is available for
          * WebApplicationException an implementation MUST use the
          * provider to create a new Response instance, otherwise
          * the response property is used directly.
          */
      if (unwrappedException instanceof WebApplicationException) {
         WebApplicationException wae = (WebApplicationException) unwrappedException;
         if (wae.getResponse() != null && wae.getResponse().getEntity() != null) {
            return new WebApplicationExceptionMapperInternal();
         } else {
            mapper = executeExceptionMapperSuperClasses(wae);
            if (mapper == null) {
               mapper = handleWebApplicationException(wae);
            }
         }
      } else if (unwrappedException instanceof Failure) {
         mapper = handleFailure(request, (Failure)unwrappedException);
      } else {
         mapper = executeExceptionMapperSuperClasses(unwrappedException);

         if (mapper == null) {
            if (unwrappedExceptions.contains(unwrappedException.getClass().getName())
                    && unwrappedException.getCause() != null) {
               return unwrapException(request, unwrappedException, mappedThrowable);
            }
         }
      }
      return mapper;
   }


   @SuppressWarnings(value = "unchecked")
   private ExceptionMapper executeExceptionMapperSuperClasses(Throwable exception) {
      ExceptionMapper mapper = null;

      Class causeClass = exception.getClass();
      while (mapper == null) {
         if (causeClass == null){
            break;
         }
         mapper = providerFactory.getExceptionMappers().get(causeClass);
         if (mapper == null){
            causeClass = causeClass.getSuperclass();
         }
      }

      return mapper;
   }


   /**
    *
    * @param request
    * @param failure
    * @return
    */
   private ExceptionMapper handleFailure(HttpRequest request, Failure failure) {
      if (failure.isLoggable()) {
         LogMessages.LOGGER.failedExecutingError(request.getHttpMethod(),
                 request.getUri().getPath(), failure);
      } else {
         LogMessages.LOGGER.failedExecutingDebug(request.getHttpMethod(),
                 request.getUri().getPath(), failure);
      }

      return new FailureMapperInternal();
   }

   /**
    *
    * @param request
    * @param e
    * @return
    */
   private ExceptionMapper handleWriterException(HttpRequest request, WriterException e,
                                           Throwable[] mappedThrowable) {
      mappedThrowable[0] = e;

      // See if there is a general mapper for WriterException
      ExceptionMapper mapper =
              providerFactory.getExceptionMappers().get(WriterException.class);

      if (mapper == null) {
         if (e.getResponse() != null || e.getErrorCode() > -1) {
            return handleFailure(request, e);
         } else if (e.getCause() != null) {
            mapper = unwrapException(request, e, mappedThrowable);
         }
      }

      if (mapper == null) {
         e.setErrorCode(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR);
         return handleFailure(request, e);
      }

      return mapper;
   }


   /**
    *
    * @param request
    * @param e
    * @return
    */
   private ExceptionMapper handleReaderException(HttpRequest request, ReaderException e,
                                           Throwable[] mappedThrowable) {
      mappedThrowable[0] = e;

      // See if there is a general mapper for ReaderException
      ExceptionMapper mapper =
              providerFactory.getExceptionMappers().get(ReaderException.class);

      if (mapper == null) {
         if (e.getResponse() != null || e.getErrorCode() > -1) {
            return handleFailure(request, e);
         } else if (e.getCause() != null) {
            mapper = unwrapException(request, e, mappedThrowable);
         }
      }

      if (mapper == null) {
         e.setErrorCode(HttpResponseCodes.SC_BAD_REQUEST);
         return handleFailure(request, e);
      }

      return mapper;
   }

   /**
    *
    * @param wae
    * @return
    */
   private ExceptionMapper handleWebApplicationException(WebApplicationException wae) {
      if (wae instanceof NotFoundException) {
         LogMessages.LOGGER.failedToExecuteDebug(wae);
      } else if (!(wae instanceof NoLogWebApplicationException)) {
         LogMessages.LOGGER.failedToExecute(wae);
      }

      return new WebApplicationExceptionMapperInternal();
   }

   /**
    * @param request
    * @param e
    * @return
    */
   public Response handleException(HttpRequest request, Throwable e) {

      // We need to get back from select methods 2 return values
      // One a Throwable and the other an ExceptionMapper.
      // The Throwable is return in the array object that is an input param to the method
      // The ExceptionMapper is always returned as the method's return type.
      Throwable[] mappedThrowable = new Throwable[1];
      mappedThrowable[0] = e;

      // lookup mapper on classname of exception
      ExceptionMapper mapper = providerFactory.getExceptionMappers().get(e.getClass());

      if (mapper == null) {
         if (e instanceof WebApplicationException) {
            /*
             * If the response property of the exception does not
             * contain an entity and an exception mapping provider
             * (see section 4.4) is available for
             * WebApplicationException an implementation MUST use the
             * provider to create a new Response instance, otherwise
             * the response property is used directly.
             */
            WebApplicationException wae = (WebApplicationException) e;
            if (wae.getResponse() != null && wae.getResponse().getEntity() != null) {
               mapper = new WebApplicationExceptionMapperInternal();
            } else {
               mapper = executeExceptionMapperSuperClasses(e);;
               if (mapper == null) {
                  mapper = handleWebApplicationException(wae);
               }
            }
         } else if (e instanceof Failure) {
            // known exceptions that extend from Failure
            if (e instanceof WriterException) {
               mapper = handleWriterException(request, (WriterException) e, mappedThrowable);
            } else if (e instanceof ReaderException) {
               mapper = handleReaderException(request, (ReaderException) e, mappedThrowable);
            } else {
               mapper = handleFailure(request, (Failure)e);
            }
         } else {
            // ApplicationException is a Resteasy specific class
            if (e instanceof ApplicationException) {
               mapper = handleApplicationException(request,(ApplicationException) e,
                       mappedThrowable);
            } else {
               // look for mappers for any superclass of this unknown exception.
               mapper = executeExceptionMapperSuperClasses(e);
            }
         }
      }

      Response jaxrsResponse = null;
      if (mapper == null) {
         LogMessages.LOGGER.unknownException(request.getHttpMethod(),
                 request.getUri().getPath(), e);
         throw new UnhandledException(e);
      } else {
         RESTEasyTracingLogger logger = RESTEasyTracingLogger.getInstance(request);
         jaxrsResponse = executeMapper(mapper, mappedThrowable[0], logger);
      }
      return jaxrsResponse;
   }

   /**
    * Generate the response from the mapper or provide a 204 "No Content" status.
    * @param mapper
    * @param exception
    * @param logger
    * @return
    */
   private Response executeMapper(ExceptionMapper mapper, Throwable exception,
                                  RESTEasyTracingLogger logger) {
      if (logger == null)
         logger = RESTEasyTracingLogger.empty();

      mapperExecuted = true;

      final long timestamp = logger.timestamp("EXCEPTION_MAPPING");
      Response jaxrsResponse = mapper.toResponse(exception);

      if (jaxrsResponse == null) {
         jaxrsResponse = Response.status(HttpResponseCodes.SC_NO_CONTENT).build();
      }

      logger.logDuration("EXCEPTION_MAPPING", timestamp, mapper, exception,
              exception.getLocalizedMessage(), jaxrsResponse);

      return jaxrsResponse;
   }


   /**
    * A convenience mapper to enable consistent exception
    * handling of WebApplicationException which require special
    * internal handling.  This class can not be provided as a
    * builtin because of the nature of the special handling
    * provided in local methods.
    */
   private class WebApplicationExceptionMapperInternal implements
           ExceptionMapper<WebApplicationException> {

      public WebApplicationExceptionMapperInternal() {
      }

      public Response toResponse(WebApplicationException wae) {
         return wae.getResponse();
      }
   }

   /**
    * A convenience mapper to enable consistent exception
    * handling of Failure exception which require special
    * internal handling.  This class can not be provided as a
    * builtin because of the nature of the special handling
    * provided in local methods
    */
   private class FailureMapperInternal  implements ExceptionMapper<Failure> {

      public FailureMapperInternal() {
      }

      public Response toResponse(Failure failure) {

         Response response = failure.getResponse();

         if (response != null) {
            return response;
         } else {
            Response.ResponseBuilder builder = Response.status(failure.getErrorCode());
            if (failure.getMessage() != null)
               builder.type(MediaType.TEXT_HTML).entity(failure.getMessage());
            Response resp = builder.build();
            return resp;
         }
      }
   }
}
