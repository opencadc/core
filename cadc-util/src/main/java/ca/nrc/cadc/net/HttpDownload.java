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
import ca.nrc.cadc.util.FileMetadata;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;
import javax.net.ssl.HttpsURLConnection;
import org.apache.log4j.Logger;

/**
 * Simple task to encapsulate a single download (GET). This class supports http and https
 * (SSL) with optional client certificate authentication using a SSLSocketFactory created one from
 * the current Subject. This class also supports retrying downloads if the server responds
 * with a 503 and a valid Retry-After header, where valid means an integer (number of seconds)
 * that is between 0 and HttpTransfer.MAX_RETRY_DELAY.
 * 
 * <p>Note: Redirects are followed by default.
 *
 * @author pdowler
 */
public class HttpDownload extends HttpTransfer {

    private static Logger log = Logger.getLogger(HttpDownload.class);

    private static int NONE = 0;
    private static int GZIP = 1;
    private static int ZIP = 2;

    private boolean headOnly = false;
    private String logAction = "HTTP GET";
    private boolean decompress = false;
    private boolean overwrite = false;

    private File destDir = null;
    private File origFile;
    private File decompFile;
    private File removeFile;
    private int decompressor;

    private OutputStream destStream;

    private String serverFilename;
    private File destFile;
    private InputStreamWrapper wrapper;

    //private boolean skipped = false;
    private long decompSize = -1;
    private long size = -1;

    private OverwriteChooser overwriteChooser;

    /**
     * Constructor with default user-agent string.
     *
     * @see HttpDownload (String, URL, File)
     * @param src URL to read
     * @param dest file or directory to write to
     */
    public HttpDownload(URL src, File dest) {
        this(null, src, dest);
    }

    /**
     * @see HttpDownload (String, URL, OutputStream)
     * @param src URL to read
     * @param dest output stream to write to
     * @deprecated use HttpGet
     */
    @Deprecated
    public HttpDownload(URL src, OutputStream dest) {
        this(null, src, dest);
    }

    /**
     * @param src
     * @param dest
     * @deprecated use HttpGet
     */
    @Deprecated
    public HttpDownload(URL src, InputStreamWrapper dest) {
        this(null, src, dest);
    }

    /**
     * Constructor. If the user agent string is not supplied, a default value will be generated.
     * <p>
     * The src URL cannot be null. If the protocol is https, this class will get the current Subject from
     * the AccessControlContext and use the Certificate(s) and PrivateKey(s) found there to set up an
     * SSLSocketFactory.
     * </p>
     * <p>
     * The dest File cannot be null. If dest is a directory, the downloaded
     * file will be saved in that directory and the filename will be determined from the HTTP headers or
     * URL. If dest is an existing file or it does not exist but it's parent is a directory, dest will
     * be used directly.
     * </p>
     *
     * @param userAgent user-agent string to report in HTTP headers
     * @param url URL to read
     * @param dest file or directory to write to
     */
    public HttpDownload(String userAgent, URL url, File dest) {
        super(url, true);
        setUserAgent(userAgent);

        if (url == null) {
            throw new IllegalArgumentException("source URL cannot be null");
        }

        if (dest == null) {
            throw new IllegalArgumentException("destination File cannot be null");
        }

        if (dest.exists() && dest.isDirectory()) {
            this.destDir = dest;
        } else {
            File parent = dest.getParentFile();
            if (parent == null) {
                // relative path
                throw new IllegalArgumentException("destination File cannot be relative");
            }
            //if (parent.exists() && parent.isDirectory())
            //{
            this.destDir = parent;
            this.localFile = dest;
            //}
            //else
            //    throw new IllegalArgumentException("destination File parent must be a directory that exists");
        }

        // dest does not exist == dest is the file to write
        // dest exists and is a file = dest is the file to (over)write
        // dest exists and it a directory == dest is the parent, we determine filename
        // all other path components are directories, if the do not exist we create them
    }

    /**
     * Constructor. If the user agent string is not supplied, a default value will be generated.
     *
     * <p>
     * The src URL cannot be null. If the protocol is https, this class will get the current Subject from
     * the AccessControlContext and use the Certificate(s) and PrivateKey(s) found there to set up an
     * SSLSocketFactory.
     * </p>
     *
     * <p>
     * The dest output stream cannot be null.
     * </p>
     *
     * @param userAgent user-agent string to report in HTTP headers
     * @param url URL to read
     * @param dest output stream to write to
     * @deprecated use HttpGet
     */
    @Deprecated
    public HttpDownload(String userAgent, URL url, OutputStream dest) {
        super(url, true);
        setUserAgent(userAgent);
        if (dest == null) {
            throw new IllegalArgumentException("destination stream cannot be null");
        }
        this.destStream = dest;
    }

    /**
     * 
     * @param userAgent
     * @param url
     * @param dest
     * @deprecated use HttpGet
     */
    @Deprecated
    public HttpDownload(String userAgent, URL url, InputStreamWrapper dest) {
        super(url, true);
        setUserAgent(userAgent);
        if (dest == null) {
            throw new IllegalArgumentException("destination wrapper cannot be null");
        }
        this.wrapper = dest;
    }

    @Override
    public String toString() {
        return "HttpDownload[" + remoteURL + "," + localFile + "]";
    }

    /**
     * Set mode so only an HTTP HEAD will be performed. After the download is run(),
     * the http header parameters from the response can be checked via various
     * get methods.
     *
     * @param headOnly
     */
    public void setHeadOnly(boolean headOnly) {
        this.headOnly = headOnly;
        if (headOnly) {
            this.logAction = "HTTP HEAD";
        }
    }

    /**
     * Enable optional decompression of the data after download. GZIP and ZIP are supported.
     *
     * @param decompress
     */
    public void setDecompress(boolean decompress) {
        this.decompress = decompress;
    }

    public void setOverwriteChooser(OverwriteChooser overwriteChooser) {
        this.overwriteChooser = overwriteChooser;
    }
    
    /**
     * Enable forced overwrite of existing destination file.
     *
     * @param overwrite
     */
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    /**
     * Get the size of the result file. This may be smaller than the content-length if the
     * file is being decompressed.
     *
     * @return the size in bytes, or -1 of unknown
     */
    public long getSize() {
        return size;
    }

    public String getFilename() {
        return serverFilename;
    }

    /**
     * Get a reference to the result file. In some cases this is null until the
     * download is complete.
     *
     * @return reference to the output file or null if download failed
     */
    public File getFile() {
        return destFile;
    }

    @Override
    public void prepare()
            throws AccessControlException, NotAuthenticatedException,
            ByteLimitExceededException, ExpectationFailedException,
            IllegalArgumentException, PreconditionFailedException,
            ResourceAlreadyExistsException, ResourceNotFoundException,
            TransientException, IOException, InterruptedException {

        // not feasible to separate all the setup and defer the read 
        // to run()
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void run() {
        try {
            doActionWithRetryLoop();
        } catch (Throwable t) {
            this.failure = t;
        } finally {
            responseStream = null;
        }
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
        URL currentURL = remoteURL;
        try {
            // store the thread so that other threads (typically the
            // Swing event thread) can terminate the Download
            this.thread = Thread.currentThread();

            fireEvent(TransferEvent.CONNECTING);

            boolean done = false;

            List<URL> visitedURLs = new ArrayList<URL>();
            while (!done) {
                done = true;
                doGet(currentURL);

                if (followRedirects && redirectURL != null) {
                    if (visitedURLs.contains(redirectURL)) {
                        throw new IllegalArgumentException("redirect back to a previously visited URL: " + redirectURL);
                    }

                    if (visitedURLs.size() > 6) {
                        throw new IllegalArgumentException("redirect exceeded hard-coded limit (6): " + redirectURL);
                    }

                    visitedURLs.add(currentURL);
                    currentURL = redirectURL;
                    redirectURL = null;
                    done = false;
                }
            }

            if (decompress && decompressor != NONE) {
                fireEvent(decompFile, TransferEvent.DECOMPRESSING);
                doDecompress();
            }
        } finally {
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

            if (failure == null && removeFile != null) {
                // only remove if download was successful
                log.debug("removing: " + removeFile);
                fireEvent(removeFile, TransferEvent.DELETED);
                removeFile.delete();
            }

            if (!go) {
                log.debug("cancelled");
                fireEvent(TransferEvent.CANCELLED);
            } else if (failure != null) {
                log.debug("failed: " + failure);
                fireEvent(failure);
            } else if (!throwTE) {
                log.debug("completed");
                FileMetadata meta = new FileMetadata();
                meta.setContentType(getContentType());
                meta.setContentEncoding(getContentEncoding());
                meta.setContentLength(getContentLength());
                meta.setMd5Sum(getContentMD5());
                meta.setLastModified(getLastModified());
                fireEvent(destFile, TransferEvent.COMPLETED, meta);
            }
        }
    }

    protected boolean askOverwrite(File f, Long length, Date lastModified) {
        // chooser API mismatch
        Long lastMod = null;
        if (lastModified != null) {
            lastMod = lastModified.getTime();
        }
        return overwrite
                || (overwriteChooser != null
                && overwriteChooser.overwriteFile(f.getAbsolutePath(), f.length(), f.lastModified(), length, lastMod));
    }

    // determine which file to read and write, enable optional decompression
    private boolean doCheckDestination()
            throws InterruptedException {
        // check/clear interrupted flag and throw if necessary
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        boolean doDownload = true;
        if (origFile.exists()) {
            log.debug(origFile + " exists");
            if (askOverwrite(origFile, getContentLength(), getLastModified())) {
                log.debug("overwrite: YES -- " + origFile);
                origFile.delete();
                if (decompFile != null && decompFile.exists()) {
                    decompFile.delete();
                }

                if (decompress && decompressor != NONE) {
                    this.destFile = decompFile; // download and decompress
                } else {
                    this.destFile = origFile; // download
                }
            } else {
                log.debug("overwrite: NO -- " + origFile);
                if (decompress && decompressor != NONE) {
                    decompFile.delete();
                    doDownload = false;
                    this.destFile = decompFile; // decomp only
                    this.removeFile = origFile; // remove after decompress
                } else {
                    doDownload = false;
                    //this.skipped = true;
                }
            }
        } else if (decompFile != null && decompFile.exists()) {
            log.debug(decompFile + " exists");
            if (askOverwrite(decompFile, decompSize, getLastModified())) {
                log.debug("overwrite: YES -- " + decompFile);
                //decompFile.delete(); // origFile does not exist
                this.removeFile = decompFile;
                if (decompress && decompressor != NONE) {
                    this.destFile = decompFile;    // download and decompress
                } else {
                    this.destFile = origFile;      // download
                }
            } else {
                log.debug("overwrite: NO -- " + decompFile);
                this.destFile = decompFile;
                this.removeFile = null;
                doDownload = false;
                //this.skipped = true;
            }
        } else if (decompress && decompressor != NONE && decompFile != null) {
            this.destFile = decompFile;
        } else {
            this.destFile = origFile;
        }
        log.debug("destination file: " + destFile);
        this.localFile = destFile;
        return doDownload;
    }

    // called from doHead and doGet to capture HTTP standard header values
    private void processHeader(HttpURLConnection conn)
            throws IOException, InterruptedException {

        // custom CADC header
        String ucl = conn.getHeaderField("X-Uncompressed-Length");
        if (ucl != null) {
            try {
                this.decompSize = Long.parseLong(ucl);
            } catch (NumberFormatException ignore) {
                // do nothing
            }
        }

        this.serverFilename = getServerFilename(conn);

        if (destStream == null && wrapper == null) {
            // download to file: extra metadata
            String origFilename = null;

            // first option: use what the caller suggested
            if (localFile != destDir) {
                this.origFile = localFile;
            }

            if (origFile == null) {
                this.origFile = new File(destDir, serverFilename);
            }
            origFilename = origFile.getName();

            // encoding mucks with filename
            if ("gzip".equals(getContentEncoding()) || origFilename.endsWith(".gz")) {
                if (origFilename.endsWith(".gz")) {
                    this.decompFile = new File(destDir, origFilename.substring(0, origFilename.length() - 3));
                } else {
                    this.decompFile = origFile;
                    this.origFile = new File(destDir, origFilename + ".gz");
                }

                this.decompressor = GZIP;
            } else if ("zip".equals(getContentEncoding()) || origFilename.endsWith(".zip")) {
                if (origFilename.endsWith(".zip")) {
                    this.decompFile = new File(destDir, origFilename.substring(0, origFilename.length() - 4));
                } else {
                    this.decompFile = origFile;
                    this.origFile = new File(destDir, origFilename + ".zip");
                }

                this.decompressor = ZIP;
            }
        }

        log.debug("   original file: " + origFile);
        log.debug("     decomp file: " + decompFile);
        log.debug("  content length: " + getContentLength());
        log.debug("     content md5: " + getContentMD5());
        log.debug("    content type: " + getContentType());
        log.debug("content encoding: " + getContentEncoding());
        log.debug("     decomp size: " + decompSize);
        log.debug("    decompressor: " + decompressor);
        log.debug("    lastModified: " + getLastModified());
    }

    private String getServerFilename(HttpURLConnection conn) {
        String ret = null;

        // first option: use supplied filename if present in http header
        String cdisp = conn.getHeaderField("Content-Disposition");
        log.debug("HTTP HEAD: Content-Disposition = " + cdisp);
        if (cdisp != null) {
            ret = parseContentDisposition(cdisp);
        }

        // alternative: pull something from the end of the URL
        if (ret == null) {
            String s = remoteURL.getPath();
            String query = remoteURL.getQuery();
            int i = s.lastIndexOf('/');
            if (i != -1 && i < s.length() - 1) {
                ret = s.substring(i + 1, s.length());
            }

            if (query != null) {
                ret += "?" + query;
            }
        }

        // last resort for no path: use hostname
        if (ret == null) {
            ret = remoteURL.getHost();
        }

        return ret;
    }

    private void doGet(URL url)
            throws AccessControlException, NotAuthenticatedException,
            ByteLimitExceededException, ExpectationFailedException, IllegalArgumentException,
            PreconditionFailedException, ResourceAlreadyExistsException, ResourceNotFoundException,
            TransientException, IOException, InterruptedException {
        // check/clear interrupted flag and throw if necessary
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        InputStream istream = null;
        OutputStream ostream = null;
        try {
            // open connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            log.debug("HttpURLConnection type: " + conn.getClass().getName() + " for GET " + url);
            if (headOnly) {
                conn.setRequestMethod("HEAD");
            } else {
                conn.setRequestMethod("GET");
            }

            setRequestAuthHeaders(conn);
            if (conn instanceof HttpsURLConnection) {
                HttpsURLConnection sslConn = (HttpsURLConnection) conn;
                initHTTPS(sslConn);
            }
            conn.setInstanceFollowRedirects(followRedirects);
            conn.setRequestProperty("Accept", "*/*");

            setRequestHeaders(conn);

            requestStartTime = System.currentTimeMillis();

            checkErrors(url, conn);
            checkRedirects(url, conn);
            processHeader(conn);

            if (headOnly || (!followRedirects && redirectURL != null)) {
                return;
            }

            // evaulate overwrite of complete file
            boolean doDownload = true;
            if (destStream == null && wrapper == null) {
                doDownload = doCheckDestination();
            }

            // go=false means cancelled, doDownload==false means skipped
            go = go && doDownload;
            if (!go) {
                return;
            }

            // evaluate possible resume?
            File tmp = origFile;

            this.size = getContentLength();
            String pkey = null;
            String pvalue = null;
            boolean append = false;
            long startingPos = 0;
            if (destStream == null && wrapper == null) {
                // downloading to file
                // temporary destination
                origFile = new File(origFile.getAbsolutePath() + ".part");
                if (origFile.exists() && origFile.length() < getContentLength()) {
                    // partial file from previous download
                    pkey = "Range";
                    pvalue = "bytes=" + origFile.length() + "-"; // open ended
                }
            }

            if (pkey != null) {
                // open 2nd connection with a range request
                HttpURLConnection rconn = (HttpURLConnection) url.openConnection();
                log.debug("HttpURLConnection type: " + conn.getClass().getName() + " for GET " + url);
                rconn.setRequestMethod("GET");
                setRequestAuthHeaders(rconn);
                if (rconn instanceof HttpsURLConnection) {
                    HttpsURLConnection sslConn = (HttpsURLConnection) rconn;
                    initHTTPS(sslConn);
                }

                rconn.setInstanceFollowRedirects(true);
                rconn.setRequestProperty("Accept", "*/*");
                setRequestHeaders(rconn);
                log.debug("trying: " + pkey + " = " + pvalue);
                rconn.setRequestProperty(pkey, pvalue);

                int rcode = rconn.getResponseCode();
                log.debug(logAction + " status: " + rcode + " for range request to " + url);
                if (rcode == HttpURLConnection.HTTP_PARTIAL) {
                    String cr = conn.getHeaderField("Content-Range");
                    log.debug("Content-Range = " + cr);
                    if (cr != null) {
                        cr = cr.trim();
                        if (cr.startsWith("bytes")) {
                            cr = cr.substring(6);
                            String[] parts = cr.split("-");
                            startingPos = Long.parseLong(parts[0]);
                            log.debug("found startingPos = " + startingPos);
                            String[] ss = cr.split("/");
                            this.size = Long.parseLong(ss[1]);
                            log.debug("found real size = " + size);
                            append = true;
                        }
                    }
                    if (append) {
                        try {
                            log.debug("can resume: closing first connection");
                            conn.disconnect();
                        } catch (Exception ignore) {
                            // do nothing
                        }
                        conn = rconn; // use the second connection with partial
                        checkErrors(url, conn); // recapture responseCode, headers, etc
                    } else { // proceed with original connection
                        try {
                            log.debug("cannot resume: closing second connection");
                            rconn.disconnect();
                        } catch (Exception ignore) {
                            // do nothing
                        }
                    }
                }
            }

            fireEvent(TransferEvent.CONNECTED);

            // check eventID hook
            findEventID(conn);

            fireEvent(origFile, TransferEvent.TRANSFERING);

            istream = conn.getInputStream();
            if (!(istream instanceof BufferedInputStream)) {
                log.debug("using BufferedInputStream");
                istream = new BufferedInputStream(istream, bufferSize);
            }

            if (this.destStream != null) {
                log.debug("output: supplied OutputStream");
                ostream = destStream;
                log.debug("using BufferedOutputStream");
                ostream = new BufferedOutputStream(ostream, bufferSize);
            } else if (wrapper == null) {
                // prepare to write to origFile
                File parent = origFile.getParentFile();
                parent.mkdirs();
                if (!parent.exists()) {
                    throw new IOException("failed to create one or more parent dir(s):" + parent);
                }

                log.debug("output: " + origFile + " append: " + append);
                ostream = new FileOutputStream(origFile, append);
                log.debug("using BufferedOutputStream");
                ostream = new BufferedOutputStream(ostream, bufferSize);
            }

            // TODO: use super.readResponse
            if (wrapper != null) {
                wrapper.read(istream);
            } else if (userNio) {
                nioLoop(istream, ostream, 2 * bufferSize, startingPos);
            } else {
                String md5 = ioLoop(istream, ostream, 2 * bufferSize, startingPos);
                if (getContentMD5() != null && md5 != null) {
                    if (!md5.equals(getContentMD5())) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("MD5 mismatch: ");
                        sb.append(getContentMD5()).append(" (header) != ").append(md5).append(" (bytes)");
                        if (url != null) {
                            sb.append(" url: ").append(url);
                        }

                        if (destFile != null) {
                            sb.append(" destFile: ").append(destFile.getAbsolutePath());
                        }

                        throw new IncorrectContentChecksumException(sb.toString());
                    }
                }
            }

            if (ostream != null) {
                long writeStart = System.currentTimeMillis();
                ostream.flush();
                if (logIO) {
                    long flushTime = System.currentTimeMillis() - writeStart;
                    writeTime += flushTime;
                    log.debug("Time (ms) to flush: " + flushTime);
                }
            }

            log.debug("download completed");
            if (destStream == null && wrapper == null) {
                // downloading to file
                log.debug("renaming " + origFile + " to " + tmp);
                origFile.renameTo(tmp);
                origFile = tmp;
                destFile = tmp;
            }
        } finally {
            if (istream != null) {
                log.debug("closing InputStream");
                try {
                    istream.close();
                } catch (Exception ignore) {
                    // do nothing
                }
            }

            if (ostream != null) {
                log.debug("closing OutputStream");
                try {
                    ostream.close();
                } catch (Exception ignore) {
                    // do nothing
                }
            }
        }
    }

    private void doDecompress()
            throws IOException, InterruptedException {
        // check/clear interrupted flag and throw if necessary
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        InputStream istream = null;
        OutputStream ostream = null;
        //RandomAccessFile ostream = null;
        try {
            this.size = decompSize;
            int sz = bufferSize;
            if (decompressor == GZIP) {
                log.debug("input: GZIPInputStream(BufferedInputStream(FileInputStream)");
                istream = new GZIPInputStream(new FileInputStream(origFile), sz);
            } else if (decompressor == ZIP) {
                log.debug("input: ZIPInputStream(BufferedInputStream(FileInputStream)");
                istream = new ZipInputStream(new BufferedInputStream(new FileInputStream(origFile)));
            }

            log.debug("output: " + decompFile);
            ostream = new BufferedOutputStream(new FileOutputStream(decompFile), sz);

            this.removeFile = origFile;

            if (userNio) {
                nioLoop(istream, ostream, sz, 0);
            } else {
                ioLoop(istream, ostream, sz, 0);
            }

            ostream.flush();

            this.destFile = decompFile; // ?? 
        } finally {
            if (istream != null) {
                try {
                    istream.close();
                } catch (Exception ignore) {
                    // do nothing
                }
            }

            if (ostream != null) {
                try {
                    ostream.close();
                } catch (Exception ignore) {
                    // do nothing
                }
            }
        }
    }

    private static char SINGLE_QUOTE = "'".charAt(0);
    private static char DOUBLE_QUOTE = "\"".charAt(0);

    private static boolean isFilenameDisposition(String cdisp) {
        if (cdisp == null) {
            return false;
        }

        cdisp = cdisp.toLowerCase(); // just for checking
        // HACK: HTTP/1.1 allows attachment or extension token, but some sites use inline anyway
        return (cdisp.startsWith("attachment") || cdisp.startsWith("inline"));
    }

    /**
     * Parse a Content-Disposition header value and extract the filename.
     *
     * @param cdisp value of the Content-Disposition header
     * @return a filename, or null
     */
    public static String parseContentDisposition(String cdisp) {
        if (!isFilenameDisposition(cdisp)) {
            return null;
        }

        // TODO: should split on ; and check each part for filename=something
        // extra filename from cdisp value
        String[] parts = cdisp.split(";");
        for (int p = 0; p < parts.length; p++) {
            String part = parts[p].trim();
            // check/remove double quotes
            if (part.charAt(0) == '"') {
                part = part.substring(1, part.length());
            }

            if (part.charAt(part.length() - 1) == '"') {
                part = part.substring(0, part.length() - 1);
            }

            if (part.startsWith("filename")) {
                int i = part.indexOf('=');
                String filename = part.substring(i + 1, part.length());

                // strip off optional quotes
                char c1 = filename.charAt(0);
                char c2 = filename.charAt(filename.length() - 1);
                boolean rs = (c1 == SINGLE_QUOTE || c1 == DOUBLE_QUOTE);
                boolean re = (c2 == SINGLE_QUOTE || c2 == DOUBLE_QUOTE);
                if (rs && re) {
                    filename = filename.substring(1, filename.length() - 1);
                } else if (rs) {
                    filename = filename.substring(1, filename.length());
                } else if (re) {
                    filename = filename.substring(0, filename.length() - 1);
                }

                // strip off optional path information
                i = filename.lastIndexOf('/'); // unix
                if (i >= 0) {
                    filename = filename.substring(i + 1);
                }

                i = filename.lastIndexOf('\\'); // windows
                if (i >= 0) {
                    filename = filename.substring(i + 1);
                }

                // TODO: check/sanitize for security issues  
                return filename;
            }
        }
        return null;
    }
}
