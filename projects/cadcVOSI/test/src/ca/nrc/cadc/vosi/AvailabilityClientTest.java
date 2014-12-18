/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2009.                            (c) 2009.
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
*  $Revision: 4 $
*
************************************************************************
*/
package ca.nrc.cadc.vosi;

import ca.nrc.cadc.net.HttpDownload;
import org.apache.log4j.Logger;
import org.easymock.EasyMock;
import org.jdom2.JDOMException;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.net.URL;

import static org.junit.Assert.*;

public class AvailabilityClientTest
{
    private static Logger log = Logger.getLogger(AvailabilityClientTest.class);

    @Test
    public void testNullURL() throws Exception
    {
        try
        {
            AvailabilityClient client = new AvailabilityClient();
            client.getAvailability(null);
            fail("null URL should throw IllegalArgumentException");
        }
        catch (IllegalArgumentException expected) {}
    }

    @Test
    public void testServiceResponding() throws Exception
    {
        final String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<vosi:availability xmlns:vosi=\"http://www.ivoa.net/xml/VOSIAvailability/v1.0\">\n" +
                "  <vosi:available>true</vosi:available>\n" +
                "  <vosi:note>service is accepting queries</vosi:note>\n" +
                "</vosi:availability>";

        final HttpDownload mockDownload = EasyMock.createMock(HttpDownload.class);
        mockDownload.run();
        EasyMock.expectLastCall();
        EasyMock.expect(mockDownload.getResponseCode()).andReturn(200).once();

        final ByteArrayOutputStream mockOut = EasyMock.createMock(ByteArrayOutputStream.class);
        EasyMock.expect(mockOut.toString("UTF-8")).andReturn(xml);

        EasyMock.replay(mockDownload, mockOut);

        AvailabilityClient client = new AvailabilityClient()
        {
            @Override
            protected HttpDownload getHttpDownload(URL url, ByteArrayOutputStream out)
            {
                return mockDownload;
            }

            @Override
            protected ByteArrayOutputStream getOutputStream()
            {
                return mockOut;
            }
        };

        Availability availability = client.getAvailability(new URL("http://localhost/foo"));

        assertNotNull(availability);
        assertTrue(availability.getStatus().isAvailable());

        EasyMock.verify(mockDownload, mockOut);
    }

    @Test
    public void testServiceNotResponding() throws Exception
    {
        final HttpDownload mockDownload = EasyMock.createMock(HttpDownload.class);
        mockDownload.run();
        EasyMock.expectLastCall().once();
        EasyMock.expect(mockDownload.getResponseCode()).andReturn(404).once();

        final ByteArrayOutputStream mockOut = EasyMock.createMock(ByteArrayOutputStream.class);

        EasyMock.replay(mockDownload, mockOut);

        AvailabilityClient client = new AvailabilityClient()
        {
            @Override
            protected HttpDownload getHttpDownload(URL url, ByteArrayOutputStream out)
            {
                return mockDownload;
            }

            @Override
            protected ByteArrayOutputStream getOutputStream()
            {
                return mockOut;
            }
        };

        Availability availability = client.getAvailability(new URL("http://localhost/foo"));

        assertNotNull(availability);
        assertFalse(availability.getStatus().isAvailable());

        EasyMock.verify(mockDownload, mockOut);
    }

    @Test
    public void testInvalidXMLReturned() throws Exception
    {
        final String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<vosi:availability xmlns:vosi=\"http://www.ivoa.net/xml/VOSIAvailability/v1.0\">\n" +
                "  <vosi:available>true</vosi:available>\n";

        final HttpDownload mockDownload = EasyMock.createMock(HttpDownload.class);
        mockDownload.run();
        EasyMock.expectLastCall().once();
        EasyMock.expect(mockDownload.getResponseCode()).andReturn(200).once();

        final ByteArrayOutputStream mockOut = EasyMock.createMock(ByteArrayOutputStream.class);
        EasyMock.expect(mockOut.toString("UTF-8")).andReturn(xml).once();

        EasyMock.replay(mockDownload, mockOut);

        AvailabilityClient client = new AvailabilityClient()
        {
            @Override
            protected HttpDownload getHttpDownload(URL url, ByteArrayOutputStream out)
            {
                return mockDownload;
            }

            @Override
            protected ByteArrayOutputStream getOutputStream()
            {
                return mockOut;
            }
        };

        Availability availability = client.getAvailability(new URL("http://localhost/foo"));

        assertNotNull(availability);
        assertFalse(availability.getStatus().isAvailable());

        EasyMock.verify(mockDownload, mockOut);
    }

}