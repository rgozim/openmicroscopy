/*
 * ome.parameters.Parameters
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.parameters;

// Java imports
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Third-party libraries

// Application-internal dependencies
import ome.conditions.ApiUsageException;

/**
 * container object for {@link QueryParameter} and {@link Filter} instances.
 * Parameters are provided to {@link IQuery} calls
 * {@link IQuery#findByQuery(String, Parameters)} and
 * {@link IQuery#findAllByQuery(String, Parameters)}, and define all named
 * query parameters needed for the call.
 * 
 * The public Strings available here are used throughout this class and should
 * also be used in query strings as named parameteres. For example, the field
 * {@link Parameters#ID} has the value "id", and a query which would like to use
 * the {@link Parameters#addId(Long)} method, should define a named parameter of
 * the form ":id".
 * 
 * @author <br>
 *         Josh Moore&nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:josh.moore@gmx.de"> josh.moore@gmx.de</a>
 * @version 3.0 <small> (<b>Internal version:</b> $Revision$ $Date$)
 *          </small>
 * @since 3.0-M2
 */
public class Parameters implements Serializable {

    /**
     * named parameter "id". Used in query strings as ":id"
     */
    public final static String ID = "id";

    /**
     * named parameter "ids". Used in query strings as ":ids"
     */
    public final static String IDS = "ids";

    /**
     * named parameter "class". Used in query strings as ":class"
     */
    public final static String CLASS = "class";

    /**
     * named parameter "options". Used in query strings as ":options"
     */
    public final static String OPTIONS = "options";

    /**
     * named parameter "algorithm". Used in query strings as ":algorithm"
     */
    public final static String ALGORITHM = "algorithm";

    /**
     * named parameter "ownerId". Used in query strings as ":ownerId"
     */
    public final static String OWNER_ID = "ownerId";

    /**
     * named parameter "groupId". Used in query strings as ":groupId"
     */
    public final static String GROUP_ID = "groupId";

    /**
     * single {@link Filter} instance for this Parameters. Is lazily-loaded by
     * the getter.
     */
    private Filter filter;

    /**
     * storage for the {@link QueryParameter query parameters}. For
     * serialization, {@link #writeObject(ObjectOutputStream)} and
     * {@link #readObject(ObjectInputStream)} have been over-written for a more
     * compact form.
     */
    private transient Map queryParameters = new HashMap();

    /**
     * default constructor. {@link Filter} is left null.
     * {@link QueryParameter queryParameters} collection is initialized to empty
     * {@link Collection}
     */
    public Parameters() {
    }

    /**
     * Filter constructor. Allows for the simple specification of "unique"
     * results. <code>new Parameters( new Filter().unique() ); </code> Filter
     * can be null since this is the default behavior anyway.
     */
    public Parameters(Filter filter) {
        this.filter = filter;
    }

    /**
     * copy constructor. {@link Filter} is taken from old instance and
     * {@link QueryParameter queryParameters} are merged.
     * 
     * @param old
     */
    public Parameters(Parameters old) {
        if (old == null) {
            return;
        }
        addAll(old);
    }

    /**
     * copy constructor. Merges {@link QueryParameter}s.
     */
    public Parameters(QueryParameter[] queryParameters) {
        addAll(queryParameters);
    }

    // ~ READ METHODS
    // =========================================================================
    /**
     * returns the Filter for this instance. If there was previously not a
     * Filter, a default will be instantiated.
     */
    public Filter getFilter() {
        if (filter == null) {
            filter = new Filter();
        }
        return filter;
    }

    /**
     * copies all QueryParameters to an array. Changes to this array do not
     * effect the internal QueryParameters.
     * 
     * @return array of QueryParameter.
     */
    public QueryParameter[] queryParameters() {
        return (QueryParameter[]) queryParameters.values().toArray(
                new QueryParameter[queryParameters.size()]);
    }

    /**
     * lookup a QueryParameter by name.
     */
    public QueryParameter get(String name) {
        return (QueryParameter) queryParameters.get(name);
    }

    /**
     * the Set of all names which would would return a non-null value from
     * {@link Parameters#get(String)}
     * 
     * @return a Set of Strings.
     */
    public Set keySet() {
        return new HashSet(queryParameters.keySet());
    }

    // ~ WRITE METHODS
    // =========================================================================

    public Parameters setFilter(Filter filter) {
        this.filter = filter;
        return this;
    }

    public Parameters add(QueryParameter parameter) {
        if (parameter == null) {
            throw new ApiUsageException("Parameter argument may not be null.");
        }

        queryParameters.put(parameter.name, parameter);
        return this;
    }

    /**
     * adds all the information from the passed in Parameters instance to this
     * instance. All {@link QueryParameter}s are added, and the {@link Filter}
     * instance is added <em>if</em> the current
     * 
     * @param old
     *            Non-null Parameters instance.
     * @return this
     */
    public Parameters addAll(Parameters old) {
        if (old == null) {
            throw new ApiUsageException("Parameters argument may not be null.");
        }

        if (old.filter != null) {
            if (filter != null) {
                throw new ApiUsageException(
                        "Two filters not allowed during copy constructor.");
            } else {
                filter = old.filter;
            }
        }

        return addAll(old.queryParameters());

    }

    /**
     * adds all the information from the passed in Parameters instance to this
     * instance. All {@link QueryParameter}s are added, and the {@link Filter}
     * instance is added <em>if</em> the current
     * 
     * @param queryParameters
     *            Non-null array of QueryParameters.
     * @return this
     */
    public Parameters addAll(QueryParameter[] queryParameters) {

        if (queryParameters == null) {
            throw new ApiUsageException(
                    "Array of QueryParameters may not be null.");
        }

        for (int i = 0; i < queryParameters.length; i++) {
            add(queryParameters[i]);
        }

        return this;

    }

    public Parameters addClass(Class klass) {
        addClass(CLASS, klass);
        return this;
    }

    public Parameters addClass(String name, Class value) {
        add(new QueryParameter(name, Class.class, value));
        return this;
    }

    public Parameters addBoolean(String name, Boolean value) {
        add(new QueryParameter(name, Boolean.class, value));
        return this;
    }

    public Parameters addInteger(String name, Integer value) {
        add(new QueryParameter(name, Integer.class, value));
        return this;
    }

    public Parameters addLong(String name, Long value) {
        add(new QueryParameter(name, Long.class, value));
        return this;
    }

    public Parameters addSet(String name, Set value) {
        add(new QueryParameter(name, Set.class, value));
        return this;
    }

    public Parameters addList(String name, List value) {
        add(new QueryParameter(name, List.class, value));
        return this;
    }

    public Parameters addMap(String name, Map value) {
        add(new QueryParameter(name, Map.class, value));
        return this;
    }

    public Parameters addString(String name, String value) {
        add(new QueryParameter(name, String.class, value));
        return this;
    }

    public Parameters addId(Long id) {
        add(new QueryParameter(ID, Long.class, id));
        return this;
    }

    public Parameters addIds(Collection ids) {
        add(new QueryParameter(IDS, Collection.class, ids));
        return this;
    }

    public Parameters addOptions(Map options) {
        addMap(OPTIONS, options);
        return this;
    }

    public Parameters addAlgorithm(String algo) {
        addString(ALGORITHM, algo);
        return this;
    }

    // ~ Serialization
    // =========================================================================
    private static final long serialVersionUID = 6428983610525830551L;

    private void readObject(ObjectInputStream s) throws IOException,
            ClassNotFoundException {
        s.defaultReadObject();
        int size = s.readInt();

        queryParameters = new HashMap();
        for (int i = 0; i < size; i++) {
            add((QueryParameter) s.readObject());
        }

    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();

        Set keySet = queryParameters.keySet();
        s.writeInt(keySet.size());

        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            s.writeObject(queryParameters.get(key));
        }

    }

}
