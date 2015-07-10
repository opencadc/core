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
 * 4/17/12 - 10:55 AM
 *
 *
 *
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */
package ca.nrc.cadc.auth;

import ca.nrc.cadc.util.RsaSignatureGenerator;
import ca.nrc.cadc.util.RsaSignatureVerifier;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.security.InvalidKeyException;
import java.security.Principal;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/**
 * Manage authentication cookies.
 */
public class SSOCookieManager
{
    public static final String DEFAULT_SSO_COOKIE_NAME = "CADC_SSO";

    // For token and cookie value generation.
    private static final int SSO_COOKIE_LIFETIME_HOURS = 2 * 24; // in hours
    private static final String TOKEN_VALUE_FORMAT = "%s-%s-%tY%tm%td%tH%tM%tS";
    private static final String COOKIE_VALUE_FORMAT = TOKEN_VALUE_FORMAT + "-%s";
    
    // RegEx and formatters for parsing.
    private static final SimpleDateFormat DATE_FORMAT = 
                   new SimpleDateFormat("yyyyMMddhhmmss");
    private static final String ALPHA_CAPTURE_REGEX = "(\\w+)";
    private static final Pattern COOKIE_VALUE_PATTERN = 
                         Pattern.compile(ALPHA_CAPTURE_REGEX + "\\-" 
                                         + ALPHA_CAPTURE_REGEX 
                                         + "\\-(\\d{14})\\-"
                                         + ALPHA_CAPTURE_REGEX);

    private final RsaSignatureGenerator rsaSignatureGenerator;
    private final RsaSignatureVerifier rsaSignatureVerifier;


	 /**
	  * Constructor to use for generating new cookie values, and parsing
	  * existing ones.  Either argument can be null depending on the
	  * operation desired.
	  *
	  * @param rsaSignatureGenerator		The generator for the token section 
	  * 										   of the cookie value.
	  * @param rsaSignatureVerifier     The verifier used for cookie value 
	  *                                 parsing.
	  */
    public SSOCookieManager(final RsaSignatureGenerator rsaSignatureGenerator,
                            final RsaSignatureVerifier rsaSignatureVerifier)
    {
        this.rsaSignatureGenerator = rsaSignatureGenerator;
        this.rsaSignatureVerifier = rsaSignatureVerifier;
    }



    /**
     * Parse the cookie value.  If validation is successful, then the stream
     * is read in and a Principal representing the cookie value is returned.
     *
     * NOTE: It is the responsibility of the caller to handle the finalization
     * of the given InputStream (e.g. closing).
     *
     * @param inputStream           The stream to read in.
     * @return		The Principal decoded if the cookie value can be parsed 
     *            and validated.
     * @throws IOException
     */
    public final Principal parse(final InputStream inputStream) 
                    throws IOException
    {
        final BufferedReader reader = 
               new BufferedReader(new InputStreamReader(inputStream));
        final StringBuilder sb = new StringBuilder(256);
        String line = null;
        
        while ((line = reader.readLine()) != null) 
        {
            sb.append(line);
        }
        
        final String cookieValue = sb.toString();
        final Matcher matcher = COOKIE_VALUE_PATTERN.matcher(cookieValue);
        
        if (matcher.matches())
        {
        	   final String principalName = matcher.group(1);
        	   final String principalType = matcher.group(2);
        	   final String expirationDateUTF = matcher.group(3);
        	   final String token = matcher.group(4);
        	   
        	   try
        	   {
	        	    final Date expirationDate = DATE_FORMAT.parse(expirationDateUTF);
	        	   
	        	    if (this.rsaSignatureVerifier.verify(
	        	         createInputStream(getTokenInputBytes(principalName, 
	        	                                              principalType, 
	        	                                              expirationDate)),
	        	         token.getBytes()))
	        	    {
	        	        return new HttpPrincipal(principalName);
	        	    }
	        	    else
	        	    {
	                 throw new IOException("Unauthorized");        	   
	        	    }
	        	}
	        	catch (ParseException pe)
	        	{
	        	    throw new IOException("Can't parse date: " + expirationDateUTF);
	        	}
	        	catch (InvalidKeyException ike)
	        	{
	        		 throw new IOException("Invalid Key.");
	        	}
        }
        else
        {
            throw new IOException("Invalid cookie presented.\n" + cookieValue);
        }
    }

    /**
     * Generate a new cookie value for the given Principal.
     *
     * @param principal The Principal to generate the value from.
     * @return char array of the value.  never null.
     * @throws IOException Any errors with writing and generation.
     */
    public final char[] generate(final Principal principal) throws IOException
    {
        final String principalType =
                AuthenticationUtil.getPrincipalType(principal);
        final Date expirationDateInUTF = getExpirationDate();
        final String token = new String(
                generateToken(getTokenInputBytes(principal.getName(), 
                											 principalType,
                                                 expirationDateInUTF)));
        final String cookieValue = String.format(COOKIE_VALUE_FORMAT,
                                                 principal.getName(),
                                                 principalType,
                                                 expirationDateInUTF,
                                                 expirationDateInUTF,
                                                 expirationDateInUTF,
                                                 expirationDateInUTF,
                                                 expirationDateInUTF,
                                                 expirationDateInUTF,
                                                 token);

        return cookieValue.toCharArray();
    }

    /**
     * Obtain the byte array of values that will be passed to the token
     * generator to produce an encoded token string.
     *
     * @param principalName         The principal's name.
     * @param principalType         The principal type.
     * @param expirationDate        The Date this cookie expires.
     * @return                      byte array.  Never null.
     * @throws IOException
     */
    private byte[] getTokenInputBytes(final String principalName,
                                      final String principalType,
                                      final Date expirationDate)
            throws IOException
    {
        final String tokenInputString = String.format(TOKEN_VALUE_FORMAT,
                                                      principalName,
                                                      principalType,
                                                      expirationDate,
                                                      expirationDate,
                                                      expirationDate,
                                                      expirationDate,
                                                      expirationDate,
                                                      expirationDate);
        return tokenInputString.getBytes();
    }

    /**
     * Create a new InputStream for the given bytes to use in the token 
     * generator.  Used by testers to override.
     * 
     */
    InputStream createInputStream(final byte[] bytes)
    {
        return new ByteArrayInputStream(bytes);
    }

    /**
     * Call upon the RSA signature generator to produce a new token.
     *
     * @param input             The input source for the token.
     * @return                  byte array of the new token.
     * @throws IOException
     */
    private byte[] generateToken(final byte[] input) throws IOException
    {
        if (rsaSignatureGenerator == null)
        {
        	   throw new IllegalStateException(
        	       "RSA Generator cannot be null to produce a token.");        
        }    	
    	
        try
        {
            return rsaSignatureGenerator.sign(createInputStream(input));
        }
        catch (InvalidKeyException e)
        {
            throw new IOException("Invalid key.", e);
        }
    }

    /**
     * Produce an expiration date.  The default is forty-eight (48) hours.
     *
     * @return      Date of expiration.  Never null.
     */
    private Date getExpirationDate()
    {
        final Calendar cal = getCurrentCalendar();
        cal.add(Calendar.HOUR, SSO_COOKIE_LIFETIME_HOURS);

        return cal.getTime();
    }

    Calendar getCurrentCalendar()
    {
        return Calendar.getInstance(TimeZone.getTimeZone("UTF-8"));
    }
}
