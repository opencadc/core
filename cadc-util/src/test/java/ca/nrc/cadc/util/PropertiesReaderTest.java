/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2011.                            (c) 2011.
 * National Research Council            Conseil national de recherches
 * Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 * All rights reserved                  Tous droits reserves
 *
 * NRC disclaims any warranties         Le CNRC denie toute garantie
 * expressed, implied, or statu-        enoncee, implicite ou legale,
 * tory, of any kind with respect       de quelque nature que se soit,
 * to the software, including           concernant le logiciel, y com-
 * without limitation any war-          pris sans restriction toute
 * ranty of merchantability or          garantie de valeur marchande
 * fitness for a particular pur-        ou de pertinence pour un usage
 * pose.  NRC shall not be liable       particulier.  Le CNRC ne
 * in any event for any damages,        pourra en aucun cas etre tenu
 * whether direct or indirect,          responsable de tout dommage,
 * special or general, consequen-       direct ou indirect, particul-
 * tial or incidental, arising          ier ou general, accessoire ou
 * from the use of the software.        fortuit, resultant de l'utili-
 *                                      sation du logiciel.
 *
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */

package ca.nrc.cadc.util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;


public class PropertiesReaderTest
{

    private static final String TEST_PROPERTIES =
            "prop1 = value1\n" +
            "prop2 = value2a\n" +
            "prop2 = value2b";

    static
    {
        Log4jInit.setLevel("ca.nrc.cadc", org.apache.log4j.Level.INFO);
    }

    @Test
    public void testResilientProperties() throws Exception
    {

        File propFile = new File("test.properties");
        try
        {
            System.setProperty(PropertiesReader.class.getName() + ".dir", "./");
            if (!propFile.exists())
                propFile.createNewFile();

            FileOutputStream out = new FileOutputStream(propFile);
            out.write(TEST_PROPERTIES.getBytes("UTF-8"));
            out.close();

            // get the properties
            PropertiesReader propReader = new PropertiesReader("test.properties");
            List<String> prop1 = propReader.getPropertyValues("prop1");
            Assert.assertEquals("missing prop 1, value 1", "value1",
                                prop1.get(0));
            List<String> prop2 = propReader.getPropertyValues("prop2");
            Assert.assertEquals("missing prop 2, value a", "value2a",
                                prop2.get(0));
            Assert.assertEquals("missing prop 2, value b", "value2b",
                                prop2.get(1));

            // delete the test properties file and ensure the properties are
            // still available
            propFile.delete();

            // get the properties from inTwo (should be saved from first config)
            prop1 = propReader.getPropertyValues("prop1");
            Assert.assertEquals("missing prop 1, value 1", "value1",
                                prop1.get(0));
            prop2 = propReader.getPropertyValues("prop2");
            Assert.assertEquals("missing prop 2, value a", "value2a",
                                prop2.get(0));
            Assert.assertEquals("missing prop 2, value b", "value2b",
                                prop2.get(1));

            String prop3 = propReader.getFirstPropertyValue("prop2");
            Assert.assertEquals("missing prop 2, value a", "value2a", prop3);
        }
        finally
        {
            System.setProperty(PropertiesReader.class.getName() + ".dir", "");
            // cleanup
            if (propFile.exists())
                propFile.delete();
        }
    }

    @Test
    public void testAllowEmptyPropertiesFile() throws Exception
    {

        File propFile = new File("test.properties");
        try
        {
            System.setProperty(PropertiesReader.class.getName() + ".dir", "./");
            if (!propFile.exists())
                propFile.createNewFile();

            // get the properties
            PropertiesReader propReader = new PropertiesReader("test.properties");
            List<String> prop1 = propReader.getPropertyValues("prop1");
            Assert.assertNull("should have been null", prop1);
        }
        finally
        {
            System.setProperty(PropertiesReader.class.getName() + ".dir", "");
            // cleanup
            if (propFile.exists())
                propFile.delete();
        }
    }
}
