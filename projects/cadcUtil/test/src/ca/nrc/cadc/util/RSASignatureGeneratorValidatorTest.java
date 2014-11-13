/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2011.                         (c) 2011.
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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.MissingResourceException;
import org.apache.log4j.Level;

import org.junit.Test;

public class RSASignatureGeneratorValidatorTest
{
    static
    {
        Log4jInit.setLevel("ca.nrc.cadc.util", Level.INFO);
    }
    
    @Test
    public void matches() throws Exception
    {
        String keysDirectory = getCompleteKeysDirectoryName();
        RsaSignatureGenerator.genKeyPair(keysDirectory);
        
        // successful test
        RsaSignatureGenerator sg = new RsaSignatureGenerator();
        RsaSignatureVerifier sv = new RsaSignatureVerifier();
        String testString = "cadcauthtest1-" + new Date();
        assertTrue("Signature does not work!!!!", 
                sv.verify(new ByteArrayInputStream(testString.getBytes()), 
                sg.sign(new ByteArrayInputStream(testString.getBytes()))));
        
        
        // file not found
        File privFile = new File(keysDirectory, 
                RsaSignatureGenerator.PRIV_KEY_FILE_NAME);
        String privKeyFileName = privFile.getAbsolutePath();
        
        File pubFile = new File(keysDirectory, 
                RsaSignatureGenerator.PUB_KEY_FILE_NAME);
        String pubKeyFileName = pubFile.getAbsolutePath();
        
        
        pubFile.delete();
        try
        {
            sv = new RsaSignatureVerifier();
            fail("Expected exception not thrown");
        }
        catch(MissingResourceException ignore){}
        
        //generator still works
        sg = new RsaSignatureGenerator();
        assertTrue("Signature does not work!!!!", 
                sg.verify(new ByteArrayInputStream(testString.getBytes()), 
                sg.sign(new ByteArrayInputStream(testString.getBytes()))));
        
        
        privFile.delete();
        try
        {
            sg = new RsaSignatureGenerator();
            fail("Expected exception not thrown");
        }
        catch(MissingResourceException ignore){}        

        
        // no keys in the file
        PrintWriter out = new PrintWriter(privFile);
        out.println("--");
        out.close();
        try
        {
            sg = new RsaSignatureGenerator();
            fail("Expected exception not thrown");
        }
        catch(IllegalStateException ignore){}
        
        out = new PrintWriter(pubFile);
        out.println("--");
        out.close();
        try
        {
            sv = new RsaSignatureVerifier();
            fail("Expected exception not thrown");
        }
        catch(IllegalStateException ignore){}
        
        // file with wrong type of key
        RsaSignatureGenerator.genKeyPair(keysDirectory);
        File keyFile = new File(privKeyFileName);
        keyFile.renameTo(new File(pubKeyFileName));
        try
        {
            sv = new RsaSignatureVerifier();
            fail("Expected exception not thrown");
        }
        catch(IllegalStateException ignore){}
        
        RsaSignatureGenerator.genKeyPair(keysDirectory);
        keyFile = new File(pubKeyFileName);
        keyFile.renameTo(new File(privKeyFileName));
        try
        {
            sg = new RsaSignatureGenerator();
            fail("Expected exception not thrown");
        }
        catch(IllegalStateException ignore){}
        
        // run against a file with two public keys
        // generate a new public key
        RsaSignatureGenerator.genKeyPair(keysDirectory);
        
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                RsaSignatureGenerator.KEY_ALGORITHM);
        kpg.initialize(1024);
        KeyPair keyPair = kpg.genKeyPair();
        char[] base64PubKey = Base64.encode(keyPair.getPublic().getEncoded());
        char[] base64PrivKey = Base64.encode(keyPair.getPrivate().getEncoded());
        PrintWriter outpub = new PrintWriter(new BufferedWriter(new FileWriter(pubKeyFileName, true)));
        outpub.println(RsaSignatureVerifier.PUB_KEY_START);
        outpub.println(base64PubKey);
        outpub.println(RsaSignatureVerifier.PUB_KEY_END);
        outpub.close();
        sv = new RsaSignatureVerifier();
        sg = new RsaSignatureGenerator();
        assertEquals("Missing public key", 2, sv.getPublicKeys().size());
        
        assertTrue("Signature does not work!!!!", 
                sv.verify(new ByteArrayInputStream(testString.getBytes()), 
                sg.sign(new ByteArrayInputStream(testString.getBytes()))));
        
        // same as above but try to add 2 private keys
        PrintWriter outpriv = new PrintWriter(new BufferedWriter(new FileWriter(privKeyFileName, true)));
        outpriv.println(RsaSignatureGenerator.PRIV_KEY_START);
        outpriv.println(base64PrivKey);
        outpriv.println(RsaSignatureGenerator.PRIV_KEY_END);
        outpriv.close();
        try
        {
            sg = new RsaSignatureGenerator();
            fail("Exception expected");
        }
        catch(IllegalStateException ignore){}

    }



    /**
     * Return the complete name of the directory where key files are to be 
     * created so that the RsaSignature classes can find it.
     * @return
     */
    public static String getCompleteKeysDirectoryName()
    {
        URL classLocation = 
                RsaSignatureGenerator.class.getResource(
                        RsaSignatureGenerator.class.getSimpleName() + ".class");
        if (!"file".equalsIgnoreCase(classLocation.getProtocol()))
        {
            throw new 
            IllegalStateException("SignatureUtil class is not stored in a file.");
        }
        File classPath = new File(classLocation.getPath()).getParentFile();
        String packageName = RsaSignatureGenerator.class.getPackage().getName();
        String packageRelPath = packageName.replace('.', File.separatorChar);
        
        String dir = classPath.getAbsolutePath().
                substring(0, classPath.getAbsolutePath().indexOf(packageRelPath));
        
        if (dir == null)
        {
            throw new RuntimeException("Cannot find the class directory");
        }
        return dir;
    }

}
