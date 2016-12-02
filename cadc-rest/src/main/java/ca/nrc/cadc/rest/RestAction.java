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

package ca.nrc.cadc.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.CertificateException;

import org.apache.log4j.Logger;

import ca.nrc.cadc.io.ByteLimitExceededException;
import ca.nrc.cadc.log.WebServiceLogInfo;
import ca.nrc.cadc.net.HttpTransfer;
import ca.nrc.cadc.net.ResourceAlreadyExistsException;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.net.TransientException;

/**
 * Base REST action class.
 * 
 * @author pdowler
 */
public abstract class RestAction implements PrivilegedExceptionAction<Object>
{
    private static final Logger log = Logger.getLogger(RestAction.class);

    protected SyncInput syncInput;
    protected SyncOutput syncOutput;
    protected WebServiceLogInfo logInfo;

    public static final String URLENCODED = "application/x-www-form-urlencoded";
    public static final String MULTIPART = "multipart/form-data";

    protected RestAction()
    {
        super();
    }

    /**
     * Create inline content handler to process non-form data. Non-form data could 
     * be a document or part of a multi-part request). Null return value is allowed 
     * if the service never expects non-form data or wants to ignore non-form data.
     * 
     * @return
     */
    abstract protected InlineContentHandler getInlineContentHandler();

    /**
     * Implemented by subclass
     *
     * The following exceptions, when thrown by this function, are
     * automatically mapped into HTTP errors by RestAction class:
     *
     *  java.lang.IllegalArgumentException : 400
     *  java.security.AccessControlException : 403
     *  java.security.cert.CertificateException : 403
     *  ca.nrc.cadc.net.ResourceNotFoundException : 404
     *  ca.nrc.cadc.net.ResourceAlreadyExistsException : 409
     *  ca.nrc.cadc.io.ByteLimitExceededException : 413
     *  ca.nrc.cadc.net.TransientException : 503
     *  java.lang.RuntimeException : 500
     *  java.lang.Error : 500
     * @throws Exception
     */
    public abstract void doAction() throws Exception;

    public void setLogInfo(WebServiceLogInfo logInfo)
    {
        this.logInfo = logInfo;
    }

    public void setSyncInput(SyncInput syncInput)
    {
        this.syncInput = syncInput;
    }

    public void setSyncOutput(SyncOutput syncOutput)
    {
        this.syncOutput = syncOutput;
    }

    public Object run()
        throws Exception
    {
        try
        {
            logInfo.setSuccess(false);
            if (syncInput != null)
            {
                syncInput.init();
            }

            doAction();

            logInfo.setSuccess(true);
        }
        catch(AccessControlException ex)
        {
            logInfo.setSuccess(true);
            handleException(ex, 403, ex.getMessage(), false, false);
        }
        catch(CertificateException ex)
        {
            handleException(ex, 403, "permission denied -- reason: invalid proxy certficate", false, true);
        }
        catch(IllegalArgumentException ex)
        {
            logInfo.setSuccess(true);
            handleException(ex, 400, ex.getMessage(), false, true);
        }
        catch(ResourceNotFoundException ex)
        {
            logInfo.setSuccess(true);
            handleException(ex, 404, ex.getMessage(), false, false);
        }
        catch(ResourceAlreadyExistsException ex)
        {
            logInfo.setSuccess(true);
            handleException(ex, 409, ex.getMessage(), false, false);
        }
        catch(ByteLimitExceededException ex)
        {
            logInfo.setSuccess(true);
            handleException(ex, 413, ex.getMessage(), false, false);
        }
        catch(UnsupportedOperationException ex)
        {
            logInfo.setSuccess(true);
            handleException(ex, 400, ex.getMessage(), false, false);
        }
        catch(TransientException ex)
        {
            logInfo.setSuccess(true);
            syncOutput.setHeader(HttpTransfer.SERVICE_RETRY, ex.getRetryDelay());
            handleException(ex, 503, "temporarily unavailable: " + syncInput.getPath(), true, false);
        }
        catch(RuntimeException unexpected)
        {
            logInfo.setSuccess(false);
            handleException(unexpected, 500, "unexpected failure: " + syncInput.getPath(), true, true);
        }
        catch(Error unexpected)
        {
            logInfo.setSuccess(false);
            handleException(unexpected, 500, "unexpected error: " + syncInput.getPath(), true, true);
        }

        return null;
    }

    /**
     * Add message to logInfo, print stack trace in debug level, and try to send a
     * response to output. If the output stream has not been opened already, set the
     * response code and write the message in text/plain. Optionally write a full
     * except5ion stack trace to output (showExceptions = true).
     *
     * @param ex
     * @param code
     * @param message
     * @param showExceptions
     * @throws IOException
     */
    protected void handleException(
        Throwable ex, int code, String message, boolean showStackTrace, boolean showExceptions)
            throws IOException
    {
        logInfo.setMessage(message);
        if (showStackTrace)
        {
            log.error(message, ex);
        }
        else
        {
            log.debug(message, ex);
        }
        if (!syncOutput.isOpen())
        {
            syncOutput.setCode(code);
            syncOutput.setHeader("Content-Type", "text/plain");

            OutputStream os = syncOutput.getOutputStream();
            StringBuilder sb = new StringBuilder();
            sb.append(message);

            if (showExceptions)
            {
                sb.append(ex.toString()).append("\n");
                Throwable cause = ex.getCause();
                while (cause != null)
                {
                    sb.append("cause: ");
                    sb.append(cause.toString()).append("\n");
                    cause = cause.getCause();
                }
            }

            os.write(sb.toString().getBytes());
        }
        else
            log.error("unexpected situation: SyncOutput is open", ex);
    }

}
