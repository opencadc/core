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


import static org.junit.Assert.assertEquals;

import ca.nrc.cadc.util.Log4jInit;
import ca.nrc.cadc.util.RSASignatureGeneratorValidatorTest;
import ca.nrc.cadc.util.RsaSignatureGenerator;
import java.io.File;
import java.util.Date;
import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class SSOCookieCredentialTest
{
    static int MS_IN_HOUR = 3600 * 1000;
    static
    {
        Log4jInit.setLevel("ca.nrc.cadc.auth", Level.INFO);
    }
    
    @Test
    public void testCookie() throws Exception
    {
        Date baseTime = new Date(System.currentTimeMillis());
        Date cookieExpiry = new Date(baseTime.getTime() + (48 * MS_IN_HOUR));
        SSOCookieCredential newcookie = new SSOCookieCredential("VALUE_1", "en.host.com", cookieExpiry);

        assertEquals(newcookie.getExpiryDate(), cookieExpiry);
        assertEquals(newcookie.isExpired(), false);

        Date expiredDate = new Date(baseTime.getTime() - (48 * MS_IN_HOUR));
        SSOCookieCredential expiredCookie = new SSOCookieCredential("VALUE_1", "en.host.com", expiredDate);

        assertEquals(expiredCookie.getExpiryDate(), expiredDate);
        assertEquals(expiredCookie.isExpired(), true);
    }
}
