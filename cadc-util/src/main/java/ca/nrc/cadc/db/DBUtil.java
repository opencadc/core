/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2016.                            (c) 2016.
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
*  $Revision: 5 $
*
************************************************************************
*/

package ca.nrc.cadc.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.CannotCreateTransactionException;

/**
 * Utility class for obtaining JDBC DataSources.
 * 
 * @author pdowler
 */
public class DBUtil {
    private static final Logger log = Logger.getLogger(DBUtil.class);

    public static String DEFAULT_JNDI_ENV_CONTEXT = "java:/comp/env";

    /**
     * Create a DataSource with a single connection to the server. The DataSource is
     * tested before return. All failures are thrown as RuntimeException with a
     * Throwable (cause).
     *
     * @param config
     * @return a connected single connection DataSource
     * @throws DBConfigException
     */
    public static DataSource getDataSource(ConnectionConfig config) throws DBConfigException {
        return getDataSource(config, false, true);
    }

    /**
     * Create a DataSource with a single connection to the server. All failures are
     * thrown as RuntimeException with a Throwable (cause).
     *
     * @param config
     * @param suppressClose suppress close calls on the underlying Connection
     * @param test          test the datasource before return (might throw)
     * @return a connected single connection DataSource
     * @throws DBConfigException
     */
    public static DataSource getDataSource(ConnectionConfig config, boolean suppressClose, boolean test)
            throws DBConfigException {
        try {
            log.debug("server: " + config.getServer());
            log.debug("driver: " + config.getDriver());
            log.debug("url: " + config.getURL());
            log.debug("database: " + config.getDatabase());

            // load JDBC driver
            Class.forName(config.getDriver());

            SingleConnectionDataSource ds = new SingleConnectionDataSource(config.getURL(), config.getUsername(),
                    config.getPassword(), suppressClose);

            Properties props = new Properties();
            props.setProperty("APPLICATIONNAME", getMainClass());
            ds.setConnectionProperties(props);

            if (test) {
                testDS(ds, true);
            }

            return ds;
        } catch (ClassNotFoundException ex) {
            throw new DBConfigException("failed to load JDBC driver: " + config.getDriver(), ex);
        } catch (SQLException ex) {
            throw new DBConfigException("failed to open connection: " + config.getURL(), ex);
        }
    }

    /**
     * This just calls findJNDIDataSource(String).
     * 
     * @param dataSource
     * @return
     * @throws NamingException
     * @deprecated
     */
    public static DataSource getDataSource(String dataSource) throws NamingException {
        return findJNDIDataSource(dataSource);
    }

    /**
     * Create a data source with the specified configuration and bind it in a
     * standalone JNDI context so that a later call to getJNDIDataSource(String)
     * will return it. This is for use in command-line apps (not application
     * servers!!!!) and test code. This method uses the default JDNI environment
     * context that works in Tomcat App server as the name s
     * 
     * @param dataSourceName
     * @param config
     * @throws NamingException
     */
    public static void createJNDIDataSource(String dataSourceName, ConnectionConfig config) throws NamingException {
        createJNDIDataSource(dataSourceName, DEFAULT_JNDI_ENV_CONTEXT, config);
    }

    /**
     * Create a JNDI DataSOurce resource with the specified name and environment
     * context.
     * 
     * @param dataSourceName
     * @param envContextName
     * @param config
     * @throws NamingException
     */
    public static void createJNDIDataSource(String dataSourceName, String envContextName, ConnectionConfig config)
            throws NamingException {
        log.debug("createDataSource: " + dataSourceName + " START");
        StandaloneContextFactory.initJNDI();

        Context initContext = new InitialContext();
        Context envContext = (Context) initContext.lookup(envContextName);
        if (envContext == null) {
            envContext = initContext.createSubcontext(envContextName);
        }
        log.debug("env: " + envContext);

        DataSource ds = getDataSource(config, true, true);
        envContext.bind(dataSourceName, ds);
        log.debug("createDataSource: " + dataSourceName + " DONE");
    }

    /**
     * Find a JNDI DataSource in an application server context. This is a
     * convenience method for finding a DataSource via the default context
     * (java:/comp/env) that Tomcat uses.
     * 
     * @param dataSource JNDI name of the data source
     * @return a DataSource found via JNDI
     * @throws NamingException if the context name cannot be resolved
     */
    public static DataSource findJNDIDataSource(String dataSource) throws NamingException {
        return findJNDIDataSource(dataSource, DEFAULT_JNDI_ENV_CONTEXT);
    }

    /**
     * Find a JNDI DataSource in the specified context.
     * 
     * @param dataSource
     * @param envContextName
     * @return
     * @throws NamingException
     */
    public static DataSource findJNDIDataSource(String dataSource, String envContextName) throws NamingException {
        log.debug("getDataSource: " + dataSource);
        Context initContext = new InitialContext();
        Context envContext = (Context) initContext.lookup(envContextName);
        DataSource ds = (DataSource) envContext.lookup(dataSource);

        return ds;
    }

    /**
     * Return true if this is a known Spring DAO transient exception.
     * 
     * @param t
     * @return
     */
    public static boolean isTransientDBException(Throwable t) {
        if (t instanceof TransientDataAccessException || t instanceof CannotGetJdbcConnectionException
                || t instanceof CannotCreateTransactionException) {
            return true;
        }

        return false;
    }

    private static void testDS(DataSource ds, boolean keepOpen) throws SQLException {
        Connection con = null;
        try {
            con = ds.getConnection();
            DatabaseMetaData meta = con.getMetaData();
            if (!log.getEffectiveLevel().equals(Level.DEBUG)) {
                return;
            }

            log.debug("connected to server: " + meta.getDatabaseProductName() + " " + meta.getDatabaseMajorVersion()
                    + "." + meta.getDatabaseMinorVersion() + " driver: " + meta.getDriverName() + " "
                    + meta.getDriverMajorVersion() + "." + meta.getDriverMinorVersion());
        } finally {
            if (!keepOpen && con != null) {
                try {
                    con.close();
                } catch (Exception ignore) {
                    // do nothing
                }
            }
        }
    }

    /**
     * Try to infer a suitable application name.
     *
     * <p>TODO: make this config more generic and accessible to calling code
     *
     * @return
     */
    public static String getMainClass() {
        String ret = "java";
        try {
            throw new RuntimeException();
        } catch (RuntimeException rex) {
            StackTraceElement[] st = rex.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if (st[i].getClassName().startsWith("ca.nrc.cadc.")) {
                    ret = st[i].getClassName();
                }
            }
        }
        ret = shortenClassName(ret);
        return ret;
    }

    private static String shortenClassName(String cname) {
        String ret = cname;

        // truncate top-level packages
        if (ret.startsWith("ca.nrc.cadc.")) {
            ret = ret.substring("ca.nrc.cadc.".length());
        }

        while (ret.length() > 30) {
            // sybase program_name limit
            // strip off one package name at a time
            int ii = ret.indexOf('.');
            if (ii > 0) {
                ret = ret.substring(ii + 1);
            } else {
                ret = ret.substring(0, 30);
            }
        }
        return ret;
    }
}
