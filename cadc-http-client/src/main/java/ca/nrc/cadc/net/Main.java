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

package ca.nrc.cadc.net;

import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.AuthorizationToken;
import ca.nrc.cadc.auth.CertCmdArgUtil;
import ca.nrc.cadc.auth.RunnableAction;
import ca.nrc.cadc.net.event.TransferEvent;
import ca.nrc.cadc.net.event.TransferListener;
import ca.nrc.cadc.util.ArgumentMap;
import ca.nrc.cadc.util.Log4jInit;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.security.auth.Subject;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author pdowler
 */
public class Main implements Runnable, TransferListener {

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

            Log4jInit.setLevel("ca.nrc.cadc.net", level);
            Log4jInit.setLevel("ca.nrc.cadc.auth", level);

            Subject s = AuthenticationUtil.getAnonSubject();
            if (am.isSet("cert")) {
                s = CertCmdArgUtil.initSubject(am);
            } else if (am.isSet("token")) {
                s = getSubjectWithToken(am.getValue("token"), am.getValue("domain"));
            }

            List<String> urls = am.getPositionalArgs();
            String fname = am.getValue("in");
            if (fname != null) {
                File f = new File(fname);
                LineNumberReader r = new LineNumberReader(new FileReader(fname));
                String line = r.readLine();
                while (line != null) {
                    String[] tokens = line.split(" ");
                    if (tokens.length == 1) {
                        urls.add(tokens[0]);
                    } else {
                        log.info("ignoring line: " + line);
                    }
                    line = r.readLine();
                }
            }
            Main m = new Main(urls);
            Subject.doAs(s, new RunnableAction(m));
        } catch (Throwable t) {
            log.error("FAIL", t);
        }
    }

    private static Subject getSubjectWithToken(String token, String domain) {
        String[] ss = token.split(":");
        String tokenType = ss[0];
        String tokenValue = ss[1];
        List<String> domains = new ArrayList<>();
        domains.add(domain);
        AuthorizationToken atk = new AuthorizationToken(tokenType, tokenValue, domains);
        Subject s = new Subject();
        s.getPublicCredentials().add(atk);
        return s;
    }

    private static void usage() {
        System.out.println("usage: cadc-http-client [-v|--verbose|-d|--debug] [--in=<fname>] [<url> ...]");
        System.out.println("         --in : file name with URLs (one per line)");
        System.out.println("         <url> : URLs directly on the command line");
        System.out.println();
        System.out.println(" auth options");
        System.out.println("         --cert=<file> : file name with URLs (one per line)");
        System.out.println();
        System.out.println("         --token  : a token in the form {name}:{value} (e.g. bearer:blah-blah)");
        System.out.println("         --domain : domain the token should me sent to");
        System.exit(1);
    }

    private Subject subject;
    private List<String> urls;

    private Main() {
    }

    private Main(List<String> urls) {
        this.urls = urls;
    }

    public void run() {
        for (String surl : urls) {
            try {
                URL url = new URL(surl);
                File dest = new File(System.getProperty("user.dir"));
                HttpDownload doit = new HttpDownload(url, dest);
                doit.setOverwrite(true);
                doit.setTransferListener(this);
                doit.run();
                if (doit.getThrowable() != null) {
                    throw doit.getThrowable();
                }
            } catch (MalformedURLException ex) {
                log.error("invalid input URL: " + surl);
            } catch (Throwable t) {
                log.error("FAIL", t);
            }
        }
    }

    @Override
    public void transferEvent(TransferEvent te) {
        switch (te.getState()) {
            case TransferEvent.CONNECTED:
            case TransferEvent.DECOMPRESSING:
            case TransferEvent.TRANSFERING:
                log.debug(te);
                break;
            case TransferEvent.CONNECTING:
                log.info(te.getStateLabel() + ": " + te.getURL());
                break;
            case TransferEvent.COMPLETED:
                log.info(te.getStateLabel() + ": " + te.getURL() + " -> " + te.getFile());
                break;
            case TransferEvent.RETRYING:
                log.warn(te.getStateLabel() + ": " + te.getURL() + " reason: " + te.getError().getMessage());
                break;
            case TransferEvent.FAILED:
            case TransferEvent.CANCELLED:
                log.error(te.getStateLabel() + ": " + te.getURL() + " reason: " + te.getError().getMessage());
        }
    }

    @Override
    public String getEventHeader() {
        return "";
    }

}
