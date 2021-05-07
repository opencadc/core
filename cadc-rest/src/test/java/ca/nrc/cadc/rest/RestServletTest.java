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

package ca.nrc.cadc.rest;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.AuthorizationToken;
import ca.nrc.cadc.auth.AuthorizationTokenPrincipal;
import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.auth.NotAuthenticatedException;
import ca.nrc.cadc.auth.NotAuthenticatedException.AuthError;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.LocalAuthority;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.util.Log4jInit;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author majorb
 *
 */
public class RestServletTest {
    
    private static Logger log = Logger.getLogger(RestServletTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.rest", Level.INFO);
    }

    @Test
    public void testSetAuthenticateHeaders() {
        log.info("TEST: testSetAuthenticateHeaders");

        try {

            SyncOutput out = EasyMock.createMock(SyncOutput.class);
            
            // Authenticated scenarios "x-vo-authenticated"
            
            // "<http userid>"
            Subject s = new Subject();
            s.getPrincipals().add(new HttpPrincipal("userid"));
            s.getPublicCredentials().add(new AuthorizationToken("type", "creds", new ArrayList<String>()));
            out.addHeader("x-vo-authenticated", "userid");
            runTest(s, out, null);
            
            // "<other principal name if userid missing>"
            s = new Subject();
            s.getPrincipals().add(new AuthorizationTokenPrincipal("some-value"));
            s.getPublicCredentials().add(new AuthorizationToken("type", "creds", new ArrayList<String>()));
            out.addHeader("x-vo-authenticated", "some-value");
            EasyMock.expectLastCall().once();
            runTest(s, out, null);
            
            // "<http userid if multiple>"
            s = new Subject();
            s.getPrincipals().add(new AuthorizationTokenPrincipal("some-value"));
            s.getPrincipals().add(new HttpPrincipal("userid"));
            s.getPublicCredentials().add(new AuthorizationToken("type", "creds", new ArrayList<String>()));
            out.addHeader("x-vo-authenticated", "userid");
            EasyMock.expectLastCall().once();
            runTest(s, out, null);
            
            // Not authenticated scenarios "WWW-Authenticate"
            
            // no errors, all headers
            s = new Subject();
            out.addHeader("WWW-Authenticate", "ivoa standard_id=\"ivo://ivoa.net/sso#tls-with-password\", access_url=\"https://example.com/ac/login\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa standard_id=\"ivo://ivoa.net/sso#OAuth\", access_url=\"https://example.com/ac/authorize\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa standard_id=\"ivo://ivoa.net/sso#tls-with-certificate\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "Bearer");
            EasyMock.expectLastCall().once();
            runTest(s, out, null);
            
            // ivoa token error no description
            s = new Subject();
            out.addHeader("WWW-Authenticate", "ivoa standard_id=\"ivo://ivoa.net/sso#tls-with-password\", access_url=\"https://example.com/ac/login\", error=\"insufficient_scope\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa standard_id=\"ivo://ivoa.net/sso#OAuth\", access_url=\"https://example.com/ac/authorize\", error=\"insufficient_scope\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa standard_id=\"ivo://ivoa.net/sso#tls-with-certificate\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "Bearer");
            EasyMock.expectLastCall().once();
            NotAuthenticatedException ex = new NotAuthenticatedException("ivoa", AuthError.INSUFFICIENT_SCOPE, null);
            runTest(s, out, ex);
            
            // ivoa token error with description
            s = new Subject();
            out.addHeader("WWW-Authenticate", "ivoa standard_id=\"ivo://ivoa.net/sso#tls-with-password\", access_url=\"https://example.com/ac/login\", error=\"insufficient_scope\", error_description=\"text\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa standard_id=\"ivo://ivoa.net/sso#OAuth\", access_url=\"https://example.com/ac/authorize\", error=\"insufficient_scope\", error_description=\"text\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa standard_id=\"ivo://ivoa.net/sso#tls-with-certificate\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "Bearer");
            EasyMock.expectLastCall().once();
            ex = new NotAuthenticatedException("ivoa", AuthError.INSUFFICIENT_SCOPE, "text");
            runTest(s, out, ex);
            
            // bearer token error no description
            s = new Subject();
            out.addHeader("WWW-Authenticate", "ivoa standard_id=\"ivo://ivoa.net/sso#tls-with-password\", access_url=\"https://example.com/ac/login\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa standard_id=\"ivo://ivoa.net/sso#OAuth\", access_url=\"https://example.com/ac/authorize\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa standard_id=\"ivo://ivoa.net/sso#tls-with-certificate\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "Bearer error=\"insufficient_scope\"");
            EasyMock.expectLastCall().once();
            ex = new NotAuthenticatedException("Bearer", AuthError.INSUFFICIENT_SCOPE, null);
            runTest(s, out, ex);
            
            // bearer token error with description
            s = new Subject();
            out.addHeader("WWW-Authenticate", "ivoa standard_id=\"ivo://ivoa.net/sso#tls-with-password\", access_url=\"https://example.com/ac/login\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa standard_id=\"ivo://ivoa.net/sso#OAuth\", access_url=\"https://example.com/ac/authorize\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa standard_id=\"ivo://ivoa.net/sso#tls-with-certificate\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "Bearer error=\"insufficient_scope\", error_description=\"text\"");
            EasyMock.expectLastCall().once();
            ex = new NotAuthenticatedException("Bearer", AuthError.INSUFFICIENT_SCOPE, "text");
            runTest(s, out, ex);

        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    private void runTest(Subject s, SyncOutput mockOut, NotAuthenticatedException ex) {
        EasyMock.expect(mockOut.isOpen()).andReturn(Boolean.FALSE);
        RestServlet restServlet = new TestRestServlet();
        EasyMock.replay(mockOut);
        restServlet.setAuthenticateHeaders(s, mockOut, ex, null);
        EasyMock.verify(mockOut);
        EasyMock.reset(mockOut);
    }
    
    public class TestRestServlet extends RestServlet {
       @Override
       RegistryClient getRegistryClient() {
           return new TestRegistryClient();
       }
       @Override
       URI getLocalServiceURI(URI stdID) {
           return URI.create("not:used");
       }
    }
    
    public class TestRegistryClient extends RegistryClient {
        @Override
        public URL getServiceURL(URI serviceID, URI securityMethod, AuthMethod authMethod) {
            try {
                if (securityMethod.equals(Standards.SECURITY_METHOD_PASSWORD)) {
                    return new URL("https://example.com/ac/login");
                } else {
                    return new URL("https://example.com/ac/authorize");
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        
    }

}
