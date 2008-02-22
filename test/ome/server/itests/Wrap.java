/*
 *   $Id$
 *
 *   Copyright 2007 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.server.itests;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.ejb.SessionContext;
import javax.interceptor.InvocationContext;

import ome.logic.HardWiredInterceptor;
import ome.logic.QueryImpl;
import ome.services.RenderingBean;
import ome.services.util.OmeroAroundInvoke;
import ome.system.Principal;
import ome.system.SelfConfigurableService;

import org.jmock.core.matcher.InvokeAtLeastOnceMatcher;
import org.jmock.core.stub.ReturnStub;

/**
 * This class allows one to create a backdoor into the server for testing
 * purposes. <code>
 * final MyTest test = this;
 * new Wrap("user", new Backdoor() {
 *
 * @Override
 * @RolesAllowed("user") 
 * public void run() { 
 *    // code
 * });
 * </code>
 * @author Josh Moore, josh at glencoesoftware.com
 * 
 */
public class Wrap extends OmeroAroundInvoke {

    /**
     * This class can be used in combination with {@link Wrap} to simulate
     * running arbitrary code within the server.
     * 
     * @author Josh Moore, josh at glencoesoftware.com
     */
    public static interface Backdoor extends Runnable, SelfConfigurableService {
    }

    public static abstract class QueryBackdoor extends QueryImpl implements
            Backdoor {
    }

    public static abstract class REBackdoor extends RenderingBean implements
            Backdoor {
    }

    final Principal p;

    final Backdoor backdoor;

    public Wrap(final String user, final Backdoor backdoor) throws Exception {
        this(new Principal(user, "user", "Test"), backdoor);
    }

    public Wrap(List<HardWiredInterceptor> cptors, String user,
            Backdoor backdoor) throws Exception {
        this(cptors, new Principal(user, "user", "Test"), backdoor);
    }

    public Wrap(final Principal p, final Backdoor backdoor) throws Exception {
        this.p = p;
        this.backdoor = backdoor;
        init();
    }

    public Wrap(final List<HardWiredInterceptor> cptors, final Principal p,
            final Backdoor backdoor) throws Exception {
        super(cptors);
        this.p = p;
        this.backdoor = backdoor;
        init();
    }

    public void init() throws Exception {
        sessionContext();
        InvocationContext ic = new InvocationContext() {
            public Object getTarget() {
                return backdoor;
            }

            public Map getContextData() {
                return null;
            }

            public Method getMethod() {
                try {
                    return backdoor.getClass().getMethod("run");
                } catch (Exception e) {
                    return null;
                }
            }

            public Object[] getParameters() {
                return new Object[] {};
            }

            public Object proceed() throws Exception {
                backdoor.run();
                return null;
            }

            public void setParameters(Object[] arg0) {
            }
        };
        backdoor.selfConfigure();
        loginAndSpringWrap(ic);
    }

    void sessionContext() throws Exception {
        Field sessionContext = this.getClass().getSuperclass()
                .getDeclaredField("sessionContext");
        sessionContext.setAccessible(true);
        org.jmock.Mock mockContext = new org.jmock.Mock(SessionContext.class);
        SessionContext sc = (SessionContext) mockContext.proxy();
        mockContext.expects(new InvokeAtLeastOnceMatcher()).method(
                "getCallerPrincipal").will(new ReturnStub(p));
        sessionContext.set(this, sc);
    }

}
