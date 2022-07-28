/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2022.                            (c) 2022.
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

package ca.nrc.cadc.util;

import java.util.Iterator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test bucket selector
 *
 * @author jeevesh
 */
public class BucketSelectorTest {

    private static final Logger log = Logger.getLogger(BucketSelectorTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.util", Level.INFO);
    }

    public BucketSelectorTest() { }

    @Test
    public void testValidRangeMinMax() {
        String goodRange = "0-f";

        try {
            BucketSelector bucketSel = new BucketSelector(goodRange);
            Assert.assertEquals("BucketSelector[0,f]", bucketSel.toString());
            int bucketCount = blurtBucket(bucketSel.getBucketIterator());
            Assert.assertEquals(16, bucketCount);
            Assert.assertEquals("0", bucketSel.getMinBucket());
            Assert.assertEquals("f", bucketSel.getMaxBucket());
        } catch (Exception t) {
            log.error("unexpected exception", t);
            Assert.fail("unexpected exception: " + t);
        }

        goodRange = "3-7";
        try {
            BucketSelector bucketSel = new BucketSelector(goodRange);
            Assert.assertEquals("BucketSelector[3,7]", bucketSel.toString());
            int bucketCount = blurtBucket(bucketSel.getBucketIterator());
            Assert.assertEquals(5, bucketCount);
            Assert.assertEquals("3", bucketSel.getMinBucket());
            Assert.assertEquals("7", bucketSel.getMaxBucket());
        } catch (Exception t) {
            log.error("unexpected exception", t);
            Assert.fail("unexpected exception: " + t);
        }
    }
    
    @Test
    public void testValidRangeMinMaxPadding() {

        String goodRange = "3-7";
        try {
            BucketSelector bucketSel = new BucketSelector(goodRange);
            Assert.assertEquals("BucketSelector[3,7]", bucketSel.toString());
            int bucketCount = blurtBucket(bucketSel.getBucketIterator());
            Assert.assertEquals(5, bucketCount);
            Assert.assertEquals("3", bucketSel.getMinBucket());
            Assert.assertEquals("7", bucketSel.getMaxBucket());
            
            Assert.assertEquals("30", bucketSel.getMinBucket(2));
            Assert.assertEquals("7f", bucketSel.getMaxBucket(2));
            
            Assert.assertEquals("30000", bucketSel.getMinBucket(5));
            Assert.assertEquals("7ffff", bucketSel.getMaxBucket(5));
        } catch (Exception t) {
            log.error("unexpected exception", t);
            Assert.fail("unexpected exception: " + t);
        }
    }
    
    @Test
    public void testValidRange() {
        String goodRange = "0-f";

        try {
            BucketSelector bucketSel = new BucketSelector(goodRange);
            Assert.assertEquals("BucketSelector[0,f]", bucketSel.toString());
            int bucketCount = blurtBucket(bucketSel.getBucketIterator());
            Assert.assertEquals(16, bucketCount);
        } catch (Exception t) {
            log.error("unexpected exception", t);
            Assert.fail("unexpected exception: " + t);
        }

        goodRange = "3-7";
        try {
            BucketSelector bucketSel = new BucketSelector(goodRange);
            Assert.assertEquals("BucketSelector[3,7]", bucketSel.toString());
            int bucketCount = blurtBucket(bucketSel.getBucketIterator());
            Assert.assertEquals(5, bucketCount);
        } catch (Exception t) {
            log.error("unexpected exception", t);
            Assert.fail("unexpected exception: " + t);
        }
    }

    @Test
    public void testValidRangeSpacing() {
        String goodRange = "    0 - f  ";

        try {
            BucketSelector bucketSel = new BucketSelector(goodRange);
            Assert.assertEquals("BucketSelector[0,f]", bucketSel.toString());
            int bucketCount = blurtBucket(bucketSel.getBucketIterator());
            Assert.assertEquals(16, bucketCount);
        } catch (Exception t) {
            log.error("unexpected exception", t);
            Assert.fail("unexpected exception: " + t);
        }

        goodRange = "3 - 7";
        try {
            BucketSelector bucketSel = new BucketSelector(goodRange);
            Assert.assertEquals("BucketSelector[3,7]", bucketSel.toString());
            int bucketCount = blurtBucket(bucketSel.getBucketIterator());
            Assert.assertEquals(5, bucketCount);
        } catch (Exception t) {
            log.error("unexpected exception", t);
            Assert.fail("unexpected exception: " + t);
        }
    }

    @Test
    public void testValidSingleSelector() {
        String goodRange = "d";

        try {
            BucketSelector bucketSel = new BucketSelector(goodRange);
            Assert.assertEquals("BucketSelector[d,d]", bucketSel.toString());
            int bucketCount = blurtBucket(bucketSel.getBucketIterator());
            Assert.assertEquals(1, bucketCount);
        } catch (Exception t) {
            log.error("unexpected exception", t);
            Assert.fail("unexpected exception: " + t);
        }
    }

    @Test
    public void testValidSingleSelectorSpaces() {
        String goodVal = " d";

        try {
            BucketSelector bucketSel = new BucketSelector(goodVal);
            Assert.assertEquals("BucketSelector[d,d]", bucketSel.toString());
            int bucketCount = blurtBucket(bucketSel.getBucketIterator());
            Assert.assertEquals(1, bucketCount);
        } catch (Exception t) {
            log.error("unexpected exception", t);
            Assert.fail("unexpected exception: " + t);
        }

        goodVal = "d  ";

        try {
            BucketSelector bucketSel = new BucketSelector(goodVal);
            Assert.assertEquals("BucketSelector[d,d]", bucketSel.toString());
            int bucketCount = blurtBucket(bucketSel.getBucketIterator());
            Assert.assertEquals(1, bucketCount);
        } catch (Exception t) {
            log.error("unexpected exception", t);
            Assert.fail("unexpected exception: " + t);
        }
    }


    @Test
    public void testInvalidRangeInverted() {
        String badRange = "e-3";

        try {
            BucketSelector bucketSel = new BucketSelector(badRange);
            Assert.fail("should have failed.");
        } catch (IllegalArgumentException expected) {
            log.debug("expected error: " + expected);
        } catch (Exception t) {
            log.error("unexpected exception", t);
            Assert.fail("unexpected exception: " + t);
        }
    }

    @Test
    public void testInvalidRangeBadSelector() {
        String badRange = "3-j";

        try {
            BucketSelector bucketSel = new BucketSelector(badRange);
            Assert.fail("should have failed.");
        } catch (IllegalArgumentException expected) {
            log.debug("expected error: " + expected);
        } catch (Exception t) {
            log.error("unexpected exception", t);
            Assert.fail("unexpected exception: " + t);
        }
    }

    @Test
    public void testInvalidRangeRangeMinTooBig() {
        String badRange = "33";

        try {
            BucketSelector bucketSel = new BucketSelector(badRange);
            Assert.fail("should have failed.");
        } catch (IllegalArgumentException expected) {
            log.debug("expected error: " + expected);
        } catch (Exception t) {
            log.error("unexpected exception", t);
            Assert.fail("unexpected exception: " + t);
        }
    }


    private int blurtBucket(Iterator<String> bucketIter) {
        int size = 0;
        while (bucketIter.hasNext()) {
            String nextBucket = bucketIter.next();
            size++;
            System.out.println("bucket " + size + "= " + nextBucket);
        }
        return size;
    }

}

