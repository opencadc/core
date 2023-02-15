/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2023.                            (c) 2023.
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
 *  : 5 $
 *
 ************************************************************************
 */

package ca.nrc.cadc.db;

import ca.nrc.cadc.util.Log4jInit;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class DBUtilTest {

    private static final Logger log = Logger.getLogger(DBUtilTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.db", Level.INFO);
    }

    public DBUtilTest() {
    }

    @Test
    public void testCreatePool() {
        String poolName = "jdbc/create-pool";
        long maxWait = 6000L;
        try {
            DBConfig dbrc = new DBConfig();
            ConnectionConfig cc = dbrc.getConnectionConfig("DBUTIL_TEST", "cadctest");
            DBUtil.PoolConfig pc = new DBUtil.PoolConfig(cc, 2, maxWait, "select 123");

            DBUtil.createJNDIDataSource(poolName, pc);

            DataSource pool = DBUtil.findJNDIDataSource(poolName);
            Assert.assertNotNull("pool", pool);

            Connection con1 = pool.getConnection();
            Assert.assertNotNull("connection 1", con1);
            log.info("connection 1: " + con1.getCatalog() + " " + con1.isValid(0));

            Connection con2 = pool.getConnection();
            Assert.assertNotNull("connection 2", con2);
            log.info("connection 2: " + con2.getCatalog() + " " + con2.isValid(0));

            long t1 = System.currentTimeMillis();
            try {
                log.info("get connection: BLOCKED for " + maxWait + " ...");
                Connection con3 = pool.getConnection();
                Assert.fail("expected pool timeout, got: " + con3.getCatalog() + " " + con3.isValid(0));
            } catch (SQLException ex) {
                if (!ex.getMessage().toLowerCase().contains("timeout")) {
                    throw ex;
                }

                long t2 = System.currentTimeMillis();
                log.info("  caught: " + ex);
                long timeout = t2 - t1;
                log.info("timeout: " + timeout);
                long dt = Math.abs(timeout - maxWait);
                Assert.assertTrue("timeout", dt < 1000L); // within 3 ms
            }

            con1.close();
            con2.close();

            t1 = System.currentTimeMillis();
            con1 = pool.getConnection();
            Assert.assertNotNull("connection 1", con1);
            log.info("connection 1: " + con1.getCatalog() + " " + con1.isValid(0));
            long t2 = System.currentTimeMillis();
            long dt = t2 - t1;
            Assert.assertTrue("connection returned to pool", dt < 1000L);

        } catch (Exception t) {
            log.error("unexpected exception", t);
            Assert.fail("unexpected exception: " + t);
        }
    }

}
