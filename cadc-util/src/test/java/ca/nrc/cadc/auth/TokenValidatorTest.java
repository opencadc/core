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
 ************************************************************************
 */
package ca.nrc.cadc.auth;

import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;
import ca.nrc.cadc.util.RsaSignatureGenerator;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.security.auth.Subject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author majorb
 *
 */
public class TokenValidatorTest {
    
    private static Logger log = Logger.getLogger(TokenValidatorTest.class);
    
    static {
        Log4jInit.setLevel("ca.nrc.cadc.auth", Level.DEBUG);
    }

    @Test
    public void testValidateTokens() {
        
        File pubFile = null;
        File privFile = null;

        try {
            
            // init keys
            File config = FileUtil.getFileFromResource("DelegationToken.properties", TokenValidatorTest.class);
            File keysDir = config.getParentFile();
            RsaSignatureGenerator.genKeyPair(keysDir);
            privFile = new File(keysDir, RsaSignatureGenerator.PRIV_KEY_FILE_NAME);
            pubFile = new File(keysDir, RsaSignatureGenerator.PUB_KEY_FILE_NAME);
            
            Date expiry = new Date(new Date().getTime() + (48 * 3600 * 1000));
            List<String> domains = Arrays.asList(new String[] {"cadc.org"});
            
            // test no credentials
            Subject subject = new Subject();
            subject = TokenValidator.validateTokens(subject);
            Assert.assertEquals("no credentials", 0, subject.getPublicCredentials().size());
            
            // test cookies
            subject = new Subject();
            SignedToken token = new SignedToken(new HttpPrincipal("user"), null, expiry, domains);
            String value = SignedToken.format(token);
            CookiePrincipal cookiePrincipal = new CookiePrincipal("key", value);
            subject.getPrincipals().add(cookiePrincipal);
            subject = TokenValidator.validateTokens(subject);
            Assert.assertEquals("cookie credential", 1, subject.getPublicCredentials(SSOCookieCredential.class).size());
            Assert.assertEquals("cookie principal", 0, subject.getPrincipals(AuthorizationTokenPrincipal.class).size());
            
            // test cadc deprecated tokens
            subject = new Subject();
            token = new SignedToken(new HttpPrincipal("user"), null, expiry, domains);
            value = SignedToken.format(token);
            AuthorizationTokenPrincipal authPrincipal = new AuthorizationTokenPrincipal(AuthenticationUtil.TOKEN_TYPE_CADC, value);
            subject.getPrincipals().add(authPrincipal);
            subject = TokenValidator.validateTokens(subject);
            Assert.assertEquals("token credential", 1, subject.getPublicCredentials(AuthorizationToken.class).size());
            AuthorizationToken authToken = subject.getPublicCredentials(AuthorizationToken.class).iterator().next();
            Assert.assertEquals("cadc token type", AuthenticationUtil.TOKEN_TYPE_CADC, authToken.getType());
            Assert.assertEquals("cadc token value", value, authToken.getCredentials());
            
            // bearer tokens
            subject = new Subject();
            token = new SignedToken(new HttpPrincipal("user"), URI.create("the:scope"), expiry, domains);
            value = SignedToken.format(token);
            authPrincipal = new AuthorizationTokenPrincipal(AuthenticationUtil.AUTHORIZATION_HEADER, "Bearer " + value);
            subject.getPrincipals().add(authPrincipal);
            subject = TokenValidator.validateTokens(subject);
            Assert.assertEquals("token credential", 1, subject.getPublicCredentials(AuthorizationToken.class).size());
            authToken = subject.getPublicCredentials(AuthorizationToken.class).iterator().next();
            Assert.assertEquals("bearer token type", AuthenticationUtil.CHALLENGE_TYPE_BEARER, authToken.getType());
            Assert.assertEquals("bearer token value", value, authToken.getCredentials());
            Assert.assertEquals("bearer token scope", "the:scope", authToken.getScope().toString());
            Assert.assertEquals("token principal", 0, subject.getPrincipals(AuthorizationTokenPrincipal.class).size());
            
            // ivoa tokens
            subject = new Subject();
            token = new SignedToken(new HttpPrincipal("user"), URI.create("the:scope"), expiry, domains);
            value = SignedToken.format(token);
            authPrincipal = new AuthorizationTokenPrincipal(AuthenticationUtil.AUTHORIZATION_HEADER, "ivoa " + value);
            subject.getPrincipals().add(authPrincipal);
            subject = TokenValidator.validateTokens(subject);
            Assert.assertEquals("token credential", 1, subject.getPublicCredentials(AuthorizationToken.class).size());
            authToken = subject.getPublicCredentials(AuthorizationToken.class).iterator().next();
            Assert.assertEquals("ivoa token type", AuthenticationUtil.CHALLENGE_TYPE_IVOA, authToken.getType());
            Assert.assertEquals("ivoa token value", value, authToken.getCredentials());
            Assert.assertEquals("ivoa token scope", "the:scope", authToken.getScope().toString());
            
            // invalid bearer token
            subject = new Subject();
            authPrincipal = new AuthorizationTokenPrincipal(AuthenticationUtil.AUTHORIZATION_HEADER, "Bearer tampered");
            subject.getPrincipals().add(authPrincipal);
            try {
                subject = TokenValidator.validateTokens(subject);
                Assert.fail("Should have received NotAuthenticatedException");
            } catch (NotAuthenticatedException e) {
                Assert.assertEquals(AuthenticationUtil.CHALLENGE_TYPE_BEARER, e.getChallenge());
                Assert.assertEquals("invalid_token", e.getAuthError().getValue());
            }
            
            // invalid ivoa token
            subject = new Subject();
            authPrincipal = new AuthorizationTokenPrincipal(AuthenticationUtil.AUTHORIZATION_HEADER, "ivoa tampered");
            subject.getPrincipals().add(authPrincipal);
            try {
                subject = TokenValidator.validateTokens(subject);
                Assert.fail("Should have received NotAuthenticatedException");
            } catch (NotAuthenticatedException e) {
                Assert.assertEquals(AuthenticationUtil.CHALLENGE_TYPE_IVOA, e.getChallenge());
                Assert.assertEquals("invalid_token", e.getAuthError().getValue());
            }
            
            // unsupported challenge type
            subject = new Subject();
            token = new SignedToken(new HttpPrincipal("user"), null, expiry, domains);
            value = SignedToken.format(token);
            authPrincipal = new AuthorizationTokenPrincipal(AuthenticationUtil.AUTHORIZATION_HEADER, "Foo " + value);
            subject.getPrincipals().add(authPrincipal);
            try {
                subject = TokenValidator.validateTokens(subject);
                Assert.fail("Should have received NotAuthenticatedException");
            } catch (NotAuthenticatedException e) {
                Assert.assertEquals("Foo", e.getChallenge());
                Assert.assertEquals("invalid_request", e.getAuthError().getValue());
            }
            
        } catch (Throwable t) {
            log.error("unexpected", t);
            Assert.fail("unexpected: " + t.getMessage());
        } finally {
            try {
                pubFile.delete();
                privFile.delete();
            } catch (Throwable t) {
                log.error("failed to cleanup keys", t);
            }
        }
    }
    
}
