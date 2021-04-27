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
 * 4/10/12 - 3:10 PM
 *
 *
 *
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */

package ca.nrc.cadc.auth;

import java.io.Serializable;
import java.security.Principal;

/**
 * Represents the key and value of a Cookie as a principal.
 * Interface Principal.java expects getName() to return
 * the value (id) of the principal.  The key variable in 
 * this class is for representing the 'name' of a cookie.
 */
public class CookiePrincipal implements Principal, Serializable {
    private static final long serialVersionUID = 20130313151134L;

    private String key;
    private String value;

    /**
     * Constructor
     * @param key The 'name' of the cookie
     * @param value The cookie value
     */
    public CookiePrincipal(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("key required");
        }
        if (value == null) {
            throw new IllegalArgumentException("value required");
        }
        this.key = key;
        this.value = value;
    }

    /**
     * Returns the name (value) of this principal.
     *
     * @return the name of this principal.
     */
    @Override
    public String getName() {

        return key;
    }
    
    /**
     * The cookie 'name'
     * @return The cookie 'name'
     */
    public String getKey() {
        return key;
    }

    /**
     * The cookie value
     * @return The cookie value
     */
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof CookiePrincipal)) {
            return false;
        }
        CookiePrincipal c = (CookiePrincipal) o;
        return c.getKey().equals(key) && c.getValue().equals(value);
    }
    
    @Override
    public String toString() {
        return "CookiePrincipal[" + key + "=" + value + "]";
    }

}
