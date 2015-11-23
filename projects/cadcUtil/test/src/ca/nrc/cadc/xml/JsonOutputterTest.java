/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2015.                            (c) 2015.
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

package ca.nrc.cadc.xml;

import ca.nrc.cadc.util.Log4jInit;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.StringWriter;
import java.io.Writer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom2.Attribute;

import org.jdom2.Namespace;
import org.jdom2.Text;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;


public class JsonOutputterTest
{
    private static final Logger log = Logger.getLogger(JsonOutputterTest.class);
    
    static
    {
        Log4jInit.setLevel("ca.nrc.cadc.xml", Level.DEBUG);
    }
    
    @Test
    public void writeMultiObject() throws Exception
    {
        final JsonOutputter testSubject = new JsonOutputter();
        testSubject.getListElementNames().add("items");
        testSubject.getStringElementNames().add("item");
        
        final Element root = new Element("root");
        final Element itemsElement = new Element("items");

        // Array of five items.
        for (int i = 0; i < 5; i++)
        {
            final Element itemElement = new Element("item");
            itemElement.getAttributes().add(new Attribute("i", Integer.toString(i)));
            itemElement.addContent(new Text(Integer.toString(i)));
            itemsElement.addContent(itemElement);
        }

        final Element metaElement = new Element("meta");
        metaElement.addContent(new Text("META"));

        root.addContent(metaElement);
        root.addContent(itemsElement);

        final Document document = new Document();
        document.setRootElement(root);

        final Writer writer = new StringWriter();

        testSubject.output(document, writer);

        final JSONObject expected = new JSONObject("{\"root\" : {"
                                                   + "\"meta\" : {\"$\": \"META\"},"
                                                   + "\"items\" : {"
                                                   + "\"$\" : ["
                                                   + "{\"@i\" : \"0\", \"$\" : \"0\"},"
                                                   + "{\"@i\" : \"1\", \"$\" : \"1\"},"
                                                   + "{\"@i\" : \"2\", \"$\" : \"2\"},"
                                                   + "{\"@i\" : \"3\", \"$\" : \"3\"},"
                                                   + "{\"@i\" : \"4\", \"$\" : \"4\"}"
                                                   + "] } } }");
        String actual = writer.toString();
        log.debug("writeMultiObject:\n" + actual);
        final JSONObject result = new JSONObject(actual);

        JSONAssert.assertEquals(expected, result, true);
    }

    @Test
    public void writeRootArray() throws Exception
    {
        final JsonOutputter testSubject = new JsonOutputter();
        testSubject.getListElementNames().add("items");
        testSubject.getStringElementNames().add("item");
        
        final Element itemsElement = new Element("items");

        // Array of five items.
        for (int i = 0; i < 5; i++)
        {
            final Element itemElement = new Element("item");
            itemElement.addContent(new Text(Integer.toString(i)));

            itemsElement.addContent(itemElement);
        }

        final Document document = new Document();

        document.setRootElement(itemsElement);

        final Writer writer = new StringWriter();

        testSubject.output(document, writer);

        final JSONObject expected = new JSONObject("{\"items\" : {"
                                                   + "\"$\" : ["
                                                   + "{\"$\" : \"0\"},"
                                                   + "{\"$\" : \"1\"},"
                                                   + "{\"$\" : \"2\"},"
                                                   + "{\"$\" : \"3\"},"
                                                   + "{\"$\" : \"4\"}"
                                                   + "] } }");
        String actual = writer.toString();
        log.debug("writeRootArray:\n" + actual);
        final JSONObject result = new JSONObject(actual);

        JSONAssert.assertEquals(expected, result, true);
    }

    @Test
    public void writeNamespacePrefix() throws Exception
    {
        final JsonOutputter testSubject = new JsonOutputter();
        testSubject.getListElementNames().add("items");
        testSubject.getStringElementNames().add("item");
        
        // no prefix
        Namespace ns = Namespace.getNamespace("nsi", "http://ns.items.com");
        final Element itemsElement = new Element("items", ns);
        itemsElement.addNamespaceDeclaration(ns);

        // Array of five items.
        for (int i = 0; i < 5; i++)
        {
            final Element itemElement = new Element("item", ns);
            itemElement.addContent(new Text(Integer.toString(i)));
            itemsElement.addContent(itemElement);
        }

        final Document document = new Document();

        document.setRootElement(itemsElement);

        final Writer writer = new StringWriter();

        testSubject.output(document, writer);

        final JSONObject expected = new JSONObject("{\r\n"
                                                   + "\"nsi:items\" : {"
                                                   + "\"@xmlns:nsi\": \"http://ns.items.com\","
                                                   + "\"$\": ["
                                                   + "{\"$\" : \"0\"},"
                                                   + "{\"$\" : \"1\"},"
                                                   + "{\"$\" : \"2\"},"
                                                   + "{\"$\" : \"3\"},"
                                                   + "{\"$\" : \"4\"}"
                                                   + "] } }");
        String actual = writer.toString();
        log.debug("writeNamespacePrefix:\n" + actual);
        final JSONObject result = new JSONObject(actual);

        JSONAssert.assertEquals(expected, result, true);
    }
    
    @Test
    public void writeNamespaceNoPrefix() throws Exception
    {
        final JsonOutputter testSubject = new JsonOutputter();
        testSubject.getListElementNames().add("items");
        testSubject.getStringElementNames().add("item");
        
        // no prefix
        Namespace ns = Namespace.getNamespace("http://ns.items.com");
        final Element itemsElement = new Element("items", ns);
        itemsElement.addNamespaceDeclaration(ns);

        // Array of five items.
        for (int i = 0; i < 5; i++)
        {
            final Element itemElement = new Element("item", ns);
            itemElement.addContent(new Text(Integer.toString(i)));
            itemsElement.addContent(itemElement);
        }

        final Document document = new Document();

        document.setRootElement(itemsElement);

        final Writer writer = new StringWriter();

        testSubject.output(document, writer);

        final JSONObject expected = new JSONObject("{\r\n"
                                                   + "\"items\" : {"
                                                   + "\"@xmlns\": \"http://ns.items.com\","
                                                   + "\"$\": ["
                                                   + "{\"$\" : \"0\"},"
                                                   + "{\"$\" : \"1\"},"
                                                   + "{\"$\" : \"2\"},"
                                                   + "{\"$\" : \"3\"},"
                                                   + "{\"$\" : \"4\"}"
                                                   + "] } }");
        String actual = writer.toString();
        log.debug("writeNamespaceNoPrefix:\n" + actual);
        final JSONObject result = new JSONObject(actual);

        JSONAssert.assertEquals(expected, result, true);
    }
}
