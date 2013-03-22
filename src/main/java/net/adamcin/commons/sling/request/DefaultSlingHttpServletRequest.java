package net.adamcin.commons.sling.request;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.*;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.Cookie;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

public class DefaultSlingHttpServletRequest extends DefaultHttpServletRequest
        implements SlingHttpServletRequest {

    private Resource resource;
    private RequestPathInfo rpi;

    public DefaultSlingHttpServletRequest() {
        super(null, null);
    }

    public DefaultSlingHttpServletRequest(String method, String path) {
        super(method, path);
        this.rpi = new DefaultRequestPathInfo(path, null);
    }

    public DefaultSlingHttpServletRequest(String method, String path, String pathInfo) {
        super(method, path, pathInfo);
        this.rpi = new DefaultRequestPathInfo(path, pathInfo);
    }

    public DefaultSlingHttpServletRequest(String method, RequestPathInfo rpi) {
        super(method, rpi.getResourcePath(), DefaultRequestPathInfo.toResourceMetadata(rpi).getResolutionPathInfo());
        this.rpi = rpi;
    }

    public DefaultSlingHttpServletRequest(String method, RequestPathInfo rpi, Resource resource) {
        super(method, resource.getResourceMetadata().getResolutionPath(),
                rpi != null ? DefaultRequestPathInfo.toResourceMetadata(rpi).getResolutionPathInfo()
                        : resource.getResourceMetadata().getResolutionPathInfo());
        this.resource = resource;
        this.rpi = rpi;
    }

    public Resource getResource() {
        return resource;
    }

    public ResourceResolver getResourceResolver() {
        if (resource != null) {
            return resource.getResourceResolver();
        } else {
            return null;
        }
    }

    public RequestPathInfo getRequestPathInfo() {
        return rpi;
    }

    public RequestParameter getRequestParameter(String name) { return null; }
    public RequestParameter[] getRequestParameters(String name) { return new RequestParameter[0]; }
    public RequestParameterMap getRequestParameterMap() { return null; }
    public RequestDispatcher getRequestDispatcher(String path, RequestDispatcherOptions options) { return null; }
    public RequestDispatcher getRequestDispatcher(Resource resource, RequestDispatcherOptions options) { return null; }
    public RequestDispatcher getRequestDispatcher(Resource resource) { return null; }
    public Cookie getCookie(String name) { return null; }
    public String getResponseContentType() { return null; }
    public Enumeration<String> getResponseContentTypes() { return null; }
    public ResourceBundle getResourceBundle(Locale locale) { return null; }
    public ResourceBundle getResourceBundle(String baseName, Locale locale) { return null; }
    public RequestProgressTracker getRequestProgressTracker() { return null; }
}
