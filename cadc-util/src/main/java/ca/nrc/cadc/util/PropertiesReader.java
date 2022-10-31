/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2019.                            (c) 2019.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;


/**
 * This class enables the reading of configuration properties from a file
 * using the MultiValuedProperties utility
 *
 * <p>This class will pick up any changes that are made to the file automatically.
 *
 * <p>If the configuration file becomes unreadable, it will use the properties
 * of the last time the file was successfully read.
 *
 * @author majorb
 * @see ca.nrc.cadc.util.MultiValuedProperties
 */
public class PropertiesReader {

    private static final Logger log = Logger.getLogger(PropertiesReader.class);

    private static final String DEFAULT_CONFIG_DIR = System.getProperty("user.home") + "/config";
    public static final String CONFIG_DIR_SYSTEM_PROPERTY = PropertiesReader.class.getName() + ".dir";

    // Make final to ensure it is only set once and not later to null.
    private final File propertiesFile;
    private String cachedPropsKey;

    // Holder for the last known readable set of properties
    private static Map<String, MultiValuedProperties> cachedProperties = new ConcurrentHashMap<>();

    /**
     * Constructor.
     *
     * <p>The properties file, specified by 'fileName' will be read from one of two places:
     *
     * <p>1) If the system property ca.nrc.cadc.util.PropertiesReader.dir is set, it will use the
     * value of this property as the directory.
     * 2) Otherwise, the file will be read from ${user.home}/config/
     *
     * @param filename The file in which to read.
     * @throws InvalidConfigException If the properties file cannot be found or cannot be read.
     */
    public PropertiesReader(String filename) throws InvalidConfigException {
        if (filename == null) {
            throw new IllegalArgumentException("fileName cannot be null.");
        }

        String configDir = DEFAULT_CONFIG_DIR;
        if (System.getProperty(CONFIG_DIR_SYSTEM_PROPERTY) != null) {
            configDir = System.getProperty(CONFIG_DIR_SYSTEM_PROPERTY);
        }

        propertiesFile = new File(new File(configDir), filename);
        cachedPropsKey = propertiesFile.getAbsolutePath();
        log.debug("properties file: " + propertiesFile);
    }

    /**
     * Obtain whether the file associated with this Properties Reader can be accessed and read from.
     * @return  True if the provided file exists in the constrained locations and can be read.  False otherwise.
     */
    public boolean canRead() {
        return Files.isReadable(this.propertiesFile.toPath());
    }

    /**
     * Get all the properties (possibly returning a previously cached version if the
     * file is now missing.
     *
     * @return MultiValuedProperties or null if properties file cannot be read
     */
    public MultiValuedProperties getAllProperties() {
        
        MultiValuedProperties properties = null;
        try {
            if (canRead()) {
                InputStream in = new FileInputStream(propertiesFile);
                properties = new MultiValuedProperties();
                properties.load(in);
            }
        } catch (IOException ex) {
            log.warn("failed to read " + cachedPropsKey + ": " + ex);
            
        }

        if (properties == null) {
            properties = cachedProperties.get(cachedPropsKey);
            if (properties == null) {
                log.warn("No cached resource available at " + cachedPropsKey);
            }
        } else {
            cachedProperties.put(cachedPropsKey, properties);
        }

        return properties;
    }

    /**
     * Given the key, return the values of the property.
     *
     * @param key The key to lookup.
     * @return The property values or null if it is not set or is missing.
     * @deprecated use getAllProperties
     */
    @Deprecated
    public List<String> getPropertyValues(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Provided key is null.");
        }

        List<String> ret = null;
        MultiValuedProperties properties = getAllProperties();
        if (properties != null) {
            ret = properties.getProperty(key);
        }

        return ret;
    }

    /**
     * Given the key, return the first value of the property.
     *
     * @param key The key to lookup.
     * @return The first property value or null if it is not set or is missing.
     * @deprecated use getAllProperties
     */
    @Deprecated
    public String getFirstPropertyValue(String key) {
        List<String> values = getPropertyValues(key);
        if (values != null && values.size() > 0) {
            return values.get(0);
        }

        return null;
    }

}
