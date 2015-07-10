/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2012.                         (c) 2012.
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


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.security.Principal;

import ca.nrc.cadc.util.RsaSignatureGenerator;
import ca.nrc.cadc.util.RsaSignatureVerifier;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;


public class SSOCookieManagerTest
{
    private static final RsaSignatureGenerator MOCK_RSA_SIGNATURE_GENERATOR =
            createMock(RsaSignatureGenerator.class);
    private static final RsaSignatureVerifier MOCK_RSA_SIGNATURE_VERIFIER =
            createMock(RsaSignatureVerifier.class);            
    private static final Calendar EXPIRATION_CALENDAR = Calendar.getInstance();

    static
    {
        EXPIRATION_CALENDAR.set(1977, Calendar.NOVEMBER, 25, 3, 21, 0);
        EXPIRATION_CALENDAR.set(Calendar.MILLISECOND, 0);
    }

    
    @Test
    public void parse() throws Exception
    {
        final String mockToken = "ENCODEDTOKEN";
        final String tokenValue = "CADCtest-http-19771127032100";
        final String testCookieValueString = tokenValue + "-" + mockToken;
        final InputStream inputStream =
                new ByteArrayInputStream(tokenValue.getBytes());    	
    	
        final SSOCookieManager testSubject = 
        			 new SSOCookieManager(MOCK_RSA_SIGNATURE_GENERATOR,
        			                      MOCK_RSA_SIGNATURE_VERIFIER)
                {                    
                    @Override
                    InputStream createInputStream(byte[] bytes)
                    {
                        assertTrue("Wrong bytes.",
                                   Arrays.equals(tokenValue.getBytes(), bytes));
                        return inputStream;
                    }
                };    
        			                      
        final InputStream testCookieValue = 
               new ByteArrayInputStream(testCookieValueString.getBytes());
               
        expect(MOCK_RSA_SIGNATURE_VERIFIER.verify(eq(inputStream), 
                                                  aryEq(mockToken.getBytes()))).
                                   andReturn(true).once();               
               
        replay(MOCK_RSA_SIGNATURE_VERIFIER);
        final Principal principal1 = testSubject.parse(testCookieValue);
        
        assertTrue("Wrong principal.", principal1 instanceof HttpPrincipal);
        verify(MOCK_RSA_SIGNATURE_VERIFIER);
    }

    @Test
    public void generateCookie() throws Exception
    {
        final String mockToken = "ENCODEDTOKEN";
        final String tokenValue = "CADCtest-http-19771127032100";
        final String expectedCookieValue = tokenValue + "-ENCODEDTOKEN";
        final InputStream inputStream =
                new ByteArrayInputStream(tokenValue.getBytes());

        final SSOCookieManager testSubject =
                new SSOCookieManager(MOCK_RSA_SIGNATURE_GENERATOR,
                                     MOCK_RSA_SIGNATURE_VERIFIER)
                {
                    @Override
                    Calendar getCurrentCalendar()
                    {
                        return EXPIRATION_CALENDAR;
                    }

                    @Override
                    InputStream createInputStream(byte[] bytes)
                    {
                        assertTrue("Wrong bytes.",
                                   Arrays.equals(tokenValue.getBytes(), bytes));
                        return inputStream;
                    }
                };


        expect(MOCK_RSA_SIGNATURE_GENERATOR.sign(inputStream)).andReturn(
                mockToken.getBytes()).once();

        replay(MOCK_RSA_SIGNATURE_GENERATOR);
        final String resultCookieValue =
                String.valueOf(testSubject.generate(
                        new HttpPrincipal("CADCtest")));
        assertEquals("Wrong cookie value.", expectedCookieValue,
                     resultCookieValue);
        verify(MOCK_RSA_SIGNATURE_GENERATOR);
    }
}
