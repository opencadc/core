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

import ca.nrc.cadc.util.StringUtil;
import org.apache.log4j.Logger;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.List;


public class JsonOutputter implements Serializable
{
    private static final long serialVersionUID = 20150205121500L;

    private final Logger log = Logger.getLogger(getClass());

    private int indent = 0;


    /**
     * Set the format for the outputter. Default is compact format.
     *
     * @param format the format to set.
     */
    public void setFormat(Format format)
    {
        String indentString = format.getIndent();
        if (indentString != null)
        {
            indent = format.getIndent().length();
        }
    }

    /**
     * Converts a JDOM Document into a JSON string and writes the result into
     * the specified OutputStream.
     *
     * @param document the JDOM Document.
     * @param ostream  the OutputStream.
     * @throws IOException if one is thrown.
     */
    public void output(Document document, OutputStream ostream)
            throws IOException, JSONException
    {
        ostream.write(outputString(document).getBytes());
    }

    /**
     * Converts the JDOM Document into a JSON string and writes the result into
     * the specified Writer.
     *
     * @param document the JDOM Document.
     * @param writer   the Writer.
     * @throws IOException if one is thrown.
     */
    public void output(Document document, Writer writer) throws IOException,
                                                                JSONException
    {
        writer.write(outputString(document));
    }

    /**
     * Convenience method that accepts an XML string and returns a String
     * representing the converted JSON Object.
     *
     * @param xml the input XML string.
     * @return the String representation of the converted JSON object.
     * @throws IOException   if one is thrown.
     * @throws JDOMException if one is thrown.
     */
    public String outputString(String xml) throws IOException, JDOMException,
                                                  JSONException
    {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(new StringReader(xml));
        return outputString(doc);
    }


    /**
     * Converts the JDOM Document into a JSON String and returns it.
     *
     * @param document the JDOM Document.
     * @return the JSON String representing the JDOM Document.
     */
    public String outputString(Document document) throws JSONException
    {
        Element rootElement = document.getRootElement();
        JSONObject jsonObject = new JSONObject();
        JSONObject namespaceJsonObject = getNamespaceJsonObject(rootElement);
        processElement(rootElement, jsonObject, namespaceJsonObject);
        processChildren(rootElement, jsonObject, namespaceJsonObject);

        if (indent == 0)
        {
            return jsonObject.toString().replaceAll("/", "\\/");
        }
        else
        {
            return jsonObject.toString(indent).replaceAll("/", "\\/");
        }
    }

    /**
     * Process the children of the specified JDOM element. This method is recursive.
     * The children for the given element are found, and the method is called for
     * each child.
     *
     * @param element             the element whose children needs to be processed.
     * @param jsonObject          the reference to the JSON Object to update.
     * @param namespaceJsonObject the reference to the root Namespace JSON object.
     */
    private void processChildren(Element element, JSONObject jsonObject,
                                 JSONObject namespaceJsonObject)
            throws JSONException
    {
        List<Element> children = element.getChildren();
        JSONObject properties;
        if (jsonObject.has(getQName(element)))
        {
            properties = jsonObject.getJSONObject(getQName(element));
        }
        else
        {
            properties = new JSONObject();
        }
        for (Element child : children)
        {
            // Rule 1: Element names become object properties
            // Rule 9: Elements with namespace prefixes become object properties, too.
            JSONObject childJsonObject = new JSONObject();
            processElement(child, childJsonObject, namespaceJsonObject);
            processChildren(child, childJsonObject, namespaceJsonObject);

            if (childJsonObject.length() > 0)
            {
                properties.accumulate(getQName(child), childJsonObject
                        .getJSONObject(getQName(child)));
            }
        }
        if (properties.length() > 0)
        {
            jsonObject.put(getQName(element), properties);
        }
    }

    /**
     * Process the text content and attributes of a JDOM element into a JSON object.
     *
     * @param element             the element to parse.
     * @param jsonObject          the JSONObject to update with the element's properties.
     * @param namespaceJsonObject the reference to the root Namespace JSON object.
     */
    private void processElement(Element element, JSONObject jsonObject,
                                JSONObject namespaceJsonObject)
            throws JSONException
    {
        JSONObject properties = new JSONObject();
        // Rule 2: Text content of elements goes in the $ property of an object.
        if (StringUtil.hasLength(element.getTextTrim()))
        {
            properties.accumulate("$", element.getTextTrim());
        }
        // Rule 5: Attributes go in properties whose names begin with @.
        List<Attribute> attributes = element.getAttributes();
        for (Attribute attribute : attributes)
        {
            properties.accumulate("@" + attribute.getName(), attribute
                    .getValue());
        }

        if (namespaceJsonObject.length() > 0)
        {
            properties.accumulate("@xmlns", namespaceJsonObject);
        }

        if (properties.length() > 0)
        {
            jsonObject.accumulate(getQName(element), properties);
        }
    }

    /**
     * Return a JSON Object containing the default and additional namespace
     * properties of the Element.
     *
     * @param element the element whose namespace properties are to be extracted.
     * @return the JSON Object with the namespace properties.
     */
    private JSONObject getNamespaceJsonObject(Element element)
            throws JSONException
    {
        // Rule 6: Active namespaces for an element go in the element's @xmlns property.
        // Rule 7: The default namespace URI goes in @xmlns.$.
        JSONObject namespaceProps = new JSONObject();
        Namespace defaultNamespace = element.getNamespace();
        if (StringUtil.hasLength(defaultNamespace.getURI()))
        {
            namespaceProps.accumulate("$", defaultNamespace.getURI());
        }
        // Rule 8: Other namespaces go in other properties of @xmlns.
        List<Namespace> additionalNamespaces = element
                .getAdditionalNamespaces();
        for (Namespace additionalNamespace : additionalNamespaces)
        {
            if (StringUtil.hasLength(additionalNamespace.getURI()))
            {
                namespaceProps.accumulate(additionalNamespace
                                                  .getPrefix(), additionalNamespace
                                                  .getURI());
            }
        }
        return namespaceProps;
    }

    /**
     * Return the qualified name (namespace:elementname) of the element.
     *
     * @param element the element to set.
     * @return the element name qualified with its namespace.
     */
    private String getQName(Element element)
    {
        if (StringUtil.hasLength(element.getNamespacePrefix()))
        {
            return element.getNamespacePrefix() + ":" + element.getName();
        }
        else
        {
            return element.getName();
        }
    }
}
