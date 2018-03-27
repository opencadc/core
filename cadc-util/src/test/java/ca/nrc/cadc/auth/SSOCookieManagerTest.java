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
 * 4/17/12 - 11:21 AM
 *
 *
 *
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */
package ca.nrc.cadc.auth;


import ca.nrc.cadc.util.Base64;
import ca.nrc.cadc.util.Log4jInit;
import ca.nrc.cadc.util.PropertiesReader;
import ca.nrc.cadc.util.RSASignatureGeneratorValidatorTest;
import ca.nrc.cadc.util.RsaSignatureGenerator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;


public class SSOCookieManagerTest
{    
    static
    {
        Log4jInit.setLevel("ca.nrc.cadc.auth", Level.INFO);
    }
    
    File pubFile, privFile;
    
    @Before
    public void initKeys() throws Exception
    {
        String keysDir = RSASignatureGeneratorValidatorTest.getCompleteKeysDirectoryName();
        RsaSignatureGenerator.genKeyPair(keysDir);
        privFile = new File(keysDir, RsaSignatureGenerator.PRIV_KEY_FILE_NAME);
        pubFile = new File(keysDir, RsaSignatureGenerator.PUB_KEY_FILE_NAME);
    }
    
    @After
    public void cleanupKeys() throws Exception
    {
        pubFile.delete();
        privFile.delete();
    }
    
    @Test
    public void roundTrip() throws Exception
    {
        final HttpPrincipal userPrincipal = new HttpPrincipal("CADCtest");
        SSOCookieManager cm = new SSOCookieManager();
        DelegationToken cookieToken = cm.parse(cm.generate(userPrincipal, null));
        HttpPrincipal actualPrincipal = cookieToken.getUser();
        
        assertEquals(userPrincipal, actualPrincipal);
    }

    public String createCookieString() throws InvalidKeyException, IOException {

        // Set properties file location.
        System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "./build/resources/test");
        // get the properties
        PropertiesReader propReader = new PropertiesReader(SSOCookieManager.DOMAINS_PROP_FILE);
        List<String> propertyValues = propReader.getPropertyValues("domains");
        List<String> domainList = Arrays.asList(propertyValues.get(0).split(" "));

        Date baseTime = new Date();
        Date cookieExpiry = new Date(baseTime.getTime() + (48 * 3600 * 1000));
        String testCookieStringDate = "expirytime=" + cookieExpiry.getTime();

        String testCookieStringBody ="&userid=someuser&domain=www.canfar.phys.uvic.ca" +
            "&domain=www.cadc.hia.nrc.gc.ca&domain=www.ccda.iha.cnrc.gc.ca" +
            "&domain=www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca";

        StringBuilder sb = new StringBuilder(testCookieStringDate + testCookieStringBody);

        //sign and add the signature field
        String toSign = sb.toString();
        sb.append("&");
        sb.append("signature");
        sb.append("=");
        RsaSignatureGenerator su = new RsaSignatureGenerator();
        byte[] sig =
            su.sign(new ByteArrayInputStream(toSign.getBytes()));
        sb.append(new String(Base64.encode(sig)));

        return sb.toString();
    }

    @Test
    public void createCookieSet() throws Exception {
        List<SSOCookieCredential> cookieList = new ArrayList<>();

        try {
//            // Set properties file location.
//            System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "./build/resources/test");
//            // get the properties
//            PropertiesReader propReader = new PropertiesReader(SSOCookieManager.DOMAINS_PROP_FILE);
//            List<String> propertyValues = propReader.getPropertyValues("domains");
//            List<String> domainList = Arrays.asList(propertyValues.get(0).split(" "));
//
//            Date baseTime = new Date();
//            Date cookieExpiry = new Date(baseTime.getTime() + (48 * 3600 * 1000));
//            String testCookieStringDate = "expirytime=" + cookieExpiry.getTime();
//
//            String testCookieStringBody ="&userid=someuser&domain=www.canfar.phys.uvic.ca" +
//                "&domain=www.cadc.hia.nrc.gc.ca&domain=www.ccda.iha.cnrc.gc.ca" +
//                "&domain=www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca" +
//                "&signature=";
//
//            StringBuilder sb = new StringBuilder(testCookieStringDate + testCookieStringBody);
//
//            //sign and add the signature field
//            String toSign = sb.toString();
//            sb.append("&");
//            sb.append("signature");
//            sb.append("=");
//            RsaSignatureGenerator su = new RsaSignatureGenerator();
//            byte[] sig =
//                su.sign(new ByteArrayInputStream(toSign.getBytes()));
//            sb.append(new String(Base64.encode(sig)));


            //                String testCookieStringBody = "&userid=someuser&domain=www.canfar.phys.uvic.ca" +
//                "&domain=www.cadc.hia.nrc.gc.ca&domain=www.ccda.iha.cnrc.gc.ca" +
//                "&domain=www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca&signature=test.signature";
            String cookieValue = createCookieString();
            cookieList = new SSOCookieManager().getSSOCookieCredentials(cookieValue, "www.canfar.phys.uvic.ca");

            // cookieList length should be same as domainList
//            assertEquals(cookieList.size(),domainList.length);
        }
        finally
        {
            System.setProperty(PropertiesReader.class.getName() + ".dir", "");
        }
    }

}
