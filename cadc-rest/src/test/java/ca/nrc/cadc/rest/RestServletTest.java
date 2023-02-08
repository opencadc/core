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
import ca.nrc.cadc.auth.AuthorizationToken;
import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.auth.IdentityManager;
import ca.nrc.cadc.auth.NoOpIdentityManager;
import ca.nrc.cadc.auth.NotAuthenticatedException;
import ca.nrc.cadc.auth.NotAuthenticatedException.AuthError;
import ca.nrc.cadc.reg.AccessURL;
import ca.nrc.cadc.reg.Capabilities;
import ca.nrc.cadc.reg.Capability;
import ca.nrc.cadc.reg.Interface;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.util.Log4jInit;
import ca.nrc.cadc.util.PropertiesReader;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;

import java.util.Set;
import javax.security.auth.Subject;

import javax.security.auth.x500.X500Principal;
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
        Log4jInit.setLevel("ca.nrc.cadc.rest", Level.DEBUG);
    }

    @Test
    public void testSetAuthenticateHeaders() {
        log.info("TEST: testSetAuthenticateHeaders");

        try {
            
            System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "src/test/resources");
            System.setProperty(IdentityManager.class.getName(), TestIdentityManager.class.getName());
            
            SyncOutput out = EasyMock.createMock(SyncOutput.class);
            
            // Authenticated scenarios "x-vo-authenticated"
            
            // "<http userid>"
            Subject s = new Subject();
            s.getPrincipals().add(new HttpPrincipal("userid"));
            s.getPublicCredentials().add(new AuthorizationToken("type", "creds", new ArrayList<String>()));
            s.getPublicCredentials().add(AuthMethod.PASSWORD);
            out.addHeader("x-vo-authenticated", "userid");
            runTest(s, out, null);
            
            // "<other principal name if userid missing>"
            s = new Subject();
            s.getPrincipals().add(new X500Principal("C=ca,O=peole,CN=me"));
            s.getPublicCredentials().add(new AuthorizationToken("type", "creds", new ArrayList<String>()));
            s.getPublicCredentials().add(AuthMethod.TOKEN);
            out.addHeader("x-vo-authenticated", "C=ca,O=peole,CN=me");
            EasyMock.expectLastCall().once();
            runTest(s, out, null);
            
            // "<http userid if multiple>"
            s = new Subject();
            s.getPrincipals().add(new X500Principal("C=ca,O=peole,CN=me"));
            s.getPrincipals().add(new HttpPrincipal("userid"));
            s.getPublicCredentials().add(new AuthorizationToken("type", "creds", new ArrayList<String>()));
            s.getPublicCredentials().add(AuthMethod.TOKEN);
            out.addHeader("x-vo-authenticated", "userid");
            EasyMock.expectLastCall().once();
            runTest(s, out, null);
            
            // Not authenticated scenarios "WWW-Authenticate"
            
            // no errors, all headers
            s = new Subject();
            out.addHeader("WWW-Authenticate", "ivoa_bearer standard_id=\"ivo://ivoa.net/sso#tls-with-password\", access_url=\"https://example.com/ac/login\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_bearer standard_id=\"ivo://ivoa.net/sso#OpenID\", access_url=\"https://oidc.example.net/\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_x509 standard_id=\"ivo://ivoa.net/sso#BasicAA\", access_url=\"https://example.com/cred/priv/basic\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_x509 standard_id=\"ivo://ivoa.net/sso#tls-with-password\", access_url=\"https://example.com/cred/priv/pass\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "Bearer");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_x509");
            EasyMock.expectLastCall().once();
            runTest(s, out, null);
            
            // ivoa token error no description
            s = new Subject();
            out.addHeader("WWW-Authenticate", "ivoa_bearer standard_id=\"ivo://ivoa.net/sso#tls-with-password\", access_url=\"https://example.com/ac/login\", error=\"insufficient_scope\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_bearer standard_id=\"ivo://ivoa.net/sso#OpenID\", access_url=\"https://oidc.example.net/\", error=\"insufficient_scope\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_x509 standard_id=\"ivo://ivoa.net/sso#BasicAA\", access_url=\"https://example.com/cred/priv/basic\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_x509 standard_id=\"ivo://ivoa.net/sso#tls-with-password\", access_url=\"https://example.com/cred/priv/pass\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "Bearer");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_x509");
            EasyMock.expectLastCall().once();
            NotAuthenticatedException ex = new NotAuthenticatedException("ivoa_bearer", AuthError.INSUFFICIENT_SCOPE, null);
            runTest(s, out, ex);
            
            // ivoa token error with description
            s = new Subject();
            out.addHeader("WWW-Authenticate", "ivoa_bearer standard_id=\"ivo://ivoa.net/sso#tls-with-password\", access_url=\"https://example.com/ac/login\", error=\"insufficient_scope\", error_description=\"text\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_bearer standard_id=\"ivo://ivoa.net/sso#OpenID\", access_url=\"https://oidc.example.net/\", error=\"insufficient_scope\", error_description=\"text\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_x509 standard_id=\"ivo://ivoa.net/sso#BasicAA\", access_url=\"https://example.com/cred/priv/basic\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_x509 standard_id=\"ivo://ivoa.net/sso#tls-with-password\", access_url=\"https://example.com/cred/priv/pass\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "Bearer");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_x509");
            EasyMock.expectLastCall().once();
            ex = new NotAuthenticatedException("ivoa_bearer", AuthError.INSUFFICIENT_SCOPE, "text");
            runTest(s, out, ex);
            
            // bearer token error no description
            s = new Subject();
            out.addHeader("WWW-Authenticate", "ivoa_bearer standard_id=\"ivo://ivoa.net/sso#tls-with-password\", access_url=\"https://example.com/ac/login\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_bearer standard_id=\"ivo://ivoa.net/sso#OpenID\", access_url=\"https://oidc.example.net/\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_x509 standard_id=\"ivo://ivoa.net/sso#BasicAA\", access_url=\"https://example.com/cred/priv/basic\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_x509 standard_id=\"ivo://ivoa.net/sso#tls-with-password\", access_url=\"https://example.com/cred/priv/pass\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "Bearer error=\"insufficient_scope\"");
            EasyMock.expectLastCall().once();
            ex = new NotAuthenticatedException("Bearer", AuthError.INSUFFICIENT_SCOPE, null);
            out.addHeader("WWW-Authenticate", "ivoa_x509");
            EasyMock.expectLastCall().once();
            runTest(s, out, ex);
            
            // bearer token error with description
            s = new Subject();
            out.addHeader("WWW-Authenticate", "ivoa_bearer standard_id=\"ivo://ivoa.net/sso#tls-with-password\", access_url=\"https://example.com/ac/login\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_bearer standard_id=\"ivo://ivoa.net/sso#OpenID\", access_url=\"https://oidc.example.net/\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_x509 standard_id=\"ivo://ivoa.net/sso#BasicAA\", access_url=\"https://example.com/cred/priv/basic\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "ivoa_x509 standard_id=\"ivo://ivoa.net/sso#tls-with-password\", access_url=\"https://example.com/cred/priv/pass\"");
            EasyMock.expectLastCall().once();
            out.addHeader("WWW-Authenticate", "Bearer error=\"insufficient_scope\", error_description=\"text\"");
            EasyMock.expectLastCall().once();
            ex = new NotAuthenticatedException("Bearer", AuthError.INSUFFICIENT_SCOPE, "text");
            out.addHeader("WWW-Authenticate", "ivoa_x509");
            EasyMock.expectLastCall().once();
            runTest(s, out, ex);

        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        } finally {
            System.clearProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY);
        }
    }
    
    private void runTest(Subject s, SyncOutput mockOut, NotAuthenticatedException ex) {
        EasyMock.expect(mockOut.isOpen()).andReturn(Boolean.FALSE);
        EasyMock.replay(mockOut);
        RestServlet.setAuthenticateHeaders(s, mockOut, ex, new TestRegistryClient());
        EasyMock.verify(mockOut);
        EasyMock.reset(mockOut);
    }
    
    public class TestRegistryClient extends RegistryClient {
        @Override
        public URL getServiceURL(URI serviceID, URI securityMethod, AuthMethod authMethod) {
            try {
                if (securityMethod.equals(Standards.SECURITY_METHOD_PASSWORD)) {
                    return new URL("https://example.com/ac/login");
                } else { 
                    return new URL("https://example.com/ac");
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        
        @Override
        public Capabilities getCapabilities(URI resourceID) throws MalformedURLException {
            Capabilities caps = new Capabilities();
            Capability cap = new Capability(Standards.CRED_PROXY_10);
            Interface ifc = new Interface(Standards.INTERFACE_PARAM_HTTP, new AccessURL(new URL("https://example.com/cred/priv/basic")));
            ifc.getSecurityMethods().add(Standards.SECURITY_METHOD_HTTP_BASIC);
            Interface ifc2 = new Interface(Standards.INTERFACE_PARAM_HTTP, new AccessURL(new URL("https://example.com/cred/priv/pass")));
            ifc2.getSecurityMethods().add(Standards.SECURITY_METHOD_PASSWORD);
            Interface ifc3 = new Interface(Standards.INTERFACE_PARAM_HTTP, new AccessURL(new URL("https://example.com/cred/priv/cookie")));
            ifc2.getSecurityMethods().add(Standards.SECURITY_METHOD_COOKIE);
            cap.getInterfaces().add(ifc);
            cap.getInterfaces().add(ifc2);
            cap.getInterfaces().add(ifc3);
            caps.getCapabilities().add(cap);
            return caps;
        }
        
    }

    public static class TestIdentityManager extends NoOpIdentityManager {

        public TestIdentityManager() {
            super();
        }
        
        @Override
        public String toDisplayString(Subject subject) {
            // x-vo-authenticated
            if (subject != null) {
                // prefer username
                Set<HttpPrincipal> ps = subject.getPrincipals(HttpPrincipal.class);
                if (!ps.isEmpty()) {
                    return ps.iterator().next().getName();
                }
                // default to first
                Set<Principal> ps2 = subject.getPrincipals();
                if (!ps2.isEmpty()) {
                    return ps2.iterator().next().getName();
                }
            }
            return null;
        }
    }
}
