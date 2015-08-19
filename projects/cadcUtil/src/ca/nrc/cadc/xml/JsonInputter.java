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
 