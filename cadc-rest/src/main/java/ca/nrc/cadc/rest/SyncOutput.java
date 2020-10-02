/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2018.                            (c) 2018.
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

package ca.nrc.cadc.rest;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * Wrapper around an application-server response.
 *
 * @author pdowler
 */
public class SyncOutput {

    private static final Logger log = Logger.getLogger(SyncOutput.class);

    private final HttpServletResponse response;
    private SafeOutputStream outputStream;

    public SyncOutput(HttpServletResponse response) {
        this.response = response;
        this.outputStream = null;
    }

    /**
     * Check is the output stream is open. If true, the header has been committed and additional
     * calls to setHeader will be ignored.
     *
     * @return true if response header committed and output stream has been opened
     */
    public boolean isOpen() {
        log.debug("OutputStream open: " + (this.outputStream != null && this.outputStream.isOpen()));
        return (this.outputStream != null && this.outputStream.isOpen());
    }

    /**
     * Compatibility with cadc-uws-server API that is in use.
     *
     * @param code HTTP status code
     * @deprecated
     */
    @Deprecated
    public void setResponseCode(int code) {
        setCode(code);
    }

    /**
     * Set HTTP response code.
     *
     * @param code HTTP response code
     */
    public void setCode(int code) {
        if (isOpen()) {
            IllegalStateException e = new IllegalStateException();
            log.warn("OutputStream already open, not setting response code to: " + code, e);
            return;
        }
        this.response.setStatus(code);
    }

    /**
     * Set HTTP header.
     *
     * @param key HTTP header name
     * @param value HTTP header value
     */
    public void setHeader(String key, Object value) {
        if (isOpen()) {
            IllegalStateException e = new IllegalStateException();
            log.warn("OutputStream already open, not setting header: " + key + " to: " + value, e);
            return;
        }

        if (value == null) {
            this.response.setHeader(key, null);
        } else {
            this.response.setHeader(key, value.toString());
        }
    }

    /**
     * Get the output stream.
     *  ** Calling this method no longer commits the request. **
     *
     * @return the output stream for writing the response
     * @throws IOException fail to open output stream
     */
    public OutputStream getOutputStream()
            throws IOException {
        if (this.outputStream == null) {
            log.debug("getOutputStream called");
            this.outputStream = new SafeOutputStream();
        }
        return this.outputStream;
    }

    /**
     * OutputStream that does not allow the stream to be closed,
     * and does not open the OutputStream (committing the response)
     * until a write method is called.
     */
    private class SafeOutputStream extends FilterOutputStream {

        SafeOutputStream() {
            super(null);
        }

        @Override
        public void close()
                throws IOException {
            // cannot close service output streams
            log.debug("close()");
        }

        @Override
        public void flush()
            throws IOException {
            if (this.out == null) {
                log.debug("flush - first open of OutputStream");
                this.out = response.getOutputStream();
            }
            super.flush();
        }

        @Override
        public void write(int b)
            throws IOException {
            if (this.out == null) {
                log.debug("write(int b) - first open of OutputStream");
                this.out = response.getOutputStream();
            }
            super.write(b);
        }

        @Override
        public void write(byte[] b)
            throws IOException {
            if (this.out == null) {
                log.debug("write(byte[] b) - first open of OutputStream");
                this.out = response.getOutputStream();
            }
            super.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len)
            throws IOException {
            if (this.out == null) {
                log.debug("write(byte[] b, int off, int len) - first open of OutputStream");
                this.out = response.getOutputStream();
            }
            super.write(b, off, len);
        }

        boolean isOpen() {
            return (this.out != null);
        }

    }
}
