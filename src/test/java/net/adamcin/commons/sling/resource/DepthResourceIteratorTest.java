package net.adamcin.commons.sling.resource;

import net.adamcin.commons.testing.sling.MockResource;
import net.adamcin.commons.testing.sling.MockResourceResolver;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;


public class DepthResourceIteratorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DepthResourceIteratorTest.class);
    private ResourceResolver resolver;
    private NonExistingResource dummy;
    private MockResource rootResource = new MockResource(this.dummy, "catalog");
    
    
    @Before
    public void setUp() {
        this.resolver = new MockResourceResolver();
        this.dummy = new NonExistingResource(this.resolver, "/apps/dummy/place");
        
        MockResource l100 = new MockResource(this.dummy, "00");
        rootResource.addChild(l100);
        
        MockResource l101 = new MockResource(this.dummy, "01");
        rootResource.addChild(l101);
        
        MockResource l102 = new MockResource(this.dummy, "02");
        rootResource.addChild(l102);
        
        MockResource l200 = new MockResource(this.dummy, "00");
        l100.addChild(l200);
        
        MockResource l201 = new MockResource(this.dummy, "01");
        l100.addChild(l201);
        
        MockResource l212 = new MockResource(this.dummy, "12");
        l101.addChild(l212);
    }
    
    @Test
    public void test1LevelCatalogIterator() {

        DepthResourceIterator it = new DepthResourceIterator(rootResource, 1, "000000", "999999");
        int count = 0;
        while (it.hasNext()) {
            count++;
            LOGGER.error("Resource Name: " + it.next().getName());
        }
        assertEquals("count should be 3", 3, count);
    }
    
    @Test
    public void test2LevelCatalogIterator() {

        DepthResourceIterator it = new DepthResourceIterator(rootResource, 2, "000000", "999999");
        int count = 0;
        while (it.hasNext()) {
            count++;
            LOGGER.error("Resource Name: " + it.next().getName());
        }
        assertEquals("count should be 3", 3, count);
    }


}
