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

/**
 * Constants for HTTP headers and status codes.
 *
 * */
public class HttpConstants {

    // Note: keep header keys lowercase to maintain uniformity.
    // Header Keys
    public static final String HDR_CONTENT_ENCODING = "content-encoding";
    public static final String HDR_CONTENT_LENGTH = "content-length";
    public static final String HDR_CONTENT_MD5 = "content-md5";
    public static final String HDR_CONTENT_TYPE = "content-type";
    public static final String HDR_CONTENT_RANGE = "content-range";
    public static final String HDR_CONTENT_DISPOSITION = "content-disposition";
    public static final String HDR_DIGEST = "digest";
    public static final String HDR_RETRY_AFTER = "retry-after";
    public static final String HDR_LOCATION = "location";
    public static final String HDR_ACCEPT = "accept";
    public static final String HDR_ACCEPT_ENCODING = "accept-encoding";
    public static final String HDR_ACCEPT_RANGES = "accept-ranges";
    public static final String HDR_RANGE = "range";
    public static final String HDR_CADC_CONTENT_LENGTH = "X-CADC-Content-Length";
    public static final String HDR_CADC_STREAM = "X-CADC-Stream";
    public static final String HDR_CADC_PARTIAL_READ = "X-CADC-Partial-Read";

    // Header Values
    public static final String RANGE_BYTES = "bytes";
    public static final String ENCODING_GZIP = "gzip";
    public static final String ENCODING_BASE64 = "base64";

    // Status Code
    public static final int HTTP_RANGE_NOT_SATISFIABLE = 416;
    public static final int HTTP_EXPECT_FAIL = 417;
    public static final int HTTP_LOCKED = 423;
}
