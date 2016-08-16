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

package ca.nrc.cadc.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Simple wrapper that keeps track of bytes read from the input stream. 
 * 
 * If a limit is provided, a ByteLimitExceededException will be thrown if that
 * limit is reached.
 * 
 * @version $Version$
 * @author majorb
 */
public class ByteCountInputStream extends InputStream implements ByteCounter
{
    
    private InputStream inputStream;
    private long byteCount = 0L;
    private Long byteLimit = null;

    /**
     * Constructor that takes the target input stream..
     * 
     * @param inputStream  The InputStream to wrap.
     */
    public ByteCountInputStream(final InputStream inputStream)
    {
        this.inputStream = inputStream;
    }
    
    /**
     * Constructor that takes the target input stream and a byte limit.
     * 
     * @param inputStream  The InputStream to wrap.
     * @param byteLimit    The limit to the number of bytes to read.
     */
    public ByteCountInputStream(final InputStream inputStream,
                                final long byteLimit)
    {
        this.inputStream = inputStream;
        if (byteLimit > 0)
            this.byteLimit = byteLimit;
    }
    
    /**
     * Return the number of bytes that were read.
     */
    @Override
    public long getByteCount()
    {
        return byteCount;
    }

    /**
     * The quota space left to be written to.
     *
     * @return      Quota space left, in bytes.
     */
    public Long getByteLimit()
    {
        return byteLimit;
    }

    @Override
    public int available() throws IOException
    {
        return inputStream.available();
    }
    
    @Override
    public void close() throws IOException
    {
        inputStream.close();
    }
    
    @Override
    public void mark(int readlimit)
    {
        inputStream.mark(readlimit);
    }
    
    @Override
    public boolean markSupported()
    {
        return inputStream.markSupported();
    }

    @Override
    public int read() throws IOException
    {
        if (hasReachedLimit())
            throw new ByteLimitExceededException(byteLimit);

        int value = inputStream.read();
        byteCount++;
        return value;
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        if (hasReachedLimit())
            throw new ByteLimitExceededException(byteLimit);

        int bytesRead = inputStream.read(b);
        if (bytesRead != -1)
            byteCount += bytesRead;

        return bytesRead;
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        if (hasReachedLimit())
            throw new ByteLimitExceededException(byteLimit);
               
        int bytesRead = inputStream.read(b, off, len);
        if (bytesRead != -1)
            byteCount += bytesRead;
        
        return bytesRead;
    }

    /**
     * Reset this stream, if supported.
     *
     * @throws IOException  If not supported, or something went wrong.
     *
     */
    @Override
    public void reset() throws IOException
    {
        inputStream.reset();
        byteCount = 0;
    }
    
    @Override
    public long skip(long n) throws IOException
    {
        return inputStream.skip(n);
    }

    /**
     * Obtain whether the byte limit has been reached.
     */
    private boolean hasReachedLimit()
    {
        if (byteLimit == null)
            return false;
        return byteCount >= byteLimit;
    }
}
