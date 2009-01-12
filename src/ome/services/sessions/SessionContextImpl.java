/*
 *   $Id$
 *
 *   Copyright 2007 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.sessions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ome.model.internal.Permissions;
import ome.model.meta.Session;
import ome.services.sessions.stats.SessionStats;

public class SessionContextImpl implements SessionContext {

    private int ref = 0;
    private final Object refLock = new Object();
    private final Session session;
    private final SessionStats stats;
    private final List<Long> leaderOfGroups;
    private final List<Long> memberOfGroups;
    private final List<String> roles; /* group names for memberOfGroups */
    private Long shareId = null;

    @SuppressWarnings("unchecked")
    public SessionContextImpl(Session session, List<Long> lGroups,
            List<Long> mGroups, List<String> roles, SessionStats stats) {
        this.stats = stats;
        this.session = session;
        this.leaderOfGroups = Collections.unmodifiableList(new ArrayList(
                lGroups));
        this.memberOfGroups = Collections.unmodifiableList(new ArrayList(
                mGroups));
        this.roles = Collections.unmodifiableList(new ArrayList(roles));
    }

    public int refCount() {
        synchronized (refLock) {
            return ref;
        }
    }

    public int increment() {
        synchronized (refLock) {
            if (ref < 0) {
                ref = 1;
            } else {
                ref = ref + 1;
            }
            return ref;
        }
    }

    public int decrement() {
        synchronized (refLock) {
            if (ref < 1) {
                ref = 0;
            } else {
                ref = ref - 1;
            }
            return ref;
        }
    }

    public SessionStats stats() {
        return stats;
    }
    
    public Session getSession() {
        return session;
    }

    public List<String> getUserRoles() {
        return roles;
    }

    public void setShareId(Long shareId) {
        this.shareId = shareId;
    }

    public Long getCurrentShareId() {
        return shareId;
    }

    public Long getCurrentSessionId() {
        return getSession().getId();
    }

    public String getCurrentSessionUuid() {
        return getSession().getUuid();
    }

    public Long getCurrentEventId() {
        throw new UnsupportedOperationException();
    }

    public String getCurrentEventType() {
        return session.getDefaultEventType();
    }

    public Long getCurrentGroupId() {
        return session.getDetails().getGroup().getId();
    }

    public String getCurrentGroupName() {
        return session.getDetails().getGroup().getName();
    }

    public Long getCurrentUserId() {
        return session.getDetails().getOwner().getId();
    }

    public String getCurrentUserName() {
        return session.getDetails().getOwner().getOmeName();
    }

    public List<Long> getLeaderOfGroupsList() {
        return leaderOfGroups;
    }

    public List<Long> getMemberOfGroupsList() {
        return memberOfGroups;
    }

    public boolean isCurrentUserAdmin() {
        throw new UnsupportedOperationException();
    }

    public boolean isReadOnly() {
        throw new UnsupportedOperationException();
    }

    public Permissions getCurrentUmask() {
        throw new UnsupportedOperationException();
    }
}