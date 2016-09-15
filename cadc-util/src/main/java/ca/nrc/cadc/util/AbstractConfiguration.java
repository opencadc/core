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
 * @author adriand
 * 
 * @version $Revision$
 * 
 * 
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */

package ca.nrc.cadc.util;

import java.io.File;


/**
 * <p>
 * Interface that has to be implemented by the configuration classes to 
 * automatically initialize from a configuraton file.
 * </p><p>
 * The implementation depends on reflection to fill the class variables
 * for an instance, which leads to the following pre-conditions for each
 * class variable that implements this interface:
 * 1. The method set&lt;ClassVariableName&gt;() must be publicly defined.
 *    This method may be declared static.
 * 2. The constant &lt;ClassVariableName&gt;_CONFIG_LOOKUP_VALUE must be
 *    defined - this is equivalent to the key for an entry, the key for
 *    a list or a map, and the name for a struct.  The access modifiers for this
 *    constant must include public and static.  If the
 *    set&lt;ClassVariableName&gt; method is defined, but the associated constant
 *    is not, ConfigurationReader will not invoke the
 *    set&lt;ClassVariableName&gt; method.
 * 3. Class variables may be one of the primitive types int, double,
 *    boolean, or the class String.  They may also be an array of int,
 *    double, boolean or String.  A class variable may also resolve to
 *    another class (one level of nesting only).  A class variable may
 *    be declared static.
 * 4. If a class variable resolves to another class, the nullary
 *    constructor for the resolved class must exist and it must be
 *    declared with public access.
 * 5. Define a config label that is returned by the &lt;tt&gt; getConfigLabel&lt;/tt&gt;
 *    method and corresponds to a subset (if branch) of the configuration
 *    information of the configuration file.
 * </p><p>
 * 
 * <p>
 * The DTD (if there was one, which there isn't) for the configuration file 
 * would look like this:
 * <!-- xml well-formedness preamble goes here -->
 * <!-- configuration is the root element, and may consist of one or more -->
 * <!-- entry, struct, list, map, and if elements -->
 * &lt;ELEMENT configuration (entry* | struct* | list* | map* | if*)&gt;
 * <!-- An entry is a key value pair -->
 * &lt;ELEMENT entry&gt;
 * &lt;ATTLIST entry
 *      key CDATA #REQUIRED
 *      value CDATA #REQUIRED&gt;
 * <!-- A struct is some list of entry or list elements.  They are differentiated by their name. -->
 * &lt;ELEMENT struct (entry*, list*) &gt;
 * &lt;ATTLIST struct
 *      name CDATA #REQUIRED&gt;
 * <!-- A list is a single key, with many values.-->
 * &lt;ELEMENT list (value*)&gt;
 * &lt;ATTLIST list
 *      key CDATA #REQUIRED&gt;
 * <!-- A map is a list of keywords, values.-->
 * &lt;ELEMENT map (entry*, value*) &gt;
 * &lt;ATTLIST map
 * 		key CDATA #REQUIRED
 * 		value CDTA #REQUIRED&gt;
 * <!-- An if statement is used to choose some set of entry, struct, list or map elements -->
 * <!-- as the source of configuration information. -->
 * &lt;ELEMENT if (entry* | struct* | list* | map* &gt;
 * &lt;ATTLIST if
 *     name CDATA #REQUIRED&gt;
 * <p>
 * If the values given for key and name attributes are not unique, the last occurrence
 * in the XML file will be used as the configuration value.
 * <p>
 * Usage example:
 *   Configuration c = new Configuration();
 *   c.update("aConfigRoot");
 *   c.update("anotherConfigRootName");
 * where Configuration is a subclass of AbstractConfiguration class that meets 
 * the pre-conditions mentioned above.
 *
 * 
 */

abstract public class AbstractConfiguration
{

	/**
	 * Represents the name of the configuration label to be used. A 
	 * configuration lable (represented by an if statement in the config file)
	 * is a mechanism to group configuration information that applies to a 
	 * specific case. If multiple applications share a common configuration file
	 * they can use configuration lables to distinguish between their specific
	 * configuration fields by overriding this method. By default, 
	 * getConfigLabel returns null (no configuration label) 
	 * @return name of the configuration label
	 */
	public String getConfigLabel() {return null;}


	/**
	 * Verifies whether the configuration information is valid. This is the
	 * place to verify whether all the configuration information was sufficient
	 * and correct in the config file.
	 * @return true if configuration is valid, false otherwise
	 */
	public abstract boolean isValid();

	/**
	 * Method called to update the content of an AbstractConfiguration object
	 * from a specified configuration root.
	 * @param cfgRootName configuration root name (eg: libnc, libmddb etc.)
	 */
	public void update(String cfgRootName)
	{
		XmlConfigReader reader = new XmlConfigReader();
		reader.obtainConfig(cfgRootName, this);
	}

	/**
	 * Method called to update the content of an AbstractConfiguration object
	 * from a specified absolute configuration file.
	 * @param cfgFile configuration absolute file name (eg: /etc/config/mycfg.config)
	 */
	public void updateAbsolute(File cfgFile)
	{
		XmlConfigReader reader = new XmlConfigReader();
		reader.obtainConfigAbsolute(cfgFile, this);
	}

}
