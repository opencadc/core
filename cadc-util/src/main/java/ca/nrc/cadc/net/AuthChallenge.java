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

package ca.nrc.cadc.net;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * Class to capture the value of a www-authenticate challenge.
 * 
 * @author pdowler
 */
public class AuthChallenge {
    private static final Logger log = Logger.getLogger(AuthChallenge.class);

    private final String name;
    private Map<String,String> params = new TreeMap<>();
        
    /**
     * Parse a www-authenticate challenge of the form
     * name [key="value"[, key="value" ...]]
     * 
     */
    public AuthChallenge(String challenge) {
        String s = challenge.replace(",", " "); // remove comma(s) between k=v pairs

        String[] parts = s.split("[ \t]");
        this.name = parts[0];
        if (parts.length == 1) {
            return;
        }
        
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i].trim();
            if (!p.isEmpty()) {
                String[] kv = parts[i].split("=");
                if (kv.length == 1) {
                    params.put(kv[0], null);
                } else {
                    // just ignore kv[2+], eg k=v=q
                    String v = kv[1].replace("\"", ""); // strip quotes
                    params.put(kv[0], v);
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getParamValue(String paramName) {
        return params.get(paramName);
    }
    
    public String toString() {
        return "Challenge[" + name + "," + params.size() + "]";
    }

    /**
     * Serialise a challenge as a www-authenticate header value.
     * 
     * @return www-authenticate header value
     */
    public String getHeaderValue() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" ");
        boolean needComma = false;
        for (Map.Entry<String,String> me : params.entrySet()) {
            if (needComma) {
                sb.append(",");
            }
            sb.append(me.getKey() + " =\"").append(me.getValue()).append("\"");
            needComma = true;
        }
        return sb.toString();
    }
}
