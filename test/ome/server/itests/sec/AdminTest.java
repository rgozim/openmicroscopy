/*
 *   $Id$
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.server.itests.sec;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ome.conditions.ApiUsageException;
import ome.conditions.SecurityViolation;
import ome.model.IObject;
import ome.model.containers.Dataset;
import ome.model.containers.Project;
import ome.model.core.Image;
import ome.model.internal.Permissions;
import ome.model.internal.Permissions.Flag;
import ome.model.meta.Experimenter;
import ome.model.meta.ExperimenterGroup;
import ome.server.itests.AbstractManagedContextTest;
import ome.system.Roles;
import ome.util.IdBlock;

import org.testng.annotations.ExpectedExceptions;
import org.testng.annotations.Test;

public class AdminTest extends AbstractManagedContextTest {

    // ~ IAdmin.createUser
    // =========================================================================

    @Test
    @ExpectedExceptions(ApiUsageException.class)
    public void testUserAccountCreationWithNull() throws Exception {
        iAdmin.createUser(null, null);
    }

    @Test
    @ExpectedExceptions(ApiUsageException.class)
    public void testUserAccountCreationWithEmpty() throws Exception {
        Experimenter e = new Experimenter();
        iAdmin.createUser(e, null);
    }

    @Test
    @ExpectedExceptions(ApiUsageException.class)
    public void testUserAccountCreationWithUnknownGroup() throws Exception {
        Experimenter e = new Experimenter();
        iAdmin.createUser(e, uuid()); // uuid won't exist
    }

    @Test
    public void testUserAccountCreation() throws Exception {
        ExperimenterGroup g = testGroup();
        iAdmin.createGroup(g);
        Experimenter e = testExperimenter();
        e = iAdmin.getExperimenter(iAdmin.createUser(e, g.getName()));
        assertNotNull(e.getEmail());
        assertNotNull(e.getOmeName());
        assertNotNull(e.getFirstName());
        assertNotNull(e.getLastName());
        int size = e.sizeOfGroupExperimenterMap();
        assertTrue(String.format("%d not 2", size), size == 2);
    }

    // ~ IAdmin.createSystemUser
    // =========================================================================

    @Test
    @ExpectedExceptions(ApiUsageException.class)
    public void testSysUserAccountCreationWithNull() throws Exception {
        iAdmin.createUser(null, null);
    }

    @Test
    @ExpectedExceptions(ApiUsageException.class)
    public void testSysUserAccountCreationWithEmpty() throws Exception {
        Experimenter e = new Experimenter();
        iAdmin.createSystemUser(e);
    }

    @Test
    public void testSysUserAccountCreation() throws Exception {
        Experimenter e = testExperimenter();
        e = iAdmin.getExperimenter(iAdmin.createSystemUser(e));
        assertNotNull(e.getEmail());
        assertNotNull(e.getOmeName());
        assertNotNull(e.getFirstName());
        assertNotNull(e.getLastName());
        assertTrue(iAdmin.containedGroups(e.getId()).length == 2);
        assertTrue(e.sizeOfGroupExperimenterMap() == 2);
    }

    // ~ IAdmin.createExperimenter
    // =========================================================================

    @Test
    @ExpectedExceptions(ApiUsageException.class)
    public void testExperimenterAccountCreationWithAllNulls() throws Exception {
        iAdmin.createExperimenter(null, null, null);
    }

    @Test
    @ExpectedExceptions(ApiUsageException.class)
    public void testExperimenterAccountCreationWithEmpty() throws Exception {
        Experimenter e = new Experimenter();
        iAdmin.createExperimenter(e, null, null);
    }

    @Test
    public void testExperimenterAccountCreation() throws Exception {
        Experimenter e = testExperimenter();
        e = iAdmin.getExperimenter(iAdmin.createExperimenter(e,
                new ExperimenterGroup(0L, false), null));
        assertNotNull(e.getEmail());
        assertNotNull(e.getOmeName());
        assertNotNull(e.getFirstName());
        assertNotNull(e.getLastName());
        assertTrue(e.sizeOfGroupExperimenterMap() == 1);
    }

    @Test(groups = "ticket:1021")
    public void testDefaultGroupNotAddedTwice() throws Exception {
        List<ExperimenterGroup> groups = iAdmin.lookupGroups();
        ExperimenterGroup def = null, nonDef = null;
        for (ExperimenterGroup group : groups) {
            if (group.getName().equals("user")
                    || group.getName().equals("system")) {
                continue;
            }
            if (def == null) {
                def = group;
            } else if (nonDef == null) {
                nonDef = group;
            } else {
                break;
            }
        }
        assertNotNull(def);
        assertNotNull(nonDef);

        Experimenter e = testExperimenter();
        long id = iAdmin.createExperimenter(e, def, nonDef);
        e = iAdmin.lookupExperimenter(e.getOmeName());
        assertEquals("should be 2", 2, e.sizeOfGroupExperimenterMap());

        e = testExperimenter();
        id = iAdmin.createExperimenter(e, def, nonDef, def);
        e = iAdmin.lookupExperimenter(e.getOmeName());
        assertEquals("should still be 2", 2, e.sizeOfGroupExperimenterMap());

    }

    private ExperimenterGroup testGroup() {
        ExperimenterGroup g = new ExperimenterGroup();
        g.setName(uuid());
        return g;
    }

    private Experimenter testExperimenter() {
        Experimenter e = new Experimenter();
        e.setEmail("blah");
        e.setFirstName("foo");
        e.setLastName("bar");
        e.setOmeName(UUID.randomUUID().toString());
        return e;
    }

    // ~ Groups
    // =========================================================================
    @Test(groups = "ticket:293")
    public void testUserCanOnlySetDetailsOnOwnObject() throws Exception {
        ExperimenterGroup g = testGroup();
        iAdmin.createGroup(g);

        Experimenter e1 = testExperimenter();
        iAdmin.createUser(e1, g.getName());

        loginUser(e1.getOmeName());

        Image i = new Image();
        i.setName("test");
        i = iUpdate.saveAndReturnObject(i);

        // this user should not be able to change things
        Experimenter e2 = testExperimenter();
        iAdmin.createUser(e2, g.getName());

        loginUser(e2.getOmeName());

        try {
            iAdmin.changeOwner(i, e2.getOmeName());
            fail("secvio!");
        } catch (SecurityViolation sv) {
        }
        try {
            iAdmin.changeGroup(i, "system");
            fail("secvio!");
        } catch (SecurityViolation sv) {
        }
        try {
            iAdmin.changePermissions(i, Permissions.EMPTY);
            fail("secvio!");
        } catch (SecurityViolation sv) {
        }

        // guarantee that the client-side check for ticket:293 still holds.
        // see: TicketsUpTo500Test
        loginUser(e1.getOmeName());
        iAdmin.changePermissions(i, Permissions.EMPTY);
        iAdmin.changePermissions(i, Permissions.DEFAULT);
        loginRoot();
        iAdmin.changePermissions(i, Permissions.EMPTY);
        iAdmin.changePermissions(i, Permissions.DEFAULT);

    }

    @Test
    public void testUserCanOnlySetDetailsToOwnGroup() throws Exception {
        ExperimenterGroup g = testGroup();
        iAdmin.createGroup(g);

        Experimenter e1 = testExperimenter();
        e1.setId(iAdmin.createUser(e1, g.getName()));

        ExperimenterGroup g1 = new ExperimenterGroup(), g2 = new ExperimenterGroup();

        g1.setName(uuid());
        g2.setName(uuid());

        g1.setId(iAdmin.createGroup(g1));
        g2.setId(iAdmin.createGroup(g2));

        login(e1.getOmeName(), g.getName(), "Test");

        Image i = new Image();
        i.setName("test");
        i = iUpdate.saveAndReturnObject(i);

        try {
            iAdmin.changeGroup(i, g2.getName());
            fail("secvio!");
        } catch (SecurityViolation sv) {
            // ok
        }

        // add the user to these groups and try again.
        iAdmin.addGroups(e1, g1, g2);

        // should now work.
        iAdmin.changeGroup(i, g2.getName());

    }

    @Test(groups = "ticket:343")
    public void testSetGroupOwner() throws Exception {
        ExperimenterGroup g = testGroup();
        iAdmin.createGroup(g);

        Experimenter e1 = testExperimenter();
        e1.setId(iAdmin.createUser(e1, g.getName()));

        ExperimenterGroup g1 = new ExperimenterGroup();
        g1.setName(uuid());
        g1.setId(iAdmin.createGroup(g1));

        loginRoot();

        iAdmin.setGroupOwner(g1, e1);

        ExperimenterGroup test = iQuery
                .get(ExperimenterGroup.class, g1.getId());
        assertEquals(test.getDetails().getOwner().getId(), e1.getId());

    }

    // ~ chgrp
    // =========================================================================
    @Test
    public void testUserUsesChgrpThroughAdmin() throws Exception {

        Experimenter e = loginNewUser();

        // a second group
        loginRoot();
        ExperimenterGroup g = new ExperimenterGroup();
        g.setName(UUID.randomUUID().toString());
        g = iAdmin.getGroup(iAdmin.createGroup(g));
        iAdmin.addGroups(e, g);
        loginUser(e.getOmeName());

        // create a new image
        Image i = new Image();
        i.setName(UUID.randomUUID().toString());
        i = factory.getUpdateService().saveAndReturnObject(i);

        // it should be in some other group
        Long group = i.getDetails().getGroup().getId();
        assertFalse(group.equals(g.getId()));

        // now let's try to change that group
        factory.getAdminService().changeGroup(i, g.getName());
        Image copy = factory.getQueryService().get(Image.class, i.getId());
        Long test = copy.getDetails().getGroup().getId();

        assertFalse(test.equals(group));
        assertTrue(test.equals(g.getId()));

    }

    // ~ IAdmin.setDefaultGroup
    // =========================================================================
    @Test
    public void testSetDefaultGroup() throws Exception {
        loginRoot();

        // test group
        String gid = uuid();
        ExperimenterGroup g = new ExperimenterGroup();
        g.setName(gid);
        g = iAdmin.getGroup(iAdmin.createGroup(g));

        // create a new user for the test
        Experimenter e = new Experimenter();
        e.setFirstName("user admin setters");
        e.setLastName("test");
        e.setOmeName(UUID.randomUUID().toString());
        e = iAdmin.getExperimenter(iAdmin.createUser(e, gid));

        // check current default group
        ExperimenterGroup def = iAdmin.getDefaultGroup(e.getId());
        assertEquals(def.getId(), g.getId());

        // new test group
        String gid2 = uuid();
        ExperimenterGroup g2 = new ExperimenterGroup();
        g2.setName(gid2);
        g2 = iAdmin.getGroup(iAdmin.createGroup(g2));

        // now change
        iAdmin.addGroups(e, g2);
        iAdmin.setDefaultGroup(e, g2);

        // test
        def = iAdmin.getDefaultGroup(e.getId());
        assertEquals(def.getId(), g2.getId());

    }

    // ~ IAdmin.addGroups & .removeGroups
    // =========================================================================
    @Test
    public void testPlusAndMinusGroups() throws Exception {
        loginRoot();

        ExperimenterGroup g = testGroup();
        iAdmin.createGroup(g);

        // create a new user for the test
        Experimenter e = new Experimenter();
        e.setFirstName("user admin setters");
        e.setLastName("test");
        e.setOmeName(UUID.randomUUID().toString());
        e = iAdmin.getExperimenter(iAdmin.createUser(e, g.getName()));

        int size = e.sizeOfGroupExperimenterMap();
        assertTrue(String.format("%d not 2", size), size == 2);

        // two new test groups
        ExperimenterGroup g1 = new ExperimenterGroup();
        g1.setName(UUID.randomUUID().toString());
        g1 = iAdmin.getGroup(iAdmin.createGroup(g1));
        ExperimenterGroup g2 = new ExperimenterGroup();
        g2.setName(UUID.randomUUID().toString());
        g2 = iAdmin.getGroup(iAdmin.createGroup(g2));

        iAdmin.addGroups(e, g1, g2);

        // test
        e = iAdmin.lookupExperimenter(e.getOmeName());
        assertTrue(e.linkedExperimenterGroupList().size() == 4);

        iAdmin.removeGroups(e, g1);
        e = iAdmin.lookupExperimenter(e.getOmeName());
        assertTrue(e.linkedExperimenterGroupList().size() == 3);
    }

    // ~ IAdmin.contained*
    // =========================================================================
    @Test
    public void testContainedUsersAndGroups() throws Exception {
        loginRoot();

        ExperimenterGroup g = testGroup();
        iAdmin.createGroup(g);

        // create a new user for the test
        Experimenter e = new Experimenter();
        e.setFirstName("user admin setters");
        e.setLastName("test");
        e.setOmeName(UUID.randomUUID().toString());
        e = iAdmin.getExperimenter(iAdmin.createUser(e, g.getName()));

        // two new test groups
        ExperimenterGroup g1 = new ExperimenterGroup();
        g1.setName(UUID.randomUUID().toString());
        g1 = iAdmin.getGroup(iAdmin.createGroup(g1));
        ExperimenterGroup g2 = new ExperimenterGroup();
        g2.setName(UUID.randomUUID().toString());
        g2 = iAdmin.getGroup(iAdmin.createGroup(g2));

        // add them all together
        iAdmin.addGroups(e, g1, g2);

        // test
        Experimenter[] es = iAdmin.containedExperimenters(g1.getId());
        assertTrue(es.length == 1);
        assertTrue(es[0].getId().equals(e.getId()));

        ExperimenterGroup[] gs = iAdmin.containedGroups(e.getId());
        assertTrue(gs.length == 4);
        List<Long> ids = new ArrayList<Long>();
        for (ExperimenterGroup group : gs) {
            ids.add(group.getId());
        }
        assertTrue(ids.contains(1L));
        assertTrue(ids.contains(g1.getId()));
        assertTrue(ids.contains(g2.getId()));
    }

    // ~ IAdmin.lookup* & .get*
    // =========================================================================
    @Test
    public void testLookupAndGet() throws Exception {
        loginRoot();
        // create a new user for the test
        Experimenter e = new Experimenter();
        e.setFirstName("user admin setters");
        e.setLastName("test");
        e.setOmeName(UUID.randomUUID().toString());
        e = iAdmin.getExperimenter(iAdmin.createSystemUser(e));

        loginUser(e.getOmeName());

        Experimenter test_e = iAdmin.lookupExperimenter(e.getOmeName());
        ExperimenterGroup test_g = iAdmin.getGroup(0L);

        assertTrue(test_e.linkedExperimenterGroupList().size() == 2);
        assertTrue(test_g.eachLinkedExperimenter(new IdBlock()).contains(
                e.getId()));
    }

    @Test(groups = "ticket:910")
    public void testLookupGroupsReturnsExperimentersWithGroupsLoaded() {
        loginRoot();

        List<ExperimenterGroup> list = iAdmin.lookupGroups();
        ExperimenterGroup group = list.get(0);
        Experimenter exp = group.linkedExperimenterList().get(0);
        assertNotNull(exp.getPrimaryGroupExperimenterMap());
    }

    // ~ IAdmin.unlock
    // =========================================================================

    @Test
    public void testUnlock() throws Exception {
        loginRoot();

        boolean[] unlocked;

        Project pt, p = new Project();
        p.setName("unlock test");

        Dataset dt, d = new Dataset();
        d.setName("unlock test");

        pt = iUpdate.saveAndReturnObject(p);
        unlocked = iAdmin.unlock(pt);
        assertTrue(unlocked[0]);

        pt.linkDataset(d);
        pt = iUpdate.saveAndReturnObject(pt);
        assertTrue(pt.getDetails().getPermissions().isSet(Flag.LOCKED));
        unlocked = iAdmin.unlock(pt);
        assertFalse(unlocked[0]);
        iUpdate.deleteObject((IObject) pt.collectDatasetLinks(null).get(0));
        unlocked = iAdmin.unlock(pt);
        assertTrue(unlocked[0]);
        pt = iQuery.get(pt.getClass(), pt.getId());
        assertFalse(pt.getDetails().getPermissions().isSet(Flag.LOCKED));

    }

    // ~ Passwords
    // =========================================================================

    /**
     * using this test to visually inspect the log output for changeUserPassword
     * it will fail and so there should be no side-effects.
     */
    // SECURITY CHECKS AREN'T DONE FROM WITHIN. NEED TO HANDLE THIS!!!
    @Test(groups = { "ticket:209", "security", "broken" })
    public void testUnallowedPasswordChange() throws Exception {
        loginRoot();

        // and a new group
        ExperimenterGroup g = new ExperimenterGroup();
        g.setName(UUID.randomUUID().toString());
        iAdmin.createGroup(g);

        // create a new user for the test
        Experimenter e = new Experimenter();
        e.setFirstName("user admin setters");
        e.setLastName("test");
        e.setOmeName(UUID.randomUUID().toString());
        iAdmin.createUser(e, g.getName());

        loginUser(e.getOmeName());
        try {
            iAdmin.changeUserPassword("root", "THIS SHOULD NOT BE VISIBLE.");
            fail("secvio!");
        } catch (SecurityViolation ex) {
            // ok.
        }

    }

    // ~ Security context
    // =========================================================================

    @Test(groups = "ticket:328")
    public void testRoles() throws Exception {
        loginRoot();

        Roles r = iAdmin.getSecurityRoles();
        assertNotNull(r.getRootName());
        assertNotNull(r.getSystemGroupName());
        assertNotNull(r.getUserGroupName());
    }

    // ~ Bugs
    // =========================================================================

    public void testSetDefaultGroupCanNotUpateRows() throws Exception {

        Experimenter e1 = loginNewUser();
        Experimenter e2 = loginNewUser();
        loginRoot();

        ExperimenterGroup eg = iAdmin.getDefaultGroup(e1.getId());
        iAdmin.addGroups(e2, eg);
        iAdmin.setDefaultGroup(e2, eg);

    }

}
