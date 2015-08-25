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

package ca.nrc.cadc.xml;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * JsonInputter
 */
public class JsonInputter
{
    public JsonInputter() {}

    private final Map<String, String> listElementMap = new TreeMap<String, String>();

    public Map<String, String> getListElementMap()
    {
        return listElementMap;
    }

    public Document input(final String json)
        throws JSONException
    {
        JSONObject rootJson = new JSONObject(json);
        List<String> keys = Arrays.asList(JSONObject.getNames(rootJson));
        List<Namespace> namespaces = new ArrayList<Namespace>();
        Namespace namespace = getNamespace(namespaces, rootJson, keys);

        String rootKey = null;
        List<Attribute> attributes = new ArrayList<Attribute>();
        for (String key : keys)
        {
            if (!key.startsWith("@xmlns"))
            {
                if (key.startsWith("@"))
                {
                    String value;
                    if (rootJson.isNull(key))
                        value = "";
                    else
                        value = getStringValue(rootJson.get(key));
                    attributes.add(new Attribute(key.substring(1), value));
                }
                else
                {
                    // DOM can only have one root element.
                    if (rootKey != null)
                    {
                        throw new IllegalStateException("Found multiple root entries");
                    }
                    rootKey = key;
                }
            }
        }

        Element rootElement = new Element(rootKey, namespace);
        for (Attribute attribute : attributes)
        {
            rootElement.setAttribute(attribute);
        }

        Object value = rootJson.get(rootKey);
        processObject(rootKey, value, rootElement, namespace, namespaces);

        Document document = new Document();
        document.setRootElement(rootElement);
        return document;
    }

    private void processObject(String key, Object value, Element element,
                               Namespace namespace, List<Namespace> namespaces)
        throws JSONException
    {
        if (value == null)
            return;

        if (value instanceof JSONObject)
        {
            processJSONObject((JSONObject) value, element, namespaces);
        }
        else if (value instanceof JSONArray)
        {
            processJSONArray(key, (JSONArray) value, element, namespace, namespaces);
        }
        else
        {
            element.setText(getStringValue(value));
        }
    }

    private void processJSONObject(JSONObject jsonObject, Element element, List<Namespace> namespaces)
        throws JSONException
    {
        List<String> keys = Arrays.asList(JSONObject.getNames(jsonObject));
        Namespace namespace = getNamespace(namespaces, jsonObject, keys);
        if (namespace == null)
        {
            namespace = element.getNamespace();
        }

        for (String key : keys)
        {
            if (jsonObject.isNull(key))
            {
                continue;
            }

            // attribute
            if (key.startsWith("@"))
            {
                Object value = jsonObject.get(key);
                element.setAttribute(new Attribute(key.substring(1), getStringValue(value)));
                continue;
            }

            // text content
            Object value = jsonObject.get(key);
            if (key.equals("$"))
            {
                element.setText(getStringValue(value));
                continue;
            }

            Element child = new Element(key, namespace);
            if (value instanceof JSONObject)
            {
                processJSONObject((JSONObject) value, child, namespaces);
            }
            else if (value instanceof JSONArray)
            {
                processJSONArray(key, (JSONArray) value, child, namespace, namespaces);
            }
            element.addContent(child);
        }
    }

    private void processJSONArray(String key, JSONArray jsonArray, Element element,
                                  Namespace namespace, List<Namespace> namespaces)
        throws JSONException
    {
        String name = getListElementMap().get(key);

        for (int i = 0; i < jsonArray.length(); i++)
        {
            if (jsonArray.isNull(i))
                continue;

            Element child;
            if (name == null)
            {
                child = element;
            }
            else
            {
                child = new Element(name, namespace);
                element.addContent(child);
            }

            Object value = jsonArray.get(i);
            processObject(key, value, child, namespace, namespaces);
        }
    }

    private Namespace getNamespace(List<Namespace> namespaces,
                                   JSONObject jsonObject,
                                   List<String> keys)
        throws JSONException
    {
        for (String key : keys)
        {
            if (key.equals("@xmlns"))
            {
                if (jsonObject.isNull(key))
                    break;
                String uri = jsonObject.getString(key);
                Namespace namespace = Namespace.getNamespace(uri);
                if (!namespaces.contains(namespace))
                {
                    namespaces.add(namespace);
                }
                return namespace;
            }
        }
        return null;
    }

    private String getStringValue(Object value)
    {
        if (value instanceof String)
        {
            return (String) value;
        }
        else if (value instanceof Integer ||
                 value instanceof Double ||
                 value instanceof Long)
        {
            return ((Number) value).toString();
        }
        else if (value instanceof Boolean)
        {
            return ((Boolean) value).toString();
        }
        else
        {
            String error = "Unknown value " + value.getClass().getSimpleName();
            throw new IllegalArgumentException(error);
        }
    }

}
 