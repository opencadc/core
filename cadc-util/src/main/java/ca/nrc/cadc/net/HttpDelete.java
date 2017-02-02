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
 *
 ************************************************************************
 */

package ca.nrc.cadc.net;

import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessControlException;


public class HttpDelete extends HttpTransfer
{
    private static final Logger LOGGER = Logger.getLogger(HttpUpload.class);


    /**
     * Complete constructor.
     *
     * @param resourceURL     The resource to delete.
     * @param followRedirects True to follow the resource's redirect, or
     *                        False otherwise.
     */
    public HttpDelete(final URL resourceURL, final boolean followRedirects)
    {
        super(followRedirects);

        this.remoteURL = resourceURL;
    }


    @Override
    public String toString()
    {
        return HttpDelete.class.getSimpleName() + "[" + remoteURL + "]";
    }


    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run()
    {
        try
        {
            LOGGER.debug(remoteURL);
            final HttpURLConnection connection = connect();
            verifyDelete(connection);
        }
        catch (Throwable t)
        {
            LOGGER.debug("Failed to delete node: " + t, t);
            failure = t;
        }
    }

    /**
     * Verify the given connection can properly perform the delete.
     *
     * @param connection The open connection.
     * @throws IOException Any errors waiting for the response code and
     *                     message.
     */
    void verifyDelete(final HttpURLConnection connection) throws IOException
    {
        final int responseCode = connection.getResponseCode();
        final String responseMessage = connection.getResponseMessage();

        switch (responseCode)
        {
            case HttpURLConnection.HTTP_OK:
            {
                // Successful deletion.
                break;
            }

            case HttpURLConnection.HTTP_INTERNAL_ERROR:
            {
                // The service SHALL throw a HTTP 500 status code including an
                // InternalFault fault in the entity-body if the operation fails
                //
                // If a parent node in the URI path does not exist then
                // the service MUST throw a HTTP 500 status code including a
                // ContainerNotFound fault in the entity-body
                //
                // If a parent node in the URI path is a LinkNode,
                // the service MUST throw a HTTP 500 status code including a
                // LinkFound fault in the entity-body.
                //
                throw new RuntimeException(responseMessage);
            }

            case -1:
            case HttpURLConnection.HTTP_FORBIDDEN:
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            {
                // The service SHALL throw a HTTP 401 status code including a
                // PermissionDenied fault in the entity-body if the user does
                // not have permissions to perform the operation.
                //
                final String msg = (responseMessage == null)
                                   ? "permission denied" : responseMessage;

                throw new AccessControlException(msg);
            }

            case HTTP_LOCKED:
            {
                // The service SHALL throw a HTTP 423 status code if the given
                // resource is locked, or inaccessible.
                //
                throw new AccessControlException(responseMessage);
            }

            case HttpURLConnection.HTTP_NOT_FOUND:
            {
                // The service SHALL throw a HTTP 404 status code including a
                // FileNotFound fault in the entity-body if the target node
                // does not exist.
                //
                // If the target node in the URI path does not exist,
                // the service MUST throw a HTTP 404 status code including a
                // FileNotFound fault in the entity-body.
                //
                throw new FileNotFoundException(responseMessage);
            }

            default:
            {
                LOGGER.error(responseMessage + ". HTTP Code: "
                             + responseCode);
                throw new RuntimeException("unexpected failure mode: "
                                           + responseMessage + "("
                                           + responseCode + ")");
            }
        }
    }

    /**
     * Setup the HTTP Connection for use.  This does not obtain a status code
     * or any messages from the remote endpoint, it will only establish a
     * connection to use.
     *
     * @return The HTTPURLConneciton instance.
     * @throws IOException Any connectivity issues.
     */
    private HttpURLConnection connect() throws IOException
    {
        final HttpURLConnection connection =
                (HttpURLConnection) remoteURL.openConnection();

        if (connection instanceof HttpsURLConnection)
        {
            final HttpsURLConnection sslConn = (HttpsURLConnection) connection;
            initHTTPS(sslConn);
        }

        setRequestSSOCookie(connection);
        connection.setRequestMethod("DELETE");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(false);

        return connection;
    }
}
