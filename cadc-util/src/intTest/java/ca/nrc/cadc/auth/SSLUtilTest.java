/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2024.                            (c) 2024.
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

package ca.nrc.cadc.auth;

import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;
import java.io.File;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Set;
import javax.net.SocketFactory;
import javax.security.auth.Subject;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for SSLUtil.
 *
 * @author pdowler
 */
public class SSLUtilTest
{
    private static Logger log = Logger.getLogger(SSLUtilTest.class);
    private static String TEST_PEM_FN = "proxy.pem";
    private static File SSL_PEM;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
        Log4jInit.setLevel("ca.nrc.cadc.auth", Level.INFO);
        SSL_PEM = FileUtil.getFileFromResource(TEST_PEM_FN, SSLUtilTest.class);
    }
    
    @Test
    public void testGetSocketFactoryFromNull() throws Exception
    {
        try
        {
            SocketFactory sf;
            
            X509CertificateChain chain = null;
            sf = SSLUtil.getSocketFactory(chain);
            Assert.assertNotNull("SSLSocketFactory from null X509CertificateChain", sf);

            Subject sub = null;
            sf = SSLUtil.getSocketFactory(sub);
            Assert.assertNull("SSLSocketFactory from null Subject", sf);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            Assert.fail("unexpected exception: " + t);
        }
    }

    @Test
    public void testGetSocketFactoryFromFile() throws Exception
    {
        try
        {
            SocketFactory sf;

            sf = SSLUtil.getSocketFactory(SSL_PEM);
            Assert.assertNotNull("SSLSocketFactory from cert/key file", sf);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            Assert.fail("unexpected exception: " + t);
        }
    }
    
    @Test
    public void testReadUserCert() {
        // test reading a real user cert
        try {
            File f = new File(System.getProperty("user.home") + "/.ssl/" + System.getProperty("user.name") + ".pem");
            log.info("in: " + f.getAbsolutePath());
            
            Subject s = SSLUtil.createSubject(f);
            log.info("created: " + s);
            Assert.assertFalse(s.getPrincipals().isEmpty());
            
            Set<X509CertificateChain> cs = s.getPublicCredentials(X509CertificateChain.class);
            Assert.assertFalse("chain", cs.isEmpty());
            X509CertificateChain chain = cs.iterator().next();
            Assert.assertNotNull(chain.getChain());
            Assert.assertEquals(1, chain.getChain().length);
            Assert.assertNotNull(chain.getPrivateKey());
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testInitSSL() throws Exception
    {
        try
        {
            SSLUtil.initSSL(SSL_PEM);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            Assert.fail("unexpected exception: " + t);
        }
    }

    @Test
    public void testValidSubject() throws Exception
    {
        boolean thrown = false;
        
        // subject with no credentials
        Subject subject = new Subject();
        try
        {
            SSLUtil.validateSubject(subject, null);
        }
        catch(CertificateException ex)
        {
            thrown = true;
        }
        Assert.assertTrue("CertificateException expected", thrown);
        
        subject = SSLUtil.createSubject(SSL_PEM);
        
        // subject with valid credentials
        SSLUtil.validateSubject(subject, null);
        
        GregorianCalendar date = new GregorianCalendar();
        
        thrown = false;
        // Move the date way in the past so the certificate should
        // not be valid yet
        date.add(Calendar.YEAR, -15);
        try
        {
            SSLUtil.validateSubject(subject, date.getTime());
        }
        catch(CertificateNotYetValidException ex)
        {
            thrown = true;
        }
        Assert.assertTrue("CertificateNotYetValidException expected", thrown);
        
        thrown = false;
        // Move the date way in the future so the certificate should
        // not be valid anymore (double the number of years we moved
        // back in the previous step
        date.add(Calendar.YEAR, 30);
        try
        {
            SSLUtil.validateSubject(subject, date.getTime());
        }
        catch(CertificateExpiredException ex)
        {
            thrown = true;
        }
        Assert.assertTrue("CertificateExpiredException expected", thrown);
    }

}
