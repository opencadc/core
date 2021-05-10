/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2018.                            (c) 2018.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*
*  NRC disclaims any warranties,        Le CNRC dénie toute garantie
*  expressed, implied, or               énoncée, implicite ou légale,
*  statutory, of any kind with          de quelque nature que ce
*  respect to the software,             soit, concernant le logiciel,
*  including without limitation         y compris sans restriction
*  any warranty of merchantability      toute garantie de valeur
*  or fitness for a particular          marchande ou de pertinence
*  purpose. NRC shall not be            pour un usage particulier.
*  liable in any event for any          Le CNRC ne pourra en aucun cas
*  damages, whether direct or           être tenu responsable de tout
*  indirect, special or general,        dommage, direct ou indirect,
*  consequential or incidental,         particulier ou général,
*  arising from the use of the          accessoire ou fortuit, résultant
*  software.  Neither the name          de l'utilisation du logiciel. Ni
*  of the National Research             le nom du Conseil National de
*  Council of Canada nor the            Recherches du Canada ni les noms
*  names of its contributors may        de ses  participants ne peuvent
*  be used to endorse or promote        être utilisés pour approuver ou
*  products derived from this           promouvoir les produits dérivés
*  software without specific prior      de ce logiciel sans autorisation
*  written permission.                  préalable et particulière
*                                       par écrit.
*
*  This file is part of the             Ce fichier fait partie du projet
*  OpenCADC project.                    OpenCADC.
*
*  OpenCADC is free software:           OpenCADC est un logiciel libre ;
*  you can redistribute it and/or       vous pouvez le redistribuer ou le
*  modify it under the terms of         modifier suivant les termes de
*  the GNU Affero General Public        la “GNU Affero General Public
*  License as published by the          License” telle que publiée
*  Free Software Foundation,            par la Free Software Foundation
*  either version 3 of the              : soit la version 3 de cette
*  License, or (at your option)         licence, soit (à votre gré)
*  any later version.                   toute version ultérieure.
*
*  OpenCADC is distributed in the       OpenCADC est distribué
*  hope that it will be useful,         dans l’espoir qu’il vous
*  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
*  without even the implied             GARANTIE : sans même la garantie
*  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
*  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
*  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
*  General Public License for           Générale Publique GNU Affero
*  more details.                        pour plus de détails.
*
*  You should have received             Vous devriez avoir reçu une
*  a copy of the GNU Affero             copie de la Licence Générale
*  General Public License along         Publique GNU Affero avec
*  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
*  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
*                                       <http://www.gnu.org/licenses/>.
*
*  $Revision: 5 $
*
************************************************************************
*/

package ca.nrc.cadc.auth;

import ca.nrc.cadc.date.DateUtil;

import ca.nrc.cadc.util.PropertiesReader;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manage authentication cookies.
 */
public class SSOCookieManager {

    public static final String DEFAULT_SSO_COOKIE_NAME = "CADC_SSO";

    // For token and cookie value generation.
    public static final int SSO_COOKIE_LIFETIME_HOURS = 2 * 24; // in hours
    
    public static final URI SCOPE_URI = URI.create("sso:cadc+canfar");

    public static final String DOMAINS_PROP_FILE = "ac-domains.properties";

    // Offset to add to the expiry hours. This is mainly used to set a cookie
    // date in the past to expire it. This can be a negative value.
    private int offsetExpiryHours = 1;

    /**
     * Parse the cookie value. If validation is successful, then the stream is read
     * in and a Principal representing the cookie value is returned. Format of the
     * value is: UserPrincipal-PrincipalType-ExpirationDateUTC-Base64SignatureToken
     * where: UserPrincipal - principal of the user PrincipalType - principal type
     * ExpirationDateUTC - long representing the expiration Java date in UTC
     * Base64SignatureToken - The signature token of the 3 fields above in Base64
     * format.
     *
     * @param value Cookie value.
     * @return The HttpPrincipal decoded if the cookie value can be parsed and
     *         validated.
     * @throws InvalidSignedTokenException
     */
    public final SignedToken parse(final String value) throws InvalidSignedTokenException {
        if (value == null) {
            throw new IllegalArgumentException("value required");
        }
        try {
            return SignedToken.parse(value);
        } catch (Exception e) {
            throw new InvalidSignedTokenException("Bad token." + value);
        }
    }

    /**
     * Generate a new cookie value for the given HttpPrincipal. Format of the value
     * is: HttpPrincipal-ExpirationDateUTC-Base64SignatureToken where: HttpPrincipal
     * - principal of the user ExpirationDateUTC - long representing the expiration
     * Java date in UTC Base64SignatureToken - The signature token of the 2 fields
     * above in Base64 format.
     *
     * @param principal The HttpPrincipal to generate the value from.
     * @return string of the value. never null.
     * @throws IOException         Any errors with writing and generation.
     * @throws InvalidKeyException Signing key is invalid
     */
    public final String generate(final HttpPrincipal principal) throws InvalidKeyException, IOException {
        Set<Principal> principalSet = new HashSet<>();
        principalSet.add(principal);
        return generate(principalSet);
    }
    
    /**
     * Generate a new cookie value for the given HttpPrincipal. Format of the value
     * is: HttpPrincipal-ExpirationDateUTC-Base64SignatureToken where: HttpPrincipal
     * - principal of the user ExpirationDateUTC - long representing the expiration
     * Java date in UTC Base64SignatureToken - The signature token of the 2 fields
     * above in Base64 format.
     */
    public final String generate(Set<Principal> principalSet, URI scope) throws InvalidKeyException, IOException {
        return generate(principalSet, null, scope);
    }

    /**
     * Generate a new cookie value for the set of Principals, scope and expiryDate.
     * Sets a default scope and expiry if either not supplied
     * 
     * @param principalSet
     * @param expiryDate
     * @return
     * @throws InvalidKeyException
     * @throws IOException
     */
    public final String generate(final Set<Principal> principalSet, Date expiryDate, URI scope)
            throws InvalidKeyException, IOException {
        if (expiryDate == null) {
            expiryDate = getExpirationDate();
        }
        if (scope == null) {
            scope = SCOPE_URI;
        }
        List<String> domainList = null;
        PropertiesReader propReader = new PropertiesReader(DOMAINS_PROP_FILE);
        List<String> domainValues = propReader.getPropertyValues("domains");
        if (domainValues != null && (domainValues.size() > 0)) {
            domainList = Arrays.asList(domainValues.get(0).split(" "));
        }
        SignedToken token = new SignedToken(principalSet, scope, expiryDate, domainList);
        return SignedToken.format(token);
    }

    /**
     * Generate a new cookie value for the set of Principals.
     *
     * @param principalSet The HttpPrincipal to generate the value from.
     * @return string of the value. never null.
     * @throws IOException         Any errors with writing and generation.
     * @throws InvalidKeyException Signing key is invalid
     */
    public final String generate(final Set<Principal> principalSet) throws InvalidKeyException, IOException {
        return generate(principalSet, getExpirationDate(), null);
    }

    /**
     * Produce an expiration date. The default is forty-eight (48) hours.
     *
     * @return Date of expiration. Never null.
     */
    public Date getExpirationDate() {
        final Calendar cal = getCurrentCalendar();
        cal.add(Calendar.HOUR, (SSO_COOKIE_LIFETIME_HOURS * offsetExpiryHours));

        return cal.getTime();
    }

    /**
     * Testers can override this to provide a consistent test.
     *
     * @return Calendar instance. Never null.
     */
    public Calendar getCurrentCalendar() {
        return Calendar.getInstance(DateUtil.UTC);
    }

    public void setOffsetExpiryHours(int offsetExpiryHours) {
        this.offsetExpiryHours = offsetExpiryHours;
    }

    /**
     * Generate a list of cookies based on the original credentials passed in, one
     * for each of the supported domains.
     *
     * @param cookieValue
     * @return cookieList
     */
    public List<SSOCookieCredential> getSSOCookieCredentials(final String cookieValue)
            throws InvalidSignedTokenException {

        List<SSOCookieCredential> cookieList = new ArrayList<>();
        SignedToken cookieToken = SignedToken.parse(cookieValue);

        for (String domain : cookieToken.getDomains()) {
            SSOCookieCredential nextCookie = new SSOCookieCredential(cookieValue, domain, cookieToken.getExpiryTime());
            cookieList.add(nextCookie);
        }

        return cookieList;
    }

}
