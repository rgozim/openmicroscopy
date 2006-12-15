/*
 * ome.server.utests.UpdateFilterMockTest
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.server.utests;

// Java imports

// Third-party libraries
import java.util.Arrays;
import java.util.List;

import org.jmock.MockObjectTestCase;
import org.jmock.core.Constraint;
import org.testng.annotations.*;

// Application-internal dependencies
import ome.api.ITypes;
import ome.api.local.LocalAdmin;
import ome.api.local.LocalQuery;
import ome.api.local.LocalUpdate;
import ome.model.IObject;
import ome.model.core.Image;
import ome.model.enums.EventType;
import ome.model.internal.Details;
import ome.model.meta.Event;
import ome.model.meta.Experimenter;
import ome.model.meta.ExperimenterGroup;
import ome.security.basic.BasicSecuritySystem;
import ome.security.SecuritySystem;
import ome.system.Principal;
import ome.testing.MockServiceFactory;
import ome.tools.hibernate.UpdateFilter;

/**
 * @author Josh Moore &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:josh.moore@gmx.de">josh.moore@gmx.de</a>
 * @version 1.0 <small> (<b>Internal version:</b> $Rev$ $Date$) </small>
 * @since Omero 2.0
 */
@Test
public class AbstractLoginMockTest extends MockObjectTestCase {

    public final static Long ROOT_OWNER_ID = 0L;

    public final static Long SYS_GROUP_ID = 0L;

    public final static Long INITIAL_EVENT_ID = 0L;

    public Long USER_OWNER_ID = 1L;

    public Long USER_GROUP_ID = 1L;

    public Experimenter ROOT;

    public ExperimenterGroup ROOT_GROUP;

    public Event INITIAL_EVENT;

    public EventType BOOTSTRAP;

    public EventType USEREVENT;

    public Experimenter USER;

    public ExperimenterGroup USER_GROUP;

    public List<Long> LEADER_OF_GROUPS;

    public List<Long> MEMBER_OF_GROUPS;

    protected UpdateFilter filter;

    protected SecuritySystem sec;

    protected MockServiceFactory sf;

    @Override
    @Configuration(beforeTestMethod = true)
    protected void setUp() throws Exception {
        super.setUp();

        sf = new MockServiceFactory();
        sec = new BasicSecuritySystem(sf);

        sf.mockAdmin = mock(LocalAdmin.class);
        sf.mockQuery = mock(LocalQuery.class);
        sf.mockUpdate = mock(LocalUpdate.class);
        sf.mockTypes = mock(ITypes.class);

        filter = new UpdateFilter();

        ROOT = new Experimenter(ROOT_OWNER_ID);
        ROOT_GROUP = new ExperimenterGroup(SYS_GROUP_ID);
        INITIAL_EVENT = new Event(INITIAL_EVENT_ID);
        BOOTSTRAP = new EventType(INITIAL_EVENT_ID);
        USEREVENT = new EventType(INITIAL_EVENT_ID + 1);

        BOOTSTRAP.setValue("Bootstrap");
        USEREVENT.setValue("User");

        USER = new Experimenter(USER_OWNER_ID);
        USER_GROUP = new ExperimenterGroup(USER_GROUP_ID);

        rootLogin();

    }

    protected void rootLogin() {
        LEADER_OF_GROUPS = Arrays.asList(0L);
        MEMBER_OF_GROUPS = Arrays.asList(0L, 1L);
        sf.mockAdmin.expects(atLeastOnce()).method("userProxy")
                .with(eq("root")).will(returnValue(ROOT));
        sf.mockAdmin.expects(atLeastOnce()).method("groupProxy").with(
                eq("system")).will(returnValue(ROOT_GROUP));
        sf.mockAdmin.expects(atLeastOnce()).method("getLeaderOfGroupIds").with(
                eq(ROOT)).will(returnValue(LEADER_OF_GROUPS));
        sf.mockAdmin.expects(atLeastOnce()).method("getMemberOfGroupIds").with(
                eq(ROOT)).will(returnValue(MEMBER_OF_GROUPS));
        sf.mockTypes.expects(atLeastOnce()).method("getEnumeration").with(
                eq(EventType.class), eq("Bootstrap")).will(
                returnValue(BOOTSTRAP));
        INITIAL_EVENT.setType(BOOTSTRAP);
        sf.mockUpdate.expects(atLeastOnce()).method("saveAndReturnObject")
                .with(new Type(Event.class)).will(returnValue(INITIAL_EVENT));
        sec.login(new Principal("root", "system", "Bootstrap"));
        sec.loadEventContext(false);
    }

    protected void userLogin() {
        LEADER_OF_GROUPS = Arrays.asList(1L);
        MEMBER_OF_GROUPS = Arrays.asList(1L);
        sf.mockAdmin.expects(atLeastOnce()).method("userProxy").with(
                eq("user1")).will(returnValue(USER));
        sf.mockAdmin.expects(atLeastOnce()).method("groupProxy").with(
                eq("user")).will(returnValue(USER_GROUP));
        sf.mockAdmin.expects(atLeastOnce()).method("getLeaderOfGroupIds").with(
                eq(USER)).will(returnValue(LEADER_OF_GROUPS));
        sf.mockAdmin.expects(atLeastOnce()).method("getMemberOfGroupIds").with(
                eq(USER)).will(returnValue(MEMBER_OF_GROUPS));
        sf.mockTypes.expects(atLeastOnce()).method("getEnumeration").with(
                eq(EventType.class), eq("User")).will(returnValue(USEREVENT));
        INITIAL_EVENT.setType(USEREVENT);
        sf.mockUpdate.expects(atLeastOnce()).method("saveAndReturnObject")
                .with(new Type(Event.class)).will(returnValue(INITIAL_EVENT));
        sec.login(new Principal("user1", "user", "User"));
        sec.loadEventContext(false);
    }

    @Override
    @Configuration(afterTestMethod = true)
    protected void tearDown() throws Exception {
        super.verify();
        super.tearDown();
        sec.clearEventContext();
    }

    // ~ Protected helpers
    // =========================================================================

    protected void checkSomeoneIsLoggedIn() {
        assertNotNull(sec.getEventContext().getCurrentUserId());
        assertNotNull(sec.getEventContext().getCurrentGroupId());
        assertNotNull(sec.getEventContext().getCurrentEventType());
    }

    /** creates a "managed image" (has ID) to the currently logged in user. */
    protected Image managedImage() {
        checkSomeoneIsLoggedIn();
        Image i = new Image(0L);
        Details managed = new Details();
        managed.setOwner(new Experimenter(sec.getEventContext()
                .getCurrentUserId(), false));
        managed.setGroup(new ExperimenterGroup(sec.getEventContext()
                .getCurrentGroupId(), false));
        managed.setCreationEvent(new Event(sec.getEventContext()
                .getCurrentEventId(), false));
        i.setDetails(managed);
        return i;
    }

    protected void assertDetails(IObject o) {
        assertDetails(o, ROOT_OWNER_ID, SYS_GROUP_ID, INITIAL_EVENT_ID);
    }

    protected void assertDetails(IObject o, Long owner, Long group, Long event) {
        assertNotNull(o.getDetails());
        assertNotNull(o.getDetails().getOwner());
        assertNotNull(o.getDetails().getGroup());
        assertNotNull(o.getDetails().getCreationEvent());

        assertTrue(o.getDetails().getOwner().getId().equals(owner));
        assertTrue(o.getDetails().getGroup().getId().equals(group));
        // This is now null because secsys creates a new event
        // assertTrue( o.getDetails().getCreationEvent().getId().equals( event
        // ));

    }

    /**
     * setups the mocking of hibernateTemplate.load(Image,id) the argument
     * provided should be created in exactly the same way as the image passed
     * into filter. E.g.: <code>
     *  Image i = managedImage();
     *  willLoadImage( managedImage() );
     *  filter.filter(null,i);
     *  </code>
     * 
     * One exception to this rule is the testing of unloaded status where the
     * use will resemble: <code>
     *  Image i = new Image(...).unload();
     *  willLoadImage( new Image(...).setDetails(...);
     *  filter.filter(null,i);
     *  </code>
     */
    protected void willLoadImage(Image persistentImage) {
        sf.mockQuery.expects(once()).method("get").with(eq(Image.class),
                eq(persistentImage.getId())).will(returnValue(persistentImage));
    }

    protected void willLoadUser(Long id) {
        sf.mockQuery.expects(atLeastOnce()).method("get").with(
                eq(Experimenter.class), eq(id)).will(
                returnValue(new Experimenter(id)));
    }

    protected void willLoadEvent(Long id) {
        sf.mockQuery.expects(once()).method("get")
                .with(eq(Event.class), eq(id)).will(returnValue(new Event(id)));
    }

    protected void willLoadEventType(Long id) {
        sf.mockQuery.expects(once()).method("get").with(eq(EventType.class),
                eq(id)).will(returnValue(new EventType(id)));
    }

    protected void willLoadGroup(Long id) {
        sf.mockQuery.expects(atLeastOnce()).method("get").with(
                eq(ExperimenterGroup.class), eq(id)).will(
                returnValue(new ExperimenterGroup(id)));
    }

    protected void willCheckRootDetails() {
        // sf.mockQuery.expects( once() ).method( "get" )
        // .with( eq( Event.class ), eq( INITIAL_EVENT_ID ))
        // .will( returnValue( INITIAL_EVENT ));
        sf.mockQuery.expects(once()).method("get").with(eq(EventType.class),
                eq(INITIAL_EVENT_ID)).will(returnValue(BOOTSTRAP));
        sf.mockQuery.expects(once()).method("get").with(eq(Experimenter.class),
                eq(ROOT_OWNER_ID)).will(returnValue(ROOT));
        sf.mockQuery.expects(once()).method("get").with(
                eq(ExperimenterGroup.class), eq(SYS_GROUP_ID)).will(
                returnValue(ROOT_GROUP));
    }

    protected void willCheckUserDetails() {
        // sf.mockQuery.expects( once() ).method( "get" )
        // .with( eq( Event.class ), eq( userEvent.getId() ))
        // .will( returnValue( userEvent ));
        sf.mockQuery.expects(once()).method("get").with(eq(Experimenter.class),
                eq(USER_OWNER_ID)).will(returnValue(USER));
        sf.mockQuery.expects(once()).method("get").with(
                eq(ExperimenterGroup.class), eq(USER_GROUP_ID)).will(
                returnValue(USER_GROUP));
    }

    protected void chown(IObject i, Long userId) {
        Details myDetails = i.getDetails() == null ? new Details() : i
                .getDetails();
        myDetails.setOwner(new Experimenter(userId));
        i.setDetails(myDetails);
    }

    protected void chgrp(IObject i, Long grpId) {
        Details myDetails = i.getDetails() == null ? new Details() : i
                .getDetails();
        myDetails.setGroup(new ExperimenterGroup(grpId));
        i.setDetails(myDetails);
    }

    protected void setRootDetails(IObject i) {
        setDetails(i, ROOT_OWNER_ID, SYS_GROUP_ID, INITIAL_EVENT_ID);
    }

    protected void setDetails(IObject i, Long rootId, Long groupId, Long eventId) {
        Details myDetails = new Details();
        myDetails.setOwner(new Experimenter(1L));
        myDetails.setGroup(ROOT_GROUP);
        myDetails.setCreationEvent(INITIAL_EVENT);
        i.setDetails(myDetails);
    }

    private static class Type implements Constraint {
        private Class type;

        public Type(Class type) {
            this.type = type;
        }

        public StringBuffer describeTo(StringBuffer buffer) {
            buffer.append(" of type ");
            buffer.append(type);
            return buffer;
        }

        public boolean eval(Object o) {
            if (type.isAssignableFrom(o.getClass())) {
                return true;
            }
            return false;
        }
    }
}
