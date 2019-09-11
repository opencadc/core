/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2019.                            (c) 2019.
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
 * @author adriand
 *
 * @version $Revision: $
 *
 *
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */

package ca.nrc.cadc.auth;

import java.io.Serializable;
import java.security.Principal;

/**
 * Class that represents a Posix Principal. This is useful for
 * representing a user with a PosixAccount key reference.
 */
public class PosixPrincipal implements Principal, Serializable
{
	private static final long serialVersionUID = 5423257890488724644L;
	private int uidNumber;

    /**
     * Ctor
     * @param uidNumber unique identifier
     */
    public PosixPrincipal(int uidNumber)
    {
        this.uidNumber = uidNumber;
    }

    @Override
    public String getName()
    {
        return Integer.toString(uidNumber);
    }

    public int getUidNumber()
    {
        return uidNumber;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + uidNumber;
		return result;
	}

    @Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PosixPrincipal other = (PosixPrincipal) obj;
		if (uidNumber != other.uidNumber)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PosixPrincipal [uidNumber=" + uidNumber + "]";
	}
}
