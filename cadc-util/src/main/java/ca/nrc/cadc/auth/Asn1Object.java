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

import java.io.IOException;
import java.math.BigInteger;

/**
 * An ASN.1 TLV. The object is not parsed. It can only handle integers and
 * strings.
 * 
 * @author zhang
 *
 */
class Asn1Object {

    // Classes
    public static final int UNIVERSAL = 0x00;
    public static final int APPLICATION = 0x40;
    public static final int CONTEXT = 0x80;
    public static final int PRIVATE = 0xC0;

    // Constructed Flag
    public static final int CONSTRUCTED = 0x20;

    // Tag and data types
    public static final int ANY = 0x00;
    public static final int BOOLEAN = 0x01;
    public static final int INTEGER = 0x02;
    public static final int BIT_STRING = 0x03;
    public static final int OCTET_STRING = 0x04;
    public static final int NULL = 0x05;
    public static final int OBJECT_IDENTIFIER = 0x06;
    public static final int REAL = 0x09;
    public static final int ENUMERATED = 0x0a;
    public static final int RELATIVE_OID = 0x0d;

    public static final int SEQUENCE = 0x10;
    public static final int SET = 0x11;

    public static final int NUMERIC_STRING = 0x12;
    public static final int PRINTABLE_STRING = 0x13;
    public static final int T61_STRING = 0x14;
    public static final int VIDEOTEX_STRING = 0x15;
    public static final int IA5_STRING = 0x16;
    public static final int GRAPHIC_STRING = 0x19;
    public static final int ISO646_STRING = 0x1A;
    public static final int GENERAL_STRING = 0x1B;

    public static final int UTF8_STRING = 0x0C;
    public static final int UNIVERSAL_STRING = 0x1C;
    public static final int BMP_STRING = 0x1E;

    public static final int UTC_TIME = 0x17;
    public static final int GENERALIZED_TIME = 0x18;

    protected final int type;
    protected final int length;
    protected final byte[] value;
    protected final int tag;

    /**
     * Construct a ASN.1 TLV. The TLV could be either a constructed or primitive
     * entity.
     * 
     * <p/>
     * The first byte in DER encoding is made of following fields,
     * 
     * <pre>
     *-------------------------------------------------
     *|Bit 8|Bit 7|Bit 6|Bit 5|Bit 4|Bit 3|Bit 2|Bit 1|
     *-------------------------------------------------
     *|  Class    | CF  |     +      Type             |
     *-------------------------------------------------
     * </pre>
     * <ul>
     * <li>Class: Universal, Application, Context or Private
     * <li>CF: Constructed flag. If 1, the field is constructed.
     * <li>Type: This is actually called tag in ASN.1. It indicates data type
     * (Integer, String) or a construct (sequence, choice, set).
     * </ul>
     * 
     * @param tag    Tag or Identifier
     * @param length Length of the field
     * @param value  Encoded octet string for the field.
     */
    public Asn1Object(int tag, int length, byte[] value) {
        this.tag = tag;
        this.type = tag & 0x1F;
        this.length = length;
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public int getLength() {
        return length;
    }

    public byte[] getValue() {
        return value;
    }

    public boolean isConstructed() {
        return (tag & Asn1Object.CONSTRUCTED) == Asn1Object.CONSTRUCTED;
    }

    /**
     * For constructed field, return a parser for its content.
     * 
     * @return A parser for the construct.
     * @throws IOException
     */
    public DerParser getParser() throws IOException {
        if (!isConstructed()) {
            throw new IOException("Invalid DER: can't parse primitive entity"); //$NON-NLS-1$
        }

        return new DerParser(value);
    }

    /**
     * Get the value as integer
     * 
     * @return BigInteger
     * @throws IOException
     */
    public BigInteger getInteger() throws IOException {
        if (type != Asn1Object.INTEGER) {
            throw new IOException("Invalid DER: object is not integer"); //$NON-NLS-1$
        }

        return new BigInteger(value);
    }

    /**
     * Get value as string. Most strings are treated as Latin-1.
     * 
     * @return Java string
     * @throws IOException
     */
    public String getString() throws IOException {

        String encoding;

        switch (type) {

            // Not all are Latin-1 but it's the closest thing
            case Asn1Object.NUMERIC_STRING:
            case Asn1Object.PRINTABLE_STRING:
            case Asn1Object.VIDEOTEX_STRING:
            case Asn1Object.IA5_STRING:
            case Asn1Object.GRAPHIC_STRING:
            case Asn1Object.ISO646_STRING:
            case Asn1Object.GENERAL_STRING:
                encoding = "ISO-8859-1"; //$NON-NLS-1$
                break;

            case Asn1Object.BMP_STRING:
                encoding = "UTF-16BE"; //$NON-NLS-1$
                break;

            case Asn1Object.UTF8_STRING:
                encoding = "UTF-8"; //$NON-NLS-1$
                break;

            case Asn1Object.UNIVERSAL_STRING:
                throw new IOException("Invalid DER: can't handle UCS-4 string"); //$NON-NLS-1$

            default:
                throw new IOException("Invalid DER: object is not a string"); //$NON-NLS-1$
        }

        return new String(value, encoding);
    }
}
