/*
 *   $Id$
 *
 *   Copyright 2008 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.server.itests.sharing;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ome.api.IShare;
import ome.conditions.SecurityViolation;
import ome.conditions.ValidationException;
import ome.model.IObject;
import ome.model.annotations.Annotation;
import ome.model.annotations.TextAnnotation;
import ome.model.containers.Dataset;
import ome.model.core.Image;
import ome.model.internal.Permissions;
import ome.model.internal.Permissions.Right;
import ome.model.internal.Permissions.Role;
import ome.model.meta.Experimenter;
import ome.model.meta.Session;
import ome.parameters.Filter;
import ome.parameters.Parameters;
import ome.server.itests.AbstractManagedContextTest;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * 
 */
@Test(groups = { "sharing" })
public class SharingTest extends AbstractManagedContextTest {

    private static Filter justOne = new Filter().page(0, 1);

    protected IShare share;

    @BeforeMethod
    public void setup() {
        share = factory.getShareService();
        loginRoot();
    }

    @Test
    public void testDescription() {
        share = factory.getShareService();
        long id = share.createShare("before", null, null, null, null, false);
        share.setDescription(id, "after");
    }
    
    @Test
    public void testClose() {
        share = factory.getShareService();
        long id = share.createShare("to close", null, null, null, null, false);
        share.closeShare(id);
    }

    @Test
    public void testComments() {
        share = factory.getShareService();
        long id = share.createShare("disabled", null, null, null, null, false);
        TextAnnotation annotation = share.addComment(id, "hello");

        List<Annotation> annotations = share.getComments(id);
        assertContained(annotation, annotations);

        share.deleteComment(annotation);

        annotations = share.getComments(id);
        assertNotContained(annotation, annotations);

    }

    @Test
    public void testPrivateCommentsVisibleForMembers() {

        Experimenter e = loginNewUser();

        share = factory.getShareService();
        long id = share.createShare("with comments", null, null, null, null,
                true);
        TextAnnotation annotation = share.addComment(id, "hello");
        assertFalse(annotation.getDetails().getPermissions().isGranted(
                Role.GROUP, Right.READ));

        List<Annotation> annotations = share.getComments(id);
        assertContained(annotation, annotations);

        // Add a new user
        Experimenter member = loginNewUser();
        loginUser(e.getOmeName());
        share.addUser(id, member);
        
        // Then as that user try to obtain the comments
        loginUser(member.getOmeName());
        annotations = share.getComments(id);
        assertContained(annotation, annotations);

    }

    @Test
    public void testPrivateCommentsNotVisibleForNonMembers() {
     
        Experimenter e = loginNewUser();

        share = factory.getShareService();
        long id = share.createShare("with comments", null, null, null, null,
                true);
        TextAnnotation annotation = share.addComment(id, "hello");
        assertFalse(annotation.getDetails().getPermissions().isGranted(
                Role.GROUP, Right.READ));

        List<Annotation> annotations = share.getComments(id);
        assertContained(annotation, annotations);

        // NOT adding a new user
        Experimenter member = loginNewUser();
        // share.addUser(id, member);
        
        // Then as that user try to obtain the comments
        loginUser(member.getOmeName());
        annotations = share.getComments(id);
        assertNotContained(annotation, annotations);
    }
    
    @Test
    public void testPrivateCommentsVisibleForAdmin() {
     
        Experimenter e = loginNewUser();

        share = factory.getShareService();
        long id = share.createShare("with comments", null, null, null, null,
                true);
        TextAnnotation annotation = share.addComment(id, "hello");
        assertFalse(annotation.getDetails().getPermissions().isGranted(
                Role.GROUP, Right.READ));

        List<Annotation> annotations = share.getComments(id);
        assertContained(annotation, annotations);

        // NOT adding root either
                
        // Then as that root try to obtain the comments
        loginRoot();
        annotations = share.getComments(id);
        assertContained(annotation, annotations);
    }
    
    @Test
    public void testRetrieval() {
        loginRoot();

        long id = share.createShare("disabled", null, null, null, null, false);

        Set<Session> shares = share.getAllShares(true);
        assertShareNotReturned(id, shares);

        shares = share.getAllShares(false);
        assertShareReturned(id, shares);

        shares = share.getOwnShares(true);
        assertShareNotReturned(id, shares);

        shares = share.getOwnShares(false);
        assertShareReturned(id, shares);

        share.setActive(id, true);

        shares = share.getOwnShares(true);
        assertShareReturned(id, shares);

        share.activate(id);

        shares = share.getOwnShares(true);
        assertShareReturned(id, shares);

        // Create a new user and then add them to the share (as root)
        Experimenter member = loginNewUser();
        loginRoot();
        share.addUser(id, member);

        // Now as that user let's see how retrieval works
        loginUser(member.getOmeName());

        shares = share.getOwnShares(true);
        assertShareNotReturned(id, shares);

        shares = share.getOwnShares(false);
        assertShareNotReturned(id, shares);

        shares = share.getMemberShares(false);
        assertShareReturned(id, shares);

        shares = share.getMemberShares(true);
        assertShareReturned(id, shares);

        // As root, let's disable the share.
        loginRoot();
        share.setActive(id, false);
        loginUser(member.getOmeName());

        // Now it should not be returned
        shares = share.getMemberShares(true);
        assertShareNotReturned(id, shares);

    }

    @Test
    public void testMembershipFunctions() {

        Experimenter nonMember = loginNewUser();
        Experimenter secondMember = loginNewUser();
        Experimenter firstMember = loginNewUser();
        Experimenter owner = loginNewUser();

        String firstGuest = "example1@example.com";
        String secondGuest = "example2@example.com";

        Dataset d = new Dataset("Dataset for share");
        d.getDetails().setPermissions(Permissions.USER_PRIVATE);
        d = iUpdate.saveAndReturnObject(d);

        long id = share.createShare("description", null, Collections
                .singletonList(d), Arrays.asList(firstMember, secondMember),
                Arrays.asList(firstGuest, secondGuest), true);

        // Members

        assertEquals(1, share.getSharesOwnedBy(owner, true).size());
        assertEquals(0, share.getMemberSharesFor(owner, true).size());
        assertEquals(0, share.getSharesOwnedBy(firstMember, true).size());
        assertEquals(1, share.getMemberSharesFor(firstMember, true).size());
        assertEquals(0, share.getSharesOwnedBy(firstMember, true).size());
        assertEquals(1, share.getMemberSharesFor(firstMember, true).size());
        assertEquals(0, share.getSharesOwnedBy(nonMember, true).size());
        assertEquals(0, share.getMemberSharesFor(nonMember, true).size());
        assertEquals(2, share.getAllMembers(id).size());
        boolean foundFirst = false;
        boolean foundSecond = false;
        for (Experimenter e : share.getAllMembers(id)) {
            if (e.getId().equals(firstMember.getId())) {
                foundFirst = true;
            } else if (e.getId().equals(secondMember.getId())) {
                foundSecond = true;
            }
        }
        assertTrue(foundFirst);
        assertTrue(foundSecond);

        share.removeUser(id, secondMember);

        assertEquals(1, share.getSharesOwnedBy(owner, true).size());
        assertEquals(0, share.getMemberSharesFor(owner, true).size());
        assertEquals(0, share.getSharesOwnedBy(firstMember, true).size());
        assertEquals(1, share.getMemberSharesFor(firstMember, true).size());
        assertEquals(0, share.getSharesOwnedBy(secondMember, true).size());
        assertEquals(0, share.getMemberSharesFor(secondMember, true).size());
        assertEquals(0, share.getSharesOwnedBy(nonMember, true).size());
        assertEquals(0, share.getMemberSharesFor(nonMember, true).size());
        foundFirst = false;
        foundSecond = false;
        for (Experimenter e : share.getAllMembers(id)) {
            if (e.getId().equals(firstMember.getId())) {
                foundFirst = true;
            } else if (e.getId().equals(secondMember.getId())) {
                foundSecond = true;
            }
        }
        assertTrue(foundFirst);
        assertFalse(foundSecond);

        share.addUser(id, secondMember);

        assertEquals(1, share.getSharesOwnedBy(owner, true).size());
        assertEquals(0, share.getMemberSharesFor(owner, true).size());
        assertEquals(0, share.getSharesOwnedBy(firstMember, true).size());
        assertEquals(1, share.getMemberSharesFor(firstMember, true).size());
        assertEquals(0, share.getSharesOwnedBy(secondMember, true).size());
        assertEquals(1, share.getMemberSharesFor(secondMember, true).size());
        assertEquals(0, share.getSharesOwnedBy(nonMember, true).size());
        assertEquals(0, share.getMemberSharesFor(nonMember, true).size());
        foundFirst = false;
        foundSecond = false;
        for (Experimenter e : share.getAllMembers(id)) {
            if (e.getId().equals(firstMember.getId())) {
                foundFirst = true;
            } else if (e.getId().equals(secondMember.getId())) {
                foundSecond = true;
            }
        }
        assertTrue(foundFirst);
        assertTrue(foundSecond);

        // Guests

        assertEquals(2, share.getAllGuests(id).size());
        assertTrue(share.getAllGuests(id).contains(firstGuest));
        assertTrue(share.getAllGuests(id).contains(secondGuest));

        share.removeGuest(id, secondGuest);

        assertEquals(1, share.getAllGuests(id).size());
        assertTrue(share.getAllGuests(id).contains(firstGuest));
        assertFalse(share.getAllGuests(id).contains(secondGuest));

        share.addGuest(id, secondGuest);

        assertEquals(2, share.getAllGuests(id).size());
        assertTrue(share.getAllGuests(id).contains(firstGuest));
        assertTrue(share.getAllGuests(id).contains(secondGuest));

        // All users

        Set<String> names = share.getAllUsers(id);
        assertTrue(names.contains(firstGuest));
        assertTrue(names.contains(secondGuest));
        assertTrue(names.contains(firstMember.getOmeName()));
        assertTrue(names.contains(secondMember.getOmeName()));

    }

    @Test
    public void testElementFunctions() {

        Experimenter owner = loginNewUser();
        Dataset d = new Dataset("elements");
        d = iUpdate.saveAndReturnObject(d);
        long id = share.createShare("another description", null, null, null,
                null, true);

        assertEquals(0, share.getContentSize(id));
        assertEquals(0, share.getContents(id).size());
        assertEquals(0, share.getContentSubList(id, 0, 0).size());
        assertEquals(0, share.getContentMap(id).size());
        share.addObjects(id, d);
        assertEquals(1, share.getContentSize(id));
        assertEquals(1, share.getContents(id).size());
        assertEquals(1, share.getContentSubList(id, 0, 1).size());
        assertEquals(1, share.getContentMap(id).size());
        
        Dataset d2 = new Dataset("elements transitively");
        Image i2 = new Image(new Timestamp(0), "transitive image");
        d2.linkImage(i2);
        d2 = iUpdate.saveAndReturnObject(d2);
        
        share.addObject(id, d2);
        assertEquals(4, share.getContentSize(id));
    }

    @Test
    public void testOnlyOwnerMembersAndGuestsCanActivateShare() {

        Experimenter nonMember = loginNewUser();
        Experimenter member = loginNewUser();
        Experimenter owner = loginNewUser();

        Dataset d = new Dataset("Dataset for share");
        d.getDetails().setPermissions(Permissions.USER_PRIVATE);
        d = iUpdate.saveAndReturnObject(d);

        long id = share.createShare("description", null, Collections
                .singletonList(d), Collections.singletonList(member), null,
                true);

        loginUser(owner.getOmeName());
        share.activate(id);
        iQuery.get(Dataset.class, d.getId());

        loginUser(member.getOmeName());
        share.activate(id);
        iQuery.get(Dataset.class, d.getId());

        loginUser(nonMember.getOmeName());
        try {
            share.activate(id);
            fail("Should not be allowed");
        } catch (ValidationException e) {
            // ok
        }
        try {
            iQuery.get(Dataset.class, d.getId());
            fail("Should not be allowed");
        } catch (SecurityViolation e) {
            // ok
        }
    }

    @Test
    public void testShareCreationAndViewing() {

        // New user who should be able to see the share.
        Experimenter member = loginNewUser();

        loginRoot();
        Dataset d = new Dataset("Dataset for share");
        d.getDetails().setPermissions(Permissions.USER_PRIVATE);
        d = iUpdate.saveAndReturnObject(d);

        long id = share.createShare("description", null, Collections
                .singletonList(d), Collections.singletonList(member), null,
                true);

        loginUser(member.getOmeName());
        share.activate(id);
        iQuery.get(Dataset.class, d.getId());
    }

    @Test
    public void testUserAddsPrivateImageAndThenShares() {

        Experimenter owner = loginNewUser();
        Image i = new Image(new Timestamp(System.currentTimeMillis()), "test");
        i.getDetails().setPermissions(Permissions.USER_PRIVATE);
        i = iUpdate.saveAndReturnObject(i);

        long id = share.createShare("desc", null, Collections.singletonList(i),
                null, null, true);

        Experimenter member = loginNewUser();
        loginUser(owner.getOmeName());
        share.addUser(id, member);

        loginUser(member.getOmeName());
        assertEquals(1, share.getContentSize(id));
        
        try {
            iQuery.find(Image.class, i.getId());
            fail("Should throw");
        } catch (SecurityViolation sv) {
            // good.
        }
        share.activate(id);
        assertNotNull(iQuery.find(Image.class, i.getId()));
        
        Parameters p = new Parameters();
        p.addIds(Arrays.asList(i.getId()));
        String sql = "select im from Image im where im.id in (:ids) order by im.name";
        List<Image> res = iQuery.findAllByQuery(sql, p);
        assertEquals(1, res.size());
    }

    @Test
    public void testWhoCanDoWhat() {
        fail("NYI");
    }

    @Test
    public void testIsTheUUIDProtected() {
        fail("NYI");
    }

    // Assertions
    // =========================================================================

    private void assertShareReturned(long id, Set<Session> shares) {
        boolean found = false;
        for (Session session : shares) {
            found |= session.getId().longValue() == id;
        }
        assertTrue(found);
    }

    private void assertShareNotReturned(long id, Set<Session> shares) {
        for (Session session : shares) {
            assertFalse(id + "==" + session, id == session.getId());
        }
    }

    private <I extends IObject> boolean contains(I obj, List<I> list) {
        boolean found = false;
        for (I test : list) {
            if (test.getId().equals(obj.getId())) {
                found = true;
            }
        }
        return found;
    }

    private <I extends IObject> void assertContained(I obj, List<I> list) {
        boolean found = contains(obj, list);
        assertTrue(found);
    }

    private <I extends IObject> void assertNotContained(I obj, List<I> list) {
        boolean found = contains(obj, list);
        assertFalse(found);
    }

}
