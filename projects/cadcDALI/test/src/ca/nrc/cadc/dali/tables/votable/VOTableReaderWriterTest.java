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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
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
    
    public VOTableReaderWriterTest() { }

    @BeforeClass
    public static void setUpClass() throws Exception
    {
        Log4jInit.setLevel("ca.nrc.cadc.dali.tables", Level.INFO);
        dateFormat = DateUtil.getDateFormat(DateUtil.IVOA_DATE_FORMAT, DateUtil.UTC);
    }

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
            
            vr = new VOTableResource("results");
            expected.getResources().add(vr);
            vr.setName(resourceName);
            VOTableData vot = new VOTableData();
            vr.setTable(vot);

            // Add INFO's.
            vot.getInfos().addAll(getTestInfos());

            // Add TableFields.
            vot.getParams().addAll(getTestParams());
            vot.getFields().addAll(getTestFields());

            // Add TableData.
            vot.setTableData(new TestTableData());

            // Write VOTable to xml.
            StringWriter sw = new StringWriter();
            VOTableWriter writer = new VOTableWriter();
            writer.write(expected, sw);
            String xml = sw.toString();
            log.debug("XML: \n\n" + xml);

            // Read in xml to VOTable with schema validation.
            VOTableReader reader = new VOTableReader();
            VOTableDocument actual = reader.read(xml);
            
            log.debug("Expected:\n\n" + expected);
            log.debug("Actual:\n\n" + actual);

            // Compare VOTAble's.
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
            expected.getResources().add(vr);
            vr.setName(resourceName);

            VOTableData vot = new VOTableData();
            vr.setTable(vot);
            
            // Add INFO's.
            vot.getInfos().addAll(getTestInfos());

            // Add TableFields.
            vot.getParams().addAll(getTestParams());
            vot.getFields().addAll(getTestFields());

            // Add TableData.
            vot.setTableData(new TestTableData());

            // Write VOTable to xml.
            StringWriter sw = new StringWriter();
            VOTableWriter writer = new VOTableWriter();
            writer.write(expected, sw, 3L);
            String xml = sw.toString();
            log.debug("Expected XML: \n\n" + xml);

            // Read in xml to VOTable with schema validation.
            VOTableReader reader = new VOTableReader();
            VOTableDocument actual = reader.read(xml);
            
            log.debug("Expected:\n\n" + expected);
            log.debug("Actual:\n\n" + actual);

            // Compare VOTAble's.
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
        // RESOURCE name
        Assert.assertEquals(expected.getName(), actual.getName());

        compareInfos(expected.getInfos(), actual.getInfos());

        compareParams(expected.getParams(), actual.getParams());

        compareTables(expected.getTable(), actual.getTable(), actualMax);
        
        
    }
    
    public void compareTables(VOTableData expected, VOTableData actual, Long actualMax)
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
        Long iteratorCount = 0L;
        while (expectedIter.hasNext())
        {
            log.debug("iteratorCount: " + (iteratorCount));

            List<Object> expectedList = expectedIter.next();
            log.debug("Next expected row: " + expectedList);
            
            if (actualMax != null && iteratorCount >= actualMax)
            {
                Assert.assertTrue("Should have reached max.", !actualIter.hasNext());
            }
            else
            {
                iteratorCount++;
            
                Assert.assertTrue("Missing " + expectedList, actualIter.hasNext());
                List<Object> actualList = actualIter.next();
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
        }
        
        if (actualMax != null)
            Assert.assertEquals("wrong number of iterations", actualMax, iteratorCount);
    }

    public void compareInfos(List<Info>  expected, List<Info> actual)
    {
        // INFO
        Assert.assertNotNull(expected);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++)
        {
            Info expectedInfo = expected.get(i);
            Info actualInfo = actual.get(i);
            Assert.assertNotNull(expectedInfo);
            Assert.assertNotNull(actualInfo);
            Assert.assertEquals(expectedInfo.getName(), actualInfo.getName());
            Assert.assertEquals(expectedInfo.getValue(), actualInfo.getValue());
        }
    }
    public void compareParams(List<TableParam>  expected, List<TableParam> actual)
    {
        // PARAM
        Assert.assertNotNull(expected);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++)
        {
            TableParam expectedParam = expected.get(i);
            TableParam actualParam = actual.get(i);
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
            Assert.assertEquals(expectedParam.arraysize, actualParam.arraysize);
            Assert.assertEquals(expectedParam.variableSize, actualParam.variableSize);
            Assert.assertEquals(expectedParam.description, actualParam.description);
            List<String> expectedValues = expectedParam.values;
            List<String> actualValues = actualParam.values;
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
    public void compareFields(List<TableField>  expected, List<TableField> actual)
    {
        // FIELD
        Assert.assertNotNull(expected);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++)
        {
            TableField expectedField = expected.get(i);
            TableField actualField = actual.get(i);
            Assert.assertNotNull(expectedField);
            Assert.assertNotNull(actualField);
            Assert.assertEquals(expectedField.getName(), actualField.getName());
            Assert.assertEquals(expectedField.getDatatype(), expectedField.getDatatype());
            Assert.assertEquals(expectedField.id, actualField.id);
            Assert.assertEquals(expectedField.ucd, actualField.ucd);
            Assert.assertEquals(expectedField.unit, actualField.unit);
            Assert.assertEquals(expectedField.utype, actualField.utype);
            Assert.assertEquals(expectedField.xtype, actualField.xtype);
            Assert.assertEquals(expectedField.arraysize, actualField.arraysize);
            Assert.assertEquals(expectedField.variableSize, actualField.variableSize);
            Assert.assertEquals(expectedField.description, actualField.description);
        }
    }
    protected List<Info> getTestInfos()
    {
        List<Info> infos = new ArrayList<Info>();

        Info ok = new Info("QUERY_STATUS", "OK");
        infos.add(ok);

        Info overflow = new Info("QUERY_STATUS", "OVERFLOW");
        infos.add(overflow);

        return infos;
    }

    protected List<TableParam> getMetaParams()
    {
        List<TableParam> params = new ArrayList<TableParam>();
        params.add(new TableParam("standardID", "char", "ivo://ivoa.net/std/DataLink/1.0"));
        params.add(new TableParam("resourceIdentifier", "char", "ivo://cadc.nrc.ca/datalink"));
        params.add(new TableParam("accessURL", "char", "http://www.cadc.hia.nrc.gc.ca/caom2ops/datalink"));
        TableParam servParam = new TableParam("ID", "char", "");
        servParam.ref = "someID";
        params.add(servParam);
        return params;
    }
    protected List<TableParam> getTestParams()
    {
        List<TableParam> params = new ArrayList<TableParam>();
        
        TableParam intersects = new TableParam("INPUT:INTERSECTS", "char", "OVERLAPS");
        intersects.id = null;
        intersects.ucd = null;
        intersects.unit = null;
        intersects.utype = null;
        intersects.xtype = null;
        intersects.arraysize = null;
        intersects.variableSize = true;
        intersects.description = null;
        intersects.values = new ArrayList<String>();
        intersects.values.add("ALL");
        intersects.values.add("BLAST");
        intersects.values.add("CFHT");
        intersects.values.add("HST");
        intersects.values.add("JCMT");
        params.add(intersects);

        TableParam collection = new TableParam("INPUT:COLLECTION", "char", "ALL");
        collection.id = null;
        collection.ucd = null;
        collection.unit = null;
        collection.utype = null;
        collection.xtype = null;
        collection.arraysize = null;
        collection.variableSize = true;
        collection.description = null;
        params.add(collection);

        return params;
    }

    protected List<TableField> getTestFields()
    {
        List<TableField> fields = new ArrayList<TableField>();

        // Add TableFields.
        TableField booleanColumn = new TableField("boolean column", "boolean");
        booleanColumn.id = "booleanColumn.id";
        booleanColumn.ucd = "booleanColumn.ucd";
        booleanColumn.unit = "booleanColumn.unit";
        booleanColumn.utype = "booleanColumn.utype";
        booleanColumn.xtype = null;
        booleanColumn.arraysize = null;
        booleanColumn.variableSize = null;
        booleanColumn.description = "boolean column";
        fields.add(booleanColumn);

        TableField byteArrayColumn = new TableField("byte[] column", "unsignedByte");
        byteArrayColumn.id = "byteArrayColumn.id";
        byteArrayColumn.ucd = "byteArrayColumn.ucd";
        byteArrayColumn.unit = "byteArrayColumn.unit";
        byteArrayColumn.utype = "byteArrayColumn.utype";
        byteArrayColumn.xtype = null;
        byteArrayColumn.arraysize = null;
        byteArrayColumn.variableSize = true;
        byteArrayColumn.description = "byte[] column";
        fields.add(byteArrayColumn);

        TableField byteColumn = new TableField("byte column", "unsignedByte");
        byteColumn.id = "byteColumn.id";
        byteColumn.ucd = "byteColumn.ucd";
        byteColumn.unit = "byteColumn.unit";
        byteColumn.utype = "byteColumn.utype";
        byteColumn.xtype = null;
        byteColumn.arraysize = null;
        byteColumn.variableSize = null;
        byteColumn.description = "byte column";
        fields.add(byteColumn);

        TableField doubleArrayColumn = new TableField("double[] column", "double");
        doubleArrayColumn.id = "doubleArrayColumn.id";
        doubleArrayColumn.ucd = "doubleArrayColumn.ucd";
        doubleArrayColumn.unit = "doubleArrayColumn.unit";
        doubleArrayColumn.utype = "doubleArrayColumn.utype";
        doubleArrayColumn.xtype = null;
        doubleArrayColumn.arraysize = null;
        doubleArrayColumn.variableSize = true;
        doubleArrayColumn.description = "double[] column";
        fields.add(doubleArrayColumn);

        TableField doubleColumn = new TableField("double column", "double");
        doubleColumn.id = "doubleColumn.id";
        doubleColumn.ucd = "doubleColumn.ucd";
        doubleColumn.unit = "doubleColumn.unit";
        doubleColumn.utype = "doubleColumn.utype";
        doubleColumn.xtype = null;
        doubleColumn.arraysize = null;
        doubleColumn.variableSize = null;
        doubleColumn.description = "double column";
        fields.add(doubleColumn);

        TableField floatArrayColumn = new TableField("float[] column", "float");
        floatArrayColumn.id = "floatArrayColumn.id";
        floatArrayColumn.ucd = "floatArrayColumn.ucd";
        floatArrayColumn.unit = "floatArrayColumn.unit";
        floatArrayColumn.utype = "floatArrayColumn.utype";
        floatArrayColumn.xtype = null;
        floatArrayColumn.arraysize = null;
        floatArrayColumn.variableSize = true;
        floatArrayColumn.description = "float[] column";
        fields.add(floatArrayColumn);

        TableField floatColumn = new TableField("float column", "float");
        floatColumn.id = "floatColumn.id";
        floatColumn.ucd = "floatColumn.ucd";
        floatColumn.unit = "floatColumn.unit";
        floatColumn.utype = "floatColumn.utype";
        floatColumn.xtype = null;
        floatColumn.arraysize = null;
        floatColumn.variableSize = null;
        floatColumn.description = "float column";
        fields.add(floatColumn);

        TableField intArrayColumn = new TableField("int[] column", "int");
        intArrayColumn.id = "intArrayColumn.id";
        intArrayColumn.ucd = "intArrayColumn.ucd";
        intArrayColumn.unit = "intArrayColumn.unit";
        intArrayColumn.utype = "intArrayColumn.utype";
        intArrayColumn.xtype = null;
        intArrayColumn.arraysize = null;
        intArrayColumn.variableSize = true;
        intArrayColumn.description = "int[] column";
        fields.add(intArrayColumn);

        TableField integerColumn = new TableField("int column", "int");
        integerColumn.id = "integerColumn.id";
        integerColumn.ucd = "integerColumn.ucd";
        integerColumn.unit = "integerColumn.unit";
        integerColumn.utype = "integerColumn.utype";
        integerColumn.xtype = null;
        integerColumn.arraysize = null;
        integerColumn.variableSize = null;
        integerColumn.description = "float column";
        fields.add(integerColumn);

        TableField longArrayColumn = new TableField("long[] column", "long");
        longArrayColumn.id = "longArrayColumn.id";
        longArrayColumn.ucd = "longArrayColumn.ucd";
        longArrayColumn.unit = "longArrayColumn.unit";
        longArrayColumn.utype = "longArrayColumn.utype";
        longArrayColumn.xtype = null;
        longArrayColumn.arraysize = null;
        longArrayColumn.variableSize = true;
        longArrayColumn.description = "long[] column";
        fields.add(longArrayColumn);

        TableField longColumn = new TableField("long column", "long");
        longColumn.id = "longColumn.id";
        longColumn.ucd = "longColumn.ucd";
        longColumn.unit = "longColumn.unit";
        longColumn.utype = "longColumn.utype";
        longColumn.xtype = null;
        longColumn.arraysize = null;
        longColumn.variableSize = null;
        longColumn.description = "long column";
        fields.add(longColumn);

        TableField shortArrayColumn = new TableField("short[] column", "short");
        shortArrayColumn.id = "shortArrayColumn.id";
        shortArrayColumn.ucd = "shortArrayColumn.ucd";
        shortArrayColumn.unit = "shortArrayColumn.unit";
        shortArrayColumn.utype = "shortArrayColumn.utype";
        shortArrayColumn.xtype = null;
        shortArrayColumn.arraysize = null;
        shortArrayColumn.variableSize = true;
        shortArrayColumn.description = "short[] column";
        fields.add(shortArrayColumn);

        TableField shortColumn = new TableField("short column", "short");
        shortColumn.id = "shortColumn.id";
        shortColumn.ucd = "shortColumn.ucd";
        shortColumn.unit = "shortColumn.unit";
        shortColumn.utype = "shortColumn.utype";
        shortColumn.xtype = null;
        shortColumn.arraysize = null;
        shortColumn.variableSize = null;
        shortColumn.description = "short column";
        fields.add(shortColumn);

        TableField charColumn = new TableField("char column", "char");
        charColumn.id = "charColumn.id";
        charColumn.ucd = "charColumn.ucd";
        charColumn.unit = "charColumn.unit";
        charColumn.utype = "charColumn.utype";
        charColumn.xtype = null;
        charColumn.arraysize = null;
        charColumn.variableSize = true;
        charColumn.description = "char column";
        fields.add(charColumn);

        TableField dateColumn = new TableField("date column", "char");
        dateColumn.id = "dateColumn.id";
        dateColumn.ucd = "dateColumn.ucd";
        dateColumn.unit = "dateColumn.unit";
        dateColumn.utype = "dateColumn.utype";
        dateColumn.xtype = "adql:TIMESTAMP";
        dateColumn.arraysize = null;
        dateColumn.variableSize = true;
        dateColumn.description = "date column";
        fields.add(dateColumn);

        TableField pointColumn = new TableField("point column", "char");
        pointColumn.id = "pointColumn.id";
        pointColumn.ucd = "pointColumn.ucd";
        pointColumn.unit = "pointColumn.unit";
        pointColumn.utype = "pointColumn.utype";
        pointColumn.xtype = "adql:POINT";
        pointColumn.arraysize = null;
        pointColumn.variableSize = true;
        pointColumn.description = "point column";
        fields.add(pointColumn);

        TableField regionColumn = new TableField("region column", "char");
        regionColumn.id = "regionColumn.id";
        regionColumn.ucd = "regionColumn.ucd";
        regionColumn.unit = "regionColumn.unit";
        regionColumn.utype = "regionColumn.utype";
        regionColumn.xtype = "adql:REGION";
        regionColumn.arraysize = null;
        regionColumn.variableSize = true;
        regionColumn.description = "region column";
        fields.add(regionColumn);

        TableField idColumn = new TableField("id column", "char");
        idColumn.id = "someID";
        idColumn.ucd = "idColumn.ucd";
        idColumn.unit = "idColumn.unit";
        idColumn.utype = "idColumn.utype";
        idColumn.xtype = "adql:REGION";
        idColumn.arraysize = null;
        idColumn.variableSize = true;
        idColumn.description = "id column";
        fields.add(idColumn);
        
        return fields;
    }
    
    public class TestTableData implements TableData
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
            fields.add(row1);
            fields.add(row1);
            fields.add(row1);
            fields.add(row1);
            fields.add(row1);

            List<Object> row2 = new ArrayList<Object>();
            row2.add(null);
            row2.add(null);
            row2.add(null);
            row2.add(null);
            row2.add(null);
            row2.add(null);
            row2.add(null);
            row2.add(null);
            row2.add(null);
            row2.add(null);
            row2.add(null);
            row2.add(null);
            row2.add(null);
            row2.add(null);
            row2.add(null);
            row2.add(null);
            row2.add(null);
            fields.add(row2);
        }

        public Iterator<List<Object>> iterator()
        {
            return fields.iterator();
        }

    }

}
