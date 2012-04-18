/*
 * Copyright 2012 Glencoe Software, Inc. All rights reserved.
 * Use is subject to license terms supplied in LICENSE.txt
 */
package ome.server.utests.sec;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.jmock.MockObjectTestCase;
import org.testng.annotations.Test;

import ome.model.core.Image;
import ome.model.internal.Details;
import ome.model.internal.Permissions;
import ome.model.meta.Experimenter;
import ome.model.meta.ExperimenterGroup;
import ome.model.meta.Session;
import ome.security.SystemTypes;
import ome.security.basic.BasicACLVoter;
import ome.security.basic.BasicEventContext;
import ome.security.basic.CurrentDetails;
import ome.security.basic.TokenHolder;
import ome.services.sessions.SessionContext;
import ome.services.sessions.SessionContextImpl;
import ome.services.sessions.state.SessionCache;
import ome.services.sessions.stats.NullSessionStats;
import ome.system.Principal;

/**
 * Intended to test the "write-ability" granted to users based on the current
 * context and the object in question. These permissions should be passed
 * back via the "disallowAnnotate" and "disallowEdit" flags.
 *
 * @since 4.4.0
 * @see ticket 8277
 */
@Test(groups = { "unit", "permissions", "ticket:8277" })
public class WritePermissionsTest extends MockObjectTestCase {

    final Long ROOT = 0L;

    final Long THE_GROUP = 2L;

    final Long THE_OWNER = 2L;

    final Long GROUP_MEMBER = 3L;

    final SessionCache cache = new SessionCache();

    final CurrentDetails cd = new CurrentDetails(cache);

    final BasicACLVoter voter = new BasicACLVoter(cd, new SystemTypes(),
            new TokenHolder(), null);

    protected Session login(String perms, long user, boolean leader) {

        Session s = sess(perms, user, THE_GROUP);
        SessionContext sc = context(s, leader);
        cache.putSession(s.getUuid(), sc);
        BasicEventContext bec = new BasicEventContext(new Principal(s.getUuid()),
                new NullSessionStats(), sc);
        ExperimenterGroup g = s.getDetails().getGroup();
        bec.setGroup(g, g.getDetails().getPermissions());
        bec.setOwner(s.getDetails().getOwner());
        cd.login(bec);
        return s;
    }

    protected Details objectBelongingTo(Session session, long user) {
        return objectBelongingTo(session, user,
                session.getDetails().getPermissions());
    }

    protected Details objectBelongingTo(Session session, long user, String s) {
        return objectBelongingTo(session, user, Permissions.parseString(s));
    }

    /**
     * Creates an object which is in the group given by the {@link Session}
     * object, but which belongs to the given {@link Experimenter} and has
     * the given {@link Permissions}
     *
     * @param session
     *            Session which the object was created during.
     * @param user
     *            User who owns this object.
     * @param p
     *            Permissions to set on the object details.
     */
    protected Details objectBelongingTo(Session session, long user, Permissions p) {
        Image i = new Image();
        Details d = i.getDetails();
        d.setOwner(new Experimenter(user, true));
        d.setGroup(session.getDetails().getGroup());
        d.setPermissions(p);
        voter.postProcess(i);
        return d;
    }

    // object setting differs from group
    // =========================================================================
    // Since in 4.4, it's possible for permissions settings of an object to
    // differ from those of the group, we need to make sure that post-processing
    // properly maps to the group permissions.

    public void testDifferentPerms() {
        Session s = login("rwr---", THE_OWNER, false);
        Details d = objectBelongingTo(s, THE_OWNER, "r-r-r-");
        assertCanAnnotate(d);
        assertCanEdit(d);
        assertEquals("rwr---", d.getPermissions().toString());
    }

    // rwr, non-system owner
    // =========================================================================

    public void testOwnerCanAllByDefault() {
        Session s = login("rwr---", THE_OWNER, false);
        Details d = objectBelongingTo(s, THE_OWNER);
        assertCanAnnotate(d);
        assertCanEdit(d);
    }

    public void testAdminCanAllByDefault() {
        Session s = login("rwr---", ROOT, false);
        Details d = objectBelongingTo(s, THE_OWNER);
        assertCanAnnotate(d);
        assertCanEdit(d);
    }

    public void testGroupMemberCannotByDefault() {
        Session s = login("rwr---", GROUP_MEMBER, false);
        Details d = objectBelongingTo(s, THE_OWNER);
        assertCannotAnnotate(d);
        assertCannotEdit(d);
    }

    public void testGroupLeaderCannotByDefault() {
        Session s = login("rwr---", GROUP_MEMBER, true);
        Details d = objectBelongingTo(s, THE_OWNER);
        assertCanAnnotate(d);
        assertCanEdit(d);
    }

    // Helpers
    // =========================================================================

    void assertCanAnnotate(Details d) {
        assertFalse(d.getPermissions().isDisallowAnnotate());
    }

    void assertCanEdit(Details d) {
        assertFalse(d.getPermissions().isDisallowEdit());
    }

    void assertCannotAnnotate(Details d) {
        assertTrue(d.getPermissions().isDisallowAnnotate());
    }

    void assertCannotEdit(Details d) {
        assertTrue(d.getPermissions().isDisallowEdit());
    }

    Session sess(String perms, long user, long group) {
        Permissions p = Permissions.parseString(perms);
        Session s = new Session();
        s.setStarted(new Timestamp(System.currentTimeMillis()));
        s.setTimeToIdle(0L);
        s.setTimeToLive(0L);
        s.setUuid(UUID.randomUUID().toString());
        s.getDetails().setPermissions(p);
        // group
        ExperimenterGroup g = new ExperimenterGroup(group, true);
        g.getDetails().setPermissions(Permissions.parseString(perms));
        s.getDetails().setGroup(g);
        // user
        Experimenter e = new Experimenter(user, true);
        s.getDetails().setOwner(e);
        return s;
    }

    SessionContext context(Session s, boolean leader) {

        final Long user = s.getDetails().getOwner().getId();
        final Long group = s.getDetails().getGroup().getId();
        List<String> roles = new ArrayList<String>();
        List<Long> memberOf = new ArrayList<Long>();
        List<Long> leaderOf = new ArrayList<Long>();

        roles.add("user");
        memberOf.add(1L);
        memberOf.add(group);

        if (user.equals(0L)) { // use "root" as proxy for "admin"
            memberOf.add(0L); // system
            roles.add("system");
        }

        if (leader) {
            leaderOf = Arrays.asList(group);
        }

        return new SessionContextImpl(s, leaderOf, memberOf, roles,
                new NullSessionStats(), null);
    }
}
