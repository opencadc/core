/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2016.                            (c) 2016.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.Principal;
import java.util.Date;
import ca.nrc.cadc.util.Base64;
import ca.nrc.cadc.util.RsaSignatureGenerator;
import ca.nrc.cadc.util.RsaSignatureVerifier;
import ca.nrc.cadc.util.StringUtil;

import java.util.HashMap;
import java.util.HashSet;
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
 *
 * This class can serialize and de-serialize the information into a
 * String token. Optionally, it can sign the serialized token and
 * verify when one is received.
 */
public class DelegationToken implements Serializable
{
    private static final Logger log = Logger.getLogger(DelegationToken.class);

    private static final long serialVersionUID = 20180321000000l;

    private Set<Principal> identityPrincipals;
    private Date expiryTime; // expiration time of the delegation (UTC)
    private URI scope; // resources that are the object of the delegation

    public static final String FIELD_DELIM = "&";
    public static final String VALUE_DELIM = "=";

    public static class ScopeValidator
    {
        public void verifyScope(URI scope, String requestURI)
            throws InvalidDelegationTokenException
        {
            throw new InvalidDelegationTokenException("default: invalid scope");
        }
    }

    /**
     * Constructor.
     *
     * @param user identity of the delegating user - required
     * @param scope - scope of the delegation, i.e. resource that it applies
     * to - optional
     * @param expiryTime - the expiry date of this token (UTC)
     */
    public DelegationToken(HttpPrincipal user, URI scope, Date expiryTime)
    {
        // Validation of parameter means using this() to call
        // other constructor isn't possible.
        if (user == null)
        {
            throw new IllegalArgumentException("User identity required");
        }
        Set<Principal> principalSet = new HashSet<Principal>();
        principalSet.add(user);

        this.identityPrincipals = principalSet;

        if(expiryTime == null)
        {
            throw new IllegalArgumentException("No expiry time");
        }
        this.expiryTime = expiryTime;
        this.scope = scope;
    }

    /**
     * Constructor.
     * @param principals - set of identity principals (http, x500, cadc)
     * @param scope - scope of the delegation, i.e. resource that it applies to - optional
     * @param expiryTime - the expiry date of this token (UTC)
     */
    public DelegationToken(Set<Principal> principals, URI scope, Date expiryTime)
    {
        if (principals == null || principals.size() == 0)
        {
            throw new IllegalArgumentException("Identity principals required (ie http, x500, cadc internal)");
        }
        this.identityPrincipals = principals;

        if(expiryTime == null)
        {
            throw new IllegalArgumentException("No expiry time");
        }
        this.expiryTime = expiryTime;
        this.scope = scope;
    }

    /**
     * Serializes and signs the object into a string of attribute-value pairs.
     *
     * @param token the token to format
     * the returned string
     * @return String with DelegationToken information
     * @throws IOException
     * @throws InvalidKeyException
     */
    public static String format(DelegationToken token)
        throws InvalidKeyException, IOException
    {
        StringBuilder sb = getContent(token);

        //sign and add the signature field
        String toSign = sb.toString();
        sb.append(FIELD_DELIM);
        sb.append("signature");
        sb.append(VALUE_DELIM);
        RsaSignatureGenerator su = new RsaSignatureGenerator();
        byte[] sig =
                su.sign(new ByteArrayInputStream(toSign.getBytes()));
        sb.append(new String(Base64.encode(sig)));

        return sb.toString();
    }

    // the formatted content without the signature
    private static StringBuilder getContent(DelegationToken token)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("expirytime");
        sb.append(VALUE_DELIM);
        sb.append(token.getExpiryTime().getTime());

        // Add all available identity principals to the content
        for (Principal prin: token.identityPrincipals) {
            String tmpKey = "";
            if (prin.getClass() == HttpPrincipal.class) {
                tmpKey = "userid";
            } else if (prin.getClass() == X500Principal.class) {
                tmpKey = "x500";
            } else if (prin.getClass() == NumericPrincipal.class) {
                tmpKey = "cadc";
            } else {
                continue;
            }
            sb.append(FIELD_DELIM);
            sb.append(tmpKey);
            sb.append(VALUE_DELIM);
            sb.append(prin.getName());
        }
        HttpPrincipal user = token.getUser();
        if (StringUtil.hasText(user.getProxyUser()))
        {
            sb.append(FIELD_DELIM);
            sb.append("proxyuser");
            sb.append(VALUE_DELIM);
            sb.append(user.getProxyUser());
        }

        if (token.getScope() != null)
        {
            sb.append(FIELD_DELIM);
            sb.append("scope");
            sb.append(VALUE_DELIM);
            sb.append(token.getScope());
        }
        log.info("getContent: " + sb);
        return sb;
    }

    /**
     *
     * @param text
     * @param requestURI
     * @return
     * @throws InvalidDelegationTokenException
     */
    public static DelegationToken parse(String text, String requestURI)
            throws InvalidDelegationTokenException
    {
        return parse(text, requestURI, null);
    }

    /**
     * Builds a DelegationToken from a text string
     * @param text to parse
     * @param requestURI the request URI
     * @param sv
     * @return corresponding DelegationToken
     * @throws InvalidDelegationTokenException
     */
    public static DelegationToken parse(String text, String requestURI, ScopeValidator sv)
            throws InvalidDelegationTokenException
    {
        String[] fields = text.split(FIELD_DELIM);
        String userid = null;
        Set<Principal> principalSet = new HashSet<>();
        String proxyUser = null;
        Date expirytime = null;
        URI scope = null;
        String signature = null;
        try
        {
            for (String field : fields)
            {
                String key = field.substring(0, field.indexOf(VALUE_DELIM));
                String value = field.substring(field.indexOf(VALUE_DELIM) + 1);
                if (key.equalsIgnoreCase("userid"))
                {
                    userid = value;
                }
                if (key.equalsIgnoreCase("proxyuser"))
                {
                    proxyUser = value;
                }
                if (key.matches("x500")) {
                    principalSet.add(new X500Principal(value));
                }
                // Treating CADC principal as a NumericPrincipal
                if (key.equalsIgnoreCase("cadc")) {
                    principalSet.add(new NumericPrincipal(UUID.fromString(value)));
                }

                if (key.equalsIgnoreCase("expirytime"))
                {
                    expirytime = new Date(Long.valueOf(value));
                }
                if (key.equalsIgnoreCase("scope"))
                {
                    scope = new URI(value);
                }
                if (key.equalsIgnoreCase("signature"))
                {
                    signature = value;
                }
            }

            // Construct HttpPrincipal
            if (userid != null && proxyUser != null) {
                principalSet.add(new HttpPrincipal(userid, proxyUser));
            } else if (userid != null) {
                principalSet.add(new HttpPrincipal(userid));
            } // Should no HttpPrincipal generation be flagged here?
            
        }
        catch (NumberFormatException ex)
        {
            throw new InvalidDelegationTokenException("invalid numeric field", ex);
        }
        catch (URISyntaxException ex)
        {
            throw new InvalidDelegationTokenException("invalid scope URI", ex);
        }

        if (signature == null)
            throw new InvalidDelegationTokenException("Missing signature");

        // validate expiry
        if (expirytime == null)
            throw new InvalidDelegationTokenException("missing expirytime");
        Date now = new Date();

        if (now.getTime() > expirytime.getTime())
            throw new InvalidDelegationTokenException("expired");

        // validate scope
        if (scope != null)
        {
            if (sv == null) // not supplied
                sv = getScopeValidator();
            sv.verifyScope(scope, requestURI);
        }
        
        // validate signature
        DelegationToken result = 
                new DelegationToken(principalSet, scope, expirytime);

        try
        {
            RsaSignatureVerifier su = new RsaSignatureVerifier();
            String str = DelegationToken.getContent(result).toString();
            boolean valid = su.verify(
                new ByteArrayInputStream(str.getBytes()),
                    Base64.decode(signature));
            if (valid)
                return result;
            log.debug("invalid token: " + str);

        }
        catch(Exception ex)
        {
            log.debug("failed to verify DelegationToken signature", ex);
        }

        throw new InvalidDelegationTokenException("Cannot verify signature");
    }

    private static ScopeValidator getScopeValidator()
    {
        try
        {
            String fname = DelegationToken.class.getSimpleName()+".properties";
            String pname = DelegationToken.class.getName() + ".scopeValidator";
            Properties props = new Properties();
            props.load(DelegationToken.class.getClassLoader().getResource(fname).openStream());
            String cname = props.getProperty(pname);
            log.debug(fname + ": " + pname + " = " + cname);
            Class c = Class.forName(cname);
            ScopeValidator ret = (ScopeValidator) c.newInstance();
            log.debug("created: " + ret.getClass().getName());
            return ret;
        }
        catch(Exception ignore)
        {
            log.debug("failed to load custom ScopeValidator", ignore);
        }
        finally { }

        // default
        return new ScopeValidator();
    }

    public Set<Principal> getIdentityPrincipals() {
        return identityPrincipals;
    }

    public HttpPrincipal getUser() {
        return getPrincipalByClass(HttpPrincipal.class);
    }

    public <T extends Principal> T getPrincipalByClass(Class clazz) {
        for (Principal prin : this.identityPrincipals) {
            if (prin.getClass() == clazz) {
                return (T)prin;
            }
        }
        return null;
    }

    public Date getExpiryTime()
    {
        return expiryTime;
    }

    public URI getScope()
    {
        return scope;
    }

    // TODO: update this
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("DelegationToken(user=");
        if (StringUtil.hasText(getUser().getProxyUser()))
        {
            sb.append(",proxyUser=");
            sb.append(getUser().getProxyUser());
        }
        sb.append(getUser());
        sb.append(",scope=");
        sb.append(getScope());
        sb.append(",startTime=");
        sb.append(getExpiryTime());
        sb.append(")");
        return sb.toString();
    }

}
