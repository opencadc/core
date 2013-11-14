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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;


public class PropertiesReaderTest
{

    private static final Logger log = Logger
            .getLogger(PropertiesReaderTest.class);

    static
    {
        Log4jInit.setLevel("ca.nrc.cadc", org.apache.log4j.Level.INFO);
    }

    private static final String TEST_PROPERTIES =
            "prop1 = value1\n" +
            "prop2 = value2a\n" +
            "prop2 = value2b";

    private class TestInputStream extends InputStream
    {

        private InputStream one;
        private InputStream two;
        private int switchNum;
        private int count = 0;

        public TestInputStream(InputStream one, InputStream two, int switchNum)
        {
            this.one = one;
            this.two = two;
            this.switchNum = switchNum;
        }

        @Override
        public int read() throws IOException
        {
            count++;
            if (count <= switchNum)
            {
                log.debug("Reading from in one");
                return one.read();
            }
            else
            {
                log.debug("Reading from in two");
                return two.read();
            }

        }

        @Override
        public int read(byte[] b) throws IOException
        {
            count++;
            if (count <= switchNum)
            {
                log.debug("Reading from in one");
                return one.read(b);
            }
            else
            {
                log.debug("Reading from in two");
                return two.read(b);
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            count++;
            if (count <= switchNum)
            {
                log.debug("Reading from in one");
                return one.read(b, off, len);
            }
            else
            {
                log.debug("Reading from in two");
                return two.read(b, off, len);
            }
        }

    }

    @Test
    public void testResilientProperties() throws Exception
    {
        ByteArrayInputStream inOne = new ByteArrayInputStream(
                TEST_PROPERTIES.getBytes("UTF-8"));
        ByteArrayInputStream inTwo = new ByteArrayInputStream("".getBytes());
        TestInputStream in = new TestInputStream(inOne, inTwo, 3);

        try
        {
            // get the properties from inOne
            PropertiesReader propReader = new PropertiesReader(in);
            List<String> prop1 = propReader.getPropertyValues("prop1");
            Assert.assertEquals("missing prop 1, value 1", "value1",
                                prop1.get(0));
            List<String> prop2 = propReader.getPropertyValues("prop2");
            Assert.assertEquals("missing prop 2, value a", "value2a",
                                prop2.get(0));
            Assert.assertEquals("missing prop 2, value b", "value2b",
                                prop2.get(1));

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
            in.close();
        }
    }
}
