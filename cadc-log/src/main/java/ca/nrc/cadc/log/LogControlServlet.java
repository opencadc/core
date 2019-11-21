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

package ca.nrc.cadc.log;

import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.Authorizer;
import ca.nrc.cadc.auth.ServletPrincipalExtractor;
import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.util.Log4jInit;
import ca.nrc.cadc.util.PropertiesReader;
import ca.nrc.cadc.util.StringUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.security.AccessControlException;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Sets up log4j for whichever webapp contains this
 * servlet. To make sure the logging level gets set before any
 * logging gets done, set load-on-startup to a smaller
 * whole number than is used for any other servlet
 * in the webapp.
 * <p>
 * The initial level is set with an init-param named
 * <code>logLevel</code> and value equivalent to one of the
 * log4j levels (upper case, eg INFO).
 * </p>
 * <p>
 * The initially configured packages are set with an init-param
 * named <code>logLevelPackages</code> and value of whitespace-separated
 * package names.
 * </p>
 * <p>
 * The current configuration can be retrieved with an HTTP GET.
 * </p>
 * <p>
 * The configuration can be modified with an HTTP POST to this servlet.
 * The currently supported params are <code>level</code> (for example,
 * level=DEBUG) and <code>package</code> (for example, package=ca.nrc.cadc.log).
 * The level parameter is required. The package parameter is optional and
 * may specify a new package to configure
 * or a change in level for an existing package; if no packages are specified, the
 * level is changed for all previously configured packages.
 * </p>
 * <p>
 * Authorization to change logging levels can be configured using the init-param
 * <code>logAccessGroup</code> which is a URI of a group authorized to change
 * logging levels, and <code>groupAuthorizer</code> which is a fully qualified
 * class name of an Authenticator interface used to authenticate that the calling
 * user is a member of the authorized group.
 * </p>
 * <p>
 * An optional properties file containing authorized users and groups can be configured
 * using the init-param <code>logControlProperties</code>. The properties file
 * can contain one or both of the following properties. <code>users</code> is a
 * space delimited list of distinguished names for authorized users. The calling user
 * is compared to the list of users to authorize access. <code>groups</code>
 * is a space delimited list of authorized group URI's. The groupURI's are used
 * to authenticate that the calling user is a member of an authorized group.
 * </p>
 */
public class LogControlServlet extends HttpServlet {

    private static final long serialVersionUID = 200909091014L;

    private static final Logger logger = Logger.getLogger(LogControlServlet.class);

    private static final Level DEFAULT_LEVEL = Level.INFO;

    private static final String LOG_LEVEL_PARAM = "logLevel";
    private static final String PACKAGES_PARAM = "logLevelPackages";

    private static final String GROUP_PARAM = "logAccessGroup";
    private static final String GROUP_AUTHORIZER = "groupAuthorizer";

    private static final String LOG_CONTROL_PROPERTIES = "logControlProperties";
    static final String USER_DNS_PROPERTY = "users";
    static final String GROUP_URIS_PROPERTY = "groups";

    private Level level = null;
    private List<String> packages;

    private String authorizerClassName;
    private Authorizer groupAuthorizer;
    private String logControlProperties;

    /**
     * Initialize the logging. This method should only get
     * executed once and, if properly configured, it should
     * be the first method to be executed.
     *
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        this.packages = new ArrayList<String>();

        //  Determine the desired logging level.
        String levelVal = config.getInitParameter(LOG_LEVEL_PARAM);
        if (levelVal == null) {
            level = DEFAULT_LEVEL;
        } else if (levelVal.equalsIgnoreCase(Level.TRACE.toString())) {
            level = Level.TRACE;
        } else if (levelVal.equalsIgnoreCase(Level.DEBUG.toString())) {
            level = Level.DEBUG;
        } else if (levelVal.equalsIgnoreCase(Level.INFO.toString())) {
            level = Level.INFO;
        } else if (levelVal.equalsIgnoreCase(Level.WARN.toString())) {
            level = Level.WARN;
        } else if (levelVal.equalsIgnoreCase(Level.ERROR.toString())) {
            level = Level.ERROR;
        } else if (levelVal.equalsIgnoreCase(Level.FATAL.toString())) {
            level = Level.FATAL;
        } else {
            level = DEFAULT_LEVEL;
        }

        String webapp = config.getServletContext().getServletContextName();
        if (webapp == null) {
            webapp = "[?]";
        }

        String thisPkg = LogControlServlet.class.getPackage().getName();
        Log4jInit.setLevel(webapp, thisPkg, Level.WARN);
        packages.add(thisPkg);
        logger.warn("log level: " + thisPkg + " =  " + Level.WARN);

        String packageParamValues = config.getInitParameter(PACKAGES_PARAM);
        if (packageParamValues != null) {
            StringTokenizer stringTokenizer = new StringTokenizer(packageParamValues, " \n\t\r", false);
            while (stringTokenizer.hasMoreTokens()) {
                String pkg = stringTokenizer.nextToken();
                if (pkg.length() > 0) {
                    logger.warn(pkg + ": " + level);
                    Log4jInit.setLevel(webapp, pkg, level);
                    if (!packages.contains(pkg)) {
                        packages.add(pkg);
                    }
                }
            }
        }

        // get the access group and group authorizer
        String accessGroup = config.getInitParameter(GROUP_PARAM);
        authorizerClassName = config.getInitParameter(GROUP_AUTHORIZER);

        // instantiate the class if all configuration is present
        if (authorizerClassName != null) {
            try {
                Class authClass = Class.forName(authorizerClassName);
                if (accessGroup != null) {
                    try {
                        Constructor ctor = authClass.getConstructor(String.class);
                        Object o = ctor.newInstance(accessGroup);
                        groupAuthorizer = (Authorizer) o;
                    } catch (NoSuchMethodException ex) {
                        logger.warn("authorizer " + authorizerClassName + " has no constructor(String), ignoring accessGroup=" + accessGroup);
                        Object o = authClass.newInstance();
                        groupAuthorizer = (Authorizer) o;
                    }
                } else {
                    // no-arg constructor
                    Object o = authClass.newInstance();
                    groupAuthorizer = (Authorizer) o;
                }
            } catch (Exception e) {
                logger.error("Could not load group authorizer", e);
            }
        }

        // get the logControl properties file for this service if it exists
        logControlProperties = config.getInitParameter(LOG_CONTROL_PROPERTIES);

        // these are here to help detect problems with logging setup
        logger.warn("init complete");
        logger.info("init: YOU SHOULD NEVER SEE THIS MESSAGE -- " + thisPkg + " should not be included in " + PACKAGES_PARAM);
    }

    /**
     * In response to an HTTP GET, return the current logging level and the list
     * of packages for which logging is enabled.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            authorize(request, true);
        } catch (AccessControlException e) {
            logger.debug("Forbidden");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (TransientException e) {
            logger.error("Error calling group authorizer", e);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        } catch (Throwable t) {
            logger.error("Error calling group authorizer", t);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain");
        PrintWriter writer = response.getWriter();

        //writer.println("Logging level " + level + " set on " + packageNames.length + " packages:");
        for (String pkg : packages) {
            Logger log = Logger.getLogger(pkg);
            writer.println(pkg + " " + log.getLevel());
        }

        writer.close();
    }

    /**
     * Allows the caller to set the log level (e.g. with the level=DEBUG parameter).
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            authorize(request, false);
        } catch (AccessControlException e) {
            logger.debug("Forbidden");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (TransientException e) {
            logger.error("Error calling group authorizer", e);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        } catch (Throwable t) {
            logger.error("Error calling group authorizer", t);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        String[] params = request.getParameterValues("level");
        String levelVal = null;
        if (params != null && params.length > 0) {
            levelVal = params[0];
        }
        if (levelVal != null) {
            if (levelVal == null) {
                level = DEFAULT_LEVEL;
            } else if (levelVal.equalsIgnoreCase(Level.TRACE.toString())) {
                level = Level.TRACE;
            } else if (levelVal.equalsIgnoreCase(Level.DEBUG.toString())) {
                level = Level.DEBUG;
            } else if (levelVal.equalsIgnoreCase(Level.INFO.toString())) {
                level = Level.INFO;
            } else if (levelVal.equalsIgnoreCase(Level.WARN.toString())) {
                level = Level.WARN;
            } else if (levelVal.equalsIgnoreCase(Level.ERROR.toString())) {
                level = Level.ERROR;
            } else if (levelVal.equalsIgnoreCase(Level.FATAL.toString())) {
                level = Level.FATAL;
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/plain");
                PrintWriter writer = response.getWriter();
                writer.println("unrecognised value for level: " + levelVal);
                writer.close();
            }
        }

        String[] pkgs = request.getParameterValues("package");
        if (pkgs != null) {
            String dnt = request.getParameter("notrack");
            boolean track = (dnt == null);
            for (String p : pkgs) {
                logger.warn("setLevel: " + p + " -> " + level);
                Log4jInit.setLevel(p, level);
                if (!packages.contains(p) && track) {
                    packages.add(p);
                }
            }
        } else { 
            // all currently configured packages
            for (String p : packages) {
                logger.warn("setLevel: " + p + " -> " + level);
                Log4jInit.setLevel(p, level);
            }
        }

        // redirect the caller to the resulting settings
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        String url = request.getRequestURI();
        response.setHeader("Location", url);
    }

    /**
     * Check for proper group membership.
     */
    private void authorize(HttpServletRequest request, boolean readOnly)
        throws AccessControlException, TransientException {

        // first check if request user matches authorized config file users
        try {
            if (isAuthorizedUser(request)) {
                return;
            }
        } catch (Exception e) {
            if (e instanceof AccessControlException) {
                logger.warn("User authorization failed: " + e.getMessage());
            }
        }

        if (authorizerClassName != null && groupAuthorizer == null) {
            throw new RuntimeException("CONFIG: group authorizer was configured but failed to load: " + authorizerClassName);
        }

        Subject subject = AuthenticationUtil.getSubject(request);
        logger.debug(subject.toString());

        // Check group membership using config file properties
        Set<String> groupUris = getAuthorizedGroupUris();
        for (String groupUri : groupUris) {
            Authorizer authorizer = getAuthorizer(groupUri);
            if (authorizer != null) {
                try {
                    isAuthorizedGroup(authorizer, subject, readOnly);
                    return;
                } catch (AccessControlException ignore) { }
            }
        }

        // Check membership using servlet init parameters
        if (groupAuthorizer == null) {
            logger.warn("Authorization not configured, log control is public.");
            return;
        }
        isAuthorizedGroup(groupAuthorizer, subject, readOnly);
    }

    /**
     * Get a Group authorizer for the given group URI.
     *
     * @param groupURI groupURI string value.
     * @return A Group authorizer.
     */
    Authorizer getAuthorizer(String groupURI) {
        Authorizer authorizer = null;
        if (authorizerClassName != null) {
            try {
                Class authClass = Class.forName(authorizerClassName);
                if (groupURI != null) {
                    try {
                        Constructor ctor = authClass.getConstructor(String.class);
                        Object o = ctor.newInstance(groupURI);
                        authorizer = (Authorizer) o;
                    } catch (NoSuchMethodException ex) {
                        logger.warn("authorizer " + authorizerClassName + " has no constructor(String), ignoring groupURI=" + groupURI);
                        Object o = authClass.newInstance();
                        authorizer = (Authorizer) o;
                    }
                } else {
                    // no-arg constructor
                    Object o = authClass.newInstance();
                    authorizer = (Authorizer) o;
                }
            } catch (Exception e) {
                logger.error("Could not load group authorizer for groupURI=" + groupURI, e);
            }
        }
        return authorizer;
    }

    /**
     * Checks if the caller Principal matches an authorized Principal
     * from the properties file.
     *
     * @param request The Http request.
     * @return true if the calling Principal matches an authorized principal.
     */
    boolean isAuthorizedUser(HttpServletRequest request) throws AccessControlException {
        String callerName = "unknown";
        ServletPrincipalExtractor principalExtractor = new ServletPrincipalExtractor(request);
        Set<Principal> principals = principalExtractor.getPrincipals();
        for (Principal caller : principals) {
            if (caller instanceof X500Principal) {
                callerName = caller.getName();
                Set<Principal> authorizedPrincipals = getAuthorizedUserPrincipals();
                for (Principal authorizedPrincipal : authorizedPrincipals) {
                    if (AuthenticationUtil.equals(authorizedPrincipal, caller)) {
                        logger.debug("Authorized user: " + callerName);
                        return true;
                    }
                }
                break;
            }
        }
        throw new AccessControlException("Unauthorized user: " + callerName);
    }

    /**
     * Check if the given Subject is a member of the authorizer group.
     *
     * @param authorizer The Group authorizer
     * @param subject The Subject to check
     * @param readOnly Is access read only.
     * @return True is the authorizer check is successful, false otherwise.
     * @throws AccessControlException
     * @throws TransientException
     */
    void isAuthorizedGroup(Authorizer authorizer, Subject subject, boolean readOnly)
        throws AccessControlException, TransientException {
        GroupAuthorizationAction groupCheck = new GroupAuthorizationAction(authorizer, readOnly);
        try {
            if (subject == null) {
                groupCheck.run();
            } else {
                try {
                    Subject.doAs(subject, groupCheck);
                } catch (PrivilegedActionException e) {
                    throw e.getException();
                }
            }
        } catch (Exception e) {
            if (e instanceof AccessControlException) {
                throw (AccessControlException) e;
            }
            if (e instanceof TransientException) {
                throw (TransientException) e;
            }
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get a Set of X500Principal's from the logControl properties. Return
     * an empty set if the properties do not exist or can't be read.
     *
     * @return Set of authorized X500Principals, can be an empty Set if none configured.
     */
    Set<Principal> getAuthorizedUserPrincipals() {
        Set<Principal> principals = new HashSet<Principal>();
        PropertiesReader propertiesReader = getLogControlProperties();
        if (propertiesReader != null) {
            try {
                List<String> properties = propertiesReader.getPropertyValues(USER_DNS_PROPERTY);
                if (properties != null) {
                    for (String property : properties) {
                        if (StringUtil.hasLength(property)) {
                            principals.add(new X500Principal(property));
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                logger.debug("No authorized DN's configured");
            }
        }
        return principals;
    }

    /**
     * Get a Set of groupURI's from the logControl properties. Return
     * an empty set if the properties do not exist or can't be read.
     *
     * @return Set of authorized groupURI's, can be an empty Set if none configured.
     */
    Set<String> getAuthorizedGroupUris() {
        Set<String> groupUris = new HashSet<String>();
        PropertiesReader propertiesReader = getLogControlProperties();
        if (propertiesReader != null) {
            try {
                List<String> properties = propertiesReader.getPropertyValues(GROUP_URIS_PROPERTY);
                if (properties != null) {
                    for (String property : properties) {
                        if (StringUtil.hasLength(property)) {
                            groupUris.add(property);
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                logger.info("No authorized groupURI's configured");
            }
        }
        return groupUris;
    }

    /**
     * Read the logControl properties file and returns a PropertiesReader.
     *
     * @return A PropertiesReader, or null if the properties file does not
     *          exist or can not be read.
     */
    PropertiesReader getLogControlProperties() {
        PropertiesReader reader = null;
        if (logControlProperties != null) {
            reader = new PropertiesReader(logControlProperties);
            if (!reader.canRead()) {
                reader = null;
            }
        }
        return reader;
    }

    class GroupAuthorizationAction implements PrivilegedExceptionAction<Object> {

        private Authorizer authorizer;
        private boolean readOnly;

        GroupAuthorizationAction(Authorizer authorizer, boolean readOnly) {
            this.authorizer = authorizer;
            this.readOnly = readOnly;
        }

        @Override
        public Object run() throws Exception {
            try {
                if (readOnly) {
                    authorizer.getReadPermission(null);
                } else {
                    authorizer.getWritePermission(null);
                }
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("UnexpectedException", e);
            }

            return null;
        }
    }

}
