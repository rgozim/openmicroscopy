/*
 * ome.tools.hibernate.ProxyCleanupFilter
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.tools.hibernate;

// Java imports
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Third-party libraries
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.Hibernate;
import org.hibernate.collection.AbstractPersistentCollection;

// Application-internal dependencies
import ome.api.StatefulServiceInterface;
import ome.model.IObject;
import ome.model.internal.Details;
import ome.util.ContextFilter;
import ome.util.Filterable;
import ome.util.Utils;

/**
 * removes all proxies from a return graph to prevent ClassCastExceptions and
 * Session Closed exceptions. You need to be careful with printing. Calling
 * toString() on an unitialized object will break before filtering is complete.
 * 
 * Note: we aren't setting the filtered collections here because it's "either
 * null/unloaded or filtered". We will definitiely filter here, so it would just
 * increase bandwidth.
 * 
 * @author Josh Moore &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:josh.moore@gmx.de">josh.moore@gmx.de</a>
 * @version 1.0 <small> (<b>Internal version:</b> $Rev$ $Date$) </small>
 * @since 1.0
 */
public class ProxyCleanupFilter extends ContextFilter {

    protected Map unloadedObjectCache = new IdentityHashMap();

    @Override
    public Filterable filter(String fieldId, Filterable f) {
        if (f == null) {
            return null;
        }

        if (unloadedObjectCache.containsKey(f)) {
            return (IObject) unloadedObjectCache.get(f);
        }

        // A proxy; send over the wire in altered form.
        if (!Hibernate.isInitialized(f)) {

            if (f instanceof IObject) {
                IObject proxy = (IObject) f;
                IObject unloaded = (IObject) Utils.trueInstance(f.getClass());
                unloaded.setId(proxy.getId()); // TODO is this causing a DB
                                                // hit?
                unloaded.unload();
                unloadedObjectCache.put(f, unloaded);
                return unloaded;
            } else if (f instanceof Details) {
                // Currently Details is only "known" non-IObject Filterable
                return super.filter(fieldId, ((Details) f).shallowCopy());
            } else {
                // TODO Here there's not much we can do. copy constructor?
                throw new RuntimeException(
                        "Bailing out. Don't want to set to a value to null.");
            }

            // Not a proxy; it will be serialized and sent over the wire.
        } else {

            // Any clean up here.
            return super.filter(fieldId, f);

        }

    }

    @Override
    public Collection filter(String fieldId, Collection c) {
        // is a proxy. null it. will be refilled by
        // MergeEventListener on re-entry.
        if (null == c || !Hibernate.isInitialized(c)) {
            return null;
        }

        Collection retVal = super.filter(fieldId, c);

        // ticket:61 : preventing Hibernate collection types from escaping.
        if (retVal instanceof AbstractPersistentCollection) {
            if (retVal instanceof Set) {
                retVal = new HashSet(retVal);
            } else if (retVal instanceof List) {
                retVal = new ArrayList(retVal);
            }

        } // end ticket:61

        return retVal;
    }

    @Override
    public Map filter(String fieldId, Map m) {

        if (null == m || !Hibernate.isInitialized(m)) {
            return null;
        }

        Map retVal = super.filter(fieldId, m);

        // ticket:61 : preventing Hibernate collection types from escaping.
        if (retVal instanceof AbstractPersistentCollection) {
            retVal = new HashMap(retVal);
        } // end ticket:61

        return retVal;
    }

    // TODO FIXME need to further test this.
    @Override
    protected void doFilter(String arg0, Object arg1) {
        if (arg1 instanceof Object[]) {
            Object[] arr = (Object[]) arg1;
            for (int i = 0; i < arr.length; i++) {
                arr[i] = this.filter(arg0, arr[i]);
            }
        }

    }

    /** wraps a filter for each invocation */
    public static class Interceptor implements MethodInterceptor {

        public Object invoke(MethodInvocation arg0) throws Throwable {
            Object result = arg0.proceed();

            if (!StatefulServiceInterface.class.isAssignableFrom(arg0.getThis()
                    .getClass())) {
                result = new ProxyCleanupFilter().filter(null, result);
            }

            return result;
        }
    }

}
