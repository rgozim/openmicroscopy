/*
 *   $Id$
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.server.itests.sec;

import ome.conditions.SecurityViolation;
import ome.model.ILink;
import ome.model.containers.Dataset;
import ome.model.containers.Project;
import ome.model.core.Image;
import ome.model.core.Pixels;
import ome.model.internal.Permissions;
import ome.model.internal.Permissions.Flag;
import ome.model.internal.Permissions.Right;
import ome.model.internal.Permissions.Role;
import ome.model.meta.Experimenter;
import ome.model.meta.ExperimenterGroup;
import ome.parameters.Parameters;
import ome.server.itests.AbstractManagedContextTest;
import ome.testing.ObjectFactory;

import org.testng.annotations.Configuration;
import org.testng.annotations.Test;

/**
 * provides a simple litmus test for the locking and unlocking of permissions
 * This simply validates the prototype and should not be taken as an implication
 * of completeness. See the client-side test for that.
 */
@Test(groups = { "ticket:337" })
public class LockingTest extends AbstractManagedContextTest {

    String uname, gname;

    Project p;

    Dataset d;

    Experimenter e1, e2;

    ExperimenterGroup g1, g2;

    @Configuration(beforeTestClass = true)
    public void init() throws Exception {

        setUp(); // get services
        loginRoot();

        uname = uuid();
        gname = uuid();

        ExperimenterGroup g = new ExperimenterGroup();
        g.setName(gname);
        g1 = new ExperimenterGroup(iAdmin.createGroup(g), false);

        Experimenter e = new Experimenter();
        e.setFirstName("ticket:337");
        e.setLastName("user 1");
        e.setOmeName(uname);

        e1 = new Experimenter(iAdmin.createUser(e, gname), false);

        e.setFirstName("ticket:337");
        e.setLastName("user 2 -- in same group");
        e.setOmeName(uuid());

        e2 = new Experimenter(iAdmin.createUser(e, gname), false);

        g2 = new ExperimenterGroup(1L, false);

        // we have to create the extra user and group, because otherwise
        // user1 wouldn't be able to change the group field anyway!

    }

    /** tests both transient and managed entities */
    public void test_ProjectIsLockedOnAddedDataset() throws Exception {

        login(uname, gname, "Test");

        p = new Project();
        p.setName("ticket:337");
        p = iUpdate.saveAndReturnObject(p);

        assertFalse(p.getDetails().getPermissions().isSet(Flag.LOCKED));

        d = new Dataset();
        d.setName("ticket:337");
        p.linkDataset(d);

        p = iUpdate.saveAndReturnObject(p);
        d = p.linkedDatasetList().get(0);

        p = iQuery.find(p.getClass(), p.getId().longValue());
        d = iQuery.find(d.getClass(), d.getId().longValue());

        assertTrue(p.getDetails().getPermissions().isSet(Flag.LOCKED));
        assertTrue(d.getDetails().getPermissions().isSet(Flag.LOCKED));

    }

    @Test(dependsOnMethods = "test_ProjectIsLockedOnAddedDataset")
    public void test_RootCantOverride() throws Exception {
        loginRoot();
        reacquire();

        // try to change
        p.getDetails().getPermissions().revoke(Role.USER, Right.READ);
        assertFails();

        reacquire();
        p.getDetails().getPermissions().unSet(Flag.LOCKED);
        assertNoChange();

        // this succeeds because of loosened semantics. see:
        // https://trac.openmicroscopy.org.uk/omero/changeset/944
        // https://trac.openmicroscopy.org.uk/omero/ticket/337
        reacquire();
        p.getDetails().setOwner(e2);
        assertSucceeds();

        // now return it to the previous owner for testing.
        reacquire();
        p.getDetails().setOwner(e1);
        assertSucceeds();

        // but we can't change the group. too dynamic.
        reacquire();
        p.getDetails().setGroup(g2);
        assertFails();

    }

    @Test(dependsOnMethods = "test_ProjectIsLockedOnAddedDataset")
    public void test_UserCantOverride() throws Exception {

        login(uname, gname, "Test");
        reacquire();

        // try to change
        p.getDetails().getPermissions().revoke(Role.USER, Right.READ);
        assertFails();

        reacquire();
        p.getDetails().getPermissions().unSet(Flag.LOCKED);
        assertNoChange();

        // no set owner

        reacquire();
        p.getDetails().setGroup(g2);
        assertFails();

    }

    @Test(dependsOnMethods = { "test_RootCantOverride", "test_UserCantOverride" }, groups = "ticket:366")
    public void test_OnceDatasetIsRemovedCanUnlock() throws Exception {

        login(uname, gname, "Test");

        ILink link = iQuery.findByQuery(
                "select pdl from ProjectDatasetLink pdl "
                        + "where parent.id = :pid and child.id = :cid",
                new Parameters().addLong("pid", p.getId()).addLong("cid",
                        d.getId()));
        iUpdate.deleteObject(link);

        iAdmin.unlock(p);

        p = iQuery.find(p.getClass(), p.getId());
        assertFalse(p.getDetails().getPermissions().isSet(Flag.LOCKED));
    }

    @Test
    public void test_AllowInitialLock() throws Exception {

        login(uname, gname, "Test");

        Permissions perms = new Permissions().set(Flag.LOCKED);

        p = new Project();
        p.setName("ticket:337");
        p.getDetails().setPermissions(perms);

        Project t = iUpdate.saveAndReturnObject(p);
        assertTrue(t.getDetails().getPermissions().isSet(Flag.LOCKED));

        t = iUpdate.saveAndReturnObject(p); // cloning
        t.getDetails().getPermissions().set(Flag.LOCKED);
        t = iUpdate.saveAndReturnObject(t); // save changes on managed
        assertTrue(t.getDetails().getPermissions().isSet(Flag.LOCKED));

    }

    @Test(groups = "ticket:339")
    public void test_HandlesExplicitPermissionReduction() throws Exception {
        login(uname, gname, "Test");

        p = new Project();
        p.setName("ticket:339");
        d = new Dataset();
        d.setName("ticket:339");
        p.linkDataset(d);

        Permissions perms = Permissions.READ_ONLY; // relatively common
        // use-case
        p.getDetails().setPermissions(perms);

        Project t = iUpdate.saveAndReturnObject(p);

    }

    @Test(groups = "ticket:357")
    public void test_OneToOnesGetLockedAsWell() throws Exception {
        login(uname, gname, "Test");

        Image i = new Image();
        i.setName("ticket:357");
        Pixels p = ObjectFactory.createPixelGraph(null);
        i.addPixels(p);

        i = iUpdate.saveAndReturnObject(i);
        p = i.iteratePixels().next();

        assertTrue(p.getDetails().getPermissions().isSet(Flag.LOCKED));

    }

    // ~ Helpers
    // =========================================================================

    private void reacquire() {
        p = iQuery.find(p.getClass(), p.getId().longValue());
        assertTrue("Permissions should still be locked.", p.getDetails()
                .getPermissions().isSet(Flag.LOCKED));
    }

    private void assertSucceeds() {
        p = iUpdate.saveAndReturnObject(p);
    }

    private void assertFails() {
        try {
            assertSucceeds();
            fail("secvio!");
        } catch (SecurityViolation sv) {
            // ok
        }
    }

    private void assertNoChange() {
        Permissions p1 = p.getDetails().getPermissions();
        p = iUpdate.saveAndReturnObject(p);
        Permissions p2 = p.getDetails().getPermissions();
        assertTrue(p1.sameRights(p2));
    }
}
