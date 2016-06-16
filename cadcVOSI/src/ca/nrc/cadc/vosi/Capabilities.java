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
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;


/**
 * Minimal implementation of the Capabilities model in VOResource 1.0.
 * 
 * resourceIdentifier is a base URI which identifies a service provided by 
 * the managed authority, e.g. ivoa://cadc.nrc.ca/vospacev2.1. 
 * 
 * capability represents a general function of the service, usually in terms
 * of a standard service protocol (e.g. SIA), but not necessarily. A service 
 * can have many capabilities associated with it, each reflecting a
 * different aspect of the functionality it provides.
 * 
 * @author yeunga
 */
public class Capabilities
{
    private static Logger log = Logger.getLogger(Capabilities.class);

    private URI resourceIdentifier;
    private List<Capability> capabilities;

    /**
     * Constructor. 
     * @throws URISyntaxException 
     */
    public Capabilities(final URI resourceID) 
    		throws URISyntaxException
    {
    	validateResourceID(resourceID);

        this.resourceIdentifier = new URI(resourceID.toString());
        this.capabilities = new ArrayList<Capability>();
    }
    
    /**
     * Find the resource identifier associated with the capabilities.
     * 
     * @return associated resource identifier
     */
    public URI getResourceIdentifier() 
    {
		try 
		{
			return new URI(this.resourceIdentifier.toString());
		} 
		catch (URISyntaxException e) 
		{
			// Checked at construction time, so should not happen.
			throw new RuntimeException(e);
		}
	}

    /**
     * Find all associated capabilities.
     * 
     * @return all associated capabilities.
     */
	public List<Capability> getCapabilities() 
	{
		return this.capabilities;
	}
	
	/**
	 * Find the capability associated with the specified standard identifier.
	 * 
	 * @param standardID standard identifier for the required capability
	 * @return capability found or null if not found
	 */
	public Capability findCapability(final URI standardID)
	{
		for (Capability cap : this.capabilities)
		{
			if (cap.hasAttribute(standardID))
			{
				return cap;
			}
		}
		
		return null;
	}
	
	/**
	 * Produce an XML document
	 */
	public Document toXmlDocument()
	{
        Namespace xsi = Namespace.getNamespace("xsi", VOSI.XSI_NS_URI);
        Namespace cap = Namespace.getNamespace("vosi", VOSI.CAPABILITIES_NS_URI);
        Namespace vod = Namespace.getNamespace("vod", VOSI.VODATASERVICE_NS_URI);

        Element eleCapabilities = new Element("capabilities", cap);
        eleCapabilities.addNamespaceDeclaration(xsi);
        eleCapabilities.addNamespaceDeclaration(cap);
        eleCapabilities.addNamespaceDeclaration(vod);

        Document document = new Document();
        document.addContent(eleCapabilities);

        for (Capability capability : this.capabilities)
        {
            eleCapabilities.addContent(capability.toXmlElement(xsi, cap, vod));
        }
        
        return document;
	}
	
	private void validateResourceID(final URI resourceID)
	{
		if (resourceID == null)
		{
			String msg = "resource identifier for a Capabilities object cannot be null.";
			throw new IllegalArgumentException(msg);
		}
	}
}
