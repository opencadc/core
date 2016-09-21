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
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */

package ca.nrc.cadc.util;

import org.apache.log4j.Logger;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Provide error handling for XML parsing work in ConfigurationReader.
 * 
 * @author goliaths
 *
 */
public class DOMTreeErrorReporter extends DefaultHandler
{
	private static Logger logger = Logger.getLogger(DOMTreeErrorReporter.class);

	/**
	 * 
	 */
	public DOMTreeErrorReporter()
	{
		sawErrors = false;
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
	 */
	public void warning(SAXParseException toCatch)
	{
		// EMPTY - ingore all warnings
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
	 */
	public void error(SAXParseException toCatch)
	{
		sawErrors = true;
		logger.error(
			"Error occurred at line number: "
				+ toCatch.getLineNumber()
				+ ", column number: "
				+ toCatch.getColumnNumber(),
			toCatch);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
	 */
	public void fatalError(SAXParseException toCatch)
	{
		sawErrors = true;
		logger.error(
			"Error occurred at line number: "
				+ toCatch.getLineNumber()
				+ ", column number: "
				+ toCatch.getColumnNumber(),
			toCatch);
	}

	/**
	 * Accessor method for sawErrors
	 */
	public void resetErrors()
	{
		sawErrors = false;
	}

	/**
	 * Accessor method for sawErrors.
	 * 
	 * @return boolean indication of whether the parser saw errors
	 */
	public boolean getSawErrors()
	{
		return sawErrors;
	}

	// save state about parser success
	//	
	private boolean sawErrors;

} // end DOMTreeErrorReporter
