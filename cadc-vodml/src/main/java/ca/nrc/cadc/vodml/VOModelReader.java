/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2016.                            (c) 2016.
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
************************************************************************
*/

package ca.nrc.cadc.vodml;

import ca.nrc.cadc.xml.W3CConstants;
import ca.nrc.cadc.xml.XmlUtil;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.xslt.SchematronResourceSCH;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.transform.stream.StreamSource;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.oclc.purl.dsdl.svrl.FailedAssert;
import org.oclc.purl.dsdl.svrl.SchematronOutputType;

/**
 * Initial prototype VO-DML data model reader. This currently returns an
 * org.jdom2.Document object aso it is only good for validation. Once we have
 * access to or write some classes that implement the VO-DML meta model we can
 * return an instance of VODataModel instead.
 * 
 * @author pdowler
 */
public class VOModelReader 
{
    private static final Logger log = Logger.getLogger(VOModelReader.class);
    
    private static final String VODML_NS = "http://www.ivoa.net/xml/VODML/v1.0";
    private static final String VODML_SCHEMA = "vo-dml-v1.0.xsd";
    private static final String VODML_SCHEMATRON = "vo-dml.sch.xml";

    private Map<String,String> schemaMap;
    private boolean schematronVal;
    private boolean logWarnings;
    
    public VOModelReader() 
    { 
        this(true, true, false); 
    }
    
    public VOModelReader(boolean schemaVal, boolean schematronVal, boolean logWarnings)
    {
        if (schemaVal)
        {
            schemaMap = new TreeMap<>();
            String vodmlSchemaLoc = XmlUtil.getResourceUrlString(VODML_SCHEMA, VOModelReader.class);
            log.debug("schemaMap: " + VODML_NS + " -> " + vodmlSchemaLoc);
            schemaMap.put(VODML_NS, vodmlSchemaLoc);
            
            String w3cSchemaURL = XmlUtil.getResourceUrlString(W3CConstants.XSI_SCHEMA, VOModelReader.class);
            log.debug("schemaMap: " + W3CConstants.XSI_SCHEMA + " -> " + w3cSchemaURL);
            schemaMap.put(W3CConstants.XSI_NS_URI.toString(), w3cSchemaURL);
        }
        this.schematronVal = schematronVal;
        this.logWarnings = logWarnings;
    }
    
    public Document read(InputStream in) 
        throws JDOMException, IOException, SchematronValidationException
    {
        InputStreamReader isr = new InputStreamReader(in);
        return read(isr);
    }
    
    public Document read(Reader in) 
        throws JDOMException, IOException, SchematronValidationException
    {
        SAXBuilder builder = XmlUtil.createBuilder(schemaMap);
        Document doc = builder.build(in);
        if (schematronVal)
            validateSchematron(doc);
        return doc;
    }
    
    private void validateSchematron(Document doc)
        throws IOException, SchematronValidationException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        VOModelWriter w = new VOModelWriter(false);
        w.write(doc, bos);
        byte[] buf = bos.toByteArray();
        
        ISchematronResource sch = SchematronResourceSCH.fromClassPath(VODML_SCHEMATRON);
        if (sch == null)
            throw new RuntimeException("failed to load " + VODML_SCHEMATRON);
        if (!sch.isValidSchematron ())
            throw new IllegalArgumentException("VO-DML schematron is invalid: " + VODML_SCHEMATRON);
            
        SchematronOutputType result;
        try
        {
            result = sch.applySchematronValidationToSVRL(new StreamSource(new ByteArrayInputStream(buf)));
        }
        catch (Exception ex)
        {
            throw new RuntimeException("failed to validate", ex);
        }
        if (result == null)
        {
            throw new RuntimeException("failed to validate: no result from schematron validator (unexpected)");
        }
        
        int num = result.getTextCount();
        for (int i=0; i<num; i++)
        {
            String o = result.getTextAtIndex(i);
            log.debug("[" + i + "] " + o);
        }
        num = result.getActivePatternAndFiredRuleAndFailedAssertCount();
        List<String> emsgs = new ArrayList<>();
        for (int i=0; i<num; i++)
        {
            Object o = result.getActivePatternAndFiredRuleAndFailedAssertAtIndex(i);
            log.debug("[" + i + "," + o.getClass().getName() + "] " + o);
            if (o instanceof FailedAssert)
            {
                FailedAssert fa = (FailedAssert) o;
                if ("error".equalsIgnoreCase(fa.getFlag()))
                    emsgs.add("[" + emsgs.size() + 1 + "] " + o);
                else if (logWarnings && "warning".equalsIgnoreCase(fa.getFlag()))
                    log.warn(fa.getText());
            }
        }

        if (!emsgs.isEmpty())
            throw new SchematronValidationException(emsgs.size(), emsgs);
    }
}
