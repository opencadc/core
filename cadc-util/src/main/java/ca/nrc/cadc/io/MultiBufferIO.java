/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2020.                            (c) 2020.
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

package ca.nrc.cadc.io;

import ca.nrc.cadc.thread.ConditionVar;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.log4j.Logger;

/**
 * Double buffer IO loop implementation that uses a separate thread to write the
 * output.
 * 
 * @author pdowler
 */
public class MultiBufferIO {
    private static final Logger log = Logger.getLogger(MultiBufferIO.class);

    private static final String READ_FAIL = "read from input stream failed";
    private static final String WRITE_FAIL = "write to output stream failed";
    
    private final int numBuffers;
    private final int bufferSize;
    
    private final BlockingQueue<Item> rq = new LinkedBlockingQueue();
    private final BlockingQueue<Item> wq = new LinkedBlockingQueue();
    
    /**
     * Create default instance. The default is triple-buffered with 64K buffers.
     */
    public MultiBufferIO() {
        this(3, 64 * 1024);
    }
    
    /**
     * Create instance with non-default buffering. 
     * 
     * @param numBuffers number of buffers
     * @param bufferSize size of each buffer
     */
    public MultiBufferIO(int numBuffers, int bufferSize) { 
        if (numBuffers < 1) {
            throw new IllegalArgumentException("invalid numBuffers: " + numBuffers + " (minimum: 1)");
        }
        if (bufferSize < 8192) {
            throw new IllegalArgumentException("invalid bufferSize: " + bufferSize + " (minimum: 8192)");
        }
        this.numBuffers = numBuffers;
        this.bufferSize = bufferSize;
    }
    
    /**
     * Copy input stream to output stream.
     * 
     * @param input source/producer
     * @param output sink/consumer
     * @throws InterruptedException if the main thread gets an interrupt
     * @throws ReadException if input.read() fails
     * @throws WriteException if output.write() fails
     */
    public void copy(InputStream input, OutputStream output) throws InterruptedException, ReadException, WriteException {
        for (int i = 0; i < numBuffers; i++) {
            rq.put(new Item(bufferSize));
        }
        
        Worker w = new Worker(output);
        Thread writer = new Thread(w);
        writer.start();
        
        Throwable readFail = doit(input, w);
        log.debug("reader completed: " + readFail);
        if (readFail != null) {
            writer.interrupt();
        }
        writer.join();
        log.debug("writer completed: " + w.fail);
        
        if (readFail != null) {
            throw new ReadException(READ_FAIL, readFail);
        }
        if (w.fail != null) {
            throw new WriteException(WRITE_FAIL, w.fail);
        }
    }
    
    private Exception doit(InputStream istream, Worker w) {
        Exception ret = null;
        
        try {
            while (w.fail == null) {
                // check/clear interrupted flag and throw if necessary
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                //log.debug("reader: wait for buffer...");
                Item buf = rq.take();
                if (buf.num == -1) {
                    return null; // done
                }
                //log.debug("reader: read from stream...");
                buf.num = istream.read(buf.buffer);
                //log.debug("reader: queue buffer: " + buf.num);
                wq.put(buf);
                if (buf.num == -1) {
                    return null; // done
                }
            }
            //log.debug("reader: detected writer.fail: " + w.fail);
            return null;
        } catch (Exception ex) {
            Item term = new Item(0);
            term.num = -1;
            try {
                wq.put(term);
            } catch (InterruptedException oops) {
                log.debug("interrupted while putting terminator", oops);
            }
            return ex;
        }
    }
    
    private class Item {
        final byte[] buffer;
        int num = 0;
        
        Item(int sz) {
            buffer = new byte[sz];
        }
    }
    
    // take buffer from wq, write to output, recycle via rq
    private class Worker implements Runnable {
        private OutputStream ostream;
        Exception fail;

        public Worker(OutputStream ostream) {
            this.ostream = ostream;
        }

        @Override
        public void run() {
           
            try {
                while (true) {
                    // check/clear interrupted flag and throw if necessary
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    //log.debug("writer: wait for buffer...");
                    Item buf = wq.take();
                    //log.debug("writer: got buffer " + buf.num);
                    if (buf.num == -1) {
                        return; // done
                    }
                    //log.debug("writer: write to stream...");
                    ostream.write(buf.buffer, 0, buf.num);
                    //log.debug("writer: recycle buffer...");
                    rq.put(buf);
                }
            } catch (Exception ex) {
                this.fail = ex;
                Item term = new Item(0);
                term.num = -1;
                try {
                    rq.put(term);
                } catch (InterruptedException oops) {
                    log.debug("interrupted while putting terminator", oops);
                }
                return;
            }
        }
    }
}
