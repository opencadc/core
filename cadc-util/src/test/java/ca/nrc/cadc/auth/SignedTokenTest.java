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

package ca.nrc.cadc.auth;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ca.nrc.cadc.util.Base64;
import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;
import ca.nrc.cadc.util.RsaSignatureGenerator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.security.auth.x500.X500Principal;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class SignedTokenTest {
    private static final Logger log = Logger.getLogger(SignedTokenTest.class);

    private static final char[] ILLEGAL_COOKIE_CHARACTERS = new char[] {',', ';', '\\', '"'};

    static {
        Log4jInit.setLevel("ca.nrc.cadc", Level.DEBUG);
    }

    File pubFile, privFile;
    URI scope = URI.create("foo:bar");

    @Before
    public void initKeys() throws Exception {
        File config = FileUtil.getFileFromResource("DelegationToken.properties", SignedTokenTest.class);
        File keysDir = config.getParentFile();
        RsaSignatureGenerator.genKeyPair(keysDir);
        privFile = new File(keysDir, RsaSignatureGenerator.PRIV_KEY_FILE_NAME);
        pubFile = new File(keysDir, RsaSignatureGenerator.PUB_KEY_FILE_NAME);
    }

    @After
    public void cleanupKeys() {
        pubFile.delete();
        privFile.delete();
    }

    @Test
    public void matchDecode() throws Exception {

        // round trip test with signature

        final List<String> domainList = new ArrayList<>();
        domainList.add("www.canfar.phys.uvic.ca");
        domainList.add("www.cadc.hia.nrc.gc.ca");
        domainList.add("www.ccda.iha.cnrc.gc.ca");
        domainList.add("www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca");

        final HttpPrincipal httpPrincipal = new HttpPrincipal("someuser");
        final URI scope = new URI("foo:bar");
        final int durationHours = 10; // h
        Calendar expiry = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        expiry.add(Calendar.HOUR, durationHours);

        StringBuilder tokenValue = new StringBuilder();

        tokenValue.append(SignedToken.EXPIRY_LABEL);
        tokenValue.append("=");
        tokenValue.append(expiry.getTime().getTime());
        tokenValue.append(SignedToken.FIELD_DELIM);
        tokenValue.append(IdentityType.USERID.getValue());
        tokenValue.append("=");
        tokenValue.append(httpPrincipal.getName());
        tokenValue.append(SignedToken.FIELD_DELIM);
        tokenValue.append("scope=");
        tokenValue.append(scope.toString());

        for (final String domain : domainList) {
            tokenValue.append(SignedToken.FIELD_DELIM).append("domain=").append(domain);
        }

        final RsaSignatureGenerator su = new RsaSignatureGenerator();
        final byte[] sig = su.sign(new ByteArrayInputStream(tokenValue.toString().getBytes()));
        tokenValue.append(SignedToken.FIELD_DELIM);
        tokenValue.append(SignedToken.SIGNATURE_LABEL);
        tokenValue.append("=");
        tokenValue.append(new String(Base64.encode(sig)));

        // test with base64: index
        String token = "base64:" + String.valueOf(Base64.encode(tokenValue.toString().getBytes()));

        log.debug("valid token: " + token);
        SignedToken actToken = SignedToken.parse(token);

        assertEquals("User id not the same", httpPrincipal, actToken.getUser());
        assertEquals("Expiry time not the same", expiry.getTime().getTime(),
                     actToken.getExpiryTime().getTime());
        assertEquals("Domain list size not the same", domainList.size(), actToken.getDomains().size());
        assertArrayEquals("Wrong set of domains.", domainList.toArray(new String[0]),
                          actToken.getDomains().toArray(new String[0]));
        
        // test without base64 index
        token = String.valueOf(Base64.encode(tokenValue.toString().getBytes()));

        log.debug("valid token: " + token);
        actToken = SignedToken.parse(token);

        assertEquals("User id not the same", httpPrincipal, actToken.getUser());
        assertEquals("Expiry time not the same", expiry.getTime().getTime(),
                     actToken.getExpiryTime().getTime());
        assertEquals("Domain list size not the same", domainList.size(), actToken.getDomains().size());
        assertArrayEquals("Wrong set of domains.", domainList.toArray(new String[0]),
                          actToken.getDomains().toArray(new String[0]));
        
    }

    @Test
    public void matches() throws Exception {
        // round trip test with signature

        List<String> domainList = new ArrayList<>();
        domainList.add("www.canfar.phys.uvic.ca");
        domainList.add("www.cadc.hia.nrc.gc.ca");
        domainList.add("www.ccda.iha.cnrc.gc.ca");
        domainList.add("www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca");

        HttpPrincipal userid = new HttpPrincipal("someuser");
        int duration = 10; // h
        Calendar expiry = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        expiry.add(Calendar.HOUR, duration);
        SignedToken expToken = new SignedToken(userid, scope, expiry.getTime(), domainList);

        String token = SignedToken.format(expToken);
        log.debug("valid token: " + token);
        SignedToken actToken = SignedToken.parse(token);

        assertEquals("User id not the same", expToken.getUser(),
                     actToken.getUser());
        assertEquals("Expiry time not the same", expToken.getExpiryTime(),
                     actToken.getExpiryTime());
        assertEquals("Domain list size not the same", domainList.size(), expToken.getDomains().size());
        for (String domain : expToken.getDomains()) {
            assertTrue("Domain list content not the same", domainList.contains(domain));
        }


        // round trip test without signature and principal with proxy user
        userid = new HttpPrincipal("someuser", "someproxyuser");
        duration = 10; // h
        expiry = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        expiry.add(Calendar.HOUR, duration);
        expToken = new SignedToken(userid, scope, expiry.getTime(), null);

        token = SignedToken.format(expToken);
        log.debug("valid token: " + token);
        actToken = SignedToken.parse(token);

        assertEquals("User id not the same", expToken.getUser(),
                     actToken.getUser());
        assertEquals("Expiry time not the same", expToken.getExpiryTime(),
                     actToken.getExpiryTime());

        // round trip test without signature and principal with proxy user
        Set<Principal> testPrincipals = new HashSet<>();
        testPrincipals.add(new HttpPrincipal("someuser", "someproxyuser"));
        testPrincipals.add(new X500Principal("CN=JBP,OU=nrc-cnrc.gc.ca,O=grid,C=CA"));

        // CADC identity
        UUID testUUID = UUID.randomUUID();
        testPrincipals.add(new NumericPrincipal(testUUID));

        duration = 10; // h
        expiry = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        expiry.add(Calendar.HOUR, duration);
        expToken = new SignedToken(testPrincipals, scope, expiry.getTime(), null);

        token = SignedToken.format(expToken);
        log.debug("valid token: " + token);
        actToken = SignedToken.parse(token);

        assertEquals("User id not the same", expToken.getUser(),
                     actToken.getUser());
        assertEquals("Expiry time not the same", expToken.getExpiryTime(),
                     actToken.getExpiryTime());
        assertEquals("x509 principal not the same", expToken.getPrincipalByClass(X500Principal.class),
                     actToken.getPrincipalByClass(X500Principal.class));
        assertEquals("Numeric (CADC) principal not the same", expToken.getPrincipalByClass(NumericPrincipal.class),
                     actToken.getPrincipalByClass(NumericPrincipal.class));

        Calendar expiredDate = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        expiredDate.add(Calendar.DATE, -1);
        // parse expired token
        try {
            expToken = new SignedToken(userid, null, expiredDate.getTime(), null);
            SignedToken.parse(SignedToken.format(expToken));
            fail("Exception expected");
        } catch (InvalidSignedTokenException expected) {
            log.debug("caught expected exception: " + expected);
        }

        //invalidate signature field
        try {
            expToken = new SignedToken(userid, scope, expiry.getTime(), null);

            token = SignedToken.format(expToken);
            CharSequence subSequence = token.subSequence(10, token.length());
            SignedToken.parse(subSequence.toString());
            fail("Exception expected");
        } catch (InvalidSignedTokenException expected) {
            log.debug("caught expected exception: " + expected);
        }

        // tamper with one character in the signature
        try {
            expToken = new SignedToken(userid, scope, expiry.getTime(), null);

            token = SignedToken.format(expToken);
            token = token.substring(0, token.length() - 1);
            token = token + "A";

            SignedToken.parse(token);
            fail("Exception expected");
        } catch (InvalidSignedTokenException expected) {
            log.debug("caught expected exception: " + expected);
        }
    }

    @Test
    public void validCookieValue() throws Exception {

        final Principal httpPrincipal = new HttpPrincipal("some;user", "someproxyuser");
        final Principal x509 = new X500Principal("CN=JBP,OU=nrc-cnrc.gc.ca,O=grid,C=CA");

        // The DN should be left out.
        final Principal distinguishedName = new DNPrincipal("uid=88,OU=nrc-cnrc.gc.ca,O=grid,C=CA");

        final Set<Principal> principals = new HashSet<>();

        principals.add(httpPrincipal);
        principals.add(distinguishedName);
        principals.add(x509);

        final int duration = 10; // h
        final Calendar expiry = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        expiry.add(Calendar.HOUR, duration);
        final SignedToken delegationToken = new SignedToken(principals, scope, expiry.getTime(), null);

        final String tokenValue = SignedToken.format(delegationToken);

        for (final char c : ILLEGAL_COOKIE_CHARACTERS) {
            assertFalse(String.format("Token %s invalid characters (%s).",
                                      tokenValue,
                                      Arrays.toString(ILLEGAL_COOKIE_CHARACTERS)),
                        tokenValue.contains(Character.toString(c)));
        }

        final SignedToken parsedDelegationToken = SignedToken.parse(tokenValue);
        final Set<Principal> expectedPrincipals = new HashSet<>();

        expectedPrincipals.add(httpPrincipal);
        expectedPrincipals.add(x509);

        final Set<Principal> parsedPrincipals = parsedDelegationToken.getIdentityPrincipals();

        assertEquals("Wrong principals.", expectedPrincipals, parsedPrincipals);
    }
}

