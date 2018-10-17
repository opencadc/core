/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2018.                            (c) 2018.
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;

/**
 *
 * @author pdowler
 */
public class KeyValueDAO {

    private static final Logger log = Logger.getLogger(KeyValueDAO.class);

    protected String[] columnNames;
    
    private final String tableName;
    private final JdbcTemplate jdbc;
    private final ResultSetExtractor extractor;

    private final Calendar utcCalendar = Calendar.getInstance(DateUtil.UTC);

    public KeyValueDAO(DataSource dataSource, String database, String schema) {
        this(dataSource, database, schema, KeyValue.class);
    }
    
    public KeyValueDAO(DataSource dataSource, String database, String schema, Class tupleType) {
        this.jdbc = new JdbcTemplate(dataSource);
        StringBuilder tn = new StringBuilder();
        if (database != null) {
            tn.append(database).append(".");
        }
        if (schema != null) {
            tn.append(schema).append(".");
        }
        tn.append(tupleType.getSimpleName());
        this.tableName = tn.toString();
        this.extractor = new ModelVersionExtractor();
        this.columnNames = new String[] { "value", "lastModified", "name" };
    }

    public KeyValue get(String name) {
        Object o = null;

        SelectStatementCreator sel = new SelectStatementCreator();
        sel.setValues(name);
        try {
            o = jdbc.query(sel, extractor);
        } catch (BadSqlGrammarException ex) {
            try {
                // try simples query possible to see if table exists
                log.debug("check exists: " + tableName);
                jdbc.queryForInt("SELECT count(*) FROM " + tableName);

                // some other kind of error
                throw ex;
            } catch (BadSqlGrammarException ex2) {
                log.debug("previous install not found: " + ex2.getMessage());
                o = null;
            }
        }
        if (o == null) {
            KeyValue mv = new KeyValue(name);
            log.debug("created: " + mv);
            return mv;
        }
        return (KeyValue) o;
    }

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
    
    public void delete(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name arg cannot be null");
        }
        String sql = "DELETE FROM " + tableName + " WHERE " + columnNames[2] + " = ?";
        Object[] arg = new Object[1];
        arg[0] = name;
        jdbc.update(sql, arg);
    }

    private class SelectStatementCreator implements PreparedStatementCreator {

        private String model;

        public SelectStatementCreator() {
        }

        public void setValues(String model) {
            if (model == null) {
                throw new IllegalStateException("null model");
            }
            this.model = model;
        }

        @Override
        public PreparedStatement createPreparedStatement(Connection conn)
                throws SQLException {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT ");
            sb.append(columnNames[0]).append(",");
            sb.append(columnNames[1]).append(",");
            sb.append(columnNames[2]);
            sb.append(" FROM ").append(tableName);
            sb.append(" WHERE ").append(columnNames[2]).append(" = ?");        
            String sql = sb.toString();
            PreparedStatement prep = conn.prepareStatement(sql);
            log.debug(sql);
            loadValues(prep);
            return prep;
        }

        private void loadValues(PreparedStatement ps)
                throws SQLException {
            ps.setString(1, model);
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
        public PreparedStatement createPreparedStatement(Connection conn)
                throws SQLException {
            StringBuilder sb = new StringBuilder();
                    
            if (update) {
                sb.append("UPDATE ").append(tableName).append(" SET ");
                sb.append(columnNames[0]).append(" = ?, ");
                sb.append(columnNames[1]).append(" = ?");
                sb.append(" WHERE ").append(columnNames[2]).append(" = ?");
            } else {
                sb.append("INSERT INTO ").append(tableName).append( "(");
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

        private void loadValues(PreparedStatement ps)
                throws SQLException {
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

    private class ModelVersionExtractor implements ResultSetExtractor {

        @Override
        public Object extractData(ResultSet rs)
                throws SQLException {
            KeyValue ret = null;
            if (rs.next()) {
                String value = rs.getString(1);
                Date lastModified = getDate(rs, 2, utcCalendar);
                String name = rs.getString(3);
                ret = new KeyValue(name);
                ret.value = value;
                ret.lastModified = lastModified;
            }
            return ret;
        }
    }
    
    private static Date getDate(ResultSet rs, int col, Calendar cal) throws SQLException {
        Object o = rs.getTimestamp(col, cal);
        return DateUtil.toDate(o);
    }
}
