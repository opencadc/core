package ca.nrc.cadc.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Assert;
import junit.framework.TestCase;
import ca.nrc.cadc.util.XmlConfigReader;

/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

/**
 * @author goliaths
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class XmlConfigReaderTest extends TestCase
{

    private static final String CONFIG_DIR = "/tmp/";
    //    "/usr/cadcdev/cadc/current/develop/javaUtil/test/config/";

    private static final String DB_NAME = "cadctemp";

    protected void setUp()
    {
        // dssc library configuration file
        //
        try
        {

            BufferedWriter cf = new BufferedWriter(new FileWriter(new File(CONFIG_DIR, "libhid.config")));

            cf.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            cf.write("<configuration>\n");
            cf.write("			<entry key = \"DATABASE\" value = \"gemini\"/>\n");
            cf.write("			<if name = \"GEMINI\">\n");
            cf.write("				<entry key = \"WIDE\" value = \"hit_wide\"/>\n");
            cf.write("				<entry key = \"NARROW_INT\" value = \"hit_int\"/>\n");
            cf.write("				<entry key = \"NARROW_TEXT\" value = \"hit_tex \"/>\n");
            cf.write("				<entry key = \"NARROW_DATETIME\" value = \"hit_datetime\"/>\n");
            cf.write("				<entry key = \"NARROW_FLOAT\" value = \"hit_float\"/>\n");
            cf.write("				<entry key = \"STRINGTYPE\" value = \"something representing a string\"/>\n");
            cf.write("				<entry key = \"INTTYPE\" value = \"42\"/>\n");
            cf.write("				<entry key = \"DOUBLETYPE\" value = \"3.1415\"/>\n");
            cf.write("				<entry key = \"BOOLEANTYPE\" value = \"true\"/>\n");
            cf.write("		  <struct name = \"CONFIG\">\n");
            cf.write("				<entry key=\"DD\" value=\"hid_dd\" />\n");
            cf.write("				<entry key=\"DDTABLE\" value=\"hid_ddtable\" />\n");
            cf.write("				<entry key=\"FORMAT\" value=\"%Y-%m-%dT%H:%M:%S\"/>\n");
            cf.write("				<entry key=\"IFMKEYWORD\" value=\"hid_ifmkeyword\" />\n");
            cf.write("				<entry key=\"JOINKEY\" value=\"DATE\" />\n");
            cf.write("		  	<map key=\"COLUMNS\">\n");
            cf.write("					<entry key=\"STRUCT_COLUMN1\" value=\"column1\"/>\n");
            cf.write("					<entry key=\"STRUCT_COLUMN2\" value=\"column2\" />\n");
            cf.write("					<entry key=\"STRUCT_COLUMN3\" value=\"column3\" />\n");
            cf.write("    		</map>\n");
            cf.write("		  </struct>\n");
            cf.write("		  <map key=\"COLUMNS\">\n");
            cf.write("				<entry key=\"COLUMN1\" value=\"attribute\"/>\n");
            cf.write("				<entry key=\"COLUMN2\" value=\"provenance\" />\n");
            cf.write("				<entry key=\"COLUMN3\" value=\"value\" />\n");
            cf.write("   	 </map>\n");
            cf.write("				<entry key=\"stuff\" value=\"cfht\" />\n");
            cf.write("			</if>\n");
            cf.write("			<if name = \"CFHT\">\n");
            cf.write("				<entry key = \"DATABASE\" value = \"cfht\"/>\n");
            cf.write("				<entry key = \"DD\" value = \"cfht_wide\"/>\n");
            cf.write("				<entry key = \"DDTABLE\" value = \"cfht_int\"/>\n");
            cf.write("				<entry key = \"JOINKEY\" value = \"cfht_text\"/>\n");
            cf.write("			</if>\n");
            cf.write("			<list key = \"STRING ARRAY TYPE\">\n");
            cf.write("			  <value> one </value>\n");
            cf.write("			  <value> two </value>\n");
            cf.write("			  <value> three </value>\n");
            cf.write("			</list>\n");
            cf.write("			<list key = \"INT ARRAY TYPE\">\n");
            cf.write("			  <value> 12 </value>\n");
            cf.write("			  <value> 13 </value>\n");
            cf.write("			  <value> 14 </value>\n");
            cf.write("			</list>\n");
            cf.write("			<list key = \"BOOLEAN ARRAY TYPE\">\n");
            cf.write("			  <value> true </value>\n");
            cf.write("			  <value> false </value>\n");
            cf.write("			<value> true </value>\n");
            cf.write("		  </list>\n");
            cf.write("		  <list key = \"DOUBLE ARRAY TYPE\">\n");
            cf.write("			<value> 3.1415 </value>\n");
            cf.write("			<value> 3.0912e+17 </value>\n");
            cf.write("			<value> 579999.2345673827493874 </value>\n");
            cf.write("		  </list>\n");
            cf.write("		  <struct name = \"CONFIG\">\n");
            cf.write("			  <entry key=\"DD\" value=\"config_dd\" />\n");
            cf.write("			  <entry key=\"DDTABLE\" value=\"config_ddtable\" />\n");
            cf.write("			  <entry key=\"FORMAT\" value=\"config%m-%dT%H:%M:%S\" />\n");
            cf.write("			  <entry key=\"IFMKEYWORD\" value=\"config_ifmkeyword\" />\n");
            cf.write("			  <entry key=\"JOINKEY\" value=\"configDATE\" />\n");
            cf.write("		  <list key = \"EMBEDDED LIST\">\n");
            cf.write("			<value> four </value>\n");
            cf.write("			<value> five </value>\n");
            cf.write("			<value> six </value>\n");
            cf.write("		  </list>\n");
            cf.write("		  </struct>\n");
            cf.write("  <entry key = \"STATIC\" value = \"static keyword 1\"/>\n");
            cf.write("  <entry key = \"STATIC2\" value = \"static keyword 2\"/>\n");
            cf.write("        <list key = \"TABLES\">\n");
            cf.write("          <struct name = \"TABLE\">\n");
            cf.write("             <entry key=\"ENTRY1\" value=\"S1\" />\n");
            cf.write("             <entry key=\"ENTRY2\" value=\"S2\" />\n");
            cf.write("             <entry key=\"ENTRY3\" value=\"S3\" />\n");
            cf.write("		  <list key = \"ENTRY4\">\n");
            cf.write("			<value> fourone </value>\n");
            cf.write("			<value> fourtwo </value>\n");
            cf.write("			<value> fourthree </value>\n");
            cf.write("		  </list>\n");
            cf.write("          </struct>\n");
            cf.write("          <struct name = \"TABLE\">\n");
            cf.write("             <entry key=\"ENTRY1\" value=\"S4\" />\n");
            cf.write("             <entry key=\"ENTRY2\" value=\"S5\" />\n");
            cf.write("             <entry key=\"ENTRY3\" value=\"S6\" />\n");
            cf.write("		  <list key = \"ENTRY4\">\n");
            cf.write("			<value> fiveone </value>\n");
            cf.write("			<value> fivetwo </value>\n");
            cf.write("			<value> fivethree </value>\n");
            cf.write("		  </list>\n");
            cf.write("          </struct>\n");
            cf.write("        </list>\n");
            cf.write("        <map key = \"PATTERNS\">\n");
            cf.write("          <struct name = \"DEFAULT\">\n");
            cf.write("             <entry key=\"ENTRY1\" value=\"M1\" />\n");
            cf.write("             <entry key=\"ENTRY2\" value=\"M2\" />\n");
            cf.write("             <entry key=\"ENTRY3\" value=\"M3\" />\n");
            cf.write("        <list key = \"ENTRY4\">\n");
            cf.write("          <value> mapfourone </value>\n");
            cf.write("          <value> mapfourtwo </value>\n");
            cf.write("          <value> mapfourthree </value>\n");
            cf.write("        </list>\n");
            cf.write("          </struct>\n");
            cf.write("          <struct name = \"GMOS\">\n");
            cf.write("             <entry key=\"ENTRY1\" value=\"M4\" />\n");
            cf.write("             <entry key=\"ENTRY2\" value=\"M5\" />\n");
            cf.write("             <entry key=\"ENTRY3\" value=\"M6\" />\n");
            cf.write("        <list key = \"ENTRY4\">\n");
            cf.write("          <value> mapfiveone </value>\n");
            cf.write("          <value> mapfivetwo </value>\n");
            cf.write("          <value> mapfivethree </value>\n");
            cf.write("        </list>\n");
            cf.write("          </struct>\n");
            cf.write("        </map>\n");
            cf.write("</configuration>\n");
            cf.flush();
            cf.close();

            BufferedWriter cf1 = new BufferedWriter(new FileWriter(new File(CONFIG_DIR, "libhid1.config")));

            cf1.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            cf1.write("<configuration>\n");
            cf1.write("			<entry key = \"DATABASE\" value = \"gemini\"/>\n");
            cf1.write("			<if name = \"GEMINI\">\n");
            cf1.write("				<entry key = \"XTRA\" value = \"second config file\"/>\n");
            cf1.write("			</if>\n");
            cf1.write("</configuration>\n");
            cf1.flush();
            cf1.close();
        }
        catch (IOException e)
        {
            System.out.println(e);
        }

    } // end setUp

    protected void tearDown()
    {

    } // end tearDown

    public void testNoSystemProp()
    {
        try
        {
            XmlConfigReader.getConfigFileName("something");
            fail("Should have thrown an exception");
        }
        catch (IllegalArgumentException e)
        {

        }
        catch (Exception e)
        {
            fail("Unexpected exception");
        }
    }

    public void testSpecificDefaultConfigDirLocation()
    {
        System.setProperty(XmlConfigReader.CONFIG_DIR_PROPERTY, CONFIG_DIR);

        //
        // incorrectly formatted library configuration file in
        // default location
        //
        try
        {

            BufferedWriter cf = new BufferedWriter(new FileWriter(new File(CONFIG_DIR + "/libhidtest.config")));

            cf.write("hello");
            cf.flush();
            cf.close();
        }
        catch (IOException e)
        {
            System.out.println(e);
        }

        //		
        // no properties are set, file exists in default location
        //

        XmlConfigReader cr = new XmlConfigReader();
        String configFile = "libhidtest";
        assertEquals(CONFIG_DIR + configFile + ".config", XmlConfigReader.getConfigFileName(configFile));

        //
        // no properties are set, file doesn't exist in default location
        //

        try
        {
            XmlConfigReader.getConfigFileName("invalid");
            assertFalse(true); // don't expect to get here
        }
        catch (IllegalArgumentException e1)
        {

            //
            // expect to get here
            //

        }

        File f = new File(CONFIG_DIR, "libhidtest.config");
        if (!f.delete())
        {
            System.out.println("Clean up your muddy footprints in the kitchen - java-util/libhidtest.config");
            assertFalse(true);
        }

    } // end testSpecificDefaultConfigDirLocation

    public void testObtainConfig()
    {

        AllTypesConfig atc = new AllTypesConfig();
        Config cfg = new Config();
        cfg.setConfigLabel("GEMINI");
        atc.setConfigLabel("GEMINI");
        atc.update("libhid");
        cfg.update("libhid");

        // get flat config - no structures, lists, or ifs
        //		
        Assert.assertEquals(true, atc.booleanType_);
        Assert.assertEquals(3.1415, atc.doubleType_, 0.0005);
        Assert.assertEquals("something representing a string", atc.stringType_);
        Assert.assertEquals(42, atc.intType_);

        Assert.assertEquals("static keyword 1", Config.staticKeyword_);
        Assert.assertEquals("static keyword 2", Config.staticKeyword2_);

        Assert.assertEquals(3, cfg.columns.size());
        Assert.assertEquals("attribute", cfg.columns.get("COLUMN1"));
        Assert.assertEquals("provenance", cfg.columns.get("COLUMN2"));
        Assert.assertEquals("value", cfg.columns.get("COLUMN3"));

        Assert.assertEquals(3, atc.config_.columns.size());
        Assert.assertEquals("column1", atc.config_.columns
                .get("STRUCT_COLUMN1"));
        Assert.assertEquals("column2", atc.config_.columns
                .get("STRUCT_COLUMN2"));
        Assert.assertEquals("column3", atc.config_.columns
                .get("STRUCT_COLUMN3"));

        assertNull(cfg.xtra);

        // update config according to the second config file
        cfg.update("libhid1");

        Assert.assertEquals(3, cfg.columns.size());
        Assert.assertEquals("attribute", cfg.columns.get("COLUMN1"));
        Assert.assertEquals("provenance", cfg.columns.get("COLUMN2"));
        Assert.assertEquals("value", cfg.columns.get("COLUMN3"));
        Assert.assertEquals("second config file", cfg.xtra);

        //		
        // test that "if" element works on the same configuration file, when retrieving
        // some number of entries with no nesting
        //

        cfg.setConfigLabel("CFHT");
        cfg.update("libhid");
        Assert.assertEquals(cfg.database_, "cfht");
        Assert.assertEquals(cfg.dd_, "cfht_wide");
        Assert.assertEquals(cfg.ddTable_, "cfht_int");
        Assert.assertEquals(cfg.joinKey_, "cfht_text");

        //
        // test that list element works in a configuration file, when retrieving a list
        // of values for the same element
        //
        // string list type
        //

        Assert.assertEquals(atc.stringArrayType_[0], "one");
        Assert.assertEquals(atc.stringArrayType_[1], "two");
        Assert.assertEquals(atc.stringArrayType_[2], "three");

        //
        // int list type
        //

        Assert.assertEquals(atc.intArrayType_[0], 12);
        Assert.assertEquals(atc.intArrayType_[1], 13);
        Assert.assertEquals(atc.intArrayType_[2], 14);

        //
        // boolean list type
        //

        Assert.assertEquals(atc.booleanArrayType_[0], true);
        Assert.assertEquals(atc.booleanArrayType_[1], false);
        Assert.assertEquals(atc.booleanArrayType_[2], true);

        //
        // double list type
        //

        Assert.assertEquals(atc.doubleArrayType_[0], 3.1415, 0.0005);
        Assert.assertEquals(atc.doubleArrayType_[1], 3.0912e+17, 1);
        Assert.assertEquals(atc.doubleArrayType_[2], 579999.2345673827493874, 0.00000000001);

        //
        // test that struct element with list elements works in a 
        // configuration file
        //

        atc.setConfigLabel("");
        atc.update("libhid");
        Assert.assertEquals("configDATE", atc.config_.joinKey_);
        Assert.assertEquals("config_ifmkeyword", atc.config_.ifmKeyword_);
        Assert.assertEquals("config_dd", atc.config_.dd_);
        Assert.assertEquals("config_ddtable", atc.config_.ddTable_);
        Assert.assertEquals("config%m-%dT%H:%M:%S", atc.config_.format_);
        Assert.assertEquals(atc.config_.embeddedList[0], "four");
        Assert.assertEquals(atc.config_.embeddedList[1], "five");
        Assert.assertEquals(atc.config_.embeddedList[2], "six");

        StructInList sil = atc.getStructInList("S1");
        assertTrue(sil != null);
        Assert.assertEquals(sil.entry1, "S1");
        Assert.assertEquals(sil.entry2, "S2");
        Assert.assertEquals(sil.entry3, "S3");
        assertTrue(sil.entry4 != null);
        Assert.assertEquals(sil.entry4.get(0), "fourone");
        Assert.assertEquals(sil.entry4.get(1), "fourtwo");
        Assert.assertEquals(sil.entry4.get(2), "fourthree");

        sil = atc.getStructInList("S4");
        assertTrue(sil != null);
        Assert.assertEquals(sil.entry1, "S4");
        Assert.assertEquals(sil.entry2, "S5");
        Assert.assertEquals(sil.entry3, "S6");
        assertTrue(sil.entry4 != null);
        Assert.assertEquals(sil.entry4.get(0), "fiveone");
        Assert.assertEquals(sil.entry4.get(1), "fivetwo");
        Assert.assertEquals(sil.entry4.get(2), "fivethree");

    } // end testObtainConfig

    public void testClassExpectationFailures()
    {

        ExpectationConfig cfg = new ExpectationConfig();
        cfg.setConfigLabel("GEMINI");
        cfg.update("libhid");

        // 
        // lookup value exists, set method doesn't exist
        //

        //
        // lookup value doesn't exist, set method does exist
        //

        Assert.assertEquals("default value", cfg.missingLookupValue_);
    }

} // end test class
