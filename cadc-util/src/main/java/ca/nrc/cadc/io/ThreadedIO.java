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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

/**
 * This class performs two-threaded IO through a queue of bytes.
 * 
 * @author majorb
 */
public class ThreadedIO {
    
    private static Logger log = Logger.getLogger(ThreadedIO.class);
    
    private static int DEFAULT_BUFFER_SIZE_BYTES = 2 ^ 13; // = 8192
    private static int DEFAULT_MAX_QUEUE_SIZE_BUFFERS = 8; // = 65 536
    
    private int bufferSize = DEFAULT_BUFFER_SIZE_BYTES;
    private int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE_BUFFERS;
    
    /**
     * Default no-arg constructor.
     */
    public ThreadedIO() {
    }
    
    /**
     * Override the default buffer size and max queue size.
     * @param bufferSize The size of each byte[] in the queue (bytes)
     * @param maxQueueSize The max size of the queue
     */
    public ThreadedIO(int bufferSize, int maxQueueSize) {
        this.bufferSize = bufferSize;
        this.maxQueueSize = maxQueueSize;
    }

    /**
     * Stream between the input stream and output stream until all bytes
     * are read or until a read or write exception occurs.
     * @param out The byte destination.
     * @param in The byte source.
     * @throws ReadException If a failure occurred reading from in.
     * @throws WriteException If a failure occurred writing to out.
     */
    public void ioLoop(OutputStream out, InputStream in) throws ReadException, WriteException {

        Throwable producerThrowable = null;
        Throwable consumerThrowable = null;
        
        try {
            LinkedBlockingQueue<QueueItem> queue = new LinkedBlockingQueue<QueueItem>(maxQueueSize);
                
            QueueProducer producer = new QueueProducer(queue, in);
            QueueConsumer consumer = new QueueConsumer(queue, out);
            FutureTask<Throwable> producerTask = new FutureTask<Throwable>(producer);
            FutureTask<Throwable> consumerTask = new FutureTask<Throwable>(consumer);
            Thread producerThread = new Thread(producerTask);
            Thread consumerThread = new Thread(consumerTask);
            producer.consumer = consumerThread;
            consumer.producer = producerThread;
            
            consumerThread.start();
            producerThread.start();
            
            producerThrowable = producerTask.get();
            consumerThrowable = consumerTask.get();
                
        } catch (Throwable t) {
            String message = "i/o loop failed";
            log.error(message, t);
            throw new IllegalStateException(message, t);
        }
        
        if (producerThrowable != null) {
            String message = "failed reading from input stream";
            throw new ReadException(message, producerThrowable);
        }
        if (consumerThrowable != null) {
            String message = "failed writing to output stream"; 
            throw new WriteException(message, consumerThrowable);
        }
        
    }
    
    private class QueueProducer implements Callable<Throwable> {
        
        LinkedBlockingQueue<QueueItem> queue;
        InputStream in;
        Thread consumer;
        
        QueueProducer(LinkedBlockingQueue<QueueItem> queue, InputStream in) {
            this.queue = queue;
            this.in = in;
        }

        @Override
        public Throwable call() throws Exception {
            try {
                QueueItem next;
                byte[] buffer = new byte[bufferSize];
                int bytesRead = in.read(buffer);
                while (bytesRead > 0) {        
                    next = new QueueItem(buffer, bytesRead);
                    queue.put(next);
                    buffer = new byte[bufferSize];
                    bytesRead = in.read(buffer);
                    log.debug("read " + bytesRead + " bytes: " + new String(buffer));
                }
                log.debug("sending stop control to consumer");
                next = new QueueItem(null, 0);
                next.stop = true;
                queue.put(next);
                return null;
            } catch (InterruptedException e) {
                log.debug("Producer interrupted", e);
                return null;
            } catch (Throwable t) {
                consumer.interrupt();
                return t;
            }
        }
    }
    
    private class QueueConsumer implements Callable<Throwable> {
        
        LinkedBlockingQueue<QueueItem> queue;
        OutputStream out;
        Thread producer;
        
        QueueConsumer(LinkedBlockingQueue<QueueItem> queue, OutputStream out) {
            this.queue = queue;
            this.out = out;
        }

        @Override
        public Throwable call() throws Exception {
            QueueItem next;
            try {
                next = queue.take();
                while (!next.stop) {
                    out.write(next.data, 0, next.length);
                    log.debug("wrote " + next.length + " bytes: " + new String(next.data));
                    next = queue.take();
                    if (log.isDebugEnabled() && next.stop) {
                        log.debug("received stop control from producer");
                    }
                }
                out.flush();
                return null;
            } catch (InterruptedException e) {
                log.debug("consumer interrupted", e);
                return null;
            } catch (Throwable t) {
                producer.interrupt();
                return t;
            }
        }
    } 
    
    private class QueueItem {
        byte[] data;
        int length;
        boolean stop = false;
        
        QueueItem(byte[] data, int length) {
            this.data = data;
            this.length = length;
        }

    }
    
}
