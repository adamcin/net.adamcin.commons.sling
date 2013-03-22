package net.adamcin.commons.sling.resource;

import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Iterator;

public class SlingAdaptableResourceWrapper extends SlingAdaptable implements Resource {

    private final Resource resource;

    public SlingAdaptableResourceWrapper(Resource resource){
        this.resource = resource;
    }

    public Resource getResource() {
        return this.resource;
    }

    public String getPath() {
        return getResource().getPath();
    }

    public String getName() {
        return getResource().getName();
    }

    public Resource getParent() {
        return getResource().getParent();
    }

    public Resource getChild(String relPath) {
        return getResource().getChild(relPath);
    }

    public Iterator<Resource> listChildren() {
        return getResource().listChildren();
    }

    public ResourceMetadata getResourceMetadata() {
        return getResource().getResourceMetadata();
    }

    public ResourceResolver getResourceResolver() {
        return getResource().getResourceResolver();
    }

    public String getResourceType() {
        return getResource().getResourceType();
    }

    public String getResourceSuperType() {
        return getResource().getResourceSuperType();
    }

    public boolean isResourceType(String resourceType) {
        return getResource().isResourceType(resourceType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        AdapterType myAdapter = super.adaptTo(type);
        if (myAdapter != null) {
            return myAdapter;
        } else {
            return getResource().adaptTo(type);
        }
    }

    @Override
    public String toString() {
        String className = getClass().getSimpleName() == null ? getClass().getName() : getClass().getSimpleName();
        return className + ", type=" + getResourceType() + ", path=" + getPath() + ", resource=[" + getResource() + "]";
    }
}
