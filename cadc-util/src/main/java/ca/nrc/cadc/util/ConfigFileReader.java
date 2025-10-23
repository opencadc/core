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

package ca.nrc.cadc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * Common code to find and read text-based configuration files. The default comment 
 * character is #, but this can be changed before reading.
 * 
 * @author pdowler
 */
public class ConfigFileReader {
    private static final Logger log = Logger.getLogger(ConfigFileReader.class);

    private static final String DEFAULT_CONFIG_DIR = System.getProperty("user.home") + "/config";
    public static final String CONFIG_DIR_SYSTEM_PROPERTY = ConfigFileReader.class.getName() + ".dir";

    private final File configFile;
    private String commentChars = "#";
    
    /**
     * Normal constructor to find the specified config file. This method checks for a
     * custom location (ca.nrc.cadc.util.ConfigFileReader.dir system property) and 
     * defaults to {user.home}/config.
     * 
     * @param filename relative filename for the configuration
     */
    public ConfigFileReader(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("filename cannot be null.");
        }

        String configDir = DEFAULT_CONFIG_DIR;
        if (System.getProperty(CONFIG_DIR_SYSTEM_PROPERTY) != null) {
            configDir = System.getProperty(CONFIG_DIR_SYSTEM_PROPERTY);
        }

        this.configFile = new File(new File(configDir), filename);
    }
    
    public ConfigFileReader(File configFile) {
        this.configFile = configFile;
    }
    
    /**
     * Specify one or more characters that delimit comments. The comment character 
     * and any text after it on the same line are ignored. Blank lines (0-length or 
     * whitespace) are always ignored.
     * 
     * @param commentChars new set of comment characters
     */
    public final void setCommentChars(String commentChars) {
        this.commentChars = commentChars;
    }

    public boolean canRead() {
        return Files.isReadable(this.configFile.toPath());
    }

    /**
     * Get all the active lines from the properties file. This strips out empty lines and comments.
     * 
     * @return list of active lines
     */
    public List<String> getRawContent() throws IOException {
        List<String> ret = new ArrayList<>();
        
        BufferedReader br = new BufferedReader(new FileReader(configFile));
        String line;
        while ((line = br.readLine()) != null) {
            for (char c : commentChars.toCharArray()) {
                int i = line.indexOf(c);
                if (i >= 0) {
                    line = line.substring(0, i);                
                }
            }
            line = line.trim();
            if (!line.isEmpty()) {
                ret.add(line);
            }
        }
        
        return ret;
    }
}
