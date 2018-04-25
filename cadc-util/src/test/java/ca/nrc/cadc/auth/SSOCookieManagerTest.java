/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2018.                            (c) 2018.
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


import ca.nrc.cadc.util.Base64;
import ca.nrc.cadc.util.Log4jInit;
import ca.nrc.cadc.util.PropertiesReader;
import ca.nrc.cadc.util.RSASignatureGeneratorValidatorTest;
import ca.nrc.cadc.util.RsaSignatureGenerator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.Principal;
import java.util.ArrayList;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.security.auth.x500.X500Principal;
import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;


public class SSOCookieManagerTest
{    
    static
    {
        Log4jInit.setLevel("ca.nrc.cadc.auth", Level.INFO);
    }
    
    File pubFile, privFile;

    List<String> domainList = new ArrayList<>();

    @Before
    public void initKeys() throws Exception
    {
        String keysDir = RSASignatureGeneratorValidatorTest.getCompleteKeysDirectoryName();
        RsaSignatureGenerator.genKeyPair(keysDir);
        privFile = new File(keysDir, RsaSignatureGenerator.PRIV_KEY_FILE_NAME);
        pubFile = new File(keysDir, RsaSignatureGenerator.PUB_KEY_FILE_NAME);

        domainList.add("canfar.phys.uvic.ca");
        domainList.add("cadc.hia.nrc.gc.ca");
        domainList.add("ccda.iha.cnrc.gc.ca");
        domainList.add("cadc-ccda.hia-iha.nrc-cnrc.gc.ca");

    }
    
    @After
    public void cleanupKeys() throws Exception
    {
        pubFile.delete();
        privFile.delete();
    }
    
    @Test
    public void roundTripMin() throws Exception
    {
        final HttpPrincipal userPrincipal = new HttpPrincipal("CADCtest");
        SSOCookieManager cm = new SSOCookieManager();
        DelegationToken cookieToken = cm.parse(cm.generate(userPrincipal, null));
        HttpPrincipal actualPrincipal = cookieToken.getUser();

        //Check principal
        assertEquals(userPrincipal, actualPrincipal);
    }

    @Test
    public void roundTrip() throws Exception
    {
        SSOCookieManager cm = new SSOCookieManager();
        System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "./build/resources/test");

        // round trip test
        Set<Principal> testPrincipals = new HashSet<>();
        HttpPrincipal hp = new HttpPrincipal("someuser");
        testPrincipals.add(hp);
        X500Principal xp = new X500Principal("CN=JBP,OU=nrc-cnrc.gc.ca,O=grid,C=CA");
        testPrincipals.add(xp);

        // Pretend CADC identity
        UUID testUUID = UUID.randomUUID();
        testPrincipals.add(new NumericPrincipal(testUUID));

        URI scope = new URI("sso:cadc+canfar");
        String cookieValue = cm.generate(testPrincipals, scope);

        DelegationToken actToken = cm.parse(cookieValue);

        assertEquals("User id not the same", hp, actToken.getUser());
        assertEquals("Scope not the same", scope, actToken.getScope());
        assertEquals("x509 principal not the same", xp, actToken.getPrincipalByClass(X500Principal.class));

        assertEquals("domain list not equal", domainList, actToken.getDomains());
    }


    public String createCookieString() throws InvalidKeyException, IOException {

        // Set properties file location.
        System.setProperty(PropertiesReader.CONFIG_DIR_SYSTEM_PROPERTY, "./build/resources/test");
        // get the properties
        PropertiesReader propReader = new PropertiesReader(SSOCookieManager.DOMAINS_PROP_FILE);
        List<String> propertyValues = propReader.getPropertyValues("domains");
        List<String> domainList = Arrays.asList(propertyValues.get(0).split(" "));

        Date baseTime = new Date();
        Date cookieExpiry = new Date(baseTime.getTime() + (48 * 3600 * 1000));
        String testCookieStringDate = DelegationToken.EXPIRY_LABEL + "=" + cookieExpiry.getTime();

        String testCookieStringBody ="&" + DelegationToken.USER_LABEL + "=someuser&" +
            DelegationToken.DOMAIN_LABEL + "=" + domainList.get(0) + "&" +
            DelegationToken.DOMAIN_LABEL + "=" + domainList.get(1) + "&" +
            DelegationToken.DOMAIN_LABEL + "=" + domainList.get(2) + "&" +
            DelegationToken.DOMAIN_LABEL + "=" + domainList.get(3);

        StringBuilder sb = new StringBuilder(testCookieStringDate + testCookieStringBody);

        //sign and add the signature field
        String toSign = sb.toString();
        sb.append("&");
        sb.append(DelegationToken.SIGNATURE_LABEL);
        sb.append("=");
        RsaSignatureGenerator su = new RsaSignatureGenerator();
        byte[] sig =
            su.sign(new ByteArrayInputStream(toSign.getBytes()));
        sb.append(new String(Base64.encode(sig)));

        return sb.toString();
    }

    @Test
    public void createCookieSet() throws Exception {
        List<SSOCookieCredential> cookieList = new ArrayList<>();

        try {
            String cookieValue = createCookieString();
            cookieList = new SSOCookieManager().getSSOCookieCredentials(cookieValue, "www.canfar.phys.uvic.ca");

            // cookieList length should be same as list of expected domains
            assertEquals(cookieList.size(), domainList.size());
        }
        finally
        {
            System.setProperty(PropertiesReader.class.getName() + ".dir", "");
        }
    }

}
