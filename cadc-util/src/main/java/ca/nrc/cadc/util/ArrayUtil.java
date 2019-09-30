/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2016.                            (c) 2016.
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

import java.util.Arrays;


/**
 * @version $Version$
 * @author pdowler
 */
public class ArrayUtil {
    /**
     * Find a string in an array of specified items.
     *
     * @param s         The value to find.
     * @param as        The array.
     *
     * @return          The location of s within the array, or -1 if not found.
     */
    public static <O> int find(final O s, final O[] as) {
        if (isEmpty(as)) {
            return -1;
        } else {
            for (int i = 0; i < as.length; i++) {
                if (s.equals(as[i])) {
                    return i;
                }
            }

            return -1;
        }
    }

    /**
     * Match a String in an array of Strings.
     *
     * @param regexp            The regexp string to match.
     * @param as                The array of Strings to match against.
     * @param caseInsensitive   Whether or not to perform a case insensitive
     *                          match.
     * @return          Location of regexp within the array, or -1 if not
     *                  found.
     */
    public static int matches(final String regexp, final String[] as,
                              final boolean caseInsensitive) {
        if (StringUtil.hasLength(regexp) && !isEmpty(as)) {
            for (int i = 0; i < as.length; i++) {
                if (StringUtil.hasLength(as[i])
                    && StringUtil.matches(as[i], regexp, caseInsensitive)) {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Obtain whether the given array contains the element.
     *
     * @param element       The element to check.
     * @param array         The array to search.
     * @param <O>           The type of the array/element.
     * @return              True if it is contained, false otherwise.
     */
    public static <O> boolean contains(final O element, final O[] array) {
        return !(isEmpty(array) || (element == null))
               && Arrays.asList(array).contains(element);
    }

    /**
     * Obtain whether the given array is empty.
     * 
     * @param array     The array to check.
     * @return          True if it is empty, False otherwise.
     */
    public static <O> boolean isEmpty(final O[] array) {
        return (array == null) || (array.length == 0);
    }

    /**
     * Obtain whether the given char array is empty.
     *
     * @param array     The array to check.
     * @return          True if it is empty, False otherwise.
     */
    public static boolean isEmpty(final char[] array) {
        return (array == null) || (array.length == 0);
    }

    /**
     * Obtain whether the given int array is empty.
     *
     * @param array     The array to check.
     * @return          True if it is empty, False otherwise.
     */
    public static boolean isEmpty(final int[] array) {
        return (array == null) || (array.length == 0);
    }

    /**
     * Obtain whether the given byte array is empty.
     *
     * @param array     The array to check.
     * @return          True if it is empty, False otherwise.
     */
    public static boolean isEmpty(final byte[] array) {
        return (array == null) || (array.length == 0);
    }
}
