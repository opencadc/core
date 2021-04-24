/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2020.                            (c) 2020.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*
*  NRC disclaims any warranties,        Le CNRC dénie toute garantie
*  expressed, implied, or               énoncée, implicite ou légale,
*  statutory, of any kind with          de quelque nature que ce
*  respect to the software,             soit, concernant le logiciel,
*  including without limitation         y compris sans restriction
*  any warranty of merchantability      toute garantie de valeur
*  or fitness for a particular          marchande ou de pertinence
*  purpose. NRC shall not be            pour un usage particulier.
*  liable in any event for any          Le CNRC ne pourra en aucun cas
*  damages, whether direct or           être tenu responsable de tout
*  indirect, special or general,        dommage, direct ou indirect,
*  consequential or incidental,         particulier ou général,
*  arising from the use of the          accessoire ou fortuit, résultant
*  software.  Neither the name          de l'utilisation du logiciel. Ni
*  of the National Research             le nom du Conseil National de
*  Council of Canada nor the            Recherches du Canada ni les noms
*  names of its contributors may        de ses  participants ne peuvent
*  be used to endorse or promote        être utilisés pour approuver ou
*  products derived from this           promouvoir les produits dérivés
*  software without specific prior      de ce logiciel sans autorisation
*  written permission.                  préalable et particulière
*                                       par écrit.
*
*  This file is part of the             Ce fichier fait partie du projet
*  OpenCADC project.                    OpenCADC.
*
*  OpenCADC is free software:           OpenCADC est un logiciel libre ;
*  you can redistribute it and/or       vous pouvez le redistribuer ou le
*  modify it under the terms of         modifier suivant les termes de
*  the GNU Affero General Public        la “GNU Affero General Public
*  License as published by the          License” telle que publiée
*  Free Software Foundation,            par la Free Software Foundation
*  either version 3 of the              : soit la version 3 de cette
*  License, or (at your option)         licence, soit (à votre gré)
*  any later version.                   toute version ultérieure.
*
*  OpenCADC is distributed in the       OpenCADC est distribué
*  hope that it will be useful,         dans l’espoir qu’il vous
*  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
*  without even the implied             GARANTIE : sans même la garantie
*  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
*  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
*  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
*  General Public License for           Générale Publique GNU Affero
*  more details.                        pour plus de détails.
*
*  You should have received             Vous devriez avoir reçu une
*  a copy of the GNU Affero             copie de la Licence Générale
*  General Public License along         Publique GNU Affero avec
*  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
*  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
*                                       <http://www.gnu.org/licenses/>.
*
*  $Revision: 5 $
*
************************************************************************
*/

package ca.nrc.cadc.net;

import ca.nrc.cadc.auth.NotAuthenticatedException;
import ca.nrc.cadc.auth.SSLUtil;
import ca.nrc.cadc.auth.SSOCookieCredential;
import ca.nrc.cadc.auth.SSOCookieManager;
import ca.nrc.cadc.io.ByteCountInputStream;
import ca.nrc.cadc.io.ByteLimitExceededException;
import ca.nrc.cadc.net.event.ProgressListener;
import ca.nrc.cadc.net.event.TransferEvent;
import ca.nrc.cadc.net.event.TransferListener;
import ca.nrc.cadc.util.CaseInsensitiveStringComparator;
import ca.nrc.cadc.util.FileMetadata;
import ca.nrc.cadc.util.HexUtil;
import ca.nrc.cadc.util.StringUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.Subject;
import org.apache.log4j.Logger;

/**
 * Base class for HTTP transfers.
 * @author pdowler
 */
public abstract class HttpTransfer implements Runnable {
    private static Logger log = Logger.getLogger(HttpTransfer.class);

    /**
     * Not documented in HttpURLConnection. A client specified Expect(ation)
     * was not satisfied.
     */
    static final int HTTP_EXPECT_FAIL = 417;
    
    /**
     * Not documented in HttpURLConnection. The resource is locked.
     */
    static final int HTTP_LOCKED = 423;

    static {
        String jv = "Java " + System.getProperty("java.version") + ";" + System.getProperty("java.vendor");
        String os = System.getProperty("os.name") + " " + System.getProperty("os.version");
        DEFAULT_USER_AGENT = "OpenCADC/" + HttpTransfer.class.getPackage().getName() + "/" + jv + "/" + os;
    }
    
    public static final String DEFAULT_USER_AGENT;
    public static final String CADC_CONTENT_LENGTH_HEADER = "X-CADC-Content-Length";
    public static final String CADC_STREAM_HEADER = "X-CADC-Stream";
    public static final String CADC_PARTIAL_READ_HEADER = "X-CADC-Partial-Read";
    
    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_MD5 = "Content-MD5";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String DIGEST = "Digest";
    
    public static final String SERVICE_RETRY = "Retry-After";

    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024; // 8KB
    // note: the combination of a large buffer, small-ish streamed put w/ no
    // content-length, and tomcat6 fails, plus apache+tomcat seem to have some
    // limits at 8k anyway

    public static enum RetryReason {
        /**
         * Never retry.
         */
        NONE(0),

        /**
         * Retry when the server says to do so (503 + Retry-After).
         */
        SERVER(1),

        /**
         * Retry for all failures deemed transient (undocumented). This option
         * includes the SERVER reasons.
         */
        TRANSIENT(2),

        /**
         * Retry for all failures (yes, even 4xx failures). This option includes
         * the TRANSIENT reasons.
         */
        ALL(3);

        private int value;

        private RetryReason(int val) { 
            this.value = val; 
        }
    }

    /**
     * The maximum retry delay (128 seconds).
     */
    public static final int MAX_RETRY_DELAY = 128;
    public static final int DEFAULT_RETRY_DELAY = 30;
    
    protected int maxRetries = 3;
    protected int retryDelay = 1; // 1, 2, 4 sec
    protected RetryReason retryReason = RetryReason.TRANSIENT;

    protected int numRetries = 0;
    protected int curRetryDelay = 0; // scaled after each retry

    protected int bufferSize = DEFAULT_BUFFER_SIZE;
    
    protected ProgressListener progressListener;
    protected TransferListener transferListener;
    protected boolean fireEvents = false;
    protected boolean fireCancelOnce = true;
    protected File localFile; // for events
    
    public String eventID = null;
    
    protected String userAgent;
    protected boolean userNio = false; // throughput not great, needs work before use
    protected boolean logIO = false;
    protected long writeTime = 0L;
    protected long readTime = 0L;

    protected boolean go;
    protected Thread thread;

    // state set by caller
    protected final URL remoteURL;
    protected boolean followRedirects;

    
    public Throwable failure;
    protected URL redirectURL;
    protected int responseCode = -1;
    protected boolean prepareStream;
    
    // latency tracking
    protected Long requestStartTime;
    protected Long responseLatency;
    
    private boolean customSSLSocketFactory = false;
    private final List<HttpRequestProperty> requestProperties = new ArrayList<HttpRequestProperty>();
    //private OutputStream requestStream;
    
    private final Map<String,String> responseHeaders = new TreeMap<String,String>(new CaseInsensitiveStringComparator());
    protected InputStream responseStream;
    private String contentType;
    private String contentEncoding;
    private String contentMD5;
    private long contentLength = -1;
    private Date lastModified;
    private String digest;
    
    // error capture
    protected int maxReadFully = 32 * 1024; // read up to 32k text responses into memory
    
    // output for run()
    protected OutputStream responseDestination;
    protected InputStreamWrapper responseStreamWrapper;

    protected final void assertNotNull(String name, Object value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }
    
    protected HttpTransfer(URL url, boolean followRedirects) {
        assertNotNull("url", url);
        this.remoteURL = url;
        this.followRedirects = followRedirects;
        
        this.go = true;
        this.userAgent = DEFAULT_USER_AGENT;

        String bsize = null;
        try {
            bsize = System.getProperty(HttpTransfer.class.getName() + ".bufferSize");
            if (bsize != null) {
                int mult = 1;
                String sz = bsize;
                bsize = bsize.trim();
                if (bsize.endsWith("k")) {
                    mult = 1024;
                    sz = bsize.substring(0, bsize.length() - 1);
                } else if (bsize.endsWith("m")) {
                    mult = 1024 * 1024;
                    sz = bsize.substring(0, bsize.length() - 1);
                }
                
                this.bufferSize = mult * Integer.parseInt(sz);
            }
        } catch (NumberFormatException warn) {
            log.warn("invalid buffer size: " + bsize + ", using default " + DEFAULT_BUFFER_SIZE);
            this.bufferSize = DEFAULT_BUFFER_SIZE;
        }
        log.debug("bufferSize: " + bufferSize);
    }

    /**
     * Initiate the request and perform all actions up to opening the response stream.
     * Non-error status can be checked by calling getResponseCode() and errors handled
     * via the thrown exception. The response can be read by calling getInputStream().
     * 
     * @throws ca.nrc.cadc.io.ByteLimitExceededException
     * @throws ca.nrc.cadc.net.ExpectationFailedException
     * @throws ca.nrc.cadc.net.ResourceAlreadyExistsException
     * @throws ca.nrc.cadc.net.ResourceNotFoundException
     * @throws ca.nrc.cadc.net.TransientException
     * @throws java.lang.InterruptedException
     */
    public abstract void prepare() 
        throws AccessControlException, NotAuthenticatedException,
            ByteLimitExceededException, ExpectationFailedException, 
            IllegalArgumentException, PreconditionFailedException, 
            ResourceAlreadyExistsException, ResourceNotFoundException, 
            TransientException, IOException, InterruptedException;
    
    /**
     * Call doAction in a loop that retries for TransientException (up to maxRetries
     * times). Subclasses must override doAction and satisfy the expected behaviour of
     * prepare or run (depending on where this is used).
     * 
     * @throws AccessControlException
     * @throws ByteLimitExceededException
     * @throws ExpectationFailedException
     * @throws IllegalArgumentException
     * @throws PreconditionFailedException
     * @throws ResourceAlreadyExistsException
     * @throws ResourceNotFoundException
     * @throws TransientException
     * @throws IOException
     * @throws InterruptedException 
     */
    protected void doActionWithRetryLoop()
        throws AccessControlException, NotAuthenticatedException,
            ByteLimitExceededException, ExpectationFailedException, 
            IllegalArgumentException, PreconditionFailedException, 
            ResourceAlreadyExistsException, ResourceNotFoundException, 
            TransientException, IOException, InterruptedException {
        
        boolean done = false;
        while (!done) {
            try {
                doAction();
                done = true;
            } catch (TransientException ex) {
                try {
                    long dt = 1000L * ex.getRetryDelay(); // to milliseconds
                    numRetries++;
                    if (numRetries == maxRetries) {
                        log.debug("retry limit reached");
                        throw ex;
                    }
                    log.debug("retry " + numRetries + " sleeping  for " + dt);
                    Thread.sleep(dt);
                    fireEvent(TransferEvent.RETRYING);
                } catch (InterruptedException iex) {
                    log.debug("retry interrupted");
                    done = true;
                }
            }
        }
    }
    
    protected void doAction()
        throws AccessControlException, NotAuthenticatedException,
            ByteLimitExceededException, ExpectationFailedException, 
            IllegalArgumentException, PreconditionFailedException, 
            ResourceAlreadyExistsException, ResourceNotFoundException, 
            TransientException, IOException, InterruptedException {
        // default: no-op
    }
    
    /**
     * Run the request to completion. The standard implementation here is to call prepare() 
     * and then read then response stream. This method will catch all thrown exceptions. 
     * The status can be checked by calling getResponseCode() and the exception accessed 
     * via getThrowable().
     */
    public void run() {
        if (failure == null) {
            try {
                if (responseStream == null) {
                    prepare();
                }
                readResponse(responseStream);
            } catch (Throwable t) {
                this.failure = t;
            }
        }
    }
    
    /**
     * Get the response stream. This is only valid after prepare() and not after run().
     * @return stream for reading the response or null if no response is available
     */
    public InputStream getInputStream() {
        return responseStream;
    }
    
    /**
     * Convenience: get the content-type returned by the server.
     * 
     * @return content-type or null
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Convenience: get the content encoding (usually compression) returned by the server.
     * 
     * @return content-encoding or null
     */
    public String getContentEncoding() {
        return contentEncoding;
    }

    /**
     * Convenience: get the size of the response.
     *
     * @return content-length or -1
     */
    public long getContentLength() { 
        return contentLength; 
    }
    
    /**
     * Convenience: Get the MD5 checksum sum of the response.
     * 
     * @return the MD5 checksum in hex form or null
     */
    public String getContentMD5() { 
        return contentMD5; 
    }

    /**
     * Last-modified timestamp from http header.
     * 
     * @return last-modified or null
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * URI of the Digest HTTP header. URI is of the form: algorithm:checksum
     * @return uri or null
     */
    public URI getDigest() {
        return DigestUtil.getURI(this.digest);
    }

    /**
     * Latency from start of call to first bytes of response. This could be null if some methods
     * do not or cannot track latency.
     * 
     * @return response latency in milliseconds
     */
    public Long getResponceLatency() {
        return responseLatency;
    }
    
    /**
     * Get an HTTP header value from the response. Subclasses may provide more convenient type-safe
     * methods to get specific standard header values.
     * 
     * @param key
     * @return header value, possibly null
     */
    public String getResponseHeader(String key) {
        return responseHeaders.get(key);
    }
    
    private void captureResponseHeaders(HttpURLConnection con) {
        responseHeaders.clear();
        for (String key : con.getHeaderFields().keySet()) {
            if (key != null) {
                String value = con.getHeaderField(key);
                if (value != null) {
                    responseHeaders.put(key, value);
                }
            }
        }
        // convenience
        this.contentType = responseHeaders.get(CONTENT_TYPE);
        this.contentEncoding = responseHeaders.get(CONTENT_ENCODING);
        this.contentMD5 = responseHeaders.get(CONTENT_MD5);
        this.contentLength = con.getContentLengthLong();
        long lastMod = con.getLastModified();
        if (lastMod > 0) {
            this.lastModified = new Date(lastMod);
        }
        this.digest = responseHeaders.get(DIGEST);
    }
    
    /**
     * Set the current following redirects behaviour.
     *
     * @param followRedirects
     */
    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    /**
     * If the response resulted in a redirect that wasn't followed, it
     * can be retrieved here.
     */
    public URL getRedirectURL() {
        return redirectURL;
    }


    /**
     * Enable retry (maxRetries &gt; 0) and set the maximum number of times
     * to retry before failing. The default is to retry only when the server
     * says to do so (e.g. 503 + Retry-After).
     *
     * @param maxRetries
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * Configure retry of failed transfers. If configured to retry, transfers will
     * be retried when failing for the reason which match the specified reason up
     * to maxRetries times. The retryDelay (in seconds) is scaled by a factor of two
     * for each subsequent retry (eg, 2, 4, 8, ...) in cases where the server response
     * does not provide a retry delay.
     * <p>
     * The default reason is RetryReason.SERVER.
     * </p>
     * 
     * @param maxRetries number of times to retry, 0 or negative to disable retry
     * @param retryDelay delay in seconds before retry
     * @param reason
     */
    public void setRetry(int maxRetries, int retryDelay, RetryReason reason) {
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
        this.retryReason = reason;
    }

    public URL getURL() { 
        return remoteURL; 
    }

    /**
     * Set the buffer size in bytes. Transfers allocate a buffer to use in
     * the IO loop and also wrap BufferedInputStream and BufferedOutputStream
     * around the underlying InputStream and OutputStream (if they are not already
     * buffered).
     * 
     * <p>Note: The buffer size can also be set with the system property
     * <code>ca.nrc.cadc.net.HttpTransfer.bufferSize</code> which is an integer
     * number of bytes. The value may be specified in KB by appending 'k' or MB by
     * appending 'm' (e.g. 16k or 2m).
     * </p>
     * 
     * @param bufferSize
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public final void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        if (userAgent == null) {
            this.userAgent = DEFAULT_USER_AGENT;
        }
    }
    
    /**
     * If enabled, the time spent on the buffer reading and writing will
     * be available through getIOReadTime() and getIOWriteTime().
     * @param logIO
     */
    public void setLogIO(boolean logIO) {
        this.logIO = logIO;
    }
    
    /*
     * If logIO is set to true, return the time in milliseconds spent
     * reading from the input stream.  Otherwise, return null.
     */
    public Long getIOReadTime() {
        if (logIO) {
            return readTime;
        }
        
        return null;
    }
    
    /*
     * If logIO is set to true, return the time in milliseconds spent
     * writing to the output stream.  Otherwise, return null.
     */
    public Long getIOWriteTime() {
        if (logIO) {
            return writeTime;
        }
        
        return null;
    }

    /**
     * Set the Digest header for the given checksum URI.
     *
     * @param checksumURI
     */
    public void setDigest(URI checksumURI) {
        String algorithm = checksumURI.getScheme();
        String checksum = DigestUtil.base64Encode(checksumURI.getSchemeSpecificPart());
        setRequestProperty(DIGEST, String.format("%s=%s", algorithm, checksum));
    }

    /**
     * Set single request headers. Do not set the same value twice by using this
     * method and the specific set methods (like setUserAgent, setContentType, etc) in this
     * class or subclasses.
     *
     * @param header
     * @param value
     */
    public void setRequestProperty(String header, String value) {
        requestProperties.add(new HttpRequestProperty(header, value));
    }

    /**
     * Set multiple request properties. Adds all the specified properties to
     * those set with setRequestProperty (if any).
     *
     * @see setRequestProperty
     * @param props
     */
    public void setRequestProperties(List<HttpRequestProperty> props) {
        if (props != null) {
            log.debug("add request properties: " + props.size());
            this.requestProperties.addAll(props);
        }
    }

    /**
     * Get currently set request properties. This list is modifiable up to the point that
     * prepare() or run() is called.
     * 
     * @return current list of request properties
     */
    public List<HttpRequestProperty> getRequestProperties() {
        return requestProperties;
    }

    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
        fireEvents = (progressListener != null || transferListener != null);
    }

    public void setTransferListener(TransferListener listener) {
        this.transferListener = listener;
        fireEvents = (progressListener != null || transferListener != null);
    }

    /**
     * Get the total number of retries performed.
     *
     * @return number of retries performed
     */
    public int getRetriesPerformed() {
        return numRetries;
    }

    /**
     * Get the ultimate (possibly after retries) HTTP response code.
     *
     * @return HTTP response code or -1 if no HTTP call made
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * If the transfer ultimately failed, this will return the last failure.

     * @return the last failure, or null if successful
     */
    public Throwable getThrowable() { 
        return failure; 
    }

    public void terminate() {
        this.fireEvents = false; // prevent run() and future calls to terminate from firing the CANCELLED event
        this.go = false;
        synchronized (this) {
            // other synchronized block in in the finally part of run()
            if (thread != null) {
                // give it a poke just in case it is blocked/slow
                log.debug("terminate(): interrupting " + thread.getName());
                try {
                    thread.interrupt();
                } catch (Throwable ignore) { 
                    // do nothing
                }
            }
        }
        
        fireCancelledEvent();
        this.fireCancelOnce = false;
    }

    /**
     *  Determine if the failure was transient according to the config options.
     * @param code status code
     * @param msg message
     * @param conn connection
     * @throws TransientException to cause retry
     */
    protected void checkTransient(int code, String msg, HttpURLConnection conn)
        throws TransientException {
        if (RetryReason.NONE.equals(retryReason)) {
            return;
        }

        boolean trans = false;
        int dt = 0;

        // try to get the retry delay from the response
        if (code == HttpURLConnection.HTTP_UNAVAILABLE) {
            if (!StringUtil.hasText(msg)) {
                msg = "server busy";
            }
            String retryAfter = responseHeaders.get(SERVICE_RETRY);
            log.debug("got " + HttpURLConnection.HTTP_UNAVAILABLE + " with " + SERVICE_RETRY + ": " + retryAfter);
            if (StringUtil.hasText(retryAfter)) {
                try {
                    dt = Integer.parseInt(retryAfter);
                    trans = true; // retryReason==SERVER satisfied
                    if (dt > MAX_RETRY_DELAY) {
                        dt = MAX_RETRY_DELAY;
                    }
                } catch (NumberFormatException nex) {
                    log.warn(SERVICE_RETRY + " after a 503 was not a number: " + retryAfter + ", ignoring");
                }
            }
        }

        if (RetryReason.TRANSIENT.equals(retryReason)) {
            switch (code) {
                case HttpURLConnection.HTTP_UNAVAILABLE:
                case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
                case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                case HttpURLConnection.HTTP_PAYMENT_REQUIRED:   // maybe it will become free :-)
                    trans = true;
                    break;
                default:
                    // do nothing
            }
        }
        
        if (RetryReason.ALL.equals(retryReason)) {
            trans = true;
        }

        if (trans) {
            if (dt == 0) {
                if (curRetryDelay == 0) {
                    curRetryDelay = retryDelay;
                }
                
                if (curRetryDelay > 0) {
                    dt = curRetryDelay;
                    curRetryDelay *= 2;
                } else {
                    dt = DEFAULT_RETRY_DELAY;
                }
            }
            
            throw new TransientException(msg.trim(), dt);
        }
    }
    
    protected void checkErrors(URL url, HttpURLConnection conn)
        throws AccessControlException, NotAuthenticatedException,
            ByteLimitExceededException, ExpectationFailedException, IllegalArgumentException,
            PreconditionFailedException, ResourceAlreadyExistsException, ResourceNotFoundException, 
            TransientException, IOException, InterruptedException {
        this.responseCode = conn.getResponseCode();
        log.debug("checkErrors: " + responseCode + " for " + url);
        
        captureResponseHeaders(conn);
        
        if (100 <= responseCode && responseCode < 400) {
            return;
        }
        log.debug("error: " + contentType + " " + contentLength);
        String responseBody = readErrorFromResponseBody(conn);
        log.debug("error: " + contentType + " " + contentLength + " response.length: " + responseBody.length());

        checkTransient(responseCode, responseBody, conn);
        
        switch (responseCode) {
            case HttpURLConnection.HTTP_BAD_REQUEST:
                throw new IllegalArgumentException(responseBody);

            case -1: 
                if (customSSLSocketFactory) {
                    // invalid client-cert
                    throw new NotAuthenticatedException(responseBody);
                }
                throw new IOException(responseBody);

            case HttpURLConnection.HTTP_UNAUTHORIZED:
                throw new NotAuthenticatedException(responseBody);
            case HttpURLConnection.HTTP_FORBIDDEN:
            case HTTP_LOCKED:
                throw new AccessControlException(responseBody);
            case HttpURLConnection.HTTP_NOT_FOUND:
                throw new ResourceNotFoundException(responseBody);
            case HttpURLConnection.HTTP_CONFLICT:
                throw new ResourceAlreadyExistsException(responseBody);
            case HttpURLConnection.HTTP_PRECON_FAILED:
                throw new PreconditionFailedException(responseBody);

            case HttpURLConnection.HTTP_ENTITY_TOO_LARGE:
                throw new ByteLimitExceededException(responseBody, -1);
            case HTTP_EXPECT_FAIL:
                throw new ExpectationFailedException(responseBody);

            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                throw new RuntimeException(responseBody);
                
            case HttpURLConnection.HTTP_UNAVAILABLE:
                throw new TransientException(responseBody);

            default:
                throw new IOException(responseBody);
        }
    }

    protected void checkRedirects(URL url, HttpURLConnection conn) 
        throws ResourceNotFoundException, IOException {
        // check for a redirect
        String location = conn.getHeaderField("Location");
        switch (responseCode) {
            case HttpURLConnection.HTTP_MOVED_TEMP:
            case HttpURLConnection.HTTP_SEE_OTHER:
                if (location == null) {
                    throw new RuntimeException("incomplete server response: status " + responseCode + " with Location: null");
                }
                this.redirectURL = new URL(location);
                log.debug("redirectURL: " + redirectURL);
                return;
            case HttpURLConnection.HTTP_MOVED_PERM:
                if (location == null) {
                    throw new ResourceNotFoundException("resource " + url + " moved permanently; Location: null");
                }
                this.redirectURL = new URL(location);
                log.debug("redirectURL: " + redirectURL);
                return;
            default:
        // no-op
        }
    }
    
    protected void findEventID(HttpURLConnection conn) {
        String eventHeader = null;
        if (transferListener != null) {
            eventHeader = transferListener.getEventHeader();
        }
        
        if (eventHeader != null) {
            this.eventID = conn.getHeaderField(eventHeader);
        }
    }

    private void fireCancelledEvent() {
        if (fireCancelOnce) {
            TransferEvent e = new TransferEvent(this, eventID, remoteURL, localFile, TransferEvent.CANCELLED);
            fireEvent(e);
        }
    }
    
    private void fireEvent(TransferEvent e) {
        log.debug("fireEvent: " + e);
        if (transferListener != null) {
            transferListener.transferEvent(e);
        }
        
        if (progressListener != null) {
            progressListener.transferEvent(e);
        }
    }

    protected void fireEvent(int state) {
        fireEvent(localFile, state);
    }

    protected void fireEvent(File file, int state) {
        fireEvent(file, state, null);
    }

    protected void fireEvent(File file, int state, FileMetadata meta) {
        if (fireEvents) {
            TransferEvent e = new TransferEvent(this, eventID, remoteURL, file, state);
            e.setFileMetadata(meta);
            fireEvent(e);
        }
    }

    protected void fireEvent(Throwable t) {
        fireEvent(localFile, t);
    }

    protected void fireEvent(File file, Throwable t) {
        if (fireEvents) {
            TransferEvent e = new TransferEvent(this, eventID, remoteURL, file, t);
            fireEvent(e);
        }
    }

    /**
     * @param sslConn
     */
    protected void initHTTPS(HttpsURLConnection sslConn) {
        customSSLSocketFactory = false;
        log.debug("initHTTPS: lazy init");
        AccessControlContext ac = AccessController.getContext();
        Subject s = Subject.getSubject(ac);
        SSLSocketFactory sf = SSLUtil.getSocketFactory(s);
        if (sf != null) {
            log.debug("setting SSLSocketFactory on " + sslConn.getClass().getName());
            sslConn.setSSLSocketFactory(sf);
            customSSLSocketFactory = true;
        }
    }

    /**
     * Perform the IO loop. This method reads from the input and writes to the output using an
     * internal byte array of the specified size.
     *
     * @param istream
     * @param ostream
     * @param sz
     * @param startingPos for resumed transfers, this effects the reported value seen by
     *     the progressListener (if set)
     * @return string representation of the content md5sum
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    protected String ioLoop(InputStream istream, OutputStream ostream, int sz, long startingPos)
        throws IOException, InterruptedException {
        log.debug("ioLoop: using java.io with byte[] buffer size " + sz + " startingPos " + startingPos);
        long readStart = 0;
        long writeStart = 0;
        byte[] buf = new byte[sz];

        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5"); 
        } catch (NoSuchAlgorithmException oops) {
            log.warn("failed to create MessageDigest(MD5): " + oops);
        }
        
        int nb = 0;
        int nb2 = 0;
        long tot = startingPos; // non-zero for resumed transfer
        int n = 0;

        if (progressListener != null) {
            progressListener.update(0, tot);
        }

        while (nb != -1) {
            // check/clear interrupted flag and throw if necessary
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            if (logIO) {
                readStart = System.currentTimeMillis();
            }
            
            nb = istream.read(buf, 0, sz);
            if (logIO) {
                readTime += System.currentTimeMillis() - readStart;
            }
            
            if (requestStartTime != null) {
                responseLatency = System.currentTimeMillis() - requestStartTime;
            }
            
            if (nb != -1) {
                if (nb < sz / 2) {
                    // try to get more data: merges a small chunk with a
                    // subsequent one to minimise write calls
                    if (logIO) {
                        readStart = System.currentTimeMillis();
                    }
                    
                    nb2 = istream.read(buf, nb, sz - nb);
                    if (logIO) {
                        readTime += System.currentTimeMillis() - readStart;
                    }
                    
                    if (nb2 > 0) {
                        nb += nb2;
                    }
                }
                
                //log.debug("write buffer: " + nb);
                if (md5 != null) {
                    md5.update(buf, 0, nb);
                }
                
                if (logIO) {
                    writeStart = System.currentTimeMillis();
                }
                
                ostream.write(buf, 0, nb);
                if (logIO) {
                    writeTime += System.currentTimeMillis() - writeStart;
                }
                
                tot += nb;
                if (progressListener != null) {
                    progressListener.update(nb, tot);
                }
            }
        }
        if (md5 != null) {
            byte[] md5sum = md5.digest();
            String ret = HexUtil.toHex(md5sum);
            return ret;
        }
        
        return null;
    }

    /**
     * Perform the IO loop using the nio library.
     *
     * @param istream
     * @param ostream
     * @param sz
     * @param startingPos
     * @throws IOException
     * @throws InterruptedException
     */
    protected void nioLoop(InputStream istream, OutputStream ostream, int sz, long startingPos)
        throws IOException, InterruptedException {
        // Note: If NIO is enabled, the logIO option should be added at
        // the same time (see ioLoop).
        
        log.debug("[Download] nioLoop: using java.nio with ByteBuffer size " + sz);
        // check/clear interrupted flag and throw if necessary
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        
        MessageDigest md5 = null;
        try { 
            md5 = MessageDigest.getInstance("MD5"); 
        } catch (NoSuchAlgorithmException oops) {
            log.warn("failed to create MessageDigest(MD5): " + oops);
        }

        ReadableByteChannel rbc = Channels.newChannel(istream);
        WritableByteChannel wbc = Channels.newChannel(ostream);

        long tot = startingPos; // non-zero for resumed transfer
        int count = 0;

        ByteBuffer buffer = ByteBuffer.allocate(sz);

        if (progressListener != null) {
            progressListener.update(count, tot);
        }

        while (count != -1) {
            // check/clear interrupted flag and throw if necessary
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            count = rbc.read(buffer);
            if (count != -1) {
                wbc.write((ByteBuffer)buffer.flip());
                buffer.flip();
                tot += count;
                if (progressListener != null) {
                    progressListener.update(count, tot);
                }
            }
        }
    }

    protected void setRequestSSOCookie(HttpURLConnection conn) {
        AccessControlContext acc = AccessController.getContext();
        Subject subj = Subject.getSubject(acc);
        if (subj != null) {
            Set<SSOCookieCredential> cookieCreds = subj
                    .getPublicCredentials(SSOCookieCredential.class);
            if ((cookieCreds != null) && (cookieCreds.size() > 0)) {
                // grab the first cookie that matches the domain
                boolean found = false;
                for (SSOCookieCredential cookieCred : cookieCreds) {
                    if (conn.getURL().getHost().endsWith(cookieCred.getDomain())) {
                        // HACK ("Pat Said") - this is rather horrenous, but in the java HTTP
                        // library, the cookie isn't sent with the redirect. But it doesn't flag it
                        // as a problem. This flags the problem early, allows us to detect attempts
                        // to send cookies + redirect via POST.
                        // GET (HttpDownload) works, and sends the cookies as expected.
                        if (followRedirects && "POST".equals(conn.getRequestMethod())) {
                            throw new UnsupportedOperationException("Attempt to follow redirect with cookies (POST).");
                        }

                        String cval = SSOCookieManager.DEFAULT_SSO_COOKIE_NAME
                                + "=\"" + cookieCred.getSsoCookieValue() + "\"";
                        conn.setRequestProperty("Cookie", cval);
                        log.debug("setRequestSSOCookie: " + cval);
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    log.debug("setRequestSSOCookie: no cookie for domain: "
                            + conn.getURL().getHost());
                }
            } else {
                log.debug("setRequestSSOCookie: no cookie");
            }
        }
    }

    protected void setRequestHeaders(HttpURLConnection conn) {
        log.debug("custom request properties: " + requestProperties.size());
        boolean doChunked = false;
        if (conn.getDoOutput()) {
            doChunked = true;
        }
        for (HttpRequestProperty rp : requestProperties) {
            String p = rp.getProperty();
            String v = rp.getValue();
            log.debug("set request property: " + p + "=" + v);
            if (CONTENT_LENGTH.equalsIgnoreCase(p)) {
                try {
                    long len = Long.parseLong(v);
                    conn.setFixedLengthStreamingMode(len);
                    doChunked = false;
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("invalid " + CONTENT_LENGTH + " header value: " + v);
                }
            } else {
                // default to string value
                conn.setRequestProperty(p, v);
            }
        }
        if (doChunked) {
            // note: setting the bufferSize to be larger than 8k without a content-length
            // seems to cause trouble with apache+ajp+tomcat in cases where the actual
            // payload is bigger than 8k but smaller than the buffer: avoid it for now (pdd)
            conn.setChunkedStreamingMode(8192);
        }
        conn.setRequestProperty("User-Agent", userAgent);
    }
    
    private void readResponse(InputStream istream) throws IOException, InterruptedException {
        log.debug("readResponse - START");
        
        if (responseStreamWrapper != null) {
            try {
                responseStreamWrapper.read(istream);
            } finally {
                responseStream = null;
            }
        } else if (responseDestination != null) {
            try {
                if (userNio) {
                    nioLoop(istream, responseDestination, 2 * bufferSize, 0);
                } else {
                    String md5 = ioLoop(istream, responseDestination, 2 * bufferSize, 0);
                    if (getContentMD5() != null && md5 != null) {
                        if (!md5.equals(getContentMD5())) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("MD5 mismatch: (header) ");
                            sb.append(getContentMD5()).append(" != ").append(md5).append(" (bytes)");
                            throw new IncorrectContentChecksumException(sb.toString());
                        }
                    }
                }
            } finally {
                responseStream = null;
            }
        } else {
            log.debug("response capture not enabled");
        }
    }
    
    // for reading text content into a String, eg. after an error for exception message
    private String readErrorFromResponseBody(HttpURLConnection conn)
        throws IOException, InterruptedException {
        try {
            InputStream istream = conn.getErrorStream();
            if (istream == null) {
                istream = conn.getInputStream();
            }
            return readResponseBody(istream);
        } catch (IOException ignore) {
            log.debug("no response body");
        }
        return "";
    }
    
    protected String readResponseBody(InputStream istream)
            throws IOException, InterruptedException {
        ByteCountInputStream bcis = new ByteCountInputStream(istream, maxReadFully);
        try (ByteArrayOutputStream byteArrayOstream = new ByteArrayOutputStream()) {
            try {
                ioLoop(bcis, byteArrayOstream, bufferSize, 0);
            } catch (ByteLimitExceededException truncated) {
                log.debug("error response body truncated", truncated);
            }
            byteArrayOstream.flush();
            return new String(byteArrayOstream.toByteArray(), "UTF-8");
        }
    }
}
