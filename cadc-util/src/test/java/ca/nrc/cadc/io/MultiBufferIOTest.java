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
************************************************************************
*/

package ca.nrc.cadc.io;

import ca.nrc.cadc.util.HexUtil;
import ca.nrc.cadc.util.Log4jInit;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Random;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author pdowler
 */
public class MultiBufferIOTest {
    private static final Logger log = Logger.getLogger(MultiBufferIOTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.io", Level.INFO);
    }
    
    public MultiBufferIOTest() { 
    }
    
    @Test
    public void testSingleBuffer() {
        doRoundtrip(1);
    }
    
    @Test
    public void testDoubleBuffer() {
        doRoundtrip(2);
    }
    
    @Test
    public void testTripleBuffer() {
        doRoundtrip(3);
    }
    
    private void doRoundtrip(int num) {
        try {
            Random rnd = new Random();
            byte[] data = new byte[32 * 1024];
            rnd.nextBytes(data);
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);
            final String expectedMD5 = HexUtil.toHex(md.digest());
            md.reset();
            
            InputStream istream = new ByteArrayInputStream(data);
            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            
            MultiBufferIO cb = new MultiBufferIO(num, 8192);
            cb.copy(istream, ostream);
            byte[] actual = ostream.toByteArray();
            Assert.assertEquals("length", data.length, actual.length);
            md.update(actual);
            String actualMD5 = HexUtil.toHex(md.digest());
            Assert.assertEquals("md5", expectedMD5, actualMD5);
            
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testReadFail() {
        try {
            InputStream istream = getFailingInput(12345);
            OutputStream ostream = new DiscardOutputStream();
            
            MultiBufferIO cb = new MultiBufferIO(3, 8192); // fail filling second buffer
            log.info("testReadFail: starting copy...");
            cb.copy(istream, ostream);
            Assert.fail("expected ReadException but copy completed");
        } catch (ReadException expected) {
            log.info("caught expected: " + expected);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testWriteFail() {
        try {
            Random rnd = new Random();
            byte[] data = new byte[32*1024];
            rnd.nextBytes(data);
            InputStream istream = new ByteArrayInputStream(data);
            OutputStream ostream = getFailingOutputStream(12345);
            
            MultiBufferIO cb = new MultiBufferIO(3, 8192); // fail writing second buffer
            log.info("testWriteFail: starting copy...");
            cb.copy(istream, ostream);
            Assert.fail("expected WriteException but copy completed");
        } catch (WriteException expected) {
            log.info("caught expected: " + expected);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    private InputStream getFailingInput(final int failAfter) {
        return new InputStream() {
            int num = 0;
            
            @Override
            public int read() throws IOException {
                if (num++ > failAfter) {
                    throw new IOException("failAfter: " + failAfter);
                }
                return 123;
            }
        };
    }
    
    private OutputStream getFailingOutputStream(final int failAfter) {
        return new OutputStream() {
            int num = 0;
            
            @Override
            public void write(int i) throws IOException {
                if (num++ > failAfter) {
                    throw new IOException("failAfter: " + failAfter);
                }
            }
        };
    }
}
