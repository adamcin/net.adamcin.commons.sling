package net.adamcin.commons.sling.resource;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class DefaultRedirectResource extends SyntheticResource {

    public static final String RT_SLING_REDIRECT = "sling:redirect";
    public static final String PROP_SLING_TARGET = "sling:target";
    public static final String PROP_SLING_STATUS = "sling:status";

    private final Map<String, Object> values;

    private Resource wrappedResource;

    protected DefaultRedirectResource(ResourceResolver resourceResolver, String path, String target, int status) {
        super(resourceResolver, path, RT_SLING_REDIRECT);
        HashMap<String, Object> props = new HashMap<String, Object>();
        props.put(PROP_SLING_TARGET, target);
        props.put(PROP_SLING_STATUS, Integer.valueOf(status));

        this.values = Collections.unmodifiableMap(props);
    }

    protected DefaultRedirectResource(Resource resource, String target, int status) {
        this(resource.getResourceResolver(), resource.getPath(), target, status);
        this.wrappedResource = resource;
    }

    public String toString() {
        return super.toString() + ", values=" + this.values;
    }

    public Resource getWrappedResource() {
        return this.wrappedResource;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == Resource.class) {
            return (AdapterType) getWrappedResource();
        } else if (type == ValueMap.class) {
            return (AdapterType) new ValueMapDecorator(this.values);
        } else if (type == Map.class) {
            return (AdapterType) this.values;
        } else {
            return super.adaptTo(type);
        }
    }

    public static Resource redirectTo(Resource resource, String targetPath) {
        if (resource == null) {
            throw new NullPointerException("resource");
        }

        return new DefaultRedirectResource(resource, targetPath, HttpServletResponse.SC_MOVED_PERMANENTLY);
    }

    public static Resource redirectTo(ResourceResolver resourceResolver, String fromPath, String targetPath) {
        if (resourceResolver == null) {
            throw new NullPointerException("resourceResolver");
        }

        return new DefaultRedirectResource(resourceResolver, fromPath, targetPath, HttpServletResponse.SC_MOVED_PERMANENTLY);
    }

    public static Resource tempRedirectTo(Resource resource, String targetPath) {
        if (resource == null) {
            throw new NullPointerException("resource");
        }

        return new DefaultRedirectResource(resource, targetPath, HttpServletResponse.SC_MOVED_TEMPORARILY);
    }

    public static Resource tempRedirectTo(ResourceResolver resourceResolver, String fromPath, String targetPath) {
        if (resourceResolver == null) {
            throw new NullPointerException("resourceResolver");
        }

        return new DefaultRedirectResource(resourceResolver, fromPath, targetPath, HttpServletResponse.SC_MOVED_TEMPORARILY);
    }
}
