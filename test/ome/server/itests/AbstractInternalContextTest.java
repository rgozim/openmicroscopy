package ome.server.itests;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

import ome.api.local.LocalQuery;
import ome.api.local.LocalUpdate;
import ome.model.enums.EventType;
import ome.model.meta.Experimenter;
import ome.model.meta.ExperimenterGroup;
import ome.security.CurrentDetails;
import ome.system.OmeroContext;


public class AbstractInternalContextTest
        extends AbstractTransactionalDataSourceSpringContextTests
{
    @Override
    protected String[] getConfigLocations()
    {
        return new String[]{};
    }
    
    protected ConfigurableApplicationContext getContext(Object key)
    {
        return OmeroContext.getInternalServerContext();
    }

    private   Session                       session;    
    protected SessionFactory                sessionFactory;
    protected LocalQuery                    iQuery;
    protected LocalUpdate                   iUpdate;
    protected JdbcTemplate                  jdbcTemplate;

    @Override
    protected void onSetUpBeforeTransaction() throws Exception
    {
        sessionFactory = (SessionFactory) applicationContext.getBean("sessionFactory");
        jdbcTemplate = (JdbcTemplate) applicationContext.getBean("jdbcTemplate");
        
        // This is an internal test we don't want the wrapped spring beans. 
        iQuery = (LocalQuery) applicationContext.getBean("ome.api.IQuery");
        iUpdate = (LocalUpdate) applicationContext.getBean("ome.api.IUpdate");

    }

    @Override
    protected void onSetUpInTransaction() throws Exception
    {
        /* make sure the whole test runs within a single session,
         * unless closeSession() is called */
        setComplete();
        openSession();
        prepareCurrentDetails();
    }

    protected void prepareCurrentDetails()
    {
        /* TODO run experiment as root. Dangerous? */
        Experimenter o = 
            (Experimenter) iQuery.getById(Experimenter.class,0);
        ExperimenterGroup g = 
            (ExperimenterGroup) iQuery.getById(ExperimenterGroup.class,0);
        CurrentDetails.setOwner(o);
        CurrentDetails.setGroup(g);
        EventType test = 
            (EventType) iQuery.getUniqueByFieldEq(EventType.class,"value","Test");
        CurrentDetails.newEvent(test);
    }

    protected void openSession()
    {
        session = SessionFactoryUtils.getSession(sessionFactory,true);
    }

    // TODO remove?
    // cf: http://forum.hibernate.org/viewtopic.php?t=929167
    protected void closeSession() throws Exception
    {
        flush();
        clear();
//        session.connection().commit();
//        TransactionSynchronizationManager.unbindResource(sessionFactory);
//        SessionFactoryUtils.releaseSession(session, sessionFactory);
//  SessionFactoryUtils.processDeferredClose(sessionFactory); // TODO see OpenInView
    }
    
    protected void clear()
    {
        session.clear();
    }
    
    protected void flush()
    { 
        session.flush();
    }

}
