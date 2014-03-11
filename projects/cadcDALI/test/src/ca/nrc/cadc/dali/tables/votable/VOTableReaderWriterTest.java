/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2009.                            (c) 2009.
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
*  $Revision: 4 $
*
************************************************************************
*/
package ca.nrc.cadc.dali.tables.votable;

import ca.nrc.cadc.dali.tables.TableData;
import ca.nrc.cadc.dali.tables.TableWriter;
import ca.nrc.cadc.date.DateUtil;
import ca.nrc.cadc.stc.Circle;
import ca.nrc.cadc.stc.Flavor;
import ca.nrc.cadc.stc.Frame;
import ca.nrc.cadc.stc.Position;
import ca.nrc.cadc.stc.ReferencePosition;
import ca.nrc.cadc.stc.Region;
import ca.nrc.cadc.stc.STC;
import ca.nrc.cadc.util.Log4jInit;
import java.io.StringWriter;
import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jburke
 */
public class VOTableReaderWriterTest
{
    private static final Logger log = Logger.getLogger(VOTableReaderWriterTest.class);

    private static final String DATE_TIME = "2009-01-02T11:04:05.678";
    private static DateFormat dateFormat;
    
    static
    {
        Log4jInit.setLevel("ca.nrc.cadc.dali.tables", Level.INFO);
        dateFormat = DateUtil.getDateFormat(DateUtil.IVOA_DATE_FORMAT, DateUtil.UTC);
    }
    
    public VOTableReaderWriterTest() { }

    @Test
    public void testReadWriteVOTable() throws Exception
    {
        log.debug("testReadWriteVOTable");
        try
        {
            String resourceName = "VOTable resource name";

            // Build a VOTable.
            VOTableDocument expected = new VOTableDocument();
            
            VOTableResource vr = new VOTableResource("meta");
            expected.getResources().add(vr);
            vr.getParams().addAll(getMetaParams());
            vr.getGroups().add(getMetaGroup());
            
            vr = new VOTableResource("results");
            expected.getResources().add(vr);
            vr.setName(resourceName);
            VOTableTable vot = new VOTableTable();
            vr.setTable(vot);

            // Add INFO's.
            vot.getInfos().addAll(getTestInfos());

            // Add VOTableFields.
            vot.getParams().addAll(getTestParams());
            vot.getFields().addAll(getTestFields());

            // Add TableData.
            vot.setTableData(new TestTableData());

            // Write VOTable to xml.
            StringWriter sw = new StringWriter();
            TableWriter<VOTableDocument> writer = new VOTableWriter();
            writer.write(expected, sw);
            String xml = sw.toString();
            log.debug("XML: \n\n" + xml);

            // Read in xml to VOTable with schema validation.
            VOTableReader reader = new VOTableReader();
            VOTableDocument actual = reader.read(xml);
            
            log.debug("Expected:\n\n" + expected);
            log.debug("Actual:\n\n" + actual);

            // writer always places thios placeholder after a table
            vr.getInfos().add(new VOTableInfo("QUERY_STATUS", "OK"));
            compareVOTable(expected, actual, null);

            log.info("testReadWriteVOTable passed");
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testReadWriteVOTableWithMax() throws Exception
    {
        log.debug("testReadWriteVOTable");
        try
        {
            String resourceName = "VOTable resource name";

            // Build a VOTable.
            VOTableDocument expected = new VOTableDocument();
            VOTableResource vr = new VOTableResource("results");
            vr.getInfos().add(new VOTableInfo("QUERY_STATUS", "OK"));
            expected.getResources().add(vr);
            vr.setName(resourceName);

            VOTableTable vot = new VOTableTable();
            vr.setTable(vot);
            
            // Add INFO's.
            vot.getInfos().addAll(getTestInfos());

            // Add VOTableFields.
            vot.getParams().addAll(getTestParams());
            vot.getFields().addAll(getTestFields());

            // Add TableData.
            vot.setTableData(new TestTableData());

            // Write VOTable to xml.
            StringWriter sw = new StringWriter();
            TableWriter<VOTableDocument> writer = new VOTableWriter();
            writer.write(expected, sw, 3L);
            String xml = sw.toString();
            log.debug("XML: \n\n" + xml);

            // Read in xml to VOTable with schema validation.
            VOTableReader reader = new VOTableReader();
            VOTableDocument actual = reader.read(xml);
            
            // the write should stick in this extra INFO element, so add to expected
            vr.getInfos().add(new VOTableInfo("QUERY_STATUS", "OVERFLOW"));
            compareVOTable(expected, actual, 3L);

            log.info("testReadWriteVOTable passed");
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    /**
     *
     * Test might be a bit dodgy since it's assuming the VOTable
     * elements will be written and read in the same order.
     */
    public void compareVOTable(VOTableDocument expected, VOTableDocument actual, Long actualMax)
    {
        Assert.assertNotNull(expected);
        Assert.assertNotNull(actual);

        Assert.assertEquals(expected.getResources().size(), actual.getResources().size());
        
        for (int i=0; i<expected.getResources().size(); i++)
        {
            compareVOTableResource(expected.getResources().get(i), actual.getResources().get(i), actualMax);
        }
    }
    
    public void compareVOTableResource(VOTableResource expected, VOTableResource actual, Long actualMax)
    {
        Assert.assertEquals(expected.getName(), actual.getName());

        compareInfos(expected.getInfos(), actual.getInfos());

        compareParams(expected.getParams(), actual.getParams());
        
        compareGroups(expected.getGroups(), actual.getGroups());

        compareTables(expected.getTable(), actual.getTable(), actualMax);
    }
    
    public void compareTables(VOTableTable expected, VOTableTable actual, Long actualMax)
    {
        if (expected != null)
            Assert.assertNotNull(actual);
        else
        {
            Assert.assertNull(actual);
            return;
        }
        
        compareInfos(expected.getInfos(), actual.getInfos());
        compareParams(expected.getParams(), actual.getParams());
        compareFields(expected.getFields(), actual.getFields());
        
        // TABLEDATA
        Assert.assertNotNull(expected.getTableData());
        Assert.assertNotNull(actual.getTableData());
        TableData expectedTableData = expected.getTableData();
        TableData actualTableData = actual.getTableData();
        Iterator<List<Object>> expectedIter = expectedTableData.iterator();
        Iterator<List<Object>> actualIter = actualTableData.iterator();
        Assert.assertNotNull(expectedIter);
        Assert.assertNotNull(actualIter);
        int iteratorCount = 0;
        while (actualIter.hasNext()) // this one is the smaller list
        {
            iteratorCount++;
            log.debug("iteratorCount: " + (iteratorCount));

            List<Object> actualList = actualIter.next();
            List<Object> expectedList = expectedIter.next();
            log.debug("expected row: " + expectedList);
            log.debug("  actual row: " + actualList);
            
            Assert.assertEquals(expectedList.size(), actualList.size());
            for (int i = 0; i < expectedList.size(); i++)
            {
                Object expectedObject = expectedList.get(i);
                Object actualObject = actualList.get(i);

                if (expectedObject instanceof byte[] && actualObject instanceof byte[])
                {
                    Assert.assertArrayEquals((byte[]) expectedObject, (byte[]) actualObject);
                }
                else if (expectedObject instanceof double[] && actualObject instanceof double[])
                {
                    Assert.assertArrayEquals((double[]) expectedObject, (double[]) actualObject, 0.0);
                }
                else if (expectedObject instanceof float[] && actualObject instanceof float[])
                {
                    Assert.assertArrayEquals((float[]) expectedObject, (float[]) actualObject, 0.0f);
                }
                else if (expectedObject instanceof int[] && actualObject instanceof int[])
                {
                    Assert.assertArrayEquals((int[]) expectedObject, (int[]) actualObject);
                }
                else if (expectedObject instanceof long[] && actualObject instanceof long[])
                {
                    Assert.assertArrayEquals((long[]) expectedObject, (long[]) actualObject);
                }
                else if (expectedObject instanceof short[] && actualObject instanceof short[])
                {
                    Assert.assertArrayEquals((short[]) expectedObject, (short[]) actualObject);
                }
                else if (expectedObject instanceof Position && actualObject instanceof Position)
                {
                    Position expectedPosition = (Position) expectedObject;
                    Position actaulPosition = (Position) actualObject;
                    Assert.assertEquals(STC.format(expectedPosition), STC.format(actaulPosition));
                }
                else if (expectedObject instanceof Region && actualObject instanceof Region)
                {
                    Region expectedRegion = (Region) expectedObject;
                    Region actualRegion = (Region) actualObject;
                    Assert.assertEquals(STC.format(expectedRegion), STC.format(actualRegion));
                }
                else
                {
                     Assert.assertEquals(expectedObject, actualObject);
                }
            }
        }
        
        if (actualMax != null)
            Assert.assertEquals("wrong number of iterations", actualMax.intValue(), iteratorCount);
    }

    public void compareInfos(List<VOTableInfo>  expected, List<VOTableInfo> actual)
    {
        // INFO
        Assert.assertNotNull(expected);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++)
        {
            VOTableInfo expectedInfo = expected.get(i);
            VOTableInfo actualInfo = actual.get(i);
            Assert.assertNotNull(expectedInfo);
            Assert.assertNotNull(actualInfo);
            Assert.assertEquals(expectedInfo.getName(), actualInfo.getName());
            Assert.assertEquals(expectedInfo.getValue(), actualInfo.getValue());
        }
    }
    public void compareGroups(List<VOTableGroup> expected, List<VOTableGroup> actual)
    {
        Assert.assertEquals("number of groups", expected.size(), actual.size());
        for (int i=0; i<expected.size(); i++)
        {
            VOTableGroup eg = expected.get(i);
            VOTableGroup ag = actual.get(i);
            Assert.assertEquals(eg.getName(), ag.getName());
            compareParams(eg.getParams(), ag.getParams());
            compareGroups(eg.getGroups(), ag.getGroups());
        }
    }
    
    public void compareParams(List<VOTableParam>  expected, List<VOTableParam> actual)
    {
        // PARAM
        Assert.assertNotNull(expected);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++)
        {
            VOTableParam expectedParam = expected.get(i);
            VOTableParam actualParam = actual.get(i);
            Assert.assertNotNull(expectedParam);
            Assert.assertNotNull(actualParam);
            Assert.assertEquals(expectedParam.getName(), actualParam.getName());
            Assert.assertEquals(expectedParam.getDatatype(), expectedParam.getDatatype());
            Assert.assertEquals(expectedParam.getValue(), expectedParam.getValue());
            Assert.assertEquals(expectedParam.id, actualParam.id);
            Assert.assertEquals(expectedParam.ucd, actualParam.ucd);
            Assert.assertEquals(expectedParam.unit, actualParam.unit);
            Assert.assertEquals(expectedParam.utype, actualParam.utype);
            Assert.assertEquals(expectedParam.xtype, actualParam.xtype);
            Assert.assertEquals(expectedParam.getArraysize(), actualParam.getArraysize());
            Assert.assertEquals(expectedParam.isVariableSize(), actualParam.isVariableSize());
            Assert.assertEquals(expectedParam.description, actualParam.description);
            List<String> expectedValues = expectedParam.getValues();
            List<String> actualValues = actualParam.getValues();
            if (expectedValues == null)
            {
                Assert.assertNull(actualValues);
                continue;
            }
            Assert.assertEquals(expectedValues.size(), actualValues.size());
            for (int j = 0; j < expectedValues.size(); j++)
            {
                Assert.assertEquals(expectedValues.get(j), actualValues.get(j));
            }
        }
    }
    public void compareFields(List<VOTableField>  expected, List<VOTableField> actual)
    {
        // FIELD
        Assert.assertNotNull(expected);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++)
        {
            VOTableField expectedField = expected.get(i);
            VOTableField actualField = actual.get(i);
            Assert.assertNotNull(expectedField);
            Assert.assertNotNull(actualField);
            Assert.assertEquals(expectedField.getName(), actualField.getName());
            Assert.assertEquals(expectedField.getDatatype(), expectedField.getDatatype());
            Assert.assertEquals(expectedField.id, actualField.id);
            Assert.assertEquals(expectedField.ucd, actualField.ucd);
            Assert.assertEquals(expectedField.unit, actualField.unit);
            Assert.assertEquals(expectedField.utype, actualField.utype);
            Assert.assertEquals(expectedField.xtype, actualField.xtype);
            Assert.assertEquals(expectedField.getArraysize(), actualField.getArraysize());
            Assert.assertEquals(expectedField.isVariableSize(), actualField.isVariableSize());
            Assert.assertEquals(expectedField.description, actualField.description);
        }
    }
    public static List<VOTableInfo> getTestInfos()
    {
        List<VOTableInfo> infos = new ArrayList<VOTableInfo>();

        infos.add(new VOTableInfo("QUERY", "select * from ivoa.ObsCore"));
        
        return infos;
    }

    public static List<VOTableParam> getMetaParams()
    {
        List<VOTableParam> params = new ArrayList<VOTableParam>();
        params.add(new VOTableParam("standardID", "char", "ivo://ivoa.net/std/DataLink/1.0"));
        params.add(new VOTableParam("resourceIdentifier", "char", "ivo://cadc.nrc.ca/datalink"));
        params.add(new VOTableParam("accessURL", "char", "http://www.cadc.hia.nrc.gc.ca/caom2ops/datalink"));
        return params;
    }
    public static VOTableGroup getMetaGroup()
    {
        VOTableGroup ret = new VOTableGroup("input");
        
        VOTableParam servParam = new VOTableParam("ID", "char", "");
        servParam.ref = "someID";
        ret.getParams().add(servParam);
        ret.getParams().add(new VOTableParam("MAXREC", "int", "666"));
                
        return ret;
    }
    
    public static List<VOTableParam> getTestParams()
    {
        List<VOTableParam> params = new ArrayList<VOTableParam>();
        
        VOTableParam intersects = new VOTableParam("INPUT:INTERSECTS", "char", "OVERLAPS");
        intersects.id = null;
        intersects.ucd = null;
        intersects.unit = null;
        intersects.utype = null;
        intersects.xtype = null;
        intersects.setVariableSize(true);
        intersects.description = null;
        intersects.getValues().add("ALL");
        intersects.getValues().add("BLAST");
        intersects.getValues().add("CFHT");
        intersects.getValues().add("HST");
        intersects.getValues().add("JCMT");
        params.add(intersects);

        VOTableParam collection = new VOTableParam("INPUT:COLLECTION", "char", "ALL");
        collection.id = null;
        collection.ucd = null;
        collection.unit = null;
        collection.utype = null;
        collection.xtype = null;
        collection.setVariableSize(true);
        collection.description = null;
        params.add(collection);

        return params;
    }

    public static List<VOTableField> getTestFields()
    {
        List<VOTableField> fields = new ArrayList<VOTableField>();

        // Add VOTableFields.
        VOTableField booleanColumn = new VOTableField("boolean column", "boolean");
        booleanColumn.id = "booleanColumn.id";
        booleanColumn.ucd = "booleanColumn.ucd";
        booleanColumn.unit = "booleanColumn.unit";
        booleanColumn.utype = "booleanColumn.utype";
        booleanColumn.xtype = null;
        booleanColumn.description = "boolean column";
        fields.add(booleanColumn);

        VOTableField byteArrayColumn = new VOTableField("byte[] column", "unsignedByte");
        byteArrayColumn.id = "byteArrayColumn.id";
        byteArrayColumn.ucd = "byteArrayColumn.ucd";
        byteArrayColumn.unit = "byteArrayColumn.unit";
        byteArrayColumn.utype = "byteArrayColumn.utype";
        byteArrayColumn.xtype = null;
        byteArrayColumn.setVariableSize(true);
        byteArrayColumn.description = "byte[] column";
        fields.add(byteArrayColumn);

        VOTableField byteColumn = new VOTableField("byte column", "unsignedByte");
        byteColumn.id = "byteColumn.id";
        byteColumn.ucd = "byteColumn.ucd";
        byteColumn.unit = "byteColumn.unit";
        byteColumn.utype = "byteColumn.utype";
        byteColumn.xtype = null;
        byteColumn.description = "byte column";
        fields.add(byteColumn);

        VOTableField doubleArrayColumn = new VOTableField("double[] column", "double");
        doubleArrayColumn.id = "doubleArrayColumn.id";
        doubleArrayColumn.ucd = "doubleArrayColumn.ucd";
        doubleArrayColumn.unit = "doubleArrayColumn.unit";
        doubleArrayColumn.utype = "doubleArrayColumn.utype";
        doubleArrayColumn.xtype = null;
        doubleArrayColumn.setVariableSize(true);
        doubleArrayColumn.description = "double[] column";
        fields.add(doubleArrayColumn);

        VOTableField doubleColumn = new VOTableField("double column", "double");
        doubleColumn.id = "doubleColumn.id";
        doubleColumn.ucd = "doubleColumn.ucd";
        doubleColumn.unit = "doubleColumn.unit";
        doubleColumn.utype = "doubleColumn.utype";
        doubleColumn.xtype = null;
        doubleColumn.description = "double column";
        fields.add(doubleColumn);

        VOTableField floatArrayColumn = new VOTableField("float[] column", "float");
        floatArrayColumn.id = "floatArrayColumn.id";
        floatArrayColumn.ucd = "floatArrayColumn.ucd";
        floatArrayColumn.unit = "floatArrayColumn.unit";
        floatArrayColumn.utype = "floatArrayColumn.utype";
        floatArrayColumn.xtype = null;
        floatArrayColumn.setVariableSize(true);
        floatArrayColumn.description = "float[] column";
        fields.add(floatArrayColumn);

        VOTableField floatColumn = new VOTableField("float column", "float");
        floatColumn.id = "floatColumn.id";
        floatColumn.ucd = "floatColumn.ucd";
        floatColumn.unit = "floatColumn.unit";
        floatColumn.utype = "floatColumn.utype";
        floatColumn.xtype = null;
        floatColumn.description = "float column";
        fields.add(floatColumn);

        VOTableField intArrayColumn = new VOTableField("int[] column", "int");
        intArrayColumn.id = "intArrayColumn.id";
        intArrayColumn.ucd = "intArrayColumn.ucd";
        intArrayColumn.unit = "intArrayColumn.unit";
        intArrayColumn.utype = "intArrayColumn.utype";
        intArrayColumn.xtype = null;
        intArrayColumn.setVariableSize(true);
        intArrayColumn.description = "int[] column";
        fields.add(intArrayColumn);

        VOTableField integerColumn = new VOTableField("int column", "int");
        integerColumn.id = "integerColumn.id";
        integerColumn.ucd = "integerColumn.ucd";
        integerColumn.unit = "integerColumn.unit";
        integerColumn.utype = "integerColumn.utype";
        integerColumn.xtype = null;
        integerColumn.description = "float column";
        fields.add(integerColumn);

        VOTableField longArrayColumn = new VOTableField("long[] column", "long");
        longArrayColumn.id = "longArrayColumn.id";
        longArrayColumn.ucd = "longArrayColumn.ucd";
        longArrayColumn.unit = "longArrayColumn.unit";
        longArrayColumn.utype = "longArrayColumn.utype";
        longArrayColumn.xtype = null;
        longArrayColumn.setVariableSize(true);
        longArrayColumn.description = "long[] column";
        fields.add(longArrayColumn);

        VOTableField longColumn = new VOTableField("long column", "long");
        longColumn.id = "longColumn.id";
        longColumn.ucd = "longColumn.ucd";
        longColumn.unit = "longColumn.unit";
        longColumn.utype = "longColumn.utype";
        longColumn.xtype = null;
        longColumn.description = "long column";
        fields.add(longColumn);

        VOTableField shortArrayColumn = new VOTableField("short[] column", "short");
        shortArrayColumn.id = "shortArrayColumn.id";
        shortArrayColumn.ucd = "shortArrayColumn.ucd";
        shortArrayColumn.unit = "shortArrayColumn.unit";
        shortArrayColumn.utype = "shortArrayColumn.utype";
        shortArrayColumn.xtype = null;
        shortArrayColumn.setVariableSize(true);
        shortArrayColumn.description = "short[] column";
        fields.add(shortArrayColumn);

        VOTableField shortColumn = new VOTableField("short column", "short");
        shortColumn.id = "shortColumn.id";
        shortColumn.ucd = "shortColumn.ucd";
        shortColumn.unit = "shortColumn.unit";
        shortColumn.utype = "shortColumn.utype";
        shortColumn.xtype = null;
        shortColumn.description = "short column";
        fields.add(shortColumn);

        VOTableField charColumn = new VOTableField("char column", "char");
        charColumn.id = "charColumn.id";
        charColumn.ucd = "charColumn.ucd";
        charColumn.unit = "charColumn.unit";
        charColumn.utype = "charColumn.utype";
        charColumn.xtype = null;
        charColumn.setVariableSize(true);
        charColumn.description = "char column";
        fields.add(charColumn);

        VOTableField dateColumn = new VOTableField("date column", "char");
        dateColumn.id = "dateColumn.id";
        dateColumn.ucd = "dateColumn.ucd";
        dateColumn.unit = "dateColumn.unit";
        dateColumn.utype = "dateColumn.utype";
        dateColumn.xtype = "adql:TIMESTAMP";
        dateColumn.setVariableSize(true);
        dateColumn.description = "date column";
        fields.add(dateColumn);

        VOTableField pointColumn = new VOTableField("point column", "char");
        pointColumn.id = "pointColumn.id";
        pointColumn.ucd = "pointColumn.ucd";
        pointColumn.unit = "pointColumn.unit";
        pointColumn.utype = "pointColumn.utype";
        pointColumn.xtype = "adql:POINT";
        pointColumn.setVariableSize(true);
        pointColumn.description = "point column";
        fields.add(pointColumn);

        VOTableField regionColumn = new VOTableField("region column", "char");
        regionColumn.id = "regionColumn.id";
        regionColumn.ucd = "regionColumn.ucd";
        regionColumn.unit = "regionColumn.unit";
        regionColumn.utype = "regionColumn.utype";
        regionColumn.xtype = "adql:REGION";
        regionColumn.setVariableSize(true);
        regionColumn.description = "region column";
        fields.add(regionColumn);

        VOTableField idColumn = new VOTableField("id column", "char");
        idColumn.id = "someID";
        idColumn.ucd = "idColumn.ucd";
        idColumn.unit = "idColumn.unit";
        idColumn.utype = "idColumn.utype";
        idColumn.setVariableSize(true);
        idColumn.description = "id column";
        fields.add(idColumn);
        
        return fields;
    }
    
    public static class TestTableData implements TableData
    {
        List<List<Object>> fields;

        public TestTableData() throws Exception
        {
            fields = new ArrayList<List<Object>>();

            List<Object> row1 = new ArrayList<Object>();
            row1.add(Boolean.TRUE);
            row1.add(new byte[] {1, 2});
            row1.add(new Byte("1"));
            row1.add(new double[] {3.3, 4.4});
            row1.add(new Double("5.5"));
            row1.add(new float[] {6.6f, 7.7f});
            row1.add(new Float("8.8"));
            row1.add(new int[] {9, 10});
            row1.add(new Integer("11"));
            row1.add(new long[] {12l, 13l});
            row1.add(new Long("14"));
            row1.add(new short[] {15, 16});
            row1.add(new Short("17"));
            row1.add("string value");
            row1.add(dateFormat.parse(DATE_TIME));
            row1.add(new Position(Frame.ICRS, ReferencePosition.BARYCENTER, Flavor.SPHERICAL2, 1.0, 2.0));
            row1.add(new Circle(Frame.ICRS, ReferencePosition.GEOCENTER, Flavor.SPHERICAL2, 1.0, 2.0, 3.0));
            row1.add("foo:bar/baz");
            fields.add(row1);
            fields.add(row1);
            fields.add(row1);
            fields.add(row1);
            fields.add(row1);

            List<Object> row2 = new ArrayList<Object>();
            row2.add(Boolean.FALSE);
            for (int i = 1; i<row1.size(); i++)
            {
                row2.add(null);
            }
            fields.add(row2);
        }

        public Iterator<List<Object>> iterator()
        {
            return fields.iterator();
        }

    }

}
