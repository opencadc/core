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

import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.AuthorizationToken;
import ca.nrc.cadc.auth.AuthorizationTokenPrincipal;
import ca.nrc.cadc.auth.BearerTokenPrincipal;
import ca.nrc.cadc.auth.SignedToken;
import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.auth.SSOCookieCredential;
import ca.nrc.cadc.util.Log4jInit;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import javax.security.auth.Subject;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.PrivilegedAction;
import java.util.TreeMap;

/**
 *
 * @author pdowler
 */
public class HttpTransferTest {

    private static Logger log = Logger.getLogger(HttpTransferTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.net", Level.INFO);
    }

    @Test
    public void testConstructors() {
        log.debug("TEST: testConstructors");

        try {
            URL nurl = null;
            URL url = new URL("https://www.example.net/robots.txt");
            InputStream nis = null;
            OutputStream nbos = null;
            OutputStream bos = new ByteArrayOutputStream();
            File nfile = null;
            File file = new File("/tmp/foo.txt");

            try {
                new HttpDownload(nurl, bos);
                Assert.fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
                log.debug("caught expected: " + expected);
            }

            try {
                new HttpDownload(url, nbos);
                Assert.fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
                log.debug("caught expected: " + expected);
            }

            try {
                new HttpDownload(url, nfile);
                Assert.fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
                log.debug("caught expected: " + expected);
            }
            
            try {
                new HttpGet(nurl, bos);
                Assert.fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
                log.debug("caught expected: " + expected);
            }

            try {
                new HttpGet(url, nbos);
                Assert.fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
                log.debug("caught expected: " + expected);
            }

            try {
                new HttpUpload(file, nurl);
                Assert.fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
                log.debug("caught expected: " + expected);
            }

            try {
                new HttpUpload(nfile, url);
                Assert.fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
                log.debug("caught expected: " + expected);
            }

            try {
                new HttpUpload(nis, url);
                Assert.fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
                log.debug("caught expected: " + expected);
            }

            try {
                new HttpPost(nurl, new TreeMap<String, Object>(), true);
                Assert.fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
                log.debug("caught expected: " + expected);
            }
            try {
                FileContent nfc = null;
                new HttpPost(url, nfc, true);
                Assert.fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
                log.debug("caught expected: " + expected);
            }

            try {
                new HttpDelete(nurl, true);
                Assert.fail("expected IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
                log.debug("caught expected: " + expected);
            }

        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testGetLoggableURL() {
        log.debug("TEST: testGetLoggableURL");
        try {
            // no trailing /
            URL in = new URL("https://www.example.net");
            String exp = "https://www.example.net";
            String act = HttpTransfer.toLoggableString(in);
            log.info("in: " + in + " loggable: " + act);
            Assert.assertEquals(exp, act);
            
            // with trailing /
            in = new URL("https://www.example.net/");
            exp = "https://www.example.net/";
            act = HttpTransfer.toLoggableString(in);
            log.info("in: " + in + " loggable: " + act);
            Assert.assertEquals(exp, act);
            
            // one path comp
            in = new URL("https://www.example.net/foo");
            exp = "https://www.example.net/foo";
            act = HttpTransfer.toLoggableString(in);
            log.info("in: " + in + " loggable: " + act);
            Assert.assertEquals(exp, act);
            
            // one path comp; trailing /
            in = new URL("https://www.example.net/foo/");
            exp = "https://www.example.net/foo/";
            act = HttpTransfer.toLoggableString(in);
            log.info("in: " + in + " loggable: " + act);
            Assert.assertEquals(exp, act);
            
            in = new URL("https://www.example.net/foo?BAR=1");
            exp = "https://www.example.net/foo?...";
            act = HttpTransfer.toLoggableString(in);
            log.info("in: " + in + " loggable: " + act);
            Assert.assertEquals(exp, act);
            
            in = new URL("https://www.example.net/foo/?BAR=1");
            exp = "https://www.example.net/foo/?...";
            act = HttpTransfer.toLoggableString(in);
            log.info("in: " + in + " loggable: " + act);
            Assert.assertEquals(exp, act);
            
            in = new URL("https://www.example.net/foo/bar?BAZ=1");
            exp = "https://www.example.net/foo/bar?...";
            act = HttpTransfer.toLoggableString(in);
            log.info("in: " + in + " loggable: " + act);
            Assert.assertEquals(exp, act);
            
            in = new URL("https://www.example.net/foo/bar/?BAZ=1");
            exp = "https://www.example.net/foo/bar/?...";
            act = HttpTransfer.toLoggableString(in);
            log.info("in: " + in + " loggable: " + act);
            Assert.assertEquals(exp, act);
            
            in = new URL("https://www.example.net/foo/bar/1234567890?BAZ=1");
            exp = "https://www.example.net/foo/bar/...?...";
            act = HttpTransfer.toLoggableString(in);
            log.info("in: " + in + " loggable: " + act);
            Assert.assertEquals(exp, act);
            
            in = new URL("https://www.example.net/foo/bar/1234567890/?BAZ=1");
            exp = "https://www.example.net/foo/bar/...?...";
            act = HttpTransfer.toLoggableString(in);
            log.info("in: " + in + " loggable: " + act);
            Assert.assertEquals(exp, act);
            
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testBufferSize() throws Exception {
        log.debug("TEST: testBufferSize");
        try {
            String cur = System.getProperty(HttpTransfer.class.getName() + ".bufferSize");
            Assert.assertNull("test setup", cur);

            HttpTransfer trans = new TestDummy();
            Assert.assertEquals("default buffer size", HttpTransfer.DEFAULT_BUFFER_SIZE, trans.getBufferSize());

            trans.setBufferSize(12345);
            Assert.assertEquals("set buffer size", 12345, trans.getBufferSize());

            System.setProperty(HttpTransfer.class.getName() + ".bufferSize", "16384");
            trans = new TestDummy();
            Assert.assertEquals("system prop buffer size (bytes)", 16384, trans.getBufferSize());

            System.setProperty(HttpTransfer.class.getName() + ".bufferSize", "32k");
            trans = new TestDummy();
            Assert.assertEquals("system prop buffer size KB", 32 * 1024, trans.getBufferSize());

            System.setProperty(HttpTransfer.class.getName() + ".bufferSize", "2m");
            trans = new TestDummy();
            Assert.assertEquals("system prop buffer size MB", 2 * 1024 * 1024, trans.getBufferSize());

            // bad syntax -> default
            System.setProperty(HttpTransfer.class.getName() + ".bufferSize", "123d");
            trans = new TestDummy();
            Assert.assertEquals("system prop buffer size (invalid)", HttpTransfer.DEFAULT_BUFFER_SIZE, trans.getBufferSize());

        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testLogIO() {
        try {
            HttpTransfer test = new TestDummy();
            Assert.assertNull(test.getIOReadTime());
            Assert.assertNull(test.getIOWriteTime());

            test = new TestDummy();
            test.setLogIO(true);
            Assert.assertNotNull(test.getIOReadTime());
            Assert.assertNotNull(test.getIOWriteTime());
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    /*
    // fails with java 21 + gradle8
    @Test
    public void setRequestAuthHeaders() throws Exception {
        final URL testURL = new URL("http://www.fr.host.com/my/path/to/file.txt");
        final HttpTransfer testSubject = new HttpTransfer(testURL, false) {
            @Override
            public void prepare() {
            }

            @Override
            public void run() {
            }
        };

        final Subject subject = new Subject();
        Date expiry = new Date(new Date().getTime() + 48 * 3600 * 1000);
        
        AuthorizationToken authToken = new AuthorizationToken("Bearer", "123", Arrays.asList("en.host.com", "fr.host.com"));
        subject.getPublicCredentials().add(authToken);
        
        subject.getPublicCredentials().add(
                new SSOCookieCredential("VALUE_1", "en.host.com", expiry));
        subject.getPublicCredentials().add(
                new SSOCookieCredential("VALUE_2", "fr.host.com", expiry));

        final HttpURLConnection mockConnection
                = EasyMock.createMock(HttpURLConnection.class);

        EasyMock.expect(mockConnection.getURL()).andReturn(testURL).anyTimes();

        mockConnection.setRequestProperty(AuthenticationUtil.AUTHORIZATION_HEADER, "Bearer 123");
        EasyMock.expectLastCall().once();
        mockConnection.setRequestProperty("Cookie", "CADC_SSO=\"VALUE_2\"");
        EasyMock.expectLastCall().once();

        EasyMock.replay(mockConnection);

        Subject.doAs(subject, new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                testSubject.setRequestAuthHeaders(mockConnection);
                return null;
            }
        });

        EasyMock.verify(mockConnection);
    }
    */
    
    private class TestDummy extends HttpTransfer {

        TestDummy() throws MalformedURLException {
            super(new URL("http://www.fr.host.com/my/path/to/file.txt"), true);
        }

        @Override
        public void prepare() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void run() {
            prepare();
        }

    }
}
