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

package ca.nrc.cadc.thread;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import org.apache.log4j.Logger;

/**
 * Implementation of a thread pool executor.
 * Thread pool is fixed size. At startup, threads are set to consume the contents of the BlockingQueue.
 * Threads in the pool are blocked when trying to pull tasks from an empty queue. (see API for BlockingQueue.take().)
 * Some jobs in the queue may be lost if the pool is terminated while working.
 */
public class ThreadedRunnableExecutor {
    private static Logger log = Logger.getLogger(ThreadedRunnableExecutor.class);

    private final String poolBasename = ThreadedRunnableExecutor.class.getName();
    private final BlockingQueue<Runnable> taskQueue;
    private final ArrayList<WorkerThread> threads;

    public ThreadedRunnableExecutor(BlockingQueue<Runnable> blockingQueue, int nthreads) {

        if (nthreads <= 0) {
            throw new IllegalArgumentException("nthreads should > 1 (" + nthreads + ")");
        }

        this.taskQueue = blockingQueue;
        this.threads = new ArrayList<>(nthreads);

        log.info(poolBasename + " - starting up");
        log.debug("initial thread count: " + threads.size() + " requested size: " + nthreads);

        while (threads.size() < nthreads) {
            int threadNum = threads.size() + 1;
            log.debug("adding worker thread " + threadNum);
            WorkerThread t = new WorkerThread();
            t.setDaemon(true);
            t.setName(poolBasename + "-" + threadNum);
            t.setPriority(Thread.MIN_PRIORITY);
            threads.add(t);
            t.start();
        }
        log.debug("after pool startup - thread count: " + threads.size() + " requested size: " + nthreads);
        log.debug(poolBasename + " - ctor done");
    }

    public void terminate() {
        log.debug(poolBasename + ".terminate() starting");

        // terminate thread pool members
        Iterator<WorkerThread> threadIter = threads.iterator();
        while (threadIter.hasNext()) {
            WorkerThread t = threadIter.next();
            log.debug(poolBasename + ".terminate() interrupting WorkerThread " + t.getName());
            threadIter.remove();
            t.interrupt();
        }

        log.debug(poolBasename + ".terminate() DONE");
    }
    
    // this is a bit of a hack and could incorrectly return true
    // - if called while a thread is in take()
    // - but FileSync only calls it when taskQueue is empty so currently: OK
    public boolean getAllThreadsIdle() {
        boolean ret = true;
        Iterator<WorkerThread> threadIter = threads.iterator();
        while (threadIter.hasNext()) {
            WorkerThread t = threadIter.next();
            ret = ret && t.idle();
        }
        return ret;
    }

    private class WorkerThread extends Thread {
        Runnable currentTask;

        WorkerThread() {
            super();
        }

        boolean idle() {
            return currentTask == null;
        }
        
        // threads keep running as long as they are in the threads list
        public void run() {
            log.debug(poolBasename + " - START");
            boolean cont = true;
            while (cont) {
                try {
                    log.debug("taking from taskQueue");
                    currentTask = null;
                    currentTask = taskQueue.take(); // should block on take from queue if queue empty
                    log.debug("running current task");
                    currentTask.run();
                    log.debug("finished running task");
                } catch (InterruptedException ex) {
                    // taskQueue.take() or the task.run() could throw this
                    log.debug("thread interrupted: " + ex);
                    return;
                } catch (Exception ignore) {
                    log.debug("poorly behaved task threw an exception.");
                } finally {
                    currentTask = null;
                }
            }
            log.debug(poolBasename + " - END");
        }
    }
}
