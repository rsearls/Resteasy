package org.jboss.resteasy.client.jaxrs.engines;

import org.jboss.resteasy.client.jaxrs.i18n.LogMessages;
import org.jboss.resteasy.client.jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.config.ConfigurationFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * An Apache HTTP engine for use with the new Builder Config style.
 */
public abstract class ManualClosingApacheHttpClientEngineCommon implements
        ApacheHttpClientEngine {

    /**
     * Used to build temp file prefix.
     */
    private static final String processId;

    static {
        try {
            processId = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws Exception {
                    return ManagementFactory.getRuntimeMXBean()
                            .getName().replaceAll("[^0-9a-zA-Z]", "");
                }
            });
        } catch (PrivilegedActionException pae) {
            throw new RuntimeException(pae);
        }

    }

    private boolean closed;
    protected final boolean allowClosingHttpClient;
    protected SSLContext sslContext;
    protected HostnameVerifier hostnameVerifier;
    protected int responseBufferSize = 8192;
    protected boolean chunked = false;
    protected boolean followRedirects = false;

    /**
     * For uploading File's over JAX-RS framework, this property, together with {@link #fileUploadMemoryUnit},
     * defines the maximum File size allowed in memory. If fileSize exceeds this size, it will be stored to
     * {@link #fileUploadTempFileDir}. <br>
     * <br>
     * Defaults to 1 MB
     */
    protected int fileUploadInMemoryThresholdLimit = 1;

    /**
     * The unit for {@link #fileUploadInMemoryThresholdLimit}. <br>
     * <br>
     * Defaults to MB.
     *
     * @see MemoryUnit
     */
    protected MemoryUnit fileUploadMemoryUnit = MemoryUnit.MB;

    /**
     * Temp directory to write output request stream to. Any file to be uploaded has to be written out to the
     * output request stream to be sent to the service and when the File is too huge the output request stream is
     * written out to the disk rather than to memory. <br>
     * <br>
     * Defaults to JVM temp directory.
     */
    protected File fileUploadTempFileDir = new File(ConfigurationFactory
            .getInstance()
            .getConfiguration()
            .getOptionalValue("java.io.tmpdir", String.class)
            .orElse(null));

    public ManualClosingApacheHttpClientEngineCommon(final boolean closeHttpClient) {
        this.allowClosingHttpClient = closeHttpClient;
    }

    protected boolean isAllowClosingHttpClient() {
        return allowClosingHttpClient;
    }

    /**
     * Response stream is wrapped in a BufferedInputStream.  Default is 8192.
     * Value of 0 will not wrap it.
     * Value of -1 will use a SelfExpandingBufferedInputStream
     *
     * @return response buffer size
     */
    public int getResponseBufferSize() {
        return responseBufferSize;
    }

    /**
     * Response stream is wrapped in a BufferedInputStream.  Default is 8192.
     * Value of 0 will not wrap it.
     * Value of -1 will use a SelfExpandingBufferedInputStream
     *
     * @param responseBufferSize response buffer size
     */
    public void setResponseBufferSize(int responseBufferSize) {
        this.responseBufferSize = responseBufferSize;
    }

    /**
     * Based on memory unit
     *
     * @return threshold limit
     */
    public int getFileUploadInMemoryThresholdLimit() {
        return fileUploadInMemoryThresholdLimit;
    }

    public void setFileUploadInMemoryThresholdLimit(int fileUploadInMemoryThresholdLimit) {
        this.fileUploadInMemoryThresholdLimit = fileUploadInMemoryThresholdLimit;
    }

    public MemoryUnit getFileUploadMemoryUnit() {
        return fileUploadMemoryUnit;
    }

    public void setFileUploadMemoryUnit(MemoryUnit fileUploadMemoryUnit) {
        this.fileUploadMemoryUnit = fileUploadMemoryUnit;
    }

    public File getFileUploadTempFileDir() {
        return fileUploadTempFileDir;
    }

    public void setFileUploadTempFileDir(File fileUploadTempFileDir) {
        this.fileUploadTempFileDir = fileUploadTempFileDir;
    }

    @Override
    public SSLContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    protected InputStream createBufferedStream(InputStream is) {
        if (responseBufferSize == 0) {
            return is;
        }
        if (responseBufferSize < 0) {
            return new SelfExpandingBufferredInputStream(is);
        }
        return new BufferedInputStream(is, responseBufferSize);
    }

    public abstract Response invoke(Invocation inv);

    public boolean isChunked() {
        return chunked;
    }

    public void setChunked(boolean chunked) {
        this.chunked = chunked;
    }

    @Override
    public boolean isFollowRedirects() {
        return followRedirects;
    }

    @Override
    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    /**
     * Use context information, which will include node name, to avoid conflicts in case of multiple VMS using same
     * temp directory location.
     *
     * @return -
     */
    protected String getTempfilePrefix() {
        return processId;
    }

    /**
     * @return - the constant to multiply {@link #fileUploadInMemoryThresholdLimit} with based on
     * {@link #fileUploadMemoryUnit} enumeration value.
     */
    protected int getMemoryUnitMultiplier() {
        switch (this.fileUploadMemoryUnit) {
            case BY:
                return 1;
            case KB:
                return 1024;
            case MB:
                return 1024 * 1024;
            case GB:
                return 1024 * 1024 * 1024;
        }
        return 1;
    }

    /**
     * Log that the file did not get deleted but prevent the request from failing by eating the exception.
     * Register the file to be deleted on exit, so it will get deleted eventually.
     *
     * @param tempRequestFile -
     * @param ex              - a null may be passed in which case this param gets ignored.
     */
    protected void handleFileNotDeletedError(File tempRequestFile, Exception ex) {
        LogMessages.LOGGER.warn(Messages.MESSAGES.couldNotDeleteFile(tempRequestFile.getAbsolutePath()), ex);
        tempRequestFile.deleteOnExit();
    }

    protected boolean isClosed() {
        return closed;
    }

    protected void setClosed(boolean c) {
        closed = c;
    }

    public abstract void close();

}
