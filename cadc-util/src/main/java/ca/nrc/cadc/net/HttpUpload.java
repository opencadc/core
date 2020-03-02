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
import ca.nrc.cadc.io.ByteLimitExceededException;
import ca.nrc.cadc.net.event.TransferEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessControlException;
import javax.net.ssl.HttpsURLConnection;
import org.apache.log4j.Logger;

/**
 * Perform an upload (PUT). 
 * 
 * <p>Note: Redirects are never followed.
 * 
 * @author zhangsa, pdowler
 *
 */
public class HttpUpload extends HttpTransfer {
    private static Logger log = Logger.getLogger(HttpUpload.class);

    // input
    private File srcFile;
    private InputStream istream;
    private OutputStreamWrapper wrapper;
    private FileContent fileContent;
    private Long srcContentLength;
    
    public HttpUpload(File src, URL dest) {
        super(dest, false);
        this.srcFile = src;
        if (srcFile == null) {
            throw new IllegalArgumentException("source File cannot be null");
        }
        this.srcContentLength = srcFile.length();
    }

    public HttpUpload(FileContent src, URL dest) {
        super(dest, false);
        this.fileContent = src;
        if (fileContent == null) {
            throw new IllegalArgumentException("source FileContent cannot be null");
        }
        byte[] b = fileContent.getBytes();
        this.istream = new ByteArrayInputStream(b);
        this.srcContentLength = (long) b.length;
    }
    
    public HttpUpload(InputStream src, URL dest) {
        super(dest, false);
        this.istream = src;
        if (istream == null) {
            throw new IllegalArgumentException("source InputStream cannot be null");
        }
    }

    public HttpUpload(OutputStreamWrapper src, URL dest) {
        super(dest, false);
        this.wrapper = src;
        if (wrapper == null) {
            throw new IllegalArgumentException("source OutputStreamWrapper cannot be null");
        }
    }
    
    /**
     * @param val
     * @deprecated use setRequestProperty(String, String)
     */
    @Deprecated
    public void setContentLength(long val) {
        setRequestProperty(CONTENT_LENGTH, Long.toString(val));
    }
    
    /**
     * @param val
     * @deprecated use setRequestProperty(String, String)
     */
    @Deprecated
    public void setContentMD5(String val) {
        setRequestProperty(CONTENT_MD5, val);
    }
    
    /**
     * @param val
     * @deprecated use setRequestProperty(String, String)
     */
    @Deprecated
    public void setContentEncoding(String val) {
        setRequestProperty(CONTENT_ENCODING, val);
    }
    
    /**
     * @param val
     * @deprecated use setRequestProperty(String, String)
     */
    @Deprecated
    public void setContentType(String val) {
        setRequestProperty(CONTENT_TYPE, val);
    }
    
    /**
     * @return response converted to UTF-8 string
     * @throws java.io.IOException
     * @deprecated use prepare() and getInputStream()
     */
    @Deprecated
    public String getResponseBody() throws IOException {
        try {
            if (responseStream != null) {
                return readResponseBody(responseStream);
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException("read interrupted", ex);
        }
        return null;
    }

    public void setResponseDestination(OutputStream responseDestination) {
        this.responseDestination = responseDestination;
        this.responseStreamWrapper = null;
    }

    public void setResponseStreamWrapper(InputStreamWrapper responseStreamWrapper) {
        this.responseStreamWrapper = responseStreamWrapper;
        this.responseDestination = null;
    }
    
    @Override 
    public void setFollowRedirects(boolean followRedirects) {
        if (followRedirects) {
            throw new IllegalArgumentException("followRedirects=true not allowed for upload");
        }
    }

    @Override
    public String toString() { 
        return "HttpUpload[" + remoteURL + "," + srcFile + "]"; 
    }
    
    @Override
    public void prepare() 
        throws AccessControlException, NotAuthenticatedException,
            ByteLimitExceededException, ExpectationFailedException, 
            IllegalArgumentException, PreconditionFailedException, 
            ResourceAlreadyExistsException, ResourceNotFoundException, 
            TransientException, IOException, InterruptedException {
        
        doActionWithRetryLoop();
    }
    
    @Override
    protected void doAction()
        throws AccessControlException, NotAuthenticatedException,
            ByteLimitExceededException, ExpectationFailedException, 
            IllegalArgumentException, PreconditionFailedException, 
            ResourceAlreadyExistsException, ResourceNotFoundException, 
            TransientException, IOException, InterruptedException {
        log.debug(this.toString());
        if (!go) {
            return; // cancelled while queued, event notification handled in terminate()
        }

        boolean throwTE = false;
        try {
            this.thread = Thread.currentThread();

            fireEvent(TransferEvent.CONNECTING);

            HttpURLConnection conn = (HttpURLConnection) this.remoteURL.openConnection();
            conn.setRequestMethod("PUT");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
        
            setRequestSSOCookie(conn);
            if (conn instanceof HttpsURLConnection) {
                HttpsURLConnection sslConn = (HttpsURLConnection) conn;
                initHTTPS(sslConn);
            }

            doPut(conn);
            this.responseStream = conn.getInputStream();
        } catch (InterruptedException iex) {
            // need to catch this or it looks like a failure instead of a cancel
            this.go = false;
        } catch (TransientException tex) {
            log.debug("caught: " + tex);
            throwTE = true;
            throw tex;
        } catch (Throwable t) {
            failure = t;
        } finally {
            if (istream != null) {
                log.debug("closing InputStream");
                try { 
                    istream.close(); 
                } catch (Exception ignore) { 
                    // do nothing
                }
            }

            synchronized (this) {
                // vs sync block in terminate()
                if (thread != null) {
                    // clear interrupt status
                    if (Thread.interrupted()) {
                        go = false;
                    }
                    
                    this.thread = null;
                }
            }

            if (!go) {
                log.debug("cancelled");
                fireEvent(TransferEvent.CANCELLED);
            } else if (failure != null) {
                log.debug("failed: " + failure);
                fireEvent(failure);
            } else if (!throwTE) {
                log.debug("completed");
                fireEvent(TransferEvent.COMPLETED);
            }
        }
    }

    private void doPut(HttpURLConnection conn)
        throws AccessControlException, NotAuthenticatedException,
            ByteLimitExceededException, ExpectationFailedException, 
            IllegalArgumentException, PreconditionFailedException, 
            ResourceAlreadyExistsException, ResourceNotFoundException, 
            TransientException, IOException, InterruptedException {
        OutputStream ostream = null;

        if (srcContentLength != null) {
            try {
                // Try using the setFixedLengthStreamingMode method that takes a long as a parameter.
                // (Only available in Java 7 and up)
                Method longContentLengthMethod = conn.getClass().getMethod("setFixedLengthStreamingMode", long.class);
                longContentLengthMethod.invoke(conn, new Long(srcContentLength));
                log.debug("invoked setFixedLengthStreamingMode(long)");
            } catch (Exception noCanDo) {
                // Check if the file size is greater than Integer.MAX_VALUE
                if (srcContentLength > Integer.MAX_VALUE) {
                    // Cannot set the header length in the standard fashion, so
                    // set it to chunked streaming mode and set a custom header
                    // for use by servers that recognize this attribute
                    conn.setChunkedStreamingMode(bufferSize);
                    conn.setRequestProperty(CADC_CONTENT_LENGTH_HEADER, Long.toString(srcContentLength));
                    log.debug("invoked setChunkedStreamingMode");
                } else {
                    // Set the file size with integer representation
                    conn.setFixedLengthStreamingMode((int) srcContentLength.intValue());
                    log.debug("invoked setFixedLengthStreamingMode(int)");
                }
            }
        } else {
            // note: settig the bufferSize to be larger than 8k without a content-length
            // seems to cause troble with apache+ajp+tomcat6 in cases where the actual
            // payload is bigger than 8k but smaller than the buffer: avoid it for now (pdd)
            conn.setChunkedStreamingMode(8192);
            log.debug("invoked setChunkedStreamingMode");
        }

        setRequestHeaders(conn);

        FileInputStream fin = null;
        InputStream in = null;
        try {
            ostream = conn.getOutputStream();
            fireEvent(TransferEvent.CONNECTED);
            
            if (srcFile != null) {
                fin = new FileInputStream(srcFile);
                in = fin;
            } else if (istream != null) {
                in = istream;
            }
            
            if (!(ostream instanceof BufferedOutputStream)) {
                log.debug("using BufferedOutputStream");
                ostream = new BufferedOutputStream(ostream, bufferSize);
            }

            if (in != null) {
                log.debug("using BufferedInputStream");
                in = new BufferedInputStream(in, bufferSize);
            }

            fireEvent(TransferEvent.TRANSFERING);

            if (in != null) {
                ioLoop(in, ostream, 2 * this.bufferSize, 0);
            } else {
                wrapper.write(ostream);
            }

            log.debug("OutputStream.flush");
            long writeStart = System.currentTimeMillis();
            ostream.flush();
            if (logIO) {
                long flushTime = System.currentTimeMillis() - writeStart;
                writeTime += flushTime;
                log.debug("Time (ms) to flush: " + flushTime);
            }
            
            log.debug("OutputStream.flush OK");
        } finally {
            try {
                if (ostream != null) {
                    log.debug("OutputStream.close");
                    ostream.close();
                    log.debug("OutputStream.close OK");
                }
            } catch (IOException ignore) {
                log.debug("OutputStream.close FAIL", ignore);
            }
            
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException ignore) { 
                // do nothing
            }
        }

        checkErrors(remoteURL, conn);
    }
}
