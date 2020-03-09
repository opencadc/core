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

import ca.nrc.cadc.io.ThreadedIO;
import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.AccessControlException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author pdowler
 */
public class HttpGetTest {

    private static Logger log = Logger.getLogger(HttpGetTest.class);

    private final URL okURL;
    private final URL permissionDeniedURL;
    private final URL notFoundURL;

    private File tmpDir;

    static {
        Log4jInit.setLevel("ca.nrc.cadc.net", Level.DEBUG);
    }

    public HttpGetTest() throws Exception {
        this.okURL = new URL("https://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/robots.txt");
        this.permissionDeniedURL = new URL("https://httpbin.org/status/403");
        this.notFoundURL = new URL("https://httpbin.org/status/404");

        this.tmpDir = new File(System.getProperty("java.io.tmpdir"));
    }

    @Test
    public void testHeadOnly() throws Exception {
        log.debug("TEST: testHeadOnly");
        URL src = okURL;
        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        try {
            HttpGet dl = new HttpGet(src, dest);
            dl.setHeadOnly(true);
            dl.run();
            Assert.assertEquals("response code", 200, dl.getResponseCode());
            Assert.assertTrue("no bytes read", dest.toByteArray().length == 0); // did not actually read bytes
            Assert.assertTrue("content-length", dl.getContentLength() > 0L);
            Assert.assertNotNull("content-type", dl.getContentType());
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testPrepareInputStream() throws Exception {
        log.debug("TEST: testPrepareInputStream");

        try {
            URL src = okURL;
            HttpGet dl = new HttpGet(src, true);
            dl.prepare();
            Assert.assertEquals("response code", 200, dl.getResponseCode());

            ByteArrayOutputStream dest = new ByteArrayOutputStream();
            ThreadedIO tio = new ThreadedIO();
            tio.ioLoop(dest, dl.getInputStream());
            byte[] out = dest.toByteArray();
            Assert.assertNotNull("dest stream after download", out);
            Assert.assertTrue("size > 0", out.length > 0);
            Assert.assertNotNull(dl.getContentType());
            Assert.assertTrue("content-length > 0", dl.getContentLength() > 0);
            Assert.assertEquals("size == content-length", out.length, dl.getContentLength());
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testDownloadToOutputStream() throws Exception {
        log.debug("TEST: testDownloadToOutputStream");

        try {
            URL src = okURL;
            ByteArrayOutputStream dest = new ByteArrayOutputStream(8192);
            HttpGet dl = new HttpGet(src, dest);
            dl.run();
            dest.close();
            byte[] out = dest.toByteArray();
            Assert.assertEquals("response code", 200, dl.getResponseCode());
            Assert.assertNotNull("dest stream after download", out);
            Assert.assertTrue("size > 0", out.length > 0);
            Assert.assertNotNull(dl.getContentType());
            Assert.assertTrue("content-length > 0", dl.getContentLength() > 0);
            Assert.assertEquals("size == content-length", out.length, dl.getContentLength());
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testDownloadToInputStreamWrapper() throws Exception {
        log.debug("TEST: testDownloadToInputStreamWrapper");

        try {
            URL src = okURL;
            ByteArrayOutputStream dest = new ByteArrayOutputStream(8192);
            TestWrapper tw = new TestWrapper(dest);
            HttpGet dl = new HttpGet(src, tw);
            dl.run();
            dest.close();
            byte[] out = dest.toByteArray();
            Assert.assertEquals("response code", 200, dl.getResponseCode());
            Assert.assertNotNull("dest stream after download", out);
            Assert.assertTrue("size > 0", out.length > 0);
            Assert.assertNotNull(dl.getContentType());
            Assert.assertTrue("content-length > 0", dl.getContentLength() > 0);
            Assert.assertEquals("size == content-length", out.length, dl.getContentLength());
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    private class TestWrapper implements InputStreamWrapper {

        OutputStream dest;

        TestWrapper(OutputStream dest) {
            this.dest = dest;
        }

        public void read(InputStream in)
                throws IOException {
            byte[] buf = new byte[2048];
            int n = in.read(buf);
            while (n != -1) {
                dest.write(buf, 0, n);
                n = in.read(buf);
            }
        }

    }

    @Test
    public void testDownloadWithCustomRequestProperty() throws Exception {
        log.debug("TEST: testCustomRequestProperty");

        try {
            URL src = okURL;
            ByteArrayOutputStream dest = new ByteArrayOutputStream(8192);
            HttpGet dl = new HttpGet(src, dest);
            dl.setRequestProperty("X-Custom-Prop", "hello");
            dl.run();
            dest.close();
            byte[] out = dest.toByteArray();
            Assert.assertEquals("response code", 200, dl.getResponseCode());
            Assert.assertNotNull("dest stream after download", out);
            Assert.assertTrue("size > 0", out.length > 0);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testPrepareNotFound() throws Exception {
        log.debug("TEST: testPrepareNotFound");

        try {
            URL src = notFoundURL;
            HttpGet dl = new HttpGet(src, true);
            dl.prepare();
            Assert.fail("expected ResourceNotFoundException, got: " + dl.getResponseCode());
        } catch (ResourceNotFoundException expected) {
            log.info("caught expected: " + expected);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testDownloadNotFound() throws Exception {
        log.debug("TEST: testDownloadNotFound");

        try {
            URL src = notFoundURL;
            ByteArrayOutputStream dest = new ByteArrayOutputStream(8192);
            HttpGet dl = new HttpGet(src, dest);
            dl.run();
            Throwable t = dl.getThrowable();
            Assert.assertEquals("response code", 404, dl.getResponseCode());
            Assert.assertNotNull(t);
            Assert.assertTrue("throwable should be ResourceNotFoundException", t instanceof ResourceNotFoundException);
            log.debug("found expected exception: " + t.toString());
            //Assert.assertTrue(t.getMessage().startsWith("not found"));
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testPreparePermissionDenied() throws Exception {
        log.debug("TEST: testPreparePermissionDenied");

        try {
            URL src = permissionDeniedURL;
            HttpGet dl = new HttpGet(src, true);
            dl.prepare();
            Assert.fail("expected AccessControlException, got: " + dl.getResponseCode());
        } catch (AccessControlException expected) {
            log.info("caught expected: " + expected);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testDownloadPermissionDenied() throws Exception {
        log.debug("TEST: testDownloadPermissionDenied");

        try {
            URL src = permissionDeniedURL;
            ByteArrayOutputStream dest = new ByteArrayOutputStream(8192);
            HttpGet dl = new HttpGet(src, dest);
            dl.run();
            Throwable t = dl.getThrowable();
            Assert.assertEquals("response code", 403, dl.getResponseCode());
            Assert.assertNotNull(t);
            Assert.assertTrue("throwable should be AccessControlException", t instanceof AccessControlException);
            log.debug("found expected exception: " + t.toString());
            //Assert.assertTrue(t.getMessage().startsWith("permission denied"));
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
}
