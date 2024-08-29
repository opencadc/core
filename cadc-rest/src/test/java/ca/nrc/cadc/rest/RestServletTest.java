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
import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.auth.IdentityManager;
import ca.nrc.cadc.auth.NoOpIdentityManager;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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
    
    static final String REALM = "opencadc.org";

    private class Output extends SyncOutput {

        Map<String,List<String>> headers = new TreeMap<>();
        
        Output() {
        }
        
        @Override
        public void addHeader(String key, Object value) {
            List<String> vals = headers.get(key);
            if (vals == null) {
                vals = new ArrayList<>();
                headers.put(key, vals);
            }
            vals.add((String) value);
        }

        @Override
        public void setHeader(String key, Object value) {
            List<String> vals = headers.get(key);
            if (vals == null) {
                vals = new ArrayList<>();
                headers.put(key, vals);
            }
            vals.clear();
            vals.add((String) value);
        }
    }
    
    @Test
    public void testSetAuthenticated() {

        try {
            System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "src/test/resources");
            System.setProperty(IdentityManager.class.getName(), TestIdentityManager.class.getName());
            
            Output out;
            
            // username
            Subject s = new Subject();
            s.getPrincipals().add(new HttpPrincipal("userid"));
            out = new Output();
            RestServlet.setAuthenticateHeaders(s, REALM, out, null);
            // verify
            List<String> xvoauth = out.headers.get(AuthenticationUtil.VO_AUTHENTICATED_HEADER);
            Assert.assertNotNull(xvoauth);
            Assert.assertEquals(1, xvoauth.size());
            String actual = xvoauth.get(0);
            Assert.assertEquals("userid", actual);
            List<String> wwwauth = out.headers.get(AuthenticationUtil.AUTHENTICATE_HEADER);
            Assert.assertNull(wwwauth);
            
            // x509 only
            s = new Subject();
            s.getPrincipals().add(new X500Principal("C=ca,O=people,CN=me"));
            out = new Output();
            RestServlet.setAuthenticateHeaders(s, REALM, out, null);
            // verify
            xvoauth = out.headers.get(AuthenticationUtil.VO_AUTHENTICATED_HEADER);
            Assert.assertNotNull(xvoauth);
            Assert.assertEquals(1, xvoauth.size());
            actual = xvoauth.get(0);
            Assert.assertEquals("C=ca,O=people,CN=me", actual);
            wwwauth = out.headers.get(AuthenticationUtil.AUTHENTICATE_HEADER);
            Assert.assertNull(wwwauth);
            
            // multiple principals (eg after augment)
            s = new Subject();
            s.getPrincipals().add(new X500Principal("C=ca,O=people,CN=me"));
            s.getPrincipals().add(new HttpPrincipal("userid"));
            out = new Output();
            RestServlet.setAuthenticateHeaders(s, REALM, out, null);
            // verify
            xvoauth = out.headers.get(AuthenticationUtil.VO_AUTHENTICATED_HEADER);
            Assert.assertNotNull(xvoauth);
            Assert.assertEquals(1, xvoauth.size());
            actual = xvoauth.get(0);
            Assert.assertEquals("userid", actual);
            wwwauth = out.headers.get(AuthenticationUtil.AUTHENTICATE_HEADER);
            Assert.assertNull(wwwauth);
            
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        } finally {
            System.clearProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY);
        }
    }
    
    @Test
    public void testSetChallenges() {

        try {
            System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "src/test/resources");
            System.setProperty(IdentityManager.class.getName(), TestIdentityManager.class.getName());
            
            Output out;
            
            // username
            Subject s = AuthenticationUtil.getAnonSubject();
            out = new Output();
            RestServlet.setAuthenticateHeaders(s, REALM, out, null);
            // verify
            List<String> xvoauth = out.headers.get(AuthenticationUtil.VO_AUTHENTICATED_HEADER);
            Assert.assertNull(xvoauth);
            
            List<String> wwwauth = out.headers.get(AuthenticationUtil.AUTHENTICATE_HEADER);
            Assert.assertNotNull(wwwauth);
            int foundBearer = 0;
            int foundIvoaBearer = 0;
            int foundIvoaX509 = 0;
            for (String cs : wwwauth) {
                log.info("found challenge: " + AuthenticationUtil.AUTHENTICATE_HEADER + ": " + cs);
                String csLower = cs.toLowerCase();
                if (csLower.equals("bearer")) {
                    foundBearer++;
                } else if (csLower.startsWith("ivoa_bearer standard_id=")) {
                    foundIvoaBearer++;
                } else if (csLower.startsWith("ivoa_x509")) {
                    foundIvoaX509++;
                }
            }
            // expectation based on cadc-registry.properties and TestRegistryClient capability output below
            Assert.assertEquals("bearer", 1, foundBearer);
            Assert.assertEquals("ivoa_bearer", 1, foundIvoaBearer); // unable to configure for this hack
            Assert.assertEquals("ivoa_x509", 1, foundIvoaX509);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        } finally {
            System.clearProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY);
        }
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
        public Set<URI> getSecurityMethods() {
            Set<URI> ret = new TreeSet<>();
            ret.add(Standards.SECURITY_METHOD_CERT);
            ret.add(Standards.SECURITY_METHOD_OPENID);
            return ret;
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
