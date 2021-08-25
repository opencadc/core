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
 * 12/8/11 - 2:39 PM
 *
 *
 *
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */
package ca.nrc.cadc.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.MissingResourceException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.junit.Test;

public class RSASignatureGeneratorValidatorTest {

    private static final Logger log = Logger.getLogger(RSASignatureGeneratorValidatorTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.util", Level.INFO);
    }

    File keysDir = new File("build/tmp");

    @Test
    public void testSignVerify() throws Exception {
        final String testString = "cadcauthtest1-" + new Date();
        
        
        int[] keyLengths = { 512, 1024, 2048, 4096 };
        for (int len : keyLengths) {
            log.info("testSignVerify: " + len);
            File pub = new File(keysDir, "public-" + len + ".key");
            File priv = new File(keysDir, "private-" + len + ".key");
            RsaSignatureGenerator.genKeyPair(pub, priv, len);

            // successful test
            RsaSignatureGenerator sg = new RsaSignatureGenerator(priv);
            RsaSignatureVerifier sv = new RsaSignatureVerifier(pub);
            assertTrue("Signature does not work!!!!",
                    sv.verify(new ByteArrayInputStream(testString.getBytes()),
                            sg.sign(new ByteArrayInputStream(testString.getBytes()))));
        }
    }

    @Test
    public void testKeyFileNotFound() throws Exception {
        final String testString = "cadcauthtest1-" + new Date();

        File pub = new File(keysDir, "public.key");
        File priv = new File(keysDir, "private.key");
        int len = 1024;
        RsaSignatureGenerator.genKeyPair(pub, priv, len);

        pub.delete();
        try {
            RsaSignatureVerifier sv = new RsaSignatureVerifier(pub);
            fail("Expected exception not thrown");
        } catch (MissingResourceException ex) {
            log.info("caught expected: " + ex);
        }

        //generator still works
        RsaSignatureGenerator sg = new RsaSignatureGenerator(priv);
        assertTrue("Signature does not work!!!!",
                sg.verify(new ByteArrayInputStream(testString.getBytes()),
                        sg.sign(new ByteArrayInputStream(testString.getBytes()))));

        priv.delete();
        try {
            sg = new RsaSignatureGenerator(priv);
            fail("Expected exception not thrown");
        } catch (MissingResourceException ex) {
            log.info("caught expected: " + ex);
        }

    }

    @Test
    public void testNoKeysInFile() throws Exception {
        final String testString = "cadcauthtest1-" + new Date();

        File pub = new File(keysDir, "public.key");
        File priv = new File(keysDir, "private.key");
        int len = 1024;
        RsaSignatureGenerator.genKeyPair(pub, priv, len);

        // no keys in the file
        PrintWriter out = new PrintWriter(priv);
        out.println("--");
        out.close();
        try {
            RsaSignatureGenerator sg = new RsaSignatureGenerator(priv);
            fail("Expected exception not thrown");
        } catch (IllegalStateException ex) {
            log.info("caught expected: " + ex);
        }

        out = new PrintWriter(pub);
        out.println("--");
        out.close();
        try {
            RsaSignatureVerifier sv = new RsaSignatureVerifier(pub);
            fail("Expected exception not thrown");
        } catch (IllegalStateException ex) {
            log.info("caught expected: " + ex);
        }
    }
    
    @Test
    public void testWrongKeyTypes() throws Exception {
        final String testString = "cadcauthtest1-" + new Date();

        File pub = new File(keysDir, "public.key");
        File priv = new File(keysDir, "private.key");
        int len = 1024;
        RsaSignatureGenerator.genKeyPair(pub, priv, len);
        
        try {
            RsaSignatureVerifier sv = new RsaSignatureVerifier(priv);
            fail("Expected exception not thrown");
        } catch (IllegalStateException ex) {
            log.info("caught expected: " + ex);
        }

        try {
            RsaSignatureGenerator sg = new RsaSignatureGenerator(pub);
            fail("Expected exception not thrown");
        } catch (IllegalStateException ex) {
            log.info("caught expected: " + ex);
        }
    }
    
    //@Test
    public void testMultiplePubkeys() throws Exception {
        final String testString = "cadcauthtest1-" + new Date();
        int len = 1024;
        
        File pub1 = new File(keysDir, "public1.key");
        File priv1 = new File(keysDir, "private1.key");
        RsaSignatureGenerator.genKeyPair(pub1, priv1, len);
        
        
        File pub2 = new File(keysDir, "public2.key");
        File priv2 = new File(keysDir, "private2.key");
        RsaSignatureGenerator.genKeyPair(pub2, priv2, len);
        
        
        // concatenate pub keys and create a multi-key verifier
        File multi = new File(keysDir, "multipub.key");
        // TODO: concat pub1 + pub2 -> multi
        RsaSignatureVerifier mk = new RsaSignatureVerifier(multi);
        RsaSignatureGenerator gen1 = new RsaSignatureGenerator(priv1);
        RsaSignatureGenerator gen2 = new RsaSignatureGenerator(priv2);
        
        assertTrue("Signature 1 does not work!!!!",
                mk.verify(new ByteArrayInputStream(testString.getBytes()),
                        gen1.sign(new ByteArrayInputStream(testString.getBytes()))));

        assertTrue("Signature 2 does not work!!!!",
                mk.verify(new ByteArrayInputStream(testString.getBytes()),
                        gen2.sign(new ByteArrayInputStream(testString.getBytes()))));
    }
}
