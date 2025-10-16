/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2025.                            (c) 2025.
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

import static ca.nrc.cadc.net.HttpConstants.HDR_ACCEPT_RANGES;
import static ca.nrc.cadc.net.HttpConstants.HDR_CONTENT_RANGE;
import static ca.nrc.cadc.net.HttpConstants.HDR_RANGE;
import static ca.nrc.cadc.net.HttpConstants.RANGE_BYTES;

import ca.nrc.cadc.io.RandomAccessSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * A RandomAccessSource implementation that uses HTTP range requests to provide
 * random access to a resource identified by a URL.
 * <p>
 * Note: The server hosting the URL must support HTTP range requests for this
 * class to function correctly.
 * </p>
 */
public class RandomAccessURL implements RandomAccessSource {
    private static final Logger log = Logger.getLogger(RandomAccessURL.class);
    private final URL url;
    private long position = 0;
    private final long contentLength;

    /**
     * Constructs a RandomAccessURL for the provided URL.
     * This constructor performs a HEAD request to verify that the provided URL supports HTTP range requests and fetches the content length.
     * If the server does not support range requests, an UnsupportedOperationException is thrown.
     *
     * @param url The URL to check.
     * @throws IOException                   If an I/O error occurs.
     * @throws ResourceNotFoundException     If the resource is not found.
     * @throws UnsupportedOperationException If range requests are not supported by the server.
     */
    public RandomAccessURL(URL url) throws IOException, ResourceNotFoundException, UnsupportedOperationException {
        log.debug("RandomAccessURL requested for URL: " + url);
        this.url = url;
        this.contentLength = fetchContentLength(url);
    }

    @Override
    public void seek(long position) throws IOException {
        if (position < 0 || position > contentLength) {
            throw new IOException("Seek position out of bounds");
        }
        this.position = position;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (position >= contentLength) {
            return -1;
        }

        int bytesToRead = (int) Math.min(length, Math.min(contentLength - position, buffer.length - offset));
        long rangeEnd = position + bytesToRead - 1;
        String range = RANGE_BYTES + "=" + position + "-" + rangeEnd;

        HttpGet get = new HttpGet(url, true);
        get.setRequestProperty(HDR_RANGE, range);
        get.setFollowRedirects(true);

        try {
            get.prepare();
        } catch (ResourceAlreadyExistsException | ResourceNotFoundException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        validatePartialContentResponse(get, position, rangeEnd);

        InputStream in = get.getInputStream();
        int bytesRead = in.read(buffer, offset, bytesToRead);
        if (bytesRead > 0) {
            position += bytesRead;
        }
        return bytesRead;
    }

    @Override
    public long length() throws IOException {
        return contentLength;
    }

    // Verifies that the URL supports range requests and fetches the content length.
    private long fetchContentLength(URL url) throws IOException, ResourceNotFoundException {
        HttpGet get = new HttpGet(url, true);
        get.setHeadOnly(true);
        get.setFollowRedirects(true);

        try {
            get.prepare();
            long contentLength = get.getContentLength();

            String acceptRanges = get.getResponseHeader(HDR_ACCEPT_RANGES);
            if (acceptRanges == null) {
                throw new UnsupportedOperationException("Range requests not supported for this URL : " + url);
            }

            return contentLength;
        } catch (ResourceAlreadyExistsException | InterruptedException e) {
            throw new RuntimeException("BUG: unexpected fail : ", e);
        }
    }

    @Override
    public void close() throws IOException {
        // Nothing to close
    }

    // verify status code and content-range header
    private void validatePartialContentResponse(HttpGet get, long expectedStart, long expectedEnd) throws IOException {
        log.debug("Validating partial content response");
        int responseCode = get.getResponseCode();
        String contentRange = get.getResponseHeader(HDR_CONTENT_RANGE);
        if (responseCode != 206 || contentRange == null) {
            throw new IOException("Expected HTTP 206 Partial Content and Content-Range header, got: " + responseCode + ", Content-Range: " + contentRange);
        }

        Pattern pattern = Pattern.compile("bytes (\\d+)-(\\d+)/(\\d+|\\*)");
        Matcher matcher = pattern.matcher(contentRange);
        if (matcher.matches()) {
            long start = Long.parseLong(matcher.group(1));
            long end = Long.parseLong(matcher.group(2));

            if (start != expectedStart || end != expectedEnd) {
                throw new IOException(String.format("Server returned unexpected range: %d-%d (expected %d-%d)",
                        start, end, expectedStart, expectedEnd));
            }
        } else {
            throw new IOException("Invalid content-range format: " + contentRange);
        }
        log.debug("Partial content response validated");
    }
}
