package ca.nrc.cadc.util;/*
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
 * @version $Revision: $
 * 
 * 
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */

import ca.nrc.cadc.util.AbstractConfiguration;



/**
 * Set up to exercise the test cases where:
 *    1. the lookup value is defined, but a set method isn't
 *    2. the set method is defined, but a lookup value isn't 
 *  
 * @author goliaths
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ExpectationConfig extends AbstractConfiguration
{
	public String configLabel;
	public ExpectationConfig() {}
	
	/* (non-Javadoc)
	 * @see ca.nrc.cadc.util.AbstractConfiguration#getConfigLabel()
	 */
	public String getConfigLabel()
	{
		return configLabel;
	}

	/**
	 * Set the config label
	 * @param configLabel new config label
	 */
	public void setConfigLabel(String configLabel)
	{
		this.configLabel = configLabel;
	}
	
	public static final String MissingSetMethod_CONFIG_LOOKUP_VALUE = "Missing";
	
	public void setMissingLookupValue( String s )
	{
		missingLookupValue_ = s;
	}

	public String missingLookupValue_ = "default value";
	
	public boolean isValid()
	{
		return true;
	}
	
} // end class ExpectationConfig
