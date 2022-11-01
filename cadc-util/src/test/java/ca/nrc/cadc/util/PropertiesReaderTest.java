/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2016.                            (c) 2016.
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
import java.nio.file.Files;
import java.nio.file.Path;
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
        Log4jInit.setLevel("ca.nrc.cadc.util", org.apache.log4j.Level.DEBUG);
    }
    
    String getTestConfigDir() { 
        return System.getProperty("user.dir") + "/build/tmp";
    }

    @Test
    public void testResilientProperties() throws Exception
    {

        File propFile = new File(getTestConfigDir(), "testResilientProperties.properties");
        try
        {
            System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, getTestConfigDir());
            if (!propFile.exists())
                propFile.createNewFile();

            FileOutputStream out = new FileOutputStream(propFile);
            out.write(TEST_PROPERTIES.getBytes("UTF-8"));
            out.close();

            // get the properties
            PropertiesReader propReader = new PropertiesReader(propFile.getName());
            MultiValuedProperties mvp  = propReader.getAllProperties();
            Assert.assertNotNull(mvp);
            Assert.assertFalse("not empty", mvp.isEmpty());
            
            List<String> prop1 = mvp.getProperty("prop1");
            Assert.assertEquals("missing prop 1, value 1", "value1", prop1.get(0));
            List<String> prop2 = mvp.getProperty("prop2");
            Assert.assertEquals("missing prop 2, value a", "value2a", prop2.get(0));
            Assert.assertEquals("missing prop 2, value b", "value2b", prop2.get(1));

            // delete the test properties file and ensure the properties are
            // still available
            propFile.delete();

            // get the properties from inTwo (should be saved from first config)
            mvp  = propReader.getAllProperties();
            Assert.assertFalse("not empty", mvp.isEmpty());
            
            prop1 = mvp.getProperty("prop1");
            Assert.assertEquals("missing prop 1, value 1", "value1", prop1.get(0));
            prop2 = mvp.getProperty("prop2");
            Assert.assertEquals("missing prop 2, value a", "value2a", prop2.get(0));
            Assert.assertEquals("missing prop 2, value b", "value2b", prop2.get(1));

            String prop3 = mvp.getFirstPropertyValue("prop2");
            Assert.assertEquals("missing prop 2, value a", "value2a", prop3);
        }
        finally
        {
            System.clearProperty(PropertiesReader.class.getName() + ".dir");
            // cleanup
            if (propFile.exists())
                propFile.delete();
        }
    }

    @Test
    public void testAllowEmptyPropertiesFile() throws Exception
    {

        File propFile = new File(getTestConfigDir(), "testAllowEmptyPropertiesFile.properties");
        try
        {
            System.setProperty(PropertiesReader.class.getName() + ".dir", getTestConfigDir());
            if (!propFile.exists())
                propFile.createNewFile();

            // get the properties
            PropertiesReader propReader = new PropertiesReader(propFile.getName());
            List<String> prop1 = propReader.getPropertyValues("prop1");
            Assert.assertTrue("should have been empty", prop1.isEmpty());
        }
        finally
        {
            System.clearProperty(PropertiesReader.class.getName() + ".dir");
            // cleanup
            if (propFile.exists())
                propFile.delete();
        }
    }

    /**
     * Creating a file that actually cannot be read is problematic across systems, so just override the public method
     * here instead to emulate it.
     */
    @Test
    public void testCanNotRead() throws Exception {
        System.setProperty(PropertiesReader.class.getName() + ".dir", getTestConfigDir());
        final String fileName = "unReadableFile.properties";
        final Path path = new File(getTestConfigDir(), fileName).toPath();
        Files.deleteIfExists(path);
        Files.createFile(path);

        try {
            PropertiesReader r = new PropertiesReader(fileName) {
                @Override
                public boolean canRead() {
                    return false;
                }
            };
            MultiValuedProperties mvp = r.getAllProperties();
            Assert.assertNotNull(mvp);
            Assert.assertTrue(mvp.isEmpty());
        } finally {
            System.clearProperty(PropertiesReader.class.getName() + ".dir");
        }
    }

    @Test
    public void testDoesNotExist() {
        System.setProperty(PropertiesReader.class.getName() + ".dir", getTestConfigDir());
        try {
            PropertiesReader r = new PropertiesReader("BOGUSFILE.nope");
            MultiValuedProperties mvp = r.getAllProperties();
            Assert.assertNotNull(mvp);
            Assert.assertTrue(mvp.isEmpty());
        } finally {
            System.clearProperty(PropertiesReader.class.getName() + ".dir");
        }
    }

    @Test
    public void testCanRead() throws Exception {
        final File tmpDir = new File(getTestConfigDir());
        final File propFile = File.createTempFile("testCanRead", ".tmp", tmpDir);

        try {
            System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, getTestConfigDir());
            final PropertiesReader testSubject = new PropertiesReader(propFile.getName());
            Assert.assertTrue("Should be able to read.", testSubject.canRead());
        } finally {
            System.clearProperty(PropertiesReader.class.getName() + ".dir");
            if (propFile.exists()) {
                propFile.delete();
            }
        }
    }
}
