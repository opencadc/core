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
 ************************************************************************
 */

package ca.nrc.cadc.db.version;

import ca.nrc.cadc.db.DatabaseTransactionManager;
import ca.nrc.cadc.net.ResourceNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Utility class to setup tables in the database.
 *
 * @author pdowler
 */
public abstract class InitDatabase {

    private static final Logger log = Logger.getLogger(InitDatabase.class);

    private final String modelName;
    private final String modelVersion;
    private final String prevModelVersion;

    /**
     * Sequence of SQL files to run to create from empty. Used by doInit().
     */
    protected final List<String> createSQL = new ArrayList<String>();
    
    /**
     * Sequence of SQL files to run to upgrade to current version. The
     * previous version is optional but requires that upgrade be idempotent.
     * Used by doInit().
     */
    protected final List<String> upgradeSQL = new ArrayList<String>();
    
    /**
     * Sequence of SQL files to run to perform database maintenance.
     * Used by doMaintenance().
     */
    protected final List<String> maintenanceSQL = new ArrayList<String>();

    private final DataSource dataSource;
    private final String database;
    private final String schema;

    /**
     * Constructor.
     * 
     * @param dataSource may be null to use parseDDL only
     * @param database may be null to use default database
     * @param schema may be null to use default schema
     * @param modelName short name for the model
     * @param modelVersion version of the model
     * @param prevModelVersion previous version from which upgrade is supported
     */
    public InitDatabase(DataSource dataSource, String database, String schema,
            String modelName, String modelVersion, String prevModelVersion) {
        assertNotNull("dataSource", dataSource);
        assertNotNull("modelName", modelName);
        assertNotNull("modelVersion", modelVersion);
        assertNotNull("prevModelVersion", prevModelVersion);
        this.dataSource = dataSource;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.prevModelVersion = prevModelVersion;
        
        this.database = database;
        this.schema = schema;
    }

    /**
     * Constructor. Create or idempotent upgrade when the previous version doesn't matter.
     * This can be used when the upgrade scripts can wipe and redo the entire init process,
     * such as populating the tap_schema for a TAP service.
     * 
     * @param dataSource may be null to use parseDDL only
     * @param database may be null to use default database
     * @param schema may be null to use default schema
     * @param modelName short name for the model
     * @param modelVersion version of the model
     */
    public InitDatabase(DataSource dataSource, String database, String schema,
            String modelName, String modelVersion) {
        assertNotNull("dataSource", dataSource);
        assertNotNull("modelName", modelName);
        assertNotNull("modelVersion", modelVersion);
        this.dataSource = dataSource;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        
        this.database = database;
        this.schema = schema;
        this.prevModelVersion = null;
    }
    
    private void assertNotNull(String name, Object value) {
        if (value == null) {
            throw new IllegalArgumentException(InitDatabase.class.getSimpleName() + ": " + name + " cannot be null");
        }
    }
    
    // used by int test code
    String getVersion() {
        ModelVersionDAO vdao = new ModelVersionDAO(dataSource, database, schema);
        KeyValue cur = vdao.get(modelName);
        if (cur != null) {
            return cur.value;
        }
        return null;
    }
    
    /**
     * Create or upgrade the configured database with CAOM tables and indices.
     *
     * @return true if tables were created/upgraded; false for no-op
     */
    public boolean doInit() {
        log.debug("doInit: " + modelName + " " + modelVersion);
        long t = System.currentTimeMillis();
        String prevVersion = null;

        DatabaseTransactionManager txn = new DatabaseTransactionManager(dataSource);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        try {
            // Current modelVersion
            KeyValue cur;

            // create new tables
            boolean createTables = false;

            // select SQL to execute
            List<String> ddls = new ArrayList<>();

            ModelVersionDAO vdao = new ModelVersionDAO(dataSource, database, schema);

            // check if model exists in the database
            if (tableInDatabase()) {
                log.debug("table exists: " + modelName);

                // get current ModelVersion
                cur = vdao.get(modelName);
                log.debug("found: " + cur);

                // quick check there is nothing to do and early return
                if (cur != null) {
                    if (modelVersion.equals(cur.value)) {
                        log.debug("doInit: already up to date - nothing to do");
                        return false;
                    } else if (prevModelVersion == null || prevModelVersion.equals(cur.value)) {
                        log.debug("doInit: possible to update - proceeding");
                    } else if (cur.value != null) {
                        throw new UnsupportedOperationException("doInit: cannot convert version " + cur.value + " (DB) to " + modelVersion + " (software)");
                    }
                } else {
                    log.debug("doInit: possible to create - proceeding");
                    createTables = true;
                    cur = new KeyValue(modelName);
                    ddls = createSQL;
                }
            } else {
                // new installation
                log.debug("doInit: possible to create - proceeding");
                createTables = true;
                cur = new KeyValue(modelName);
                ddls = createSQL;
            }

            // start transaction
            txn.startTransaction();
            if (cur != null && !createTables) {
                // recheck upgrade with lock
                cur = vdao.lock(modelName);
                if (modelVersion.equals(cur.value)) {
                    log.debug("doInit: already up to date - nothing to do");
                    // empty ddls ok: need to commit so can't return here
                } else if (prevModelVersion == null || prevModelVersion.equals(cur.value)) {
                    ddls = upgradeSQL;
                } else {
                    throw new UnsupportedOperationException("doInit: cannot convert version " + cur.value + " (DB) to " + modelVersion + " (software)");
                }
            }

            boolean ret = false;
            if (!ddls.isEmpty()) {
                // execute SQL
                for (String fname : ddls) {
                    log.info("process file: " + fname);
                    List<String> statements = parseDDL(fname, schema);
                    for (String sql : statements) {
                        log.info("execute statement:\n" + sql);
                        jdbc.execute(sql);
                    }
                }
                // update ModelVersion
                if (createTables) {
                    prevVersion = modelVersion;
                } else {
                    prevVersion = cur.value;
                }
                cur.value = modelVersion;
                vdao.put(cur);
                ret = true;
            }

            // commit transaction
            txn.commitTransaction();
            
            long dt = System.currentTimeMillis() - t;
            log.debug("doInit: " + modelName + " " + prevVersion + " to " + modelVersion + " " + dt + "ms");
            
            return ret;
        } catch (UnsupportedOperationException ex) {
            if (txn.isOpen()) {
                try {
                    txn.rollbackTransaction();
                } catch (Exception oops) {
                    log.error("failed to rollback transaction", oops);
                }
            }
            throw ex;
        } catch (Exception ex) {
            log.debug("epic fail", ex);

            if (txn.isOpen()) {
                try {
                    txn.rollbackTransaction();
                } catch (Exception oops) {
                    log.error("failed to rollback transaction", oops);
                }
            }
            throw new RuntimeException("failed to init database", ex);
        } finally {
            // check for open transaction
            if (txn.isOpen()) {
                log.error("BUG: open transaction in finally");
                try {
                    txn.rollbackTransaction();
                } catch (Exception ex) {
                    log.error("failed to rollback transaction in finally", ex);
                }
            }
        }
    }
    
    /**
     * Do time-based maintenance. 
     * 
     * @param lastModified if last modification timestamp is older than this: do the work
     * @param tag optional tag value used to replace tag symbol in SQL
     * @return true if performed, false if not time yet
     */
    public boolean doMaintenance(Date lastModified, String tag) {
        log.debug("doMaintenance: " + modelName + " " + modelVersion);
        long t = System.currentTimeMillis();
        String prevVersion = null;
        if (lastModified == null) {
            throw new IllegalArgumentException("lastModified cannot be null");
        }

        DatabaseTransactionManager txn = new DatabaseTransactionManager(dataSource);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        try {
            // get current ModelVersion
            ModelVersionDAO vdao = new ModelVersionDAO(dataSource, database, schema);
            KeyValue cur = vdao.get(modelName);
            log.debug("found: " + cur);

            // quick check there is nothing to do and early return
            if (cur == null) {
                throw new ResourceNotFoundException("ModelVersion not found: " + modelName);
            }
            if (lastModified.after(cur.lastModified)) {
                if (modelVersion.equals(cur.value)) {
                    log.debug("doMaintenance: possible to update - proceeding");
                } else {
                    throw new UnsupportedOperationException("doMaintenance: cannot operate on " + cur.value + " (DB) with " + modelVersion + " (software)");
                }
            } else {
                return false;
            }

            // start transaction
            txn.startTransaction();
            cur = vdao.lock(modelName);
            
            // recheck
            if (lastModified.after(cur.lastModified)) {
                if (modelVersion.equals(cur.value)) {
                    log.debug("doMaintenance: possible to update - proceeding");
                } else {
                    throw new UnsupportedOperationException("doMaintenance: cannot operate on " + cur.value + " (DB) with " + modelVersion + " (software)");
                }
            } else {
                txn.rollbackTransaction();
                return false;
            }
            
            boolean ret = false;
            if (!maintenanceSQL.isEmpty()) {
                // execute SQL
                for (String fname : maintenanceSQL) {
                    log.info("process file: " + fname);
                    List<String> statements = parseDDL(fname, schema, tag);
                    for (String sql : statements) {
                        log.info("execute statement:\n" + sql);
                        jdbc.execute(sql);
                    }
                }
                
                // update ModelVersion timestamp
                vdao.put(cur);
                ret = true;
            }

            // commit transaction
            txn.commitTransaction();
            
            long dt = System.currentTimeMillis() - t;
            log.debug("doMaintenance: " + modelName + " " + modelVersion + " " + dt + "ms");
            
            return ret;
        } catch (UnsupportedOperationException ex) {
            if (txn.isOpen()) {
                try {
                    txn.rollbackTransaction();
                } catch (Exception oops) {
                    log.error("failed to rollback transaction", oops);
                }
            }
            throw ex;
        } catch (Exception ex) {
            log.debug("epic fail", ex);

            if (txn.isOpen()) {
                try {
                    txn.rollbackTransaction();
                } catch (Exception oops) {
                    log.error("failed to rollback transaction", oops);
                }
            }
            throw new RuntimeException("failed to init database", ex);
        } finally {
            // check for open transaction
            if (txn.isOpen()) {
                log.error("BUG: open transaction in finally");
                try {
                    txn.rollbackTransaction();
                } catch (Exception ex) {
                    log.error("failed to rollback transaction in finally", ex);
                }
            }
        }
    }
    
    /**
     * Find SQL with the given filename.
     * 
     * @param fname
     * @return URl from which the file can be read
     */
    protected abstract URL findSQL(String fname);
    
    public List<String> parseDDL(String fname, String schema) throws IOException {
        return parseDDL(fname, schema, null);
    }
    
    public List<String> parseDDL(String fname, String schema, String tag) throws IOException {
        List<String> ret = new ArrayList<>();

        // find file
        URL url = findSQL(fname);
        log.info(this.getClass().getName() + " found: " + fname + " at " + url);
        if (url == null) {
            throw new RuntimeException("CONFIG: failed to find " + fname);
        }

        // read
        InputStreamReader isr = new InputStreamReader(url.openStream());
        LineNumberReader r = new LineNumberReader(isr);
        try {
            StringBuilder sb = new StringBuilder();
            String line = r.readLine();
            boolean eos = false;
            while (line != null) {
                line = line.trim();
                if (line.startsWith("--")) {
                    line = "";
                }
                if (!line.isEmpty()) {
                    if (line.endsWith(";")) {
                        eos = true;
                        line = line.substring(0, line.length() - 1);
                    }
                    sb.append(line).append(" ");
                    if (eos) {
                        String st = sb.toString();
                        
                        st = st.replaceAll("<schema>", schema);
                        if (tag != null) {
                            st = st.replace("<tag>", tag);
                        }
                        
                        log.debug("statement: " + st);
                        ret.add(st);
                        sb = new StringBuilder();
                        eos = false;
                    }
                }
                line = r.readLine();
            }
        } finally {
            r.close();
        }

        return ret;
    }

    private boolean tableInDatabase() {
        String tableName = ModelVersion.class.getSimpleName();
        Connection con = null;
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try {
            con = jdbc.getDataSource().getConnection();
            DatabaseMetaData dm = con.getMetaData();
            String db = database == null ? null : database.toLowerCase();
            String sm = this.schema == null ? null : this.schema.toLowerCase();
            ResultSet rs = dm.getTables(db, sm, tableName.toLowerCase(), null);
            if (rs != null && !rs.next()) {
                log.debug("table does not exist: " + tableName);
                return false;
            }
            log.debug("table exists: " + tableName);
        } catch (SQLException oops) {
            throw new RuntimeException("failed to determine if table exists: " + tableName, oops);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ignore) {
                    log.debug("failed to close database metadata query result", ignore);
                }
            }
        }
        return true;
    }

}
