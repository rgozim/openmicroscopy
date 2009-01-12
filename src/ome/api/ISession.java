/*
 *   $Id$
 *
 *   Copyright 2007 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.api;

import java.util.Set;

import ome.annotations.Hidden;
import ome.annotations.NotNull;
import ome.conditions.ApiUsageException;
import ome.conditions.RemovedSessionException;
import ome.conditions.SecurityViolation;
import ome.conditions.SessionTimeoutException;
import ome.model.meta.Session;
import ome.system.Principal;

/**
 * <em>Start here</em>: {@link Session} creation service for OMERO. Access to
 * all other services is dependent upon a properly created and still active
 * {@link Session}.
 * 
 * The {@link Session session's} {@link Session#getUuid() uuid} can be
 * considered a capability token, or temporary single use password. Simply by
 * possessing it the client has access to all information available to the
 * {@link Session}.
 * 
 * Note: Both the RMI {@link ome.system.ServiceFactory} as well as the Ice
 * {@link omero.api.ServiceFactoryPrx} use {@link ISession} to acquire a
 * {@link Session}. In the RMI case, the {@link ISession} instance is the first
 * remote proxy accessed. In the Ice case, Glacier2 contacts {@link ISession}
 * itself and returns a ServiceFactory remote proxy. From both ServiceFactory
 * instances, it is possible but not necessary to access {@link ISession}.
 * 
 * @author Josh Moore, josh at glencoesoftware.com
 * @since 3.0-Beta3
 */
public interface ISession extends ServiceInterface {

    /**
     * Allows an admin to create a {@link Session} for the give
     * {@link Principal}
     * 
     * @param principal
     *            Non-null {@link Principal} with the target user's name
     * @param timeToLiveMilliseconds
     *            The time that this {@link Session} has until destruction. This
     *            is useful to override the server default so that an initial
     *            delay before the user is given the token will not be construed
     *            as idle time. A value less than 1 will cause the default max
     *            timeToLive to be used; but timeToIdle will be disabled.
     */
    Session createSessionWithTimeout(@NotNull Principal principal,
            long timeToLiveMilliseconds);

    /**
     * Allows an admin to create a {@link Session} for the give
     * {@link Principal}
     * 
     * @param principal
     *            Non-null {@link Principal} with the target user's name
     * @param timeToLiveMillseconds
     *            The time that this {@link Session} has until destruction.
     *            Setting the value to 0 will prevent destruction unless the
     *            session remains idle.
     * @param timeToIdleMilliseconds
     *            The time that this {@link Session} can remain idle before
     *            being destroyed. Setting the value to 0 will prevent idleness
     *            based destruction.
     */
    Session createSessionWithTimeouts(@NotNull Principal principal,
            long timeToLiveMilliseconds, long timeToIdleMilliseconds);

    /**
     * Creates a new session and returns it to the user.
     * 
     * @throws ApiUsageException
     *             if principal is null
     * @throws SecurityViolation
     *             if the password check fails
     */
    Session createSession(@NotNull Principal principal,
            @Hidden String credentials);

    /**
     * Updates subset of the fields from the {@link Session} object to the
     * {@link Session} matching the given uuid. If the {@link Session#getUuid()
     * uuid} is not present, then a {@link RemovedSessionException} is thrown.
     * 
     * Updated: group, {@link Session#userAgent}, {@link Session#message},
     * {@link Session#defaultUmask}, {@link Session#setDefaultEventType(String)}
     * 
     * Conditionally updated: timeToLive, timeToIdle These can only be set
     * within boundaries provided by the system administrator. Currently this is
     * hard-coded to mean 10 times the defaultTimeToLive and defaultTimeToIdle,
     * respectively.
     * 
     * Ignored: All others, but especially user, {@link Session#events}
     * {@link Session#uuid}, and the timestamps.
     * 
     * @param session
     *            The {@link Session} instance to be updated.
     * @return The {@link Session} updated instance. Should replace the current
     *         value: <code> session = iSession.updateSession(session); </code>
     */
    Session updateSession(@NotNull Session session);

    /**
     * Retrieves the session associated with this uuid. Throws a
     * {@link RemovedSessionException} if not present, or a
     * {@link SessionTimeoutException} if expired.
     * 
     * This method can be used as a {@link Session} ping.
     */
    Session getSession(@NotNull String sessionUuid);

    /**
     * Closes session and releases all resources. It is preferred that all
     * clients call this method as soon as possible to free memory, but it is
     * possible to not call close, and rejoin a session later.
     */
    void closeSession(@NotNull Session session);

    // void addNotification(String notification);
    // void removeNotification(String notification);
    // List<String> listNotifications();
    // void defaultNotifications();
    // void clearNotifications();

    // Session joinSessionByName(@NotNull String sessionName); // Here you don't
    // have a
    // void disconnectSession(@NotNull Session session);
    // void pingSession(@NotNull Session session); // Add to ServiceFactoryI

    // Environment contents
    // =========================================================================

    /**
     * Retrieves an entry from the given {@link Session session's} input
     * environment. If the value is null, the key will be removed.
     */
    Object getInput(String session, String key);

    /**
     * Retrieves all keys in the {@link Session sesson's} input environment.
     * 
     * @param session
     * @return
     */
    Set<String> getInputKeys(String session);

    /**
     * Places an entry in the given {@link Session session's} input environment.
     * If the value is null, the key will be removed.
     */
    void setInput(String session, String key, Object objection);

    /**
     * Retrieves all keys in the {@link Session sesson's} output environment.
     */
    Set<String> getOutputKeys(String session);

    /**
     * Retrieves an entry from the {@link Session session's} output environment.
     */
    Object getOutput(String session, String key);

    /**
     * Places an entry in the given {@link Session session's} output
     * environment. If the value is null, the key will be removed.
     */
    void setOutput(String session, String key, Object objection);
}
