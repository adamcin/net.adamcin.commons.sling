package net.adamcin.commons.sling.request;

import org.apache.sling.api.adapter.SlingAdaptable;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DefaultHttpServletRequest extends SlingAdaptable implements HttpServletRequest {

    private final String method;
    private final String path;
    private final String pathInfo;
    private final Map<String, Object> attributes;
    private final Map<String, String> parameters;

    public DefaultHttpServletRequest(String method, String path) {
        this(method, path, "");
    }

    public DefaultHttpServletRequest(String method, String path, String pathInfo) {
        this.method = method;
        this.path = path;
        this.pathInfo = pathInfo;
        this.attributes = new HashMap<String, Object>();
        this.parameters = new HashMap<String, String>();
    }

    public String getMethod() { return this.method; }

    public String getPathInfo() { return this.pathInfo; }
	
    public String getServletPath() { return this.path; }

    public String getRequestURI() { return getServletPath() + getPathInfo(); }
	
    public StringBuffer getRequestURL() {
        StringBuffer sb = new StringBuffer(getScheme());
        sb.append(getServerName());
        sb.append(":");
        sb.append(getServerPort());
        sb.append(getServletPath());
        sb.append(getPathInfo());
        return sb;
    }

    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    public Enumeration getAttributeNames() {
        return null;
    }

    public void setAttribute(String name, Object o) {
        this.attributes.put(name, o);
    }

    public void removeAttribute(String name) {
        this.attributes.remove(name);
    }

    public String getAuthType() { return null; }
    public Cookie[] getCookies() { return new Cookie[0]; }
    public long getDateHeader(String name) { return 0; }
    public String getHeader(String name) { return null; }
    public Enumeration getHeaders(String name) { return null; }
    public Enumeration getHeaderNames() { return null; }
    public int getIntHeader(String name) { return 0; }
    public String getPathTranslated() { return null; }
    public String getContextPath() { return ""; }
    public String getQueryString() { return null; }
    public String getRemoteUser() { return null; }
    public boolean isUserInRole(String role) { return false; }
    public Principal getUserPrincipal() { return null; }
    public String getRequestedSessionId() { return null; }
    public HttpSession getSession(boolean create) { return null; }
    public HttpSession getSession() { return null; }
    public boolean isRequestedSessionIdValid() { return false; }
    public boolean isRequestedSessionIdFromCookie() { return false; }
    public boolean isRequestedSessionIdFromURL() { return false; }
    public boolean isRequestedSessionIdFromUrl() { return false; }
    public String getCharacterEncoding() { return null; }
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException { }
    public int getContentLength() { return 0; }
    public String getContentType() { return null; }
    public ServletInputStream getInputStream() throws IOException { return null; }
    public String getParameter(String name) { return null; }
    public Enumeration getParameterNames() { return null; }
    public String[] getParameterValues(String name) { return null; }
    public Map getParameterMap() { return this.parameters; }
    public String getProtocol() { return null; }
    public String getScheme() { return "http"; }
    public String getServerName() { return "localhost"; }
    public int getServerPort() { return 4502; }
    public BufferedReader getReader() throws IOException { return null; }
    public String getRemoteAddr() { return null; }
    public String getRemoteHost() { return null; }
    public Locale getLocale() { return null; }
    public Enumeration getLocales() { return null; }
    public boolean isSecure() { return false; }
    public RequestDispatcher getRequestDispatcher(String path) { return null; }
    public String getRealPath(String path) { return null; }
    public int getRemotePort() { return 0; }
    public String getLocalName() { return null; }
    public String getLocalAddr() { return null; }
    public int getLocalPort() { return 0; }
}
