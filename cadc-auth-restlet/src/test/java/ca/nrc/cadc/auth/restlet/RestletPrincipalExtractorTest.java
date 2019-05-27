/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2012.                         (c) 2012.
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
 * @author jenkinsd
 * 4/20/12 - 12:49 PM
 *
 *
 *
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */

package ca.nrc.cadc.auth.restlet;

import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.DelegationToken;
import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.util.RsaSignatureGenerator;
import ca.nrc.cadc.auth.NotAuthenticatedException;

import java.io.File;
import java.net.URI;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.security.auth.x500.X500Principal;
import org.apache.log4j.Logger;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.data.ClientInfo;
import org.restlet.data.Cookie;
import org.restlet.data.Form;
import org.restlet.engine.util.CookieSeries;
import org.restlet.util.Series;


public class RestletPrincipalExtractorTest {

    private static final Logger log = Logger.getLogger(RestletPrincipalExtractorTest.class);

    private RestletPrincipalExtractor testSubject;
    private final Request mockRequest = createMock(Request.class);
    
    @Test
    public void testCaseInsensitiveDelegationToken() throws Exception {
    	// create an invalid token and test that RestletPrincipalExtractor finds it even when case of
    	// attribute is changed
        setTestSubject(new RestletPrincipalExtractor() {
            @Override
            public Request getRequest() {
                return getMockRequest();
            }

            @Override
            protected String getAuthenticatedUsername() {
                return null;
            }
        });
 
        final ConcurrentMap<String, Object> attributes
                = new ConcurrentHashMap<String, java.lang.Object>();
        Form form = new Form(AuthenticationUtil.AUTH_HEADER + "=foo");
        attributes.put("org.restlet.http.headers", form);
        expect(getMockRequest().getAttributes()).andReturn(attributes).atLeastOnce();

        replay(getMockRequest());
        try {
        		DelegationToken dt = getTestSubject().getDelegationToken();
        		assertTrue(false);
        } catch (NotAuthenticatedException e)
        {
        		assertEquals("Unexpected exception", "invalid delegation token. null", e.getMessage());
        }
        
        // repeat test with lowercase attributes
        reset(getMockRequest());
        attributes.clear();
        form = new Form(AuthenticationUtil.AUTH_HEADER.toLowerCase() + "=foo");
        attributes.put("org.restlet.http.headers", form);
        expect(getMockRequest().getAttributes()).andReturn(attributes).atLeastOnce();
        
		replay(getMockRequest());
		try {
				DelegationToken dt = getTestSubject().getDelegationToken();
				assertTrue(false);
		} catch (NotAuthenticatedException e)
		{
				assertEquals("Unexpected exception", "invalid delegation token. null", e.getMessage());
		}
    }

    @Test
    public void testGetDelegationToken() throws Exception {
        setTestSubject(new RestletPrincipalExtractor() {
            @Override
            public Request getRequest() {
                return getMockRequest();
            }

            @Override
            protected String getAuthenticatedUsername() {
                return null;
            }
        });

        final Series<Cookie> requestCookies = new CookieSeries();
        expect(getMockRequest().getCookies()).andReturn(requestCookies).atLeastOnce();

        final ConcurrentMap<String, Object> attributes
                = new ConcurrentHashMap<String, java.lang.Object>();
        Form mockHeaders = createMock(Form.class);
        attributes.put("org.restlet.http.headers", mockHeaders);
        expect(mockHeaders.getFirstValue(AuthenticationUtil.AUTH_HEADER, true)).andReturn(null).atLeastOnce();
        expect(getMockRequest().getAttributes()).andReturn(attributes).atLeastOnce();

        replay(mockHeaders);
        replay(getMockRequest());

        DelegationToken dt = getTestSubject().getDelegationToken();
        System.out.println(dt);
        assertNull("Should have no token", dt);

        Set<Principal> ps = getTestSubject().getPrincipals();
        assertTrue("Should have no principals.", ps.isEmpty());

        verify(getMockRequest());
    }

    @Test
    public void addCookiePrincipal() throws Exception {
        final Series<Cookie> requestCookies = new CookieSeries();

        setTestSubject(new RestletPrincipalExtractor() {
            @Override
            public Request getRequest() {
                return getMockRequest();
            }
        });

        final ClientInfo clientInfo = new ClientInfo();
        expect(getMockRequest().getClientInfo()).andReturn(clientInfo).atLeastOnce();

        expect(getMockRequest().getCookies()).andReturn(requestCookies).atLeastOnce();

        final ConcurrentMap<String, Object> attributes
                = new ConcurrentHashMap<String, java.lang.Object>();
        Form mockHeaders = createMock(Form.class);
        attributes.put("org.restlet.http.headers", mockHeaders);
        expect(mockHeaders.getFirstValue(AuthenticationUtil.AUTH_HEADER, true)).andReturn(null).atLeastOnce();
        expect(getMockRequest().getAttributes()).andReturn(attributes).atLeastOnce();

        replay(mockHeaders);
        replay(getMockRequest());

        Set<Principal> ps = getTestSubject().getPrincipals();

        assertTrue("Should have no principals.", ps.isEmpty());

        verify(getMockRequest());

        //
        // TEST 2 -- can't test this without full setup of keys to generate and validate
        /*
        reset(mockHeaders);
        reset(getMockRequest());

        String sessionID = new SSOCookieManager().generate(new HttpPrincipal("foo"));
        requestCookies.add("CADC_SSO", sessionID);

        expect(getMockRequest().getClientInfo()).andReturn(clientInfo).atLeastOnce();
        
        expect(getMockRequest().getCookies()).andReturn(requestCookies).atLeastOnce();
        
        expect(mockHeaders.getFirstValue(AuthenticationUtil.AUTH_HEADER)).andReturn(null).atLeastOnce();
        
        expect(getMockRequest().getAttributes()).andReturn(attributes).atLeastOnce();

        replay(mockHeaders);
        replay(getMockRequest());

        ps = getTestSubject().getPrincipals();

        assertEquals("Should have one principal.", 1, ps.size());
        CookiePrincipal cp = (CookiePrincipal) ps.iterator().next();
        assertEquals(sessionID, cp.getSessionId());
        

        verify(getMockRequest());
         */
    }

    @Test
    public void addHTTPPrincipal() throws Exception {

        setTestSubject(new RestletPrincipalExtractor() {
            @Override
            public Request getRequest() {
                return getMockRequest();
            }

            @Override
            protected String getAuthenticatedUsername() {
                return null;
            }
        });

        final Series<Cookie> requestCookies = new CookieSeries();
        expect(getMockRequest().getCookies()).andReturn(requestCookies).atLeastOnce();

        final ConcurrentMap<String, Object> attributes
                = new ConcurrentHashMap<String, java.lang.Object>();
        Form mockHeaders = createMock(Form.class);
        attributes.put("org.restlet.http.headers", mockHeaders);
        expect(mockHeaders.getFirstValue(AuthenticationUtil.AUTH_HEADER, true)).andReturn(null).atLeastOnce();
        expect(getMockRequest().getAttributes()).andReturn(attributes).atLeastOnce();

        replay(mockHeaders);
        replay(getMockRequest());

        Set<Principal> ps = getTestSubject().getPrincipals();

        assertTrue("Should have no principals.", ps.isEmpty());

        verify(getMockRequest());
    }

    @Test
    public void addX500Principal() throws Exception {
        setTestSubject(new RestletPrincipalExtractor() {
            @Override
            public Request getRequest() {
                return getMockRequest();
            }
        });

        final ClientInfo clientInfo = new ClientInfo();
        expect(getMockRequest().getClientInfo()).andReturn(clientInfo).atLeastOnce();

        final ConcurrentMap<String, Object> attributes
                = new ConcurrentHashMap<String, java.lang.Object>();
        Form mockHeaders = createMock(Form.class);
        attributes.put("org.restlet.http.headers", mockHeaders);
        expect(mockHeaders.getFirstValue(AuthenticationUtil.AUTH_HEADER, true)).andReturn(null).atLeastOnce();
        expect(getMockRequest().getAttributes()).andReturn(attributes).atLeastOnce();

        final Series<Cookie> requestCookies = new CookieSeries();
        expect(getMockRequest().getCookies()).andReturn(requestCookies).atLeastOnce();

        replay(mockHeaders);
        replay(getMockRequest());

        Set<Principal> ps = getTestSubject().getPrincipals();

        assertTrue("Should have no principals.", ps.isEmpty());

        verify(getMockRequest());

        //
        // TEST 2
        reset(getMockRequest());

        expect(getMockRequest().getClientInfo()).andReturn(clientInfo).atLeastOnce();

        expect(getMockRequest().getCookies()).andReturn(requestCookies).atLeastOnce();

        mockHeaders = createMock(Form.class);
        attributes.put("org.restlet.http.headers", mockHeaders);
        expect(mockHeaders.getFirstValue(AuthenticationUtil.AUTH_HEADER, true)).andReturn(null).atLeastOnce();

        final Calendar notAfterCal = Calendar.getInstance();
        notAfterCal.set(1977, Calendar.NOVEMBER, 25, 3, 21, 0);
        notAfterCal.set(Calendar.MILLISECOND, 0);

        final X500Principal subjectX500Principal
                = new X500Principal("CN=CN1,O=O1");
        final X500Principal issuerX500Principal
                = new X500Principal("CN=CN2,O=O2");
        final Date notAfterDate = notAfterCal.getTime();
        final X509Certificate mockCertificate
                = createMock(X509Certificate.class);

        final Collection<X509Certificate> certificates1
                = new ArrayList<X509Certificate>();

        certificates1.add(mockCertificate);

        attributes.put("org.restlet.https.clientCertificates", certificates1);
        expect(getMockRequest().getAttributes()).andReturn(attributes).atLeastOnce();

        expect(mockCertificate.getNotAfter()).andReturn(notAfterDate).once();
        expect(mockCertificate.getSubjectX500Principal()).
                andReturn(subjectX500Principal).once();
        expect(mockCertificate.getIssuerX500Principal()).andReturn(
                issuerX500Principal).once();

        replay(mockHeaders);
        replay(getMockRequest(), mockCertificate);

        ps = getTestSubject().getPrincipals();

        assertEquals("Should have one HTTP principal.", 1, ps.size());

        verify(getMockRequest(), mockCertificate);
    }

    protected RestletPrincipalExtractor getTestSubject() {
        return testSubject;
    }

    protected void setTestSubject(final RestletPrincipalExtractor testSubject) {
        this.testSubject = testSubject;
    }

    public Request getMockRequest() {
        return mockRequest;
    }
}
