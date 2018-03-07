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
 * 4/17/12 - 10:55 AM
 *
 *
 *
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */
package ca.nrc.cadc.auth;

import ca.nrc.cadc.date.DateUtil;

import ca.nrc.cadc.util.PropertiesReader;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


/**
 * Manage authentication cookies.
 */
public class SSOCookieManager
{

    public static final String DEFAULT_SSO_COOKIE_NAME = "CADC_SSO";

    // For token and cookie value generation.
    public static final int SSO_COOKIE_LIFETIME_HOURS = 2 * 24; // in hours

    public static final URI SCOPE_URI = URI.create("sso:cadc+canfar");

    public static final String SUPPORTED_DOMAINS_PROP_FILE = "SupportedDomains.properties";

    // Offset to add to the expiry hours.  This is mainly used to set a cookie
    // date in the past to expire it.  This can be a negative value.
    private int offsetExpiryHours = 1;


    /**
     * Parse the cookie value.  If validation is successful, then the stream
     * is read in and a Principal representing the cookie value is returned.
     * Format of the value is:
     *    UserPrincipal-PrincipalType-ExpirationDateUTC-Base64SignatureToken
     *    where:
     *    UserPrincipal - principal of the user
     *    PrincipalType - principal type
     *    ExpirationDateUTC - long representing the expiration Java date in UTC
     *    Base64SignatureToken - The signature token of the 3 fields above in
     *                           Base64 format.
     *
     * @param value           Cookie value.
     * @return		The HttpPrincipal decoded if the cookie value can be parsed 
     *            and validated.
     * @throws InvalidDelegationTokenException 
     */
    public final DelegationToken parse(final String value)
                    throws IOException, InvalidDelegationTokenException
    {
        /*
        TODO - The DelegationToken class really should be fixed to handle
        TODO - null values and bad entries.
        TODO - jenkinsd 2015.07.14
         */
        DelegationToken token;

        try
        {
            token = DelegationToken.parse(value, SCOPE_URI.toASCIIString(), new CookieScopeValidator());
        }
        catch (Exception e)
        {
            throw new InvalidDelegationTokenException("Bad token." + value);
        }

        return token;
    }

    /**
     * Generate a new cookie value for the given HttpPrincipal.
     * Format of the value is:
     *    HttpPrincipal-ExpirationDateUTC-Base64SignatureToken
     *    where:
     *    HttpPrincipal - principal of the user
     *    ExpirationDateUTC - long representing the expiration Java date in UTC
     *    Base64SignatureToken - The signature token of the 2 fields above in
     *                           Base64 format.
     *
     * @param principal The HttpPrincipal to generate the value from.
     * @return string of the value.  never null.
     * @throws IOException Any errors with writing and generation.
     * @throws InvalidKeyException Signing key is invalid
     */
    public final String generate(final HttpPrincipal principal) 
            throws InvalidKeyException, IOException
    {
        DelegationToken token =
                new DelegationToken(principal, SCOPE_URI, getExpirationDate());
        return DelegationToken.format(token);
    }

    /**
     * Produce an expiration date.  The default is forty-eight (48) hours.
     *
     * @return      Date of expiration.  Never null.
     */
    private Date getExpirationDate()
    {
        final Calendar cal = getCurrentCalendar();
        cal.add(Calendar.HOUR, (SSO_COOKIE_LIFETIME_HOURS * offsetExpiryHours));

        return cal.getTime();
    }

    /**
     * Testers can override this to provide a consistent test.
     *
     * @return  Calendar instance.  Never null.
     */
    public Calendar getCurrentCalendar()
    {
        return Calendar.getInstance(DateUtil.UTC);
    }

    public void setOffsetExpiryHours(int offsetExpiryHours)
    {
        this.offsetExpiryHours = offsetExpiryHours;
    }

    /**
     * Generate a list of cookies based on the original credentials passed in, one for each
     * of the supported domains.
     *
     * @param cookieValue
     * @param requestedDomain
     * @param expiryDate
     * @return cookieList
     */
     public List<SSOCookieCredential> getSSOCookieCredentials(final String cookieValue, final String requestedDomain, final Date expiryDate) {
        List<SSOCookieCredential> cookieList = new ArrayList<>();

        // Make cookie with requested domain
        SSOCookieCredential requestedCookie = new SSOCookieCredential(cookieValue, requestedDomain, expiryDate);
        cookieList.add(requestedCookie);

        // Get domain list from properties
        PropertiesReader propReader = new PropertiesReader(SUPPORTED_DOMAINS_PROP_FILE);
        List<String> domainValues = propReader.getPropertyValues("domains");
        String[] domainList = domainValues.get(0).split(" ");

        for (String domain: domainList) {
            if (!domain.equals(requestedDomain)) {
                SSOCookieCredential nextCookie = new SSOCookieCredential(cookieValue, domain, expiryDate);
                cookieList.add(nextCookie);
            }
        }

        return cookieList;
    }
}
