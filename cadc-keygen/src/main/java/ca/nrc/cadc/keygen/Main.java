/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2011.                            (c) 2011.
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

package ca.nrc.cadc.keygen;

import ca.nrc.cadc.util.ArgumentMap;
import ca.nrc.cadc.util.Log4jInit;
import ca.nrc.cadc.util.RsaSignatureGenerator;
import java.io.File;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Simple utility to call RsaSignatureGenerator.genKeyPair(...).
 * 
 * @author pdowler
 */
public class Main {

    private static final Logger log = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            ArgumentMap am = new ArgumentMap(args);
            if (am.isSet("h") || am.isSet("help")) {
                usage();
            }
            Level level = Level.WARN;

            if (am.isSet("d") || am.isSet("debug")) {
                level = Level.DEBUG;
            } else if (am.isSet("v") || am.isSet("verbose")) {
                level = Level.INFO;
            }

            Log4jInit.setLevel("ca.nrc.cadc.keygen", level);
            Log4jInit.setLevel("ca.nrc.cadc.util", level);
            
            
            int keylen = 1024;
            String slen = am.getValue("len");
            if (slen != null) {
                keylen = Integer.parseInt(slen);
            }

            List<String> out = am.getPositionalArgs();
            if (out.size() != 2) {
                usage();
                System.exit(1);
            }
            File pub = new File(out.get(0));
            File priv = new File(out.get(1));
            
            Main m = new Main(pub, priv, keylen);
            m.run();
        } catch (Throwable t) {
            log.error("FAIL", t);
        }
    }

    private static void usage() {
        System.out.println("usage: cadc-keygen [-v|--verbose|-d|--debug] [--len=<key length>] <pubkey> <privkey>");
        System.out.println("                   --len=<key length> [default: 1024]");
        System.exit(1);
    }

    private File pubKey;
    private File privKey;
    private int len;

    private Main() {
    }

    private Main(File pub, File priv, int len) {
        this.pubKey = pub;
        this.privKey = priv;
        this.len = len;
    }

    public void run() {
        
        try {
            log.info("generating " + pubKey.getAbsolutePath() + " / " + privKey.getAbsolutePath() + " ...");
            RsaSignatureGenerator.genKeyPair(pubKey, privKey, len);
            log.info("generating " + pubKey.getAbsolutePath() + " / " + privKey.getAbsolutePath() + " ... [OK]");
        } catch (Exception ex) {
            log.error("failed to generate key pair", ex);
            log.info("generating " + pubKey.getAbsolutePath() + " / " + privKey.getAbsolutePath() + " ... [FAIL]");
        }
    }
}
