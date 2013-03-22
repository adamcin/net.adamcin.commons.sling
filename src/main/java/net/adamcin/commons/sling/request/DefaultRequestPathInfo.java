package net.adamcin.commons.sling.request;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;

import javax.servlet.http.HttpServletRequest;

public class DefaultRequestPathInfo implements RequestPathInfo {

    private final String selectorString;
    private final String[] selectors;
    private final String extension;
    private final String suffix;
    private final String resourcePath;
    private static final String[] NO_SELECTORS = new String[0];

    public DefaultRequestPathInfo(Resource r) {
        this(r.getResourceMetadata());
    }

    public DefaultRequestPathInfo(ResourceMetadata resourceMetadata) {
        this(resourceMetadata.getResolutionPath(), resourceMetadata.getResolutionPathInfo());
    }

    public DefaultRequestPathInfo() {
        this(null, null);
    }

    public DefaultRequestPathInfo(final String resolutionPath, final String resolutionPathInfo) {

        this.resourcePath = resolutionPath;

        String pathToParse = resolutionPathInfo;
        if (pathToParse == null) {
            pathToParse = "";
        }

        int firstSlash = pathToParse.indexOf('/');
        String pathToSplit;
        if (firstSlash < 0) {
            pathToSplit = pathToParse;
            this.suffix = null;
        } else {
            pathToSplit = pathToParse.substring(0, firstSlash);
            this.suffix = pathToParse.substring(firstSlash);
        }

        int lastDot = pathToSplit.lastIndexOf('.');

        if (lastDot <= 1) {
            this.selectorString = null;
            this.selectors = NO_SELECTORS;
        } else {
            String tmpSel = pathToSplit.substring(1, lastDot);
            this.selectors = tmpSel.split("\\.");
            this.selectorString = (this.selectors.length > 0 ? tmpSel : null);
        }

        this.extension = (lastDot + 1 < pathToSplit.length() ? pathToSplit.substring(lastDot + 1) : null);
    }

    private DefaultRequestPathInfo(String resourcePath, String selectorString, String extension, String suffix) {
        this.resourcePath = resourcePath;
        this.selectorString = selectorString;
        this.selectors = (selectorString != null ? selectorString.split("\\.") : NO_SELECTORS);

        this.extension = extension;
        this.suffix = suffix;
    }

    public String getResourcePath() {
        return this.resourcePath;
    }

    public String getExtension() {
        return this.extension;
    }

    public String getSelectorString() {
        return this.selectorString;
    }

    public String[] getSelectors() {
        return this.selectors;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public DefaultRequestPathInfo merge(RequestPathInfo baseInfo) {
        if (getExtension() == null) {
            return new DefaultRequestPathInfo(
                    getResourcePath(),
                    baseInfo.getSelectorString(),
                    baseInfo.getExtension(),
                    baseInfo.getSuffix());
        }

        return this;
    }

    public String toString() {
        return new StringBuilder().append("DefaultRequestPathInfo: path='").append(this.resourcePath).append("'")
                .append(", selectorString='").append(this.selectorString).append("'")
                .append(", extension='").append(this.extension).append("'")
                .append(", suffix='").append(this.suffix).append("'")
                .toString();
    }

    public static RequestPathInfo getRequestPathInfo(HttpServletRequest request, Resource resource) {
        if ((resource == null)
                || (resource.getResourceMetadata() != null
                && resource.getResourceMetadata().getResolutionPath() == null
                && resource.getResourceMetadata().getResolutionPathInfo() == null)) {

            if (request instanceof SlingHttpServletRequest) {
                return ((SlingHttpServletRequest) request).getRequestPathInfo();
            } else {
                return null;
            }

        } else {
            return new DefaultRequestPathInfo(resource);
        }
    }

    public static ResourceMetadata toResourceMetadata(RequestPathInfo requestPathInfo) {
        ResourceMetadata rm = new ResourceMetadata();
        if (requestPathInfo != null) {

            StringBuilder _rpi = new StringBuilder();
            String selectorString = requestPathInfo.getSelectorString();
            if (selectorString != null && selectorString.length() > 0) {
                if (!selectorString.startsWith(".")) {
                    _rpi.append(".");
                }
                _rpi.append(selectorString);
            }

            String extension = requestPathInfo.getExtension();
            if (extension != null && extension.length() > 0) {
                _rpi.append(".").append(extension);
            }

            String suffix = requestPathInfo.getSuffix();
            if (suffix != null && suffix.length() > 0) {
                _rpi.append(suffix);
            }

            rm.setResolutionPath(requestPathInfo.getResourcePath());
            rm.setResolutionPathInfo(_rpi.toString());
        }

        return rm;
    }
}
