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
************************************************************************
 */

package ca.nrc.cadc.util;

import java.util.Comparator;
import java.util.UUID;

/**
 * RFC 4122 compliant UUID comparator.
 *
 * @author pdowler
 */
public class UUIDComparator implements Comparator<UUID> {

    public UUIDComparator() {
    }

    @Override
    public int compare(UUID u1, UUID u2) {
        return staticCompare(u1, u2);
    }

    // implementation: https://github.com/cowtowncoder/java-uuid-generator/
    // The implementation of was copied from https://github.com/cowtowncoder/java-uuid-generator/
    // which is licensed under the GPL-compatible Apache License 2.0
    // some reformatting to comply to OpenCADC code style and embed the UUIDType
    // enum -- implementation is provate to this is not intended as a fork and we
    // may change to using the original code as a library in future.
    
    private static int staticCompare(UUID u1, UUID u2) {
        // First: major sorting by types
        int type = u1.version();
        int diff = type - u2.version();
        if (diff != 0) {
            return diff;
        }
        // Second: for time-based variant, order by time stamp:
        if (type == UUIDType.TIME_BASED.raw()) {
            diff = compareULongs(u1.timestamp(), u2.timestamp());
            if (diff == 0) {
                // or if that won't work, by other bits lexically
                diff = compareULongs(u1.getLeastSignificantBits(), u2.getLeastSignificantBits());
            }
        } else {
            // note: java.util.UUIDs compares with sign extension, IMO that's wrong, so:
            diff = compareULongs(u1.getMostSignificantBits(),
                    u2.getMostSignificantBits());
            if (diff == 0) {
                diff = compareULongs(u1.getLeastSignificantBits(),
                        u2.getLeastSignificantBits());
            }
        }
        return diff;
    }

    protected static final int compareULongs(long l1, long l2) {
        int diff = compareUInts((int) (l1 >> 32), (int) (l2 >> 32));
        if (diff == 0) {
            diff = compareUInts((int) l1, (int) l2);
        }
        return diff;
    }

    protected static final int compareUInts(int i1, int i2) {
        /* bit messier due to java's insistence on signed values: if both
         * have same sign, normal comparison (by subtraction) works fine;
         * but if signs don't agree need to resolve differently
         */
        if (i1 < 0) {
            return (i2 < 0) ? (i1 - i2) : 1;
        }
        return (i2 < 0) ? -1 : (i1 - i2);
    }

    private enum UUIDType {
        TIME_BASED(1),
        DCE(2),
        NAME_BASED_MD5(3),
        RANDOM_BASED(4),
        NAME_BASED_SHA1(5),
        UNKNOWN(0);

        private final int _raw;

        private UUIDType(int raw) {
            _raw = raw;
        }

        /**
         * Returns "raw" type constants, embedded within UUID bytes.
         */
        public int raw() {
            return _raw;
        }
    }
}
