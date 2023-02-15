/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2023.                            (c) 2023.
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

package ca.nrc.cadc.auth;

import javax.security.auth.Subject;

/**
 * Simple interface to manage user identity. 
 *
 * @author pdowler
 */
public interface IdentityManager {
    /**
     * Parse and validate any principals in the subject.
     * Some principals, such as X500Principal, do not require validation
     * as that is done with TLS.
     * AuthorizationTokenPrincipals must be parsed and validated.  If
     * validation is successful, an associated AuthorizationToken must
     * be put into the subject's public credentials.  At the end of the
     * validate/augment calls, the principal must remain in the subject
     * or be replaced by an HttpPrincipal with userid if available.
     * Failed validation must result in a NotAuthenticatedException.
     * 
     * @param subject The subject, with principals, to validate.
     * @return The validated subject with public credentials added.
     * @throws NotAuthenticatedException If validation fails.
     */
    public Subject validate(final Subject subject) throws NotAuthenticatedException;
    
    /**
     * Modify a Subject and return it. Implementations can modify the specified
     * Subject by adding identities (Principals) or credentials. The argument
     * subject will typically have an HttpPrinncipal if the request went through
     * HTTP authentication, or an X500principal if the user authenticated via SSL
     * with client certificate. The typical usage is to add additional principals
     * with internal identity information. This could then be used in an Authorizer
     * implementation elsewhere in the application.
     *
     * @param subject The initial subject.
     * @return The modified subject.
     */
    public Subject augment(final Subject subject);
    
    /**
     * Create a subject from the specified owner object. This is the reverse of
     * toOwner(Subject). The returned subject must include at least one Principal
     * but need not contain any credentials.
     *
     * @param owner
     * @return reconstructed Subject
     */
    Subject toSubject(Object owner);

    /**
     * Convert the specified subject into an arbitrary object. This is the reverse
     * of toSubject(owner). The persistent object must capture the identity (the
     * principal from the subject) but generally does not capture the credentials.
     *
     * @param subject
     * @return arbitrary owner object to be persisted
     */
    Object toOwner(Subject subject);

    /**
     * Convert the specified subject to a suitable string representation of the
     * owner for display (logging, display, maybe API output, etc.). Implementations 
     * will typically chose one principal from the subject and convert it to a string.
     *
     * @param subject
     * @return simple string representation of the subject
     */
    String toDisplayString(Subject subject);
}
