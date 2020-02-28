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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

/**
 * Perform an HTTP Post.
 * 
 * <p>Post data can be supplied as either parameters in a map or as a single
 * string.  For Posts that may result in large response data, the constructor
 * with an output stream should be used.
 * 
 * @author majorb, pdowler
 *
 */
public class HttpPost extends HttpTransfer {
    private static Logger log = Logger.getLogger(HttpPost.class);

    // a string that isn't going to be inside params or files
    private static final String MULTIPART_BOUNDARY = UUID.randomUUID().toString();
    private static final String LINE_FEED = "\r\n";
    
    // request information
    private Map<String,Object> paramMap = new TreeMap<String,Object>(); // removes a lot of checking for null
    private FileContent inputFileContent;
    
    /**
     * HttpPost constructor.  Redirects will be followed.
     * Ideal for large expected responses.
     * 
     * @param url The POST destination.
     * @param map A map of the data to be posted.
     * @param outputStream An output stream to capture the response data.
     */
    public HttpPost(URL url, Map<String, Object> map, OutputStream outputStream) {
        super(url, true);
        this.paramMap = map;
        super.responseDestination = outputStream;
    }
    
    public HttpPost(URL url, Map<String, Object> map, InputStreamWrapper istreamWrapper) {
        this(url, map, true);
        super.responseStreamWrapper = istreamWrapper;
    }
    
    /**
     * HttpPost constructor.
     * 
     * @param url The POST destination.
     * @param map A map of the data to be posted.
     * @param followRedirects Whether or not to follow server redirects.
     */
    public HttpPost(URL url, Map<String, Object> map, boolean followRedirects) {
        super(url, followRedirects);
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException("parameters cannot be empty.");
        }
        this.paramMap = map;
    }
    
    /**
     * HttpPost constructor.
     * 
     * @param url
     * @param input
     * @param followRedirects
     */
    public HttpPost(URL url, FileContent input, boolean followRedirects) {
        super(url, followRedirects);
        this.inputFileContent = input;
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }
    }
    
    /**
     * HttpPost constructor.
     * 
     * @param url The POST destination
     * @param content The content to post.
     * @param contentType The type of the content.
     * @param followRedirects Whether or not to follow server redirects.
     * @deprecated use a FileContent object instead
     */
    @Deprecated
    public HttpPost(URL url, String content, String contentType, boolean followRedirects) {
        this(url, new FileContent(content, contentType, Charset.forName("UTF-8")), followRedirects);
    }
    
    @Override
    public String toString() { 
        return "HttpPost[" + remoteURL + "]"; 
    }
    
    /**
     * @return Content-Encoding
     * @deprecated use getResponseHeader(HttpTransfer.CONTENT_ENCODING)
     */
    @Deprecated
    public String getResponseContentEncoding() {
        return getResponseHeader(HttpTransfer.CONTENT_ENCODING);
    }

    /**
     * @return Content-Type
     * @deprecated use getResponseHeader(HttpTransfer.CONTENT_TYPE)
     */
    @Deprecated
    public String getResponseContentType() {
        return getResponseHeader(HttpTransfer.CONTENT_TYPE);
    }
    
    /**
     * @return response converted to UTF-8 string
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     * @deprecated use prepare() and getInputStream()
     */
    @Deprecated
    public String getResponseBody() throws IOException, InterruptedException {
        if (responseStream != null) {
            return readResponseBody(responseStream);
        }
        return null;
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

        try {
            this.thread = Thread.currentThread();
            HttpURLConnection conn = (HttpURLConnection) this.remoteURL.openConnection();
            conn.setRequestMethod("POST");
            setRequestSSOCookie(conn);
            if (conn instanceof HttpsURLConnection) {
                HttpsURLConnection sslConn = (HttpsURLConnection) conn;
                initHTTPS(sslConn);
            }
            
            doPost(conn);
            this.responseStream = conn.getInputStream();
        } finally {
            if (responseDestination != null) {
                log.debug("closing OutputStream");
                try { 
                    responseDestination.close(); 
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
        }
    }
    
    private void doPost(HttpURLConnection conn)
        throws AccessControlException, NotAuthenticatedException,
            ByteLimitExceededException, ExpectationFailedException, 
            IllegalArgumentException, PreconditionFailedException, 
            ResourceAlreadyExistsException, ResourceNotFoundException, 
            TransientException, IOException, InterruptedException {
        
        if (inputFileContent != null) {
            HttpPost.this.doPost(conn, inputFileContent);
        } else {
            // separate params from uploads
            Map<String,List<Object>> pmap = new TreeMap<String,List<Object>>();
            Map<String,Object> uploads = new TreeMap<String,Object>();
            for (Map.Entry<String,Object> me : paramMap.entrySet()) {
                String key = me.getKey();
                Object value = me.getValue();
                if (value instanceof File) {
                    File u = (File) value;
                    uploads.put(key, u);
                } else if (value instanceof FileContent) {
                    FileContent fc = (FileContent) value;
                    uploads.put(key, fc);
                } else if (value instanceof Collection) {
                    Collection vals = (Collection) value;
                    List<Object> pmv = new ArrayList<Object>(vals.size());
                    pmv.addAll(vals);
                    pmap.put(key, pmv);
                } else {
                    List<Object> pmv = new ArrayList<Object>(1);
                    pmv.add(value);
                    pmap.put(key, pmv);
                }
            }
            
            HttpPost.this.doPost(conn, pmap, uploads);
        }
        
        checkRedirects(remoteURL, conn);
    }
    
    private void doPost(HttpURLConnection conn, FileContent input)
        throws AccessControlException, NotAuthenticatedException,
            ByteLimitExceededException, ExpectationFailedException, 
            IllegalArgumentException, PreconditionFailedException, 
            ResourceAlreadyExistsException, ResourceNotFoundException, 
            TransientException, IOException, InterruptedException {
        
        byte[] buf = input.getBytes();
        String len = Long.toString(buf.length);
        conn.setRequestProperty("Content-Length", len);
        log.debug("POST Content-Length: " + len);
        conn.setRequestProperty("Content-Type", input.getContentType());
        log.debug("POST Content-Type: " + input.getContentType());
        
        setRequestHeaders(conn);
        
        conn.setInstanceFollowRedirects(followRedirects);
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        
        OutputStream ostream = conn.getOutputStream();
        try {
            ostream.write(input.getBytes());
        } finally {
            ostream.flush();
            ostream.close();
        }
        
        log.debug("POST - send done: " + remoteURL.toString());
    }
    
    private void doPost(HttpURLConnection conn, Map<String,List<Object>> params, Map<String,Object> uploads)
        throws AccessControlException, NotAuthenticatedException,
            ByteLimitExceededException, ExpectationFailedException, 
            IllegalArgumentException, PreconditionFailedException, 
            ResourceAlreadyExistsException, ResourceNotFoundException, 
            TransientException, IOException, InterruptedException {
        
        String ctype = "application/x-www-form-urlencoded";
        boolean multi = false;
        
        if (!uploads.isEmpty()) {
            ctype = "multipart/form-data; boundary=" + MULTIPART_BOUNDARY;
            multi = true;
        }
        
        if (ctype != null) {
            conn.setRequestProperty("Content-Type", ctype);
        }
        log.debug("POST Content-Type: " + ctype);
        
        setRequestHeaders(conn);
        
        conn.setInstanceFollowRedirects(followRedirects);
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String,List<Object>> pe : params.entrySet()) {
            for (Object v : pe.getValue()) {
                if (multi) {
                    sb.append(LINE_FEED).append("--" + MULTIPART_BOUNDARY);
                    sb.append(LINE_FEED).append("Content-Disposition: form-data; name=\"" + pe.getKey() + "\"");
                    sb.append(LINE_FEED);
                    sb.append(LINE_FEED).append(v.toString());
                } else {
                    sb.append(pe.getKey());
                    sb.append("=");
                    sb.append(URLEncoder.encode(v.toString(), "UTF-8"));
                    sb.append("&");
                }
            }
        }
        
        log.debug("params: " + sb.toString());
        
        Charset utf8 = Charset.forName("UTF-8");
        OutputStream writer = conn.getOutputStream();
        try {
            writer.write(sb.toString().getBytes(utf8));

            if (multi) {
                for (Map.Entry<String,Object> up : uploads.entrySet()) {
                    if (up.getValue() instanceof File) {
                        writeFilePart(up.getKey(), ((File)up.getValue()), writer, utf8);
                    } else if (up.getValue() instanceof FileContent) {
                        writeFilePart(up.getKey(), (FileContent)up.getValue(), writer, utf8);
                    } else {
                        throw new UnsupportedOperationException("Unexpected upload type: " + up.getClass().getName());
                    }
                }

                String end = LINE_FEED + "--" + MULTIPART_BOUNDARY + "--" + LINE_FEED;
                writer.write(end.getBytes(utf8));
            }
        } finally {
            writer.flush();
            writer.close();
        }

        log.debug("POST - send done: " + remoteURL.toString());
    }
    
    private void checkRedirects(URL url, HttpURLConnection conn)
        throws AccessControlException, NotAuthenticatedException,
            ByteLimitExceededException, ExpectationFailedException, 
            IllegalArgumentException, PreconditionFailedException, 
            ResourceAlreadyExistsException, ResourceNotFoundException, 
            TransientException, IOException, InterruptedException {
        
        super.checkErrors(url, conn);
        
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
 
    private void writeFilePart(String fieldName, File uploadFile, OutputStream w, Charset utf8)
        throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(LINE_FEED).append("--" + MULTIPART_BOUNDARY);
        sb.append(LINE_FEED).append("Content-Disposition: form-data; name=\"" + fieldName + "\";"
            + " filename=\"" + uploadFile.getName() + "\"");
        sb.append(LINE_FEED);
        sb.append(LINE_FEED);
        
        log.debug("MULTIPART PORTION: " + sb.toString());
        w.write(sb.toString().getBytes(utf8));
        
        FileInputStream r = null;
        long len = 0;
        try {
            r = new FileInputStream(uploadFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = r.read(buffer)) != -1) {
                w.write(buffer, 0, bytesRead);
                len += bytesRead;
            }
        } finally {
            if (r != null) {
                r.close();
            }
        }
        
        log.debug("file part length: " + len);
    }

    private void writeFilePart(String fieldName, FileContent uploadContent, OutputStream w, Charset utf8)
        throws IOException {
        StringBuilder sb = new StringBuilder();
        log.debug("writeFilePart: " + uploadContent.getContentType());
        sb.append(LINE_FEED).append("--" + MULTIPART_BOUNDARY);
        // 'filename' for data entry is needed so this data is treated as
        // stream input by the accepting web service
        sb.append(LINE_FEED).append("Content-Disposition: form-data; name=\"" + fieldName + "\";"
            + " filename=\"dummyFile\"");

        if (uploadContent.getContentType() != null) {
            sb.append(LINE_FEED).append("Content-Type: " + uploadContent.getContentType());
        }
        
        sb.append(LINE_FEED);
        sb.append(LINE_FEED);

        log.debug("MULTIPART PORTION: " + sb.toString());
        w.write(sb.toString().getBytes(utf8));
        w.write(uploadContent.getBytes());
        w.flush();
    }
}
