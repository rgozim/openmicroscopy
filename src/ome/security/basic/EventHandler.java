/*
 *   $Id$
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.security.basic;

<<<<<<< HEAD:components/server/src/ome/security/basic/EventHandler.java
import java.sql.Connection;
=======
>>>>>>> 6753f94... Performance : Removing `HibernateTemplate` usage to control overhead:components/server/src/ome/security/basic/EventHandler.java
import java.util.ArrayList;
import java.util.List;

import ome.api.StatefulServiceInterface;
import ome.conditions.InternalException;
import ome.model.meta.Event;
import ome.model.meta.EventLog;
import ome.system.EventContext;
import ome.tools.hibernate.SessionFactory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.transaction.IsolatedWork;
import org.hibernate.engine.transaction.Isolater;
import org.springframework.jdbc.core.simple.SimpleJdbcOperations;
<<<<<<< HEAD:components/server/src/ome/security/basic/EventHandler.java
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
=======
>>>>>>> 6753f94... Performance : Removing `HibernateTemplate` usage to control overhead:components/server/src/ome/security/basic/EventHandler.java
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.util.Assert;

/**
 * method interceptor responsible for login and creation of Events. Calls are
 * made to the {@link BasicSecuritySystem} provided in the
 * {@link EventHandler#EventHandler(BasicSecuritySystem, HibernateTemplate)
 * constructor}.
 * 
 * After the method is {@link MethodInterceptor#invoke(MethodInvocation)
 * invoked} various cleanup actions are performed and finally all credentials
 * all {@link BasicSecuritySystem#invalidateEventContext() cleared} from the
 * {@link Thread}.
 * 
 * 
 * 
 * @author Josh Moore &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:josh.moore@gmx.de">josh.moore@gmx.de</a>
 * @since 3.0
 */
public class EventHandler implements MethodInterceptor {

    private static Log log = LogFactory.getLog(EventHandler.class);

    protected final TransactionAttributeSource txSource;

    protected final BasicSecuritySystem secSys;

    protected final SessionFactory factory;

    protected final SimpleJdbcOperations isolatedJdbc, simpleJdbc;

    /**
     * only public constructor, used for dependency injection. Requires an
     * active {@link HibernateTemplate} and {@link BasicSecuritySystem}.
     * 
     * @param securitySystem
     *            Not null.
     * @param template
     *            Not null.
     */
    public EventHandler(SimpleJdbcOperations isolatedJdbc,
            SimpleJdbcOperations simpleJdbc,
            BasicSecuritySystem securitySystem, SessionFactory factory,
            TransactionAttributeSource txSource) {
        this.secSys = securitySystem;
        this.txSource = txSource;
        this.factory = factory;
        this.simpleJdbc = simpleJdbc;
        this.isolatedJdbc = isolatedJdbc;
    }

    /**
     * invocation interceptor for prepairing this {@link Thread} for execution
     * and subsequently reseting it.
     * 
     * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
     */
    public Object invoke(MethodInvocation arg0) throws Throwable {
        boolean readOnly = checkReadOnly(arg0);
        boolean stateful = StatefulServiceInterface.class.isAssignableFrom(arg0
                .getThis().getClass());

        secSys.loadEventContext(readOnly);
        // now the user can be considered to be logged in.
        EventContext ec = secSys.getEventContext();
        if (log.isInfoEnabled()) {
            log.info(String.format(" Auth:\tuser=%s,group=%s,event=%s(%s)", ec
                    .getCurrentUserId(), ec.getCurrentGroupId(), ec
                    .getCurrentEventId(), ec.getCurrentEventType()));
        }

        boolean failure = false;
        Object retVal = null;
        Session session = factory.getSession();
        try {
            secSys.enableReadFilter(session);
            retVal = arg0.proceed();
            saveLogs(readOnly, (SessionImplementor) session);
            return retVal;
        } catch (Throwable ex) {
            failure = true;
            throw ex;
        } finally {
            try {

                // on failure, we want to make sure that no one attempts
                // any further changes.
                if (failure) {
                    // TODO we should probably do some forced clean up here.
                }

                // stateful services should NOT be flushed, because that's part
                // of the state that should hang around.
                else if (stateful) {
                    // we don't want to do anything, really.
                }

                // read-only sessions should not have anything changed.
                else if (readOnly) {
                    if (session.isDirty()) {
                        if (log.isDebugEnabled()) {
                            log.debug("Clearing dirty session.");
                        }
                        session.clear();
                    }
                }

                // stateless services, don't keep their sesssions about.
                else {
                    session.flush();
                    if (session.isDirty()) {
                        throw new InternalException(
                                "Session is dirty. Cannot properly "
                                        + "reset security system. Must rollback.\n Session="
                                        + session);
                    }
                    secSys.disableReadFilter(session);
                    session.clear();
                }

            } finally {
                secSys.invalidateEventContext();
            }
        }

    }

    /**
     * checks method (and as a fallback the class) for the Spring
     * {@link Transactional} annotation.
     * 
     * @param mi
     *            Non-null method invocation.
     * @return true if the {@link Transactional} annotation lists this method as
     *         read-only, or if no annotation is found.
     */
    boolean checkReadOnly(MethodInvocation mi) {
        TransactionAttribute ta = txSource.getTransactionAttribute(mi
                .getMethod(), mi.getThis().getClass());
        return ta == null ? true : ta.isReadOnly();

    }

    /**
     * Calling clearLogs posits that these EventLogs were successfully saved,
     * and so this method may raise an event signalling such. This could
     * eventually be reworked to be fully within the security system.
     */
    void saveLogs(boolean readOnly, SessionImplementor session) {

        // Grabbing a copy to prevent ConcurrentModificationEx
        final List<EventLog> logs = new ArrayList<EventLog>(secSys.getLogs());
        secSys.clearLogs();

        if (logs == null || logs.size() == 0) {
            return; // EARLY EXIT
        }

        if (readOnly) {
            // If we reach here, we have logs when we shouldn't.
            StringBuilder sb = new StringBuilder();
            sb.append("EventLogs in readOnly transaction:\n");
            for (EventLog eventLog : logs) {
                sb.append(eventLog.getAction());
                sb.append(" ");
                sb.append(eventLog);
                sb.append(eventLog.getEntityType());
                sb.append(" ");
                sb.append(eventLog.getEntityId());
                sb.append("\b");
            }
            throw new InternalException(sb.toString());
        }

        try {
            long lastValue = isolatedJdbc.queryForLong("select ome_nextval(?,?)",
                    "seq_eventlog", logs.size());
            long id = lastValue - logs.size() + 1;
            List<Object[]> batchData = new ArrayList<Object[]>();
            for (EventLog l : logs) {
                Event e = l.getEvent();
                if (e.getId() == null) {
                    throw new RuntimeException("Transient event");
                }
                batchData
                        .add(new Object[] { id++, -35L, l.getEntityId(),
                                l.getEntityType(), l.getAction(),
                                l.getEvent().getId() });
            }

            simpleJdbc.batchUpdate("INSERT INTO eventlog "
                    + "(id, permissions, entityid,entitytype, action, event) "
                    + "values (?,?,?,?,?,?)", batchData);

        } catch (Exception ex) {
            log.error("Error saving event logs: " + logs, ex);
        }

        if (secSys.getLogs().size() > 0) {
            throw new InternalException("More logs present after saveLogs()");
        }

    }
}
