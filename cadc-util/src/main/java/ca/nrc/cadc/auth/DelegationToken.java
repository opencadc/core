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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;

import ca.nrc.cadc.util.Base64;
import ca.nrc.cadc.util.RsaSignatureGenerator;
import ca.nrc.cadc.util.RsaSignatureVerifier;
import ca.nrc.cadc.util.StringUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import javax.security.auth.x500.X500Principal;

import org.apache.log4j.Logger;

/**
 * Class that captures the information required to perform delegation (i.e.
 * accessing resources on user's behalf). The required fields are:
 * - the user that delegates
 * - the expirty time
 * - scope of delegation
 * <p>
 * This class can serialize and de-serialize the information into a
 * String token. Optionally, it can sign the serialized token and
 * verify when one is received.
 */
public class DelegationToken implements Serializable {
    private static final Logger log = Logger.getLogger(DelegationToken.class);

    private static final long serialVersionUID = 20180321000000L;

    private Set<Principal> identityPrincipals;

    public static String PROXY_LABEL = "proxyuser";
    public static String SCOPE_LABEL = "scope";
    public static String DOMAIN_LABEL = "domain";

    // Why not simply re-use the IdentityType.USERID.getValue() here rather than a yet another custom label?
    public static String USER_LABEL = "userid";
    public static String EXPIRY_LABEL = "expirytime";
    public static String SIGNATURE_LABEL = "signature";
    public static String IDENTITIES_LABEL = "identities";


    private Date expiryTime; // expiration time of the delegation (UTC)
    private URI scope; // resources that are the object of the delegation
    private List<String> domains;

    public static final String FIELD_DELIM = "&";
    public static final String VALUE_DELIM = "=";

    public static class ScopeValidator {
        public void verifyScope(URI scope, String requestURI)
            throws InvalidDelegationTokenException {
            throw new InvalidDelegationTokenException("default: invalid scope");
        }
    }

    /**
     * Constructor.
     *
     * @param user       identity of the delegating user - required
     * @param scope      - scope of the delegation, i.e. resource that it applies
     *                   to - optional
     * @param expiryTime - the expiry date of this token (UTC)
     */
    public DelegationToken(HttpPrincipal user, URI scope, Date expiryTime, List<String> domains) {
        // Validation of parameter means using this() to call
        // other constructor isn't possible.
        if (user == null) {
            throw new IllegalArgumentException("User identity required");
        }

        this.addPrincipal(user);

        if (expiryTime == null) {
            throw new IllegalArgumentException("No expiry time");
        }
        this.expiryTime = expiryTime;
        this.scope = scope;

        this.setDomains(domains);
    }

    /**
     * Constructor.
     *
     * @param principals - sorted set of identity principals (http, x500, cadc)
     * @param scope      - scope of the delegation, i.e. resource that it applies to - optional
     * @param expiryTime - the expiry date of this token (UTC)
     * @param domains    - list of domains that this token could be used for
     */
    public DelegationToken(Set<Principal> principals, URI scope, Date expiryTime, List<String> domains) {
        if (principals == null || principals.size() == 0) {
            throw new IllegalArgumentException("Identity principals required (ie http, x500, cadc internal)");
        }

        if (expiryTime == null) {
            throw new IllegalArgumentException("No expiry time");
        }

        this.addPrincipals(principals);
        this.expiryTime = expiryTime;
        this.scope = scope;

        this.setDomains(domains);
    }

    /**
     * Serializes and signs the object into a string of attribute-value pairs.
     *
     * @param token the token to format
     *              the returned string
     * @return String with DelegationToken information
     * @throws IOException         IO problems
     * @throws InvalidKeyException The provided key is invalid
     */
    public static String format(DelegationToken token)
        throws InvalidKeyException, IOException {
        StringBuilder sb = getContent(token);

        //sign and add the signature field
        String toSign = sb.toString();
        log.debug("string to be signed: " + toSign);
        sb.append(FIELD_DELIM);
        sb.append(SIGNATURE_LABEL);
        sb.append(VALUE_DELIM);
        RsaSignatureGenerator su = new RsaSignatureGenerator();
        byte[] sig = su.sign(new ByteArrayInputStream(toSign.getBytes()));
        sb.append(new String(Base64.encode(sig)));

        return sb.toString();
    }

    // the formatted content without the signature
    private static StringBuilder getContent(DelegationToken token) {
        StringBuilder sb = new StringBuilder();

        sb.append(EXPIRY_LABEL).append(VALUE_DELIM);
        sb.append(token.getExpiryTime().getTime());

        final Set<Principal> identityPrincipals = token.identityPrincipals;

        if (!identityPrincipals.isEmpty()) {
            sb.append(FIELD_DELIM);
            sb.append(IDENTITIES_LABEL).append(VALUE_DELIM);
            sb.append(DelegationToken.encodePrincipals(identityPrincipals));
        }

        if (token.getScope() != null) {
            sb.append(FIELD_DELIM);
            sb.append(SCOPE_LABEL);
            sb.append(VALUE_DELIM);
            sb.append(token.getScope());
        }

        if (token.getDomains() != null) {
            for (String domain : token.getDomains()) {
                sb.append(FIELD_DELIM);
                sb.append(DOMAIN_LABEL);
                sb.append(VALUE_DELIM);
                sb.append(domain);
            }
        }

        log.debug("getContent: " + sb);
        return sb;
    }

    /**
     * @param text       The content of the token to parse.
     * @param requestURI The Request URI
     * @return DelegationToken instance, with parsed content.
     * @throws InvalidDelegationTokenException If the token is not in the expected format.
     */
    public static DelegationToken parse(String text, String requestURI)
        throws InvalidDelegationTokenException {
        return parse(text, requestURI, null);
    }

    /**
     * Builds a DelegationToken from a text string
     *
     * @param text       to parse
     * @param requestURI the request URI
     * @param sv         For validating the scope part of the token.
     * @return corresponding DelegationToken
     * @throws InvalidDelegationTokenException If the token is not in the expected format.
     */
    public static DelegationToken parse(String text, String requestURI, ScopeValidator sv)
        throws InvalidDelegationTokenException {
        String[] fields = text.split(FIELD_DELIM);
        Set<Principal> principalSet = new HashSet<>();
        Date expirytime = null;
        URI scope = null;
        String signature = null;
        List<String> domains = new ArrayList<>();
        try {
            for (String field : fields) {
                String key = field.substring(0, field.indexOf(VALUE_DELIM));
                String value = field.substring(field.indexOf(VALUE_DELIM) + 1);
                log.debug("key = value: " + key + "=" + value);

                if (key.equalsIgnoreCase(IDENTITIES_LABEL)) {
                    principalSet = DelegationToken.decodePrincipals(value);
                } else if (key.equalsIgnoreCase(EXPIRY_LABEL)) {
                    expirytime = new Date(Long.valueOf(value));
                } else if (key.equalsIgnoreCase(SCOPE_LABEL)) {
                    scope = new URI(value);
                } else if (key.equalsIgnoreCase(SIGNATURE_LABEL)) {
                    signature = value;
                } else if (key.equalsIgnoreCase(DOMAIN_LABEL)) {
                    domains.add(value);
                }
            }
        } catch (NumberFormatException ex) {
            throw new InvalidDelegationTokenException("invalid numeric field", ex);
        } catch (URISyntaxException ex) {
            throw new InvalidDelegationTokenException("invalid scope URI", ex);
        }

        if (signature == null) {
            throw new InvalidDelegationTokenException("missing signature");
        }

        // validate expiry
        if (expirytime == null) {
            throw new InvalidDelegationTokenException("missing expirytime");
        }
        Date now = new Date();

        if (now.getTime() > expirytime.getTime()) {
            throw new InvalidDelegationTokenException("expired");
        }

        // validate scope
        if (scope != null) {
            if (sv == null) // not supplied
            {
                sv = getScopeValidator();
            }
            sv.verifyScope(scope, requestURI);
        }

        // validate signature
        try {
            RsaSignatureVerifier su = new RsaSignatureVerifier();
            String signatureSplitter = FIELD_DELIM + DelegationToken.SIGNATURE_LABEL + "=";
            String[] cookieNSignature = text.split(signatureSplitter);
            log.debug("string to be verified" + cookieNSignature[0]);
            boolean valid = su.verify(
                new ByteArrayInputStream(cookieNSignature[0].getBytes()),
                Base64.decode(signature));

            if (!valid) {
                log.error("invalid signature: " + text);
                throw new InvalidDelegationTokenException("cannot verify signature");
            }

        } catch (Exception ex) {
            log.debug("failed to verify DelegationToken signature", ex);
            throw new InvalidDelegationTokenException("cannot verify signature", ex);
        }

        return new DelegationToken(principalSet, scope, expirytime, domains);

    }

    private static Map<String, String> principalKeysToMap(final String principalsQueryString) {
        final Map<String, String> mappedValues = new HashMap<>();
        for (final String pair : principalsQueryString.split(FIELD_DELIM)) {
            final int valueSplitIndex = pair.indexOf(VALUE_DELIM);

            // We can't use the pair.split(VALUE_DELIM) here because the X500 DN contains '='.
            final String key = pair.substring(0, valueSplitIndex);
            final String value = pair.substring(valueSplitIndex + 1);
            if (StringUtil.hasLength(key) && StringUtil.hasLength(value)) {
                mappedValues.put(key, value);
            }
        }
        return mappedValues;
    }

    /**
     * Obtain a char array of the Base64 encoded principals.
     * @param identityPrincipals        The Set of Principal instances.
     * @return  A new char array of encoded values, or empty array.  Never null.
     */
    static char[] encodePrincipals(final Set<Principal> identityPrincipals) {
        final StringBuilder principalBuilder = new StringBuilder();
        // Add all available identity principals to the content
        for (final Principal principal : identityPrincipals) {
            final String principalName = principal.getClass().getSimpleName();
            final IdentityType principalIdentity = IdentityType.principalIdentityMap.get(principalName);

            // User ID has a custom value.
            if (principalIdentity == IdentityType.USERID) {
                final HttpPrincipal httpPrincipal = (HttpPrincipal) principal;

                principalBuilder.append(USER_LABEL);
                principalBuilder.append(VALUE_DELIM);
                principalBuilder.append(principal.getName());

                if (StringUtil.hasText(httpPrincipal.getProxyUser())) {
                    principalBuilder.append(FIELD_DELIM);
                    principalBuilder.append(PROXY_LABEL);
                    principalBuilder.append(VALUE_DELIM);
                    principalBuilder.append(httpPrincipal.getProxyUser());
                }
            } else if (!principalIdentity.equals(IdentityType.ENTRY_DN)) {
                // Do not add this for external use, to cookies, etc.
                principalBuilder.append(principalIdentity.getValue());
                principalBuilder.append(VALUE_DELIM);
                principalBuilder.append(principal.getName());
            }

            principalBuilder.append(FIELD_DELIM);
        }

        if (principalBuilder.lastIndexOf(FIELD_DELIM) > 0) {
            principalBuilder.deleteCharAt(principalBuilder.lastIndexOf(FIELD_DELIM));
        }

        return Base64.encode(principalBuilder.toString().getBytes());
    }

    private static Set<Principal> decodePrincipals(final String base64EncodedString) {
        final String decodedPrincipals = new String(Base64.decode(base64EncodedString));
        final Map<String, String> decodedPrincipalsMap = DelegationToken.principalKeysToMap(decodedPrincipals);
        final Set<Principal> principals = new HashSet<>();

        if (decodedPrincipalsMap.containsKey(USER_LABEL)) {
            final String httpUserID = decodedPrincipalsMap.get(USER_LABEL);
            if (decodedPrincipalsMap.containsKey(PROXY_LABEL)) {
                principals.add(new HttpPrincipal(httpUserID, decodedPrincipalsMap.get(PROXY_LABEL)));
            } else {
                principals.add(new HttpPrincipal(httpUserID));
            }
        }

        if (decodedPrincipalsMap.containsKey(IdentityType.NUMERICID.getValue())) {
            principals.add(new NumericPrincipal(UUID.fromString(
                decodedPrincipalsMap.get(IdentityType.NUMERICID.getValue()))));
        }

        if (decodedPrincipalsMap.containsKey(IdentityType.X500.getValue())) {
            principals.add(new X500Principal(decodedPrincipalsMap.get(IdentityType.X500.getValue())));
        }

        return principals;
    }

    private static ScopeValidator getScopeValidator() {
        try {
            String fname = DelegationToken.class.getSimpleName() + ".properties";
            String pname = DelegationToken.class.getName() + ".scopeValidator";
            Properties props = new Properties();
            props.load(DelegationToken.class.getClassLoader().getResource(fname).openStream());
            String cname = props.getProperty(pname);
            log.debug(fname + ": " + pname + " = " + cname);
            Class c = Class.forName(cname);
            ScopeValidator ret = (ScopeValidator) c.newInstance();
            log.debug("created: " + ret.getClass().getName());
            return ret;
        } catch (Exception ignore) {
            log.debug("failed to load custom ScopeValidator", ignore);
        }

        // default
        return new ScopeValidator();
    }

    // user is wrong name
    public HttpPrincipal getUser() {
        return getPrincipalByClass(HttpPrincipal.class);
    }

    public <T extends Principal> T getPrincipalByClass(Class clazz) {
        for (Principal prin : this.identityPrincipals) {
            if (prin.getClass() == clazz) {
                return (T) prin;
            }
        }
        return null;
    }

    public Date getExpiryTime() {
        return expiryTime;
    }

    public URI getScope() {
        return scope;
    }

    public List<String> getDomains() {
        return domains;
    }


    // TODO: update this to include principals
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DelegationToken(").append(USER_LABEL).append("=");
        if (StringUtil.hasText(getUser().getProxyUser())) {
            sb.append(",").append(PROXY_LABEL).append("=");
            sb.append(getUser().getProxyUser());
        }
        sb.append(getUser());
        sb.append(",").append(SCOPE_LABEL).append("=");
        sb.append(getScope());
        sb.append(",startTime=");
        sb.append(getExpiryTime());

        for (String domain : domains) {
            sb.append(",").append(DOMAIN_LABEL).append("=").append(domain);
        }
        sb.append(")");
        return sb.toString();
    }

    private void setDomains(List<String> domains) {
        if (domains != null) {
            if (this.domains == null) {
                this.domains = new ArrayList<>();
            }
            this.domains.addAll(domains);
        }
    }

    private void addPrincipal(Principal p) {
        if (p != null) {
            Set<Principal> pSet = new HashSet<>();
            pSet.add(p);
            addPrincipals(pSet);
        }
    }

    private void addPrincipals(Set<Principal> principals) {
        if (principals != null) {
            if (this.identityPrincipals == null) {
                this.identityPrincipals = new HashSet<>();
            }

            this.identityPrincipals.addAll(principals);
        }
    }

    public Set<Principal> getIdentityPrincipals() {
        return identityPrincipals;
    }
}
