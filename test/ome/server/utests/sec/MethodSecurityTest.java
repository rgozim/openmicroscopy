/*
 *   $Id$
 *
 *   Copyright 2007 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.server.utests.sec;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import ome.api.IAdmin;
import ome.conditions.SecurityViolation;
import ome.logic.AdminImpl;
import ome.security.MethodSecurity;
import ome.security.basic.BasicMethodSecurity;
import ome.security.basic.BasicSecurityWiring;
import ome.services.sessions.SessionManager;
import ome.system.Principal;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.JamonPerformanceMonitorInterceptor;
import org.springframework.jdbc.core.simple.SimpleJdbcOperations;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.ExpectedExceptions;
import org.testng.annotations.Test;

@Test(groups = "mock")
public class MethodSecurityTest extends MockObjectTestCase {

    Mock mockMgr;

    SessionManager mgr;

    MethodSecurity msec;

    protected void check() throws Exception {
        try {
            super.verify();
            verify();
        } finally {
            mockMgr = null;
            super.tearDown();
        }
    }

    @BeforeMethod
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mockMgr = mock(SessionManager.class);
        mgr = (SessionManager) mockMgr.proxy();
        BasicMethodSecurity bmsec = new BasicMethodSecurity();
        bmsec.setSessionManager(mgr);
        msec = bmsec;
    }

    @Test(groups = "ticket:645")
    public void testIsActiveWithDefaultCtor() throws Exception {
        assertTrue(new BasicMethodSecurity().isActive());
        assertTrue(new BasicMethodSecurity(true).isActive());
        assertFalse(new BasicMethodSecurity(false).isActive());
    }

    @Test(groups = "ticket:645")
    @ExpectedExceptions(SecurityViolation.class)
    public void testCheckMethodThrowsException() throws Exception {

        Method sync = AdminImpl.class.getMethod("synchronizeLoginCache");
        Principal p = new Principal("foo", "bar", "baz");

        List<String> roles = Arrays.asList("user", "demo");
        mockMgr.expects(once()).method("getUserRoles").will(returnValue(roles));

        try {
            msec.checkMethod(new AdminImpl(null, null, null, null, null, null, null, null, null),
                    sync, p);
        } finally {
            check();
        }
    }

    @Test(groups = "ticket:645")
    public void testCheckMethodAllowsExecution() throws Exception {

        Method ec = AdminImpl.class.getMethod("getEventContext");
        Principal p = new Principal("foo", "bar", "baz");

        List<String> roles = Arrays.asList("user", "demo");
        mockMgr.expects(once()).method("getUserRoles").will(returnValue(roles));

        try {
            msec.checkMethod(new AdminImpl(null, null, null, null,null, null, null, null, null),
                    ec, p);
        } finally {
            check();
        }

    }

    @Test(groups = "ticket:645")
    public void testCheckMethodAllowsExecutionEvenOnProxy() throws Exception {

        Method ec = AdminImpl.class.getMethod("getEventContext");
        Principal p = new Principal("foo", "bar", "baz");

        List<String> roles = Arrays.asList("user", "demo");
        mockMgr.expects(once()).method("getUserRoles").will(returnValue(roles));

        ProxyFactory factory = new ProxyFactory();
        factory.setInterfaces(new Class[] { IAdmin.class });
        factory.setTarget(new AdminImpl(null, null, null, null, null, null, null, null, null));
        factory.addAdvice(new JamonPerformanceMonitorInterceptor());
        IAdmin proxy = (IAdmin) factory.getProxy();
        try {
            msec.checkMethod(factory.getProxy(), ec, p);
        } finally {
            check();
        }

    }

}
