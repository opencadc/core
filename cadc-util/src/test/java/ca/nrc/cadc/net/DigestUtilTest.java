/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2023.                            (c) 2023.
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

import ca.nrc.cadc.util.HexUtil;
import ca.nrc.cadc.util.Log4jInit;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class DigestUtilTest {
    private static Logger log = Logger.getLogger(DigestUtilTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.net", Level.INFO);
    }

    @Test
    public void testRoundtrip() {
        try {
            String input = "Hello World";
            MessageDigest md = MessageDigest.getInstance("md5");
            byte[] md5Bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            String md5Hex = HexUtil.toHex(md5Bytes);
            URI checksumURI = URI.create("md5:" + md5Hex);
            
            String digestValue = DigestUtil.toDigest(checksumURI);
            URI roundTrip = DigestUtil.getURI(digestValue);
            Assert.assertEquals(checksumURI, roundTrip);
            
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testGetDigest() {
        try {
            String input = "Hello World";
            MessageDigest md = MessageDigest.getInstance("md5");
            byte[] md5Bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            String md5Hex = HexUtil.toHex(md5Bytes);
            String md5Base64 = DigestUtil.base64EncodeHex(md5Hex);

            md = MessageDigest.getInstance("sha-1");
            byte[] sha1Bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            String sha1Hex = HexUtil.toHex(sha1Bytes);
            String sha1Base64 = DigestUtil.base64EncodeHex(sha1Hex);

            md = MessageDigest.getInstance("sha-256");
            byte[] sha256Bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            String sha256Hex = HexUtil.toHex(sha256Bytes);
            String sha256Base64 = DigestUtil.base64EncodeHex(sha256Hex);

            md = MessageDigest.getInstance("sha-384");
            byte[] sha384Bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            String sha384Hex = HexUtil.toHex(sha384Bytes);
            String sha384Base64 = DigestUtil.base64EncodeHex(sha384Hex);

            md = MessageDigest.getInstance("sha-512");
            byte[] sha512Bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            String sha512Hex = HexUtil.toHex(sha512Bytes);
            String sha512Base64 = DigestUtil.base64EncodeHex(sha512Hex);

            String digest;
            URI uri;
            String algorithm;

            /**
             * Invalid digest checksums.
             */
            digest = null;
            uri = DigestUtil.getURI(digest);
            Assert.assertNull("uri is not null", uri);

            digest = "";
            uri = DigestUtil.getURI(digest);
            Assert.assertNull("uri is not null", uri);

            digest = "a";
            try {
                uri = DigestUtil.getURI(digest);
                Assert.fail("IllegalArgumentException not thrown");
            }
            catch (IllegalArgumentException expected) {
                Assert.assertTrue(expected.getMessage().contains("Expected digest format"));
            }

            digest = "a=";
            try {
                uri = DigestUtil.getURI(digest);
                Assert.fail("IllegalArgumentException not thrown");
            }
            catch (IllegalArgumentException expected) {
                Assert.assertTrue(expected.getMessage().contains("Unable to parse checksum"));
            }

            digest = "=b";
            try {
                uri = DigestUtil.getURI(digest);
                Assert.fail("IllegalArgumentException not thrown");
            }
            catch (IllegalArgumentException expected) {
                Assert.assertTrue(expected.getMessage().contains("Unable to parse algorithm"));
            }

            digest = "=";
            try {
                uri = DigestUtil.getURI(digest);
                Assert.fail("IllegalArgumentException not thrown");
            }
            catch (IllegalArgumentException expected) {
                Assert.assertTrue(expected.getMessage().contains("Unable to parse algorithm"));
            }

            digest = "a=b";
            try {
                uri = DigestUtil.getURI(digest);
                Assert.fail("IllegalArgumentException not thrown");
            }
            catch (IllegalArgumentException expected) {
                Assert.assertTrue(expected.getMessage().contains("Unsupported algorithm"));
            }

            digest = "md5=";
            try {
                uri = DigestUtil.getURI(digest);
                Assert.fail("IllegalArgumentException not thrown");
            }
            catch (IllegalArgumentException expected) {
                Assert.assertTrue(expected.getMessage().contains("Unable to parse checksum"));
            }

            digest = "=" + md5Base64;
            try {
                uri = DigestUtil.getURI(digest);
                Assert.fail("IllegalArgumentException not thrown");
            }
            catch (IllegalArgumentException expected) {
                Assert.assertTrue(expected.getMessage().contains("Unable to parse algorithm"));
            }

            digest = "md6=" + md5Base64;
            try {
                uri = DigestUtil.getURI(digest);
                Assert.fail("IllegalArgumentException not thrown");
            }
            catch (IllegalArgumentException expected) {
                Assert.assertTrue(expected.getMessage().contains("Unsupported algorithm"));
            }

            // base64 length is multiple of 4
            digest = "md5=" + md5Base64.substring(0, md5Base64.length() - 4);
            try {
                uri = DigestUtil.getURI(digest);
                Assert.fail("IllegalArgumentException not thrown");
            }
            catch (IllegalArgumentException expected) {
                Assert.assertTrue(expected.getMessage().contains("Invalid MD5 checksum"));
            }

            digest = "sha-1=" + sha1Base64.substring(0, sha1Base64.length() - 4);
            try {
                uri = DigestUtil.getURI(digest);
                Assert.fail("IllegalArgumentException not thrown");
            }
            catch (IllegalArgumentException expected) {
                Assert.assertTrue(expected.getMessage().contains("Invalid SHA-1 checksum"));
            }

            digest = "sha-256="  + sha256Base64.substring(0, sha256Base64.length() - 4);;
            try {
                uri = DigestUtil.getURI(digest);
                Assert.fail("IllegalArgumentException not thrown");
            }
            catch (IllegalArgumentException expected) {
                Assert.assertTrue(expected.getMessage().contains("Invalid SHA-256 checksum"));
            }

            digest = "sha-384=" + sha384Base64.substring(0, sha384Base64.length() - 4);;
            try {
                uri = DigestUtil.getURI(digest);
                Assert.fail("IllegalArgumentException not thrown");
            }
            catch (IllegalArgumentException expected) {
                Assert.assertTrue(expected.getMessage().contains("Invalid SHA-384 checksum"));
            }

            digest = "sha-512="  + sha512Base64.substring(0, sha512Base64.length() - 4);;
            try {
                uri = DigestUtil.getURI(digest);
                Assert.fail("IllegalArgumentException not thrown");
            }
            catch (IllegalArgumentException expected) {
                Assert.assertTrue(expected.getMessage().contains("Invalid SHA-512 checksum"));
            }

            /*
             * Valid digest checksums.
             */

            // md5 16 bytes, 32 chars
            algorithm = "md5";
            try {
                uri = DigestUtil.getURI(String.format("%s=%s", algorithm, md5Base64));
                Assert.assertNotNull("uri is null", uri);
                Assert.assertEquals(URI.create(String.format("%s:%s",algorithm, md5Hex)), uri);
            }
            catch (IllegalArgumentException expected) {
                Assert.fail("IllegalArgumentException: " + expected.getMessage());
            }

            // SHA-1 20 bytes, 40 chars
            algorithm = "sha-1";
            try {
                uri = DigestUtil.getURI(String.format("%s=%s", algorithm, sha1Base64));
                Assert.assertNotNull("uri is null", uri);
                Assert.assertEquals(URI.create(String.format("%s:%s",algorithm, sha1Hex)), uri);
            }
            catch (IllegalArgumentException expected) {
                Assert.fail("IllegalArgumentException: " + expected.getMessage());
            }

            // SHA-256 32 bytes, 64 chars
            algorithm = "sha-256";
            try {
                uri = DigestUtil.getURI(String.format("%s=%s", algorithm, sha256Base64));
                Assert.assertNotNull("uri is null", uri);
                Assert.assertEquals(URI.create(String.format("%s:%s",algorithm, sha256Hex)), uri);
            }
            catch (IllegalArgumentException expected) {
                Assert.fail("IllegalArgumentException: " + expected.getMessage());
            }

            // SHA-384 48 bytes, 96 chars
            algorithm = "sha-384";
            try {
                uri = DigestUtil.getURI(String.format("%s=%s", algorithm, sha384Base64));
                Assert.assertNotNull("uri is null", uri);
                Assert.assertEquals(URI.create(String.format("%s:%s",algorithm, sha384Hex)), uri);
            }
            catch (IllegalArgumentException expected) {
                Assert.fail("IllegalArgumentException: " + expected.getMessage());
            }

            // SHA-512 64 bytes, 128 chars
            algorithm = "sha-512";
            try {
                uri = DigestUtil.getURI(String.format("%s=%s", algorithm, sha512Base64));
                Assert.assertNotNull("uri is null", uri);
                Assert.assertEquals(URI.create(String.format("%s:%s",algorithm, sha512Hex)), uri);
            }
            catch (IllegalArgumentException expected) {
                Assert.fail("IllegalArgumentException: " + expected.getMessage());
            }

        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
}
