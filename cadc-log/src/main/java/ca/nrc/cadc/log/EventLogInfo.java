/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2019.                            (c) 2019.
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

import ca.nrc.cadc.date.DateUtil;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.log4j.Logger;

/**
 * Class to be used by applications to log at INFO level either a single
 * event or the start and end of an event. All non-private, non-static, 
 * non-transient fields are logged in a simple JSON string.
 *
 * @author yeunga
 *
 */
public class EventLogInfo {

    private static final Logger log = Logger.getLogger(EventLogInfo.class);

    // name of the application
    private String applicationName;
    // ID of this application instance
    private String thread;
    // label for this action or this type of event
    private String label;
    
    // entity ID from the object being processed
    protected UUID entityID;
    // Artifact.uri if available
    protected URI artifactURI;
    // event life cycle: create | propagate
    protected EventLifeCycle lifeCycle;
    // key/value in the start of an iteration, e.g. lastModified date or bucket
    protected EventStartKVP start;
    // value (of lastModified date or bucket) within an iteration
    protected String value;
    // method used by the event, e.g. PUT, QUERY
    protected String method;
    // indicates the number of items processed
    protected int total;
    // time to process the event, or 
    //      to start getting results
    protected Long duration;
    protected Boolean success;

    public EventLogInfo(String appName, String label, String method) {
        this.applicationName = appName;
        this.thread = Thread.currentThread().getName();
        this.label = label;
        this.method = method;
    }

    /**
     * Generates the log.info message for the start of the request.
     * success field is not required and will be ignored if set.
     *
     * @return
     */
    public String start() {
        if (success != null) {
            this.success = null;
            log.warn("success = " + success + " was ignored for start event");
        }
        return "{" + getPreamble() + "\"event\":\"start\"," + doit() + "}";
    }

    /**
     * Generates the log.info message for the end of the request.
     * success field is required.
     *
     * @return
     */
    public String end() {
        if (success == null) {
            String msg = "success value is required for end event";
            throw new IllegalArgumentException(msg);
        }
        return "{" + getPreamble() + "\"event\":\"end\"," + doit() + "}";
    }

    /**
     * Generates the log.info message for a single event.
     * success field is optional
     *
     * @return
     */
    public String singleEvent() {
        return "{" + getPreamble() + "\"event\":\"single\"," + doit() + "}";
    }

    String doit() {
        StringBuilder sb = new StringBuilder();
        populate(sb, this.getClass());
        return sb.toString();
    }

    private String getTimestamp() {
        DateFormat format = DateUtil.getDateFormat(DateUtil.ISO_DATE_FORMAT, 
            DateUtil.UTC);
        Date date = new Date(System.currentTimeMillis());
        return "\"@timestamp\":\"" + format.format(date) + "\"";
    }

    private String getApplicationName() {
        return "\"application\":{\"name\":\"" + applicationName + "\"}";
    }

    private String getThread() {
        return "\"thread\":{\"name\":\"" + thread + "\"}";
    }

    private String getLabel() {
        return "\"label\":\"" + label + "\"";
    }

    private String getLoglevel() {
        return "\"log\":{\"level\":\"info\"}";
    }

    private String getPreamble() {
        StringBuilder sb = new StringBuilder();
        sb.append(getTimestamp()).append(",");
        sb.append(getApplicationName()).append(",");
        sb.append(getThread()).append(",");
        sb.append(getLabel()).append(",");
        sb.append(getLoglevel()).append(",");
        return sb.toString();
    }

    private void populate(StringBuilder sb, Class c) {
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
                    if (o != null) {
                        String val = sanitize(o);
                        if (sb.length() > 1) { // more than just the opening {
                            sb.append(",");
                        }
                        sb.append("\"").append(f.getName()).append("\"");
                        sb.append(":");
                        if (o instanceof String) {
                            sb.append("\"").append(val).append("\"");
                        } else {
                            if (f.getName().equals("start")) {
                                EventStartKVP startKVP = ((EventStartKVP) o);
                                sb.append("{\"key\":" 
                                    + startKVP.getEventStartKey() 
                                    + ",\"value\":\"" + startKVP.getValue() 
                                    + "\"}");
                            } else {
                                sb.append(val);
                            }
                        }
                    }
                } catch (IllegalAccessException ex) {
                    log.error("BUG: failed to get value for " + f.getName(), ex);
                }
            }
        }
    }

    static String sanitize(Object o) {
        String ret = o.toString();
        ret = ret.replaceAll("\"", "\'"); // double to single quote
        ret = ret.replaceAll("\\s+", " "); // multiple whitespace to single space
        return ret;
    }

    /**
     * Set the entity ID.
     *
     * @param entityID
     */
    public void setEntityID(UUID entityID) {
        this.entityID = entityID;
    }

    /**
     * Set the Artifact URI, if available.
     *
     * @param artifactURI
     */
    public void setArtifactURI(URI artifactURI) {
        this.artifactURI = artifactURI;
    }

    /**
     * Set the event life cycle.
     *
     * @param lifeCycle
     */
    public void setLifeCycle(EventLifeCycle lifeCycle) {
        this.lifeCycle = lifeCycle;
    }

    /**
     * Set the elapsed time for the event to complete or to start getting results.
     *
     * @param elapsedTime
     */
    public void setElapsedTime(Long elapsedTime) {
        this.duration = elapsedTime;
    }

    /**
     * Set the key/value pair of the starting item of the event.
     *
     * @param kvp
     */
    public void setStartKVP(EventStartKVP kvp) {
        this.start = kvp;
    }

    /**
     * Set the value of an item of the event, after the starting item.
     *
     * @param value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Set the total number of items processed.
     *
     * @param num
     */
    public void setTotal(int num) {
        this.total = num;
    }

    /**
     * Set the success/fail Boolean.
     *
     * @param isSuccessful
     */
    public void setSuccess(Boolean isSuccessful) {
        this.success = isSuccessful;
    }

}
