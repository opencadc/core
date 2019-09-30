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

/************************************************************************
 *   
 *   This class has been extracted from the PrivtKeyReader class at:
 *   http://www.androidadb.com/source/oauth-read-only/java/jmeter/jmeter/
 *   src/main/java/org/apache/jmeter/protocol/oauth/
 *   sampler/PrivateKeyReader.java.html
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *  
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 ************************************************************************/

package ca.nrc.cadc.auth;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

/**
 * A bare-minimum ASN.1 DER decoder, just having enough functions to decode
 * PKCS#1 private keys. Especially, it doesn't handle explicitly tagged types
 * with an outer tag.
 * 
 * <p/>
 * This parser can only handle one layer. To parse nested constructs, get a new
 * parser for each layer using <code>Asn1Object.getParser()</code>.
 * 
 * <p/>
 * There are many DER decoders in JRE but using them will tie this program to a
 * specific JCE/JVM.
 * 
 * @author zhang
 *
 */
class DerParser {

    protected InputStream in;

    /**
     * Create a new DER decoder from an input stream.
     * 
     * @param in The DER encoded stream
     */
    public DerParser(InputStream in) throws IOException {
        this.in = in;
    }

    /**
     * Create a new DER decoder from a byte array.
     * 
     * @param The encoded bytes
     * @throws IOException
     */
    public DerParser(byte[] bytes) throws IOException {
        this(new ByteArrayInputStream(bytes));
    }

    /**
     * Read next object. If it's constructed, the value holds encoded content and it
     * should be parsed by a new parser from <code>Asn1Object.getParser</code>.
     * 
     * @return A object
     * @throws IOException
     */
    public Asn1Object read() throws IOException {
        int tag = in.read();

        if (tag == -1) {
            throw new IOException("Invalid DER: stream too short, missing tag"); //$NON-NLS-1$
        }

        int length = getLength();

        byte[] value = new byte[length];
        int n = in.read(value);
        if (n < length) {
            throw new IOException("Invalid DER: stream too short, missing value"); //$NON-NLS-1$
        }

        Asn1Object o = new Asn1Object(tag, length, value);

        return o;
    }

    /**
     * Decode the length of the field. Can only support length encoding up to 4
     * octets.
     * 
     * <p/>
     * In BER/DER encoding, length can be encoded in 2 forms,
     * <ul>
     * <li>Short form. One octet. Bit 8 has value "0" and bits 7-1 give the length.
     * <li>Long form. Two to 127 octets (only 4 is supported here). Bit 8 of first
     * octet has value "1" and bits 7-1 give the number of additional length octets.
     * Second and following octets give the length, base 256, most significant digit
     * first.
     * </ul>
     * 
     * @return The length as integer
     * @throws IOException
     */
    private int getLength() throws IOException {

        int i = in.read();
        if (i == -1) {
            throw new IOException("Invalid DER: length missing"); //$NON-NLS-1$
        }

        // A single byte short length
        if ((i & ~0x7F) == 0) {
            return i;
        }

        int num = i & 0x7F;

        // We can't handle length longer than 4 bytes
        if (i >= 0xFF || num > 4) {
            throw new IOException("Invalid DER: length field too big (" //$NON-NLS-1$
                    + i + ")"); //$NON-NLS-1$
        }

        byte[] bytes = new byte[num];
        int n = in.read(bytes);
        if (n < num) {
            throw new IOException("Invalid DER: length too short"); //$NON-NLS-1$
        }

        return new BigInteger(1, bytes).intValue();
    }

}
