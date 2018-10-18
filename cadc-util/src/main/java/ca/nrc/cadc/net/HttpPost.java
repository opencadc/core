/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2016.                            (c) 2016.
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

import ca.nrc.cadc.net.event.TransferEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

import ca.nrc.cadc.util.StringUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Perform an HTTP Post.
 * 
 * Post data can be supplied as either parameters in a map or as a single
 * string.  For Posts that may result in large response data, the constructor
 * with an output stream should be used.
 * 
 * @author majorb, pdowler
 *
 */
public class HttpPost extends HttpTransfer
{
    private static Logger log = Logger.getLogger(HttpPost.class);

    // a string that isn't going to be inside params or files
    private static final String MULTIPART_BOUNDARY = UUID.randomUUID().toString();
    private static final String LINE_FEED = "\r\n";
    
    // request information
    private Map<String,Object> paramMap;
    private String content;
    private String contentType;
    private OutputStream outputStream;

    // result information
    private String responseContentType;
    private String responseContentEncoding;
    private String responseBody;
    
    /**
     * HttpPost contructor.  Redirects will be followed.
     * Ideal for large expected responses.
     * 
     * @param url The POST destination.
     * @param map A map of the data to be posted.
     * @param outputStream An output stream to capture the response data.
     */
    public HttpPost(URL url, Map<String, Object> map, OutputStream outputStream)
    {
        super(true);
        this.remoteURL = url;
        this.paramMap = map;
        this.outputStream = outputStream;
        this.followRedirects = true;
        if (url == null)
            throw new IllegalArgumentException("url cannot be null.");
        if (map == null || map.size() == 0)
            throw new IllegalArgumentException("parameters cannot be empty.");
        
    }
    
    /**
     * HttpPost contructor.
     * 
     * @param url The POST destination.
     * @param map A map of the data to be posted.
     * @param followRedirects Whether or not to follow server redirects.
     */
    public HttpPost(URL url, Map<String, Object> map, boolean followRedirects)
    {
        super(followRedirects);
        this.remoteURL = url;
        this.paramMap = map;
        if (url == null)
            throw new IllegalArgumentException("url cannot be null.");
        if (map == null || map.isEmpty())
            throw new IllegalArgumentException("parameters cannot be empty.");
    }
    
    /**
     * HttpPost constructor.
     * 
     * @param url The POST destination
     * @param content The content to post.
     * @param contentType The type of the content.
     * @param followRedirects Whether or not to follow server redirects.
     */
    public HttpPost(URL url, String content, String contentType, boolean followRedirects)
    {
        super(followRedirects);
        this.remoteURL = url;
        this.content = content;
        this.contentType = contentType;
        if (url == null)
            throw new IllegalArgumentException("dest cannot be null.");
        if (!StringUtil.hasText(content))
            throw new IllegalArgumentException("cannot have empty content.");
    }

    @Override
    public String toString() { return "HttpPost[" + remoteURL + "]"; }
    
    public String getResponseContentEncoding()
    {
        return responseContentEncoding;
    }

    public String getResponseContentType()
    {
        return responseContentType;
    }

    /**
     * If an OutputStream wasn't supplied in the HttpPost constructor,
     * the response can be retrieved here.
     * @return
     */
    public String getResponseBody()
    {
        return responseBody;
    }

    /**
     * Retry on TransientExceptions
     */
    public void run()
    {
        boolean done = false;
        while (!done)
        {
            try
            {
                runX();
                done = true;
            }
            catch(TransientException ex)
            {
                try
                {
                    long dt = 1000L * ex.getRetryDelay();
                    log.debug("retry " + numRetries + " sleeping  for " + dt);
                    fireEvent(TransferEvent.RETRYING);
                    Thread.sleep(dt);
                }
                catch(InterruptedException iex)
                {
                    log.debug("retry interrupted");
                    done = true;
                }
            }
        }
    }

    private void runX()
        throws TransientException
    {
        log.debug(this.toString());

        try
        {
            this.thread = Thread.currentThread();
            HttpURLConnection conn = (HttpURLConnection) this.remoteURL.openConnection();

            if (conn instanceof HttpsURLConnection)
            {
                HttpsURLConnection sslConn = (HttpsURLConnection) conn;
                initHTTPS(sslConn);
            }
            if (content != null)
                doPost(conn, contentType, content);
            else
                doPost(conn, paramMap);

        }
        catch(TransientException tex)
        {
            log.debug("caught: " + tex);
            throw tex;
        }
        catch(Throwable t)
        {
            log.debug("caught: " + t, t);
            failure = t;
        }
        finally
        {
            if (outputStream != null)
            {
                log.debug("closing OutputStream");
                try { outputStream.close(); }
                catch(Exception ignore) { }
            }

            synchronized(this) // vs sync block in terminate()
            {
                if (thread != null)
                {
                    // clear interrupt status
                    if ( Thread.interrupted() )
                        go = false;
                    this.thread = null;
                }
            }
            
            if (failure != null)
            {
                log.debug("failed: " + failure);
            }
        }
    }
    
    private void doPost(HttpURLConnection conn, Map<String, Object> parameters)
        throws IOException, InterruptedException, TransientException
    {
        if (parameters == null)
            parameters = new TreeMap<String,Object>(); // removes a lot of checking for null
        
        Set<String> keys = parameters.keySet();
        Object value = null;

        Map<String,List<Object>> pmap = new TreeMap<String,List<Object>>();
        Map<String,Object> uploads = new TreeMap<String,Object>();
        for (Map.Entry<String,Object> me : parameters.entrySet())
        {
            String key = me.getKey();
            value = me.getValue();
            if (value instanceof File)
            {
                File u = (File) value;
                uploads.put(key, u);
            }
            else if (value instanceof FileContent)
            {
                FileContent fc = (FileContent) value;
                uploads.put(key, fc);
            }
            else if (value instanceof Collection)
            {
                Collection vals = (Collection) value;
                List<Object> pmv = new ArrayList<Object>(vals.size());
                pmv.addAll(vals);
                pmap.put(key, pmv);
            }
            else
            {
                List<Object> pmv = new ArrayList<Object>(1);
                pmv.add(value);
                pmap.put(key, pmv);
            }
        }
        
        doPost(conn, pmap, uploads);
    }
    
    private void doPost(HttpURLConnection conn, String contentType, String content)
        throws IOException, InterruptedException, TransientException
    {
        setRequestSSOCookie(conn);
        conn.setRequestMethod("POST");
        if (content != null)
        {
            String len = Long.toString(content.getBytes("UTF-8").length);
            conn.setRequestProperty("Content-Length", len);
            log.debug("POST Content-Length: " + len);
        }
        if (contentType != null)
            conn.setRequestProperty("Content-Type", contentType);
        log.debug("POST Content-Type: " + contentType);
        
        setRequestHeaders(conn);
        
        conn.setInstanceFollowRedirects(followRedirects);
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        
        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
        writer.write(content);
        writer.flush();
        writer.close();
        
        log.debug("POST - done: " + remoteURL.toString());
        
        handleResponse(conn);
    }
    
    private void doPost(HttpURLConnection conn, Map<String,List<Object>> params, Map<String,Object> uploads)
        throws IOException, InterruptedException, TransientException
    {
        
        String ctype = "application/x-www-form-urlencoded";
        boolean multi = false;
        
        if (!uploads.isEmpty())
        {
            ctype = "multipart/form-data; boundary=" + MULTIPART_BOUNDARY;
            multi = true;
        }
        
        setRequestSSOCookie(conn);
        conn.setRequestMethod("POST");
        
        if (ctype != null)
            conn.setRequestProperty("Content-Type", ctype);
        log.debug("POST Content-Type: " + ctype);
        
        setRequestHeaders(conn);
        
        conn.setInstanceFollowRedirects(followRedirects);
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String,List<Object>> pe : params.entrySet())
        {
            for (Object v : pe.getValue())
            {
                if (multi)
                {
                    sb.append(LINE_FEED).append("--" + MULTIPART_BOUNDARY);
                    sb.append(LINE_FEED).append("Content-Disposition: form-data; name=\"" + pe.getKey() + "\"");
                    sb.append(LINE_FEED);
                    sb.append(LINE_FEED).append(v.toString());
                }
                else
                {
                    sb.append(pe.getKey());
                    sb.append("=");
                    sb.append( URLEncoder.encode(v.toString(), "UTF-8") );
                    sb.append("&");
                    
                }
            }
        }
        
        log.debug("params: " + sb.toString());
        
        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
        writer.write(sb.toString());
        
        if (multi)
        {
            for (Map.Entry<String,Object> up : uploads.entrySet())
            {
                if (up.getValue() instanceof File) {
                    writeFilePart(up.getKey(), ((File)up.getValue()), writer);
                }
                else if (up.getValue() instanceof FileContent) {
                    writeFilePart(up.getKey(), (FileContent)up.getValue(), writer);
                }
                else {
                    throw new UnsupportedOperationException("Unexpected upload type: " + up.getClass().getName());
                }
            }

            String end = LINE_FEED + "--" + MULTIPART_BOUNDARY + "--" + LINE_FEED;
            writer.append(end);
        }
        
        writer.flush();
        writer.close();

        log.debug("POST - done: " + remoteURL.toString());
        
        handleResponse(conn);
    }
    
    private void handleResponse(HttpURLConnection conn)
        throws IOException, InterruptedException, TransientException
    {
        //int statusCode = checkStatusCode(conn);
        this.responseCode = conn.getResponseCode();
        this.responseContentType = conn.getContentType();
        this.responseContentEncoding = conn.getContentEncoding();
        log.debug("handleResponse: " + responseCode + "|" + responseContentType);
        
        // check for a redirect
        String location = conn.getHeaderField("Location");
        if ((responseCode == HttpURLConnection.HTTP_SEE_OTHER
            || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) 
            && location != null)
        {
            this.redirectURL = new URL(location);
            log.debug("redirectURL: " + redirectURL);
            return;
        }
        
        // map some response codes to exceptions
        checkStatusCode(conn);
        
        // read response fully
        InputStream istream = conn.getInputStream();
        readResponse(istream);
    }
    
    private void readResponse(InputStream istream)
        throws IOException, InterruptedException
    {
        if (outputStream != null)
        {
            if (use_nio)
                nioLoop(istream, outputStream, 2*bufferSize, 0);
            else
                ioLoop(istream, outputStream, 2*bufferSize, 0);
            outputStream.flush();
            log.debug("wrote response to supplied " + outputStream.getClass().getName());
        }
        else
        {
            int smallBufferSize = 512;
            ByteArrayOutputStream byteArrayOstream = new ByteArrayOutputStream();
            try
            {
                if (use_nio)
                    nioLoop(istream, byteArrayOstream, smallBufferSize, 0);
                else
                    ioLoop(istream, byteArrayOstream, smallBufferSize, 0);
                byteArrayOstream.flush();
                responseBody = new String(byteArrayOstream.toByteArray(), "UTF-8");
                log.debug("captured response in local String responseBody");
            }
            finally
            {
                if (byteArrayOstream != null)
                {
                    try { byteArrayOstream.close(); }
                    catch(Exception ignore) { }
                }
            }
        }
    }
    
    private void writeFilePart(String fieldName, File uploadFile, Writer w)
        throws IOException 
    {
        StringBuilder sb = new StringBuilder();
        sb.append(LINE_FEED).append("--" + MULTIPART_BOUNDARY);
        sb.append(LINE_FEED).append("Content-Disposition: form-data; name=\"" + fieldName + "\";"
            + " filename=\"" + uploadFile.getName() + "\"");
        sb.append(LINE_FEED);
        sb.append(LINE_FEED);
        
        log.debug("MULTIPART PORTION: " + sb.toString());
        w.append(sb);
        
        FileReader r = null;
        long len = 0;
        try
        {
            r = new FileReader(uploadFile);
            char[] buffer = new char[4096];
            int bytesRead;
            while ((bytesRead = r.read(buffer)) != -1) 
            {
                w.write(buffer, 0, bytesRead);
                len += bytesRead;
            }
        }
        finally
        {
            if (r != null)
                r.close();
        }
        log.debug("file part length: " + len);
    }

    private void writeFilePart(String fieldName, FileContent uploadContent, Writer w)
        throws IOException
    {
        StringBuilder sb = new StringBuilder();
        log.warn(uploadContent.getContentType());
        sb.append(LINE_FEED).append("--" + MULTIPART_BOUNDARY);
        // 'filename' for data entry is needed so this data is treated as
        // stream input by the accepting web service
        sb.append(LINE_FEED).append("Content-Disposition: form-data; name=\"" + fieldName + "\";"
            + " filename=\"dummyFile\"");

        sb.append(LINE_FEED).append("Content-Type: " + uploadContent.getContentType() + ";");
        sb.append(LINE_FEED);
        sb.append(LINE_FEED);

        log.debug("MULTIPART PORTION: " + sb.toString());
        w.append(sb);
        w.write(uploadContent.getContent());
    }
    
    private int checkStatusCode(HttpURLConnection conn)
        throws IOException, InterruptedException, TransientException
    {
        int code = conn.getResponseCode();
        log.debug("HTTP POST status: " + code + " for " + remoteURL);
        this.responseCode = code;
        
        if (code != HttpURLConnection.HTTP_OK &&
            code != HttpURLConnection.HTTP_MOVED_TEMP &&
            code != HttpURLConnection.HTTP_SEE_OTHER)
        {
            this.responseContentType = conn.getContentType();
            this.responseContentEncoding = conn.getContentEncoding();
            String msg = "(" + code + ") " + conn.getResponseMessage();
            InputStream istream = conn.getErrorStream();
            readResponse(istream);
            checkTransient(code, msg, conn);
            switch(code)
            {
                case HttpURLConnection.HTTP_NO_CONTENT:
                    break;
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    throw new AccessControlException("permission denied: " + msg);
                case HttpURLConnection.HTTP_FORBIDDEN:
                    throw new AccessControlException("permission denied: " + msg);
                case HttpURLConnection.HTTP_NOT_FOUND:
                    throw new FileNotFoundException("resource not found " + msg);
                default:
                    throw new IOException(msg);
            }
        }
        return code;
    }

}
