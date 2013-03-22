package net.adamcin.commons.sling.resource;

import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Map;

public class ResourceResolverWrapper extends SlingAdaptable implements ResourceResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceResolverWrapper.class);

    protected ResourceResolver wrappedResolver;
    
    public ResourceResolverWrapper(final ResourceResolver wrappedResolver) {
        this.wrappedResolver = wrappedResolver;
    }
    
    public ResourceResolver getResourceResolver() {
        return this.wrappedResolver;
    }
    
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        AdapterType selfAdapter = super.adaptTo(type);
        if (selfAdapter != null) {
            return selfAdapter;
        } else {
            return getResourceResolver().adaptTo(type);
        }
    }

    public Resource resolve(HttpServletRequest request, String absPath) {
        return wrap(getResourceResolver().resolve(request, absPath));
    }

    public Resource resolve(String absPath) {
        return wrap(getResourceResolver().resolve(absPath));
    }

    @SuppressWarnings("deprecation")
    public Resource resolve(HttpServletRequest request) {
        return wrap(getResourceResolver().resolve(request));
    }

    public String map(String resourcePath) {
        return getResourceResolver().map(resourcePath);
    }

    public String map(HttpServletRequest request, String resourcePath) {
        return getResourceResolver().map(request, resourcePath);
    }

    public Resource getResource(String path) {
        return wrap(getResourceResolver().getResource(path));
    }

    public Resource getResource(Resource base, String path) {
        return wrap(getResourceResolver().getResource(base, path));
    }

    public String[] getSearchPath() {
        return getResourceResolver().getSearchPath();
    }

    public Iterator<Resource> listChildren(Resource parent) {
        return wrap(getResourceResolver().listChildren(parent));
    }

    public Iterator<Resource> findResources(String query, String language) {
        return wrap(getResourceResolver().findResources(query, language));
    }

    public Iterator<Map<String, Object>> queryResources(String query,
            String language) {
        return getResourceResolver().queryResources(query, language);
    }

    /**
     * Returns the <code>ResourceResolver</code> obtained by calling
     * <code>clone</code> on the {@link #getResourceResolver() wrapped
     * resource resolver} and wrapping it in a new instance of the used
     * <code>ResourceResolverWrapper</code>.
     *
     * This method assumes a constructor accepting a single
     * <code>ResourceResolver</code> argument. So this needs to be
     * overwritten if a different signature is expected.
     */
    public ResourceResolver clone(Map<String, Object> authenticationInfo) throws LoginException {
        final ResourceResolver clone = getResourceResolver().clone(authenticationInfo);
        try {
            final Constructor<? extends ResourceResolverWrapper> constructor =
                    this.getClass().getConstructor(ResourceResolver.class);
            return constructor.newInstance(clone);
        } catch (Exception e) {
            throw new LoginException("Failed to create cloned wrapper.", e);
        }
    }

    public boolean isLive() {
        return getResourceResolver().isLive();
    }

    public void close() {
        getResourceResolver().close();
    }

    public String getUserID() {
        return getResourceResolver().getUserID();
    }

    public Iterator<String> getAttributeNames() {
        return getResourceResolver().getAttributeNames();
    }

    public Object getAttribute(String name) {
        return getResourceResolver().getAttribute(name);
    }

    protected Resource wrap(Resource resource) {
        if (resource != null && getResourceResolver() == resource.getResourceResolver()) {
            return new ResolverOverridingResourceWrapper(resource);
        } else {
            return resource;
        }
    }

    protected Iterator<Resource> wrap(Iterator<Resource> resources) {
        if (resources != null) {
            return new ResolverOverridingResourceIterator(resources);
        }
        return null;
    }

    class ResolverOverridingResourceWrapper extends SlingAdaptableResourceWrapper {

        ResolverOverridingResourceWrapper(Resource resource) {
            super(resource);
        }

        @Override
        public ResourceResolver getResourceResolver() {
            return ResourceResolverWrapper.this;
        }

        @Override
        public Resource getParent() {
            return ResourceResolverWrapper.this.wrap(super.getParent());
        }

        @Override
        public Resource getChild(String relPath) {
            return ResourceResolverWrapper.this.wrap(super.getChild(relPath));
        }

        @Override
        public Iterator<Resource> listChildren() {
            return ResourceResolverWrapper.this.wrap(super.listChildren());
        }
    }

    class ResolverOverridingResourceIterator implements Iterator<Resource> {

        Iterator<Resource> wrapped;

        ResolverOverridingResourceIterator(Iterator<Resource> wrapped) {
            this.wrapped = wrapped;
        }

        public boolean hasNext() {
            return this.wrapped.hasNext();
        }

        public Resource next() {
            return ResourceResolverWrapper.this.wrap(this.wrapped.next());
        }

        public void remove() {
            this.wrapped.remove();
        }
    }
}
