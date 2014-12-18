/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2009.                            (c) 2009.
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
*  $Revision: 4 $
*
************************************************************************
*/

package ca.nrc.cadc.vosi.avail;

import ca.nrc.cadc.net.HttpDownload;
import ca.nrc.cadc.net.InputStreamWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.log4j.Logger;

/**
 *
 * @author pdowler
 */
public class CheckURL  implements CheckResource
{
    private final static Logger log = Logger.getLogger(CheckURL.class);

    private final String name;
    private final URL url;
    private final int expectedResponseCode;
    private final String expectedContentType;
    
    public CheckURL(String name, URL url, int expectedResponseCode, String expectedContentType)
    {
        this.name = name;
        this.url = url;
        this.expectedResponseCode = expectedResponseCode;
        this.expectedContentType = expectedContentType;
    }
    
    @Override
    public String toString()
    {
        return "CheckURL[" + name + "," + url + "]";
    }

    public void check() 
        throws CheckException 
    {
        HttpDownload get = null;
        try
        {
            InputStreamWrapper dump = new InputStreamWrapper() 
            {
                public void read(InputStream in) 
                    throws IOException 
                {
                    byte[] buf = new byte[8192];
                    int num;
                    while ( (num = in.read(buf)) != -1 )
                        log.debug("read: " + num);
                }
            };
            get = new HttpDownload(url, dump);
            get.setHeadOnly(true);
            get.setFollowRedirects(true);
            get.run();
        }
        catch(Exception ex)
        {
            log.debug("FAIL", ex);
            throw new CheckException(name + " " + url.toExternalForm() + " test failed: " + ex);
        }
            
        if (get.getThrowable() != null)
        {
            log.debug("FAIL", get.getThrowable());
            throw new CheckException(name + " " + url.toExternalForm() + " test failed: " + get.getThrowable());
        }
        
        int responseCode = get.getResponseCode();
        String contentType = get.getContentType();
        int i = contentType.indexOf(';');
        if (i > 0)
            contentType = contentType.substring(0, i);
        if ( responseCode != expectedResponseCode
                || (expectedContentType != null && !expectedContentType.equals(contentType)) )
        {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append(" ").append(url.toExternalForm()).append(" failed:");
            sb.append(" found ").append(contentType).append(" (").append(responseCode).append(")");
            sb.append(" expected ").append(expectedContentType).append(" (").append(expectedResponseCode).append(")");
            throw new CheckException(sb.toString());
        }
    }
}
