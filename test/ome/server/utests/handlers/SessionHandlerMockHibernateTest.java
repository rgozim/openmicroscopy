/*
 * ome.server.utests.handlers.SessionHandlerMockHibernateTest
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2005 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *------------------------------------------------------------------------------
 */
package ome.server.utests.handlers;

// Java imports
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Iterator;

import javax.sql.DataSource;

import junit.framework.Assert;

// Third-party libraries
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.classic.Session;
import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.builder.ArgumentsMatchBuilder;
import org.jmock.core.Invocation;
import org.jmock.core.InvocationMatcher;
import org.jmock.core.Stub;
import org.jmock.core.stub.DefaultResultStub;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testng.annotations.*;

// Application-internal dependencies
import ome.api.ServiceInterface;
import ome.api.StatefulServiceInterface;
import ome.conditions.InternalException;
import ome.tools.hibernate.SessionHandler;
import omeis.providers.re.RenderingEngine;

/**
 * @author Josh Moore &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:josh.moore@gmx.de">josh.moore@gmx.de</a>
 * @version 1.0 <small> (<b>Internal version:</b> $Rev$ $Date$) </small>
 * @since Omero 2.0
 */
@Test( groups = {"hibernate","stateful","ticket:326"} )
public class SessionHandlerMockHibernateTest extends MockObjectTestCase
{

    private static Log log = LogFactory
            .getLog(SessionHandlerMockHibernateTest.class);
    
    private ServiceInterface stateless;
    private StatefulServiceInterface stateful;
    private SessionHandler handler;
    private Session session;
    private SessionFactory factory;
    private DataSource dataSource;
    private Connection connection;
    private MethodInvocation invocation;
    private Mock mockSession, mockFactory,  
        mockInvocation, mockStateful, mockStateless, 
        mockDataSource, mockTransaction, mockConnection;

    @Configuration(beforeTestMethod = true)
    protected void setUp() throws Exception
    {
        super.setUp();
        newDataSource();
        newConnection();
        
        newSession();
        newSessionFactory();
        handler = new SessionHandler( dataSource, factory );
        // must call newXInvocation in test

        // these are reused unless otherwise noted
        newStateful();
        newStateless(); 
        
        // Things should always be cleaned up by handler/interceptor
        assertFalse( TransactionSynchronizationManager.hasResource(factory) );
        if (!TransactionSynchronizationManager.isSynchronizationActive())
        	TransactionSynchronizationManager.initSynchronization();
    }

    @Configuration(afterTestMethod = true)
    protected void tearDown() throws Exception
    {
    	session = null;
    	reset(mockStateful,mockStateless,mockSession,mockFactory,mockTransaction,
    			mockDataSource,mockConnection,mockInvocation);
        super.tearDown();
        if (TransactionSynchronizationManager.isSynchronizationActive())
        	TransactionSynchronizationManager.clearSynchronization();
    }

    // ~ Tests
    // =========================================================================

    @Test
    @ExpectedExceptions( InternalException.class )
    public void testStatelessInvocation() throws Throwable
    {
        newStatelessInvocation();
        handler = new SessionHandler( dataSource, factory );
        handler.invoke( invocation );
        super.verify();
    }

    @Test
    public void testStatefulInvocationGetsNewSession() throws Throwable
    {
        newStatefulReadInvocation();
        opensSession();
        setsFlushMode();
        beginsTransaction(1);
        // invocation here
        disconnectsSession();
        handler.invoke( invocation );
        super.verify();
    }

    @Test
    public void testSecondStatefulInvocationsReusesSession() throws Throwable
    {
        newStatefulReadInvocation();
        opensSession();
        setsFlushMode();
        //getsNewConnection();
        beginsTransaction(2);
        checksSessionIsOpen();
        // invocation here
        disconnectsSession();
        handler.invoke( invocation );

        // assume someone clears thread
        TransactionSynchronizationManager.unbindResource(factory);
     
        // And a second call should just work.
        newStatefulReadInvocation();
        // invocation here        
        
        //TODO DELETE
        //checksSessionIsConnected(false);
        //reconnectsSession();
        
        disconnectsSession();
        handler.invoke( invocation );
        super.verify();
    }
    
    @Test
    @ExpectedExceptions( InternalException.class )
    public void testStatefulInvocationWithExistingSession() throws Throwable
    {
        prepareThreadWithSession();
        
        newStatefulReadInvocation();
        checksSessionIsOpen();
        checksSessionIsConnected();
        disconnectsSession();
        closesSession();
        handler.invoke( invocation );
        super.verify();
    }
    
    @Test
    public void testClosedOnException() throws Throwable
    {
        prepareThreadWithSession();

        try {
        newStatefulReadInvocationThrows();
        checksSessionIsOpen();
        checksSessionIsConnected();
        // here it throws
        disconnectsSession();
        closesSession();
        handler.invoke( invocation );
        fail("Should have thrown.");
        } catch (Exception e)
        {}
    }
    
    @Test
    public void testStatefulInvocationWithSessionThenClosed() throws Throwable
    {
        newStatefulDestroyInvocation();
        opensSession();
        setsFlushMode(FlushMode.COMMIT);
        beginsTransaction(1);
        disconnectsSession();
        closesSession();
        handler.invoke( invocation );
        super.verify();
    }
    
    @Test
    @ExpectedExceptions( InternalException.class )
    public void testStatefulReentrantCallThrows() throws Throwable
    {
        Method method = RenderingEngine.class.getMethod("getDefaultZ");
        newStatefulInvocation( method, new Stub() {
        	public Object invoke(Invocation dummy) throws Throwable {
        		handler.invoke( invocation );
        		return null;
        	}
        	public StringBuffer describeTo(StringBuffer buffer) {
        		return buffer.append(" reentrant call ");
        	}
        });
        opensSession();
        setsFlushMode();
        beginsTransaction(2);
        checksSessionIsOpen();
        // invocation here
        checksSessionIsConnected();
        disconnectsSession();
        closesSession();
        handler.invoke( invocation );
    }
    
    // TODO add dirty session on close
    // TODO 
    
    // ~ Once Expectations (creation events)
    // =========================================================================
    
    private void opensSession()
    {
        mockFactory.expects( once() ).method( "openSession" )
            .will( returnValue( session ));
    }
    
    private void beginsTransaction(int count)
    {
     
        mockSession.expects( exactly(count) ).method("beginTransaction");
    }

    // ~ More-than-once Expectations (somewhat idempotent)
    // =========================================================================
    
    private void checksSessionIsOpen()
    {
        mockSession.expects( atLeastOnce() ).method( "isOpen" )
            .will( returnValue( true ));
    }

    private void checksSessionIsConnected(Boolean...connected)
    {
        mockSession.expects( atLeastOnce() ).method( "isConnected" )
            .will( returnValue( connected.length>0 ? connected[0] : true ) );
    }

    private void getsNewConnection()
    {
        mockDataSource.expects( atLeastOnce() ).method( "getConnection" )
            .will( returnValue( connection ));
    }
    
    private void getsSessionsConnection()
    {
        mockSession.expects( atLeastOnce() ).method( "connection" )
            .will( returnValue( connection ));
    }

    private void reconnectsSession()
    {
        mockSession.expects( atLeastOnce() ).method( "reconnect" );
    }
    
    private void setsFlushMode(FlushMode...modes)
    {
        // done by handler see ticket:557
    	if (modes.length==0)
    	{
	        mockSession.expects( atLeastOnce() ).method( "setFlushMode" )
	    		.with( eq( FlushMode.COMMIT ));
	        mockSession.expects( atLeastOnce() ).method( "setFlushMode" )
	        	.with( eq( FlushMode.MANUAL ));
    	} else {
    		for (FlushMode mode : modes) {
    	        mockSession.expects( atLeastOnce() ).method( "setFlushMode" )
    	        	.with( eq( mode ));
			}
    	}
    }
    
    private void disconnectsSession()
    {
        mockSession.expects( atLeastOnce() ).method( "disconnect" );
    }
    
    private void closesSession()
    {
        mockSession.expects( atLeastOnce() ).method( "close" );
    }
    
    // ~ Helpers
    // =========================================================================

    private void newDataSource(){
        mockDataSource = mock(DataSource.class);
        dataSource = (DataSource) mockDataSource.proxy();
    }
    
    private void newConnection(){
        mockConnection = mock(Connection.class);
        connection = (Connection) mockConnection.proxy();
    }

    private void newSession(){
        mockSession = mock(Session.class);
        session = (Session) mockSession.proxy();
    }

    private void newSessionFactory(){
        mockFactory = mock(SessionFactory.class);
        factory = (SessionFactory) mockFactory.proxy();
    }

    private void newStateful( )
    {
        mockStateful = mock(StatefulServiceInterface.class);
        stateful = (StatefulServiceInterface) mockStateful.proxy();
    }
    
    private void newStateless( )
    {
        mockStateless = mock(ServiceInterface.class);
        stateless = (ServiceInterface) mockStateless.proxy();
    }
    
    private void newStatelessInvocation(){
        mockInvocation = mock(MethodInvocation.class);
        invocation = (MethodInvocation) mockInvocation.proxy();
        mockInvocation.expects( once() ).method("getThis")
            .will( returnValue(stateless));
    }
    
    private void newStatefulReadInvocation() throws Exception    {
        Method method = RenderingEngine.class.getMethod("getDefaultZ");
        newStatefulInvocation( method );
    }

    private void newStatefulReadInvocationThrows() throws Exception    {
        Method method = RenderingEngine.class.getMethod("getDefaultZ");
        newStatefulInvocation( method, throwException(new RuntimeException()) );
    }

    private void newStatefulWriteInvocation() throws Exception    {
        Method method = RenderingEngine.class.getMethod("setDefaultZ");
        newStatefulInvocation( method );
    }

    private void newStatefulWriteInvocationThrows() throws Exception    {
        Method method = RenderingEngine.class.getMethod("setDefaultZ");
        newStatefulInvocation( method, throwException( new RuntimeException() ) );
    }
    
    private void newStatefulDestroyInvocation() throws Exception    {
        Method method = RenderingEngine.class.getMethod("close");
        newStatefulInvocation( method );
    }

    /** uses the first stub passed (if any) on the will(); clause. */
    private void newStatefulInvocation( Method method, Stub...stubs )
    {
        mockInvocation = mock(MethodInvocation.class);
        invocation = (MethodInvocation) mockInvocation.proxy();
        mockInvocation.expects( atLeastOnce() ).method("getThis")
            .will( returnValue(stateful));
        mockInvocation.expects( atLeastOnce() ).method("getMethod")
            .will( returnValue(method));
        ArgumentsMatchBuilder amb = 
        mockInvocation.expects( once() ).method( "proceed" );
        if ( stubs != null && stubs.length > 0 ) amb.will(stubs[0]);
    }
    
    private void prepareThreadWithSession()
    {
        mockSession.expects( once() ).method( "beginTransaction" ).id("prep");
        SessionHolder sessionHolder = new SessionHolder(session);
        sessionHolder.setTransaction(sessionHolder.getSession()
                .beginTransaction());
        TransactionSynchronizationManager.bindResource(factory, sessionHolder);
    }

    private void reset(Mock...mocks)
    {
    	for (Mock mock : mocks) {
			if (mock!=null) mock.reset();
		}
    }
    
    private Stub printStackTrace()
    {
        return new StackTraceStub();
    }
    
    private class StackTraceStub implements Stub
    {
        public StringBuffer describeTo( StringBuffer buffer ) {
            return buffer.append("prints stack trace");
        }

        public Object invoke( Invocation invocation ) throws Throwable {
            new Throwable().printStackTrace();
            return null;
        }
    }

    private InvokedRecorder exactly( int count )
    {
        return new InvokedRecorder( count );
    }
    
    // TODO refactor out to ome.testing
    private class InvokedRecorder implements InvocationMatcher
    {
        private int actual = 0;
        private int expected = 0;
        
        public InvokedRecorder( int expected )
        {
            this.expected = expected;
        }
        
        public boolean matches( Invocation invocation ) {
            return true;
        }

        public void invoked( Invocation invocation ) {
            actual++;
        }

        public void verify() {
            Assert.assertTrue(
                    "expected method was not called "+
                    expected+" rather "+actual+" times.", actual == expected);
        }

        public boolean hasDescription() {
            return true;
        }

        public StringBuffer describeTo( StringBuffer buffer ) {
            buffer.append("expected "+expected+" times");
            buffer.append(" and has been invoked "+actual+" times");
            return buffer;
        }
    }
    
}
