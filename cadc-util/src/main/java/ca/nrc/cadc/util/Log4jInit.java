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
*  $Revision: 5 $
*
************************************************************************
*/

package ca.nrc.cadc.util;

import ca.nrc.cadc.date.DateUtil;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;

// ABOUT imports
// public API is log4j-1.2 package
// implementation is log4j-2 package

/**
 * Initialize log4j for the specified package and level.
 *
 */
public class Log4jInit {
    private static boolean consoleAppendersCreated = false;

    // SHORT_FORMAT applies to DEBUG and TRACE logging levels
    private static final String SHORT_FORMAT = "%-4r [%t] %-5p %c{1} - %m\n";

    // LONG_FORMAT applies to INFO, WARN, ERROR and FATAL logging levels
    private static final String LONG_FORMAT_A = "%d{" + DateUtil.ISO_DATE_FORMAT + "}";
    private static final String LONG_FORMAT_B = "[%t] %-5p %c{1} - %m\n";

    // LONG_FORMAT applies to machine oriented INFO logging levels
    private static final String MESSAGE_ONLY_FORMAT = "%m\n";
    
    private static List<Writer> logWriters = new ArrayList<Writer>();
    
    static {
        
    }
    
    private static Level toV2(org.apache.log4j.Level level) {
        switch (level.toInt()) {
            case org.apache.log4j.Level.ERROR_INT:
                return Level.ERROR;
            case org.apache.log4j.Level.WARN_INT:
                return Level.WARN;
            case org.apache.log4j.Level.INFO_INT:
                return Level.INFO;
            case org.apache.log4j.Level.DEBUG_INT:
                return Level.DEBUG;
            case org.apache.log4j.Level.TRACE_INT:
                return Level.TRACE;
            default:
                throw new IllegalArgumentException("unexpected level: " + level);
        }
    }
    
    /**
     * Initialize console logging.
     * 
     * @param pkg the name of package or ancestors of package or classes
     * @param level the logging level
     */
    public static synchronized void setLevel(String pkg, org.apache.log4j.Level level) {
        setLevel(null, pkg, level);
    }
    
    /**
     * Initialize console logging with specified appName.
     * 
     * @param appName application name to add to log messages
     * @param pkg the name of package or ancestors of package or classes
     * @param level the logging level
     */
    public static synchronized void setLevel(String appName, String pkg, org.apache.log4j.Level level) {
        createLog4jConsoleAppenders(appName);

        // set specified package and level
        //Logger.getLogger(pkg).setLevel(level);
        Configurator.setLevel(pkg, toV2(level));
    }
    

    /**
     * Configure log4j for use in a GUI. All output is done using the
     * same format. This method sets up logging to the specified writer
     * on every call, so should be called with a non-null writer only once
     * (per writer). It may be called multiple times to set logging levels for
     * different packages.
     * 
     * @param pkg package to configure logging for
     * @param level log level for pkg
     * @param dest destination writer to log to  (may be null)
     */
    public static synchronized void setLevel(String pkg, org.apache.log4j.Level level, Writer dest) {
        createLog4jWriterAppender(dest);

        // set specified package and level
        Configurator.setLevel(pkg, toV2(level));
    }

    /**
     * Clear all existing appenders, then create default console appenders for
     * Log4j.  Any other extra file appender has to be initialized AFTER this
     * method is called, otherwise they would be cleared.
     *
     * @param appName       The name of the application that is calling this
     *                      method.
     */
    private static synchronized void createLog4jConsoleAppenders(String appName) {
        if (!consoleAppendersCreated) {
            // Clear all existing appenders, if there's any.
            //BasicConfigurator.resetConfiguration();
            //Logger.getRootLogger().setLevel(Level.ERROR); // must redo after reset

            boolean messageOnly = "true".equals(System.getProperty(Log4jInit.class.getName() + ".messageOnly"));
            String pattern = LONG_FORMAT_A + LONG_FORMAT_B;
            if (messageOnly) {
                pattern = MESSAGE_ONLY_FORMAT;
            } else if (appName != null) {
                pattern = LONG_FORMAT_A + " " + appName + " " + LONG_FORMAT_B;
            }
            
            ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
            AppenderComponentBuilder appenderBuilder = builder.newAppender("out", "Console");
            appenderBuilder.add(builder.newLayout("PatternLayout").addAttribute("pattern", pattern));
            appenderBuilder.addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
            builder.add(appenderBuilder);
            
            builder.add(builder.newRootLogger(Level.ERROR).add(builder.newAppenderRef("out")));
            
            Configuration conf = builder.build();
            Configurator.reconfigure(conf);
            
            consoleAppendersCreated = true;
        }
    }
    
    /**
     * Create a Log4j appender which writes logs into a writer, i.e. a
     * FileWriter.
     * 
     * @param writer        The Writer to write to for the new appenders.
     */
    private static synchronized void createLog4jWriterAppender(Writer writer) {
        if (writer != null && !logWriters.contains(writer)) {
            String pattern = LONG_FORMAT_A + LONG_FORMAT_B;
            ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
            Configuration config = builder.build();
            Configurator.reconfigure(config);

            // remove default stdout appender
            for (String aname : config.getRootLogger().getAppenders().keySet()) {
                config.getRootLogger().removeAppender(aname);
            }
            // add WriterAppender
            PatternLayout layout = PatternLayout.newBuilder().withPattern(pattern).build();
            WriterAppender appender = WriterAppender.newBuilder()
                    .setName("writeLogger")
                    .setTarget(writer)
                    .setLayout(layout)
                    .build();
            appender.start();
            config.addAppender(appender);
            final Level level = null;
            final Filter filter = null;
            for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
                loggerConfig.addAppender(appender, level, filter);
            }
            config.getRootLogger().addAppender(appender, level, filter);
            
            logWriters.add(writer);  // Keep writer in the list so it's not created more than once.
        }
    }
}
