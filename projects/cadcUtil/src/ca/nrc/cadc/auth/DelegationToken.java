/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2014.                            (c) 2014.
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
import java.util.Date;
import ca.nrc.cadc.util.Base64;
import ca.nrc.cadc.util.PropertiesReader;
import ca.nrc.cadc.util.RsaSignatureGenerator;
import ca.nrc.cadc.util.RsaSignatureVerifier;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 * Class that captures the information required to perform delegation (i.e.
 * accessing resources on user's behalf). The required fields are:
 * - the user that delegates
 * - start time of the delegation
 * - duration
 * - scope of delegation
 * 
 * This class can serialize and de-serialize the information into a 
 * String token. Optionally, it can sign the serialized token and
 * verify when one is received. 
 */
public class DelegationToken implements Serializable
{
    private static final Logger log = Logger.getLogger(DelegationToken.class);
    
    private static final long serialVersionUID = 20141025143750l;

    private HttpPrincipal user; // identity of the user
    private Date startTime; // start time of the delegation (UTC)
    private int duration; // duration of delegation (h)
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
     * @param duration - duration of the delegation. Replaced with default if
     * less then 0h.
     * @param scope - scope of the delegation, i.e. resource that it applies
     * to - optional
     * @param startTime - the start date of this token (UTC)
     */    
    public DelegationToken(HttpPrincipal user, int duration,  URI scope, Date startTime)
    {
        if (user == null)
        {
            throw new IllegalArgumentException("User identity required");
        }
        this.user = user;
        if(startTime == null)
        {
            throw new IllegalArgumentException("No start time");
        }
        this.startTime = startTime;
        if (duration > 0)
        {
            this.duration = duration;
        }
        else
        {
            throw new IllegalArgumentException("Negative duration: " + duration);
        }
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
        sb.append("userid");
        sb.append(VALUE_DELIM);
        sb.append(token.getUser().getName());
        sb.append(FIELD_DELIM);
        sb.append("starttime");
        sb.append(VALUE_DELIM);
        sb.append(token.getStartTime().getTime());
        sb.append(FIELD_DELIM);
        sb.append("duration");
        sb.append(VALUE_DELIM);
        sb.append(token.getDuration());
        if (token.getScope() != null)
        {
            sb.append(FIELD_DELIM);
            sb.append("scope");
            sb.append(VALUE_DELIM);
            sb.append(token.getScope());
        }
        return sb;
    }
    
    /**
     * Builds a DelegationToken from a text string
     * @param text to parse
     * @param requestURI the request URI
     * @return corresponding DelegationToken
     * @throws InvalidDelegationTokenException
     */
    public static DelegationToken parse(String text, String requestURI) 
            throws InvalidDelegationTokenException
    {
        String[] fields = text.split(FIELD_DELIM);
        HttpPrincipal userid = null;
        Date starttime = null;
        int duration = -1;
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
                    userid = new HttpPrincipal(value);
                }
                if (key.equalsIgnoreCase("duration"))
                {
                    duration = Integer.valueOf(value);
                }
                if (key.equalsIgnoreCase("starttime"))
                {
                    starttime = new Date(Long.valueOf(value));
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
        }
        catch(NumberFormatException ex)
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
        if (starttime == null)
            throw new InvalidDelegationTokenException("missing starttime");
        Date now = new Date();
        long durationMs = duration*60*60*1000;
        if ( (now.getTime() < starttime.getTime()) ||
                ((now.getTime() - starttime.getTime()) > durationMs) )
            throw new InvalidDelegationTokenException("expired");
        
        // validate scope
        ScopeValidator sv = getScopeValidator();
        sv.verifyScope(scope, requestURI);
        
        // validate signature
        DelegationToken result = 
                new DelegationToken(userid, duration, scope, starttime);
        
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

    public HttpPrincipal getUser()
    {
        return user;
    }


    public Date getStartTime()
    {
        return startTime;
    }


    public int getDuration()
    {
        return duration;
    }


    public URI getScope()
    {
        return scope;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("DelegationToken(user=");
        sb.append(getUser());
        sb.append(",scope=");
        sb.append(getScope());
        sb.append(",startTime=");
        sb.append(getStartTime());
        sb.append(",duration=");
        sb.append(getDuration());
        sb.append(")");
        return sb.toString();
    }

}
