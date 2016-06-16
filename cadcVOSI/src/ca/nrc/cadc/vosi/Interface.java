/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2010.                            (c) 2010.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;


/**
 * Minimal implementation of the Interface model in VOResource 1.0.
 * 
 * accessURL is the URL (or base URL) that a client uses to access the service.  
 * If multiple accessURL is provided, each URL should point to a functionally
 * identical or "mirror" installation of the same service and administered 
 * by the same publisher. Currently we only support one accessURL.
 * 
 * securityMethod is the mechanism the client must employ to gain secure 
 * access to the service.
 * 
 * 
 * @author yeunga
 */
public class Interface
{
    private static Logger log = Logger.getLogger(Interface.class);

    // Use List to preserve order
    private URI accessURL;
    private List<URI> securityMethods;
    
    // TODO: determine what we want to do with role and use, 
    //       for now hardcode it to "std"
    private String role = "std";

    /**
     * Constructor. 
     * @throws URISyntaxException 
     */
    public Interface(final URI accessURL) 
    		throws URISyntaxException
    {
    	validateAccessURL(accessURL);
    	
    	// TODO: check that each entry in a list is unique?
        this.accessURL = new URI(accessURL.toString());
        this.securityMethods = new ArrayList<URI>();
    }
    
    public URI getAccessURL() 
    {
		try 
		{
			return new URI(this.accessURL.toString());
		} 
		catch (URISyntaxException e) 
		{
			// Should not happen since it has been checked at construction time.
			throw new RuntimeException(e);
		}
	}


	public List<URI> getSecurityMethods() 
	{
		return this.securityMethods;
	}
	
	public boolean useSecurityMethod(final URI securityMethod)
	{
		for (URI sm : this.securityMethods)
		{
			if (sm.toString().equals(securityMethod.toString()))
			{
				return true;
			}
		}
		
		return false;
	}
    
    public Element toXmlElement(Namespace xsi, Namespace cap, Namespace vod)
    {
        Element eleInterface = new Element("interface");
        
    	// set Interface attributes
        Attribute attType = new Attribute("type", vod.getPrefix() + ":ParamHTTP", xsi);
        eleInterface.setAttribute(attType);            
        if (this.role != null)
        {
            eleInterface.setAttribute("role", this.role);
        }
 
        // accessURL element
        Element eleAccessURL = new Element("accessURL");
        eleInterface.addContent(eleAccessURL);            
        // TODO: determine if we can hardcode "use" to "full"
        eleAccessURL.setAttribute("use", "full");
        eleAccessURL.setText(this.accessURL.toString());
   
        // securityMethod elements
        for (URI securityMethod : this.securityMethods)
        {
        	Element eleSecurityMethod = new Element("securityMethod");
            Attribute attribute = new Attribute("standardID", securityMethod.toString());
            eleSecurityMethod.setAttribute(attribute);
        	eleInterface.addContent(eleSecurityMethod);
        }
        
        return eleInterface;
    }
	
	private void validateAccessURL(final URI accessURL)
	{
		if (accessURL == null)
		{
			String msg = "access URL for an Interface object cannot be null.";
			throw new IllegalArgumentException(msg);
		}
	}
}
