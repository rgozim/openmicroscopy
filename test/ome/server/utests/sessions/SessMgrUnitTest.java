/*
 *   $Id$
 *
 *   Copyright 2007 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.server.utests.sessions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.CacheManager;
import ome.api.local.LocalAdmin;
import ome.api.local.LocalQuery;
import ome.api.local.LocalUpdate;
import ome.conditions.AuthenticationException;
import ome.conditions.RemovedSessionException;
import ome.conditions.SecurityViolation;
import ome.conditions.SessionException;
import ome.conditions.SessionTimeoutException;
import ome.model.enums.EventType;
import ome.model.internal.Details;
import ome.model.internal.Permissions;
import ome.model.internal.Permissions.Right;
import ome.model.internal.Permissions.Role;
import ome.model.meta.Experimenter;
import ome.model.meta.ExperimenterGroup;
import ome.model.meta.Node;
import ome.model.meta.Session;
import ome.security.basic.CurrentDetails;
import ome.services.sessions.SessionContext;
import ome.services.sessions.SessionManagerImpl;
import ome.services.sessions.events.UserGroupUpdateEvent;
import ome.services.sessions.state.SessionCache;
import ome.services.sessions.state.SessionCache.StaleCacheListener;
import ome.services.sessions.stats.CounterFactory;
import ome.services.sessions.stats.SessionStats;
import ome.services.util.Executor;
import ome.system.OmeroContext;
import ome.system.Principal;
import ome.system.Roles;
import ome.system.ServiceFactory;
import ome.testing.MockServiceFactory;

import org.hibernate.envers.tools.ArraysTools;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.Constraint;
import org.jmock.core.Invocation;
import org.jmock.core.Stub;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * @author Josh Moore, josh at glencoesoftware.com
 * @since 3.0-Beta2
 */
public class SessMgrUnitTest extends MockObjectTestCase {

    private final class DoWorkStub implements Stub {
        public Object invoke(Invocation i) throws Throwable {
            Executor.Work work = (Executor.Work) i.parameterValues.get(1);
            return work.doWork(null, sf);
        }

        public StringBuffer describeTo(StringBuffer sb) {
            sb.append("calls doWork on work");
            return sb;
        }
    }

    private final class TestManager extends SessionManagerImpl {
        Session doDefine() {
            Session s = new Session();
            define(s, "uuid", "message", System.currentTimeMillis(),
                    defaultTimeToIdle, defaultTimeToLive, "Test",
                    "Test");
            return s;
        }
    }

    private OmeroContext ctx;
    private ApplicationEventMulticaster multicaster;
    private final MockServiceFactory sf = new MockServiceFactory();
    private TestManager mgr;
    private SessionCache cache;
    private org.hibernate.Session s;
    private Mock sMock;

    // State
    final Long TTL = 300 * 1000L;
    final Long TTI = 100 * 1000L;
    Session session = new Session();
    Principal principal = new Principal("u", "g", "Test");
    String credentials = "password";
    Experimenter user = new Experimenter(1L, true);
    ExperimenterGroup group = new ExperimenterGroup(1L, true);
    List<Long> m_ids = Collections.singletonList(1L);
    List<Long> l_ids = Collections.singletonList(1L);
    List<String> userRoles = Collections.singletonList("single");

    @BeforeTest
    public void config() {

        ctx = new OmeroContext("classpath:ome/services/messaging.xml");
        multicaster = (ApplicationEventMulticaster) ctx
                .getBean("applicationEventMulticaster");

        sf.mockAdmin = mock(LocalAdmin.class);
        sf.mockUpdate = mock(LocalUpdate.class);
        sf.mockQuery = mock(LocalQuery.class);

        sMock = mock(org.hibernate.Session.class);
        s = (org.hibernate.Session) sMock.proxy();
    }

    @BeforeMethod
    public void setup() {
        cache = new SessionCache();
        cache.setCacheManager(CacheManager.getInstance());
        cache.setApplicationContext(ctx);

        mgr = new TestManager();
        mgr.setCounterFactory(new CounterFactory());
        mgr.setRoles(new Roles());
        mgr.setSessionCache(cache);
        mgr.setPrincipalHolder(new CurrentDetails());
        mgr.setExecutor(new DummyExecutor(s, sf));
        mgr.setApplicationContext(ctx);
        mgr.setDefaultTimeToIdle(TTI);
        mgr.setDefaultTimeToLive(TTL);

        session = mgr.doDefine();
        session.setId(1L);

        user.setOmeName(principal.getName());
    }

    @AfterMethod
    public void cleanupMocks() throws Exception {
        tearDown();
    }

    @Test
    public void testCreateNewSession() throws Exception {
        /*
         * user logs in with: - non-sessioned principal / !E session =>
         * createSession - non-sessioned principal / E session => joinSession -
         * sessionedPrincipal => hasSession() != null, then ok
         */

        prepareForCreateSession();
        assert session == mgr.createWithAgent(principal, credentials, "Test");
        assertNotNull(session);
        assertNotNull(session.getUuid());

    }

    @Test
    public void testThatPermissionsAreAlreadyNonReadable() throws Exception {
        testCreateNewSession();
        Permissions p = session.getDetails().getPermissions();
        assertFalse(p.isGranted(Role.GROUP, Right.READ));
        assertFalse(p.isGranted(Role.GROUP, Right.WRITE));
        assertFalse(p.isGranted(Role.WORLD, Right.READ));
        assertFalse(p.isGranted(Role.WORLD, Right.WRITE));
    }

    @Test
    public void testThatDefaultTypeAreAssigned() throws Exception {
        testCreateNewSession();
        assertNotNull(session.getDefaultEventType());
    }

    @Test
    public void testThatStartedTimeIsSet() throws Exception {
        testCreateNewSession();
        assertNotNull(session.getStarted());
        assertTrue(session.getStarted().getTime() <= System.currentTimeMillis());
    }

    @Test
    public void testThatCreatedSessionIsFindable() throws Exception {
        testCreateNewSession();
        // now user has an active session, so basic security wiring will
        // allow method executions
        assertNotNull(mgr.find(session.getUuid()));
    }

    @Test
    public void testThatCreatedSessionIsUpdateable() throws Exception {

        Session rv = new Session();

        testCreateNewSession();
        session.setDefaultEventType("somethingnew");
        sf.mockUpdate.expects(once()).method("saveAndReturnObject").will(
                returnValue(rv));

        prepareForCreateSession();
        Session test = mgr.update(session);
        assertFalse(test == rv); // insists that a copy is performed
    }

    @Test(expectedExceptions = SessionException.class)
    public void testThatNonexteantSessionIsNOTUpdateable() throws Exception {
        session.setUuid("DoesNotExist");
        Session test = mgr.update(session);
    }

    @Test
    public void testThatCopiesHaveAllTheRightFields() throws Exception {
        testCreateNewSession();
        Session copy = mgr.copy(session);
        assertFalse(copy == session);
        assertNotNull(copy.getId());
        assertNotNull(copy.getStarted());
        assertNotNull(copy.getUuid());
        assertNotNull(copy.getDefaultEventType());
        assertNotNull(copy.getDetails().getPermissions());
    }

    @Test
    public void testThatUpdateProperlyHandlesDetails() throws Exception {
        testCreateNewSession();

        Session updated = new Session();
        updated.getDetails().copy(Details.create()); // As close to nulll as
        // poss

    }

    @Test
    public void testThatTimedOutSessionsAreMarkedAsSuch() throws Exception {

        // Create a session cache which checks frequently for stale keys
        cache = new SessionCache();
        cache.setUpdateInterval(10);
        cache.setCacheManager(CacheManager.getInstance());
        cache.setApplicationContext(ctx);

        prepareForCreateSession();
        Session s1 = mgr.createWithAgent(principal, credentials, "Test");
        s1.setTimeToIdle(2000L);
        mgr.update(s1);
        Thread.sleep(3000L);
        try {
            mgr.find(s1.getUuid());
            fail("This should throw on the lookup and not before");
        } catch (SessionTimeoutException ste) {
            // ok
        }
        try {
            assertNull(mgr.find(s1.getUuid()));
            fail("This should also throw");
        } catch (RemovedSessionException rse) {
            // ok
        }
    }

    @Test
    public void testThatManagerCanCloseASession() throws Exception {
        testCreateNewSession();
        Constraint closedSession = new Constraint() {
            public boolean eval(Object arg0) {
                Session s = (Session) arg0;
                return (System.currentTimeMillis() - s.getClosed().getTime()) < 1000L;
            }

            public StringBuffer describeTo(StringBuffer arg0) {
                return arg0.append("closed time was less than 1 sec ago");
            }

        };
        sf.mockUpdate.expects(once()).method("saveObject").with(closedSession);
        mgr.close(session.getUuid());
        try {
            mgr.find(session.getUuid());
            fail("Should throw");
        } catch (RemovedSessionException rse) {
            // ok
        }

    }

    @Test
    public void testThatManagerHandleAnExceptionOnClose() throws Exception {
        testCreateNewSession();
        Constraint closedSession = new Constraint() {
            public boolean eval(Object arg0) {
                Session s = (Session) arg0;
                return (System.currentTimeMillis() - s.getClosed().getTime()) < 1000L;
            }

            public StringBuffer describeTo(StringBuffer arg0) {
                return arg0.append("closed time was less than 1 sec ago");
            }

        };
        sf.mockUpdate.expects(once()).method("saveObject").with(closedSession);
        mgr.close(session.getUuid());
        try {
            mgr.find(session.getUuid());
            fail("oops");
        } catch (RemovedSessionException rse) {
            // ok
        }
    }

    @Test
    public void testThatManagerKeepsUpWithRolesPerSession() throws Exception {
        testCreateNewSession();

        List<String> roles = mgr.getUserRoles(session.getUuid());
        assertNotNull(roles);
        assertTrue(roles.size() > 0);
    }

    @Test
    public void testThatManagerCanHandleEvent() throws Exception {
        testCreateNewSession();
        List<String> preUserRoles = mgr.getUserRoles(session.getUuid());
        mgr.onApplicationEvent(null);
        List<String> postUserRoles = mgr.getUserRoles(session.getUuid());
        fail("Should here remove user from group and have roles updated."
                + "--depends on whether or not we need to catch the hiberate events"
                + "--or just events from AdminImpl");
    }

    @Test
    public void testReplacesNullGroupAndType() throws Exception {
        prepareForCreateSession();
        Session session = mgr.createWithAgent(new Principal("fake", null, null),
                credentials, "Test");
        assertNotNull(session.getDefaultEventType());
        assertNotNull(session.getDetails().getGroup());
    }

    void prepareForCreateSession() {
        sf.mockAdmin.expects(atLeastOnce()).method("userProxy")
                .will(returnValue(user));
        sf.mockAdmin.expects(atLeastOnce()).method("groupProxy").will(
                returnValue(group));
        sf.mockAdmin.expects(atLeastOnce()).method("getMemberOfGroupIds").will(
                returnValue(m_ids));
        sf.mockAdmin.expects(atLeastOnce()).method("getLeaderOfGroupIds").will(
                returnValue(l_ids));
        sf.mockAdmin.expects(atLeastOnce()).method("getUserRoles").will(
                returnValue(userRoles));
        sf.mockAdmin.expects(once()).method("checkPassword").will(
                returnValue(true));
        // execute lookup user
        sf.mockQuery.expects(once()).method("projection").will(
                returnValue(Arrays.asList((Object)new Object[]{123L})));


        EventType test = new EventType(0L, true);
        test.setValue("test");
        sf.mockTypes.expects(atLeastOnce()).method("getEnumeration").will(
                returnValue(test));

        sf.mockQuery.expects(atLeastOnce()).method("findByQuery")
            .will(returnValue(new Node()));

        sf.mockUpdate.expects(atLeastOnce()).method("saveAndReturnObject")
            .will(returnValue(session));


    }

    @Test(expectedExceptions = AuthenticationException.class)
    public void testCreateSessionFailsAUEOnNullPrincipal() throws Exception {
        sf.mockAdmin.expects(once()).method("checkPassword").will(
                returnValue(false));
        mgr.createWithAgent(null, "password", "Test");
    }

    @Test(expectedExceptions = AuthenticationException.class)
    public void testCreateSessionFailsAUEOnNullOmeName() throws Exception {
        sf.mockAdmin.expects(once()).method("checkPassword").will(
                returnValue(false));
        mgr.createWithAgent(new Principal(null, null, null), "password", "Test");
    }

    @Test(expectedExceptions = AuthenticationException.class)
    public void testCreateSessionFailsSV() throws Exception {
        sf.mockAdmin.expects(once()).method("checkPassword").will(
                returnValue(false));
        mgr.createWithAgent(principal, "password", "Test");
    }

    @Test
    public void testChecksForDefaultGroupsOnCreation() throws Exception {
        prepareForCreateSession();

        sf.mockAdmin.expects(once()).method("getDefaultGroup").will(
                returnValue(group));
        sf.mockQuery.expects(once()).method("findAllByQuery").will(
                returnValue(Collections.EMPTY_LIST));
        mgr.createWithAgent(new Principal("user", "user", "User"), "user", "Test");
    }

    @Test
    public void testWhatHappensIfAnEventOccursDuringUpdateEtc()
            throws Exception {
        fail("NYI");
    }

    @Test
    public void testcreationChecksIfUserIsMemberOfGroup() throws Exception {
        fail("NYI");
    }

    @Test
    public void testUpdateChecksIfUserIsMemberOfGroup() throws Exception {
        fail("NYI");
    }

    @Test
    public void testReferenceCounting() throws Exception {
        testCreateNewSession();
        String uuid = session.getUuid();
        SessionContext ctx = cache.getSessionContext(uuid, false/* FIXME */);

        assertEquals(1, ctx.refCount());
        assertNull(ctx.getSession().getClosed());

        mgr.createWithAgent(new Principal(uuid), "Test");
        assertEquals(2, ctx.refCount());
        assertNull(ctx.getSession().getClosed());

        mgr.close(uuid);
        assertEquals(1, ctx.refCount());
        assertNull(ctx.getSession().getClosed());

        prepareForCreateSession();
        mgr.close(uuid);
        assertEquals(0, ctx.refCount());
        // Closing the session is now done asynchronously.
        // Instead, let's make sure it's removed from the
        // cache
        // assertNotNull(ctx.getSession().getClosed());
        try {
            cache.getSessionContext(uuid, false/* FIXME */);
            fail(uuid + " not removed");
        } catch (RemovedSessionException rse) {
            // ok
        }

    }

    // Timeouts
    @Test
    public void testTimeoutDefaults() throws Exception {
        testCreateNewSession();
        SessionContext ctx = cache
                .getSessionContext(session.getUuid(), false/* FIXME */);

        assertEquals(TTL, ctx.getSession().getTimeToLive());
        assertEquals(TTI, ctx.getSession().getTimeToIdle());
    }

    @Test
    public void testTimeoutUpdatesValid() throws Exception {

        testTimeoutDefaults();
        SessionContext ctx = cache
                .getSessionContext(session.getUuid(), false/* FIXME */);

        Session s = mgr.copy(ctx.getSession());
        s.setTimeToLive(300L);
        s.setTimeToIdle(100L);
        s = mgr.update(s);

        assertEquals(new Long(300L), s.getTimeToLive());
        assertEquals(new Long(100L), s.getTimeToIdle());

        // For this first test we want to also verify that
        // the values in the session context are the same as the
        // returned values

        ctx = cache.getSessionContext(session.getUuid(), false/* FIXME */);
        assertEquals(new Long(300L), ctx.getSession().getTimeToLive());
        assertEquals(new Long(100L), ctx.getSession().getTimeToIdle());
    }

    @Test(expectedExceptions = SecurityViolation.class)
    public void testTimeoutUpdatesTTLNotZero() throws Exception {

        testTimeoutDefaults();
        SessionContext ctx = cache
                .getSessionContext(session.getUuid(), false/* FIXME */);

        Session s = mgr.copy(ctx.getSession());
        s.setTimeToLive(0L);
        s.setTimeToIdle(100L);
        s = mgr.update(s);

    }

    @Test(expectedExceptions = SecurityViolation.class)
    public void testTimeoutUpdatesTTINotZero() throws Exception {

        testTimeoutDefaults();
        SessionContext ctx = cache
                .getSessionContext(session.getUuid(), false/* FIXME */);

        Session s = mgr.copy(ctx.getSession());
        s.setTimeToLive(100L);
        s.setTimeToIdle(0L);
        s = mgr.update(s);

    }

    @Test
    public void testTimeoutUpdatesTooBig() throws Exception {

        testTimeoutDefaults();
        SessionContext ctx = cache
                .getSessionContext(session.getUuid(), false/* FIXME */);

        Session s = mgr.copy(ctx.getSession());
        s.setTimeToLive(Long.MAX_VALUE);
        s.setTimeToIdle(Long.MAX_VALUE);
        s = mgr.update(s);

        assertEquals(new Long((Long.MAX_VALUE / 10) * 10), s.getTimeToLive());
        assertEquals(new Long((Long.MAX_VALUE / 10) * 10), s.getTimeToIdle());

    }

    @Test
    public void testCreationSessionShouldBeAllowedWithoutBlocking()
            throws Exception {

        // Threads
        final CyclicBarrier barrier = new CyclicBarrier(3);

        cache.setStaleCacheListener(new StaleCacheListener() {

            public void prepareReload() {
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            public SessionContext reload(SessionContext context) {
                return null;
            }
        });

        class Update extends Thread {
            @Override
            public void run() {
                try {
                    // Here we force an update
                    cache.updateEvent(new UserGroupUpdateEvent(this));
                    cache.doUpdate();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        class Create extends Thread {
            @Override
            public void run() {
                try {
                    testCreateNewSession();
                    barrier.await(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // Run
        new Update().start();

        // Now we know that update is blocking
        new Create().start();
        barrier.await(10, TimeUnit.SECONDS);

        assertFalse(barrier.isBroken());

    }

    @Test(groups = "ticket:2804")
    public void testSessionShouldNotBeReapedDuringMethodExceution()
            throws Exception {

        testCreateNewSession();
        final String uuid = session.getUuid();
        final SessionContext ctx = cache.getSessionContext(uuid, false);
        final SessionStats stats = ctx.stats();

        // Check reaping while user is running a method
        stats.methodIn();
        mgr.close(uuid);
        assertNotNull(cache.getSessionContext(uuid, true));

        // Try to start a new method.
        // fail("NYI");

        // Checked reaping when user is not running a method
        stats.methodOut();
        mgr.close(uuid);
        assertNull(cache.getSessionContext(uuid, true));

    }

    //
    // Helper
    //

    class DummyExecutor implements Executor {

        org.hibernate.Session session;
        ServiceFactory sf;

        DummyExecutor(org.hibernate.Session session, ServiceFactory sf) {
            this.session = session;
            this.sf = sf;
        }

        public Object execute(Principal p, Work work) {
            return work.doWork(session, sf);
        }

        public <T> Future<T> submit(Callable<T> callable) {
            throw new UnsupportedOperationException();
        }

        public <T> T get(Future<T> future) {
            throw new UnsupportedOperationException();
        }

        public Object executeStateless(StatelessWork work) {
            throw new UnsupportedOperationException();
        }

        public void setApplicationContext(ApplicationContext arg0)
                throws BeansException {
            throw new UnsupportedOperationException();
        }

        public OmeroContext getContext() {
            throw new UnsupportedOperationException();
        }

        public Principal principal() {
            throw new UnsupportedOperationException();
        }

        public void setCallGroup(long gid) {
            throw new UnsupportedOperationException();
        }

        public void resetCallGroup() {
            throw new UnsupportedOperationException();
        }

    }
}
