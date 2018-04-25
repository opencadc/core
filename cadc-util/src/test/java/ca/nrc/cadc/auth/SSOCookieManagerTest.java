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
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.Principal;
import java.util.ArrayList;
import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import javax.security.auth.x500.X500Principal;
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

    List<String> domainList = new ArrayList<>();

    @Before
    public void initKeys() throws Exception
    {
        String keysDir = RSASignatureGeneratorValidatorTest.getCompleteKeysDirectoryName();
        RsaSignatureGenerator.genKeyPair(keysDir);
        privFile = new File(keysDir, RsaSignatureGenerator.PRIV_KEY_FILE_NAME);
        pubFile = new File(keysDir, RsaSignatureGenerator.PUB_KEY_FILE_NAME);

        domainList.add("canfar.phys.uvic.ca");
        domainList.add("cadc.hia.nrc.gc.ca");
        domainList.add("ccda.iha.cnrc.gc.ca");
        domainList.add("cadc-ccda.hia-iha.nrc-cnrc.gc.ca");

    }
    
    @After
    public void cleanupKeys() throws Exception
    {
        pubFile.delete();
        privFile.delete();
    }
    
    @Test
    public void roundTripMin() throws Exception
    {
        final HttpPrincipal userPrincipal = new HttpPrincipal("CADCtest");
        SSOCookieManager cm = new SSOCookieManager();
        DelegationToken cookieToken = cm.parse(cm.generate(userPrincipal, null));
        HttpPrincipal actualPrincipal = cookieToken.getUser();

        //Check principal
        assertEquals(userPrincipal, actualPrincipal);
    }

    @Test
    public void roundTrip() throws Exception
    {
        SSOCookieManager cm = new SSOCookieManager();
        System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "./build/resources/test");

        // round trip test
        Set<Principal> testPrincipals = new HashSet<>();
        HttpPrincipal hp = new HttpPrincipal("someuser");
        testPrincipals.add(hp);
        X500Principal xp = new X500Principal("CN=JBP,OU=nrc-cnrc.gc.ca,O=grid,C=CA");
        testPrincipals.add(xp);

        // Pretend CADC identity
        UUID testUUID = UUID.randomUUID();
        testPrincipals.add(new NumericPrincipal(testUUID));

        URI scope = new URI("sso:cadc+canfar");
        String cookieValue = cm.generate(testPrincipals, scope);

        DelegationToken actToken = cm.parse(cookieValue);

        assertEquals("User id not the same", hp, actToken.getUser());
        assertEquals("Scope not the same", scope, actToken.getScope());
        assertEquals("x509 principal not the same", xp, actToken.getPrincipalByClass(X500Principal.class));

        assertEquals("domain list not equal", domainList, actToken.getDomains());
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
        String testCookieStringDate = DelegationToken.EXPIRY_LABEL + "=" + cookieExpiry.getTime();

        String testCookieStringBody ="&" + DelegationToken.USER_LABEL + "=someuser&" +
            DelegationToken.DOMAIN_LABEL + "=" + domainList.get(0) + "&" +
            DelegationToken.DOMAIN_LABEL + "=" + domainList.get(1) + "&" +
            DelegationToken.DOMAIN_LABEL + "=" + domainList.get(2) + "&" +
            DelegationToken.DOMAIN_LABEL + "=" + domainList.get(3);

        StringBuilder sb = new StringBuilder(testCookieStringDate + testCookieStringBody);

        //sign and add the signature field
        String toSign = sb.toString();
        sb.append("&");
        sb.append(DelegationToken.SIGNATURE_LABEL);
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
            String cookieValue = createCookieString();
            cookieList = new SSOCookieManager().getSSOCookieCredentials(cookieValue, "www.canfar.phys.uvic.ca");

            // cookieList length should be same as list of expected domains
            assertEquals(cookieList.size(), domainList.size());
        }
        finally
        {
            System.setProperty(PropertiesReader.class.getName() + ".dir", "");
        }
    }

}
