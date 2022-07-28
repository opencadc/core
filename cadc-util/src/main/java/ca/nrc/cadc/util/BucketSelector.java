/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2022.                            (c) 2022.
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;
import org.apache.log4j.Logger;

/**
 * Support hex bucket and bucket range usage to parallelize work loads.
 * 
 * @author pdowler
 */
public class BucketSelector {
    private static final Logger log = Logger.getLogger(BucketSelector.class);

    public static final String BUCKET_CHARS = "0123456789abcdef";
    
    private static final int MAX_PREFIX_LENGTH = 1; // for ranges
    private final String rangeMin;
    private final String rangeMax;
    private final TreeSet<String> bucketList = new TreeSet<>();

    public Iterator<String> getBucketIterator() {
        return bucketList.iterator();
    }

    public BucketSelector(String selector) {
        if (selector == null) {
            throw new IllegalArgumentException("BucketSelector: selector cannot be null");
        }
        
        String[] minMax = selector.split("-");
        if (minMax.length > 2) {
            throw new IllegalArgumentException("invalid prefix range: single value or range only: "
                + selector);
        }

        // trim and convert to lower case for consistent processing
        rangeMin = minMax[0].trim().toLowerCase();

        if (minMax.length == 1) {
            rangeMax = rangeMin;
        } else {
            rangeMax = minMax[1].trim().toLowerCase();
        }

        // Prefix size is currently 1. This check will adapt if MAX_PREFIX_LENGTH is changed in future.
        if ((1 <= rangeMin.length() && rangeMin.length() <= MAX_PREFIX_LENGTH)
            && (1 <= rangeMax.length() && rangeMax.length() <= MAX_PREFIX_LENGTH)) {
            log.debug("acceptable length: " + rangeMin + " - " + rangeMax);
        } else {
            throw new IllegalArgumentException("invalid bucket prefix (" + rangeMin + " or " + rangeMax
                + "): max length is " + MAX_PREFIX_LENGTH);
        }

        // Note: Lookup of the rangeMin & rangeMax values below in Artifact.URI_BUCKET_CHARS
        // is an acceptable shortcut while MAX_PREFIX_LENGTH == 1.
        // If MAX_PREFIX_LENGTH is set to > 1, each character in rangeMin & rangeMax would
        // need to be evaluated against values in Artifact.URI_BUCKET_CHARS
        int min;
        int max;
        min = BUCKET_CHARS.indexOf(rangeMin);
        if (min == -1) {
            throw new IllegalArgumentException("invalid bucket prefix: " + rangeMin);
        }
        max = BUCKET_CHARS.indexOf(rangeMax);
        if (max == -1) {
            throw new IllegalArgumentException("invalid bucket prefix: " + rangeMax);
        }

        // order of range must be sane
        if (max < min) {
            throw new IllegalArgumentException("invalid prefix range (min - max): " + rangeMin + " - " + rangeMax);
        }

        log.debug("range values as ints: " + min + ", " + max);

        // Populate bucketList
        // Note: The index of the character position in Artifact.URI_BUCKET_CHARS can be used
        // as a sane value to generate bucketList from only in the case that MAX_PREFIX_LENGTH == 1.
        // Otherwise, a different algorithm needs to be developed here to give sane values
        // that the bucketList iterator will return.
        for (int i = min; i <= max; i++) {
            bucketList.add(Character.toString(BUCKET_CHARS.charAt(i)));
            log.debug("added " + BUCKET_CHARS.charAt(i));
        }
    }
    
    public String getMinBucket() {
        return rangeMin;
    }
    
    public String getMinBucket(int padLength) {
        char[] ca = new char[padLength];
        Arrays.fill(ca, '0');
        int i = 0;
        for (char c : rangeMin.toCharArray()) {
            ca[i++] = c;
        }
        return String.copyValueOf(ca);
    }

    public String getMaxBucket() {
        return rangeMax;
    }
    
    public String getMaxBucket(int padLength) {
        char[] ca = new char[padLength];
        Arrays.fill(ca, 'f');
        int i = 0;
        for (char c : rangeMax.toCharArray()) {
            ca[i++] = c;
        }
        return String.copyValueOf(ca);
    }
      

    @Override
    public String toString() {
        return "BucketSelector[" + rangeMin + "," + rangeMax + "]";
    }
}
