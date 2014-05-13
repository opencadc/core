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

package ca.nrc.cadc.dali.tables.ascii;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import ca.nrc.cadc.dali.tables.TableData;
import ca.nrc.cadc.dali.tables.TableWriter;
import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import ca.nrc.cadc.dali.tables.votable.VOTableField;
import ca.nrc.cadc.dali.tables.votable.VOTableResource;
import ca.nrc.cadc.dali.tables.votable.VOTableTable;
import ca.nrc.cadc.dali.util.DefaultFormat;
import ca.nrc.cadc.dali.util.Format;
import ca.nrc.cadc.dali.util.FormatFactory;

import com.csvreader.CsvWriter;

/**
 * Write a table document in ascii format (CSV or TSV). This writer can tolerate a
 * table structure with no field metadata (e.g. no TableField objects describing the
 * columns of the table) as long as the DefaultFormat can write the objects out.
 *
 * @see ca.nrc.cadc.dali.util.DefaultFormat
 * @author pdowler
 */
public class AsciiTableWriter implements TableWriter<VOTableDocument>
{
    private static final Logger log = Logger.getLogger(AsciiTableWriter.class);

    // ASCII character set.
    public static final String US_ASCII = "US-ASCII";

    // CSV format delimiter.
    public static final char CSV_DELI = ',';

    // TSV format delimiter.
    public static final char TSV_DELI = '\t';

    // Maximum number of rows to write.
    protected int maxRows;
    private char delimeter;
    private ContentType contentType;

    private FormatFactory formatFactory;


    public static enum ContentType
    {
        CSV("text/csv; header=present"),
        TSV("text/tab-separated-values");

        private String value;

        private ContentType(String s) { this.value = s; }

        public String getValue()
        {
            return value;
        }
    }



    public AsciiTableWriter(ContentType fmt)
    {
        this.contentType = fmt;
        if (ContentType.CSV.equals(fmt))
            this.delimeter = CSV_DELI;
        else
            this.delimeter = TSV_DELI;
    }

    @Override
    public String getExtension()
    {
        return "txt";
    }

    @Override
    public String getContentType()
    {
        return contentType.getValue();
    }

    @Override
    public void setFormatFactory(FormatFactory formatFactory)
    {
        this.formatFactory = formatFactory;
    }

    @Override
    public void write(VOTableDocument vot, OutputStream out)
        throws IOException
    {
        write(vot, out, null);
    }

    @Override
    public void write(VOTableDocument vot, OutputStream out, Long maxrec)
        throws IOException
    {
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, US_ASCII));
        write(vot, writer, maxrec);
    }

    @Override
    public void write(VOTableDocument vot, Writer out)
        throws IOException
    {
        write(vot, out, null);
    }

    @Override
    public void write(VOTableDocument votable, Writer writer, Long maxrec)
        throws IOException
    {
        try
        {
            if (formatFactory == null)
                this.formatFactory = new FormatFactory();
            writeImpl(votable, writer, maxrec);
        }
        finally
        {

        }
    }

    protected void writeImpl(VOTableDocument votable, Writer out, Long maxrec)
        throws IOException
    {
        // find the TableData object in the VOTable
        VOTableResource vr = votable.getResourceByType("results");
        VOTableTable vt = vr.getTable();
        TableData td = vt.getTableData();
        List<VOTableField> fields = vt.getFields();

        // initialize the list of associated formats
        List<Format<Object>> formats = new ArrayList<Format<Object>>();
        if (fields != null && !fields.isEmpty())
        {
            for (VOTableField field : fields)
            {
                Format<Object> format = null;
                if (field.getFormat() == null)
                    format = formatFactory.getFormat(field);
                else
                    format = field.getFormat();
                formats.add(format);
            }
        }

        CsvWriter writer = new CsvWriter(out, delimeter);
        try
        {
            // Add the metadata elements.
            for (VOTableField field : fields)
                writer.write(field.getName());
            writer.endRecord();

            // TODO: header comment?
            long numRows = 0L;
            boolean ok = true;
            Iterator<List<Object>>rows = td.iterator();
            while ( ok && rows.hasNext() )
            {
                List<Object> row = rows.next();
                if (!fields.isEmpty() && row.size() != fields.size() )
                    throw new IllegalStateException("cannot write row: " + fields.size() + " metadata fields, " + row.size() + " data columns");
                for (int i=0; i<row.size(); i++)
                {
                    Object o = row.get(i);
                    Format<Object> fmt = new DefaultFormat();
                    if (!fields.isEmpty())
                    {
                        fmt = formats.get(i);
                    }
                    writer.write( fmt.format(o) );
                }
                writer.endRecord();
                numRows++;
                if (maxrec != null && numRows == maxrec.longValue())
                    ok = false;

                writer.flush();
            }
        }
        catch (Exception ex)
        {
            throw new IOException("error while writing", ex);
        }
        finally
        {
            writer.flush();
        }
    }



}
