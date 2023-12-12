/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2019.                            (c) 2019.
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

package ca.nrc.cadc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;

import org.apache.log4j.Logger;

/**
 * This class is used to sign and/or verify signed messages. The class requires
 * and RSA private key to sign a message.
 * 
 * <p>The key is passed to the class via a file or as a binary array.
 * 
 * <p>Format of the key:
 * The private key in the MessageRSA.keys file must be in PEM TKCS#8 to work
 * with basic Java. These keys are in text format delimited by the following 
 * rows: 
 * "-----BEGIN PRIVATE KEY-----" and "-----END PRIVATE KEY-----".
 * 
 * <p>There are a variety of tools to generate RSA keys, the most common being
 * openssl and ssh-keygen. These tools allow for both the generation and 
 * manipulation of keys.
 * 
 * <p>For example, an ssh private key is converted to PEM TKCS#8 with command:
 * openssl pkcs8 -topk8 -nocrypt -in &lt;ssh priv key&gt;
 * 
 * <p>This class also provides a main method that can be invoked to generate
 * a set of RSA keys and save them in the MessageRSA.keys file in the
 * directory specified in the command line.
 * 
 * @author adriand
 * 
 */
public class RsaSignatureGenerator extends RsaSignatureVerifier {
    private static Logger log = Logger.getLogger(RsaSignatureGenerator.class);

    protected static RsaSignatureGenerator inst;
    protected PrivateKey privKey;
    public static final String PRIV_KEY_FILE_NAME = "RsaSignaturePriv.key";

    public static final String PRIV_KEY_START = "-----BEGIN PRIVATE KEY-----";
    public static final String PRIV_KEY_END = "-----END PRIVATE KEY-----";

    /**
     * Default constructor. This will look for a private key file named RsaSignaturePriv.key
     * and use it to sign.
     */
    @Deprecated
    public RsaSignatureGenerator() {
        this(PRIV_KEY_FILE_NAME);
    }
    
    @Deprecated
    public RsaSignatureGenerator(String keyFilename) {
        super(keyFilename, true);
        File keyFile = findFile(keyFilename); // again - ugh
        initPrivateKey(keyFile);
    }
    
    public RsaSignatureGenerator(File keyFile) {
        super(keyFile, true);
        initPrivateKey(keyFile);
    }

    public RsaSignatureGenerator(byte[] keyContent) {
        super(keyContent, true);
        initPrivateKey(keyContent);
    }
    
    private void initPrivateKey(File keysFile) {
        
        // try to load the keys
        try {
            try (BufferedReader br = new BufferedReader(new FileReader(keysFile))) {
                StringBuilder sb = null;
                boolean read = false;
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (line.equalsIgnoreCase(PRIV_KEY_START)) {
                        if (read) {
                            throw new
                                    IllegalArgumentException("Corrupted keys file");
                        }

                        if (privKey != null) {
                            throw new
                                    IllegalStateException("Found two private keys");
                        }

                        read = true;
                        sb = new StringBuilder();
                        continue;
                    }

                    if (line.equalsIgnoreCase(PRIV_KEY_END)) {
                        if (!read) {
                            throw new
                                    IllegalArgumentException("Corrupted keys file");
                        }

                        read = false;
                        String payload = sb.toString();
                        byte[] bytes = Base64.decode(payload);
                        initPrivateKey(bytes);
                    }

                    if (read) {
                        sb.append(line);
                    }
                }
            }
        } catch (IOException e) {
            String msg = "Could not read keys";
            throw new RuntimeException(msg, e);
        }

        if (privKey == null) {
            String msg = "No valid private key found";
            throw new IllegalStateException(msg);
        }
    }

    private void initPrivateKey(byte[] keysContent) {

        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e1) {
            throw new RuntimeException("BUG: Wrong algorithm " + KEY_ALGORITHM,
                    e1);
        }

        PKCS8EncodedKeySpec privateKeySpec = new
                PKCS8EncodedKeySpec(keysContent);
        try {
            privKey =
                    keyFactory.generatePrivate(privateKeySpec);
            // get corresponding public key
            RSAPrivateCrtKey privk =
                    (RSAPrivateCrtKey)privKey;
            RSAPublicKeySpec publicKeySpec =
                    new java.security.spec.RSAPublicKeySpec(
                            privk.getModulus(),
                            privk.getPublicExponent());

            PublicKey publicKey =
                    keyFactory.generatePublic(publicKeySpec);
            pubKeys.add(publicKey);
        } catch (InvalidKeySpecException e) {
            log.warn("Could not parse private key", e);
        }
    }

    /**
     * Method used to sign a message.
     * @param is input stream to be signed
     * @return signature
     * @throws IOException - IO problems
     * @throws InvalidKeyException - the provided key is invalid
     */
    public byte[] sign(InputStream is) throws IOException, InvalidKeyException {
        if (privKey == null) {
            throw new IllegalStateException(
                    "No private key available for signing");
        }
        
        try {
            Signature sig = getSignature();
            sig.initSign(privKey);
            byte[] data = new byte[1024];
            int ndRead = is.read(data);
            while (ndRead > 0) {
                sig.update(data, 0, ndRead);
                ndRead = is.read(data);
            }
            
            return sig.sign();
        } catch (SignatureException e) {
            throw new RuntimeException("Signature problem", e);
        }

    }

    
    public PrivateKey getPrivateKey() {
        return privKey;
    }
    
    private Signature getSignature() {
        try {
            return Signature.getInstance(SIG_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("BUG: Wrong signature algorithm "
                    + SIG_ALGORITHM, e);
        }
    }

    @Deprecated
    public static void genKeyPair(String directory) 
        throws FileNotFoundException { 
        genKeyPair(new File(directory));
    }
    
    public static void genKeyPair(File directory) 
        throws FileNotFoundException {      
        genKeyPair(new File(directory, PUB_KEY_FILE_NAME), new File(directory, PRIV_KEY_FILE_NAME), 1024);
    }
    
    public static void genKeyPair(File pubKey, File privKey, int keyLength) throws FileNotFoundException {
        // generate the certs
        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    "BUG: illegal key algorithm - " + KEY_ALGORITHM, e);
        }
        
        kpg.initialize(keyLength);
        KeyPair keyPair = kpg.genKeyPair();

        String base64PrivKey = 
                Base64.encodeLines(keyPair.getPrivate().getEncoded());
        String base64PubKey = 
                Base64.encodeLines(keyPair.getPublic().getEncoded());

        PrintWriter outPub = new PrintWriter(pubKey);
        try {
            outPub.println(PUB_KEY_START);
            outPub.print(base64PubKey);
            outPub.println(PUB_KEY_END);
        } finally {
            outPub.close();
        }    
        
        PrintWriter outPriv = new PrintWriter(privKey);
        try {
            outPriv.println(PRIV_KEY_START);
            outPriv.print(base64PrivKey);
            outPriv.println(PRIV_KEY_END);
        } finally {
            outPriv.close();
        }   
    }
}
