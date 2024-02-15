/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2023.                            (c) 2023.
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

package org.opencadc.persist;

import ca.nrc.cadc.util.Log4jInit;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Date;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author pdowler
 */
public class EntityTest {
    private static final Logger log = Logger.getLogger(EntityTest.class);

    static {
        Log4jInit.setLevel("org.opencadc.persist", Level.DEBUG);
        // this actually controls the large amoutn of debug output from checksum
        // algorithm, but it effects the whole jvm so only enable when running
        // these tests specificially and looking at output
        //Entity.MCS_DEBUG = true;
    }
    
    public EntityTest() { 
    }
    
    /*
    @Test
    public void testTemplate() {
        try {
            
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    */
    
    @Test
    public void testEntity() {
        // base: the cadc-inventory-0.x configuration
        doEntityTest(false, false);
        doNewVersionTest(false, false);
    }
    
    @Test
    public void testEntityTruncateDates() {
        // the caom2-2.4 configuration
        doEntityTest(true, false);
        doNewVersionTest(true, false);
    }
    
    @Test
    public void testEntityDigestFieldNames() {
        // the cadc-vos-2.x configuration
        doEntityTest(false, true);
        doNewVersionTest(false, true);
    }
    
    @Test
    public void testEntitySafeMode() {
        // no known use, but truncateDates and digestFieldNames is the safest mode
        doEntityTest(true, true);
        doNewVersionTest(true, true);
    }

    private void doEntityTest(boolean trunc, boolean dig) {
        try {
            SampleEntity sample = new SampleEntity("name-of-this-entity", trunc, dig);
            log.info("created: " + sample);
            
            URI mcs1 = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            
            sample.dateVal = new Date();
            URI mcs2 = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            Assert.assertNotEquals(mcs1, mcs2);
            
            sample.uriVal = URI.create("foo:bar/baz");
            URI mcs3 = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            Assert.assertNotEquals(mcs2, mcs3);
            
            sample.doubleVal = 2.0;
            URI mcs4 = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            Assert.assertNotEquals(mcs3, mcs4);
            
            sample.longVal = 1024L;
            URI mcs5 = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            Assert.assertNotEquals(mcs4, mcs5);
            
            sample.sampleSE = SampleStringEnum.B;
            URI mcs6 = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            Assert.assertNotEquals(mcs5, mcs6);
            
            sample.sampleIE = SampleIntEnum.Y;
            URI mcs7 = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            Assert.assertNotEquals(mcs6, mcs7);
            
            // set of string
            sample.strList.add("foo");
            URI mcs8 = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            Assert.assertNotEquals(mcs7, mcs8);
            sample.strList.add("bar");
            URI mcs9 = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            Assert.assertNotEquals(mcs8, mcs9);
            // revert to 7
            sample.strList.clear();
            URI mcs10 = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            Assert.assertEquals(mcs7, mcs10);
            
            // nested object
            sample.nested = new SampleEntity.Nested();
            URI mcs11 = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            Assert.assertEquals(mcs7, mcs11);
            sample.nested.nstr = "boo";
            URI mcs12 = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            Assert.assertNotEquals(mcs7, mcs12);
            
            // entities do not get included in metaChecksum
            sample.children.add(new SampleEntity("flibble", trunc, dig));
            URI tcs1 = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            Assert.assertEquals(mcs12, tcs1);
            
            sample.relation = new SampleEntity("flibble", trunc, dig);
            mcs11 = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            Assert.assertEquals(mcs12, mcs11);
            
            // revert
            sample.dateVal = null;
            sample.doubleVal = null;
            sample.longVal = null;
            sample.uriVal = null;
            sample.sampleSE = null;
            sample.sampleIE = null;
            sample.nested = null;
            URI clearCS = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            Assert.assertEquals(mcs1, clearCS);
            
        } catch (Exception ex) {
            log.error("unexpected exception", ex);
            Assert.fail("unexpected exception: " + ex);
        }
    }
    
    // also doubles as a sub-class/extension test
    private void doNewVersionTest(boolean trunc, boolean dig) {
        try {
            SampleEntity v1 = new SampleEntity("name-of-this-entity", trunc, dig);
            log.info("created: " + v1);
            URI mcs1 = v1.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            
            SampleEntityV2 v2 = new SampleEntityV2(v1.getID(), v1.getName(), trunc, dig);
            log.info("created: " + v1);
            URI mcs2 = v2.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            
            Assert.assertEquals(mcs1, mcs2);
        } catch (Exception ex) {
            log.error("unexpected exception", ex);
            Assert.fail("unexpected exception: " + ex);
        }
    }
    
    @Test
    public void testNonState() {
        try {
            SampleEntity sample = new SampleEntity("name-of-this-entity", false, false);
            log.info("created: " + sample);
            
            URI mcs1 = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            
            sample.transientVal = "mrs flibble";    // transient
            SampleEntity.staticVal = "electricity"; // static
            
            URI mcs2 = sample.computeMetaChecksum(MessageDigest.getInstance("MD5"));
            Assert.assertEquals(mcs1, mcs2);
            
        } catch (Exception ex) {
            log.error("unexpected exception", ex);
            Assert.fail("unexpected exception: " + ex);
        }
    }
}
