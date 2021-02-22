package org.jboss.resteasy.client.jaxrs.engines;

import org.apache.commons.io.output.DeferredFileOutputStream;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.Configurable;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.FileEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.jboss.resteasy.client.jaxrs.i18n.LogMessages;
import org.jboss.resteasy.client.jaxrs.i18n.Messages;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.resteasy.client.jaxrs.internal.FinalizedClientResponse;
import org.jboss.resteasy.util.CaseInsensitiveMap;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManualClosingApacheHttpClient5EngineAppraisal extends
        ManualClosingApacheHttpClientEngineCommon {

    private final HttpClient httpClient;
    private HttpHost defaultProxy = null;
    private HttpContext httpContext = null;

    public ManualClosingApacheHttpClient5EngineAppraisal() {
        super(true);
        this.httpClient = createDefaultHttpClient();
    }

    public ManualClosingApacheHttpClient5EngineAppraisal(final HttpHost defaultProxy) {
        super(true);
        this.httpClient = createDefaultHttpClient();
        this.defaultProxy = defaultProxy;
    }

    public ManualClosingApacheHttpClient5EngineAppraisal(final HttpClient httpClient) {
        super(true);
        this.httpClient = httpClient;
    }

    public ManualClosingApacheHttpClient5EngineAppraisal(final HttpClient httpClient,
                                                          final boolean closeHttpClient) {
        super(closeHttpClient);

        if (closeHttpClient && !(httpClient instanceof CloseableHttpClient)) {
            throw new IllegalArgumentException(
                    "httpClient must be a CloseableHttpClient instance in order for allowing engine to close it!");
        }
        this.httpClient = httpClient;
    }

    public ManualClosingApacheHttpClient5EngineAppraisal(final HttpClient httpClient,
                                                          final ManualClosingApacheHttpClient5EngineAppraisal.HttpContextProvider httpContextProvider) {
        super(true);
        httpContext = httpContextProvider.getContext();
        this.httpClient = httpClient;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public Response invoke(Invocation inv)
    {
        ClientInvocation request = (ClientInvocation)inv;
        String uri = request.getUri().toString();
        final HttpUriRequestBase httpMethod = createHttpMethod(uri, request.getMethod());
        final HttpResponse res;
        try
        {
            loadHttpMethod(request, httpMethod);

            if (System.getSecurityManager() == null) {
                res = httpClient.execute(httpMethod, httpContext);
            } else {
                try {
                    res = AccessController.doPrivileged(new PrivilegedExceptionAction<HttpResponse>() {
                        @Override
                        public HttpResponse run() throws Exception {
                            return httpClient.execute(httpMethod, httpContext);
                        }
                    });
                } catch (PrivilegedActionException pae) {
                    throw new RuntimeException(pae);
                }
            }
        }
        catch (Exception e)
        {
            LogMessages.LOGGER.clientSendProcessingFailure(e);
            throw new ProcessingException(Messages.MESSAGES.unableToInvokeRequest(e.toString()), e);
        }
        finally
        {
            cleanUpAfterExecute(httpMethod);
        }

        ClientResponse response = new FinalizedClientResponse(request.getClientConfiguration(), request.getTracingLogger())
        {
            InputStream stream;

            InputStream hc4Stream;

            @Override
            protected void setInputStream(InputStream is)
            {
                stream = is;
                resetEntity();
            }

            public InputStream getInputStream()
            {
                if (stream == null)
                {
                    HttpEntity entity = ((CloseableHttpResponse)res).getEntity();
                    if (entity == null)
                        return null;
                    try
                    {
                        hc4Stream = entity.getContent();
                        stream = createBufferedStream(hc4Stream);
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                return stream;
            }

            @Override
            public void releaseConnection() throws IOException
            {
                releaseConnection(true);
            }

            @Override
            public void releaseConnection(boolean consumeInputStream) throws IOException
            {
                if (consumeInputStream)
                {
                    // Apache Client 4 is stupid,  You have to get the InputStream and close it if there is an entity
                    // otherwise the connection is never released.  There is, of course, no close() method on response
                    // to make this easier.
                    try
                    {
                        // Another stupid thing...TCK is testing a specific exception from stream.close()
                        // so, we let it propagate up.
                        if (stream != null)
                        {
                            stream.close();
                        }
                        else
                        {
                            InputStream is = getInputStream();
                            if (is != null)
                            {
                                is.close();
                            }
                        }
                    }
                    finally
                    {
                        // just in case the input stream was entirely replaced and not wrapped, we need
                        // to close the apache client input stream.
                        if (hc4Stream != null)
                        {
                            try
                            {
                                hc4Stream.close();
                            }
                            catch (IOException ignored)
                            {

                            }
                        }
                        else
                        {
                            try
                            {
                                HttpEntity entity = ((CloseableHttpResponse)res).getEntity();
                                if (entity != null)
                                    entity.getContent().close();
                            }
                            catch (IOException ignored)
                            {
                            }

                        }

                    }
                }
                else if (res instanceof CloseableHttpResponse)
                {
                    try
                    {
                        ((CloseableHttpResponse) res).close();
                    }
                    catch (IOException e)
                    {
                        LogMessages.LOGGER.warn(Messages.MESSAGES.couldNotCloseHttpResponse(), e);
                    }
                }
            }

        };
        response.setProperties(request.getMutableProperties());
        response.setStatus(res.getCode());
        response.setReasonPhrase(res.getReasonPhrase());
        response.setHeaders(extractHeaders(res));
        response.setClientConfiguration(request.getClientConfiguration());
        return response;
    }

    public static CaseInsensitiveMap<String> extractHeaders(HttpResponse response) {
        final CaseInsensitiveMap<String> headers = new CaseInsensitiveMap<String>();

        for (Header header : response.getHeaders()) {
            headers.add(header.getName(), header.getValue());
        }
        return headers;
    }

    private HttpUriRequestBase createHttpMethod(String url, String restVerb) {
        if ("GET".equals(restVerb)) {
            return new HttpGet(url);
        } else if ("POST".equals(restVerb)) {
            return new HttpPost(url);
        } else {
            final String verb = restVerb;
            return new HttpPost(url) {
                @Override
                public String getMethod() {
                    return verb;
                }
            };
        }
    }

    private void loadHttpMethod(final ClientInvocation request,
                                HttpUriRequestBase httpMethod) throws Exception {
        if (isFollowRedirects()) {
            setRedirectRequired(request, httpMethod);
        } else {
            setRedirectNotRequired(request, httpMethod);
        }

        if (request.getEntity() != null) {
            if (httpMethod instanceof HttpGet) {
                throw new ProcessingException(Messages.MESSAGES.getRequestCannotHaveBody());
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            request.getDelegatingOutputStream().setDelegate(baos);
            try {
                HttpEntity entity = buildEntity(request);
                HttpPost post = (HttpPost) httpMethod;
                commitHeaders(request, httpMethod);
                post.setEntity(entity);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else // no body
        {
            commitHeaders(request, httpMethod);
        }
    }

    private void commitHeaders(ClientInvocation request, HttpUriRequestBase httpMethod) {
        MultivaluedMap<String, String> headers = request.getHeaders().asMap();
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            List<String> values = header.getValue();
            for (String value : values) {
                httpMethod.addHeader(header.getKey(), value);
            }
        }
    }

    /**
     * If passed httpMethod is of type HttpPost then obtain its entity. If the entity has an enclosing File then
     * delete it by invoking this method after the request has completed. The entity will have an enclosing File
     * only if it was too huge to fit into memory.
     *
     * @param httpMethod - the httpMethod to clean up.
     */
    private void cleanUpAfterExecute(final HttpUriRequestBase httpMethod) {
        if (httpMethod != null && httpMethod instanceof HttpPost) {
            HttpPost postMethod = (HttpPost) httpMethod;
            HttpEntity entity = postMethod.getEntity();
            if (entity != null && entity instanceof FileExposingFileEntity) {
                File tempRequestFile = ((FileExposingFileEntity) entity).getFile();
                try {
                    boolean isDeleted = tempRequestFile.delete();
                    if (!isDeleted) {
                        handleFileNotDeletedError(tempRequestFile, null);
                    }
                } catch (Exception ex) {
                    handleFileNotDeletedError(tempRequestFile, ex);
                }
            }
        }
    }

    /**
     * Build the HttpEntity to be sent to the Service as part of (POST) request. Creates a off-memory
     * {@link FileExposingFileEntity} or a regular in-memory {@link ByteArrayEntity} depending on if the request
     * OutputStream fit into memory when built by calling.
     *
     * @param request -
     * @return - the built HttpEntity
     * @throws IOException -
     */
    protected HttpEntity buildEntity(final ClientInvocation request) throws IOException
    {
        AbstractHttpEntity entityToBuild = null;
        DeferredFileOutputStream memoryManagedOutStream = writeRequestBodyToOutputStream(request);

        MediaType mediaType = request.getHeaders().getMediaType();
        ContentType tmpContentType = null;
        if (mediaType != null) {
            final List<NameValuePair> newParams = new ArrayList<>();
            for (final Map.Entry<String, String> entry : mediaType.getParameters().entrySet()) {
                newParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            tmpContentType = ContentType.create(
                    mediaType.getType() + "/" + mediaType.getSubtype(),
                    newParams.toArray(new NameValuePair[newParams.size()]));
        }

        if (memoryManagedOutStream.isInMemory())
        {
            ByteArrayEntity entityToBuildByteArray =
                    new ByteArrayEntity(memoryManagedOutStream.getData(),
                            tmpContentType,
                            request.isChunked());
            entityToBuild = entityToBuildByteArray;
        }
        else
        {
            entityToBuild = new FileExposingFileEntity(memoryManagedOutStream.getFile(),
                    tmpContentType);
        }

        return (HttpEntity) entityToBuild;
    }

    /**
     * Creates the request OutputStream, to be sent to the end Service invoked, as a
     * <a href="http://commons.apache.org/io/api-release/org/apache/commons/io/output/DeferredFileOutputStream.html"
     * >DeferredFileOutputStream</a>.
     *
     * @param request -
     * @return - DeferredFileOutputStream with the ClientRequest written out per HTTP specification.
     * @throws IOException -
     */
    private DeferredFileOutputStream writeRequestBodyToOutputStream(
            final ClientInvocation request) throws IOException {
        DeferredFileOutputStream memoryManagedOutStream = new DeferredFileOutputStream(
                this.fileUploadInMemoryThresholdLimit * getMemoryUnitMultiplier(),
                getTempfilePrefix(), ".tmp", this.fileUploadTempFileDir);
        request.getDelegatingOutputStream().setDelegate(memoryManagedOutStream);
        request.writeRequestBody(request.getEntityStream());
        memoryManagedOutStream.close();
        return memoryManagedOutStream;
    }


    /**
     * We use {@link org.apache.http.entity.FileEntity} as the {@link HttpEntity} implementation when the request OutputStream has been
     * saved to a File on disk (because it was too large to fit into memory see however, we have to delete
     * the File supporting the <code>FileEntity</code>, otherwise the disk will soon run out of space - remember
     * that there can be very huge files, in GB range, processed on a regular basis - and FileEntity exposes its
     * content File as a protected field. For the enclosing parent class ( {@link ApacheHttpClient4Engine} ) to be
     * able to get a handle to this content File and delete it, this class expose the content File.<br>
     * This class is private scoped to prevent access to this content File outside of the parent class.
     *
     * @author <a href="mailto:stikoo@digitalriver.com">Sandeep Tikoo</a>
     */
    private static class FileExposingFileEntity extends FileEntity {
        private static File pFile;
        /**
         * @param pFile        -
         * @param pContentType -
         */
        @SuppressWarnings("deprecation")
        FileExposingFileEntity(final File pFile, final ContentType pContentType) {
            super(pFile, pContentType);
            this.pFile = pFile;
        }

        /**
         * @return - the content File enclosed by this FileEntity.
         */
        File getFile() {
            return this.pFile;
        }
    }

    private HttpClient createDefaultHttpClient() {
        final HttpClientBuilder builder = HttpClientBuilder.create();
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        if (defaultProxy != null) {
            requestBuilder.setProxy(defaultProxy);
        }
        builder.disableContentCompression();
        builder.setDefaultRequestConfig(requestBuilder.build());
        return builder.build();
    }

    public HttpHost getDefaultProxy() {
        Configurable clientConfiguration = (Configurable) httpClient;
        return clientConfiguration.getConfig().getProxy();
    }

    private void setRedirectRequired(final ClientInvocation request,
                                       final HttpUriRequestBase httpMethod) {
        RequestConfig.Builder requestBuilder = RequestConfig.copy(
                getCurrentConfiguration(request, httpMethod));
        requestBuilder.setRedirectsEnabled(true);
        httpMethod.setConfig(requestBuilder.build());
    }

    private void setRedirectNotRequired(final ClientInvocation request,
                                          final HttpUriRequestBase httpMethod) {
        RequestConfig.Builder requestBuilder = RequestConfig.copy(
                getCurrentConfiguration(request, httpMethod));
        requestBuilder.setRedirectsEnabled(false);
        httpMethod.setConfig(requestBuilder.build());
    }

    private RequestConfig getCurrentConfiguration(final ClientInvocation request,
                                                  final HttpUriRequestBase httpMethod) {
        RequestConfig baseConfig;
        if (httpMethod != null && httpMethod.getConfig() != null) {
            baseConfig = httpMethod.getConfig();
        } else {
            ManualClosingApacheHttpClient5EngineAppraisal engine =
                    ((ManualClosingApacheHttpClient5EngineAppraisal) request.getClient().httpEngine());
            baseConfig = ((Configurable) engine.getHttpClient()).getConfig();
            if (baseConfig == null) {
                Configurable clientConfiguration = (Configurable) httpClient;
                baseConfig = clientConfiguration.getConfig();
            }
        }
        return baseConfig;
    }

    @Override
    public void close() {
        if (isClosed()) {
            return;
        }

        if (isAllowClosingHttpClient() && httpClient != null) {
            try {
                ((CloseableHttpClient) httpClient).close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        setClosed(true);
    }

    /**
     *
     */
    public interface HttpContextProvider
    {
        org.apache.hc.core5.http.protocol.HttpContext getContext();
    }
}
