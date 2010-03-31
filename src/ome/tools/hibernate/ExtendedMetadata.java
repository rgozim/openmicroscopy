/*
 *   $Id$
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.tools.hibernate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ome.annotations.RevisionDate;
import ome.annotations.RevisionNumber;
import ome.conditions.ApiUsageException;
import ome.conditions.InternalException;
import ome.model.IAnnotated;
import ome.model.IObject;
import ome.model.internal.Permissions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.EntityMode;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.Type;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * extension of the model metadata provided by {@link SessionFactory}. During
 * construction, the metadata is created and cached for later use.
 * 
 * @author Josh Moore, josh.moore at gmx.de
 * @version $Revision$, $Date$
 * @see SessionFactory
 * @since 3.0-M3
 */
@RevisionDate("$Date$")
@RevisionNumber("$Revision$")
public class ExtendedMetadata implements ApplicationListener {

    private final static Log log = LogFactory.getLog(ExtendedMetadata.class);

    private final Map<String, Locks> locksHolder = new HashMap<String, Locks>();

    private final Map<String, String[][]> lockedByHolder = new HashMap<String, String[][]>();

    private final Map<String, Immutables> immutablesHolder = new HashMap<String, Immutables>();

    private final Map<String, String> collectionCountHolder = new HashMap<String, String>();

    private final Map<String, Class<IObject>> targetHolder = new HashMap<String, Class<IObject>>();

    private final Set<Class<IAnnotated>> annotationTypes = new HashSet<Class<IAnnotated>>();

    private boolean initialized = false;

    // NOTES:
    // TODO we could just delegate to sf and implement the same interface.
    // TOTEST
    // will need to get collection items out. // no filtering.
    // will also need to do the same for checkIfNeedLock once we have
    // a collection valued (non-association) type. (does that exist??)
    // will also need to handle components if we have any other than details.
    // Doesn't handle ComponentTypes

    /**
     * Listener method which waits for a {@link ContextRefreshedEvent} and then
     * extracts the {@link SessionFactory} from the {@link ApplicationContext}
     * and pases it to {@link #setSessionFactory(SessionFactory)}.
     */
    public void onApplicationEvent(
            org.springframework.context.ApplicationEvent event) {

        if (!(event instanceof ContextRefreshedEvent)) {
            return; // EARLY EXIT!!
        }

        ContextRefreshedEvent cre = (ContextRefreshedEvent) event;
        ApplicationContext ctx = cre.getApplicationContext();
        if (ctx.containsBean("sessionFactory")) {
            SessionFactory sessionFactory = (SessionFactory) ctx
                    .getBean("sessionFactory");
            setSessionFactory(sessionFactory);
        } else {
            log.warn("No session factory found. Cannot initialize");
        }
    }

    /**
     * Initializes the metadata needed by this instance.
     * 
     * @param sessionFactory
     * @see SessionFactory#getAllClassMetadata()
     */
    public void setSessionFactory(SessionFactory sessionFactory) {

        if (initialized) {
            return; // EARLY EXIT !!
        }

        log.info("Calculating ExtendedMetadata...");

        Map<String, ClassMetadata> m = sessionFactory.getAllClassMetadata();

        // do Locks() first because they are used during the
        // calculation of LockedBy()
        for (String key : m.keySet()) {
            ClassMetadata cm = m.get(key);
            locksHolder.put(key, new Locks(cm));
        }

        // now that all Locks() are available, determine LockedBy()
        for (String key : m.keySet()) {
            lockedByHolder.put(key, lockedByFields(key, m));
        }

        for (String key : m.keySet()) {
            ClassMetadata cm = m.get(key);
            immutablesHolder.put(key, new Immutables(cm));
        }

        Set<Class<IAnnotated>> anns = new HashSet<Class<IAnnotated>>();
        for (String key : m.keySet()) {
            ClassMetadata cm = m.get(key);
            Map<String, String> queries = countQueriesAndEditTargets(key,
                    lockedByHolder.get(key));
            collectionCountHolder.putAll(queries);

            // Checking classes, specifically for ITypes
            Class c = cm.getMappedClass(EntityMode.POJO);
            if (IAnnotated.class.isAssignableFrom(c)) {
                anns.add(c);
            }
        }
        annotationTypes.addAll(anns);
        initialized = true;
    }

    /**
     * Returns all the classes which implement {@link IAnnotated}
     */
    public Set<Class<IAnnotated>> getAnnotationTypes() {
        return Collections.unmodifiableSet(annotationTypes);
    }

    /**
     * walks the {@link IObject} argument <em>non-</em>recursively and gathers
     * all {@link IObject} instances which will be linkd to by the
     * creation or updating of the argument. (Previously this was called "locking"
     * since a flag was set on the object to mark it as linked, but this was 
     * removed in 4.2)
     * 
     * @param iObject
     *            A newly created or updated {@link IObject} instance which
     *            might possibly lock other {@link IObject IObjects}. A null
     *            argument will return an empty array to be checked.
     * @return A non-null array of {@link IObject IObjects} which will be linked to.
     */
    public IObject[] getLockCandidates(IObject iObject) {
        if (iObject == null) {
            return new IObject[] {};
        }

        Locks l = locksHolder.get(iObject.getClass().getName());
        return l.getLockCandidates(iObject);
    }

    /**
     * returns all class/field name pairs which may possibly link to an object
     * of type <code>klass</code>.
     * 
     * @param klass
     *            Non-null {@link Class subclass} of {@link IObject}
     * @return A non-null array of {@link String} queries which can be used to
     *         determine if an {@link IObject} instance can be unlocked.
     * @see Permissions.Flag#LOCKED
     */
    public String[][] getLockChecks(Class<? extends IObject> klass) {
        if (klass == null) {
            throw new ApiUsageException("Cannot proceed with null klass.");
        }

        String[][] checks = lockedByHolder.get(klass.getName());

        if (checks == null) {
            throw new ApiUsageException("Metadata not found for: "
                    + klass.getName());
        }

        return checks;
    }

    public String[] getImmutableFields(Class<? extends IObject> klass) {
        if (klass == null) {
            throw new ApiUsageException("Cannot proceed with null klass.");
        }

        Immutables i = immutablesHolder.get(klass.getName());
        return i.getImmutableFields();

    }

    private final static String field_msg = " is not a valid field for counting. Make sure you use "
            + "the single-valued (e.g. ImageAnnotation.IMAGE) and "
            + "not the collection-valued (e.g. Image.ANNOTATIONS) end.";

    /**
     * Returns the query for obtaining the number of collection items to a
     * particular instance. All such queries will return a ResultSet with rows
     * of the form: 0 (Long) id of the locked class 1 (Long) count of the
     * instances locking that class
     * 
     * @param field
     *            Field name as specified in the class.
     * @return String query. Never null.
     * @throws ApiUsageException
     *             if return value would be null.
     */
    public String getCountQuery(String field) throws ApiUsageException {
        String q = collectionCountHolder.get(field);
        if (q == null) {
            throw new ApiUsageException(field + field_msg);
        }
        return q;
    }

    /**
     * Returns the {@link IObject} type which a given field points to. E.g.
     * getTargetType(ImageAnnotation.IMAGE) returns Image.class.
     */
    public Class<IObject> getTargetType(String field) throws ApiUsageException {
        Class<IObject> k = targetHolder.get(field);
        if (k == null) {
            throw new ApiUsageException(field + field_msg);
        }
        return k;
    }

    // ~ Helpers
    // =========================================================================

    /**
     * examines all model objects to see which fields contain a {@link Type}
     * which points to this class. Uses {@link #locksFields(Type[])} since this
     * is the inverse process.
     */
    private String[][] lockedByFields(String klass, Map<String, ClassMetadata> m) {
        if (m == null) {
            throw new InternalException("ClassMetadata map cannot be null.");
        }

        List<String[]> fields = new ArrayList<String[]>();

        for (String k : m.keySet()) {
            ClassMetadata cm = m.get(k);
            Type[] type = cm.getPropertyTypes();
            String[] names = cm.getPropertyNames();
            Locks inverse = locksHolder.get(k);
            for (int i = 0; i < inverse.size(); i++) {

                if (!inverse.include(i)) {
                    continue;
                }

                // this is an embedded component and must be treated
                // specially. specifically, that we cannot compare against
                // the top-level returnedClass name but rather against
                // each of the individual subtype returnedClass names.
                if (inverse.hasSubtypes(i)) {
                    for (int j = 0; j < inverse.numberOfSubtypes(i); j++) {
                        if (inverse.subtypeEquals(i, j, klass)) {
                            fields.add(new String[] { k,
                                    inverse.subtypeName(i, j) });
                        }
                    }
                }

                // no subtypes so can compare directly
                else if (klass.equals(type[i].getReturnedClass().getName())) {
                    fields.add(new String[] { k, names[i] });
                }
            }
        }
        return fields.toArray(new String[fields.size()][2]);
    }

    /**
     * Pre-builds all queries for checking the count of collections based on the
     * field names as defined in the ome.model.* classes.
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> countQueriesAndEditTargets(String type,
            String[][] lockedBy) {
        Map<String, String> queries = new HashMap<String, String>();

        for (int t = 0; t < lockedBy.length; t++) {
            String ltype = lockedBy[t][0];
            String lfield = lockedBy[t][1];

            String field_description = String.format("%s_%s", ltype, lfield);
            queries.put(field_description, String.format(
                    "select target.%s.id, count(target) "
                            + "from %s target group by target.%s.id", lfield,
                    ltype, lfield));

            try {
                targetHolder.put(field_description, (Class<IObject>) Class
                        .forName(type));
            } catch (Exception e) {
                throw new RuntimeException("Error getting class: " + ltype, e);
            }
        }

        return queries;
    }
}

/**
 * inner class which wraps the information (index number, path, etc) related to
 * what fields a particular object can lock. This is fairly complicated because
 * though the properties available are a simple array, some of those properties
 * can actually be embedded components, meaning that the value of the property
 * is the instance itself. In those cases (where {@link #hasSubtypes(int)} is
 * true, special logic must be implemented to retrieve the proper values.
 */
class Locks {
    private final ClassMetadata cm;

    private final int size;

    private int total = 0;

    private final boolean[] include;

    private final String[][] subnames;

    private final Type[][] subtypes;

    /**
     * examines all {@link Type types} for this class and stores pointers to
     * those fields which represent {@link IObject} instances. These fields may
     * need to be locked when an object of this type is created or updated.
     */
    Locks(ClassMetadata classMetadata) {

        this.cm = classMetadata;
        String[] name = cm.getPropertyNames();
        Type[] type = cm.getPropertyTypes();

        this.size = type.length;
        this.include = new boolean[size];
        this.subnames = new String[size][];
        this.subtypes = new Type[size][];

        for (int i = 0; i < type.length; i++) {
            if (type[i].isComponentType()
                    && ((ComponentType) type[i]).isEmbedded()) {
                EmbeddedComponentType embedded = (EmbeddedComponentType) type[i];
                String[] sub_name = embedded.getPropertyNames();
                Type[] sub_type = embedded.getSubtypes();
                List<String> name_list = new ArrayList<String>();
                List<Type> type_list = new ArrayList<Type>();
                for (int j = 0; j < sub_type.length; j++) {
                    if (IObject.class.isAssignableFrom(sub_type[j]
                            .getReturnedClass())) {
                        name_list.add(name[i] + "." + sub_name[j]);
                        type_list.add(sub_type[j]);
                    }
                }
                add(i, name_list.toArray(new String[name_list.size()]),
                        type_list.toArray(new Type[type_list.size()]));
            } else if (IObject.class.isAssignableFrom(type[i]
                    .getReturnedClass())) {
                add(i);
            }
        }

    }

    private void add(int i) {
        if (i >= size) {
            throw new IllegalArgumentException("size");
        }
        if (this.include[i] == true) {
            throw new IllegalStateException("set");
        }

        this.include[i] = true;
        this.subnames[i] = new String[] {};
        this.subtypes[i] = new Type[] {};
        total++;
    }

    private void add(int i, String[] paths, Type[] types) {
        if (i >= size) {
            throw new IllegalArgumentException("size");
        }
        if (paths == null) {
            throw new IllegalArgumentException("paths");
        }
        if (types == null) {
            throw new IllegalArgumentException("types");
        }
        if (paths.length != types.length) {
            throw new IllegalStateException("size");
        }
        if (this.include[i] == true) {
            throw new IllegalStateException("set");
        }

        if (paths.length > 0) {
            this.include[i] = true;
            this.subnames[i] = paths;
            this.subtypes[i] = types;
            total += paths.length;
        }
    }

    // ~ Main method
    // =========================================================================

    public IObject[] getLockCandidates(IObject o) {
        int idx = 0;
        IObject[] toCheck = new IObject[total()];
        Object[] values = cm.getPropertyValues(o, EntityMode.POJO);
        for (int i = 0; i < size(); i++) {
            if (!include(i)) {
                continue;
            }

            // this relation has subtypes and therefore is an embedded
            // component. This means that the value in values[] is the
            // instance itself. we will now have to acquire the actual
            // component values.
            if (hasSubtypes(i)) {
                for (int j = 0; j < numberOfSubtypes(i); j++) {
                    Object value = getSubtypeValue(i, j, o);
                    if (value != null) {
                        toCheck[idx++] = (IObject) value;
                    }
                }
            }

            // this is a regular relation. if the value is non null,
            // add it to the list of candidates.
            else if (values[i] != null) {
                toCheck[idx++] = (IObject) values[i];
            }
        }

        IObject[] retVal;
        retVal = new IObject[idx];
        System.arraycopy(toCheck, 0, retVal, 0, idx);
        return retVal;
    }

    // ~ Public
    // =========================================================================
    // public methods. know nothing about the arrays above. have only a
    // linear view of the contained values.

    /**
     * the total number of fields for this entity. The actual number of
     * {@link IObject} instances may vary since (1) some fields (like embedded
     * components) can possibly point to multiple instances. See
     * {@link #total()} for the final size and (2) some fields do not need to be
     * examined (Integers, e.g.). See {@link #include}
     */
    public int size() {
        return size;
    }

    /**
     * as opposed to {@link #size()}, the returns the actual number of fields
     * that will need to be checked.
     */
    public int total() {
        return total;
    }

    /**
     * returns true if this offset points to a field which may contain an
     * {@link IObject} instance
     */
    public boolean include(int i) {
        return include[i];
    }

    // ~ Subtypes
    // =========================================================================

    /**
     * returns true if this offset points to a field which is an embedded
     * component.
     */
    public boolean hasSubtypes(int i) {
        return include(i) && subtypes[i].length > 0;
    }

    /**
     * returns the number of subtypes for iterating over this secondary array.
     * If there are no subtypes, this method will return zero. Use
     * {@link #hasSubtypes(int)} to differentiate the two situations.
     */
    public int numberOfSubtypes(int i) {
        return hasSubtypes(i) ? subtypes[i].length : 0;
    }

    /**
     * uses the {@link ClassMetadata} for this {@link Locks} tp retrieve the
     * component value.
     */
    public Object getSubtypeValue(int i, int j, Object o) {
        return cm.getPropertyValue(o, subnames[i][j], EntityMode.POJO);
    }

    /**
     * returns true is the indexed subtype returns the same class type as the
     * klass argument.
     */
    public boolean subtypeEquals(int i, int j, String klass) {
        return klass.equals(subtypes[i][j].getReturnedClass().getName());
    }

    /** retrieves the full Hibernate path for this component field. */
    public String subtypeName(int i, int j) {
        return subnames[i][j];
    }
}

class Immutables {
    String[] immutableFields;

    EntityPersister ep;

    Immutables(ClassMetadata metadata) {
        if (metadata instanceof EntityPersister) {
            this.ep = (EntityPersister) metadata;
        } else {
            throw new IllegalArgumentException("Metadata passed to Immutables"
                    + " must be an instanceof EntityPersister, not "
                    + (metadata == null ? null : metadata.getClass()));
        }

        List<String> retVal = new ArrayList<String>();
        Type[] type = this.ep.getPropertyTypes();
        String[] name = this.ep.getPropertyNames();
        boolean[] up = this.ep.getPropertyUpdateability();

        for (int i = 0; i < type.length; i++) {

            // not updateable, so our work (for this type) is done.
            if (!up[i]) {
                retVal.add(name[i]);
            }

            // updateable, but maybe a component subtype is NOT.
            else if (type[i].isComponentType()
                    && ((ComponentType) type[i]).isEmbedded()) {
                EmbeddedComponentType embedded = (EmbeddedComponentType) type[i];
                String[] sub_name = embedded.getPropertyNames();
                Type[] sub_type = embedded.getSubtypes();
                List<String> name_list = new ArrayList<String>();
                List<Type> type_list = new ArrayList<Type>();
                for (int j = 0; j < sub_type.length; j++) {
                    // INCOMPLETE !!!
                }
            }
        }
        immutableFields = retVal.toArray(new String[retVal.size()]);
    }

    public String[] getImmutableFields() {
        return immutableFields;
    }

}
