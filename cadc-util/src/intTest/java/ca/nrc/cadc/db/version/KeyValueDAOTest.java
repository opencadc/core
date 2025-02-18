/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2025.                            (c) 2025.
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

package ca.nrc.cadc.db.version;

import ca.nrc.cadc.db.ConnectionConfig;
import ca.nrc.cadc.db.DBConfig;
import ca.nrc.cadc.db.DBUtil;
import ca.nrc.cadc.db.DatabaseTransactionManager;
import ca.nrc.cadc.util.Log4jInit;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class KeyValueDAOTest {
    private static final Logger log = Logger.getLogger(KeyValueDAOTest.class);

    private static final String SCHEMA = "dbversion";
    private static final String DATABASE = "cadctest";
    private static final String TABLE = KeyValue.class.getSimpleName();
    private static final String TABLE_NAME = String.format("%s.%s", SCHEMA, TABLE);
    private static DataSource dataSource;

    private static final String CREATE_TEST_TABLE = "CREATE TABLE " + TABLE_NAME +
            " (name VARCHAR(32) NOT NULL PRIMARY KEY, value VARCHAR(32) NOT NULL, lastModified TIMESTAMP NOT NULL)";
    private static final String DELETE_FROM_TEST_TABLE = "DELETE FROM " + TABLE_NAME;
    private static final String DROP_TEST_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

    public KeyValueDAOTest() {
    }

    @BeforeClass
    public static void setup() throws Exception {
        Log4jInit.setLevel("ca.nrc.cadc.db.version", Level.INFO);

        DBConfig dbrc = new DBConfig();
        ConnectionConfig cc = dbrc.getConnectionConfig("DBUTIL_TEST", DATABASE);
        DBUtil.createJNDIDataSource("jdbc/KeyValueDAOTest", cc);
        dataSource = DBUtil.findJNDIDataSource("jdbc/KeyValueDAOTest");

        DatabaseTransactionManager txn = new DatabaseTransactionManager(dataSource);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        jdbc.execute(DROP_TEST_TABLE);
        jdbc.execute(CREATE_TEST_TABLE);
    }

    @Before
    public void cleanup() {
        log.info("cleanup: "  + DELETE_FROM_TEST_TABLE);
        try {
            dataSource.getConnection().createStatement().execute(DELETE_FROM_TEST_TABLE);
        } catch (SQLException oops) {
            log.warn("delete failed: " + oops);
        }
    }

    @Test
    public void testCreateGetDelete() {
        try {
            KeyValueDAO dao = new KeyValueDAO(dataSource, DATABASE, SCHEMA);

            KeyValue expected = new KeyValue("test-key");
            expected.value = "test-value";
            dao.put(expected);

            KeyValue actual = dao.get(expected.getName());
            Assert.assertNotNull(actual);
            Assert.assertEquals(expected.getName(), actual.getName());
            Assert.assertEquals(expected.value, actual.value);
            Assert.assertEquals(expected.lastModified, actual.lastModified);

            dao.delete(expected.getName());
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testList() {
        try {
            KeyValueDAO dao = new KeyValueDAO(dataSource, DATABASE, SCHEMA);

            KeyValue kv1 = new KeyValue("test-key-1");
            kv1.value = "test-value-1";
            dao.put(kv1);

            KeyValue kv2 = new KeyValue("test-key-2");
            kv2.value = "test-value-2";
            dao.put(kv2);

            KeyValue kv3 = new KeyValue("test-key-3");
            kv3.value = "test-value-3";
            dao.put(kv3);

            List<KeyValue> actual = dao.list();

            Assert.assertNotNull(actual);
            Assert.assertEquals(3, actual.size());
            Assert.assertEquals(kv1.getName(), actual.get(0).getName());
            Assert.assertEquals(kv1.value, actual.get(0).value);
            Assert.assertEquals(kv2.getName(), actual.get(1).getName());
            Assert.assertEquals(kv2.value, actual.get(1).value);
            Assert.assertEquals(kv3.getName(), actual.get(2).getName());
            Assert.assertEquals(kv3.value, actual.get(2).value);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

}
