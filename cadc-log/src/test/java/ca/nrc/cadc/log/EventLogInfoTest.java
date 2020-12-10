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

import ca.nrc.cadc.util.Log4jInit;

import static org.junit.Assert.fail;

import java.net.URI;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class EventLogInfoTest {
    private static final Logger log = Logger.getLogger(EventLogInfoTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.log", Level.INFO);
    }

    @Test
    public void testSanitize() {
        String sketchy = "sketchy \"json\" content\n in     this     message";
        String expected = "sketchy 'json' content in this message";
        String actual = EventLogInfo.sanitize(sketchy);
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testSingleEvent() throws Throwable {
        String appName = "singleEventLogInfoTest";
        String thread = "singleEventTestThreadName";
        String label = "singleEventLabel";
        UUID entityID = new UUID(0L, 100L);
        URI artifactURI = URI.create("cadc:TEST/singleFile.fits");
        EventLifeCycle lifeCycle = EventLifeCycle.PROPAGATE;
        Boolean userSuccess = true;
    
        Thread.currentThread().setName(thread);
        long startTime = System.currentTimeMillis();
        Thread.sleep(50); // sleep for 50 ms
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        String method = "PUT";
        
        EventLogInfo eventLogInfo = new EventLogInfo(appName, label, method);
        eventLogInfo.setArtifactURI(artifactURI);
        eventLogInfo.setElapsedTime(duration);
        eventLogInfo.setEntityID(entityID);
        eventLogInfo.setAdditionalInfo("more info");
        eventLogInfo.setLifeCycle(lifeCycle);
        eventLogInfo.setUrls(3);
        eventLogInfo.setAttempts(1);
        eventLogInfo.setSuccess(userSuccess);
        String singleEventLog = eventLogInfo.singleEvent();
        
        String expected = "\"application\":{\"name\":\"singleEventLogInfoTest\",\"class\":\"singleEventLabel\"},\"thread\":{\"name\":\"singleEventTestThreadName\"},\"log\":{\"level\":\"info\"},\"event\":{\"type\":\"single\",\"entityID\":\"00000000-0000-0000-0000-000000000064\",\"artifactURI\":\"cadc:TEST/singleFile.fits\",\"lifeCycle\":\"PROPAGATE\",\"operation\":\"PUT\",\"additionalInfo\":\"more info\",\"urls\":3,\"attempts\":1,\"duration\"";
        Assert.assertTrue("Wrong single event log", singleEventLog.contains(expected));
        String expectedSuccess = "\"success\":true";
        Assert.assertTrue("Wrong single event log, expected success to be true", singleEventLog.contains(expectedSuccess));
    }

    @Test
    public void testStartEvent() throws Throwable {
        String appName = "startEventLogInfoTest";
        String thread = "startEventTestThreadName";
        String label = "startEventLabel";
        UUID entityID = new UUID(0L, 100L);
        URI artifactURI = URI.create("cadc:TEST/startFile.fits");
        EventLifeCycle lifeCycle = EventLifeCycle.CREATE;

        Thread.currentThread().setName(thread);
        long startTime = System.currentTimeMillis();
        Thread.sleep(100); // sleep for 100 ms
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        String method = "QUERY";
        
        EventLogInfo eventLogInfo = new EventLogInfo(appName, label, method);
        eventLogInfo.setArtifactURI(artifactURI);
        eventLogInfo.setElapsedTime(duration);
        eventLogInfo.setEntityID(entityID);
        eventLogInfo.setLifeCycle(lifeCycle);
        Map<EventIteratorKey, String> iteratorItem = new EnumMap<EventIteratorKey, String>(EventIteratorKey.class);
        iteratorItem.put(EventIteratorKey.LASTMODIFIED, "2020-11-07T12:18:03.694");
        eventLogInfo.setIteratorItem(iteratorItem);
        String startEventLog = eventLogInfo.start();
        
        String expected = "\"application\":{\"name\":\"startEventLogInfoTest\",\"class\":\"startEventLabel\"},\"thread\":{\"name\":\"startEventTestThreadName\"},\"log\":{\"level\":\"info\"},\"event\":{\"type\":\"start\",\"entityID\":\"00000000-0000-0000-0000-000000000064\",\"artifactURI\":\"cadc:TEST/startFile.fits\",\"lifeCycle\":\"CREATE\",\"iteratorItem\":{\"key\":LASTMODIFIED,\"value\":\"2020-11-07T12:18:03.694\"},\"operation\":\"QUERY\",\"duration\"";
        Assert.assertTrue("Wrong start event log", startEventLog.contains(expected));
        String expectedSuccess = "\"success\":";
        Assert.assertFalse("Wrong single event log, expected to have no success field", startEventLog.contains(expectedSuccess));
    }

    @Test
    public void testEndEvent() throws Throwable {
        String appName = "endEventLogInfoTest";
        String thread = "endEventTestThreadName";
        String label = "endEventLabel";
        UUID entityID = new UUID(0L, 100L);
        URI artifactURI = URI.create("cadc:TEST/endFile.fits");
        EventLifeCycle lifeCycle = EventLifeCycle.PROPAGATE;

        Thread.currentThread().setName(thread);
        long startTime = System.currentTimeMillis();
        Thread.sleep(100); // sleep for 100 ms
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        String method = "QUERY";
        
        EventLogInfo eventLogInfo = new EventLogInfo(appName, label, method);
        eventLogInfo.setArtifactURI(artifactURI);
        eventLogInfo.setElapsedTime(duration);
        eventLogInfo.setEntityID(entityID);
        eventLogInfo.setLifeCycle(lifeCycle);
        eventLogInfo.setTotal(100);
        eventLogInfo.setMessage("no error");

        try {
            // test null success
            String endEventLog = eventLogInfo.end();
            fail("Should have thrown an IllegalArgumentException due to success being null");
        } catch (IllegalArgumentException ex) {
            // expected
        }

        // test null success
        eventLogInfo.setSuccess(true);
        String endEventLog = eventLogInfo.end();
        String expected = "\"application\":{\"name\":\"endEventLogInfoTest\",\"class\":\"endEventLabel\"},\"thread\":{\"name\":\"endEventTestThreadName\"},\"log\":{\"level\":\"info\"},\"event\":{\"type\":\"end\",\"entityID\":\"00000000-0000-0000-0000-000000000064\",\"artifactURI\":\"cadc:TEST/endFile.fits\",\"lifeCycle\":\"PROPAGATE\",\"operation\":\"QUERY\",\"message\":\"no error\",\"total\":100,\"duration\"";
        Assert.assertTrue("Wrong end event log", endEventLog.contains(expected));
        String expectedSuccess = "\"success\":true";
        Assert.assertTrue("Wrong end event log, expected success field to be true", endEventLog.contains(expectedSuccess));
    }

}
