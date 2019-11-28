/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2019.                            (c) 2019.
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

package ca.nrc.cadc.log;

import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.PrincipalExtractor;
import ca.nrc.cadc.auth.ServletPrincipalExtractor;
import ca.nrc.cadc.util.Log4jInit;
import ca.nrc.cadc.util.PropertiesReader;
import java.security.AccessControlException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.opencadc.gms.GroupURI;

public class LogControlServletTest {
    private static final Logger log = Logger.getLogger(LogControlServletTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.log", Level.INFO);
    }

    @Test
    public void testIsAuthorizedUserNoneConfigured() {
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);

        EasyMock.expect(request.getAttribute(ServletPrincipalExtractor.CERT_REQUEST_ATTRIBUTE))
            .andReturn(new X509Certificate[0]).once();
        EasyMock.expect(request.getHeader(PrincipalExtractor.CERT_HEADER_FIELD))
            .andReturn(null).once();
        EasyMock.expect(request.getHeader(AuthenticationUtil.AUTH_HEADER))
            .andReturn(null).once();
        EasyMock.expect(request.getHeader(AuthenticationUtil.AUTHORIZATION_HEADER))
            .andReturn(null).once();
        EasyMock.expect(request.getRemoteUser()).andReturn(null).once();
        EasyMock.expect(request.getCookies()).andReturn(null).once();

        EasyMock.replay(request);

        LogControlServlet testSubject = new LogControlServlet() {
            @Override
            public void init(final ServletConfig config) throws ServletException {}
        };

        try {
            testSubject.doUserCheck(request, new HashSet<Principal>());
            Assert.fail("Should throw AccessControlException");
        } catch (AccessControlException e) { }
    }

    @Test
    public void testGetAuthorizedUserPrincipals() {
        final String testUser1 = "cn=foo,ou=test,o=example,c=com";
        final String testUser2 = "cn=bar,ou=test,o=example,c=com";

        final PropertiesReader reader = EasyMock.createMock(PropertiesReader.class);

        List<String> authorizedUsers = new ArrayList<>();
        authorizedUsers.add(testUser1);
        authorizedUsers.add(testUser2);

        EasyMock.expect(reader.getPropertyValues(LogControlServlet.USER_DNS_PROPERTY))
            .andReturn(authorizedUsers).once();

        EasyMock.replay(reader);

        LogControlServlet testSubject = new LogControlServlet() {
            @Override
            public void init(final ServletConfig config) throws ServletException {}
        };

        Set<Principal> principals = testSubject.getAuthorizedUserPrincipals(reader);
        Assert.assertNotNull(principals);
        Assert.assertEquals(2, principals.size());
        Assert.assertTrue(principals.contains(new X500Principal(testUser1)));
        Assert.assertTrue(principals.contains(new X500Principal(testUser2)));
    }

    @Ignore
    @Test
    public void testGetAuthorizedGroupUris() {
        final String testGroup1 = "ivo://example.com/endpoint?FOO";
        final String testGroup2 = "ivo://example.com/endpoint?BAR";

        final PropertiesReader reader = EasyMock.createMock(PropertiesReader.class);

        List<String> authorizedGroups = new ArrayList<>();
        authorizedGroups.add(testGroup1);
        authorizedGroups.add(testGroup2);

        EasyMock.expect(reader.getPropertyValues(LogControlServlet.GROUP_URIS_PROPERTY))
            .andReturn(authorizedGroups).once();

        EasyMock.replay(reader);

        LogControlServlet testSubject = new LogControlServlet() {
            @Override
            public void init(final ServletConfig config) throws ServletException {}
        };

        Set<GroupURI> groupUris = testSubject.getAuthorizedGroupUris(reader);
        Assert.assertNotNull(groupUris);
        Assert.assertEquals(2, groupUris.size());
        Iterator it = groupUris.iterator();
        Assert.assertTrue(it.next().equals(new GroupURI(testGroup1)));
        Assert.assertTrue(it.next().equals(new GroupURI(testGroup2)));
    }

}
