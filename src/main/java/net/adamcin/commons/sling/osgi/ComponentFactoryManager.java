package net.adamcin.commons.sling.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Title:       Component Factory Manager
 * User:        Mark Adamcin
 * Created:     1.00, Aug 26, 2010 / 5:51:11 PM
 * <p/>
 * DESCRIPTION
 * -----------------------------------------------------------------------------
 * This is a formalized encapsulation of some logic that I saw being used in a couple
 * places in CQ that really ought to be part of the general OSGi toolbox.
 *
 * In essence, this is a shortcut for making use of SCR component factories in your
 * OSGi services. Component Factories are a way to make your private implementations
 * more extensible to other bundles.
 *
 * An excellent example of this is the CQ QueryBuilder service, which allows non-CQ
 * bundles to register their own PredicateEvaluator classes through OSGi, thus
 * providing a facility to handle queries with more client-specific business logic in
 * the future.
 *
 * I created this class for use by the QueryBuilderCommanderServlet, which has a
 * similar use-case in that it is intended to provide a single interface for business
 * rules that haven't been defined yet.
 *
 * The <ComponentT> parameter refers to the type of component produced by the
 * factory service.
 * -----------------------------------------------------------------------------
 */
public class ComponentFactoryManager<ComponentT> {
    private static final Logger log = LoggerFactory.getLogger(ComponentFactoryManager.class);

    /**
     * We need this Class reference to provide the factory className for use in the service query
     */
    private final Class<ComponentT> componentClass;

    /**
     * The bundle context serves as the OSGi interface
     */
    private final BundleContext bundleContext;

    /**
     * To create an object instance, a ServiceReference must be gotten. When the object is no longer needed, we must be
     * able to unget the ServiceReference. Thus, we maintain a mapping between object instances and their associated
     * ServiceReferences
     */
    private final Map<Object, ServiceReference> refMap = new HashMap<Object, ServiceReference>();

    /**
     * To create an object instance, a ComponentInstance must be created. When the object is no longer needed, we must
     * be able to dispose the ComponentInstance. Thus, we maintain a mapping between object instances and their
     * associated ComponentInstances
     */
    private final Map<Object, ComponentInstance> instanceMap = new HashMap<Object, ComponentInstance>();

    private static final String FACTORY_PROPERTY = "component.factory";

    /**
     * I can't figure out how to parameterize it and the class argument at the same time, with out forcing the developer
     * to do it explicitly. If you're lazy like me, using the factory method in OsgiUtils.
     * @param bundleContext
     * @param componentClass
     */
    public ComponentFactoryManager(BundleContext bundleContext, Class<ComponentT> componentClass) {
        this.bundleContext = bundleContext;
        this.componentClass = componentClass;
    }

    /**
     * Returns a new instance of the OSGi Component speficied by the factory value "<className>/<factoryName>", where
     * className refers to the ComponentT of the Manager.
     * @param factoryName Identifier of the specific factory service, not including the component class name
     * @return The component object
     */
    public ComponentT newInstance(String factoryName) {
        return this.newInstance(factoryName, null);
    }

    /**
     * Returns a new instance of the OSGi Component specified by the factory value "<className>/<factoryName>", where
     * className refers to the ComponentT of the Manager. The properties argument is added to the existing component
     * configuration values.
     * @param factoryName Identifier of the specific factory service, not including the component class name
     * @param properties Additional configuration properties (may be null)
     * @return The component object
     */
    @SuppressWarnings("unchecked")
    public ComponentT newInstance(String factoryName, Dictionary<?,?> properties) {

        // Handle a null properties argument by replacing it with an empty Properties object
        if(properties == null){
            properties = new Properties();
        }

        // We will use the BundleContext to query for factory services matching the <className>/<factoryName> pattern
        ServiceReference[] refs = this.getServiceReferences(factoryName);

        // If we don't find a service reference, there is no way to create a component instance
        if ((refs == null) || (refs.length == 0)) {
            log.debug("refs is null or empty");
            return null;
        }

        // Take the first one
        ServiceReference ref = refs[0];
        
        // If there are any others, unget them.
        if(refs.length > 1){
            for(int i = 1; i < refs.length; i++){
                this.bundleContext.ungetService(refs[i]);
            }
        }

        // Get the service indicated by the reference, which is going to be a ComponentFactory, because we queried for
        // (component.factory=<className>/<factoryName>)
        ComponentFactory factory = (ComponentFactory)this.bundleContext.getService(ref);

        // Create a new ComponentInstance from the factory
        ComponentInstance instance = factory.newInstance(properties);

        // Try to get the instance object from the ComponentInstance
        Object component = instance.getInstance();

        // If no object is returned, we should still dispose the ComponentInstance and unget the ServiceReference
        if (component == null) {
            log.error("Unable to get " + componentClass.getSimpleName() + " instance: " + factoryName);
            instance.dispose();
            this.bundleContext.ungetService(ref);
        }
        // Otherwise, keep track of the ServiceReference and ComponentInstance for future disposal
        else {

            // Synchronize on the refMap object for thread safety
            synchronized (this.refMap) {
                this.refMap.put(component, ref);
                this.instanceMap.put(component, instance);
            }
        }

        // Return the object instance
        return (ComponentT) component;
    }

    /**
     * After the component object is no longer needed, it's associated ServiceReference and ComponentInstance should be
     * released and removed from the manager's HashMaps.
     * @param component The component object previously returned by the newInstance method
     */
    public void release(ComponentT component) {

        // We use the refMap object for synchronization, as HashMaps are not thread safe, and services that use this
        // ComponentFactoryManager will likely be used by many threads
        synchronized (this.refMap) {

            // Dispose the ComponentInstance
            if (this.instanceMap.containsKey(component)) {
                this.instanceMap.get(component).dispose();
                this.instanceMap.remove(component);
            }

            // Unget the ServiceReference
            if (this.refMap.containsKey(component)) {
                this.bundleContext.ungetService(this.refMap.get(component));
                this.refMap.remove(component);
            }
        }
    }

    /**
     * Release all component instances and unget all service references
     */
    public void releaseAll() {
        synchronized (this.refMap) {
            for (ComponentInstance instance : this.instanceMap.values()) {
                instance.dispose();
            }

            for (ServiceReference ref : this.refMap.values()) {
                this.bundleContext.ungetService(ref);
            }

            this.instanceMap.clear();
            this.refMap.clear();
        }
    }

    /**
     * Convenience method that returns only the properties map of the specified factory
     * @param factoryName
     * @return
     */
    public Map<String, Object> getFactoryProperties(String factoryName){
        Map<String,Map<String, Object>> factories = this.getAllFactoryProperties(factoryName);
        return factories.get(factoryName);
    }

    /**
     * Iterates over the array of ServiceReference objects returned by the query and builds a HashMap
     * whose key is the factoryName, and whose value is another Map of property keys and values.
     * @param factoryFilter
     * @return
     */
    public Map<String, Map<String, Object>> getAllFactoryProperties(String factoryFilter){
        HashMap<String, Map<String, Object>> factories = new HashMap<String,Map<String,Object>>();

        ServiceReference[] refs = this.getServiceReferences(factoryFilter);

        // If we don't find a service reference, there is no way to create a component instance
        if ((refs == null) || (refs.length == 0)) {
            log.debug("refs is null or empty");
            return factories;
        }

        for(ServiceReference ref : refs){
            Map<String, Object> props = new HashMap<String, Object>();
            for(String key : ref.getPropertyKeys()){
                props.put(key, ref.getProperty(key));
            }
            String factoryName = ((String) ref.getProperty(FACTORY_PROPERTY)).substring(componentClass.getName().length() + 1);
            factories.put(factoryName, props);
            this.bundleContext.ungetService(ref);
        }

        return factories;
    }

    /**
     * This method encapsulates the bundleContext service query
     * @param factoryFilter
     * @return
     */
    private ServiceReference[] getServiceReferences(String factoryFilter){
        if(factoryFilter == null){
            factoryFilter = "*";
        }

        // Here is the query. It uses LDAP-style query syntax, which is apparently still popular as a programming
        // language style.
        String serviceReferenceQuery = "(" + FACTORY_PROPERTY + "=" + componentClass.getName() + '/' + factoryFilter + ')';
        log.debug(serviceReferenceQuery);

        try {
            // Here we execute the query
            return this.bundleContext.getServiceReferences(ComponentFactory.class.getName(), serviceReferenceQuery);
        }
        catch (InvalidSyntaxException e) {
            // You did it wrong.
            log.error("Somehow the query syntax was invalid: " + serviceReferenceQuery, e);
            return null;
        }
    }

    /**
     * Convenient static factory method
     * @param bundleContext
     * @param componentClass
     * @param <ComponentType>
     * @return
     */
    public static <ComponentType> ComponentFactoryManager<ComponentType> newManagerInstance(BundleContext bundleContext, Class<ComponentType> componentClass){
        return new ComponentFactoryManager<ComponentType>(bundleContext, componentClass);
    }
}
