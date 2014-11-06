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
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ca.nrc.cadc.date.DateUtil;
import ca.nrc.cadc.util.Base64;
import ca.nrc.cadc.util.RsaSignatureGenerator;

/**
 * Class that captures the information required to perform delegation (i.e.
 * accessing resources on user's behalf). The required fields are:
 * - the user that delegates
 * - time of the delegation
 * - scope of delegation
 * 
 * This class can serialize and de-serialize the information into a 
 * String token. Optionally, it can sign the serialized token and
 * verify when one is received. 
 */
public class DelegationToken implements Serializable
{
    private static final long serialVersionUID = 20141025143750l;

    private HttpPrincipal user; // identity of the user
    private Date timestamp; // time of the delegation (UTC)
    private int duration; // duration of delegation (h)
    private URI scope; // resources that are the object of the delegation
    
    public static final String FIELD_DELIM = "&";
    public static final String VALUE_DELIM = "=";
    
    
    /**
     * Ctor
     * @param user identity of the delegating user - required 
     * @param duration - duration of the delegation. Replaced with default if
     * less then 0h.
     * @param scope - scope of the delegation, i.e. resource that it applies
     * to - optional
     */
    public DelegationToken(final HttpPrincipal user, int duration, 
            final URI scope)
    {
        this(user, duration, scope, new Date());
    }
    
    private DelegationToken(final HttpPrincipal user, int duration, 
            final URI scope, Date timestamp)
    {
        if (user == null)
        {
            throw new IllegalArgumentException("User identity required");
        }
        this.user = user;
        if(timestamp == null)
        {
            throw new IllegalArgumentException("No timestamp");
        }
        this.timestamp = timestamp;
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
     * "Serializes" the object into a string of attribute-value pairs.
     * @param signed - if true, information is signed and signature included in
     * the returned string
     * @return String with DelegationToken information
     * @throws IOException 
     * @throws InvalidKeyException 
     */
    public String format(boolean signed) throws InvalidKeyException, IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("userid");
        sb.append(VALUE_DELIM);
        sb.append(user.getName());
        sb.append(FIELD_DELIM);
        sb.append("timestamp");
        sb.append(VALUE_DELIM);
        sb.append(timestamp.getTime());
        sb.append(FIELD_DELIM);
        sb.append("duration");
        sb.append(VALUE_DELIM);
        sb.append(duration);
        if (scope != null)
        {
            sb.append(FIELD_DELIM);
            sb.append("scope");
            sb.append(VALUE_DELIM);
            sb.append(scope);
        }
        if (signed)
        {
            //sign and add the signature field
            String toSign = sb.toString();
            sb.append(FIELD_DELIM);
            sb.append("signature");
            sb.append(VALUE_DELIM);
            RsaSignatureGenerator su = new RsaSignatureGenerator();
            byte[] sig = 
                    su.sign(new ByteArrayInputStream(toSign.getBytes()));
            sb.append(new String(Base64.encode(sig)));
        }
        return sb.toString();
    }
    
    /**
     * Builds a DelegationToken from a text string
     * @param string to parse
     * @param true if signature verification required
     * @return corresponding DelegationToken or null if signature does not match
     * @throws URISyntaxException 
     * @throws IOException 
     * @throws InvalidKeyException 
     * @throws ParseException 
     * @throws InvalidDelegationTokenException - token cannot be validated
     */
    public static DelegationToken parse(final String text, boolean signed) 
            throws URISyntaxException, InvalidKeyException, IOException, 
            ParseException, InvalidDelegationTokenException
    {
        String[] fields = text.split(FIELD_DELIM);
        HttpPrincipal userid = null;
        Date timestamp = null;
        int duration = -1;
        URI scope = null;
        String signature = null;
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
            if (key.equalsIgnoreCase("timestamp"))
            {
                timestamp = new Date(Long.valueOf(value));
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
        
        DelegationToken result = 
                new DelegationToken(userid, duration, scope, timestamp);
        if (!signed)
        {
            return result;
        }
        
        if (signature != null)
        {
            // verify the result
            RsaSignatureGenerator su = new RsaSignatureGenerator();
            if (su.verify(new ByteArrayInputStream(
                    result.format(false).getBytes()), 
                    Base64.decode(signature)))
            {
                return result;
            }
            else
            {
                throw new InvalidDelegationTokenException();
            }
        }
        else
        {
            throw new InvalidDelegationTokenException();
        }

    }
    
    /**
     * checks whether the token is still valid
     * @return true - valid token, false otherwise
     */
    public boolean validateExpiry()
    {
        Date now = new Date();
        if ((now.getTime() - this.getTimestamp().getTime()) > 
            (this.getDuration()*60*60*1000))
        {
            return false;
        }
        return true;
    }
    
    public HttpPrincipal getUser()
    {
        return user;
    }


    public Date getTimestamp()
    {
        return timestamp;
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
        sb.append(",timestamp=");
        sb.append(getTimestamp());
        sb.append(",duration=");
        sb.append(getDuration());
        sb.append(")");
        return sb.toString();
    }

}
