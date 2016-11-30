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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ObjectUtilTest
{
    private static Logger log = Logger.getLogger(ObjectUtilTest.class);

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
        Log4jInit.setLevel("ca.nrc.cadc.util", Level.INFO);
    }

    @Test
    public void testImmediateClass()
    {
        try
        {
            Level3 level3 = new Level3();
            ObjectUtil.setField(level3, "value3", "attr3");
            Assert.assertNull(level3.getAttr1());
            Assert.assertNull(level3.getAttr2());
            Assert.assertEquals("value3", level3.getAttr3());
        }
        catch (Throwable t)
        {
            log.error("unexpected", t);
            Assert.fail("unexpected: " + t.getMessage());
        }
    }

    @Test
    public void testSuperClass()
    {
        try
        {
            Level3 level3 = new Level3();
            ObjectUtil.setField(level3, "value1", "attr1");
            Assert.assertEquals("value1", level3.getAttr1());
            Assert.assertNull(level3.getAttr2());
            Assert.assertNull(level3.getAttr3());
        }
        catch (Throwable t)
        {
            log.error("unexpected", t);
            Assert.fail("unexpected: " + t.getMessage());
        }
    }

    @Test
    public void testNotFound()
    {
        try
        {
            Level3 level3 = new Level3();
            ObjectUtil.setField(level3, "value", "attr4");
            Assert.fail("Should have received RuntimeException");
        }
        catch (RuntimeException e)
        {
            // expected
        }
        catch (Throwable t)
        {
            log.error("unexpected", t);
            Assert.fail("unexpected: " + t.getMessage());
        }
    }

    class Level1
    {
        private String attr1;
        public String getAttr1() { return attr1; }
    }

    class Level2 extends Level1
    {
        private String attr2;
        public String getAttr2() { return attr2; }
    }

    class Level3 extends Level2
    {
        private String attr3;
        public String getAttr3() { return attr3; }
    }

}
