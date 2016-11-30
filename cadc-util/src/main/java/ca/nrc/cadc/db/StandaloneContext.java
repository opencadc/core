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

package ca.nrc.cadc.db;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import org.apache.log4j.Logger;

/**
 * A Simple JNDI context to support testing.
 */
public class StandaloneContext implements Context
{
    private static final Logger log = Logger.getLogger(StandaloneContext.class);
    Map<String,Object> map = new HashMap<String,Object>(1);

    @Override
    public Object lookup(String name) throws NamingException
    {
        log.debug("TestContext.lookup: " + name + " " + toString());
        return map.get(name);
    }

    @Override
    public void bind(String name, Object value) throws NamingException
    {
        log.debug("TestContext.bind : " + name + " " + value + " " + toString());
        map.put(name,  value);
    }

    @Override
    public Object addToEnvironment(String arg0, Object arg1)
            throws NamingException
    {
        return null;
    }

    @Override
    public void bind(Name arg0, Object arg1) throws NamingException
    {
    }

    @Override
    public void close() throws NamingException
    {
    }

    @Override
    public Name composeName(Name arg0, Name arg1) throws NamingException
    {
        return null;
    }

    @Override
    public String composeName(String arg0, String arg1)
            throws NamingException
    {
        return null;
    }

    @Override
    public Context createSubcontext(Name arg0) throws NamingException
    {
        return null;
    }

    @Override
    public Context createSubcontext(String name) throws NamingException
    {
        log.debug("createSubContext: " + name + " " + toString());
        Context ctx = new StandaloneContext();
        map.put(name, ctx);
        log.debug("createSubContext: created " + name + " = " + ctx.toString());
        return ctx;
    }

    @Override
    public void destroySubcontext(Name arg0) throws NamingException
    {
    }

    @Override
    public void destroySubcontext(String arg0) throws NamingException
    {

    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException
    {
        return null;
    }

    @Override
    public String getNameInNamespace() throws NamingException
    {
        return null;
    }

    @Override
    public NameParser getNameParser(Name arg0) throws NamingException
    {
        return null;
    }

    @Override
    public NameParser getNameParser(String arg0) throws NamingException
    {
        return null;
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name arg0)
            throws NamingException
    {
        return null;
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String arg0)
            throws NamingException
    {
        return null;
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name arg0)
            throws NamingException
    {
        return null;
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String arg0)
            throws NamingException
    {
        return null;
    }

    @Override
    public Object lookup(Name arg0) throws NamingException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object lookupLink(Name arg0) throws NamingException
    {
        return null;
    }

    @Override
    public Object lookupLink(String arg0) throws NamingException
    {
        return null;
    }

    @Override
    public void rebind(Name arg0, Object arg1) throws NamingException
    {
    }

    @Override
    public void rebind(String arg0, Object arg1) throws NamingException
    {
    }

    @Override
    public Object removeFromEnvironment(String arg0) throws NamingException
    {
        return null;
    }

    @Override
    public void rename(Name arg0, Name arg1) throws NamingException
    {
    }

    @Override
    public void rename(String arg0, String arg1) throws NamingException
    {
    }

    @Override
    public void unbind(Name arg0) throws NamingException
    {
    }

    @Override
    public void unbind(String arg0) throws NamingException
    {
    }

}
