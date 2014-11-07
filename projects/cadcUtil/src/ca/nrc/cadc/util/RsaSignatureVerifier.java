/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2014.                            (c) 2014.
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
 *  $Revision: 4 $
 *
 ************************************************************************
 */

package ca.nrc.cadc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashSet;
import java.util.MissingResourceException;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * This class is used to verify signed messages. The class requires
 * an RSA public key to verify a message.
 * 
 * The keys are passed to the class via the RsaSignaturePub.key file in the 
 * classpath. This class cannot be instantiated without this file containing
 * public RSA keys.
 * 
 * Format of the keys:
 * Public keys in the MessageRSA.keys file must be in PEM TKCS#1These keys are
 * in text format delimited by the following rows: "-----BEGIN PUBLIC KEY-----"
 * and "-----END PUBLIC KEY-----".
 * 
 * There are a variety of tools to generate RSA keys, the most common being
 * openssl and ssh-keygen. These tools allow for both the generation and 
 * manipulation of keys.
 * 
 * For example, an ssh public key is converted to a PEM TKCS#1 with the command:
 * ssh-keygen -f <ssh pub key> -e -m pkcs8. 
 * 
 * @author adriand
 * 
 */
public class RsaSignatureVerifier
{
    private static Logger log = Logger.getLogger(RsaSignatureVerifier.class);


    protected static RsaSignatureVerifier inst;
    protected Set<PublicKey> pubKeys = new HashSet<PublicKey>();
    protected static final String KEY_ALGORITHM = "RSA";
    protected static final String SIG_ALGORITHM = "SHA1withRSA";
    
    public static final String PUB_KEY_FILE_NAME = "RsaSignaturePub.key";
            
    
    public static final String PUB_KEY_START = "-----BEGIN PUBLIC KEY-----";
    public static final String PUB_KEY_END = "-----END PUBLIC KEY-----";

    
    /**
     * Default ctor
     */
    public RsaSignatureVerifier()
    {
        this(false);
    }
    
    
    /**
     * constructor
     * @param privateKeyExpected - ctor instantiated in the context in which 
     * a private key is also expected
     */
    protected RsaSignatureVerifier(boolean privateKeyExpected)
    {
        KeyFactory keyFactory = null;
        try
        {
            keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        } 
        catch (NoSuchAlgorithmException e1)
        {
            throw new RuntimeException("BUG: Wrong algorithm " + KEY_ALGORITHM,
                    e1);
        }
        // try to load the keys
        try
        {
            File keysFile = null;
            try
            {
               keysFile = FileUtil.getFileFromResource(
                    PUB_KEY_FILE_NAME, this.getClass());
            }
            catch (MissingResourceException ex)
            {
                if (privateKeyExpected)
                {
                    return;
                }
                else
                {
                    throw ex;
                }
            }
            
            BufferedReader br = new BufferedReader(new 
                    FileReader(keysFile));
            try
            {
                StringBuilder sb = null;
                boolean readPub = false;
                String line = null;
                while ((line = br.readLine()) != null)
                {
                    if (line.equalsIgnoreCase(PUB_KEY_START))
                    {
                        if (readPub)
                        {
                            throw new 
                                IllegalArgumentException("Corrupted keys file");
                        }
                        readPub = true;
                        sb = new StringBuilder();
                        continue;
                    }
                    if (line.equalsIgnoreCase(PUB_KEY_END))
                    {
                        if (!readPub)
                        {
                            throw new 
                            IllegalArgumentException("Corrupted keys file");
                        }
                        readPub = false;
                        String payload = sb.toString();
                        byte[] bytes = Base64.decode(payload);
                        X509EncodedKeySpec publicKeySpec = 
                                new X509EncodedKeySpec(bytes);
                        try
                        {
                            pubKeys.add(keyFactory.generatePublic(publicKeySpec));
                        } 
                        catch (InvalidKeySpecException e)
                        {
                            log.warn("Could not parse public key", e);
                        }
                    }
                    if (readPub)
                    {
                        sb.append(line);
                    }
                }
            } 
            finally
            {
                br.close();
            }
        } 
        catch (IOException e)
        {
            String msg = "Could not read keys";
            log.error(msg);
            throw new RuntimeException(msg, e);
        }
        
        if ( !privateKeyExpected && (pubKeys.size() == 0))
        {
            String msg = "No valid public keys found";
            log.error(msg);
            throw new IllegalStateException(msg);
        }       
    }


    /**
     * Method use to verify a stream
     * @param is input stream to be verified
     * @param sigBytes signature associated with the input stream
     * @return true if signature matches, false otherwise
     * @throws IOException - IO problems
     * @throws InvalidKeyException - the provided key is invalid
     */
    public boolean verify(InputStream is, byte[] sigBytes) throws IOException,
            InvalidKeyException
    {
        if (pubKeys.size() == 0)
        {
            throw new IllegalStateException(
                    "No public keys available for verifying");
        }
        try
        {       
            Set<Signature> sigs = new HashSet<Signature>(pubKeys.size());
            for( PublicKey pubKey : pubKeys)
            {
                Signature sig = getSignature();
                sig.initVerify(pubKey);
                sigs.add(sig);
            }

            byte[] data = new byte[1024];
            int nRead = is.read(data);
            while (nRead > 0)
            {
                for (Signature sig : sigs)
                {
                    sig.update(data, 0, nRead);
                }
                nRead = is.read(data);
            }
            for (Signature sig : sigs)
            {
                if (sig.verify(sigBytes))
                {
                    return true;
                }
            }

            return false;
        } 
        catch (SignatureException e)
        {
            throw new RuntimeException("Signature problem", e);
        }
    }

    public Set<PublicKey> getPublicKeys()
    {
        return pubKeys;
    }
    
    private Signature getSignature()
    {
        try
        {
            return Signature.getInstance(SIG_ALGORITHM);
        } catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("BUG: Wrong signature algorithm "
                    + SIG_ALGORITHM, e);
        }
    }
}
