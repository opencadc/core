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

package ca.nrc.cadc.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import org.apache.log4j.Logger;
import org.jdom2.DefaultJDOMFactory;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.JDOMFactory;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.DefaultSAXHandlerFactory;
import org.jdom2.input.sax.SAXHandlerFactory;
import org.jdom2.input.sax.XMLReaderJDOMFactory;
import org.jdom2.input.sax.XMLReaderSAX2Factory;

/**
 * XmlUtil  class for use with JDOM-2.
 * @author pdowler
 *
 */
public class XmlUtil
{
    private static Logger log = Logger.getLogger(XmlUtil.class);
    public static final String PARSER = "org.apache.xerces.parsers.SAXParser";
    private static final String GRAMMAR_POOL = "org.apache.xerces.parsers.XMLGrammarCachingConfiguration";
    public static final Namespace XSI_NS = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

    /**
     * Deprecated convenience method.
     * 
     * @param reader
     * @return
     * @throws JDOMException
     * @throws IOException 
     * @deprecated 
     */
    public static Document validateXML(Reader reader)
        throws JDOMException, IOException
    {
        return buildDocument(reader, null);
    }
    
    /**
     * Deprecated convenience method.
     * 
     * @param reader
     * @param schemaMap
     * @return
     * @throws JDOMException
     * @throws IOException 
     * @deprecated 
     */
    public static Document validateXML(Reader reader, Map<String, String> schemaMap)
        throws JDOMException, IOException
    {
        return buildDocument(reader, schemaMap);
    }
    
    /**
     * Convenience: build an XML Document from string without schema validation.
     * 
     * @param xml
     * @return document
     * @throws IOException 
     * @throws JDOMException 
     */
    public static Document buildDocument(String xml) 
        throws JDOMException, IOException
    {
        return buildDocument(new StringReader(xml));
    }
    
    /**
     * Convenience: build an XML document with schema validation against a single
     * schema.
     * 
     * @param xml
     * @param schemaNamespace
     * @param schemaResourceFileName
     * @return document
     * @throws IOException
     * @throws JDOMException 
     */
    public static Document buildDocument(String xml, String schemaNamespace, String schemaResourceFileName)
        throws IOException, JDOMException
    {
        if (schemaNamespace == null || schemaResourceFileName == null)
            throw new IllegalArgumentException("schemaNamespace and schemaResourceFileName cannot be null");
            
        Map<String, String> map = new HashMap<String, String>();
        map.put(schemaNamespace, getResourceUrlString(schemaResourceFileName, XmlUtil.class));
        return buildDocument(new StringReader(xml), map);
    }

    /**
     * Convenience: build an XML Document without schema validation.
     * 
     * @param istream
     * @return document
     * @throws IOException 
     * @throws JDOMException 
     */
    public static Document buildDocument(InputStream istream) 
        throws JDOMException, IOException
    {
        return buildDocument(new InputStreamReader(istream), null);
    }
    
    /**
     * Convenience: build an XML Document without schema validation.
     * 
     * @param reader
     * @return document
     * @throws IOException 
     * @throws JDOMException 
     */
    public static Document buildDocument(Reader reader) 
        throws JDOMException, IOException
    {
        return buildDocument(reader, null);
    }

    /**
     * Convenience: build an XML Document without schema validation.
     * 
     * @param istream
     * @param schemaMap
     * @return document
     * @throws IOException 
     * @throws JDOMException 
     */
    public static Document buildDocument(InputStream istream, Map<String, String> schemaMap) 
        throws JDOMException, IOException
    {
        return buildDocument(new InputStreamReader(istream), schemaMap);
    }
    
    /**
     * Build an XML document with schema validation. The schemaMap argument contains 
     * pairs of namespace:location (for each required schema). The normal practice in
     * OpenCADC libraries is to store schema files inside the jar files of the code
     * that calls this utility and to use the getResourceUrlString method to find 
     * the URL at runtime.
     * 
     * @param reader
     * @param schemaMap namespace:location map, null for no validation
     * @return document
     * @throws IOException
     * @throws JDOMException 
     */
    public static Document buildDocument(Reader reader, Map<String, String> schemaMap)
        throws IOException, JDOMException
    {
        SAXBuilder sb = createBuilder(schemaMap);
        return sb.build(reader);
    }

    /**
     * Create an XML Document builder using a SAX parser.
     * 
     * @param schemaMap
     * @return document
     */
    public static SAXBuilder createBuilder(Map<String, String> schemaMap)
    {
        boolean validate = (schemaMap != null && !schemaMap.isEmpty());
        XMLReaderJDOMFactory rf = new XMLReaderSAX2Factory(validate, PARSER);
        SAXHandlerFactory sh = new DefaultSAXHandlerFactory();
        JDOMFactory jf = new DefaultJDOMFactory();
        
        boolean schemaVal = (schemaMap != null);
        String schemaResource;
        String space = " ";
        StringBuilder sbSchemaLocations = new StringBuilder();
        if (schemaVal) 
        {
            log.debug("schemaMap.size(): " + schemaMap.size());
            for (String schemaNSKey : schemaMap.keySet())
            {
                schemaResource = (String) schemaMap.get(schemaNSKey);
                sbSchemaLocations.append(schemaNSKey).append(space).append(schemaResource).append(space);
            }
            // enable xerces grammar caching
            System.setProperty("org.apache.xerces.xni.parser.XMLParserConfiguration", GRAMMAR_POOL);
        }

        SAXBuilder builder = new SAXBuilder(rf, sh, jf);
        if (schemaVal)
        {
            builder.setFeature("http://xml.org/sax/features/validation", true);
            builder.setFeature("http://apache.org/xml/features/validation/schema", true);
            if (schemaMap.size() > 0)
                builder.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation",
                    sbSchemaLocations.toString());
        }
        return builder;
    }

    /**
     * Get an URL to a schema file. This implementation finds the schema file using the ClassLoader
     * that loaded the argument class.
     * 
     * @param resourceFileName
     * @param runningClass 
     * @return
     */
    public static String getResourceUrlString(String resourceFileName, Class runningClass)
    {
        String rtn = null;
        URL url = runningClass.getClassLoader().getResource(resourceFileName);
        if (url == null)
            throw new MissingResourceException("Resource not found: " + resourceFileName, runningClass.getName(), resourceFileName);
        rtn = url.toString();
        return rtn;
    }
}
