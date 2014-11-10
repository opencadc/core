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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

import ca.nrc.cadc.auth.DelegationToken;
import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.auth.InvalidDelegationTokenException;

public class DelegationTokenTest
{

    @Test
    public void matches() throws Exception
    {
        String keysDir = RSASignatureGeneratorValidatorTest.getCompleteKeysDirectoryName();
        RsaSignatureGenerator.genKeyPair(keysDir);

        // round trip test without signature
        HttpPrincipal userid = new HttpPrincipal("someuser");
        URI scope = new URI("vos://cadc.nrc.ca~vospace/myspace");
        int duration = 10; // h
        DelegationToken expToken = new DelegationToken(userid, duration, 
                scope, new Date());
        DelegationToken actToken = DelegationToken.parse(
                expToken.format(false), false);
        assertEquals("User id not the same", expToken.getUser(),
                actToken.getUser());
        assertEquals("Duration not the same", expToken.getDuration(),
                actToken.getDuration());
        assertEquals("Start time not the same", expToken.getStartTime(),
                actToken.getStartTime());
        assertEquals("Scope not the same", expToken.getScope(),
                actToken.getScope());
        
        // round trip test with signature
        actToken = DelegationToken.parse(
                expToken.format(true), true);
        assertEquals("User id not the same", expToken.getUser(),
                actToken.getUser());
        assertEquals("Duration not the same", expToken.getDuration(),
                actToken.getDuration());
        assertEquals("Start time not the same", expToken.getStartTime(),
                actToken.getStartTime());
        assertEquals("Scope not the same", expToken.getScope(),
                actToken.getScope());
        
        // round trip with missing signature - no token
        try
        {
            actToken = DelegationToken.parse(
                expToken.format(false), true);
            fail("Exception expected");
        }
        catch(InvalidDelegationTokenException ignore){}
        
        // round trip with signature ignored
        actToken = DelegationToken.parse(
                expToken.format(true), false);
        assertEquals("User id not the same", expToken.getUser(),
                actToken.getUser());
        assertEquals("Duration not the same", expToken.getDuration(),
                actToken.getDuration());
        assertEquals("Start time not the same", expToken.getStartTime(),
                actToken.getStartTime());
        assertEquals("Scope not the same", expToken.getScope(),
                actToken.getScope());
        
        // Same tests but with no scope
        expToken = new DelegationToken(userid, duration, null, new Date());
        actToken = DelegationToken.parse(
                expToken.format(false), false);
        assertEquals("User id not the same", expToken.getUser(),
                actToken.getUser());
        assertEquals("Duration not the same", expToken.getDuration(),
                actToken.getDuration());
        assertEquals("Start time not the same", expToken.getStartTime(),
                actToken.getStartTime());
        assertNull(actToken.getScope());
        
        // round trip test with signature
        actToken = DelegationToken.parse(
                expToken.format(true), true);
        assertEquals("User id not the same", expToken.getUser(),
                actToken.getUser());
        assertEquals("Duration not the same", expToken.getDuration(),
                actToken.getDuration());
        assertEquals("Start time not the same", expToken.getStartTime(),
                actToken.getStartTime());
        assertNull(actToken.getScope());
        
        // round trip with missing signature - no token
        try
        {
            actToken = DelegationToken.parse(
                expToken.format(false), true);
            fail("Exception expected");
        }
        catch(InvalidDelegationTokenException ignore){}
        
        // round trip with signature ignored
        actToken = DelegationToken.parse(
                expToken.format(true), false);
        assertEquals("User id not the same", expToken.getUser(),
                actToken.getUser());
        assertEquals("Duration not the same", expToken.getDuration(),
                actToken.getDuration());
        assertEquals("Start time not the same", expToken.getStartTime(),
                actToken.getStartTime());
        assertNull(actToken.getScope());
        
        // some failures
        boolean thrown = false;
        try
        {
            expToken = new DelegationToken(null, duration, null, new Date());
        }
        catch (IllegalArgumentException e)
        {
            thrown = true;
        }
        assertTrue(thrown);
        try
        {
            expToken = new DelegationToken(userid, -2, null, new Date());
            fail("Exception expected");
        }
        catch(IllegalArgumentException ignore){}

        actToken = DelegationToken.parse(
                expToken.format(true), true);
        assertEquals("Duration not the same", expToken.getDuration(),
                actToken.getDuration());
        
        // check isValid
        expToken = new DelegationToken(userid, duration, 
                scope, new Date());
        assertTrue(expToken.isValid());
        // move start date into the future
        Calendar cal = Calendar.getInstance(); // creates calendar
        cal.setTime(new Date()); // sets calendar time/date
        cal.add(Calendar.HOUR_OF_DAY, duration); // adds duration hours
        expToken = new DelegationToken(userid, duration, 
                scope, cal.getTime());
        assertFalse(expToken.isValid());
        
        // now remove more hours (move start date into the past so it is expired
        cal.setTime(new Date()); // sets calendar time/date
        cal.add(Calendar.HOUR_OF_DAY, (-2) * duration); // removes 2*duration
        assertFalse(expToken.isValid());
        
        // parse expired token
        cal = Calendar.getInstance(); // creates calendar
        cal.setTime(new Date()); // sets calendar time/date
        cal.add(Calendar.HOUR_OF_DAY, duration*2); // removes duration hours
        try
        {
            expToken = new DelegationToken(userid, duration, 
                    scope, cal.getTime());
            DelegationToken.parse(expToken.format(true), true);
            fail("Exception expected");
        }
        catch(InvalidDelegationTokenException ignore){}
        
        
        cal = Calendar.getInstance(); // creates calendar
        cal.setTime(new Date()); // sets calendar time/date
        cal.add(Calendar.HOUR_OF_DAY, duration); // add duration hours
        // parse future token
        try
        {
            expToken = new DelegationToken(userid, duration, 
                    scope, cal.getTime());
            DelegationToken.parse(expToken.format(true), true);
            fail("Exception expected");
        }
        catch(InvalidDelegationTokenException ignore){}
        
        //invalidate signature field
        try
        {
            expToken = new DelegationToken(userid, duration, 
                    scope, cal.getTime());
            
            String token = expToken.format(true);
            token.subSequence(0,  token.indexOf("signature") + 10);
            DelegationToken.parse(token, true);
            fail("Exception expected");
        }
        catch(InvalidDelegationTokenException ignore){}
        
        // tamper with one character in the signature
        try
        {
            expToken = new DelegationToken(userid, duration, 
                    scope, cal.getTime());
            
            String token = expToken.format(true);
            char toReplace = token.charAt(token.indexOf("signature") + 10);
            if (toReplace != 'A')
            {
                token.replace(token.charAt(token.indexOf("signature") + 20), 'A');
            }
            else
            {
                token.replace(token.charAt(token.indexOf("signature") + 20), 'B');
            }
            DelegationToken.parse(token, true);
            fail("Exception expected");
        }
        catch(InvalidDelegationTokenException ignore){}
    }
}

