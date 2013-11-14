/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2011.                            (c) 2011.
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
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */

package ca.nrc.cadc.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * This class enables the reading of configuration properties from a file
 * using the MultiValuedProperties utility
 *
 * This class will pick up any changes that are made to the file automatically.
 *
 * If the configuration file becomes unreadable, it will use the properties
 * of the last time the file was successfully read.
 *
 * @see ca.nrc.cadc.util.MultiValuedProperties
 *
 * @author majorb
 */
public class PropertiesReader
{

    private static final Logger log = Logger.getLogger(PropertiesReader.class);

    private URL config;

    // Holder for the last known readable set of properties
    private static MultiValuedProperties lastKnownGoodProperties = null;

    /**
     * Constructor..
     *
     * @param config URL pointing to the property configuration.
     */
    public PropertiesReader(URL config)
    {
        if (config == null)
            throw new IllegalArgumentException("Provided config is null.");
        this.config = config;
    }

    /**
     * Given the key, return the values of the property.
     *
     * @param key The key to lookup.
     * @return The property values or null if it is not set or is missing.
     * @throws IOException
     */
    public List<String> getPropertyValues(String key) throws IOException
    {
        if (key == null)
            throw new IllegalArgumentException("Provided key is null.");

        InputStream in = null;
        try
        {
            MultiValuedProperties properties = new MultiValuedProperties();
            in = config.openStream();
            properties.load(in);

            if (properties.keySet() == null)
            {
                if (lastKnownGoodProperties == null)
                {
                    log.error("No property resource available at: " + config);
                    return null;
                }
                log.warn("Properties missing at: " + config + ". Using earlier vesion.");
                properties = lastKnownGoodProperties;
            }
            else
            {
                lastKnownGoodProperties = properties;
            }

            return properties.getProperty(key);
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException e)
                {
                    log.warn("Failed to close input stream for: " + config);
                }
            }
        }
    }

    /**
     * Given the key, return the first value of the property.
     *
     * @param key The key to lookup.
     * @return The first property value or null if it is not set or is missing.
     * @throws IOException
     */
    public String getFirstPropertyValue(String key) throws IOException
    {
        List<String> values = getPropertyValues(key);
        if (values != null && values.size() > 0)
            return values.get(0);
        return null;
    }

}
