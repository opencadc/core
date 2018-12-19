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
import java.security.AccessControlException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import ca.nrc.cadc.net.NetUtil;
import org.apache.log4j.Logger;
import org.apache.xerces.impl.dv.util.Base64;

import ca.nrc.cadc.util.ArrayUtil;
import ca.nrc.cadc.util.StringUtil;


/**
 * Implementation of a Principal Extractor from an HttpServletRequest.
 */
public class ServletPrincipalExtractor implements PrincipalExtractor
{
    private static final Logger log = Logger.getLogger(ServletPrincipalExtractor.class);
    
    public static final String CERT_REQUEST_ATTRIBUTE = "javax.servlet.request.X509Certificate";
    public static final String CERT_HEADER_FIELD = "X-Client-Certificate";
    
    private final HttpServletRequest request;

    private X509CertificateChain chain;
    private DelegationToken token;
    private List<SSOCookieCredential> cookieCredentialList;
    private Set<Principal> cookiePrincipals = new HashSet<>(); // identities extracted from cookie
    private Set<Principal> principals = new HashSet<>();

    private ServletPrincipalExtractor()
    {
        this.request = null;
    }

    /**
     * Constructor to create Principals from the given Servlet Request.
     *
     * @param req       The HTTP Request.
     */
    public ServletPrincipalExtractor(final HttpServletRequest req)
    {
        this.request = req;
        
        // support certs from the java request attribute and from
        // the request header.  favour the request attribute for now.
        X509Certificate[] ca = (X509Certificate[])
            request.getAttribute(CERT_REQUEST_ATTRIBUTE);
        
        if (ArrayUtil.isEmpty(ca)) {
            // try the header field
            String certString = req.getHeader(CERT_HEADER_FIELD);
            log.debug("Cert String: " + certString);
            if (certString != null && certString.length() > 0) {
                try {
                    byte[] certBytes = Base64.decode(certString);
                    if (certBytes != null) {
                        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                        ByteArrayInputStream in = new ByteArrayInputStream(certBytes);
                        List<X509Certificate> certList = (List<X509Certificate>) certFactory.generateCertificates(in);
                        chain = new X509CertificateChain(certList);
                        if (chain != null) {
                            principals.add(chain.getPrincipal());
                        }
                    }
                } catch (CertificateException e) {
                    log.error("Failed to read certificate", e);
                    throw new AccessControlException("Failed to read certificate: " + e.getMessage());
                }
            }
        } else {
            chain = new X509CertificateChain(Arrays.asList(ca));
            if (chain != null) {
                principals.add(chain.getPrincipal());
            }
        }

        // add user if they have a valid delegation token
        String tokenValue = request.getHeader(AuthenticationUtil.AUTH_HEADER);
        if ( StringUtil.hasText(tokenValue) )
        {

            try
            {
                this.token = DelegationToken.parse(tokenValue, 
                        request.getRequestURI());
            }
            catch (InvalidDelegationTokenException ex) 
            {
                log.debug("invalid DelegationToken: " + tokenValue, ex);
                throw new AccessControlException("invalid delegation token");
            }
            catch(RuntimeException ex)
            {
                log.debug("invalid DelegationToken: " + tokenValue, ex);
                throw new AccessControlException("invalid delegation token");
            }
            finally { }
        }

        // add HttpPrincipal
        final String httpUser = request.getRemoteUser();
        if (StringUtil.hasText(httpUser)) // user from HTTP AUTH
            principals.add(new HttpPrincipal(httpUser));
        else if (token != null) // user from token
            principals.add(token.getUser());
        
        Cookie[] cookies = request.getCookies();
        log.debug("Request cookies: " + cookies);
        if (cookies == null || ArrayUtil.isEmpty(cookies))
            return;
        
        for (Cookie ssoCookie : cookies)
        {
            if (SSOCookieManager.DEFAULT_SSO_COOKIE_NAME.equals(
                    ssoCookie.getName())
                    && StringUtil.hasText(ssoCookie.getValue()))
            {
                SSOCookieManager ssoCookieManager = new SSOCookieManager();
                try
                {
                    DelegationToken cookieToken = ssoCookieManager.parse(
                        ssoCookie.getValue());

                    cookiePrincipals = cookieToken.getIdentityPrincipals();
                    principals.addAll(cookiePrincipals);

                    cookieCredentialList = ssoCookieManager.getSSOCookieCredentials(ssoCookie.getValue(),
                                                NetUtil.getDomainName(request.getServerName()));
                } 
                catch (IOException e)
                {
                    log.info("Cannot use SSO Cookie. Reason: " 
                            + e.getMessage());
                } 
                catch (InvalidDelegationTokenException e)
                {
                    log.info("Cannot use SSO Cookie. Reason: " 
                            + e.getMessage());
                }
                
            }
        }
        
    }

    /**
     * Obtain a Collection of Principals from this extractor.  This should be
     * immutable.
     *
     * @return Collection of Principal instances, or empty Collection.
     *         Never null.
     */
    @Override
    public Set<Principal> getPrincipals()
    {
        return principals;
    }

    public X509CertificateChain getCertificateChain()
    {
        return chain;
    }

    public DelegationToken getDelegationToken() 
    {
        return token;
    }
    
    public List<SSOCookieCredential> getSSOCookieCredentials() {
        return cookieCredentialList;
    }

}
