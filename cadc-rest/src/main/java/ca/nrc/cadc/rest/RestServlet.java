/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2011.                            (c) 2011.
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

import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.log.ServletLogInfo;
import ca.nrc.cadc.log.WebServiceLogInfo;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.PrivilegedActionException;
import javax.security.auth.Subject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

/**
 * Very simple RESTful servlet that loads a separate RestAction subclass for each
 * supported HTTP action: get, post, put, delete.
 * 
 * @author pdowler
 */
public class RestServlet extends HttpServlet
{
    private static final long serialVersionUID = 201211071520L;

    private static final Logger log = Logger.getLogger(RestServlet.class);
    
    private Class<RestAction> getImpl;
    private Class<RestAction> postImpl;
    private Class<RestAction> putImpl;
    private Class<RestAction> deleteImpl;

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        String cname;
        this.getImpl = loadImpl(config, "get");
        this.postImpl = loadImpl(config, "post");
        this.putImpl = loadImpl(config, "put");
        this.deleteImpl = loadImpl(config, "delete");
    }
    
    private Class<RestAction> loadImpl(ServletConfig config, String method)
    {
        String cname = config.getInitParameter(method);
        if (cname != null)
        {
            try
            {
                Class<RestAction> ret = (Class<RestAction>) Class.forName(cname);
                log.info(method + ": " + cname + " [loaded]");
                return ret;
            }
            catch(Exception ex)
            {
                log.error(method + ": " + cname + " [FAILED]", ex);
            }
            finally { }
        }
        log.debug(method + ": [not configured]");
        return null;
    }

    /**
     * The default error response when a RestAction is not configured for a requested
     * HTTP action is status code 400 and text/plain error message.
     * 
     * @param action
     * @param response
     * @throws IOException 
     */
    protected void handleUnsupportedAction(String action, HttpServletResponse response)
        throws IOException
    {
        response.setStatus(400);
        response.setHeader("Content-Type", "text/plain");
        PrintWriter w = response.getWriter();
        w.println("unsupported: HTTP " + action);
        w.flush();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        if (getImpl == null)
        {
            handleUnsupportedAction("GET", response);
            return;
        }
        log.debug("doGet: " + request.getPathInfo());
        doit(request, response, getImpl);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        if (postImpl == null)
        {
            handleUnsupportedAction("POST", response);
            return;
        }
        log.debug("doPost: " + request.getPathInfo());
        doit(request, response, postImpl);
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        if (putImpl == null)
        {
            handleUnsupportedAction("PUT", response);
            return;
        }
        
        log.debug("doPut: " + request.getPathInfo());
        doit(request, response, putImpl);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        if (deleteImpl == null)
        {
            handleUnsupportedAction("DELETE", response);
            return;
        }
        
        log.debug("doDelete: " + request.getPathInfo());
        doit(request, response, deleteImpl);
    }

    
    protected void doit(HttpServletRequest request, HttpServletResponse response,  Class<RestAction> actionClass)
        throws IOException
    {
        SyncInput in = new SyncInput(request);
        SyncOutput out = new SyncOutput(response);
        
        WebServiceLogInfo logInfo = new ServletLogInfo(request);
        long start = System.currentTimeMillis();
        try
        {
            Subject subject = AuthenticationUtil.getSubject(request);
            logInfo.setSubject(subject);
            log.info(logInfo.start());
            
            RestAction action = actionClass.newInstance();
            
            action.setPath(request.getPathInfo());
            action.setSyncInput(in);
            action.setSyncOutput(out);
            action.setLogInfo(logInfo);

            doit(subject, action);
        }
        catch(IOException ex)
        {
            logInfo.setSuccess(false);
            logInfo.setMessage(ex.getMessage());
            log.debug("caught: " + ex);
            throw ex;
        }
        catch(Throwable t)
        {
            logInfo.setSuccess(false);
            logInfo.setMessage(t.getMessage());
            handleUnexpected(out, t);
        }
        finally
        {
        	logInfo.setElapsedTime(System.currentTimeMillis() - start);
        	log.info(logInfo.end());        	
        }
    }

    private void doit(Subject subject, RestAction action)
        throws Exception
    {
        if (subject == null)
            action.run();
        else
        {
            try
            {
                Subject.doAs(subject, action);
            }
            catch(PrivilegedActionException pex)
            {
                if (pex.getCause() instanceof ServletException)
                    throw (ServletException) pex.getCause();
                else if (pex.getCause() instanceof IOException)
                    throw (IOException) pex.getCause();
                else if (pex.getCause() instanceof RuntimeException)
                    throw (RuntimeException) pex.getCause();
                else
                    throw new RuntimeException(pex.getCause());
            }
        }
    }

    private void handleUnexpected(SyncOutput out, Throwable t)
        throws IOException
    {
        log.error("unexpected exception (SyncOutput.isOpen: " + out.isOpen() + ")", t);

        if (out.isOpen())
            return;

        out.setCode(500); // internal server error
        out.setHeader("Content-Type", "text/plain");
        PrintWriter w = out.getWriter();
        w.println("unexpected exception: " + t);
        w.flush();
    }
}
