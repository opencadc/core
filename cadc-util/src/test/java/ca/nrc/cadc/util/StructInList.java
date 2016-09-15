package ca.nrc.cadc.util;/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2005.                            (c) 2005.
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

import java.util.Arrays;
import java.util.List;

/**
 * @author goliaths
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class StructInList
{

    public StructInList()
    {
    }

    public void setEntry1(String c)
    {
        entry1 = c;
    }

    public void setEntry2(String c)
    {
        entry2 = c;
    }

    public void setEntry3(String c)
    {
        entry3 = c;
    }

    public void setEntry4(String[] c)
    {
        entry4 = Arrays.asList( c );
    }

    public static final String Entry1_CONFIG_LOOKUP_VALUE = "ENTRY1";
    public static final String Entry2_CONFIG_LOOKUP_VALUE = "ENTRY2";
    public static final String Entry3_CONFIG_LOOKUP_VALUE = "ENTRY3";
    public static final String Entry4_CONFIG_LOOKUP_VALUE = "ENTRY4";

    public String entry1 = null;
    public String entry2 = null;
    public String entry3 = null;
    public List entry4 = null;

}
