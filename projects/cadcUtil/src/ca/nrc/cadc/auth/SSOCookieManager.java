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

import java.io.*;
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
    public final static String DEFAULT_SSO_COOKIE_NAME = "CADC_SSO";

    public static final int SSO_COOKIE_LIFETIME_HOURS = 2 * 24; // in hours
    public final static String TOKEN_VALUE_FORMAT = "%s-%s-%tY%tm%td%tH%tM%tS";
    public final static String COOKIE_VALUE_FORMAT = TOKEN_VALUE_FORMAT + "-%s";

    private final RsaSignatureGenerator rsaSignatureGenerator;


    public SSOCookieManager(final RsaSignatureGenerator rsaSignatureGenerator)
    {
        this.rsaSignatureGenerator = rsaSignatureGenerator;
    }


    public final char[] parse(final Principal principal) throws IOException
    {
        final String principalType =
                AuthenticationUtil.getPrincipalType(principal);

        return null;
    }

    public final char[] generate(final Principal principal) throws IOException
    {
        final String principalType =
                AuthenticationUtil.getPrincipalType(principal);
        final Date expirationDateInUTF = getExpirationDate();
        final String token = new String(
                generateToken(getTokenInputBytes(principal, principalType,
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

    private byte[] getTokenInputBytes(final Principal principal,
                              final String principalType,
                              final Date expirationDate)
            throws IOException
    {
        final String tokenInputString = String.format(TOKEN_VALUE_FORMAT,
                                                      principal.getName(),
                                                      principalType,
                                                      expirationDate,
                                                      expirationDate,
                                                      expirationDate,
                                                      expirationDate,
                                                      expirationDate,
                                                      expirationDate);
        return tokenInputString.getBytes();
    }

    InputStream createInputStream(final byte[] bytes)
    {
        return new ByteArrayInputStream(bytes);
    }

    private byte[] generateToken(final byte[] input) throws IOException
    {
        try
        {
            return rsaSignatureGenerator.sign(createInputStream(input));
        }
        catch (InvalidKeyException e)
        {
            throw new IOException("Invalid key.", e);
        }
    }

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
