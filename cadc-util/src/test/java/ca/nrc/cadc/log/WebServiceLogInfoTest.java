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

package ca.nrc.cadc.log;

import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.util.Log4jInit;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class WebServiceLogInfoTest {
    private static final Logger log = Logger.getLogger(WebServiceLogInfoTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.log", Level.INFO);
    }

    private class LogInfoWithTestAttributes extends WebServiceLogInfo {
        
        protected List<Integer> intList;
        protected List<String> strList;
        protected List<UUID> uuidList;
        
        public LogInfoWithTestAttributes() {
            super();
        }
        
        public void setStrList(List<String> listOfStr) {
            this.strList = listOfStr;
        }

        public void setIntList(List<Integer> listOfInt) {
            this.intList = listOfInt;
        }

        public void setUUIDList(List<UUID> listOfUUID) {
            this.uuidList = listOfUUID;
        }
    }
    
    @Test
    public void testListAttributes() {
        
        // test empty lists
        LogInfoWithTestAttributes emptyListLogInfo = new LogInfoWithTestAttributes();
        List<String> emptyStrList = Arrays.asList();
        List<Integer> emptyIntList = Arrays.asList();
        List<UUID> emptyUUIDList = Arrays.asList();
        emptyListLogInfo.setStrList(emptyStrList);
        emptyListLogInfo.setIntList(emptyIntList);
        emptyListLogInfo.setUUIDList(emptyUUIDList);
        String start = emptyListLogInfo.start();
        log.info("testEmptyList: " + start);
        String expected = "\"phase\":\"start\",\"intList\":[],\"strList\":[],\"uuidList\":[]";
        Assert.assertTrue("Wrong start", start.contains(expected));
        
        // test non-empty list of String
        LogInfoWithTestAttributes nonEmptyStrListLogInfo = new LogInfoWithTestAttributes();
        List<String> strList = Arrays.asList("string1", "string2", "string3");
        nonEmptyStrListLogInfo.setStrList(strList);
        start = nonEmptyStrListLogInfo.start();
        log.info("testListOfStrings: " + start);
        expected = "\"phase\":\"start\",\"strList\":[\"string1\",\"string2\",\"string3\"]";
        Assert.assertTrue("Wrong start", start.contains(expected));
        
        // test non-empty list of quoted and spaced String
        LogInfoWithTestAttributes quotedStrListLogInfo = new LogInfoWithTestAttributes();
        List<String> quotedStrList = Arrays.asList("John\"s", "spa  c   ed", "Mary\"s");
        quotedStrListLogInfo.setStrList(quotedStrList);
        start = quotedStrListLogInfo.start(); 
        log.info("testListOfQuotedStrings: " + start);
        expected = "\"phase\":\"start\",\"strList\":[\"John's\",\"spa c ed\",\"Mary's\"]";
        Assert.assertTrue("Wrong start", start.contains(expected));
        
        // test non-empty list of Integers
        LogInfoWithTestAttributes nonEmptyIntListLogInfo = new LogInfoWithTestAttributes();
        List<Integer> intList = Arrays.asList(1, 2, 3);
        nonEmptyIntListLogInfo.setIntList(intList);
        start = nonEmptyIntListLogInfo.start();
        log.info("testListOfIntegers: " + start);
        expected = "\"phase\":\"start\",\"intList\":[1,2,3]";
        Assert.assertTrue("Wrong start", start.contains(expected));
        
        // test non-empty list of UUIDs
        LogInfoWithTestAttributes nonEmptyUUIDListLogInfo = new LogInfoWithTestAttributes();
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        List<UUID> uuidList = Arrays.asList(uuid1, uuid2, uuid3);
        nonEmptyUUIDListLogInfo.setUUIDList(uuidList);
        start = nonEmptyUUIDListLogInfo.start();
        log.info("testListOfUUIDs: " + start);
        expected = "\"phase\":\"start\",\"uuidList\":[\"" + uuid1 + "\",\"" + uuid2 + "\",\"" + uuid3 +"\"]";
        Assert.assertTrue("Wrong start", start.contains(expected));
        
        
    }
    
    @Test
    public void testSanitize() {
        String sketchy = "sketchy \"json\" content\n in     this     message";
        String expected = "sketchy 'json' content in this message";
        String actual = WebServiceLogInfo.sanitize(sketchy);
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testMinimalContentServlet() throws Exception {
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);

        String contextPath = "/service_name";
        String servletPath = "/servlet_name";
        String pathInfo = "/path/info";
        String runID = "A1S2D3F4G5H6J7K8L90";
        String queryEncoded = URLEncoder.encode("foo=123&bar=abc&runID=" + runID + "&baz=foobar", "UTF-8");
        String queryDecoded = URLDecoder.decode(queryEncoded);

        EasyMock.expect(request.getMethod()).andReturn("Get").once();
        EasyMock.expect(request.getContextPath()).andReturn(contextPath).once();
        EasyMock.expect(request.getServletPath()).andReturn(servletPath).once();
        EasyMock.expect(request.getPathInfo()).andReturn(pathInfo).once();
        EasyMock.expect(request.getQueryString()).andReturn(queryEncoded).once();
        EasyMock.expect(request.getHeader("X-Forwarded-For")).andReturn(null).once();
        EasyMock.expect(request.getRemoteAddr()).andReturn("192.168.0.0").once();

        EasyMock.replay(request);

        WebServiceLogInfo logInfo = new ServletLogInfo(request);
        String start = logInfo.start();
        log.info("testMinimalContentServlet: " + start);
        String end = logInfo.end();
        log.info("testMinimalContentServlet: " + end);
        String expected = "\"phase\":\"start\",\"ip\":\"192.168.0.0\",\"method\":\"GET\",\"path\":\"/service_name/servlet_name/path/info?" + queryDecoded + "\",\"runID\":\"" + runID + "\"";
        Assert.assertTrue("Wrong start", start.contains(expected));
        Assert.assertTrue("Wrong start", start.contains("\"service\":{\"name\":\"service_name\"}"));
        expected = "\"phase\":\"end\",\"ip\":\"192.168.0.0\",\"method\":\"GET\",\"path\":\"/service_name/servlet_name/path/info?" + queryDecoded + "\",\"runID\":\"" + runID + "\",\"success\":true";
        Assert.assertTrue("Wrong end", end.contains(expected));
        Assert.assertTrue("Wrong end", end.contains("\"service\":{\"name\":\"service_name\"}"));

        Assert.assertEquals(logInfo.path, contextPath + servletPath + pathInfo + "?" + queryDecoded);
        Assert.assertEquals(logInfo.runID, runID);

        EasyMock.verify(request);
    }

    @Test
    public void testMaximalContentServlet() throws Exception {
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);

        String contextPath = "/service_name";
        String servletPath = "/servlet_name";
        String pathInfo = "/path/info";
        String runID = "A1S2D3F4G5H6J7K8L90";
        String queryEncoded = URLEncoder.encode("foo=123&bar=abc&runID=" + runID + "&baz=foobar", "UTF-8");
        String queryDecoded = URLDecoder.decode(queryEncoded);

        EasyMock.expect(request.getMethod()).andReturn("Get").once();
        EasyMock.expect(request.getContextPath()).andReturn(contextPath).once();
        EasyMock.expect(request.getServletPath()).andReturn(servletPath).once();
        EasyMock.expect(request.getPathInfo()).andReturn(pathInfo).once();
        EasyMock.expect(request.getQueryString()).andReturn(queryEncoded).once();
        EasyMock.expect(request.getHeader("X-Forwarded-For")).andReturn(null).once();
        EasyMock.expect(request.getRemoteAddr()).andReturn("192.168.0.0").once();

        EasyMock.replay(request);

        WebServiceLogInfo logInfo = new ServletLogInfo(request);
        String start = logInfo.start();
        log.info("testMaximalContentServlet: " + start);
        String expected = "\"phase\":\"start\",\"ip\":\"192.168.0.0\",\"method\":\"GET\",\"path\":\"/service_name/servlet_name/path/info?" + queryDecoded + "\",\"runID\":\"" + runID + "\"";
        Assert.assertTrue("Wrong start", start.contains(expected));
        Assert.assertTrue("Wrong start", start.contains("\"service\":{\"name\":\"service_name\"}"));
        logInfo.setSuccess(false);
        logInfo.setSubject(createSubject("the user", "the proxy"));
        logInfo.setElapsedTime(1234L);
        logInfo.setBytes(10L);
        logInfo.setMessage("the message");
        String end = logInfo.end();
        log.info("testMaximalContentServlet: " + end);
        expected = "\"phase\":\"end\",\"bytes\":10,\"ip\":\"192.168.0.0\",\"message\":\"the message\",\"method\":\"GET\",\"authMethod\":\"anon\",\"path\":\"/service_name/servlet_name/path/info?" + queryDecoded + "\",\"proxyUser\":\"the proxy\",\"runID\":\"" + runID + "\",\"success\":false,\"duration\":1234,\"user\":\"the user\"";
        Assert.assertTrue("Wrong end", end.contains(expected));
        Assert.assertTrue("Wrong end", end.contains("\"service\":{\"name\":\"service_name\"}"));

        Assert.assertEquals(logInfo.path, contextPath + servletPath + pathInfo + "?" + queryDecoded);
        Assert.assertEquals(logInfo.runID, runID);

        EasyMock.verify(request);
    }

    private Subject createSubject(String userid, String proxyUser) {
        Subject s = new Subject();
        HttpPrincipal p = new HttpPrincipal(userid, proxyUser);
        s.getPrincipals().add(p);
        return s;
    }
}
