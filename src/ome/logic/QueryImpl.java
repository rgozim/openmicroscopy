/*
 *   $Id$
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.logic;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import ome.annotations.RolesAllowed;
import ome.api.IQuery;
import ome.api.ServiceInterface;
import ome.api.local.LocalQuery;
import ome.conditions.ApiUsageException;
import ome.conditions.ValidationException;
import ome.model.IObject;
import ome.parameters.Filter;
import ome.parameters.Parameters;
import ome.services.query.Query;
import ome.services.search.FullText;
import ome.services.search.SearchValues;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.MatchMode;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides methods for directly querying object graphs.
 *
 * @author Josh Moore &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:josh.moore@gmx.de">josh.moore@gmx.de</a>
 * @version 3.0 <small> (<b>Internal version:</b> $Rev$ $Date$) </small>
 * @since 3.0
 *
 */
@Transactional(readOnly = true)
public class QueryImpl extends AbstractLevel1Service implements LocalQuery {

    protected Class<? extends Analyzer> analyzer;

    public void setAnalyzer(Class<? extends Analyzer> analyzer) {
        this.analyzer = analyzer;
    }

    public Class<? extends ServiceInterface> getServiceInterface() {
        return IQuery.class;
    }

    /**
     * Temporary WORKAROUND during the removal of HibernateTemplate to
     * let IQuery continue functioning.
     */
    private HibernateTemplate getHibernateTemplate() {
        return new HibernateTemplate(getSessionFactory(), false);
    }
    
    // ~ LOCAL PUBLIC METHODS
    // =========================================================================

    @RolesAllowed("user")
    @Transactional(readOnly = false)
    public boolean contains(Object obj) {
        return getHibernateTemplate().contains(obj);
    }

    @RolesAllowed("user")
    @Transactional(readOnly = false)
    public void evict(Object obj) {
        getHibernateTemplate().evict(obj);
    }

    @RolesAllowed("user")
    @Transactional(readOnly = false)
    public void clear() {
        getHibernateTemplate().clear();
    }

    @RolesAllowed("user")
    public void initialize(Object obj) {
        Hibernate.initialize(obj);
    }

    @RolesAllowed("user")
    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean checkType(String type) {
        ClassMetadata meta = getHibernateTemplate().getSessionFactory()
                .getClassMetadata(type);
        return meta == null ? false : true;
    }

    @RolesAllowed("user")
    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean checkProperty(String type, String property) {
        ClassMetadata meta = getHibernateTemplate().getSessionFactory()
                .getClassMetadata(type);
        String[] names = meta.getPropertyNames();
        for (int i = 0; i < names.length; i++) {
            // TODO: possibly with caching and Arrays.sort/search
            if (names[i].equals(property)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see LocalQuery#execute(HibernateCallback)
     */
    @RolesAllowed("user")
    @SuppressWarnings("unchecked")
    public <T> T execute(HibernateCallback callback) {
        return (T) getHibernateTemplate().execute(callback);
    }

    /**
     * @see ome.api.local.LocalQuery#execute(Query)
     */
    @RolesAllowed("user")
    @SuppressWarnings("unchecked")
    public <T> T execute(ome.services.query.Query<T> query) {
        return (T) getHibernateTemplate().execute(query);
    }

    // ~ INTERFACE METHODS
    // =========================================================================

    /**
     * @see ome.api.IQuery#get(java.lang.Class, long)
     * @see org.hibernate.Session#load(java.lang.Class, java.io.Serializable)
     * @DEV.TODO weirdness here; learn more about CGLIB initialization.
     */
    @RolesAllowed("user")
    @SuppressWarnings("unchecked")
    public IObject get(final Class klass, final long id)
            throws ValidationException {
        return (IObject) getHibernateTemplate().execute(
                new HibernateCallback() {
                    public Object doInHibernate(Session session)
                            throws HibernateException {

                        IObject o = null;
                        try {
                            o = (IObject) session.load(klass, id);
                        } catch (ObjectNotFoundException onfe) {
                            throw new ApiUsageException(
                                    String
                                            .format(
                                                    "The requested object (%s,%s) is not available.\n"
                                                            + "Please use IQuery.find to deteremine existance.\n",
                                                    klass.getName(), id));
                        }

                        Hibernate.initialize(o);
                        return o;

                    }
                });
    }

    /**
     * @see ome.api.IQuery#find(java.lang.Class, long)
     * @see org.hibernate.Session#get(java.lang.Class, java.io.Serializable)
     * @DEV.TODO weirdness here; learn more about CGLIB initialization.
     */
    @RolesAllowed("user")
    @SuppressWarnings("unchecked")
    public IObject find(final Class klass, final long id) {
        return (IObject) getHibernateTemplate().execute(
                new HibernateCallback() {
                    public Object doInHibernate(Session session)
                            throws HibernateException {

                        IObject o = (IObject) session.get(klass, id);
                        Hibernate.initialize(o);
                        return o;

                    }
                });
    }

    /**
     * @see ome.api.IQuery#getByClass(java.lang.Class)
     */
    @RolesAllowed("user")
    @SuppressWarnings("unchecked")
    public <T extends IObject> List<T> findAll(final Class<T> klass,
            final Filter filter) {
        if (filter == null) {
            return getHibernateTemplate().loadAll(klass);
        }

        return (List<T>) getHibernateTemplate().execute(
                new HibernateCallback() {
                    public Object doInHibernate(Session session)
                            throws HibernateException, SQLException {
                        Criteria c = session.createCriteria(klass);
                        c.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
                        parseFilter(c, filter);
                        return c.list();
                    }
                });
    }

    /**
     * @see ome.api.IQuery#findByExample(ome.model.IObject)
     */
    @RolesAllowed("user")
    @SuppressWarnings("unchecked")
    public <T extends IObject> T findByExample(final T example)
            throws ApiUsageException {
        return (T) getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session)
                    throws HibernateException {

                Criteria c = session.createCriteria(example.getClass());
                c.add(Example.create(example));
                return c.uniqueResult();

            }
        });
    }

    /**
     * @see ome.api.IQuery#findAllByExample(ome.model.IObject,
     *      ome.parameters.Filter)
     */
    @RolesAllowed("user")
    @SuppressWarnings("unchecked")
    public <T extends IObject> List<T> findAllByExample(final T example,
            final Filter filter) {
        return (List<T>) getHibernateTemplate().execute(
                new HibernateCallback() {
                    public Object doInHibernate(Session session)
                            throws HibernateException {

                        Criteria c = session.createCriteria(example.getClass());
                        c.add(Example.create(example));
                        parseFilter(c, filter);
                        return c.list();

                    }
                });
    }

    /**
     * @see ome.api.IQuery#findByString(java.lang.Class, java.lang.String,
     *      java.lang.String)
     */
    @RolesAllowed("user")
    @SuppressWarnings("unchecked")
    public <T extends IObject> T findByString(final Class<T> klass,
            final String fieldName, final String value)
            throws ApiUsageException {
        return (T) getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session)
                    throws HibernateException {

                Criteria c = session.createCriteria(klass);
                c.add(Expression.eq(fieldName, value));
                return c.uniqueResult();

            }
        });
    }

    /**
     * @see ome.api.IQuery#findAllByString(java.lang.Class, java.lang.String,
     *      java.lang.String, boolean, ome.parameters.Filter)
     */
    @RolesAllowed("user")
    @SuppressWarnings("unchecked")
    public <T extends IObject> List<T> findAllByString(final Class<T> klass,
            final String fieldName, final String value,
            final boolean caseSensitive, final Filter filter)
            throws ApiUsageException {
        return (List<T>) getHibernateTemplate().execute(
                new HibernateCallback() {
                    public Object doInHibernate(Session session)
                            throws HibernateException {

                        Criteria c = session.createCriteria(klass);
                        parseFilter(c, filter);

                        if (caseSensitive) {
                            c.add(Expression.like(fieldName, value,
                                    MatchMode.ANYWHERE));
                        } else {
                            c.add(Expression.ilike(fieldName, value,
                                    MatchMode.ANYWHERE));
                        }

                        return c.list();

                    }
                });
    }

    /**
     * @see ome.api.IQuery#findByQuery(java.lang.String,
     *      ome.parameters.Parameters)
     */
    @RolesAllowed("user")
    @SuppressWarnings("unchecked")
    public <T extends IObject> T findByQuery(String queryName, Parameters params)
            throws ValidationException {

        if (params == null) {
            params = new Parameters();
        }

        // specify that we should only return a single value if possible
        params.unique();

        Query<T> q = getQueryFactory().lookup(queryName, params);
        T result = null;
        try {
            result = execute(q);
        } catch (ClassCastException cce) {
            throw new ApiUsageException(
                    "Query named:\n\t"
                            + queryName
                            + "\n"
                            + "has returned an Object of type "
                            + cce.getMessage()
                            + "\n"
                            + "Queries must return IObjects when using findByQuery. \n"
                            + "Please try findAllByQuery for queries which return Lists.");
        } catch (NonUniqueResultException nure) {
            throw new ApiUsageException(
                    "Query named:\n\t"
                            + queryName
                            + "\n"
                            + "has returned more than one Object\n"
                            + "findByQuery must return a single value.\n"
                            + "Please try findAllByQuery for queries which return Lists.");
        }
        return result;

    }

    /**
     * @see ome.api.IQuery#findAllByQuery(java.lang.String,
     *      ome.parameters.Parameters)
     */
    @RolesAllowed("user")
    public <T extends IObject> List<T> findAllByQuery(String queryName,
            Parameters params) {
        Query<List<T>> q = getQueryFactory().lookup(queryName, params);
        return execute(q);
    }

    /**
     * @see ome.api.IQuery#findAllByFullText(java.lang.String,
     *      ome.parameters.Parameteres)
     */
    @RolesAllowed("user")
    @SuppressWarnings("unchecked")
    public <T extends IObject> List<T> findAllByFullText(final Class<T> type,
            final String query, final Parameters params) {
        if (analyzer == null) {
            throw new ApiUsageException(
                    "IQuery not configured for full text search.\n"
                            + "Please use ome.api.Search instead.");
        }
        return (List<T>) getHibernateTemplate().execute(
                new HibernateCallback() {

                    public Object doInHibernate(Session session)
                            throws HibernateException, SQLException {
                        SearchValues values = new SearchValues();
                        values.onlyTypes = Arrays.asList((Class) type);
                        values.copy(params);
                        FullText fullText = new FullText(values, query,
                                analyzer);
                        return fullText.doWork(session, null);
                    }
                });
    }

    // ~ Others
    // =========================================================================

    /**
     * @see IQuery#refresh(IObject)
     */
    public <T extends IObject> T refresh(T iObject) throws ApiUsageException {
        getHibernateTemplate().refresh(iObject);
        return iObject;
    }

    // ~ HELPERS
    // =========================================================================

    /**
     * responsible for applying the information provided in a
     * {@link ome.parameters.Filter} to a {@link org.hibernate.Critieria}
     * instance.
     */
    protected void parseFilter(Criteria c, Filter f) {
        // ticket:1232 - Reverting for 4.0
        if (f != null && f.offset != null) {
            c.setFirstResult(f.offset);
        } else {
            c.setFirstResult(0);
        }
        if (f != null && f.limit != null) {
            c.setMaxResults(f.limit);
        } else {
            c.setMaxResults(Integer.MAX_VALUE);
        }
    }

}
