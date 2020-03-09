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
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
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
public class HttpGet extends HttpTransfer {

    private static Logger log = Logger.getLogger(HttpGet.class);

    private boolean headOnly = false;
    
    /**
     * Constructor with no output destination. This can be used with prepare() 
     * and getInputStream().
     * 
     * @param url
     * @param followRedirects 
     */
    public HttpGet(URL url, boolean followRedirects) {
        super(url, followRedirects);
    }
    
    /**
     * Constructor with destination stream.
     * 
     * @param url URL to read
     * @param dest output stream to write to
     */
    public HttpGet(URL url, OutputStream dest) {
        super(url, true);
        if (dest == null) {
            throw new IllegalArgumentException("destination File cannot be null");
        }
        this.responseDestination = dest;
    }

    /**
     * Constructor with input handler.
     * 
     * @param url to read
     * @param dest output wrapper to read stream
     */
    public HttpGet(URL url, InputStreamWrapper dest) {
        super(url, true);
        if (dest == null) {
            throw new IllegalArgumentException("destination wrapper cannot be null");
        }
        this.responseStreamWrapper = dest;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + remoteURL + ", head=" + headOnly + "]";
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
    public void run() {
        if (headOnly) {
            if (failure == null) {
                try {
                    if (responseStream == null) {
                        prepare();
                    }
                } catch (Throwable t) {
                    this.failure = t;
                }
            }
        } else {
            super.run();
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

        URL currentURL = remoteURL;

        fireEvent(TransferEvent.CONNECTING);

        boolean done = false;

        // keep trying to get until we don't get redirected
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
    }

    private void doGet(URL url)
            throws AccessControlException, NotAuthenticatedException,
            ByteLimitExceededException, ExpectationFailedException, IllegalArgumentException,
            PreconditionFailedException, ResourceAlreadyExistsException, ResourceNotFoundException,
            TransientException, IOException, InterruptedException {

        // open connection
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        log.debug("HttpURLConnection type: " + conn.getClass().getName() + " for GET " + url);
        if (headOnly) {
            conn.setRequestMethod("HEAD");
        } else {
            conn.setRequestMethod("GET");
        }

        setRequestSSOCookie(conn);
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
        if (redirectURL == null) {
            fireEvent(TransferEvent.CONNECTED);
            this.responseStream = conn.getInputStream();
        }
    }
}
