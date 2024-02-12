/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2024.                            (c) 2024.
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
import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.auth.IdentityManager;
import ca.nrc.cadc.cred.client.CredUtil;
import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.util.Log4jInit;
import ca.nrc.cadc.util.MultiValuedProperties;
import ca.nrc.cadc.util.PropertiesReader;
import ca.nrc.cadc.util.StringUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.opencadc.gms.GroupURI;
import org.opencadc.gms.IvoaGroupClient;

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

    private static final String LOG_CONTROL_CONFIG = "cadc-log.properties";
    static final String USER_X509_PROPERTY = "user";
    static final String GROUP_URIS_PROPERTY = "group";
    static final String USERNAME_PROPERTY = "username";
    static final String SECRET_PROPERTY = "secret";

    private Level level = null;
    private List<String> packages;

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
            logger.error("Authorization error", e);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        } catch (Throwable t) {
            logger.error("Authorization error", t);
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

        String secretQuery = request.getParameter(SECRET_PROPERTY);

        if (StringUtil.hasLength(secretQuery)) {
            url = url + "?" + SECRET_PROPERTY + "=" + secretQuery;
        }

        response.setHeader("Location", url);
    }

    /**
     * Check for proper group membership.
     */
    private void authorize(HttpServletRequest request, boolean readOnly)
        throws AccessControlException, TransientException {

        MultiValuedProperties mvp = getLogControlProperties();

        // Check the secret first.
        if (isAuthorizedSecret(request, mvp)) {
            logger.warn("Secret authorized.");
            return;
        }

        // ugh: this should happen earlier and all code should already be inside
        // a Subject.doAs(...) but this allows us to fall back to lazy augment
        // if normal augment fails
        Subject subject;

        try {
            subject = AuthenticationUtil.getSubject(request, true);
        } catch (Exception e) {
            logger.error("Augment subject failed, using non-augmented subject: " + e.getMessage());
            subject = AuthenticationUtil.getSubject(request, false);
        }

        logger.debug("caller: " + subject);
        IdentityManager im = AuthenticationUtil.getIdentityManager();
        String userDisplay = im.toDisplayString(subject);
        
        Set<Principal> authorizedUsers = getAuthorizedUserPrincipals(mvp);
        if (isAuthorizedUser(subject, authorizedUsers)) {
            logger.warn(userDisplay + " is an authorized user");
            return;
        }

        Set<GroupURI> groupUris = getAuthorizedGroupUris(mvp);
        if (!groupUris.isEmpty()) {
            try {
                if (CredUtil.checkCredentials(subject)) {
                    IvoaGroupClient groupClient = new IvoaGroupClient();
                    Set<GroupURI> mem = Subject.doAs(subject, 
                            (PrivilegedExceptionAction<Set<GroupURI>>) () -> groupClient.getMemberships(groupUris));
                    if (!mem.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(userDisplay).append(" is a member of:");
                        for (GroupURI g : mem) {
                            sb.append(" ").append(g.getURI().toASCIIString());
                        }
                        logger.warn(sb.toString());
                        return;
                    }
                }
            } catch (Exception e) {
                throw new AccessControlException("permission denied, reason: credential check failed: " + e.getMessage());
            }
        }

        throw new AccessControlException("permission denied");
    }

    /**
     * Supported secret submission is from a query parameter (secret=) or a header (X-CADC-LOGCONTROL).
     * @param request               The HTTP Servlet request.
     * @param multiValuedProperties The current log control properties.
     * @return      True if a provided secret matches a configured one.  False otherwise.
     */
    private boolean isAuthorizedSecret(HttpServletRequest request, MultiValuedProperties multiValuedProperties) {
        String requestParamSecret = request.getParameter(LogControlServlet.SECRET_PROPERTY);
        List<String> configuredSecrets = multiValuedProperties.getProperty(SECRET_PROPERTY);
        for (String configuredSecret : configuredSecrets) {
            // Intentionally kept as case-sensitive.
            if (configuredSecret.equals(requestParamSecret)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the caller Principal matches an authorized Principal
     * from the properties file.

     * @return true if the calling user is an authorized user, false otherwise.
     */
    private boolean isAuthorizedUser(Subject subject, Set<Principal> authorizedUsers) {
        if (!authorizedUsers.isEmpty()) {
            // Allow all principals to be checked.
            Set<Principal> principals = subject.getPrincipals(Principal.class);
            for (Principal caller : principals) {
                for (Principal authorizedUser : authorizedUsers) {
                    if (AuthenticationUtil.equals(authorizedUser, caller)) {
                        return true;
                    }
                }
            }
        } else {
            logger.debug("Authorized users not configured.");
        }
        return false;
    }

    /**
     * Get a Set of X500Principal's from the logControl properties. Return
     * an empty set if the properties do not exist or can't be read.
     *
     * @return Set of authorized X500Principals, can be an empty Set if none configured.
     */
    Set<Principal> getAuthorizedUserPrincipals(MultiValuedProperties mvp) {
        Set<Principal> principals = new HashSet<>();
        if (mvp != null) {
            try {
                List<String> properties = mvp.getProperty(USER_X509_PROPERTY);
                if (properties != null) {
                    for (String property : properties) {
                        if (!property.isEmpty()) {
                            principals.add(new X500Principal(property));
                        }
                    }
                }

                List<String> allowedUsernames = mvp.getProperty(LogControlServlet.USERNAME_PROPERTY);
                for (String allowedUsername : allowedUsernames) {
                    principals.add(new HttpPrincipal(allowedUsername));
                }
            } catch (IllegalArgumentException e) {
                logger.debug("No authorized users configured");
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
    Set<GroupURI> getAuthorizedGroupUris(MultiValuedProperties mvp) {
        Set<GroupURI> groupUris = new HashSet<GroupURI>();
        if (mvp != null) {
            try {
                List<String> properties = mvp.getProperty(GROUP_URIS_PROPERTY);
                if (properties != null) {
                    for (String property : properties) {
                        if (StringUtil.hasLength(property)) {
                            try {
                                groupUris.add(new GroupURI(new URI(property)));
                            } catch (IllegalArgumentException | URISyntaxException e) {
                                logger.error("invalid GroupURI: " + property, e);
                            }
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
    private MultiValuedProperties getLogControlProperties() {
        
        PropertiesReader reader = new PropertiesReader(LOG_CONTROL_CONFIG);
        if (reader.canRead()) {
            return reader.getAllProperties();
        }
        // empty
        return new MultiValuedProperties();
    }
}
