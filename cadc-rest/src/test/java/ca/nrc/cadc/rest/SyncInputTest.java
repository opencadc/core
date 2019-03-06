
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
 *
 ************************************************************************
 */

package ca.nrc.cadc.rest;


import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.junit.Test;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class SyncInputTest {

    @Test
    public void initBasic() throws Exception {
        final HttpServletRequest mockRequest = new StubHttpServletRequest("GET");
        final SyncInput testSubject = new SyncInput(mockRequest, null);
        final Map<String, String[]> requestParameters = mockRequest.getParameterMap();

        requestParameters.put("ONE", new String[] {});
        requestParameters.put("TWO", new String[] {"2"});
        requestParameters.put("THREE", new String[] {"3"});

        testSubject.init();

        Assert.assertNull("Wrong one param.", testSubject.getParameter("ONE"));
        Assert.assertEquals("Wrong two param.", "2", testSubject.getParameter("TWO"));
        Assert.assertEquals("Wrong three param.", "3", testSubject.getParameter("THREE"));
    }

    @Test
    public void initUpload() throws Exception {
        final Map<String, String[]> requestParameters = new HashMap<>();

        requestParameters.put("ONE", new String[] {"1", "1-ALT"});
        requestParameters.put("TWO", new String[] {"2"});

        final HttpEntity multipartEntity = createMultipartFormDataRequest("FILE_UPLOAD", requestParameters);
        final InputStream multipartEntityInputStream = multipartEntity.getContent();

        final HttpServletRequest mockRequest = new StubHttpServletRequest("POST",
                                                                          multipartEntity.getContentType().getValue()) {
            /**
             * Retrieves the body of the request as binary data using
             * a {@link ServletInputStream}.  Either this method or
             * {@link #getReader} may be called to read the body, not both.
             *
             * @return a {@link ServletInputStream} object containing
             * the body of the request
             *
             * @throws IllegalStateException if the {@link #getReader} method
             *                               has already been called for this request
             */
            @Override
            public ServletInputStream getInputStream() {
                return new ServletInputStream() {
                    @Override
                    public boolean isFinished() {
                        return false;
                    }

                    @Override
                    public boolean isReady() {
                        return false;
                    }

                    @Override
                    public void setReadListener(ReadListener readListener) {

                    }

                    @Override
                    public int read() throws IOException {
                        return multipartEntityInputStream.read();
                    }
                };
            }
        };
        final SyncInput testSubject = new SyncInput(mockRequest, new TestInlineContentHandler());

        testSubject.init();

        Assert.assertEquals("Wrong content", TestInlineContentHandler.CONTENT_VALUE,
                            testSubject.getContent(TestInlineContentHandler.CONTENT_NAME));
        Assert.assertArrayEquals("Wrong one params.", new String[] {"1", "1-ALT"},
                                 testSubject.getParameters("ONE").toArray());
        Assert.assertEquals("Wrong two param.", "2", testSubject.getParameter("TWO"));
    }

    private HttpEntity createMultipartFormDataRequest(final String fileParameterName,
                                                      final Map<String, String[]> textRequestParameters)
        throws IOException {

        final FileBody fileBody = new FileBody(File.createTempFile(".txt", SyncInputTest.class.getCanonicalName()),
                                               ContentType.DEFAULT_BINARY);

        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart(fileParameterName, fileBody);
        for (final Map.Entry<String, String[]> parameter : textRequestParameters.entrySet()) {
            final String key = parameter.getKey();

            for (final String param : parameter.getValue()) {
                builder.addPart(key, new StringBody(param, ContentType.MULTIPART_FORM_DATA));
            }
        }
        return builder.build();
    }
}
