/*
 * ome.api.IQuery
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.api;

// Java imports
import java.util.List;

// Third-party libraries

// Application-internal dependencies
import ome.annotations.NotNull;
import ome.conditions.ApiUsageException;
import ome.conditions.ValidationException;
import ome.model.IObject;
import ome.parameters.Filter;
import ome.parameters.Parameters;
import ome.parameters.QueryParameter;
import ome.parameters.Page;

/**
 * Provides methods for directly querying object graphs. As far as is possible,
 * IQuery should be considered the lowest level DB-access (SELECT) interface.
 * Unlike the {@link ome.api.IUpdate} interface, using other methods will most
 * likely not leave the database in an inconsitent state, but may provide stale
 * data in some situations.
 * 
 * By convention, all methods that begin with <code>get</code> will never
 * return a null or empty {@link java.util.Collection}, but instead will throw
 * a {@link ome.conditions.ValidationException}.
 * 
 * @author Josh Moore &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:josh.moore@gmx.de">josh.moore@gmx.de</a>
 * @version 3.0 <small> (<b>Internal version:</b> $Rev$ $Date$) </small>
 * @since 3.0
 */
public interface IQuery extends ServiceInterface {

    // ~ Simple Lookups
    // =========================================================================

    /**
     * lookup an entity by class and id. If no such object exists, an exception
     * will be thrown.
     * 
     * @param klass
     *            the type of the entity. Not null.
     * @param id
     *            the entity's id
     * @return an initialized entity
     * @throws ValidationException
     *             if the id doesn't exist.
     */
    <T extends IObject> T get(@NotNull
    Class<T> klass, long id) throws ValidationException;

    /**
     * lookup an entity by class and id. If no such objects exists, return a
     * null.
     * 
     * @param klass
     *            klass the type of the entity. Not null.
     * @param id
     *            the entity's id
     * @return an initialized entity or null if id doesn't exist.
     */
    <T extends IObject> T find(@NotNull
    Class<T> klass, long id);

    /**
     * lookup all entities that belong to this class and match filter.
     * 
     * @param klass
     *            entity type to be searched. Not null.
     * @param filter
     *            filters the result set. Can be null.
     * @return a collection if initialized entities or an empty List if none
     *         exist.
     */
    <T extends IObject> List<T> findAll(@NotNull
    Class<T> klass, Filter filter);

    // ~ Example-based Queries
    // =========================================================================

    /**
     * search based on provided example entity. The example entity should
     * <em>uniquely</em> specify the entity or an exception will be thrown.
     * 
     * Note: findByExample does not operate on the <code>id</code> field. For
     * that, use {@link #find(Class, long)}, {@link #get(Class, long)},
     * {@link #findByQuery(String, Parameters)}, or
     * {@link #findAllByQuery(String, Parameters)}
     * 
     * @param example
     *            Non-null example object.
     * @return Possibly null IObject result.
     * @throws ApiUsageException
     *             if more than one result is return.
     */
    <T extends IObject> T findByExample(@NotNull
    T example) throws ApiUsageException;

    /**
     * search based on provided example entity. The returned entities will be
     * limited by the {@link Filter} object.
     * 
     * Note: findAllbyExample does not operate on the <code>id</code> field.
     * For that, use {@link #find(Class, long)}, {@link #get(Class, long)},
     * {@link #findByQuery(String, Parameters)}, or
     * {@link #findAllByQuery(String, Parameters)}
     * 
     * 
     * @param example
     *            Non-null example object.
     * @param filter
     *            filters the result set. Can be null.
     * @return Possibly empty List of IObject results.
     */
    <T extends IObject> List<T> findAllByExample(@NotNull
    T example, Filter filter);

    // ~ String-field-Queries
    // =========================================================================

    /**
     * search a given field matching against a String. Method does <em>not</em>
     * allow for case sensitive or insensitive searching since this is
     * essentially a lookup. The existence of more than one result will result
     * in an exception.
     * 
     * @param klass
     *            type of entity to be searched
     * @param field
     *            the name of the field, either as simple string or as public
     *            final static from the entity class, e.g.
     *            {@link ome.model.containers.Project#NAME}
     * @param value
     *            String used for search.
     * @return found entity or possibly null.
     * @throws ome.conditions.ApiUsageException
     *             if more than one result.
     */
    <T extends IObject> T findByString(@NotNull
    Class<T> klass, @NotNull
    String field, String value) throws ApiUsageException;

    /**
     * search a given field matching against a String. Method allows for case
     * sensitive or insensitive searching using the (I)LIKE comparators. Result
     * set will be reduced by the {@link Filter} instance.
     * 
     * @param klass
     *            type of entity to be searched. Not null.
     * @param field
     *            the name of the field, either as simple string or as public
     *            final static from the entity class, e.g.
     *            {@link ome.model.containers.Project#NAME}. Not null.
     * @param value
     *            String used for search. Not null.
     * @param caseSensitive
     *            whether to use LIKE or ILIKE
     * @param filter
     *            filters the result set. Can be null.
     * @return A list (possibly empty) with the results.
     */
    <T extends IObject> List<T> findAllByString(@NotNull
    Class<T> klass, @NotNull
    String field, String stringValue, boolean caseSensitive, Filter filter);

    // ~ Parameter-based Queries
    // =========================================================================
    // These methods use the ome.parameters package for representing
    // arbitrary (Integer, Long, String, IObject, etc.) parameters. We have
    // removed java.lang.Object based parameters from the API for cross-language
    // support.

    // We on't provide method with Page argument. Include in QueryParameters.

    // Available queries:
    // is class in hibernate? use parameters as field name.
    // see ClassnameQuery for documentation
    // is class of querysource?
    // lookup in hibernate named query
    // lookup in database
    // else: see StringQuerySource documentation.

    /**
     * executes the stored query with the given name. If a query with the name
     * cannot be found, an exception will be thrown.
     * 
     * The queryName parameter can be an actualy query String if the
     * StringQuerySource is configured on the server and the user running the
     * query has proper permissions.
     * 
     * @param queryName
     *            String identifier of the query to execute
     * @param parameters
     *            array of {@link QueryParameter}. Not null. The
     *            {@link QueryParameter#name} field maps to a field-name which
     *            is then matched against the {@link QueryParameter#value}
     * @return Possibly null IObject result.
     * @throws ValidationException
     */
    <T extends IObject> T findByQuery(@NotNull
    String queryName, Parameters parameters) throws ValidationException;

    /**
     * executes the stored query with the given name. If a query with the name
     * cannot be found, an exception will be thrown.
     * 
     * The queryName parameter can be an actualy query String if the
     * StringQuerySource is configured on the server and the user running the
     * query has proper permissions.
     * 
     * If a {@link Page} is desired, add it to the query parameters.
     * 
     * @param queryName
     *            String identifier of the query to execute. Not null.
     * @param parameters
     *            array of {@link QueryParameter}. The
     *            {@link QueryParameter#name} field maps to a field-name which
     *            is then matched against the {@link QueryParameter#value}
     * @return Possibly empty List of IObject results.
     */
    <T extends IObject> List<T> findAllByQuery(@NotNull
    String queryName, Parameters parameters);

    // ~ Other
    // =========================================================================

    /**
     * refreshes an entire {@link IObject} graph, recursive loading all data for
     * the managed instances in the graph from the database. If any non-managed
     * entities are detected (e.g. without ids), an {@link ApiUsageException}
     * will be thrown.
     * 
     * @param iObject
     *            Non-null managed {@link IObject} graph which should have all
     *            values re-assigned from the database
     * @return a similar {@link IObject} graph (with possible additions and
     *         deletions) which is in-sync with the database.
     * @throws ApiUsageException
     *             if any non-managed entities are found.
     */
    <T extends IObject> T refresh(@NotNull
    T iObject) throws ApiUsageException;
}
