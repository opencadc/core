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

package ca.nrc.cadc.net;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.log4j.Logger;

/**
 * Simple wrapper around a Content-Type value to support comparisons. Current:
 * strips all extraneous whitespace. TODO: more careful comparison of parameters
 * that is order-independent.
 * 
 * @author pdowler
 */
public class ContentType {
    private static final Logger log = Logger.getLogger(ContentType.class);

    private String value;
    private String baseType;
    private final SortedMap<String,String> parameters = new TreeMap<>();

    public ContentType(String value) {
        if (value == null) {
            throw new IllegalArgumentException("null value");
        }
        
        this.value = toCanonicalForm(value);
        if (baseType == null) {
            throw new IllegalArgumentException("null base type");
        }
    }

    @Override
    public String toString() {
        return "ContentType[" + value + "]";
    }

    public String getValue() {
        return value;
    }

    public String getBaseType() {
        return baseType;
    }

    public SortedMap<String, String> getParameters() {
        return parameters;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        
        if (obj instanceof ContentType) {
            ContentType rhs = (ContentType) obj;
            return value.equalsIgnoreCase(rhs.value);
        }
        return false;
    }

    // remove extraneous whitespace
    private String toCanonicalForm(String s) {
        s = s.trim();
        String[] parts = s.split(";");
        this.baseType = parts[0].trim();
        for (int i = 1; i < parts.length; i++) {
            // parameters
            String[] kv = parts[i].trim().split("=");
            if (kv.length == 2) {
                parameters.put(kv[0], kv[1]);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(baseType);
        for (Map.Entry<String,String> me : parameters.entrySet()) {
            sb.append("; ").append(me.getKey()).append("=").append(me.getValue());
        }
        return sb.toString();
    }
}
