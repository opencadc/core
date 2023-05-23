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

package ca.nrc.cadc.auth;

import ca.nrc.cadc.util.ArgumentMap;

import java.io.File;

import javax.security.auth.Subject;

import org.apache.log4j.Logger;

/**
 * Certificate Command Line Argument Utility class.
 * 
 * 
 * @author zhangsa
 * 
 */
public class CertCmdArgUtil {

    private static Logger log = Logger.getLogger(CertCmdArgUtil.class);
    public static final String NEW_LINE = System.getProperty("line.separator");
    public static final String ARG_CERT = "cert";
    public static final String ARG_KEY = "key";

    public static final String DFT_CERTKEY_FILE = "/.ssl/cadcproxy.pem";
    public static final String DFT_CERT_FILE = "/.ssl/cadcproxy.crt";
    public static final String DFT_KEY_FILE = "/.ssl/cadcproxy.key";

    private static String userHome = System.getProperty("user.home");

    /**
     * Return a string of the usage about certificate command line arguments. It's
     * usually used by the usage of a command line client program.
     * 
     * @return A string.
     */
    public static String getCertArgUsage() {
        return "   [--cert=<Cert File or Proxy Cert&Key PEM file> [--key=<Unencrypted Key File>]]";
    }

    private static File loadFile(String fn, boolean nullOnNotFound) {
        File f = new File(fn);
        if (!f.exists()) {
            if (nullOnNotFound) {
                return null;
            }
            
            throw new IllegalArgumentException(String.format("File %s does not exist.", fn));
        }
        if (!f.canRead()) {
            throw new IllegalArgumentException(String.format("File %s cannot be read.", fn));
        }
        
        return f;
    }

    private static Subject initSubjectByPem(String fnPem, boolean nullOnNotFound) {
        File certKeyFile = loadFile(fnPem, nullOnNotFound);
        if (nullOnNotFound && certKeyFile == null) {
            return null;
        }
        
        return SSLUtil.createSubject(certKeyFile);
    }

    /**
     * Init a subject from the command line and throw an exception if not
     * successful.
     * 
     * @see initSubject (ArgumentMap)
     * @param argMap
     * @return
     */
    public static Subject initSubject(ArgumentMap argMap) {
        return initSubject(argMap, false);
    }

    /**
     * Called by a commandline client program, it initializes and return a security
     * subject.
     * 
     * <p>Logic: if argument of "--key" is NOT provided, init subject from certkey PEM
     * file; otherwise, if --cert --key are provided, init from cert and key files;
     * otherwise, if default PEM file exists and is readable, init from it;
     * otherwise, if default cert and key files exist and are readable, init from
     * them; otherwise, throw IllegalArgumentException runtime exception.
     * 
     * <p>Note: the returned subject's credentials might be not valid. Use
     * ca.nrc.cadc.auth.validateSubject() method to check the validity of the
     * Subject's credentials.
     * 
     * @param argMap
     * @param returnNullOnNotFound return null if certficate files are not found in
     *                             the default locations
     * @return a Subject
     */
    public static Subject initSubject(ArgumentMap argMap, boolean returnNullOnNotFound) {
        String strCertKey;
        String strCert;
        String strKey;

        Subject subject = null;

        if (argMap.isSet(ARG_CERT)) {
            strCertKey = argMap.getValue(ARG_CERT);
            subject = initSubjectByPem(strCertKey, false);
        }
        return subject;
    }

}
