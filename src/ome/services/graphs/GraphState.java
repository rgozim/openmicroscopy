/*
 *   $Id$
 *
 *   Copyright 2010 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.graphs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ome.model.IObject;
import ome.security.basic.CurrentDetails;
import ome.services.messages.EventLogMessage;
import ome.system.EventContext;
import ome.system.OmeroContext;
import ome.tools.hibernate.ExtendedMetadata;
import ome.tools.hibernate.QueryBuilder;
import ome.util.SqlAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;
import org.perf4j.StopWatch;
import org.perf4j.commonslog.CommonsLogStopWatch;

/**
 * Tree-structure containing all scheduled deletes which closely resembles the
 * tree structure of the {@link GraphSpec} itself. All ids of the intended
 * deletes will be collected in a preliminary phase. This is necessary since
 * intermediate deletes, may disconnect the graph, causing later deletes to fail
 * if they were solely based on the id of the root element.
 *
 * The {@link GraphState} instance can only be initialized with a graph of
 * initialized {@GraphSpec}s.
 *
 * To handle SOFT requirements, each new attempt to delete either a node or a
 * leaf in the subgraph is surrounded by a savepoint. Ids added during a
 * savepoint (or a sub-savepoint) or only valid until release is called, at
 * which time they are merged into the final view.
 *
 * @author Josh Moore, josh at glencoesoftware.com
 * @since Beta4.2.3
 */
public class GraphState implements GraphStep.Callback {

    private final static Log log = LogFactory.getLog(GraphState.class);
    /**
     * List of each individual {@link GraphStep} which this instance will
     * perform.
     */
    private final List<GraphStep> steps = new ArrayList<GraphStep>();

    /**
     * List of Maps of db table names to the ids actually deleted from that
     * table. The first entry of the list are the actual results. All later
     * elements are temporary views from some savepoint.
     *
     * TODO : refactor into {@link GraphStep}
     */
    private final LinkedList<Map<String, Set<Long>>> actualIds = new LinkedList<Map<String, Set<Long>>>();

    /**
     * Map from table name to the {@link IObject} class which will be deleted
     * for raising the {@link EventLogMessage}.
     */
    private final Map<String, Class<IObject>> classes = new HashMap<String, Class<IObject>>();

    private final GraphOpts opts = new GraphOpts();

    private final Session session;

    private final GraphStepFactory factory;

    private final SqlAction sql;

    /**
     * @param ctx
     *            Stored the {@link OmeroContext} instance for raising event
     *            during {@link #release(String)}
     * @param session
     *            non-null, active Hibernate session that will be used to delete
     *            all necessary items as well as lookup items for deletion.
     */
    public GraphState(GraphStepFactory factory, SqlAction sql, Session session, GraphSpec spec)
            throws GraphException {
        this.sql = sql;
        this.session = session;
        this.factory = factory;

        add(); // Set the actualIds size==1

        final GraphTables tables = new GraphTables();
        descend(spec, tables);

        final LinkedList<GraphStep> stack = new LinkedList<GraphStep>();
        parse(spec, tables, stack, null);

    }

    //
    // Initialization and id lookup
    //

    /**
     * Walk throw the sub-spec graph actually loading the ids which must be
     * scheduled for delete.
     *
     * @param spec
     * @param paths
     * @throws GraphException
     */
    private void descend(GraphSpec spec, GraphTables tables) throws GraphException {

        final List<GraphEntry> entries = spec.entries();

        for (int i = 0; i < entries.size(); i++) {

            final GraphEntry entry = entries.get(i);
            if (entry.skip()) { // after opts.push()
                if (log.isDebugEnabled()) {
                    log.debug("Skipping " + entry);
                }
                continue;
            }

            final GraphSpec subSpec = entry.getSubSpec();
            final long[][] results = spec.queryBackupIds(session, i, entry, null);
            tables.add(entry, results);
            if (subSpec != null) {
                if (results.length != 0) { // ticket:2823
                    descend(subSpec, tables);
                }
            }
        }
    }

    /**
     * Walk throw the sub-spec graph again, using the results provided to build
     * up a graph of {@link GraphStep} instances.
     */
    private void parse(GraphSpec spec, GraphTables tables,
            LinkedList<GraphStep> stack, long[] match)
            throws GraphException {

        final List<GraphEntry> entries = spec.entries();

        for (int i = 0; i < entries.size(); i++) {
            final GraphEntry entry = entries.get(i);
            final GraphSpec subSpec = entry.getSubSpec();

            Iterator<List<long[]>> it = tables.columnSets(entry, match);
            while (it.hasNext()) {
                List<long[]> columnSet = it.next();
                if (columnSet.size() == 0) {
                    continue;
                }

                // For the spec containers, we create a single step
                // per column-set.

                if (subSpec != null) {
                    GraphStep step = factory.create(steps.size(), stack, spec,
                            entry, null);

                    stack.add(step);
                    parse(subSpec, tables, stack, columnSet.get(0));
                    stack.removeLast();
                    this.steps.add(step);
                } else {

                    // But for the actual entries, we create a step per
                    // individual row.
                    for (long[] cols : columnSet) {
                        GraphStep step = factory.create(steps.size(), stack,
                                spec, entry, cols);
                        this.steps.add(step);
                    }

                }

            }
        }
    }

    //
    // Found and deleted Ids
    //

    /**
     * Return the total number of ids loaded into this instance.
     */
    public long getTotalFoundCount() {
        return steps.size();
    }

    /**
     * Return the total number of ids which were processed. This is calculated by
     * taking the only the completed savepoints into account.
     */
    public long getTotalProcessedCount() {
        int count = 0;
        for (Map.Entry<String, Set<Long>> entry : actualIds.getFirst()
                .entrySet()) {
            count += entry.getValue().size();
        }
        return count;
    }

    /**
     * Get the set of ids which were actually processed. See
     * {@link #addAll(String, Class, List)}
     */
    public Set<Long> getProcessedIds(String table) {
        Set<Long> set = lookup(table);
        if (set == null) {
            return new HashSet<Long>();
        } else {
            return Collections.unmodifiableSet(set);
        }
    }

    /**
     * Add the actually deleted ids to the current savepoint.
     *
     * It is critical that these ids are actually deleted and that any failure
     * for them to be removed will cause the entire transaction to fail (in
     * which case these ids will be ignored).
     *
     * @throws GraphException
     *             thrown if the {@link EventLogMessage} raised fails.
     */
    void addGraphIds(GraphStep step) throws GraphException {

        classes.put(step.table, step.iObjectType);
        Set<Long> set = lookup(step.table);
        set.add(step.id);

    }

    //
    // Iteration methods, used for actual deletes
    //

    /**
     *
     * @param step
     *            which step is to be invoked. Running a step multiple times is
     *            not supported.
     *
     * @return Any warnings which were noted during execution.
     * @throws GraphException
     *             Any errors which were caused during execution. Which
     *             execution states may be encountered is strongly tied to the
     *             definition of the specification and to the options which are
     *             passed in during initialization.
     */
    public String execute(int j) throws GraphException {

        final GraphStep step = steps.get(j);

        String msgOrNull = step.start(this);
        if (msgOrNull != null) {
            return msgOrNull; // EARLY EXIT
        }

        // Add this instance to the opts. Any method which then tries to
        // ask the opts for the current state will have an accurate view.
        step.push(opts);

        try {

            // Lazy initialization of parents.
            // To guarantee that finalization
            // happens (#3125, #3130), a special
            // marker is added and handled above.
            for (GraphStep parent : step.stack) {
                if (!parent.hasSavepoint()) {
                    parent.savepoint(this);
                }
            }
            step.savepoint(this);

            final QueryBuilder nullOp = optionalNullBuilder(step);
            final QueryBuilder qb = queryBuilder(step);

            try {

                // Phase 1: top-levels
                if (step.stack.size() <= 1) {
                    StopWatch swTop = new CommonsLogStopWatch();
                    step.spec.runTopLevel(session,
                            Arrays.<Long> asList(step.id));
                    swTop.stop("omero.delete.top." + step.id);
                }

                // Phase 2: NULL
                optionallyNullField(session, nullOp, step.id);

                // Phase 3: primary delete
                StopWatch swStep = new CommonsLogStopWatch();
                qb.param("id", step.id);
                Query q = qb.query(session);
                int count = q.executeUpdate();
                if (count > 0) {
                    addGraphIds(step);
                }
                logResults(step, count);
                swStep.lap("omero.delete." + step.table + "." + step.id);

                // Finalize.
                step.release(this);
                return "";

            } catch (ConstraintViolationException cve) {
                return handleConstraintViolation(step, cve);
            }

        } finally {
            step.pop(opts);
        }
    }

    private void logResults(final GraphStep step, final int count) {
        if (count > 0) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Graphd %s from %s: root=%s", step.id,
                        step.pathMsg, step.entry.getId()));
            }
        } else {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Missing delete %s from %s: root=%s",
                        step.id, step.pathMsg, step.entry.getId()));
            }
        }
    }

    /**
     * Method called when a {@link ConstraintViolationException} can be thrown.
     * This is both during
     * {@link #delete(Session, CurrentDetails, ExtendedMetadata, String, GraphState, List, GraphOpts)}
     * and
     * {@link #execute(Session, CurrentDetails, ExtendedMetadata, GraphState, List, GraphOpts)}
     * .
     *
     * @param session
     * @param opts
     * @param type
     * @param rv
     * @param cve
     */
    private String handleConstraintViolation(final GraphStep step,
            ConstraintViolationException cve) throws GraphException {

        // First, immediately rollback the current savepoint.
        step.rollback(this);

        String cause = "ConstraintViolation: " + cve.getConstraintName();
        String msg = String.format("Could not delete softly %s: %s due to %s",
                step.pathMsg, step.id, cause);

        // If this entry is "SOFT" then there's nothing
        // special we need to do.
        if (step.entry.isSoft()) {
            log.debug(msg);
            return "Skipping delete of " + step.table + ":" + step.id + "\n";
        }

        // Otherwise calculate if there is any "SOFT" setting about this
        // location in the graph, and clean up all of the related entries.
        // As we check down the stack, we can safely call rollback since
        // the only other option is to rollback the entire transaction.
        for (int i = step.stack.size() - 1; i >= 0; i--) {
            GraphStep parent = step.stack.get(i);
            parent.rollback(this);
            if (parent.entry.isSoft()) {
                disableRelatedEntries(parent);
                log.debug(String.format("%s. Handled by %s: %s", msg,
                        parent.pathMsg, parent.id));
                return cause;
            }
        }

        log.info(String.format("Failed to delete %s: %s due to %s",
                step.pathMsg, step.id, cause));
        throw cve;

    }

    /**
     * Finds all {@link GraphStep} instances in {@link #steps} which have the
     * given {@link GraphStep} argument in their {@link GraphStep#stack} which
     * amounts to being a descedent. All such instances are set to null in
     * {@link #steps} so that further processing cannot take place on them.
     */
    private void disableRelatedEntries(GraphStep parent) {
        for (GraphStep step : steps) {
            if (step == null || step.stack == null) {
                continue;
            } else if (step.stack.contains(parent)) {
                step.rollbackOnly();
            }
        }
    }

    private QueryBuilder optionalNullBuilder(final GraphStep step) {
        QueryBuilder nullOp = null;
        if (step.entry.isNull()) { // WORKAROUND see #2776, #2966
            // If this is a null operation, we don't want to delete the row,
            // but just modify a value. NB: below we also prevent this from
            // being raised as a delete event. TODO: refactor out to Op
            nullOp = new QueryBuilder();
            nullOp.update(step.table);
            nullOp.append("set relatedTo = null ");
            nullOp.where();
            nullOp.and("relatedTo.id = :id");
        }
        return nullOp;
    }

    private void optionallyNullField(Session session,
            final QueryBuilder nullOp, Long id) {
        if (nullOp != null) {
            nullOp.param("id", id);
            Query q = nullOp.query(session);
            int updated = q.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Nulled " + updated + " Pixels.relatedTo fields");
            }
        }
    }

    private QueryBuilder queryBuilder(GraphStep step) {
        final QueryBuilder qb = new QueryBuilder();
        qb.delete(step.table);
        qb.where();
        qb.and("id = :id");
        if (!opts.isForce()) {
            permissionsClause(step.ec, qb);
        }
        return qb;
    }

    /**
     * Appends a clause to the {@link QueryBuilder} based on the current user.
     *
     * If the user is an admin like root, then nothing is appened, and any
     * delete is permissible. If the user is a leader of the current group, then
     * the object must be in the current group. Otherwise, the object must
     * belong to the current user.
     */
    public static void permissionsClause(EventContext ec, QueryBuilder qb) {
        if (!ec.isCurrentUserAdmin()) {
            if (ec.getLeaderOfGroupsList().contains(ec.getCurrentGroupId())) {
                qb.and("details.group.id = :gid");
                qb.param("gid", ec.getCurrentGroupId());
            } else {
                // This is only a regular user, then the object must belong to
                // him/her
                qb.and("details.owner.id = :oid");
                qb.param("oid", ec.getCurrentUserId());
            }
        }
    }

    //
    // Helpers
    //

    /**
     * Lookup and initialize if necessary a {@link Set<Long>} for the given
     * table.
     *
     * @param table
     * @return
     */
    private Set<Long> lookup(String table) {
        Set<Long> set = actualIds.getLast().get(table);
        if (set == null) {
            set = new HashSet<Long>();
            actualIds.getLast().put(table, set);
        }
        return set;
    }

    //
    // Callback methods
    //

    public Class<IObject> getClass(String key) {
        return classes.get(key);
    }

    public void add() {
        actualIds.add(new HashMap<String, Set<Long>>());
    }

    public int size() {
        return actualIds.size();
    }

    public Iterable<Map.Entry<String, Set<Long>>> entrySet() {
        return actualIds.getLast().entrySet();
    }

    public int collapse(boolean keep) {
        int count = 0;
        final Map<String, Set<Long>> ids = actualIds.removeLast();
        if (keep) {
            // Update the next map up with the current values
            for (Map.Entry<String, Set<Long>> entry : ids.entrySet()) {
                String key = entry.getKey();
                Map<String, Set<Long>> last = actualIds.getLast();
                Set<Long> old = last.get(key);
                Set<Long> neu = entry.getValue();
                count += neu.size();
                if (old == null) {
                    last.put(key, neu);
                } else {
                    old.addAll(neu);
                }
            }
        } else {
            for (String key : ids.keySet()) {
                Set<Long> old = ids.get(key);
                count += old.size();
            }
        }
        return count;
    }

    public void savepoint(String savepoint) {

        sql.createSavepoint(savepoint);
        log.debug(String.format("Enter savepoint %s: new depth=%s",
                savepoint,
                actualIds.size()));

    }

    public void release(String savepoint, int count) throws GraphException {

        if (actualIds.size() == 0) {
            throw new GraphException("Release at depth 0!");
        }

        sql.releaseSavepoint(savepoint);

        log.debug(String.format(
                "Released savepoint %s with %s ids: new depth=%s", savepoint,
                count, actualIds.size()));

    }

    public void rollback(String savepoint, int count) throws GraphException {

        if (actualIds.size() == 0) {
            throw new GraphException("Release at depth 0!");
        }

        sql.rollbackSavepoint(savepoint);

        log.debug(String.format(
                "Rolled back savepoint %s with %s ids: new depth=%s",
                savepoint, count, actualIds.size()));

    }

    //
    // Misc
    //

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("\n");
        for (int i = 0; i < steps.size(); i++) {
            GraphStep step = steps.get(i);
            sb.append(i);
            sb.append(":");
            if (step == null) {
                sb.append("null");
            } else {
                sb.append(step.pathMsg);
                sb.append("==>");
                sb.append(step.id);
                sb.append(" ");
                sb.append("[");
                sb.append(step.stack);
                sb.append("]");
                sb.append("\n");
            }
        }
        return sb.toString();
    }

}
