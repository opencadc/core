/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2024.                            (c) 2024.
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
 * @author adriand
 * 
 * @version $Revision: $
 * 
 * 
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */

package ca.nrc.cadc.auth;

import java.io.Serializable;
import java.net.URL;
import java.security.Principal;

/**
 * Class that represents an openID identity. The principal consists of an immutable
 * open ID and its corresponding issuer.
 */
public class OpenIdPrincipal implements Principal, Serializable {
    private static final long serialVersionUID = 202407041230L;

    private final String sub;
    private final URL issuer;

    /**
     * Ctor
     *
     * @param issuer The issuer of the Open ID
     * @param sub Subject identifier.
     */
    public OpenIdPrincipal(final URL issuer, final String sub) {
        if (issuer == null) {
            throw new IllegalArgumentException("null issuer");
        }
        if (sub == null) {
            throw new IllegalArgumentException("null sub");
        }
        this.sub = sub;
        this.issuer = issuer;
    }

    @Override
    public String getName() {
        return sub;
    }

    public URL getIssuer() {
        return issuer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + sub.hashCode() + issuer.hashCode();
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OpenIdPrincipal)) {
            return false;
        }
        OpenIdPrincipal other = (OpenIdPrincipal) obj;
        return sub.equals(other.sub) && issuer.equals(other.issuer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[issuer=" + getIssuer() + ", openID=" + getName() + "]";
    }

}
