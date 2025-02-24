/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2025.                            (c) 2025.
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
************************************************************************
*/

package org.opencadc.persist;

import ca.nrc.cadc.auth.NumericPrincipal;
import ca.nrc.cadc.date.DateUtil;
import ca.nrc.cadc.util.HexUtil;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import org.apache.log4j.Logger;

/**
 * Base class for entity persistence. The base metaChecksum algorithm implemented here has a
 * flaw where moving a value from one optional field to another (with no values contributing
 * bytes in between) does not cause the computed metaChecksum to change. If a specific
 * data model is susceptible to this, it can use the "digestFieldNames" option to prevent
 * it, but changing options will change existing (stored) metaChecksum values so a change
 * like this has an operational impact that needs to be evaluated.
 * <p>
 * The safest and most portable use it to always use <code>digestFieldNames == true</code>
 * and to use <code>digestFieldNamesLowerCase == true</code> to make the calculation
 * independent of the standard/preferred naming in different programming languages. For example,
 * this implementation uses reflection and java code style is typically camel-case; using
 * lower case to digest field names is more portable as it  is easier to create an equivalent
 * python implementation where lower case is the norm.
 * </p>
 * <p>
 * Entity supports converting various standard data types to bytes: primitive numeric values, wrapped
 * numeric values (Numbers), arrays of bytes and numbers, UUID, URI, and String. Classes that wrap a
 * single internal value can implement the PrimitiveWrapper method to extract the wrapped value (any
 * of the previously mentioned types); enums should also have a getValue() method to return a consistent
 * value to be converted to bytes and digested.
 * </p>
 * 
 * @author pdowler
 */
public abstract class Entity {
    private static final Logger log = Logger.getLogger(Entity.class);

    private final String localPackage;
    public static boolean MCS_DEBUG = false;  // way to much debug when true
    
    private final boolean digestFieldNames;
    private final boolean digestFieldNamesLowerCase;
    private final boolean truncateDateToSec;
    private UUID id;
    private Date lastModified;
    private URI metaChecksum;
    
    /**
     * URI of the form {scheme}:{scheme-specific-part} to signify which process created this instance.
     * The scheme should be a short human-readable indicator of the institution/data-centre/provider and
     * the scheme-specific-part would normally be the name and version of a piece of software. Child entities
     * are assumed to be produced by the same process as their parent unless specifically set otherwise, so 
     * it is normally sufficient to set this for the observation only.
     */
    public URI metaProducer;

    
    static final void assertNotNull(Class caller, String name, Object test)
        throws IllegalArgumentException {
        if (test == null) {
            throw new IllegalArgumentException("invalid " + caller.getSimpleName() + "." + name + ": null");
        }
    }
    
    /**
     * Backwards compatible constructor: digestFieldNames==false.
     * 
     * @param truncateDateToSec truncate Date values to seconds when converting to bytes for meta checksum calculation
     * @deprecated hard code Entity(boolean, boolean, boolean) in model
     */
    @Deprecated
    protected Entity(boolean truncateDateToSec) {
        this(truncateDateToSec, false, false);
    }
    
    /**
     * Backwards compatible constructor: digestFieldNames==false.
     *
     * @param id assign the specified Entity.id
     * @param truncateDateToSec truncate Date values to seconds when converting to bytes for meta checksum calculation
     * @deprecated hard code Entity(UUID, boolean, boolean, boolean) in model
     */
    @Deprecated
    protected Entity(UUID id, boolean truncateDateToSec) {
        this(id, truncateDateToSec, false, false);
    }
    
    /**
     * Constructor.This creates a new entity with a random UUID.
     * 
     * @param truncateDateToSec truncate Date values to seconds when converting to bytes for meta checksum calculation
     * @param digestFieldNames when a field is not null (or collection is non-empty), include the field name in the
     *                         metaChecksum calculation
     * @param digestFieldNamesLowerCase convert field names to lower case before digesting
     */
    protected Entity(boolean truncateDateToSec, boolean digestFieldNames, boolean digestFieldNamesLowerCase) {
        this(UUID.randomUUID(), truncateDateToSec, digestFieldNames, digestFieldNamesLowerCase);
    }
    
    /**
     * Constructor.This creates an entity with an existing UUID when reconstructing an instance. The
 truncateDateToSec option should be used if instances of the model are to be serialized or stored
 in a way that does not recover the exact timestamp to milliseconds. The digestFieldNames option
 is needed for any model with "adjacent" fields that could contain the same value; this option
 ensures that "moving" the value from one field to another will change the checksum by changing
 the sequence of bytes that are digested.
     * 
     * @param id unique ID value to assign/restore
     * @param truncateDateToSec truncate Date values to seconds when converting to bytes for meta checksum calculation
     * @param digestFieldNames when a field is not null (or collection is non-empty), include the field name in the
     *                         metaChecksum calculation
     * @param digestFieldNamesLowerCase convert field names to lower case before digesting
     */
    protected Entity(UUID id, boolean truncateDateToSec, boolean digestFieldNames, boolean digestFieldNamesLowerCase) {
        Entity.assertNotNull(Entity.class, "id", id);
        this.id = id;
        this.truncateDateToSec = truncateDateToSec;
        this.digestFieldNames = digestFieldNames;
        this.digestFieldNamesLowerCase = digestFieldNamesLowerCase;
        this.localPackage = this.getClass().getPackage().getName();
    }

    public UUID getID() {
        return id;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public URI getMetaChecksum() {
        return metaChecksum;
    }
    
    /**
     * The base implementation compares numeric IDs.
     * 
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o instanceof Entity) {
            Entity a = (Entity) o;
            return this.id.equals(a.id);
        }
        return false;
    }

    /**
     * The base implementation uses the hash code of the numeric ID.
     * 
     * @return hashCode
     */
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        sb.append("[");
        sb.append(id).append(",").append(metaChecksum).append(",");
        if (lastModified != null) {
            DateFormat df = DateUtil.getDateFormat(DateUtil.IVOA_DATE_FORMAT, DateUtil.UTC);
            sb.append(df.format(lastModified));
        } else {
            sb.append(lastModified); // null
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Compute the current checksum of this entity.
     * 
     * @param digest checksum/digest implementation to use
     * @return checksum of the form {algorithm}:{hexadecimal value}
     */
    public URI computeMetaChecksum(MessageDigest digest) {
        try {
            MessageDigestWrapper mdw = new MessageDigestWrapper(digest);
            calcMetaChecksum(this.getClass(), this, mdw);
            if (MCS_DEBUG) {
                log.debug("computeMetaChecksum: " + mdw.getNumBytes() + " bytes");
            }
            byte[] metaChecksumBytes = digest.digest();
            String hexMetaChecksum = HexUtil.toHex(metaChecksumBytes);
            String alg = digest.getAlgorithm().toLowerCase();
            return new URI(alg, hexMetaChecksum, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to create metadata checksum URI for "
                + this.getClass().getName(), e);
        }
    }
    
    /**
     * Update the provided digest with state fields (values) from the specified object. 
     * This method does not finalize the digest so in principle more values can be 
     * accumulated by the digest the algorithm. USE WITH CARE!
     * 
     * @param c
     * @param o
     * @param digest 
     */
    protected final void calcMetaChecksum(Class c, Object o, MessageDigestWrapper digest) {
        // calculation order:
        // 1. Entity.id for entities
        // 2. Entity.metaProducer
        // 3. state fields in alphabetic order; depth-first recursion
        // value handling:
        // enum: find and call getValue() by reflection and continue
        // Date: normally milliseconds to long
        // optional Date handling: truncate time to whole number of seconds
        // String: UTF-8 encoded bytes
        // URI: UTF-8 encoded bytes of string representation
        // float: IEEE754 single (4 bytes)
        // double: IEEE754 double (8 bytes)
        // boolean: convert to single byte, false=0, true=1 (1 byte)
        // byte: as-is
        // short: (2 bytes, network byte order == big endian))
        // integer: (4 bytes, network byte order == big endian)
        // long: (8 bytes, network byte order == big endian)
        // UUID: 8 most-significant bytes + 8 least significant bytes (16 bytes)
        // optional for ALL fields: if non-zero bytes updated the digest, UTF-8 encoded bytes of the field name
        try {
            if (o instanceof Entity) {
                Entity ce = (Entity) o;
                digest.update(primitiveValueToBytes(ce.id, "Entity.id"));
                if (ce.metaProducer != null) {
                    digest.update(primitiveValueToBytes(ce.metaProducer, "Entity.metaProducer"));
                    if (digestFieldNames) {
                        digest.update(primitiveValueToBytes("Entity.metaProducer", "Entity.metaProducer"));
                    }
                }
            }

            SortedSet<Field> fields = getStateFields(c);
            for (Field f : fields) {
                String cf = f.getDeclaringClass().getSimpleName() + "." + f.getName();
                f.setAccessible(true);
                Object fo = f.get(o);
                if (fo != null) {
                    Class ac = fo.getClass();
                    if (ac.isEnum() || PrimitiveWrapper.class.isAssignableFrom(ac)) {
                        try {
                            log.warn("unwrap: " + ac.getSimpleName() + ".getValue()");
                            Method m = ac.getMethod("getValue");
                            Object val = m.invoke(fo);
                            digest.update(primitiveValueToBytes(val, cf));
                            if (digestFieldNames) {
                                digest.update(fieldNameToBytes(cf)); // field name
                            }
                        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                            throw new RuntimeException("BUG - enum " + ac.getName() + " does not have getValue()", ex);
                        }
                    } else if (isDataModelClass(ac)) {
                        // depth-first recursion
                        int num = digest.getNumBytes();
                        calcMetaChecksum(ac, fo, digest);
                        if (digestFieldNames && num < digest.getNumBytes()) {
                            digest.update(fieldNameToBytes(cf)); // field name
                        }
                    } else if (fo instanceof Collection) {
                        Collection stuff = (Collection) fo;
                        if (!stuff.isEmpty()) {
                            Iterator i = stuff.iterator();
                            while (i.hasNext()) {
                                Object co = i.next();
                                Class cc = co.getClass();
                                if (cc.isEnum() || PrimitiveWrapper.class.isAssignableFrom(cc)) {
                                    try {
                                        Method m = cc.getMethod("getValue");
                                        Object val = m.invoke(co);
                                        digest.update(primitiveValueToBytes(val, cf));
                                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                                        throw new RuntimeException("BUG", ex);
                                    }
                                } else if (isDataModelClass(cc)) {
                                    // depth-first recursion
                                    calcMetaChecksum(cc, co, digest);
                                } else {
                                    digest.update(primitiveValueToBytes(co, cf));
                                }
                            }
                            if (digestFieldNames) {
                                digest.update(fieldNameToBytes(cf)); // field name
                            }
                        }
                    } else {
                        digest.update(primitiveValueToBytes(fo, cf));
                        if (digestFieldNames) {
                            digest.update(fieldNameToBytes(cf)); // field name
                        }
                    }
                } else if (MCS_DEBUG) {
                    log.debug("skip null: " + cf);
                }
            }

        } catch (IllegalAccessException bug) {
            throw new RuntimeException("Unable to calculate metaChecksum for class " + c.getName(), bug);
        }
    }
    
    public static class MessageDigestWrapper {
        private MessageDigest digest;
        private int numBytes = 0;

        public MessageDigestWrapper(MessageDigest digest) {
            this.digest = digest;
        }
        
        public void update(byte[] b) {
            digest.update(b);
            numBytes += b.length;
        }

        public int getNumBytes() {
            return numBytes;
        }
    }
    
    /**
     * Determine if the argument type is part of a data model implementation
     * so reflection can be used to drill down into the structure. The standard
     * implementation checks that the class is in the same package or a subpackage
     * of the entity implementation. Subclasses can override this method and return
     * true for classes that are outside their package (imported data model classes)
     * and must otherwise call this method to return the standard value.
     * 
     * @param c
     * @return true if the class is a data model class, otherwise false
     */
    protected boolean isDataModelClass(Class c) {
        if (c.isPrimitive() || c.isArray()) {
            return false;
        }
        String pname = c.getPackage().getName();
        return pname.startsWith(localPackage);
    }
    
    public static SortedSet<Field> getStateFields(Class c)
            throws IllegalAccessException {
        SortedSet<Field> ret = new TreeSet<>(new FieldComparator());
        Field[] fields = c.getDeclaredFields();
        for (Field f : fields) {
            int m = f.getModifiers();
            boolean inc = true;
            // reasons to skip:
            inc = inc && !Modifier.isTransient(m);
            inc = inc && !Modifier.isStatic(m);
            inc = inc && !isChildCollection(f); // 0..* relations to other Entity
            inc = inc && !isChildEntity(f); // 0..1 relation to other Entity
            if (inc) {
                ret.add(f);
            }
        }
        Class sc = c.getSuperclass();
        while (sc != null && !Entity.class.equals(sc)) {
            ret.addAll(getStateFields(sc));
            sc = sc.getSuperclass();
        }
        return ret;
    }
    
    public static SortedSet<Field> getChildFields(Class c)
            throws IllegalAccessException {
        SortedSet<Field> ret = new TreeSet<>(new FieldComparator());
        Field[] fields = c.getDeclaredFields();
        for (Field f : fields) {
            int m = f.getModifiers();
            if (!Modifier.isTransient(m) && !Modifier.isStatic(m)
                    && (isChildCollection(f) || isChildEntity(f))) {
                ret.add(f);
            }
        }
        Class sc = c.getSuperclass();
        while (sc != null && !Entity.class.equals(sc)) {
            ret.addAll(getChildFields(sc));
            sc = sc.getSuperclass();
        }
        return ret;
    }
    
    // child is an Entity
    public static boolean isChildEntity(Field f) throws IllegalAccessException {
        return (Entity.class.isAssignableFrom(f.getType()));
    }

    // child collection is a non-empty Set<Entity>
    public static boolean isChildCollection(Field f) throws IllegalAccessException {
        if (Collection.class.isAssignableFrom(f.getType())) {
            if (f.getGenericType() instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) f.getGenericType();
                Type[] ptypes = pt.getActualTypeArguments();
                Class genType = (Class) ptypes[0];
                if (Entity.class.isAssignableFrom(genType)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    protected byte[] primitiveValueToBytes(Object o, String name) {
        byte[] ret = null;
        if (o instanceof Byte) {
            ret = HexUtil.toBytes((Byte) o); // auto-unbox
        } else if (o instanceof Short) {
            ret = HexUtil.toBytes((Short) o); // auto-unbox
        } else if (o instanceof Integer) {
            ret = HexUtil.toBytes((Integer) o); // auto-unbox
        } else if (o instanceof Long) {
            ret = HexUtil.toBytes((Long) o); // auto-unbox
        } else if (o instanceof Boolean) {
            Boolean b = (Boolean) o;
            if (b) {
                ret = HexUtil.toBytes((byte) 1);
            } else {
                ret = HexUtil.toBytes((byte) 0);
            }
        } else if (o instanceof Date) {
            Date date = (Date) o;
            long sec = date.getTime(); // milliseconds
            if (truncateDateToSec) {
                // use case: CAOM did this because some DBs cannot round trip milliseconds
                sec = (date.getTime() / 1000L); 
            }
            ret = HexUtil.toBytes(sec);
        } else if (o instanceof Float) {
            ret = HexUtil.toBytes(Float.floatToIntBits((Float) o)); /* auto-unbox, IEEE754 float */
        } else if (o instanceof Double) {
            ret = HexUtil.toBytes(Double.doubleToLongBits((Double) o)); /* auto-unbox, IEEE754 double */
        } else if (o instanceof String) {
            try {
                ret = ((String) o).trim().getBytes("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException("BUG: failed to encode String in UTF-8", ex);
            }
        } else if (o instanceof URI) {
            try {
                ret = ((URI) o).toASCIIString().trim().getBytes("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException("BUG: failed to encode String in UTF-8", ex);
            }
        } else if (o instanceof UUID) {
            UUID uuid = (UUID) o;
            byte[] msb = HexUtil.toBytes(uuid.getMostSignificantBits());
            byte[] lsb = HexUtil.toBytes(uuid.getLeastSignificantBits());
            ret = new byte[16];
            System.arraycopy(msb, 0, ret, 0, 8);
            System.arraycopy(lsb, 0, ret, 8, 8);
        } else if (o instanceof NumericPrincipal) {
            // TODO: thisd could have been accomplished with PrimitiveWrapper -> UUID
            NumericPrincipal np = (NumericPrincipal) o;
            UUID uuid = np.getUUID();
            byte[] msb = HexUtil.toBytes(uuid.getMostSignificantBits());
            byte[] lsb = HexUtil.toBytes(uuid.getLeastSignificantBits());
            ret = new byte[16];
            System.arraycopy(msb, 0, ret, 0, 8);
            System.arraycopy(lsb, 0, ret, 8, 8);
        } else if (o instanceof byte[]) {
            ret = (byte[]) o;
        } else if (o instanceof double[]) {
            double[] da = (double[]) o;
            ret = new byte[8 * da.length];
            for (int i = 0; i < da.length; i++) {
                byte[] b = HexUtil.toBytes(Double.doubleToLongBits(da[i])); // IEEE754 double
                System.arraycopy(b, 0, ret, i * 8, 8);
            }
        }

        if (ret != null) {
            if (MCS_DEBUG) {
                String dfn = "";
                if (o == name) {
                    dfn = " digest-field-name";
                }
                log.debug(o.getClass().getSimpleName() + " " + name + " = " + o.toString() + " " + ret.length + " bytes" + dfn);
            }
            return ret;
        }

        throw new UnsupportedOperationException("unexpected primitive/value type: " + o.getClass().getName());
    }

    protected byte[] fieldNameToBytes(String name) {
        String val = name.trim();
        if (digestFieldNamesLowerCase) {
            val = val.toLowerCase();
        }
        byte[] ret = null;
        try {
            ret = val.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("BUG: failed to encode String in UTF-8", ex);
        }
        if (ret != null) {
            if (MCS_DEBUG) {
                String dfn = " digest-field-name";
                log.debug(val.getClass().getSimpleName() + " " + name + " = " + val + " " + ret.length + " bytes" + dfn);
            }
            return ret;
        }

        throw new RuntimeException("BUG: null field name");
    }

    private static class FieldComparator implements Comparator<Field> {
        @Override
        public int compare(Field o1, Field o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }
}
