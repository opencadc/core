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

package ca.nrc.cadc.xml;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;

/**
 * Special JDOM2 Outputter that writes a Document in JSON format. This class 
 * nominally follows the Badgerfish convention (http://badgerfish.ning.com/)
 * except that it assumes it will be used for structured data and not mark up. 
 * For example, there is no mixing of child text and child elements and no 
 * attributes on elements containing ~scalar values (string, number, or boolean).
 * Unlike Badgerfish, scalar valeus are written as simple key:value pairs instead
 * of using an object value with special key ($) for the content. Also, this class
 * does not try to guess if the children of an object should be another object ({})
 * or a list ([]). You must specify which elements correspond to lists and then
 * they will be written as lists ([]) even if they do not contain multiple values.
 * 
 * @author pdowler
 */
public class JsonOutputter implements Serializable
{
    private static final long serialVersionUID = 20150205121500L;
    
    private static final Logger log = Logger.getLogger(JsonOutputter.class);
    
    private static final String QUOTE  = "\"";
    
    private Format fmt = Format.getPrettyFormat();
    
    private final List<String> listElementNames = new ArrayList<String>();
    private final List<String> stringElementNames = new ArrayList<String>();

    public JsonOutputter() { }

    /**
     * List of element names that are always written as list (array using [ ])
     * instead of objects (using { }).
     * 
     * @return 
     */
    public List<String> getListElementNames()
    {
        return listElementNames;
    }

    /** 
     * List of element names that must be forced to string even if they look 
     * like they contain values that are valid JSON data types (e.g. boolean or 
     * numeric).
     * 
     * @return 
     */
    public List<String> getStringElementNames()
    {
        return stringElementNames;
    }
    
    public void setFormat(Format fmt)
    {
        this.fmt = fmt;
    }
    
    public void output(final Document doc, Writer writer)
        throws IOException
    {
        PrintWriter pw = new PrintWriter(writer);
        Element root = doc.getRootElement();
        pw.print("{");
        writeSchema(doc, pw, 1);
        pw.print(",");
        writeElement(root, pw, 1, true);
        indent(pw, 0);
        pw.println("}");
        pw.flush();
    }
    
    private void writeSchema(Document d, PrintWriter w, int i)
        throws IOException
    {
        indent(w, i);
        w.print(QUOTE);
        w.print("@xmlns");
        w.print(QUOTE);
        w.print(":");
        w.print(QUOTE);
        w.print(d.getRootElement().getNamespace().getURI());
        w.print(QUOTE);
    }
    
    // use @ for attribute names, see: http://badgerfish.ning.com/
    private void writeAttributes(Element e, PrintWriter w, int i)
        throws IOException
    {
        
        Iterator<Attribute> iter = e.getAttributes().iterator();
        while ( iter.hasNext() )
        {
            Attribute a = iter.next();
            indent(w, i);
            w.print(QUOTE);
            w.print("@");
            w.print(a.getName());
            w.print(QUOTE);
            w.print(":");
            w.print(QUOTE);
            w.print(a.getValue());
            w.print(QUOTE);
            if (iter.hasNext())
                w.print(",");
        }
    }
    
    // use "name" : " " for scalar values
    // use "name" : { } for object values
    // use "name" : [ ] for list values
    private void writeElement(Element e, PrintWriter w, int i, boolean writeNames)
        throws IOException
    {
        String open = " [";
        String close = "]";
        boolean writeChildNames = false;
        if (!listElementNames.contains(e.getName()))
        {
            open = " {";
            close = "}";
            writeChildNames = true;
        }
        
        indent(w, i);
        if (writeNames)
        {
            w.print(QUOTE);
            w.print(e.getName());
            w.print(QUOTE);
            w.print(":");
        }
        
        // write value
        if (e.hasAttributes() || !e.getChildren().isEmpty())
        {
            w.print(open);
            writeAttributes(e, w, i+1);
            if (e.hasAttributes() && !e.getChildren().isEmpty())
                w.print(",");
            writeChildElements(e, w, i+1, writeChildNames);
            indent(w, i);
            w.print(close);
        }
        else
        {
            String sval = e.getTextNormalize();
            if ( isBoolean(e.getName(), sval) || isNumeric(e.getName(), sval) )
                w.print(sval);
            else
            {
                w.print(QUOTE);
                w.print(sval);
                w.print(QUOTE);
            }
        }
    }
    
    // comma separated list of children
    private void writeChildElements(Element e, PrintWriter w, int i, boolean writeNames)
        throws IOException
    {
        Iterator<Element> iter = e.getChildren().iterator();
        while ( iter.hasNext() )
        {
            Element c = iter.next();
            writeElement(c, w, i, writeNames);
            if (iter.hasNext())
                w.print(",");
        }
    }
    
    private boolean isBoolean(String ename, String s)
    {
        if ( stringElementNames.contains(ename))
            return false;
        return ( Boolean.TRUE.toString().equals(s) || Boolean.FALSE.toString().equals(s));
    }
    
    private boolean isNumeric(String ename, String s)
    {
        if ( stringElementNames.contains(ename))
            return false;
        try
        {
            Double.parseDouble(s); // JSON only has double
            return true;
        }
        catch(NumberFormatException nope) 
        {
            return false;
        }
    }
    
    private void indent(PrintWriter pw, int amt)
    {
        
        if (fmt != null)
        {
            pw.print(fmt.getLineSeparator());
            for (int i=0; i<amt; i++)
                pw.print(fmt.getIndent());
        }
        else
            pw.print(" ");
    }
}
