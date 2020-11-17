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

import java.net.URI;
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
    public void testSingleEvent() {
    	String appName = "singleEventLogInfoTest";
    	String thread = "singleEventTestThreadName";
    	String label = "singleEventLabel";
    	UUID entityID = new UUID(0L, 100L);
    	URI artifactURI = URI.create("cadc:TEST/singleFile.fits");
    	EventLifeCycle lifeCycle = EventLifeCycle.PROPAGATE;
    	Boolean userSuccess = true;
    	
        try {
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
        	eventLogInfo.setLifeCycle(lifeCycle);
        	eventLogInfo.setSuccess(userSuccess);
        	String singleEventLog = eventLogInfo.singleEvent();
        	
        	String expected = "\"application\":{\"name\":\"singleEventLogInfoTest\"},\"thread\":{\"name\":\"singleEventTestThreadName\"},\"label\":{\"name\":\"singleEventLabel\"},\"log\":{\"level\":\"info\"},\"event\":\"single\",\"entityID\":00000000-0000-0000-0000-000000000064,\"artifactURI\":cadc:TEST/singleFile.fits,\"lifeCycle\":PROPAGATE,\"duration\"";
            Assert.assertTrue("Wrong single event log", singleEventLog.contains(expected));
        	String expectedMethod = "\"method\":\"PUT\"";
            Assert.assertTrue("Wrong single event log, expected PUT method", singleEventLog.contains(expectedMethod));
        	String expectedSuccess = "\"success\":true";
            Assert.assertTrue("Wrong single event log, expected success to be true", singleEventLog.contains(expectedSuccess));
        }
        catch (Throwable t) {
            log.error(t.getMessage());
        }
    }

    @Test
    public void testStartEvent() {
    	String appName = "startEventLogInfoTest";
    	String thread = "startEventTestThreadName";
    	String label = "startEventLabel";
    	UUID entityID = new UUID(0L, 100L);
    	URI artifactURI = URI.create("cadc:TEST/startFile.fits");
    	EventLifeCycle lifeCycle = EventLifeCycle.CREATE;
    	
        try {
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
        	EventStartKVP startKVP = new EventStartKVP(EventStartKey.LASTMODIFIED, "2020-11-07T12:18:03.694");
        	eventLogInfo.setStartKVP(startKVP);
        	String singleEventLog = eventLogInfo.start();
        	
        	String expected = "\"application\":{\"name\":\"startEventLogInfoTest\"},\"thread\":{\"name\":\"startEventTestThreadName\"},\"label\":{\"name\":\"startEventLabel\"},\"log\":{\"level\":\"info\"},\"event\":\"start\",\"entityID\":00000000-0000-0000-0000-000000000064,\"artifactURI\":cadc:TEST/startFile.fits,\"lifeCycle\":CREATE,\"duration\"";
            Assert.assertTrue("Wrong start event log", singleEventLog.contains(expected));
        	String expectedMethod = "\"method\":\"QUERY\"";
            Assert.assertTrue("Wrong single event log, expected QUERY method", singleEventLog.contains(expectedMethod));
            String expectedStart = "\"start\":{\"key\":LASTMODIFIED,\"value\":\"2020-11-07T12:18:03.694\"}";
            Assert.assertTrue("Wrong single event log, expected start", singleEventLog.contains(expectedStart));
        	String expectedSuccess = "\"success\":";
            Assert.assertFalse("Wrong single event log, expected to have no success field", singleEventLog.contains(expectedSuccess));
        }
        catch (Throwable t) {
            log.error(t.getMessage());
        }
    }
}
