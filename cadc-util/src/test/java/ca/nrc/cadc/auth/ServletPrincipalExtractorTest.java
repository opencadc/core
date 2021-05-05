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


import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ca.nrc.cadc.util.Log4jInit;
import ca.nrc.cadc.util.PropertiesReader;
import ca.nrc.cadc.util.RSASignatureGeneratorValidatorTest;
import ca.nrc.cadc.util.RsaSignatureGenerator;

import java.io.File;
import java.util.Calendar;
import java.util.Collections;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class ServletPrincipalExtractorTest
{    
    
    private static Logger log = Logger.getLogger(ServletPrincipalExtractorTest.class);
    
    static {
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
    public void testCookie() throws Exception
    {
        try {
            System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "build/resources/test/");

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR, 2);
            DelegationToken dt = new DelegationToken(new HttpPrincipal("CADCtest"), null, cal.getTime(), null);
            String cookieValue = DelegationToken.format(dt);
            CookiePrincipal principal = new CookiePrincipal(SSOCookieManager.DEFAULT_SSO_COOKIE_NAME, cookieValue);
            HttpServletRequest request = createMock(HttpServletRequest.class);
            Cookie cookie = createMock(Cookie.class);
            Cookie[] cookies = {cookie};

            expect(request.getAttribute(
                    ServletPrincipalExtractor.CERT_REQUEST_ATTRIBUTE)).andReturn(null);
            expect(request.getHeader(AuthenticationUtil.AUTH_HEADER)).andReturn(null);
            expect(request.getHeaders("Authorization")).andReturn(Collections.emptyEnumeration());
            expect(request.getCookies()).andReturn(cookies);
            expect(request.getRemoteUser()).andReturn(null).times(2);
            expect(request.getServerName()).andReturn("cookiedomain").once();
            expect(cookie.getName()).
                andReturn(SSOCookieManager.DEFAULT_SSO_COOKIE_NAME).anyTimes();
            expect(cookie.getValue()).andReturn(cookieValue).atLeastOnce();
            //expect(cookie.getDomain()).andReturn("cookiedomain").atLeastOnce();
            expect(request.getHeader(ServletPrincipalExtractor.CERT_HEADER_FIELD)).andReturn(null);
            replay(request);
            replay(cookie);
            ServletPrincipalExtractor ex = new ServletPrincipalExtractor(request);

            // Temporarily removing this so I can decide whether this needs to be
            // re-worked in the scope of making a getSSOCookieList function...
    //        assertEquals(cookieValue, ex.getSSOCookieCredential().getSsoCookieValue());
    //        assertEquals("cookiedomain", ex.getSSOCookieCredential().getDomain());
            assertTrue(ex.getPrincipals().iterator().next() instanceof CookiePrincipal);
            assertEquals(principal, ex.getPrincipals().iterator().next());

        }
        finally
        {
            System.setProperty(PropertiesReader.class.getName() + ".dir", "");
        }

    }
}
