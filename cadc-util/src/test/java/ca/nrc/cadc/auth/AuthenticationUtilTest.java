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

package ca.nrc.cadc.auth;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Principal;
import java.security.PrivateKey;
import java.security.PrivilegedAction;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import ca.nrc.cadc.util.Log4jInit;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import org.junit.Assert;


/**
 * Tests the methods in AuthenticationUtil
 *
 * @author majorb
 *
 */
public class AuthenticationUtilTest
{

    private static Logger log = Logger.getLogger(AuthenticationUtilTest.class);

    @BeforeClass
    public static void beforeClass()
    {
        Log4jInit.setLevel("ca.nrc.cadc.auth", Level.INFO);
    }

    @Test
    public void testCreateSubjectPassword()
    {
        try
        {
            java.net.Authenticator impl = new java.net.Authenticator() 
            { 

                @Override
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    return new PasswordAuthentication("foo", "bar".toCharArray());
                }

            };
            Subject s = AuthenticationUtil.getSubject(impl);
            
            Assert.assertNotNull(s);
            Assert.assertEquals(AuthMethod.PASSWORD, AuthenticationUtil.getAuthMethod(s));
            Assert.assertEquals(AuthMethod.PASSWORD, AuthenticationUtil.getAuthMethodFromCredentials(s));
            
            PasswordAuthentication pa = java.net.Authenticator.requestPasswordAuthentication(InetAddress.getLocalHost(), 80, "https", "hi", "BASIC");
            Assert.assertEquals("foo", pa.getUserName());
            Assert.assertEquals("bar", String.valueOf(pa.getPassword()));
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }    
    
    @Test
    public void testHttpEqualsTrue()
    {
        String[][] testSet = new String[][]
              {
                  {"cadcregtest1", "cadcregtest1"} // same value
              };
        for (String[] userIdPair : testSet)
        {
            Principal p1 = new HttpPrincipal(userIdPair[0]);
            Principal p2 = new HttpPrincipal(userIdPair[1]);
            assertTrue(String.format("Should be equal: [%s] and: [%s]", userIdPair[0], userIdPair[1]),
                    AuthenticationUtil.equals(p1, p2));
            assertTrue(String.format("Should be equal: [%s] and: [%s]", userIdPair[1], userIdPair[0]),
                    AuthenticationUtil.equals(p2, p1));
        }

        // with proxy users
        Principal p1 = new HttpPrincipal("cadcregtest1", "cadcproxytest1");
        Principal p2 = new HttpPrincipal("cadcregtest1", "cadcproxytest1");
        assertTrue(p1 + " and " + p2 + " should be equal",
                AuthenticationUtil.equals(p1, p2));
        assertTrue(p2 + " and " + p1 + " should be equal",
                AuthenticationUtil.equals(p2, p1));
    }

    @Test
    public void testHttpEqualsFalse()
    {
        String[][] testSet = new String[][]
        {
            {"cadcregtest1", null, "cadcregtest2", null}, // different values
            {"cadcregtest1", null, "CADCREGTEST11", null}, // case should be sensitive
            {"cadcregtest1", "cadcregtest2", "cadcregtest1", null}, // different proxy users
            {"cadcregtest1", "cadcregtest2", "cadcregtest1", "CADCREGTEST2"}, // different proxy users
        };
        for (String[] userIdPair : testSet)
        {
            Principal p1 = new HttpPrincipal(userIdPair[0], userIdPair[1]);
            Principal p2 = new HttpPrincipal(userIdPair[2], userIdPair[3]);
            assertFalse(p1 + " and " + p2 + " should not be equal",
                    AuthenticationUtil.equals(p1, p2));
            assertFalse(p2 + " and " + p1 + " should not be equal",
                    AuthenticationUtil.equals(p1, p2));
        }

    }

    @Test
    public void testX500EqualsTrue()
    {
        String[][] testSet = new String[][]
            {
                {"cn=cadc regtest1 10577,ou=cadc,o=hia,c=ca", "cn=cadc regtest1 10577,ou=cadc,o=hia,c=ca"},    // same value
              //  {"cn=cadc regtest1 10577,ou=cadc,o=hia,c=ca", "ou=cadc,o=hia,cn=cadc regtest1 10577,c=ca"},    // mixed elements
              //  {"cn=cadc regtest1 10577,ou=cadc,o=hia,c=ca", "OU=CADC,O=HIA,CN=CADC REGTEST1 10577,C=CA"},    // upper case
              //  {"cn=cadc regtest1 10577,ou=cadc,o=hia,c=ca", "ou=cadc, o=hia, cn=cadc regtest1 10577,c=ca"},  // mid-dn spaces
              //  {"cn=cadc regtest1 10577,cn=x123,cn=p,ou=cadc,o=hia,c=ca", "cn=p,cn=x123,cn=cadc regtest1 10577,ou=cadc,o=hia,ca=ca"},    // multiple rdns
              //  {"cn=cadc regtest1 10577,cn=a,ou=cadc,o=hia,c=ca", "cn=a,cn=cadc regtest1 10577,ou=cadc,o=hia,c=ca"},  // multiple rdn values
            };
        for (String[] dnPair : testSet)
        {
            Principal p1 = new X500Principal(dnPair[0]);
            Principal p2 = new X500Principal(dnPair[1]);
            log.info("p1: " + AuthenticationUtil.canonizeDistinguishedName(dnPair[0]));
            log.info("p2: " + AuthenticationUtil.canonizeDistinguishedName(dnPair[1]));
            assertTrue(String.format("Should be equal: [%s] and: [%s]", dnPair[0], dnPair[1]),
                    AuthenticationUtil.equals(p1, p2));
            assertTrue(String.format("Should be equal: [%s] and: [%s]", dnPair[1], dnPair[0]),
                    AuthenticationUtil.equals(p2, p1));
        }
    }

    @Test
    public void testX500EqualsFalse()
    {
        String[][] testSet = new String[][]
            {
                {"cn=cadc regtest1 10577,ou=cadc,o=hia,c=ca", "cn=cadc regtest2 10577,ou=cadc,o=hia,c=ca"}
            };
        for (String[] dnPair : testSet)
        {
            Principal p1 = new X500Principal(dnPair[0]);
            Principal p2 = new X500Principal(dnPair[1]);
            assertFalse(String.format("Should be unequal: [%s] and: [%s]", dnPair[0], dnPair[1]),
                    AuthenticationUtil.equals(p1, p2));
            assertFalse(String.format("Should be unequal: [%s] and: [%s]", dnPair[1], dnPair[0]),
                    AuthenticationUtil.equals(p2, p1));
        }
    }

    @Test
    public void testEqualsMixedPrincipalTypes()
    {
        String[][] testSet = new String[][]
        {
            {"cn=cadc regtest1 10577,ou=cadc,o=hia,c=ca", "cadcregtest1"}
        };
        for (String[] namePair : testSet)
        {
            Principal p1 = new X500Principal(namePair[0]);
            Principal p2 = new HttpPrincipal(namePair[1]);
            assertFalse(String.format("Should be unequal: [%s] and: [%s]", namePair[1], namePair[0]),
                    AuthenticationUtil.equals(p1, p2));
            assertFalse(String.format("Should be unequal: [%s] and: [%s]", namePair[0], namePair[1]),
                    AuthenticationUtil.equals(p2, p1));
        }
    }

    private void testCanonicalConversion(String expected, String[] conversions)
    {
        for (String toBeConverted : conversions)
        {
            try
            {
                // convert the string
                String converted = AuthenticationUtil.canonizeDistinguishedName(toBeConverted);
                assertEquals("[" + toBeConverted + "] should be coverted to expected.",
                        expected, converted);

                try
                {
                    // convert again to ensure no loss of data
                    String convertedAgain = AuthenticationUtil.canonizeDistinguishedName(converted);
                    assertEquals("[" + toBeConverted + "] should be coverted (second time) to expected.",
                            converted, convertedAgain);
                }
                catch (IllegalArgumentException e)
                {
                    assertTrue("Converting [" + toBeConverted + "] threw IllegalArguementException on second conversion.",
                            false);
                }
            }
            catch (IllegalArgumentException e)
            {
                log.error("unexpected", e);
                assertTrue("Converting [" + toBeConverted + "] threw IllegalArguementException on first conversion.",
                        false);
            }
        }
    }

    @Test
    public void testCanonicalConversionSuccess()
    {
        String expected = null;
        String[] conversions = null;

        // Proxy type DN conversions
        expected = "cn=cadc regtest1 10577,ou=cadc,o=hia,c=ca";
        conversions = new String[]
            {
                "cn=cadc regtest1 10577,ou=cadc,o=hia,c=ca",     // same value
                "CN=CADC REGTEST1 10577,OU=CADC,O=HIA,C=CA",     // all upper case
                "cN=cadc REGtest1 10577,ou=CADC,O=HiA,c=Ca",     // mixed case
                "cn=cadc regtest1 10577, ou=cadc, o=hia, c=ca",   // one space between elements
                "cn=cadc regtest1 10577,  ou=cadc,  o=hia,  c=ca", // two spaces between elements
                "Cn=cadc regtest1 10577,  ou=cADc,  O=hiA,  c=Ca", // two spaces between elements, mixed case
                " cn=cadc regtest1 10577,ou=cadc,o=hia,c=ca ",   // leading/trailing spaces
                "c=ca,o=hia,ou=cadc,cn=cadc regtest1 10577",     // reverse element order
                "c=Ca,O=hiA,ou=cadc,cN=cadc regTEST1 10577",     // reverse element order, mixed case
                "c=ca,  o=hia,  ou=cadc,  cn=cadc regtest1 10577", // reverse element order, two spaces between elements
     //           "ou=cadc,c=ca,cn=cadc regtest1 10577,o=hia",     // mixed order
     //           "ou=cadc,cN=caDC regtest1 10577,c=cA,O=HIA",     // mixed order, mixed case
     //           "/cn=cadc regtest1 10577/ou=cadc/o=hia/c=ca",    // leading slash separator
     //           "/c=ca/o=hia/ou=cadc/cn=cadc regtest1 10577",    // leading slash separator reverse order
    //            "cn=cadc regtest1 10577/ou=cadc/o=hia/c=ca",     // slash separator
     //           "c=ca/o=hia/ou=cadc/cn=cadc regtest1 10577",     // slash separator reverse order
            };
        testCanonicalConversion(expected, conversions);

        // C is in the middle so CN causes flip
        expected= "cn=joe user,o=groupb,c=it,dc=foo,dc=example,dc=org";
        conversions = new String[]
            {
                "CN=joe user, O=GroupB, C=it, DC=foo, DC=example, DC=org",
                "DC=org, DC=example, DC=foo, C=IT, O=GroupB, CN=Joe User"
            };
        testCanonicalConversion(expected, conversions);
        
        // no CN so C causes the flip
        expected= "ou=joe user,dc=foo,c=ca";
        conversions = new String[]
            {
                "OU=joe user, DC=foo, C=ca",
                "C=ca, DC=foo, OU=Joe User"
            };
        testCanonicalConversion(expected, conversions);

        // User type DN conversions
        expected = "cn=brian major,ou=hia.nrc.ca,o=grid,c=ca";
        conversions = new String[]
            {
                "cn=brian major,ou=hia.nrc.ca,o=grid,c=ca"
            };
        testCanonicalConversion(expected, conversions);

        // DN with mutiples of RDNs
        expected = "cn=a,cn=b,cn=c,ou=hia.nrc.ca,o=grid,c=ca";
        conversions = new String[]
             {
                 "cn=a,cn=b,cn=c,ou=hia.nrc.ca,o=grid,c=ca",
                 "c=ca,o=grid,ou=hia.nrc.ca,cn=c,cn=b,cn=a",
             };
         testCanonicalConversion(expected, conversions);

        // DN with comma in element
        expected = "cn=brian\\, major,ou=hia.nrc.ca,o=grid,c=ca";
        conversions = new String[]
             {
                 "cn=brian\\, major,ou=hia.nrc.ca,o=grid,c=ca"
             };
         testCanonicalConversion(expected, conversions);

        // DN with equals sign in element
        expected = "cn=brian\\=major,ou=hia.nrc.ca,o=grid,c=ca";
        conversions = new String[]
             {
                 "cn=brian\\=major,ou=hia.nrc.ca,o=grid,c=ca"
             };
         testCanonicalConversion(expected, conversions);

         // DN with double quote in element
//         expected = "cn=brian\"major,ou=hia.nrc.ca,o=grid,c=ca";
//         conversions = new String[]
//              {
//                  "cn=brian\"major,ou=hia.nrc.ca,o=grid,c=ca"
//              };
//         testCanonicalConversion(expected, conversions);

         // DN with single quote in element
         expected = "cn=brian'major,ou=hia.nrc.ca,o=grid,c=ca";
         conversions = new String[]
              {
                  "cn=brian'major,ou=hia.nrc.ca,o=grid,c=ca"
              };
         testCanonicalConversion(expected, conversions);

         // DN with accented letters
         expected = "cn=séverin gaudet,ou=hia.nrc.ca,o=grid,c=ca";
         conversions = new String[]
              {
                  "cn=Séverin Gaudet,ou=hia.nrc.ca,o=grid,c=ca"
              };
         testCanonicalConversion(expected, conversions);

    }

    @Test
    public void testCanonicalConversionFailure()
    {
        String[] conversions = null;

        conversions = new String[]
            {
                "cn=cadc regtest1 10577,ou=cadc,o=hia,z=a",  // unrecognized element z at end
                "cn=cadc regtest1 10577,z=a,ou=cadc,o=hia",  // unrecognized element z in middle
                "z=a,cn=cadc regtest1 10577,ou=cadc,o=hia",  // unrecognized element z in front
                "foo=cadc regtest1 10577,bar=cadc,o=hia,z=a",  // multiple unrecognized elements
            };

        for (String toBeConverted : conversions)
        {
            try
            {
                AuthenticationUtil.canonizeDistinguishedName(toBeConverted);
                assertTrue("[" + toBeConverted + "] should have thrown IllegalArgumentException", false);
            }
            catch (IllegalArgumentException e)
            {
                // expected
                log.debug(e);
            }
        }

    }

    @Test
    public void testGetSubjectFromHttpServletRequest_Anon()
    {
        log.debug("testGetSubjectFromHttpServletRequest_Anon - START");
        try
        {
            final HttpServletRequest mockRequest =
                createMock(HttpServletRequest.class);

            expect(mockRequest.getRemoteUser()).andReturn(null).atLeastOnce();
            expect(mockRequest.getCookies()).andReturn(null).atLeastOnce();
            expect(mockRequest.getHeader(AuthenticationUtil.AUTH_HEADER)).andReturn(null).atLeastOnce();
            expect(mockRequest.getAttribute(
                    "javax.servlet.request.X509Certificate")).andReturn(null).atLeastOnce();

            replay(mockRequest);
            final Subject subject1 = AuthenticationUtil.getSubject(mockRequest);

            assertEquals(0, subject1.getPrincipals().size());
            AuthMethod am = AuthenticationUtil.getAuthMethod(subject1);
            assertEquals(AuthMethod.ANON, am);

            verify(mockRequest);
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            fail("unexpected exception: " + unexpected);
        }
        finally
        {
            log.debug("testGetSubjectFromHttpServletRequest_anon - DONE");
        }
    }

    @Test
    public void testGetSubjectFromHttpServletRequest_HttpPrincipal()
    {
        log.debug("testGetSubjectFromHttpServletRequest_HttpPrincipal - START");
        try
        {
            final HttpServletRequest mockRequest =
                createMock(HttpServletRequest.class);

            expect(mockRequest.getRemoteUser()).andReturn("foo").atLeastOnce();
            expect(mockRequest.getCookies()).andReturn(null).atLeastOnce();
            expect(mockRequest.getHeader(AuthenticationUtil.AUTH_HEADER)).andReturn(null).atLeastOnce();
            expect(mockRequest.getAttribute(
                    "javax.servlet.request.X509Certificate")).andReturn(null).atLeastOnce();

            replay(mockRequest);
            final Subject subject1 = AuthenticationUtil.getSubject(mockRequest);

            assertEquals(1, subject1.getPrincipals().size());
            AuthMethod am = AuthenticationUtil.getAuthMethod(subject1);
            assertEquals(AuthMethod.PASSWORD, am);
            Principal p = null;
            for (Principal tmp : subject1.getPrincipals())
            {
                if (tmp instanceof HttpPrincipal)
                {
                    p = tmp;
                    break;
                }
            }
            assertNotNull(p);
            assertEquals("foo", p.getName());

            verify(mockRequest);
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            fail("unexpected exception: " + unexpected);
        }
        finally
        {
            log.debug("testGetSubjectFromHttpServletRequest_HttpPrincipal - DONE");
        }
    }

    @Test
    public void testGetSubjectFromPrincipalExtractorWithCookieAndX500()
            throws Exception
    {
        final PrincipalExtractor mockPrincipalExtractor =
                createMock(PrincipalExtractor.class);
        final Set<Principal> principalSet = new HashSet<Principal>();
        final X509CertificateChain mockCertChain =
                createMock(X509CertificateChain.class);

        // this needs to be a list of mocks.
        List<SSOCookieCredential> mockCookieCredentials = new ArrayList<>();
        final SSOCookieCredential cookie =createMock(SSOCookieCredential.class);
        mockCookieCredentials.add(cookie);

        // To overcome the empty principals.
        principalSet.add(new HttpPrincipal("USER1"));

        expect(mockPrincipalExtractor.getPrincipals()).andReturn(
                principalSet).once();

        expect(mockPrincipalExtractor.getCertificateChain()).andReturn(
                mockCertChain).once();

        expect(mockPrincipalExtractor.getDelegationToken()).andReturn(
                null).once();

        expect(mockPrincipalExtractor.getSSOCookieCredentials()).andReturn(
                mockCookieCredentials).once();

        replay(mockPrincipalExtractor, mockCertChain, cookie);

        final Subject s = AuthenticationUtil.getSubject(mockPrincipalExtractor);

        final Set<AuthMethod> authMethods =
                s.getPublicCredentials(AuthMethod.class);
        assertEquals("Wrong auth method.", AuthMethod.CERT,
                     authMethods.toArray(new AuthMethod[authMethods.size()])[0]);

        verify(mockPrincipalExtractor, mockCertChain, cookie);
    }

    @Test
    public void testGetSubjectFromPrincipalExtractorWithCookie()
            throws Exception
    {
        final PrincipalExtractor mockPrincipalExtractor =
                createMock(PrincipalExtractor.class);
        final Set<Principal> principalSet = new HashSet<Principal>();
        // this needs to be a list of mocks.
        List<SSOCookieCredential> mockCookieCredentials = new ArrayList<>();
        final SSOCookieCredential cookie = createMock(SSOCookieCredential.class);
        mockCookieCredentials.add(cookie);

        // To overcome the empty principals.
        principalSet.add(new HttpPrincipal("USER1"));

        expect(mockPrincipalExtractor.getPrincipals()).andReturn(
                principalSet).once();

        expect(mockPrincipalExtractor.getCertificateChain()).andReturn(
                null).once();

        expect(mockPrincipalExtractor.getDelegationToken()).andReturn(
                null).once();

        expect(mockPrincipalExtractor.getSSOCookieCredentials()).andReturn(
                mockCookieCredentials).anyTimes();

        replay(mockPrincipalExtractor, cookie);

        final Subject s = AuthenticationUtil.getSubject(mockPrincipalExtractor);

        final Set<AuthMethod> authMethods =
                s.getPublicCredentials(AuthMethod.class);
        assertEquals("Wrong auth method.", AuthMethod.COOKIE,
                     authMethods.toArray(
                             new AuthMethod[authMethods.size()])[0]);

        verify(mockPrincipalExtractor, cookie);
    }

    @Test
    public void testGetSubjectFromPrincipalExtractorAnon() throws Exception
    {
        final PrincipalExtractor mockPrincipalExtractor =
                createMock(PrincipalExtractor.class);
        final Set<Principal> principalSet = new HashSet<Principal>();

        expect(mockPrincipalExtractor.getPrincipals()).andReturn(
                principalSet).once();

        expect(mockPrincipalExtractor.getCertificateChain()).andReturn(
                null).once();

        expect(mockPrincipalExtractor.getDelegationToken()).andReturn(
                null).once();

        expect(mockPrincipalExtractor.getSSOCookieCredentials()).andReturn(
                null).once();

        replay(mockPrincipalExtractor);

        final Subject s = AuthenticationUtil.getSubject(mockPrincipalExtractor);

        final Set<AuthMethod> authMethods =
                s.getPublicCredentials(AuthMethod.class);
        assertEquals("Wrong auth method.", AuthMethod.ANON,
                     authMethods.toArray(
                             new AuthMethod[authMethods.size()])[0]);

        verify(mockPrincipalExtractor);
    }

    //@Test
    public void testGetSubjectFromHttpServletRequest_X500Principal()
    {
        log.debug("testGetSubjectFromHttpServletRequest_X500Principal - START");
        try
        {
            final HttpServletRequest mockRequest =
                createMock(HttpServletRequest.class);

            String dn = "";

            Calendar notAfterCal = Calendar.getInstance();
            notAfterCal.set(1977, Calendar.NOVEMBER, 25, 3, 21, 0);
            notAfterCal.set(Calendar.MILLISECOND, 0);
            Date notAfterDate = notAfterCal.getTime();

            X500Principal subjectX500Principal = new X500Principal("CN=CN1,O=O1");
            X500Principal issuerX500Principal = new X500Principal("CN=CN2,O=O2");
            X509Certificate mockCertificate = createMock(X509Certificate.class);
            X509Certificate[] ca = new X509Certificate[] { mockCertificate };

            expect(mockRequest.getRemoteUser()).andReturn(null).atLeastOnce();
            expect(mockRequest.getCookies()).andReturn(null).atLeastOnce();
            expect(mockRequest.getHeader(AuthenticationUtil.AUTH_HEADER)).andReturn(null).atLeastOnce();
            expect(mockRequest.getAttribute(
                    "javax.servlet.request.X509Certificate")).andReturn(ca).atLeastOnce();
            expect(mockCertificate.getNotAfter()).andReturn(notAfterDate).once();
            expect(mockCertificate.getSubjectX500Principal()).
                andReturn(subjectX500Principal).once();
            expect(mockCertificate.getIssuerX500Principal()).andReturn(
                issuerX500Principal).once();

            replay(mockRequest);
            final Subject subject1 = AuthenticationUtil.getSubject(mockRequest);

            assertEquals(1, subject1.getPrincipals().size());
            AuthMethod am = AuthenticationUtil.getAuthMethod(subject1);
            assertEquals(AuthMethod.PASSWORD, am);
            Principal p = null;
            for (Principal tmp : subject1.getPrincipals())
            {
                if (tmp instanceof X500Principal)
                {
                    p = tmp;
                    break;
                }
            }
            assertNotNull(p);
            assertTrue(p instanceof X500Principal);
            X500Principal xp = (X500Principal) p;
            assertEquals(subjectX500Principal, xp);

            verify(mockRequest);
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            fail("unexpected exception: " + unexpected);
        }
        finally
        {
            log.debug("testGetSubjectFromHttpServletRequest_X500Principal - DONE");
        }
    }

    @Test
    public void getCurrentSubject() throws Exception
    {
        final Subject subject = new Subject();
        final HttpPrincipal principal = new HttpPrincipal("CADCtest");

        subject.getPrincipals().add(principal);

        Subject.doAs(subject, new PrivilegedAction<Void>()
        {
            @Override
            public Void run()
            {
                final Subject currentSubject =
                        AuthenticationUtil.getCurrentSubject();

                assertEquals("Wrong Subject found.", "CADCtest",
                             currentSubject.getPrincipals(
                                     HttpPrincipal.class).toArray(
                                     new HttpPrincipal[1])[0].getName());

                return null;
            }
        });
    }

    @Test
    @Ignore("In development")
    public void testGetSubjectFromHttpServletRequest_CookiePrincipal() throws Exception
    {
        log.debug("testGetSubjectFromHttpServletRequest_CookiePrincipal - START");

        try
        {
            final Cookie[] cookies = new Cookie[]
            {
                    new Cookie("SOMECOOKIE", "SOMEVALUE"),
                    new Cookie(SSOCookieManager.DEFAULT_SSO_COOKIE_NAME, "AAABBB")
            };
            final HttpServletRequest mockRequest = createMock(HttpServletRequest.class);

            expect(mockRequest.getRemoteUser()).andReturn(null).atLeastOnce();
            expect(mockRequest.getCookies()).andReturn(cookies).atLeastOnce();
            expect(mockRequest.getHeader(AuthenticationUtil.AUTH_HEADER)).andReturn(null).atLeastOnce();
            expect(mockRequest.getAttribute(
                    "javax.servlet.request.X509Certificate")).andReturn(null).atLeastOnce();

            replay(mockRequest);
            final Subject subject1 = AuthenticationUtil.getSubject(mockRequest);

            assertEquals(1, subject1.getPrincipals().size());
            AuthMethod am = AuthenticationUtil.getAuthMethod(subject1);
            assertEquals(AuthMethod.COOKIE, am);
            Principal p = null;
            for (Principal tmp : subject1.getPrincipals())
            {
                if (tmp instanceof CookiePrincipal)
                {
                    p = tmp;
                    break;
                }
            }
            assertNotNull(p);
            assertTrue(p instanceof CookiePrincipal);
            CookiePrincipal cp = (CookiePrincipal) p;
            assertEquals("AAABBB", new String(cp.getSessionId()));

            verify(mockRequest);
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            fail("unexpected exception: " + unexpected);
        }
        finally
        {
            log.debug("testGetSubjectFromHttpServletRequest_CookiePrincipal - DONE");
        }
    }

    //@Test
    public void testGetSubjectCertKey()
    {
        try
        {
            X509Certificate[] certs = null;
            PrivateKey pk = null;

            // TODO: add some valid input
            Subject s;

            s = AuthenticationUtil.getSubject(certs, pk);

             // TODO: verify output
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            fail("unexpected exception: " + unexpected);
        }
        finally
        {
            log.debug("testGetSubjectCertKey - DONE");
        }
    }

    @Test
    public void groupByPrincipalType() throws Exception
    {
        final Subject subject = new Subject();
        final HttpPrincipal p1 = new HttpPrincipal("CADCtest");
        final HttpPrincipal p2 = new HttpPrincipal("USER1");
        final X500Principal p4 =
                new X500Principal("cn=cadctest_636,ou=cadc,o=hia,c=ca");

        subject.getPrincipals().add(p1);
        subject.getPrincipals().add(p2);
        subject.getPrincipals().add(p4);

        Subject.doAs(subject, new PrivilegedAction<Void>()
        {
            @Override
            public Void run()
            {
                final Map<Class<Principal>, Collection<String>> groups =
                        AuthenticationUtil.groupPrincipalsByType();

                assertEquals("Should have two HttpPrincipals.", 2,
                             groups.get(HttpPrincipal.class).size());
                assertEquals("Should have one X500Principals.", 1,
                             groups.get(X500Principal.class).size());

                return null;
            }
        });
    }

    @Test
    public void testGetOrderedForm()
    {
        try
        {
            String name = "CN=Mr Toad, OU=toad.hall.com, O=Grid, C=CA"; // java normal order
            String lname = name.toLowerCase();
            String rname = "  C=CA, O=Grid, OU=toad.hall.com, CN=Mr Toad  "; // openssl normal order

            X500Principal x1 = new X500Principal(name);
            X500Principal x2 = new X500Principal(lname);
            X500Principal x3 = new X500Principal(rname);

            log.debug("x1: " + AuthenticationUtil.canonizeDistinguishedName(name));
            log.debug("x2: " + AuthenticationUtil.canonizeDistinguishedName(lname));
            log.debug("x3: " + AuthenticationUtil.canonizeDistinguishedName(rname));

            assertEquals("forward==ordered(reverse)", x1, AuthenticationUtil.getOrderedForm(x3));

            assertTrue("canon lower==reverse", AuthenticationUtil.equals(x2, x3));
            assertNotSame("lower==reverse", x2, x3);
            assertEquals("lower==ordered(reverse)", x2, AuthenticationUtil.getOrderedForm(x3));
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void getGetAuthMethods()
    {
        try
        {
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testGetAuthMethodForPrincipal()
    {
        try
        {
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            fail("unexpected exception: " + unexpected);
        }
    }

}
