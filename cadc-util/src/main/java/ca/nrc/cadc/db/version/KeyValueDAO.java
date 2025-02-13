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

import ca.nrc.cadc.date.DateUtil;
import ca.nrc.cadc.util.StringUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

/**
 *
 * @author pdowler
 */
public class KeyValueDAO {

    private static final Logger log = Logger.getLogger(KeyValueDAO.class);

    protected String[] columnNames;

    private final String database;
    private final String schema;
    private final String table;
    
    private final String tableName;
    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final ResultSetExtractor<KeyValue> extractor;

    private final Calendar utcCalendar = Calendar.getInstance(DateUtil.UTC);

    public KeyValueDAO(DataSource dataSource, String database, String schema) {
        this(dataSource, database, schema, KeyValue.class);
    }

    public KeyValueDAO(DataSource dataSource, String database, String schema, Class tupleType) {
        this.dataSource = dataSource;
        this.database = database;
        this.schema = schema;
        this.table = tupleType.getSimpleName();
        this.jdbc = new JdbcTemplate(dataSource);
        StringBuilder tn = new StringBuilder();
        if (database != null) {
            tn.append(database).append(".");
        }
        if (schema != null) {
            tn.append(schema).append(".");
        }
        tn.append(table);
        this.tableName = tn.toString();
        this.extractor = new KeyValueExtractor();
        this.columnNames = new String[] { "value", "lastModified", "name" };
    }

    /**
     * Lock the KeyValue with the given name for update.
     *
     * @param name the name of the key.
     * @return a KeyValue object or null if there was an locking the KeyValue.
     */
    public KeyValue lock(String name) {
        SelectStatementCreator sel = new SelectStatementCreator();
        sel.setValue(name, true);
        try {
            KeyValue ret = (KeyValue) jdbc.query(sel, extractor);
            return ret;
        } catch (BadSqlGrammarException ex) {
            log.error("error locking: " + name, ex);
            return null;
        }
    }

    /**
     * Get the KeyValue for the given name.
     *
     * @param name the name of the KeyValue.
     * @return a KeyValue object or null if not found.
     * @throws org.springframework.dao.DataAccessException if there is a problem querying the database.
     */
    public KeyValue get(String name) {
        SelectStatementCreator sel = new SelectStatementCreator();
        sel.setValue(name, false);
        return jdbc.query(sel, extractor);
    }

    /**
     * Put the KeyValue into the database.
     *
     * @param kv the KeyValue to put.
     * @throws org.springframework.dao.DataAccessException if there is a problem inserting into the database.
     */
    public void put(KeyValue kv) {
        boolean update = true;
        if (kv.lastModified == null) {
            update = false;
        }
        kv.lastModified = new Date();
        PutStatementCreator put = new PutStatementCreator(update);
        put.setValue(kv);
        jdbc.update(put);
    }

    /**
     * Delete the KeyValue with the given name.
     *
     * @param name the name of the KeyValue.
     * @throws org.springframework.dao.DataAccessException if there is a problem deleting from the database.
     */
    public void delete(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name arg cannot be null");
        }
        String sql = "DELETE FROM " + tableName + " WHERE " + columnNames[2] + " = ?";
        Object[] arg = new Object[1];
        arg[0] = name;
        jdbc.update(sql, arg);
    }

    /**
     * List all the KeyValue objects in the database.
     *
     * @return List of KeyValue objects.
     * @throws org.springframework.dao.DataAccessException if there is a problem querying the database.
     */
    public List<KeyValue> list() {

        SelectStatementCreator sel = new SelectStatementCreator();
        return jdbc.query(sel, new KeyValueRowMapper());
    }

    private class SelectStatementCreator implements PreparedStatementCreator {

        private boolean forUpdate;
        private String name;


        public SelectStatementCreator() {
            this.forUpdate = false;
        }

        public void setValue(String name, boolean forUpdate) {
            this.name = name;
            this.forUpdate = forUpdate;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection conn) throws SQLException {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT ");
            sb.append(columnNames[0]).append(",");
            sb.append(columnNames[1]).append(",");
            sb.append(columnNames[2]);
            sb.append(" FROM ").append(tableName);
            if (StringUtil.hasText(name)) {
                sb.append(" WHERE ").append(columnNames[2]).append(" = ?");
            }
            if (forUpdate) {
                sb.append(" FOR UPDATE");
            }
            String sql = sb.toString();
            PreparedStatement prep = conn.prepareStatement(sql);
            log.debug(sql);
            if (StringUtil.hasText(name)) {
                loadValues(prep);
            }
            return prep;
        }

        private void loadValues(PreparedStatement ps) throws SQLException {
            ps.setString(1, name);
        }
    }

    private class PutStatementCreator implements PreparedStatementCreator {

        private final boolean update;
        private KeyValue state;

        PutStatementCreator(boolean update) {
            this.update = update;
        }

        public void setValue(KeyValue state) {
            this.state = state;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection conn) throws SQLException {
            StringBuilder sb = new StringBuilder();

            if (update) {
                sb.append("UPDATE ").append(tableName).append(" SET ");
                sb.append(columnNames[0]).append(" = ?, ");
                sb.append(columnNames[1]).append(" = ?");
                sb.append(" WHERE ").append(columnNames[2]).append(" = ?");
            } else {
                sb.append("INSERT INTO ").append(tableName).append("(");
                sb.append(columnNames[0]).append(",");
                sb.append(columnNames[1]).append(",");
                sb.append(columnNames[2]).append(")");
                sb.append(" values(?, ?, ?)");
            }
            String sql = sb.toString();
            PreparedStatement prep = conn.prepareStatement(sql);
            log.debug(sql);
            loadValues(prep);
            return prep;
        }

        private void loadValues(PreparedStatement ps) throws SQLException {
            StringBuilder sb = new StringBuilder("values: ");
            int col = 1;

            ps.setString(col++, state.value);
            sb.append(state.value).append(",");

            ps.setTimestamp(col++, new Timestamp(state.lastModified.getTime()), utcCalendar);
            sb.append(state.lastModified).append(",");

            ps.setString(col++, state.getName());
            sb.append(state.getName()).append(",");

            log.debug(sb.toString());
        }
    }

    private class KeyValueExtractor implements ResultSetExtractor<KeyValue> {

        @Override
        public KeyValue extractData(ResultSet rs) throws SQLException {
            if (!rs.next()) {
                return null;
            }
            KeyValueRowMapper m = new KeyValueRowMapper();
            return m.mapRow(rs, 1);
        }
    }

    private class KeyValueRowMapper implements RowMapper<KeyValue> {
        Calendar utc = Calendar.getInstance(DateUtil.UTC);

        @Override
        public KeyValue mapRow(ResultSet rs, int i) throws SQLException {
            KeyValue keyValue = new KeyValue(rs.getString(3));
            keyValue.value = rs.getString(1);
            keyValue.lastModified = getDate(rs, 2, utcCalendar);
            return keyValue;
        }

    }

    private static Date getDate(ResultSet rs, int col, Calendar cal) throws SQLException {
        Object o = rs.getTimestamp(col, cal);
        return DateUtil.toDate(o);
    }

}
