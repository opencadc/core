package ca.nrc.cadc.vosi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.Subject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.log.ServletLogInfo;
import ca.nrc.cadc.log.WebServiceLogInfo;
import ca.nrc.cadc.reg.CapabilitiesReader;

/**
 * Servlet implementation class CapabilityServlet
 */
public class CapabilitiesServlet extends HttpServlet
{
    private static Logger log = Logger.getLogger(CapabilitiesServlet.class);
    private static final long serialVersionUID = 201003131300L;

    private String staticCapabilities;
    private String extensionSchemaNS;
    private String extensionSchemaLocation;

    @Override
    public void init(ServletConfig config)
        throws ServletException
    {
        super.init(config);
        String str = config.getInitParameter("input");

        if (str == null)
        {
            throw new ExceptionInInitializerError("Missing capabilities input");
        }

        log.info("static capabilities: " + str);
        try
        {
            URL resURL = config.getServletContext().getResource(str);
            CapabilitiesReader cr = new CapabilitiesReader(true);
            InputStream in = resURL.openStream();

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1)
            {
                result.write(buffer, 0, length);
            }
            String xml = result.toString("UTF-8");

            // validate
            cr.read(xml);
            this.staticCapabilities = xml;
        }
        catch(Throwable t)
        {
            log.error("CONFIGURATION ERROR: failed to read static capabilities file: " + str, t);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {

        WebServiceLogInfo logInfo = new ServletLogInfo(request);
        long start = System.currentTimeMillis();

        try
        {
            Subject subject = AuthenticationUtil.getSubject(request);
            logInfo.setSubject(subject);
            log.info(logInfo.start());

            transformCapabilities(request, response);
        }
        catch(Throwable t)
        {
            logInfo.setSuccess(false);
            logInfo.setMessage(t.toString());
            log.error("BUG: failed to rewrite hostname in accessURL elements", t);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage());
        }
        finally
        {
            logInfo.setElapsedTime(System.currentTimeMillis() - start);
            log.info(logInfo.end());
        }
    }

    private void transformCapabilities(HttpServletRequest request, HttpServletResponse response)
        throws IOException, JDOMException
    {
        URL rurl = new URL(request.getRequestURL().toString());
        String hostname = rurl.getHost();
        StringReader sr = new StringReader(staticCapabilities);
        CapabilitiesParser cp = new CapabilitiesParser(false);
        if (extensionSchemaNS != null && extensionSchemaLocation  != null)
            cp.addSchemaLocation(extensionSchemaNS, extensionSchemaLocation);
        Document doc = cp.parse(sr);

        Element root = doc.getRootElement();
        List<Namespace> nsList = new ArrayList<Namespace>();
        nsList.addAll(root.getAdditionalNamespaces());
        nsList.add(root.getNamespace());

        String xpath = "/vosi:capabilities/capability/interface/accessURL";
        XPathFactory xf = XPathFactory.instance();
        XPathExpression<Element> xp = xf.compile(xpath, Filters.element(),
                null, nsList);
        List<Element> accessURLs = xp.evaluate(doc);
        log.debug("xpath[" + xpath + "] found: " + accessURLs.size());
        for (Element e : accessURLs)
        {
            String surl = e.getTextTrim();
            log.debug("accessURL: " + surl);
            URL url = new URL(surl);
            URL nurl = new URL(url.getProtocol(), hostname, url.getPath());
            log.debug("accessURL: " + surl + " -> " + nurl);
            e.setText(nurl.toExternalForm());
        }
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        response.setContentType("text/xml");
        out.output(doc, response.getOutputStream());
    }
}
