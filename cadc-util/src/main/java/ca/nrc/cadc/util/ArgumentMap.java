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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * Simple command-line argument utility that takes all arguments of the
 * form --key=value and stores them in a map of key=value. As a shortcut,
 * --key is equivalent to --key=<em>true</em>, where <em>true</em> is the
 * String representation of Boolean.TRUE. Arguments that start with a single
 * dash (-) are always mapped to Boolean.TRUE, whether or not they contain
 * an = sign.
 * 
 * As an added bonus/complexity, the character sequence %% can be used to delimit
 * values that would otherwise be split up by the invoking shell.
 *
 * @version $Revision: 325 $
 * @author $Author: pdowler $
 */
public class ArgumentMap
{
    @SuppressWarnings("unchecked")
    private Map<String,Object> map = new HashMap<>();
    private List<String> pos = new ArrayList<>();

    public ArgumentMap(String[] args)
    {
        this.map = new HashMap();
        for (int i = 0; i < args.length; i++)
        {
            String key = null;
            String str = null;
            Object value = null;
            if (args[i].startsWith("--"))
            {
                // put generic arg in argmap
                try
                {
                    int j = args[i].indexOf('=');
                    if (j <= 0)
                    {
                        // map to true
                        key = args[i].substring(2, args[i].length());
                        value = Boolean.TRUE;
                    }
                    else
                    {
                        // map to string value
                        key = args[i].substring(2, j);
                        str = args[i].substring(j + 1, args[i].length());

                        // special %% stuff %% delimiters
                        if (str.startsWith("%%"))
                        {
                            // look for the next %% on the command-line
                            str = str.substring(2, str.length());
                            if (str.endsWith("%%"))
                            {
                                value = str.substring(0, str.length() - 2);
                            }
                            else
                            {
                                StringBuilder sb = new StringBuilder(str);
                                boolean done = false;
                                while (i + 1 < args.length && !done)
                                {
                                    i++;
                                    if (args[i].endsWith("%%"))
                                    {
                                        str = args[i].substring(0, args[i].length() - 2);
                                        done = true;
                                    }
                                    else
                                        str = args[i];
                                    sb.append(" " + str);
                                }
                                value = sb.toString();
                            }
                        }
                        else
                            value = str;
                    }
                }
                catch (Exception ignorable)
                {
                    //log.debug(" skipping: " + ignorable.toString());
                }
            }
            else if (args[i].startsWith("-"))
            {
                try
                {
                    key = args[i].substring(1, args[i].length());
                    value = Boolean.TRUE;
                }
                catch (Exception ignorable)
                {
                    //log.debug(" skipping: " + ignorable.toString());
                }
            }
            else
            {
                pos.add(args[i]);
                //log.debug("pos: " + args[i]);
            }
            if (key != null && value != null)
            {
                //log.debug("map: " + key + "->" + value);
                Object old_value = map.put(key, value);
                //if (old_value != null) log.debug(" (old mapping removed: " + key + " : " + old_value + ")");
            }
        }
    }

    public String getValue(String key)
    {
        Object obj = map.get(key);
        if (obj != null) return obj.toString();
        return null;
    }
    
    public List<String> getPositionalArgs()
    {
        return pos;
    }

    public boolean isSet(String key)
    {
        return map.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public Set keySet()
    {
        return map.keySet();
    }
}
