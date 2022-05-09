/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2016.                         (c) 2016.
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
 *
 * @author jenkinsd
 * Apr 28, 2009 - 2:53:11 PM
 *
 *
 *
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */

package ca.nrc.cadc.util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

public class Log4jInitTest {

    private static final Logger log = Logger.getLogger(Log4jInitTest.class);

    @Test
    public void testStdout() {
        System.out.println("before init");
        log.trace("before init");
        log.debug("before init");
        log.info("before init");
        log.warn("before init");
        log.error("before init");

        Log4jInit.setLevel("ca.nrc.cadc.util", Level.WARN);
        System.out.println("after WARN");
        log.trace("after WARN");
        log.debug("after WARN");
        log.info("after WARN");
        log.warn("after WARN");
        log.error("after WARN");

        Log4jInit.setLevel("ca.nrc.cadc.util", Level.INFO);
        System.out.println("after INFO");
        log.trace("after INFO");
        log.debug("after INFO");
        log.info("after INFO");
        log.warn("after INFO");
        log.error("after INFO");

        Log4jInit.setLevel("ca.nrc.cadc.util", Level.DEBUG);
        System.out.println("after DEBUG");
        log.trace("after DEBUG");
        log.debug("after DEBUG");
        log.info("after DEBUG");
        log.warn("after DEBUG");
        log.error("after DEBUG");

        Log4jInit.setLevel("ca.nrc.cadc.util", Level.INFO);
        System.out.println("back to INFO");
        log.trace("back to INFO");
        log.debug("back to INFO");
        log.info("back to INFO");
        log.warn("back to INFO");
        log.error("back to INFO");
    }

    @Test
    public void testWriter() {

        StringWriter w = new StringWriter();

        /*
        Log4jInit.setLevel("ca.nrc.cadc.util", Level.WARN, w);
        log.trace("after WARN");
        log.debug("after WARN");
        log.info("after WARN");
        log.warn("after WARN");
        log.error("after WARN");
        */
        
        Log4jInit.setLevel("ca.nrc.cadc.util", Level.INFO, w);
        System.out.println("after INFO");
        log.trace("after INFO");
        log.debug("after INFO");
        log.info("after INFO");
        log.warn("after INFO");
        log.error("after INFO");

        /*
        Log4jInit.setLevel("ca.nrc.cadc.util", Level.DEBUG, w);
        System.out.println("after DEBUG");
        log.trace("after DEBUG");
        log.debug("after DEBUG");
        log.info("after DEBUG");
        log.warn("after DEBUG");
        log.error("after DEBUG");

        Log4jInit.setLevel("ca.nrc.cadc.util", Level.INFO, w);
        System.out.println("back to INFO");
        log.trace("back to INFO");
        log.debug("back to INFO");
        log.info("back to INFO");
        log.warn("back to INFO");
        log.error("back to INFO");
        */
        
        String s = w.toString();
        System.out.println("----writer content----");
        System.out.println(s);
        System.out.println("----writer content----");
        
    }
}
