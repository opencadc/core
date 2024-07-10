/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2024.                            (c) 2024.
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
import ca.nrc.cadc.net.NetUtil;
import ca.nrc.cadc.util.InvalidConfigException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

/**
 * Security utility.
 *
 * @author adriand
 * @version $Version$
 */
public class AuthenticationUtil {

    @Deprecated // Should be using standard Authorization header
    public static final String AUTH_HEADER = "X-CADC-DelegationToken";

    // HTTP/1.1 Authorization header as defined by RFC 7235
    public static final String AUTHORIZATION_HEADER = "Authorization";
    
    // HTTP/1.1 WWW-Authenticate header
    public static final String AUTHENTICATE_HEADER = "WWW-Authenticate";
    
    // IVOA header to indicate successful authentication.  Value is a principal name.
    public static final String VO_AUTHENTICATED_HEADER = "x-vo-authenticated";
    
    // IVOA header to set a Bearer token in a response
    public static final String VO_TOKEN_BEARER = "x-vo-bearer";
    
    public static final String CHALLENGE_TYPE_BEARER = "Bearer";
    public static final String CHALLENGE_TYPE_BASIC = "Basic";
    public static final String CHALLENGE_TYPE_IVOA_BEARER = "ivoa_bearer";
    public static final String CHALLENGE_TYPE_IVOA_X509 = "ivoa_x509";
    @Deprecated
    public static final String TOKEN_TYPE_CADC = AUTH_HEADER;

    // Mandatory support list of RDN descriptors according to RFC 4512.
    private static final String[] ORDERED_RDN_KEYS = new String[] { "DC", "CN", "OU", "O", "STREET", "L", "ST", "C", "UID" };

    private static Logger log = Logger.getLogger(AuthenticationUtil.class);

    /**
     * Load the available IdentityManager implementation. This utility method will
     * check the <code>ca.nrc.cadc.auth.IdentityManager</code> system property for a
     * configured class name. If not configured or loading the configured implementation
     * fails, the default is the <code>ca.nrc.cadc.auth.NoOpIdentityManager</code>.
     * 
     * @return an IdentityManager implementation
     */
    public static IdentityManager getIdentityManager() {
        String cname = System.getProperty(IdentityManager.class.getName());
        IdentityManager ret = new NoOpIdentityManager();
        if (cname != null) {
            try {
                Class c = Class.forName(cname);
                Object o = c.getConstructor().newInstance();
                ret = (IdentityManager) o;
            } catch (ClassNotFoundException 
                    | IllegalAccessException | IllegalArgumentException | InstantiationException 
                    | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
                throw new InvalidConfigException("failed to load configured IdentityManager: " + cname, ex);
            }
        }
        log.debug("loaded IdentityManager: " + ret.getClass().getName());
        return ret;
    }

    // backwards compat
    private static Authenticator getAuthenticator(IdentityManager im) {
        String cname = System.getProperty(Authenticator.class.getName());
        if (cname != null && im instanceof NoOpIdentityManager) {
            try {
                Class c = Class.forName(cname);
                Object o = c.getConstructor().newInstance();
                Authenticator ret = (Authenticator) o;
                log.warn("DEPRECATED: using " + Authenticator.class.getName() + " = " + cname);
                return ret;
            } catch (ClassNotFoundException 
                    | IllegalAccessException | IllegalArgumentException | InstantiationException 
                    | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
                throw new InvalidConfigException("failed to load configured IdentityManager: " + cname, ex);
            }
        }
        return null;
    }
    
    public static Subject augmentSubject(Subject s) {
        IdentityManager auth = getIdentityManager();
        // temporary backwards compat
        //Authenticator alt = getAuthenticator(auth);
        //if (alt != null) {
        //    return alt.augment(s);
        //}
        return auth.augment(s);
    }
    
    public static Subject validateSubject(Subject s) throws NotAuthenticatedException {
        IdentityManager auth = getIdentityManager();
        // temporary backwards compat
        //Authenticator alt = getAuthenticator(auth);
        //if (alt != null) {
        //    return alt.validate(s);
        //}
        return auth.validate(s);
    }

    public static Subject getAnonSubject() {
        Subject ret = new Subject();
        setAuthMethod(ret, AuthMethod.ANON);
        return ret;
    }

    /**
     * Get the AuthMethod used by the caller. This is normally only meaningful in
     * server side applications to figure out how the caller authenticated.
     *
     * @param s
     * @return
     */
    public static AuthMethod getAuthMethod(Subject s) {
        if (s == null) {
            return null;
        }
        Set<AuthMethod> m = s.getPublicCredentials(AuthMethod.class);
        if (m.isEmpty()) {
            return null;
        }
        return m.iterator().next();
    }

    private static void setAuthMethod(Subject s, AuthMethod am) {
        if (s == null || am == null) {
            return;
        }
        s.getPublicCredentials().add(am);
    }

    /**
     * Get an AuthMethod that can be used with credentials from the specified set.
     *
     * @param subject the subject with credentials
     * @return
     */
    public static AuthMethod getAuthMethodFromCredentials(Subject subject) {
        if (subject == null || subject.getPublicCredentials().isEmpty()) {
            return AuthMethod.ANON;
        }

        // web services using CDP and command-line applications with --cert option
        Set cert = subject.getPublicCredentials(X509CertificateChain.class);
        if (!cert.isEmpty()) {
            return AuthMethod.CERT;
        }

        // command-line applications with --netrc option
        Set pa = subject.getPublicCredentials(PasswordCredentials.class);
        if (!pa.isEmpty()) {
            return AuthMethod.PASSWORD;
        }

        // ui applications pass cookie(s) along
        Set sso = subject.getPublicCredentials(SSOCookieCredential.class);
        if (!sso.isEmpty()) {
            return AuthMethod.COOKIE;
        }
        
        Set delToken = subject.getPublicCredentials(SignedToken.class);
        if (!delToken.isEmpty()) {
            return AuthMethod.TOKEN;
        }
        
        Set token = subject.getPublicCredentials(AuthorizationToken.class);
        if (!token.isEmpty()) {
            return AuthMethod.TOKEN;
        }

        return AuthMethod.ANON;
    }

    /**
     * Create a Subject using the given PrincipalExtractor. An implementation of the
     * PrincipalExtractor interface is used to extract the authentication
     * information from the incoming request. An implementation for plain servlet
     * environment is provided here and a Restlet implementation is currently
     * included in the cadcUWS library.
     * <p>
     * This method tries to detect the use of a proxy certificate and add the
     * Principal representing the real identity of the user by comparing the subject
     * and issuer fields of the certificate and using the issuer principal when the
     * certificate is self-signed. If the user has connected anonymously, the
     * returned Subject will have no principals and no credentials, but should be
     * safe to use with Subject.doAs(...).
     * </p>
     * <p>
     * This method will also try to load an implementation of the IdentityManager
     * interface and use it to process the Subject before return. By default, it
     * will try to load the <code>ca.nrc.cadc.auth.NoOpIdentityManager</code> and
     * simply ignore all authentication. Applications may override this default class 
     * name by setting the <em>ca.nrc.cadc.auth.IdentityManager</em> system property 
     * to the class name of their implementation.
     * </p>
     *
     * @param principalExtractor The PrincipalExtractor to provide Principals.
     * @param augmentSubject Whether to augment the subject using Authenticator interface.
     * @return A new Subject.
     */
    public static Subject getSubject(PrincipalExtractor principalExtractor, boolean augmentSubject) {
        if (principalExtractor == null) {
            throw new IllegalArgumentException("principalExtractor cannot be null");
        }

        final Set<Principal> principals = principalExtractor.getPrincipals();
        final X509CertificateChain chain = principalExtractor.getCertificateChain();

        AuthMethod am = null;

        final Set<Object> publicCred = new HashSet<Object>();
        final Set<Object> privateCred = new HashSet<Object>();

        if (principals.isEmpty()) {
            am = AuthMethod.ANON;
        } else if (chain != null) {
            publicCred.add(chain);
            am = AuthMethod.CERT;
        } else {
            for (final Object o : principals) {
                if (o instanceof HttpPrincipal) {
                    am = AuthMethod.PASSWORD;
                    break;
                }
                if (o instanceof AuthorizationTokenPrincipal || o instanceof BearerTokenPrincipal) {
                    am = AuthMethod.TOKEN;
                    break;
                }
                if (o instanceof CookiePrincipal) {
                    am = AuthMethod.COOKIE;
                    break;
                }
                if (o instanceof OpenIdPrincipal) {
                    am = AuthMethod.TOKEN;
                    break;
                }

            }
        }

        Subject subject = new Subject(false, principals, publicCred, privateCred);
        subject = validateSubject(subject);
        // reject un-validated authorization
        Set<AuthorizationTokenPrincipal> unvalidated = subject.getPrincipals(AuthorizationTokenPrincipal.class);
        if (!unvalidated.isEmpty()) {
            AuthorizationTokenPrincipal atp = unvalidated.iterator().next();
            throw new NotAuthenticatedException("unhandled auth: " + atp.getHeaderKey() + " " + atp.getHeaderValue());
        }
        setAuthMethod(subject, am);
        if (augmentSubject) {
            subject = augmentSubject(subject);
        }
        log.debug("getSubject(augment=" + augmentSubject + "): " + subject);
        return subject;
    }

    /**
     * Convenience method to augment the extracted Subject.
     *
     * @param principalExtractor The PrincipalExtractor to provide Principals.
     * @return A new Subject.
     */
    public static Subject getSubject(PrincipalExtractor principalExtractor) {
        return getSubject(principalExtractor, true);
    }

    /**
     * Convenience method that uses a ServletPrincipalExtractor.
     *
     * @param request The HTTP Request.
     * @param augmentSubject Whether to further augment the subject using an Authentication interface.
     * @return a Subject with all available request content
     * @see #getSubject(PrincipalExtractor)
     */
    public static Subject getSubject(final HttpServletRequest request, boolean augmentSubject) {
        return getSubject(new ServletPrincipalExtractor(request), augmentSubject);
    }

    /**
     * Convenience method that uses a ServletPrincipalExtractor and augments
     * the extracted Subject using an Authenticator interface.
     *
     * @param request The HTTP Request.
     * @return a Subject with all available request content
     * @see #getSubject(PrincipalExtractor)
     */
    public static Subject getSubject(final HttpServletRequest request) {
        return getSubject(new ServletPrincipalExtractor(request), true);
    }

    /**
     * Create a subject with the specified certificate chain and private key. This
     * method constructs an X509CertificateChain and then calls
     * getSubject(X509CertificateChain).
     *
     * @param certs a non-null and non-empty certificate chain
     * @param key   optional private key
     * @return a Subject
     */
    public static Subject getSubject(X509Certificate[] certs, PrivateKey key) {
        final X509CertificateChain chain = new X509CertificateChain(certs, key);
        return getSubject(chain);
    }

    /**
     * Create a subject from the specified certificate chain. This method is
     * intended for use by applications that load a certificate and key pair
     * (probably from a file).
     *
     * @param chain The X509Certificate chain of certificates, if any.
     * @return An augmented Subject.
     */
    public static Subject getSubject(X509CertificateChain chain) {
        Set<Principal> principals = new HashSet<Principal>();
        Set<Object> publicCred = new HashSet<Object>();
        Set privateCred = new HashSet();

        // SSL authentication
        if (chain != null) {
            principals.add(chain.getX500Principal());
            publicCred.add(chain);
            // note: we just leave the PrivateKey in the chain (eg public) rather
            // than extracting and putting it into the privateCred set... TBD
        }

        Subject subject = new Subject(false, principals, publicCred, privateCred);
        setAuthMethod(subject, AuthMethod.CERT);
        return subject; // this method for client apps only: no augment
    }

    /**
     * Create a subject for username-password authentication. This method sets a
     * global <code>java.net.Authenticator</code> instance so can only be used
     * safely in a single application environment. It is intended for use with
     * command-line apps using the NetrcAuthenticator.
     *
     * @param authenticator
     * @return
     */
    public static Subject getSubject(java.net.Authenticator authenticator) {
        Set<Principal> principals = new HashSet<Principal>();
        Set<Object> publicCred = new HashSet<Object>();
        Set privateCred = new HashSet();

        if (authenticator != null) {
            java.net.Authenticator.setDefault(authenticator);
            publicCred.add(new PasswordCredentials()); // tag subject
        }

        Subject subject = new Subject(false, principals, publicCred, privateCred);
        setAuthMethod(subject, AuthMethod.PASSWORD);
        return subject; // this method for client apps only: no augment
    }

    // Encode a Subject in the format:
    // Principal Class name[Principal name]
    public static String encodeSubject(Subject subject) {
        if (subject == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();

        for (final Principal principal : subject.getPrincipals()) {
            sb.append(principal.getClass().getName());
            sb.append("[");
            sb.append(NetUtil.encode(principal.getName()));
            sb.append("]");
        }
        return sb.toString();
    }

    /**
     * Get corresponding user IDs from Subject's HttpPrincipals
     *
     * @return set of user ids extracted from the HttpPrincipals
     */
    public static Set<String> getUseridsFromSubject() {
        AccessControlContext acc = AccessController.getContext();
        Subject subject = Subject.getSubject(acc);

        Set<String> userids = new HashSet<String>();
        if (subject != null) {
            final Set<HttpPrincipal> httpPrincipals = subject.getPrincipals(HttpPrincipal.class);
            final Set<CookiePrincipal> cookiePrincipals = subject.getPrincipals(CookiePrincipal.class);
            String userId;

            for (final HttpPrincipal principal : httpPrincipals) {
                userId = principal.getName();
                userids.add(userId);
            }
        }
        return userids;
    }

    // Build a Subject from the encoding.
    @SuppressWarnings("unchecked")
    public static Subject decodeSubject(String s) {
        if (s == null || s.length() == 0) {
            return null;
        }
        Subject subject = null;
        int prStart = 0;
        int nameStart = s.indexOf("[", prStart);
        try {
            while (nameStart != -1) {
                int nameEnd = s.indexOf("]", nameStart);
                if (nameEnd == -1) {
                    log.error("Invalid Principal encoding: " + s);
                    return null;
                }
                Class c = Class.forName(s.substring(prStart, nameStart));
                Class[] args = new Class[] { String.class };
                Constructor constructor = c.getDeclaredConstructor(args);
                String name = NetUtil.decode(s.substring(nameStart + 1, nameEnd));
                Principal principal = (Principal) constructor.newInstance(name);
                if (subject == null) {
                    subject = new Subject();
                }
                subject.getPrincipals().add(principal);
                prStart = nameEnd + 1;
                nameStart = s.indexOf("[", prStart);
            }
        } catch (IndexOutOfBoundsException ioe) {
            log.error(ioe.getMessage(), ioe);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return subject;
    }

    /**
     * Re-order the pairs in the X500 distinguished name to standard order. This
     * method causes the pairs to be ordered such that the user parts (CN) is first
     * and the country (C) is last in the string form of the distinguished name.
     *
     * @param p
     * @return ordered principal, possibly the argument if re-order not required
     */
    public static X500Principal getOrderedForm(X500Principal p) {
        try {
            X500Principal ret = p;
            String up = p.getName(X500Principal.RFC2253);
            LdapName dn = new LdapName(up);
            List<Rdn> rdns = dn.getRdns();
            Rdn left = rdns.get(rdns.size() - 1); // LDAP order from right-left
            Rdn right = rdns.get(0);
            // boolean cnOnLeft = "CN".equalsIgnoreCase(left.getType());
            boolean cnOnleft = "C".equalsIgnoreCase(left.getType());
            boolean cnOnRight = "CN".equalsIgnoreCase(right.getType());
            // boolean cOnRight = "C".equalsIgnoreCase(right.getType());
            boolean flip = (cnOnRight || cnOnleft);

            StringBuilder sb = new StringBuilder();
            if (flip) {
                for (Rdn r : rdns) {
                    // writing in normal order is actually flipping LDAP order
                    sb.append(r.toString());
                    sb.append(",");
                }
            } else {
                for (int i = rdns.size() - 1; i >= 0; i--) {
                    sb.append(rdns.get(i));
                    sb.append(",");
                }
            }
            ret = new X500Principal(sb.substring(0, sb.length() - 1)); // strip off comma-space
            log.debug("ordered form of " + up + " is " + ret);
            return ret;
        } catch (InvalidNameException ex) {
            throw new IllegalArgumentException("invalid DN: " + p.getName(), ex);
        } finally {
            // do nothing
        }
    }

    /**
     * Group together Subject principal types with their Principal values.
     *
     * @param <T> The type of Principal.
     * @return Map of class to collection of string values.
     */
    public static <T extends Principal> Map<Class<T>, Collection<String>> groupPrincipalsByType() {
        final Map<Class<T>, Collection<String>> groupedPrincipals = new HashMap<Class<T>, Collection<String>>();

        for (final Principal p : getCurrentSubject().getPrincipals()) {
            final Class<T> nextPrincipalClass = (Class<T>) p.getClass();

            if (!groupedPrincipals.containsKey(p.getClass())) {
                groupedPrincipals.put(nextPrincipalClass, new HashSet<String>());
            }

            groupedPrincipals.get(nextPrincipalClass).add(p.getName());
        }

        return groupedPrincipals;
    }

    /**
     * Given two principal objects, return true if they represent the same identity.
     * <p>
     * The equality is defined by each principal type through the equal method, with
     * the exception of X500Principals: if the principals are instances of
     * X500Principal, the cannonical form of their names are compared.
     * </p>
     * Two null principals are considered equal.
     *
     * @param p1 Principal object 1.
     * @param p2 Principal object 2.
     * @return True if they are equal, false otherwise.
     */
    public static boolean equals(Principal p1, Principal p2) {
        if (p1 == null && p2 == null) {
            return true;
        }

        if (p1 == null || p2 == null) {
            return false;
        }

        return AuthenticationUtil.compare(p1, p2) == 0;
    }

    /**
     * Compare two principals
     */
    public static int compare(Principal p1, Principal p2) {
        if (p1 == null || p2 == null) {
            throw new IllegalArgumentException("Cannot compare null objects");
        }

        if (p1 instanceof X500Principal) {
            if (p2 instanceof X500Principal) {
                String converted1 = canonizeDistinguishedName(p1.getName());
                String converted2 = canonizeDistinguishedName(p2.getName());
                return converted1.compareTo(converted2);
            }
        }

        if (p1 instanceof HttpPrincipal) {
            if (p2 instanceof HttpPrincipal) {
                HttpPrincipal h1 = (HttpPrincipal) p1;
                HttpPrincipal h2 = (HttpPrincipal) p2;
                return h1.toString().compareTo(h2.toString());
            }
        }

        if (p1.getClass().equals(p2.getClass())) {
            return p1.getName().compareTo(p2.getName());
        }

        return p1.getClass().getName().compareTo(p2.getClass().getName());
    }

    /**
     * Perform extended canonization operation on a distinguished name.
     * <p>
     * This method will convert the DN to a format that:
     * </p>
     * <ul>
     * <li>Is all lower case.</li>
     * <li>RDNs are separated by commas and no spaces.</li>
     * <li>RDNs are in the order specified by ORDERED_RDN_KEYS. If more than one RDN
     * of the same key exists, these are ordered among each other by their value by
     * String.compareTo(String another).</li>
     * <li>If other RDNs exist in that are not in ORDERED_RDN_KEYS, an
     * IllegalArgumentException is thrown.
     * </ul>
     *
     * <p>
     * Please see RFC#4514 for more information.
     * </p>
     *
     * @param dnSrc
     * @return canonized distinguished name
     */
    public static String canonizeDistinguishedName(String dnSrc) {
        try {
            X500Principal x = new X500Principal(dnSrc);
            x = AuthenticationUtil.getOrderedForm(x);
            String ret = x.getName().trim().toLowerCase();
            log.debug(dnSrc + " converted to " + ret);
            return ret;
        } catch (Exception e) {
            log.debug("Invalid dn", e);
            throw new IllegalArgumentException("Invalid DN: " + dnSrc, e);
        }
    }

    /**
     * Object the X500Principal from a Subject.
     *
     * @param subject
     * @return X500 Principal
     */
    public static X500Principal getX500Principal(Subject subject) {
        X500Principal x500Principal = null;
        Set<Principal> principals = subject.getPrincipals();
        for (Principal principal : principals) {
            if (principal instanceof X500Principal) {
                x500Principal = (X500Principal) principal;
            }
        }
        return x500Principal;
    }

    /**
     * This method checks the validity of X509Certificates associated with a
     * subject.
     *
     * @param subject subject holding the certificates to be validated
     * @throws CertificateException            Null subject or no certificates found
     * @throws CertificateNotYetValidException certificate not yet valid
     * @throws CertificateExpiredException     certificate is expired
     */
    public static void checkCertificates(final Subject subject)
            throws CertificateException, CertificateNotYetValidException, CertificateExpiredException {
        // check validity
        if (subject != null) {
            Set<X509CertificateChain> certs = subject.getPublicCredentials(X509CertificateChain.class);
            if (certs.isEmpty()) {
                // subject without certs means something went wrong above
                throw new CertificateException("No certificates associated with the subject");
            }
            DateFormat df = DateUtil.getDateFormat(DateUtil.ISO_DATE_FORMAT, DateUtil.LOCAL);
            X509CertificateChain chain = certs.iterator().next();
            Date start = null;
            Date end = null;
            for (X509Certificate c : chain.getChain()) {
                try {
                    start = c.getNotBefore();
                    end = c.getNotAfter();
                    c.checkValidity();
                } catch (CertificateExpiredException exp) {
                    // improve the message
                    String msg = "certificate has expired (valid from " + df.format(start) + " to " + df.format(end)
                            + ")";
                    throw new CertificateExpiredException(msg);
                } catch (CertificateNotYetValidException exp) {
                    // improve the message
                    String msg = "certificate not yet valid (valid from " + df.format(start) + " to " + df.format(end)
                            + ")";
                    throw new CertificateNotYetValidException(msg);
                }
            }
        } else {
            throw new CertificateException("No certificates (Null subject)");
        }
    }

    /**
     * Convenience method for often recurring task.
     *
     * @return Current Subject, or null if none.
     */
    public static Subject getCurrentSubject() {
        final AccessControlContext accessControlContext = AccessController.getContext();
        return Subject.getSubject(accessControlContext);
    }

    public static Principal createPrincipal(String userID, String idType) {
        if (IdentityType.X500.getValue().equalsIgnoreCase(idType)) {
            return new X500Principal(AuthenticationUtil.canonizeDistinguishedName(userID));
        }
        if (IdentityType.USERNAME.getValue().equalsIgnoreCase(idType)) {
            return new HttpPrincipal(userID);
        }
        if (IdentityType.CADC.getValue().equalsIgnoreCase(idType)) {
            return new NumericPrincipal(UUID.fromString(userID));
        }
        return null;
    }

    public static String getPrincipalType(Principal userID) {
        if (userID instanceof X500Principal) {
            return IdentityType.X500.getValue().toLowerCase();
        }
        if (userID instanceof HttpPrincipal) {
            return IdentityType.USERNAME.getValue().toLowerCase();
        }
        if (userID instanceof NumericPrincipal) {
            return IdentityType.CADC.getValue().toLowerCase();
        }
        if (userID instanceof PosixPrincipal) {
            return IdentityType.POSIX.getValue().toLowerCase();
        }
        return null;
    }
}
