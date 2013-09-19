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

package ca.nrc.cadc.vosi;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderSAX2Factory;

/**
 * Parser to setup the schema map for parsing a VOSI-capabilities document.
 * 
 * @author pdowler
 */
public class CapabilitiesParser 
{
    private static final Logger log = Logger.getLogger(CapabilitiesParser.class);
    
    protected Map<String,String> schemaMap;
    
    public CapabilitiesParser()
    {
        this(true);
    }
    
    public CapabilitiesParser(boolean enableSchemaValidation)
    {
        if (enableSchemaValidation)
        {
            this.schemaMap = new HashMap<String,String>();
            String url;

            url = getResourceUrlString(VOSI.CAPABILITIES_SCHEMA, CapabilitiesParser.class);
            if (url != null)
            {
                log.debug(VOSI.CAPABILITIES_NS_URI + " -> " + url);
                schemaMap.put(VOSI.CAPABILITIES_NS_URI, url);
            }
            else
                log.warn("failed to find resource: " + VOSI.CAPABILITIES_SCHEMA);

            url = getResourceUrlString(VOSI.VORESOURCE_SCHEMA, CapabilitiesParser.class);
            if (url != null)
            {
                log.debug(VOSI.VORESOURCE_NS_URI + " -> " + url);
                schemaMap.put(VOSI.VORESOURCE_NS_URI, url);
            }
            else
                log.warn("failed to find resource: " + VOSI.VORESOURCE_SCHEMA);

            url = getResourceUrlString(VOSI.VODATASERVICE_SCHEMA, CapabilitiesParser.class);
            if (url != null)
            {
                log.debug(VOSI.VODATASERVICE_NS_URI + " -> " + url);
                schemaMap.put(VOSI.VODATASERVICE_NS_URI, url);
            }
            else
                log.warn("failed to find resource: " + VOSI.VODATASERVICE_SCHEMA);

            url = getResourceUrlString(VOSI.XSI_SCHEMA, CapabilitiesParser.class);
            if (url != null)
            {
                log.debug(VOSI.XSI_NS_URI + " -> " + url);
                schemaMap.put(VOSI.XSI_NS_URI, url);
            }
            else
                log.warn("failed to find resource: " + VOSI.XSI_SCHEMA);
        }
    }
    
    /**
     * Add an additional schema to the parser configuration. This is needed if the VOSI-capabilities
     * uses an extension schema for xsi:type.
     * 
     * @param namespace
     * @param schemaLocation 
     */
    public void addSchemaLocation(String namespace, String schemaLocation)
    {
        log.debug("addSchemaLocation: " + namespace + " -> " + schemaLocation);
        schemaMap.put(VOSI.XSI_NS_URI, schemaLocation);
    }
    
    public Document parse(Reader rdr)
        throws IOException, JDOMException
    {
        SAXBuilder sb = createBuilder(schemaMap);
        return sb.build(rdr);
    }

    public Document parse(InputStream istream)
        throws IOException, JDOMException
    {
        SAXBuilder sb = createBuilder(schemaMap);
        return sb.build(istream);
    }
    
    // copied from the jdom2 based XmlUtil in cadcUWS TODO: don't forget to use common lib
    private static String getResourceUrlString(String resourceFileName, Class runningClass)
    {
        URL url = runningClass.getClassLoader().getResource(resourceFileName);
        if (url == null)
            throw new MissingResourceException("Resource not found: " + resourceFileName, runningClass.getName(), resourceFileName);
        return url.toExternalForm();
    }
    
    public static final String PARSER = "org.apache.xerces.parsers.SAXParser";
    private static final String GRAMMAR_POOL = "org.apache.xerces.parsers.XMLGrammarCachingConfiguration";
    private SAXBuilder createBuilder(Map<String, String> schemaMap)
    {
        long start = System.currentTimeMillis();
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

        XMLReaderSAX2Factory factory = new XMLReaderSAX2Factory(schemaVal, PARSER);
        SAXBuilder builder = new SAXBuilder(factory);
        if (schemaVal)
        {
            builder.setFeature("http://xml.org/sax/features/validation", true);
            builder.setFeature("http://apache.org/xml/features/validation/schema", true);
            if (schemaMap.size() > 0)
            {
                builder.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation",
                    sbSchemaLocations.toString());
            }
        }
        long finish = System.currentTimeMillis();
        log.debug("SAXBuilder in " + (finish - start) + "ms");
        return builder;
    }
}
