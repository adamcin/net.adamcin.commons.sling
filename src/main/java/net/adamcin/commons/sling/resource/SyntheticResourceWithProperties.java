package net.adamcin.commons.sling.resource;

import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import java.util.HashMap;
import java.util.Map;

public class SyntheticResourceWithProperties extends SyntheticResource {

    private final Map<String, Object> properties = new HashMap<String, Object>();

    public SyntheticResourceWithProperties(ResourceResolver resourceResolver, String path, String resourceType) {
        super(resourceResolver, path, resourceType);
    }

    public SyntheticResourceWithProperties(ResourceResolver resourceResolver, ResourceMetadata rm, String resourceType) {
        super(resourceResolver, rm, resourceType);
    }

    public SyntheticResourceWithProperties(ResourceResolver resourceResolver, String path, String resourceType, Map<String, ? extends Object> properties) {
        super(resourceResolver, path, resourceType);
        if (properties != null) {
            this.properties.putAll(properties);
        }
    }

    public SyntheticResourceWithProperties(ResourceResolver resourceResolver, ResourceMetadata rm, String resourceType, Map<String, ? extends Object> properties) {
        super(resourceResolver, rm, resourceType);
        if (properties != null) {
            this.properties.putAll(properties);
        }
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == ValueMap.class) {
            return (AdapterType) new ValueMapDecorator(this.properties);
        } else {
            return super.adaptTo(type);
        }
    }
}
