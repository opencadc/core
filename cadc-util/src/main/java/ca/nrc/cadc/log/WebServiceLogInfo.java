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

package ca.nrc.cadc.log;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.date.DateUtil;
import ca.nrc.cadc.util.StringUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import org.apache.log4j.Logger;

/**
 * Class to be used by web services to log at INFO level the start and
 * end messages for each request. All non-private, non-static, non-transient
 * fields are logged in a simple JSON string.
 *
 * @author majorb
 *
 */
public abstract class WebServiceLogInfo {

    private static final Logger log = Logger.getLogger(WebServiceLogInfo.class);

    private boolean userSuccess = true;

    protected String serviceName;
    protected Long bytes;
    protected String ip;
    protected String jobID;
    protected String message;
    protected String method;
    protected String authMethod;
    protected String path;
    protected String proxyUser;
    protected String runID;
    protected Boolean success;
    protected Long duration;
    
    protected URI resource;
    protected String grant;

    public String user;

    protected WebServiceLogInfo() { }

    /**
     * Generates the log.info message for the start of the request.
     *
     * @return
     */
    public String start() {
        return "{" + getPreamble() + "\"phase\":\"start\"," + doit() + "}";
    }

    /**
     * Generates the log.info message for the end of the request.
     *
     * @return
     */
    public String end() {
        this.success = userSuccess;
        return "{" + getPreamble() + "\"phase\":\"end\"," + doit() + "}";
    }

    String doit() {
        StringBuilder sb = new StringBuilder();
        populate(sb, this.getClass());
        return sb.toString();
    }

    private String getTimestamp() {
        DateFormat format = DateUtil.getDateFormat(DateUtil.ISO_DATE_FORMAT,  DateUtil.UTC);
        Date date = new Date(System.currentTimeMillis());
        return "\"@timestamp\":\"" + format.format(date) + "\"";
    }

    private String getServiceName() {
        return "\"service\":{\"name\":\"" + serviceName + "\"}";
    }

    private String getThreadName() {
        return "\"thread\":{\"name\":\"" + Thread.currentThread().getName() + "\"}";
    }

    private String getLoglevel() {
        return "\"log\":{\"level\":\"info\"}";
    }

    private String getPreamble() {
        StringBuilder sb = new StringBuilder();
        sb.append(getTimestamp()).append(",");
        sb.append(getServiceName()).append(",");
        sb.append(getThreadName()).append(",");
        sb.append(getLoglevel()).append(",");
        return sb.toString();
    }

    // handle Lists, refer to GroupLogInfo.addedMembers
    // currently only List<String> is handled
    private void populateList(StringBuilder sb, List<?> o) {
        sb.append("[");
        if (!(o.isEmpty())) {
            boolean isFirstElement = true;
            for (Object element : o) {
                String val = sanitize(element);
                if (isFirstElement) {
                    isFirstElement = false;
                } else {
                    sb.append(",");
                }
                if (element instanceof String || element instanceof UUID) {
                    sb.append("\"").append(val).append("\"");
                } else {
                    sb.append(val);
                }
            }
        }
        sb.append("]");
    }
    
    private void populate(StringBuilder sb, Class<?> c) {
        for (Field f : c.getDeclaredFields()) {
            log.debug("found field: " + f.getName());
            int m = f.getModifiers();
            boolean inc = true;
            inc = inc && !Modifier.isStatic(m);
            inc = inc && !Modifier.isPrivate(m);
            inc = inc && !Modifier.isTransient(m);
            if (inc) {
                try {
                    Object o = f.get(this);
                    log.debug(f.getName() + " = " + o);
                    if (o != null && !f.getName().equals("serviceName")) {
                        String val = sanitize(o);
                        if (sb.length() > 1) { // more than just the opening 
                            sb.append(",");
                        } 
                        sb.append("\"").append(f.getName()).append("\"");
                        sb.append(":");
                        if (o instanceof URI) {
                            // convert to String so serialized as a String
                            o = ((URI) o).toASCIIString();
                        }
                        if (o instanceof List<?>) {
                            populateList(sb, (List<?>) o);
                        } else if (o instanceof String) {
                            sb.append("\"").append(val).append("\"");
                        } else {
                            sb.append(val);
                        }
                    }
                } catch (IllegalAccessException ex) {
                    log.error("BUG: failed to get value for " + f.getName(), ex);
                }
            }
        }
        Class<?> sc = c.getSuperclass();
        if (WebServiceLogInfo.class.isAssignableFrom(sc)) {
            populate(sb, sc);
        }
    }

    static String sanitize(Object o) {
        String ret = o.toString();
        ret = ret.replaceAll("\"", "\'"); // double to single quote
        ret = ret.replaceAll("\\s+", " "); // multiple whitespace to single space
        return ret;
    }

    /**
     * Set the success/fail boolean.
     *
     * @param success
     */
    public void setSuccess(boolean success) {
        this.userSuccess = success;
    }

    /**
     * Set the subject. This will automatically determine the
     * userid for logging.
     *
     * @param subject
     */
    public void setSubject(Subject subject) {
        this.user = getUser(subject);
        AuthMethod amethod = AuthenticationUtil.getAuthMethod(subject);
        if (amethod != null) {
            this.authMethod = amethod.getValue();
        } else {
            this.authMethod = AuthMethod.ANON.getValue();
        }
    }

    /**
     * Set the elapsed time for the request to complete.
     *
     * @param elapsedTime
     */
    public void setElapsedTime(Long elapsedTime) {
        this.duration = elapsedTime;
    }

    /**
     * Set the number of bytes transferred in the request.
     *
     * @param bytes
     */
    public void setBytes(Long bytes) {
        this.bytes = bytes;
    }

    /**
     * Set a success or failure message.
     *
     * @param message
     */
    public void setMessage(String message) {
        if (StringUtil.hasText(message)) {
            this.message = message.trim();
        }
    }

    /**
     * Set the logical resource being accessed.
     * @param resource 
     */
    public void setResource(URI resource) {
        this.resource = resource;
    }

    /**
     * Set the grant allowing access to the resource. This is expected to be a 
     * group identifier (URI) or the string "public" when the resource can be 
     * accessed anonymously.
     * 
     * @param grant 
     */
    public void setGrant(String grant) {
        this.grant = grant;
    }
    
    /**
     * Set jobID. This is normally only needed in requests that create new jobs.
     *
     * @param jobID
     */
    public void setJobID(String jobID) {
        if (StringUtil.hasText(jobID)) {
            this.jobID = jobID.trim();
        }
    }

    protected String getUser(Subject subject) {
        try {
            if (subject != null) {
                final Set<HttpPrincipal> httpPrincipals = subject.getPrincipals(HttpPrincipal.class);
                if (!httpPrincipals.isEmpty()) {
                    HttpPrincipal principal = httpPrincipals.iterator().next();
                    this.proxyUser = principal.getProxyUser();
                    return principal.getName();
                }

                final Set<X500Principal> x500Principals = subject.getPrincipals(X500Principal.class);
                if (!x500Principals.isEmpty()) {
                    X500Principal principal = x500Principals.iterator().next();
                    return principal.getName();
                }
            }
        } catch (Throwable t) {
            // ignore - can't throw exceptions here
        }

        return null;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setRunID(String runID) {
        this.runID = runID;
    }
    
    protected String parseServiceName(String path) {
        if (path != null) {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return path.split("/")[0];
        }
        return "";
    }

}
