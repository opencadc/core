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

import java.text.NumberFormat;
import java.util.StringTokenizer;

/**
 * Simple astronomical coordinate conversion utility.
 *
 * @version $Version$
 * @author pdowler
 */
public class CoordUtil {
    // the unicode degree symbol
    public static String DEGREE_SYMBOL = new Character((char) 0x00b0).toString();

    private static String raSeparators = "'\"hmsHMS:";
    private static String decSeparators = "'\"dmsDMS:" + DEGREE_SYMBOL;

    private static NumberFormat raFormat = NumberFormat.getInstance();
    private static NumberFormat decFormat = NumberFormat.getInstance();
    
    static {
        raFormat.setMaximumFractionDigits(1);
        raFormat.setMinimumFractionDigits(1);
        raFormat.setMaximumIntegerDigits(2);
        raFormat.setMinimumIntegerDigits(2);

        decFormat.setMaximumFractionDigits(1);
        decFormat.setMinimumFractionDigits(1);
        decFormat.setMaximumIntegerDigits(2);
        decFormat.setMinimumIntegerDigits(2);
    }

    /*
     * Convert the ra,dec values in degrees to sexigessimal format. This is a
     * convenience method that calls degreesToRA() and degreesToDEC().
     * 
     * @return String[2] with ra and dec
     */
    public static String[] degreesToSexigessimal(double ra, double dec) {
        return new String[] {degreesToRA(ra), degreesToDEC(dec)};
    }

    public static String degreesToRA(double val) {
        // raneg reduction to [0.0,360.0)
        while (val < 0.0) {
            val += 360.0;
        }
        
        while (val >= 360.0) {
            val -= 360.0;
        }
        
        //if (val < 0.0 || val >= 360.0)
        // throw new IllegalArgumentException("value "+val+" out of bounds: [0.0, 360.0)");
        
        // 24 hours/360 degrees = 15 deg/hour
        int h = (int) (val / 15.0);
        val -= h * 15.0;
        // 15 deg/hour == 0.25 deg/min == 4 min/deg
        int m = (int) (val * 4.0);
        val -= m / 4.0;
        // 4 min/deg == 240 sec/deg
        val *= 240.0;
        String d = Double.toString(val);
        String s = null;
        String hh = Integer.toString(h);
        String mm = Integer.toString(m);
        if (h < 10) {
            hh = "0" + h;
        }

        if (m < 10) {
            mm = "0" + m;
        }

        s = hh + ":" + mm + ":";
        return s + raFormat.format(val);
    }
    
    public static String degreesToDEC(double val) {
        if (val < -90.0 || val > 90.0) {
            throw new IllegalArgumentException("value " + val + " out of bounds: [-90.0, 90.0]");
        }

        String sign = "+";
        if (val < 0.0) {
            sign = "-";
            val *= -1.0;
        }

        int deg = (int) (val);
        val -= deg;
        // 60 min/deg
        int m = (int) (val * 60.0);
        val -= m / 60.0;
        // 60 sec/min == 3600 sec/deg
        val *= 3600.0;
        String d = Double.toString(val);

        String degs = Integer.toString(deg);
        if (deg < 10) {
            degs = "0" + degs;
        }
        
        String min = Integer.toString(m);
        if (m < 10) {
            min = "0" + m;
        }

        String s = sign + degs + ":" + min + ":";

        return s + decFormat.format(val);
    }

    public static double[] sexigessimalToDegrees(String ra, String dec)
        throws NumberFormatException {
        return new double[] {raToDegrees(ra), decToDegrees(dec)};
    }

    /**
     * Convert a string to a right ascension value in degrees. The argument is split
     * into components using a variety of separators (space, colon, some chars). 
     * Valid formats include 15h30m45.6s = 15:30:45.6 = 15 30 45.6 ~ 232.69 (degrees).
     * If there is only one component after splitting, it is assumed to be the degrees
     * component (ie. 15 != 15:0:0) unless followed by the character 'h' (ie. 15h = 15:0:0).
     *
     * <p>TODO - This is obscure and can be simplified!
     * TODO - 2007.01.05
     * 
     * @param ra
     * @return right ascension in degrees
     * @throws NumberFormatException if arg cannot be parsed
     * @throws IllegalArgumentException if the resulting value is not in [0,360]
     */
    public static double raToDegrees(final String ra)
        throws NumberFormatException {
        StringTokenizer st = new StringTokenizer(ra, raSeparators, true);
        double h = Double.NaN;
        double m = 0.0;
        double s = 0.0;

        if (st.hasMoreTokens()) {
            h = Double.parseDouble(st.nextToken());
        }

        if (st.hasMoreTokens()) {
            String str = st.nextToken(); // consume separator
            if (str.equals("h") || str.equals(":")) {
                h *= 15.0;
            }
        }

        if (st.hasMoreTokens()) {
            m = Double.parseDouble(st.nextToken());
        }

        if (st.hasMoreTokens()) {
            st.nextToken(); // consume separator
        }

        if (st.hasMoreTokens()) {
            s = Double.parseDouble(st.nextToken());
        }

        if (Double.isNaN(h)) {
            throw new IllegalArgumentException("empty string (RA)");
        }

        double ret = h + m / 4.0 + s / 240.0;
        while (ret < 0.0) {
            ret += 360.0;
        }
        
        while (ret > 360.0) {
            ret -= 360.0;
        }
        //if (0.0 <= ret && ret < 360.0)
        return ret;
        //throw new IllegalArgumentException("RA must be in [0,360]: " + ret);
    }

    /**
     * Obtain the radian value of the given RA string.
     * @param ra
     * @return double radian
     * @throws NumberFormatException
     */
    public static double raToRadians(final String ra)
            throws NumberFormatException {
        return (raToDegrees(ra) * Math.PI) / 180.0;
    }

    /**
     * Obtain the String RA of the given Radians.
     * @param raRadians
     * @return String HH mm ss.s
     */
    public static String radiansToRA(final double raRadians) {
        return degreesToRA((raRadians * 180) / Math.PI);
    }

    /**
     * Convert a string to a declination value in degrees. The argument is split
     * into components using a variety of separators (space, colon, some chars). 
     * Valid formats include 15d30m45.6s = 15:30:45.6 = 15 30 45.6 ~ 15.51267 (degrees).
     * If there is only one component after splitting, it is assumed to be the degrees
     * component (thus, 15 == 15:0:0). Only the degrees component should have a negative
     * sign.
     * 
     * @param dec
     * @return declination in degrees
     * @throws NumberFormatException if arg cannot be parsed
     * @throws IllegalArgumentException if the resulting value is not in [-90,90]
     */
    public static double decToDegrees(String dec)
        throws IllegalArgumentException, NumberFormatException {
        StringTokenizer st = new StringTokenizer(dec, decSeparators);
        double d = Double.NaN;
        double m = 0;
        double s = 0;
        if (st.hasMoreTokens()) {
            d = Double.parseDouble(st.nextToken());
        }

        if (st.hasMoreTokens()) {
            m = Double.parseDouble(st.nextToken());
        }

        if (st.hasMoreTokens()) {
            s = Double.parseDouble(st.nextToken());
        }

        if (Double.isNaN(d)) {
            throw new IllegalArgumentException("empty string (DEC)");
        }
        
        double ret = d + m / 60.0 + s / 3600.0;
        if (dec.startsWith("-")) {
            ret = d - m / 60.0 - s / 3600.0;
        }

        if (-90.0 <= ret && ret <= 90.0) {
            return ret;
        }
        
        throw new IllegalArgumentException("DEC must be in [-90,90]: " + ret);
    }

    /**
     * Obtain the radian value of the String declination.
     *
     * @param dec
     * @return double as radians.
     * @throws IllegalArgumentException
     * @throws NumberFormatException
     */
    public static double decToRadians(final String dec)
            throws IllegalArgumentException, NumberFormatException {
        return (decToDegrees(dec) * Math.PI) / 180.0;
    }

    /**
     * Obtain the String declination of the given radians.
     *
     * @param decRadians
     * @return String declination DD mm ss.s
     */
    public static String radiansToDec(final double decRadians) {
        return degreesToDEC((decRadians * 180) / Math.PI);
    }
    
    /*
    public static void main(String[] args)
    {
        double[] ra = { 0.0, 10.0, 350.0, 360.0, 370.0, -10.0, -350.0, -370.0 };
        double[] dec = {-90.0, -85, -0.1, 0.0, 0.1, 85.0, 90.0 };
        double[] edec = { -100.0, 100.0 };
        
        for (int i=0; i<ra.length; i++)
        {
            String s = degreesToRA(ra[i]);
            System.out.println(ra[i] + " -> " + s);
            double d = raToDegrees(s);
            System.out.println(s + " -> " + d);
            assert d == ra[i];
        }
        for (int i=0; i<dec.length; i++)
        {
            String s = degreesToDEC(dec[i]);
            System.out.println(dec[i] + " -> " + s);
            double d = decToDegrees(s);
            System.out.println(s + " -> " + d);
            assert d == dec[i];
        }
        for (int i=0; i<edec.length; i++)
        {
            try
            {
                String s = degreesToDEC(edec[i]);
                System.out.println(dec[i] + " -> " + s + " [FAIL]");
                double d = decToDegrees(s);
                System.out.println(s + " -> " + d + " [FAIL]");
                assert d == dec[i];
            }
            catch(IllegalArgumentException ok) { System.out.println("caught expected: " + ok); }
        }
    }
    */
}
