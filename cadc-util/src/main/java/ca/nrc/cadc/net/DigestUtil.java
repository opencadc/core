/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2021.                            (c) 2021.
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
 *  : 5 $
 *
 ************************************************************************
 */

package ca.nrc.cadc.net;

import ca.nrc.cadc.util.StringUtil;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.log4j.Logger;

/**
 * Utility class for the Digest HTTP header.
 */
public class DigestUtil {
    private static Logger log = Logger.getLogger(DigestUtil.class);

    /**
     * Supported algorithm's in DigestUtil header.
     */
    public enum Algorithm {
        MD5("md5"),
        SHA1("sha-1"),
        SHA256("sha-256"),
        SHA384("sha-384"),
        SHA512("sha-512");

        public final String value;

        private Algorithm(String value) {
            this.value = value;
        }

        public static Algorithm fromString(String value) {
            for (Algorithm algorithm : Algorithm.values()) {
                if (algorithm.value.equalsIgnoreCase(value)) {
                    return algorithm;
                }
            }
            throw new IllegalArgumentException(String.format("Algorithm not found for value %s", value));
        }
    }

    /**
     * Parse the given digest string and return a URI of the form: {algorithm}:{checksum in hex}
     * @param digest value to parse
     * @return digest URI
     */
    public static URI getURI(String digest) {
        if (!StringUtil.hasLength(digest)) {
            return null;
        }

        int index = digest.indexOf('=');
        if (index == -1) {
            throw new IllegalArgumentException(String.format("Expected digest format <algorithm>=<checksum value> "
                                                                 + "found %s", digest));
        }
        String scheme = digest.substring(0, index).trim();
        String checksumValue = digest.substring(index + 1).trim();

        if (scheme.length() == 0) {
            throw new IllegalArgumentException(String.format("Unable to parse algorithm, expected format "
                                                                 + "<algorithm>=<checksum value> found %s",
                                                             digest));
        }
        if (checksumValue.length() == 0) {
            throw new IllegalArgumentException(String.format("Unable to parse checksum, expected format "
                                                                 + "<algorithm>=<checksum value> found %s",
                                                             digest));
        }

        Algorithm algorithm;
        try {
            algorithm = Algorithm.fromString(scheme);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Unsupported algorithm %s", scheme));
        }

        // For a MD5 checksum, determine if the checksum value is hex or base64
        boolean isHex = true;
        if (algorithm == Algorithm.MD5) {
            for (int i = 0; i < checksumValue.length(); i++) {
                if (Character.digit(checksumValue.charAt(i), 16) == -1) {
                    isHex = false;
                    break;
                }
            }
            if (!isHex) {
                // decode MD5 base64 checksum into hex
                checksumValue = base64Decode(checksumValue);
            }
            if (checksumValue.length() != 32) {
                throw new IllegalArgumentException(String.format("Invalid MD5 checksum, expected 32 hex chars, "
                                                                     + "found %s in %s",
                                                                 checksumValue.length(), checksumValue));
            }
        } else {
            // decode all other algorithms checksum into hex
            checksumValue = base64Decode(checksumValue);
        }

        // SHA-1 20 bytes or 40 chars
        if (algorithm == Algorithm.SHA1 && checksumValue.length() != 40) {
            throw new IllegalArgumentException(String.format("Invalid SHA-1 checksum, expected 40 chars, "
                                                                 + "found %s in %s",
                                                             checksumValue.length(), checksumValue));
        }

        // SHA-256 32 bytes or 64 chars
        if (algorithm == Algorithm.SHA256 && checksumValue.length() != 64) {
            throw new IllegalArgumentException(String.format("Invalid SHA-256 checksum, expected 64 chars, "
                                                                 + "found %s in %s",
                                                             checksumValue.length(), checksumValue));
        }

        // SHA-384 48 bytes or 96 chars
        if (algorithm == Algorithm.SHA384 && checksumValue.length() != 96) {
            throw new IllegalArgumentException(String.format("Invalid SHA-384 checksum, expected 96 chars, "
                                                                 + "found %s in %s",
                                                             checksumValue.length(), checksumValue));
        }

        // SHA-512 64 bytes or 128 chars
        if (algorithm == Algorithm.SHA512 && checksumValue.length() != 128) {
            throw new IllegalArgumentException(String.format("Invalid SHA-512 checksum, expected 128 chars, "
                                                                 + "found %s in %s",
                                                             checksumValue.length(), checksumValue));
        }

        return URI.create(algorithm.value + ":" + checksumValue);
    }

    /**
     * Encode the given value using the Base64 encoding scheme.
     * @param value Value to encode.
     * @return Base64 encode of the value.
     */
    public static String base64Encode(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value can not be null");
        }
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decode the given value using the Base64 encoding scheme.
     * @param value Value to decode.
     * @return Base64 decode of the value.
     */
    public static String base64Decode(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value can not be null");
        }
        byte[] decoded = Base64.getDecoder().decode(value.getBytes(StandardCharsets.UTF_8));
        return new String(decoded, StandardCharsets.UTF_8);
    }

}
