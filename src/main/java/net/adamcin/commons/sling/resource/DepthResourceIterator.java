package net.adamcin.commons.sling.resource;

import org.apache.sling.api.resource.Resource;

import java.util.*;

public class DepthResourceIterator implements Iterator<Resource> {
    static final Comparator<Resource> DEFAULT_COMPARATOR = new Comparator<Resource>() {
        public int compare(Resource left, Resource right) {
            return left.getName().compareTo(right.getName());
        }
    };

    final Comparator<Resource> resourceComparator;
    final int returnDepth;
    final String lowerBound;
    final String upperBound;
    
    final Stack<Resource> resources = new Stack<Resource>();
    final Stack<Iterator<Resource>> iterators = new Stack<Iterator<Resource>>();
    Iterator<Resource> currentIterator;
    
    Resource nextResource;

    public DepthResourceIterator(final Resource rootResource, int returnDepth, final String lowerBound, final String upperBound) {
        this(rootResource, returnDepth, lowerBound, upperBound, DEFAULT_COMPARATOR);
    }

    public DepthResourceIterator(final Resource rootResource, int returnDepth, final String lowerBound, final String upperBound, final Comparator<Resource> resourceComparator) {
        this.returnDepth = returnDepth;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.resourceComparator = resourceComparator;
        this.currentIterator = listChildren(rootResource);
        this.seek();
    }

    public boolean hasNext() {
        return nextResource != null;
    }

    public Resource next() {
        Resource temp = this.nextResource;
        seek();
        return temp;
    }

    public void remove() {
        throw new UnsupportedOperationException("remove");
    }
    
    private void seek() {
        Resource temp = null;
        this.nextResource = null;
        
        while (this.nextResource == null && (this.currentIterator.hasNext() || this.iterators.size() > 0)) {
            if (this.currentIterator.hasNext()) {
                temp = this.currentIterator.next(); 
                
                if (this.isWithinBounds(temp)) {
                    if (resources.size() + 1 < this.returnDepth) {
                        this.resources.push(temp);
                        this.iterators.push(this.currentIterator);
                        this.currentIterator = listChildren(temp);
                    } else {
                        this.nextResource = temp;
                    }
                }
                
            } else if (this.iterators.size() > 0) {
                this.resources.pop();
                this.currentIterator = this.iterators.pop();
            }
            // make sure this.currentIterator is set properly before leaving this loop
        }
       
    }

    protected Iterator<Resource> listChildren(Resource parent) {
        Iterator<Resource> simple = parent.listChildren();
        if (this.resourceComparator != null) {
            List<Resource> sortedList = new ArrayList<Resource>();

            while (simple.hasNext()) {
                sortedList.add(simple.next());
            }

            Collections.sort(sortedList, this.resourceComparator);

            return sortedList.iterator();
        } else {
            return simple;
        }
    }
    
    protected boolean isWithinBounds(Resource currentResource) {
        StringBuilder sb = new StringBuilder();
        for (Resource resource : this.resources) {
            sb.append(resource.getName());
        }
        
        sb.append(currentResource.getName());
        String name = sb.toString();
        
        int lv = 0;
        if (lowerBound != null) {
            if (lowerBound.length() > name.length()) {
                lv = lowerBound.substring(0, name.length()).compareTo(name);
            } else {
                lv = lowerBound.compareTo(name.substring(0, lowerBound.length()));
            }
        }
        
        int rv = 0;
        if (upperBound != null) {
            if (upperBound.length() > name.length()) {
                rv = name.compareTo(upperBound.substring(0, name.length()));
            } else {
                rv = name.substring(0, upperBound.length()).compareTo(upperBound);
            }
        }

        return lv <= 0 && rv <= 0;
    }

}
