/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2003.                            (c) 2003.
 * National Research Council            Conseil national de recherches
 * Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 * All rights reserved                  Tous droits reserves
 *
 * NRC disclaims any warranties         Le CNRC denie toute garantie
 * expressed, implied, or statu-        enoncee, implicite ou legale,
 * tory, of any kind with respect       de quelque nature que se soit,
 * to the software, including           concernant le logiciel, y com-
 * without limitation any war-          pris sans restriction toute
 * ranty of merchantability or          garantie de valeur marchande
 * fitness for a particular pur-        ou de pertinence pour un usage
 * pose.  NRC shall not be liable       particulier.  Le CNRC ne
 * in any event for any damages,        pourra en aucun cas etre tenu
 * whether direct or indirect,          responsable de tout dommage,
 * special or general, consequen-       direct ou indirect, particul-
 * tial or incidental, arising          ier ou general, accessoire ou
 * from the use of the software.        fortuit, resultant de l'utili-
 *                                      sation du logiciel.
 *
 *
 * @author goliaths
 *
 * @version $Revision$
 *
 *
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */

package ca.nrc.cadc.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * Reads configuration information from an XML file into an 
 * AbstractConfiguration object. Clients are not supposed to use this class
 * directly. See the doc for AbstractConfiguration on how this mechanism is
 * intended to work.
 * @author goliaths
  *
 */
public class XmlConfigReader
{
    private static Logger logger = Logger.getLogger(XmlConfigReader.class);

    // constants used for reflection into configuration classes

    // prefix that denotes methods of interest to this class
    //
    private static final String SET_PREFIX = "set";
    private static final int SET_PREFIX_LENGTH = 3;

    // suffixes that denote constants from configuration classes to
    // assist in looking up information
    //
    private static final String LOOKUP_VALUE = "_CONFIG_LOOKUP_VALUE";
    private static final String DEFAULT_VALUE = "_DEFAULT_VALUE";

    // what parameters for configuration class set methods are
    // expected to look like
    //
    private static final Map mapParameter = new TreeMap();
    private static final String stringParameter = "";
    private static final Integer intParameter = new Integer(0);
    private static final Double doubleParameter = new Double(0.0);
    private static final Boolean booleanParameter = new Boolean(false);
    private static final String[] stringArrayParameter = new String[0];
    private static final int[] intArrayParameter = new int[0];
    private static final boolean[] booleanArrayParameter = new boolean[0];
    private static final double[] doubleArrayParameter = new double[0];

    // XML configuration file element, attribute, and tag names
    //
    private static final String CONFIGURATION_NAME = "configuration";
    private static final String ENTRY_NAME = "entry";
    private static final String IF_NAME = "if";
    private static final String KEY_NAME = "key";
    private static final String LIST_NAME = "list";
    private static final String MAP_NAME = "map";
    private static final String NAME_NAME = "name";
    private static final String STRUCT_NAME = "struct";
    private static final String VALUE_NAME = "value";

    // default and property values for obtaining configuration information from a file

    // system properties used, as well as their defaults if un-defined
    //
    public static final String CONFIG_DIR_PROPERTY = "ca.nrc.cadc.configDir";
    // expected extension on configuration files
    //
    private static final String CONFIG_FILE_SUFFIX = ".config";

    // where the information initially obtained from the configuration file
    // is maintained, before it is filled into the configuration class
    //
    // An "entry" XML element is an entry in the map.
    //
    // A "list" XML element is a vector, whose name is the key for an
    // entry in this map.
    //
    // A "struct" XML element is a map of entry elements, whose name is
    // the key for an entry in this map.
    //
    private Map m;

    // which "if" XML elements to process in the configuration file
    //
    private String ifElementName;

    /**
     * Simple constructor
     */
    public XmlConfigReader()
    {
        m = new TreeMap();
    }

    /**
     *  Looks for a configuration file named &lt;cfgRootName&gt;.config in the
     * the directory specified by the system property CONFIG_DIR_PROPERTY. The 
     * method throws a IllegalArgumentException if the specified file cannot
     * be accessed.
     *
     * @param cfgRootName used as part of system properties identifiers,
     *    and as the base name for the configuration file itself
     * @return absolute filename of the configuration file
     */
    public static String getConfigFileName(String cfgRootName)
    {
        String configDir = System.getProperty(CONFIG_DIR_PROPERTY);

        String fileName = cfgRootName + ".config";

        File f = new File(configDir, fileName);

        validateFile(f);

        return f.getAbsolutePath();

    } // end determineConfigFileName

    private static void validateFile(File f)
    {
        if (f == null)
        {
            throw new IllegalArgumentException("File argument is null");
        }
        else if (!f.exists())
        {
            String errorMsg = "Could not find " + f.getAbsolutePath();
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        else if (!f.canRead())
        {
            String errorMsg = "Could not read " + f.getAbsolutePath();
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * Helper method to obtain the text associated with an XML element.
     *
     * @param el the node to retrieve the content from
     * @return the content of a text node element
     */
    private String extractContent(Element el)
    {
        el.normalize();
        Node child = el.getFirstChild();
        if (child != null && child.getNodeType() == Node.TEXT_NODE)
        {
            Text text = (Text) child;
            return text.getData();
        }
        else
        {
            return null;
        }
    } // end extractContent

    /**
     * Fills the object representation with the configuration information.
     *
     * This method looks for all methods named set<something> in the
     * class definition of o, where each set method updates the value of a class
     * variable.  For each set<something> method, this method looks for a
     * corresponding value in the internal representation, and applies the
     * set method with the value.
     *
     * @param o where configuration information ends up, as an object
     *                  representation
     * @param m internal representation of what was obtained from the XML
     *                   configuration file.
     */
    private void fill(Object o, Map map)
    {

        // use reflection to get the list of set* methods

        Class c = o.getClass();
        Method[] theMethods = c.getMethods();

        for (int ii = 0; ii < theMethods.length; ii++)
        {
            String name = theMethods[ii].getName();
            if (name.startsWith(SET_PREFIX))
            {

                try
                {

                    // get a configuration value from internal storage.  Check types
                    // to ensure argument parameters are correct when invoking
                    // the set* methods.

                    // how to find the value in internal storage
                    //
                    Field lvField =
                        c.getField(
                            name.substring(SET_PREFIX_LENGTH) + LOOKUP_VALUE);

                    // representations of lookup value required for get
                    // operation
                    //
                    String lv = (String) lvField.get(null);
                    Object argValue = get(lv, map);
                    if (argValue == null)
                    {

                        // A null parameter implies the set method does not obtain
                        // information for its value from the configuration file.
                        // Do not execute set method with null parameter,

                        continue;
                    }

                    // what the parameters to the set method are supposed to be
                    // typed as
                    //
                    Class[] paramTypes = theMethods[ii].getParameterTypes();

                    // storage for the actual parameters to the set method
                    //
                    Object argList[] = new Object[1];

                    // Retrieve the string representation of the configuration value,
                    // and set it to the expected type for argList
                    //
                    if (paramTypes[0].isInstance(stringParameter))
                    {
                        argList[0] = argValue;
                    }
                    else if (paramTypes[0].isInstance(doubleParameter))
                    {
                        argList[0] = Double.valueOf((String) argValue);
                    }
                    else if (paramTypes[0].isInstance(booleanParameter))
                    {
                        argList[0] = Boolean.valueOf((String) argValue);
                    }
                    else if (paramTypes[0].isInstance(intParameter))
                    {
                        argList[0] = Integer.valueOf((String) argValue);
                    }
                    else if (paramTypes[0].isInstance(mapParameter))
                    {
                        argList[0] = (Map) argValue;
                    }
                    else if (paramTypes[0].isInstance(stringArrayParameter))
                    {

                        Vector vv = ((Vector) argValue);
                        int vSize = vv.size();
                        String[] sa = new String[vSize];
                        for (int jj = 0; jj < vSize; jj++)
                        {
                            sa[jj] = (String) vv.get(jj);
                        }
                        argList[0] = sa;

                    }
                    else if (paramTypes[0].isInstance(intArrayParameter))
                    {
                        Vector vv = ((Vector) argValue);
                        int vSize = vv.size();
                        int[] ia = new int[vSize];
                        for (int jj = 0; jj < vSize; jj++)
                        {
                            ia[jj] = Integer.parseInt((String) vv.get(jj));
                        }
                        argList[0] = ia;

                    }
                    else if (paramTypes[0].isInstance(booleanArrayParameter))
                    {

                        Vector vv = ((Vector) argValue);
                        int vSize = vv.size();
                        boolean[] ba = new boolean[vSize];
                        for (int jj = 0; jj < vSize; jj++)
                        {
                            ba[jj] =
                                Boolean
                                    .valueOf((String) vv.get(jj))
                                    .booleanValue();
                        }

                        argList[0] = ba;

                    }
                    else if (paramTypes[0].isInstance(doubleArrayParameter))
                    {
                        Vector vv = ((Vector) argValue);
                        int vSize = vv.size();
                        double[] da = new double[vSize];
                        for (int jj = 0; jj < vSize; jj++)
                        {
                            da[jj] =
                                Double
                                    .valueOf((String) vv.get(jj))
                                    .doubleValue();
                        }

                        argList[0] = da;

                    }
                    else // assume setting the contents of a member class
                        {

                        try
                        {

                            if (paramTypes[0].isArray())
                            {

                                // have an array parameter, so build up an
                                // array of objects

                                Vector vv = (Vector) argValue;
                                ArrayList l = new ArrayList();
                                Iterator vvIt = vv.iterator();
                                while (vvIt.hasNext())
                                {
                                    Map listItemMap = (Map) vvIt.next();
                                    argList[0] =
                                        paramTypes[0]
                                            .getComponentType()
                                            .newInstance();
                                    fill(argList[0], listItemMap);
                                    l.add(argList[0]);
                                }

                                argList[0] =
                                    Array.newInstance(
                                        paramTypes[0].getComponentType(),
                                        vv.size());
                                System.arraycopy(
                                    l.toArray(),
                                    0,
                                    argList[0],
                                    0,
                                    vv.size());
                            }
                            else
                            {

                                // not an array

                                // obtain storage for the class variable of type class to be
                                // filled
                                //
                                argList[0] = paramTypes[0].newInstance();

                                // obtain the bit of the DOM tree which holds the information
                                // to fill the class variable of type class
                                //
                                Field fillField =
                                    c.getField(
                                        name.substring(SET_PREFIX_LENGTH)
                                            + LOOKUP_VALUE);
                                String fillName = (String) fillField.get(null);
                                Map fillM = (Map) map.get(fillName);

                                // fill the class variable of type class
                                //
                                fill(argList[0], fillM);

                            }

                        }
                        catch (InstantiationException e)
                        {
                            logger.error("Instantiation error", e);
                            continue;
                        }

                    } // end if parameter types

                    // execute a set method on the configuration class
                    //
                    theMethods[ii].invoke(o, argList);

                }
                catch (NoSuchFieldException e)
                {
                    logger.info(
                        "Looked for and did not find field "
                            + name.substring(SET_PREFIX_LENGTH)
                            + LOOKUP_VALUE
                            + " in "
                            + c);
                }
                catch (IllegalAccessException e)
                {
                    logger.error(
                        "Error invoking "
                            + name
                            + " with parameter "
                            + (theMethods[ii].getParameterTypes())[0].getClass(),
                        e);
                }
                catch (InvocationTargetException e)
                {
                    logger.error("Error invoking " + name, e);
                }

            } // end if SET_PREFIX

        } // end for ii

    } // end fill

    /**
     * Retrieve a value from local storage, based on its key.
     *
     * @param thisItem String used as lookup key for an item in local storage
     * @param m Map used for local storage
     * @return Object which is found in local storage
     */
    private Object get(String thisItem, Map map)
    {
        Object result;
        if (map == null)
        {
            result = null;
        }
        else
        {
            result = map.get(thisItem);
        }
        return result;

    } // end get

    /**
     * Uses the DOM interface for XML parsing to store the configuration
     * information from an XML file into a local object.
     *
     * @param xmlFile absolute file name of configuration file
     */
    private void importConfiguration(String xmlFile)
    {

        DocumentBuilderFactory dcFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = null;
        try
        {
            parser = dcFactory.newDocumentBuilder();
        }
        catch (ParserConfigurationException e)
        {
            logger.error(
                "Configuration error while parsing configuration file: "
                    + xmlFile,
                e);
            return;
        }

        // set the error handler for parsing, to allow use of SystemLogger
        // for messaging.
        //
        DOMTreeErrorReporter handler = new DOMTreeErrorReporter();
        parser.setErrorHandler(handler);

        logger.info("Parsing configuration file " + xmlFile);
        Document document = null;
        try
        {
            document = parser.parse(xmlFile);
        }
        catch (SAXException e)
        {
            logger.error(
                "XML error while parsing configuration file: " + xmlFile,
                e);
            return;
        }
        catch (IOException e1)
        {
            logger.error(
                "IO error while parsing configuration file: " + xmlFile,
                e1);
            return;
        }

        walkDocument(document);

    } // end importConfiguration

    /**
     * Transforms the configuration information that is available from an XML file
     * into an object representation.
     *
     * @param cfgRootName used to find the XML file containing configuration
     *                                    information.
     * @param cfg object representation where configuration information ends up
     */
    protected void obtainConfig(String cfgRootName, AbstractConfiguration cfg)
    {
        ifElementName = cfg.getConfigLabel();
        importConfiguration(getConfigFileName(cfgRootName));
        fill(cfg, m);
        m.clear();

    } // end obtainConfig

    /**
     * @see #obtainConfig(String, AbstractConfiguration)
     *
     * @param cfgFile used to find the XML file containing configuration
     *					information (absolute path).
     * @param cfg object representation where configuration information ends up
     */
    protected void obtainConfigAbsolute(
        File cfgFile,
        AbstractConfiguration cfg)
    {
        ifElementName = cfg.getConfigLabel();
        validateFile(cfgFile);
        importConfiguration(cfgFile.getAbsolutePath());
        fill(cfg, m);
        m.clear();
    }

    /**
     * Traverse the configuration element of an XML configuration file,
     * looking for child elements of interest.
     *
     * @param element configuration node of the configuration file
     */
    private void walkConfiguration(Element element)
    {

        NodeList children = element.getChildNodes();
        for (int ii = 0; ii < children.getLength(); ii++)
        {

            Node node = children.item(ii);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element el = (Element) node;
                if (el.getTagName().equals(ENTRY_NAME))
                {
                    walkEntry(el, m);
                }
                else if (el.getTagName().equals(IF_NAME))
                {
                    walkIf(el);
                }
                else if (el.getTagName().equals(LIST_NAME))
                {
                    walkList(el, m);
                }
                else if (el.getTagName().equals(MAP_NAME))
                {
                    walkMap(el, m);
                }
                else if (el.getTagName().equals(STRUCT_NAME))
                {
                    walkStruct(el);
                }
            } // end if ELEMENT_NODE

        } // end for ii

    } // end walkConfiguration

    /**
     *  Traverse the root node of an XML configuration file.
     *
     * @param doc root node of the configuration file
     */
    private void walkDocument(Document doc)
    {
        Element el = doc.getDocumentElement();
        if (el.getTagName().equals(CONFIGURATION_NAME))
        {
            walkConfiguration(el);
        }
    } // end walkDocument

    /**
     * Traverse the entry element of an XML configuration file,
     * looking for the values for a key,value pair to be inserted
     * into local storage.
     *
     * @param el this entry node of the configuration file
     * @param m entries may also exist in structs, so provide the
     *                   appropriate local storage ptr
     *
     */
    private void walkEntry(Element el, Map map)
    {
        String key = new String(el.getAttribute(KEY_NAME));
        String value = new String(el.getAttribute(VALUE_NAME));

        map.put(key, value);

    } // end walkEntry

    /**
     * Traverse the if element of an XML configuration file.  If the
     * name attribute for the if element is the same as the
     * ifElementName provided in the constructor, look for
     * child nodes of interest.
     *
     * @param element if node in the configuration file
     */
    private void walkIf(Element element)
    {
        String ifName = element.getAttribute(NAME_NAME);
        if (ifName.equals(ifElementName))
        {
            NodeList children = element.getChildNodes();
            for (int ii = 0; ii < children.getLength(); ii++)
            {

                Node node = children.item(ii);
                if (node.getNodeType() == Node.ELEMENT_NODE)
                {
                    Element el = (Element) node;
                    if (el.getTagName().equals(ENTRY_NAME))
                    {
                        walkEntry(el, m);
                    }
                    else if (el.getTagName().equals(LIST_NAME))
                    {
                        walkList(el, m);
                    }
                    else if (el.getTagName().equals(MAP_NAME))
                    {
                        walkMap(el, m);
                    }
                    else if (el.getTagName().equals(STRUCT_NAME))
                    {
                        walkStruct(el);
                    } // end if

                } // end if ELEMENT_NODE

            } // end for ii

        } // end if elementName

    } // end walkIf

    /**
     * Traverse the list element of an XML configuration file, looking only for
     * value nodes.
     *
     * @param element list node in the configuration file
     */
    private void walkList(Element element, Map map)
    {
        NodeList children = element.getChildNodes();
        for (int ii = 0; ii < children.getLength(); ii++)
        {

            Node node = children.item(ii);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element el = (Element) node;
                if (el.getTagName().equals(VALUE_NAME))
                {
                    walkValue(element.getAttribute(KEY_NAME), el, map);
                }
                else if (el.getTagName().equals(STRUCT_NAME))
                {
                    walkStructInList(element.getAttribute(KEY_NAME), el, map);
                }

            } // end if ELEMENT_NODE

        } // end for ii

    } // end walkList

    /**
     * Traverse the map element of an XML configuration file, looking only for
     * value nodes.
     *
     * @param element list node in the configuration file
     */
    private void walkMap(Element element, Map map)
    {
        String mapName = element.getAttribute(KEY_NAME);
        Map mapElem = (Map) map.get(mapName);
        if (mapElem == null)
        {
            mapElem = new TreeMap();
        }
        NodeList children = element.getChildNodes();
        for (int ii = 0; ii < children.getLength(); ii++)
        {

            Node node = children.item(ii);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element el = (Element) node;
                if (el.getTagName().equals(ENTRY_NAME))
                {
                    walkEntry(el, mapElem);
                }
            }

        } // end for ii

        map.put(mapName, mapElem);

    } // end walkMap

    /**
     * Traverse the struct node of an XML configuration file, looking for
     * entry nodes.  This struct node must have been found in a List node,
     * because it identifies which level of Map nesting to store values in.
     *
     * @param element struct node in the configuration file
     */
    private void walkStructInList(String key, Element element, Map listMap)
    {

        String structName = element.getAttribute(NAME_NAME);

        // get object to add to list

        Map structMap = new TreeMap();
        NodeList children = element.getChildNodes();
        for (int ii = 0; ii < children.getLength(); ii++)
        {

            Node node = children.item(ii);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element el = (Element) node;
                if (el.getTagName().equals(ENTRY_NAME))
                {
                    walkEntry(el, structMap);
                }
                if (el.getTagName().equals(LIST_NAME))
                {
                    walkList(el, structMap);
                }

            }

        } // end for ii

        // add object to the list

        Vector list = (Vector) listMap.get(key);
        if (list == null)
        {
            list = new Vector();
        }
        list.add(structMap);
        listMap.put(key, list);

    } // end walkStructInList

    /**
     * Traverse the struct node of an XML configuration file, looking for
     * entry nodes.
     *
     * @param element struct node in the configuration file
     */
    private void walkStruct(Element element)
    {

        String structName = element.getAttribute(NAME_NAME);
        Map structMap = (Map) m.get(structName);
        if (structMap == null)
        {
            structMap = new TreeMap();
        }
        NodeList children = element.getChildNodes();
        for (int ii = 0; ii < children.getLength(); ii++)
        {

            Node node = children.item(ii);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                Element el = (Element) node;
                if (el.getTagName().equals(ENTRY_NAME))
                {
                    walkEntry(el, structMap);
                }
                if (el.getTagName().equals(LIST_NAME))
                {
                    walkList(el, structMap);
                }
                if (el.getTagName().equals(MAP_NAME))
                {
                    walkMap(el, structMap);
                }

            }

        } // end for ii

        m.put(structName, structMap);

    } // end walkStruct

    /**
     * Traverse the value element of an XML configuration file, adding
     * each value to the vector that is the local storage entry for a list
     *
     * @param key lookup id for local storage
     * @param el this value node of the configuration file
     */
    private void walkValue(String key, Element el, Map map)
    {

        String value = extractContent(el);
        Vector list = (Vector) map.get(key);
        if (list == null)
        {
            list = new Vector();
        }
        list.add(value.trim());

        map.put(key, list);

    } // end walkValue

} // end class XmlConfigReader
