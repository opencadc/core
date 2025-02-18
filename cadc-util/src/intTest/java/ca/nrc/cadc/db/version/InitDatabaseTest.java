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

package ca.nrc.cadc.db.version;

import ca.nrc.cadc.date.DateUtil;
import ca.nrc.cadc.db.ConnectionConfig;
import ca.nrc.cadc.db.DBConfig;
import ca.nrc.cadc.db.DBUtil;
import ca.nrc.cadc.util.Log4jInit;
import java.net.URL;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import javax.sql.DataSource;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author pdowler
 */
public class InitDatabaseTest {
    private static final Logger log = Logger.getLogger(InitDatabaseTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.db.version", Level.DEBUG);
    }
    
    private static String SCHEMA = "dbversion";
    static String[] TABLE_NAMES = new String[] {
        SCHEMA + ".ModelVersion",
        SCHEMA + ".test_table",
        SCHEMA + ".test_table_backup"
    };
    
    private DataSource dataSource;
    
    
    public InitDatabaseTest() throws Exception {
        DBConfig dbrc = new DBConfig();
        ConnectionConfig cc = dbrc.getConnectionConfig("DBUTIL_TEST", "cadctest");
        DBUtil.createJNDIDataSource("jdbc/InitDatabaseTest", cc);
        this.dataSource = DBUtil.findJNDIDataSource("jdbc/InitDatabaseTest");
    }
    
    @Before
    public void cleanup() throws Exception {
        for (String tn : TABLE_NAMES) {
            String sql = "DROP TABLE " + tn;
            log.info("cleanup: "  + sql);
            try {
                dataSource.getConnection().createStatement().execute(sql);
            } catch (SQLException oops) {
                log.warn("drop failed: " + oops);
            }
        }
    }
    
    @Test
    public void testCreate() {
        try {
            InitDatabase init = new InitDatabase(dataSource, null, SCHEMA, "InitDatabaseTest", "0.1") {
                @Override
                protected URL findSQL(String fname) {
                    // runtime resources from intTest/resources
                    return InitDatabaseTest.class.getClassLoader().getResource(fname);
                }
            };
            init.createSQL.add(SCHEMA + ".ModelVersion.sql");
            init.createSQL.add(SCHEMA + ".test_table.sql");
            init.createSQL.add(SCHEMA + ".permissions.sql");
            
            boolean b1 = init.doInit();
            Assert.assertTrue(b1);
            
            boolean b2 = init.doInit();
            Assert.assertFalse(b2);
            
            String ver = init.getVersion();
            Assert.assertEquals("0.1", ver);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testUpgrade() {
        try {
            log.info("init 0.1 ...");
            InitDatabase init = new InitDatabase(dataSource, null, SCHEMA, "InitDatabaseTest", "0.1") {
                @Override
                protected URL findSQL(String fname) {
                    // runtime resources from intTest/resources
                    return InitDatabaseTest.class.getClassLoader().getResource(fname);
                }
            };
            init.createSQL.add(SCHEMA + ".ModelVersion.sql");
            init.createSQL.add(SCHEMA + ".test_table.sql");
            init.createSQL.add(SCHEMA + ".permissions.sql");
            
            boolean b1 = init.doInit();
            Assert.assertTrue(b1);
            
            String ver = init.getVersion();
            Assert.assertEquals("0.1", ver);
            
            log.info("upgrade to 0.2 ...");
            InitDatabase upgrade = new InitDatabase(dataSource, null, SCHEMA, "InitDatabaseTest", "0.2", "0.1") {
                @Override
                protected URL findSQL(String fname) {
                    // runtime resources from intTest/resources
                    return InitDatabaseTest.class.getClassLoader().getResource(fname);
                }
            };
            upgrade.createSQL.add(SCHEMA + ".ModelVersion.sql");
            upgrade.createSQL.add(SCHEMA + ".test_table.sql");
            upgrade.createSQL.add(SCHEMA + ".permissions.sql");
            upgrade.upgradeSQL.add(SCHEMA + ".upgrade-0.2.sql");
            
            boolean u1 = upgrade.doInit();
            Assert.assertTrue(u1);
            
            ver = init.getVersion();
            Assert.assertEquals("0.2", ver);
            
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testMaintenance() {
        try {
            InitDatabase init = new InitDatabase(dataSource, null, SCHEMA, "InitDatabaseTest", "0.1") {
                @Override
                protected URL findSQL(String fname) {
                    // runtime resources from intTest/resources
                    return InitDatabaseTest.class.getClassLoader().getResource(fname);
                }
            };
            init.createSQL.add(SCHEMA + ".ModelVersion.sql");
            init.createSQL.add(SCHEMA + ".test_table.sql");
            init.createSQL.add(SCHEMA + ".permissions.sql");
            
            init.maintenanceSQL.add(SCHEMA + ".rollover.sql");
            init.maintenanceSQL.add(SCHEMA + ".test_table.sql");
            init.maintenanceSQL.add(SCHEMA + ".permissions.sql");
            
            boolean b1 = init.doInit();
            Assert.assertTrue(b1);
            Thread.sleep(10L);
            
            // SQL table name safe tag
            //DateFormat df = DateUtil.getDateFormat("yyyyMMdd", DateUtil.UTC);
            //String tag = df.format(start);
            Date now = new Date();
            Date past = new Date(now.getTime() - 10000L);
            String tag = "backup"; // see above cleanup
            log.info("rollover tag: " + tag);
            boolean b2 = init.doMaintenance(past, tag);
            Assert.assertFalse("past no-op", b2);
            
            boolean b3 = init.doMaintenance(now, tag);
            Assert.assertFalse("no doit", b2);
            
            // verify that the rename worked 
            
            String ver = init.getVersion();
            Assert.assertEquals("0.1", ver);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testUpgradeRejected() {
        try {
            log.info("init 0.1 ...");
            InitDatabase init = new InitDatabase(dataSource, null, SCHEMA, "InitDatabaseTest", "0.1") {
                @Override
                protected URL findSQL(String fname) {
                    // runtime resources from intTest/resources
                    return InitDatabaseTest.class.getClassLoader().getResource(fname);
                }
            };
            init.createSQL.add(SCHEMA + ".ModelVersion.sql");
            init.createSQL.add(SCHEMA + ".test_table.sql");
            init.createSQL.add(SCHEMA + ".permissions.sql");
            
            boolean b1 = init.doInit();
            Assert.assertTrue(b1);
            
            String ver = init.getVersion();
            Assert.assertEquals("0.1", ver);
            
            log.info("upgrade to 0.3 ...");
            InitDatabase upgrade = new InitDatabase(dataSource, null, SCHEMA, "InitDatabaseTest", "0.3", "0.2") {
                @Override
                protected URL findSQL(String fname) {
                    // runtime resources from intTest/resources
                    return InitDatabaseTest.class.getClassLoader().getResource(fname);
                }
            };
            upgrade.upgradeSQL.add(SCHEMA + ".ModelVersion.sql");
            upgrade.upgradeSQL.add(SCHEMA + ".test_table.sql");
            upgrade.upgradeSQL.add(SCHEMA + ".permissions.sql");
            upgrade.upgradeSQL.add(SCHEMA + ".upgrade-0.2.sql");
            
            boolean u1 = upgrade.doInit();
            Assert.fail("expected UnsupportedOperationException, got: " + u1);
            
        } catch (UnsupportedOperationException expected) {
            log.info("caught expected: " + expected);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
}
