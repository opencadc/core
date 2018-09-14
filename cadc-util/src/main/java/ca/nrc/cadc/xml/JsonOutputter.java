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
*  $Revision: 5 $
*
************************************************************************
*/

package ca.nrc.cadc.xml;


import ca.nrc.cadc.util.StringUtil;
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
import org.jdom2.Namespace;
import org.jdom2.output.Format;

/**
 * Special JDOM2 Outputter that writes a Document in JSON format. This class 
 * nominally follows the Badgerfish convention (http://badgerfish.ning.com/)
 * except that it assumes it will be used for structured data and not mark up. 
 * For example, there is no mixing of child text and child elements.
 * 
 * Unlike Badgerfish, scalar values are written as simple key:value pairs instead
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

    /**
     * Write a Document instance to a writer.
     *
     * @param doc Document instance to write.
     * @param writer Writer to write to.
     * @throws IOException if the writer fails to write.
     */
    public void output(final Document doc, Writer writer)
        throws IOException
    {
        output(doc.getRootElement(), writer);
    }

    /**
     * Write a Document instance to a writer.
     *
     * @param element document to write starting from element.
     * @param writer Writer to write to.
     * @throws IOException if the writer fails to write.
     */
    public void output(final Element element, Writer writer)
        throws IOException
    {
        PrintWriter pw = new PrintWriter(writer);
        pw.print("{ ");
        writeElement(element, pw, 1, false);
        indent(pw, 0);
        pw.print("}");
        pw.flush();
    }
    
    private void writeSchema(String uri, String pre, PrintWriter w)
    {
        w.print(QUOTE);
        w.print("@");
        w.print("xmlns");
        if (StringUtil.hasText(pre))
        {
            w.print(":");
            w.print(pre);
        }
        w.print(QUOTE);
        w.print(" : ");
        w.print(QUOTE);
        w.print(uri);
        w.print(QUOTE);
    }
    
    private boolean writeSchemaAttributes(Element e, PrintWriter w, int i)
        throws IOException
    {
        boolean ret = false;
        
        // getNamespacesIntroduced: only write for newly introduced namespaces;
        // this correctly handles the current context of namespaces from root to 
        // current element
        for (Namespace ans : e.getNamespacesIntroduced())
        {
            String uri = ans.getURI();
            String pre = ans.getPrefix();
            if (ret)
            {
                w.print(",");
            }
            ret = true;
            indent(w, i);
            writeSchema(uri, pre, w);
        }
        return ret;
    }
    
    // use @ for attribute names, see: http://badgerfish.ning.com/
    private boolean writeAttributes(Element e, PrintWriter w, int i)
        throws IOException
    {
        boolean ret = writeSchemaAttributes(e, w, i);
        
        Iterator<Attribute> iter = e.getAttributes().iterator();
        if (ret && iter.hasNext())
            w.print(",");
        while ( iter.hasNext() )
        {
            ret = true;
            Attribute a = iter.next();
            indent(w, i);
            w.print(QUOTE);
            w.print("@");
            if (StringUtil.hasText(a.getNamespacePrefix()))
            {
                w.print(a.getNamespacePrefix());
                w.print(":");
            }
            w.print(a.getName());
            w.print(QUOTE);
            w.print(" : ");
            if ( isBoolean(e.getName(), a.getValue()) || isNumeric(e.getName(), a.getValue()) )
            {
                w.print(a.getValue());
            }
            else
            {
                w.print(QUOTE);
                w.print(a.getValue());
                w.print(QUOTE);
            }
            if (iter.hasNext())
                w.print(",");
        }
        
        return ret;
    }
    
    // use "name" : " " for scalar values
    // use "name" : { } for object values
    // use "name" : [ ] for list values
    private void writeElement(Element e, PrintWriter w, int i, boolean listItem)
        throws IOException
    {
        boolean childListItem = listElementNames.contains(e.getName());
        
        indent(w, i);
        
        if (listItem)
        {
            w.print(QUOTE);
            w.print(e.getName());
            w.print(QUOTE);
            w.print(" : ");
        }
        else
        {
            // write key
            w.print(QUOTE);
            if (StringUtil.hasText(e.getNamespacePrefix()))
            {
                w.print(e.getNamespacePrefix());
                w.print(":");
            }
            w.print(e.getName());
            w.print(QUOTE);
            w.print(" : ");
        }
        
        // write value
        w.print("{");
        boolean multiLine = true;
        boolean children = false;
        boolean attrs = writeAttributes(e, w, i+1);
        
        if (childListItem)
        {
            // in badgerfish, this would be the name of child elements but prefer $ since [] 
            // is the value of e and e is a list of children; $ is also consistent with how
            // we (and badgerfish) handle leaf values inside elements: { "$" : value }
            if (attrs)
            {
                w.print(",");
            }
            indent(w,i+1);
            w.print(QUOTE);
            w.print("$");
            w.print(QUOTE);
            w.print(" : ");
            w.print("[");
            children = writeChildElements(e, w, i+2, childListItem, attrs);
            indent(w,i+1);
            w.print("]");
        }
        else
            children = writeChildElements(e, w, i+1, childListItem, attrs);
        
        if (!children && !childListItem) // plain value
        {
            if (attrs)
            {
                w.print(",");
                indent(w,i+1);
            }
            else
                multiLine = false;
            String sval = e.getTextNormalize();
            w.print(QUOTE);
            w.print("$");
            w.print(QUOTE);
            w.print(" : ");
            if ( isBoolean(e.getName(), sval) || isNumeric(e.getName(), sval) )
                w.print(sval);
            else
            {
                w.print(QUOTE);
                w.print(sval);
                w.print(QUOTE);
            }
        }
        if (multiLine)
            indent(w, i);
        w.print("}");
    }
    
    // comma separated list of children
    private boolean writeChildElements(Element e, PrintWriter w, int i, boolean listItem, boolean parentAttrs)
        throws IOException
    {
        boolean ret = false;
        Iterator<Element> iter = e.getChildren().iterator();
        if (iter.hasNext() && (parentAttrs && !listItem))
            w.print(",");
        while ( iter.hasNext() )
        {
            Element c = iter.next();
            ret = true;
            if (listItem)
            {
                indent(w,i);
                w.print("{");
                writeElement(c, w, i+1, listItem);
            }
            else
            {
              writeElement(c, w, i, listItem);
            }
            if (listItem)
            {
                indent(w,i);
                w.print("}");
            }
            if (iter.hasNext())
                w.print(",");
        }
        return ret;
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
