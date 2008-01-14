/*
 * ome.security.BasicACLVoter
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.security.basic;

// Java imports

// Third-party libraries
import static ome.model.internal.Permissions.Right.WRITE;
import static ome.model.internal.Permissions.Role.GROUP;
import static ome.model.internal.Permissions.Role.USER;
import static ome.model.internal.Permissions.Role.WORLD;
import ome.annotations.RevisionDate;
import ome.annotations.RevisionNumber;
import ome.conditions.InternalException;
import ome.conditions.SecurityViolation;
import ome.model.IObject;
import ome.model.internal.Details;
import ome.model.internal.Permissions;
import ome.model.internal.Token;
import ome.security.ACLVoter;
import ome.security.SecuritySystem;
import ome.tools.hibernate.SecurityFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

/**
 * 
 * @author Josh Moore, josh.moore at gmx.de
 * @version $Revision$, $Date$
 * @see Token
 * @see SecuritySystem
 * @see Details
 * @see Permissions
 * @since 3.0-M3
 */
@RevisionDate("$Date$")
@RevisionNumber("$Revision$")
public class BasicACLVoter implements ACLVoter {

    private final static Log log = LogFactory.getLog(BasicACLVoter.class);

    private final BasicSecuritySystem secSys;

    public BasicACLVoter(BasicSecuritySystem securitySystem) {
        this.secSys = securitySystem;
    }

    // ~ Interface methods
    // =========================================================================

    /**
     * 
     */
    public boolean allowChmod(IObject iObject) {
        return secSys.isOwnerOrSupervisor(iObject);
    }

    /**
     * 
     * 
     * delegates to SecurityFilter because that is where the logic is defined
     * for the {@link #enableReadFilter(Object) read filter}
     */
    public boolean allowLoad(Class<? extends IObject> klass, Details d) {
        Assert.notNull(klass);
        // Assert.notNull(d);
        if (d == null || secSys.isSystemType(klass)) {
            return true;
        }
        return SecurityFilter.passesFilter(d, secSys.currentUserId(), secSys
                .memberOfGroups(), secSys.leaderOfGroups(), secSys
                .currentUserIsAdmin());
    }

    public void throwLoadViolation(IObject iObject) throws SecurityViolation {
        Assert.notNull(iObject);
        throw new SecurityViolation("Cannot read "
                + iObject.getClass().getName());
    }

    public boolean allowCreation(IObject iObject) {
        Assert.notNull(iObject);
        Class cls = iObject.getClass();

        if (secSys.hasPrivilegedToken(iObject) || secSys.currentUserIsAdmin()) {
            return true;
        }

        else if (secSys.isSystemType(cls)) {
            return false;
        }

        return true;
    }

    public void throwCreationViolation(IObject iObject)
            throws SecurityViolation {
        Assert.notNull(iObject);
        throw new SecurityViolation(iObject.getClass().getName()
                + " is a System-type, and may only be "
                + "created through privileged APIs.");
    }

    public boolean allowUpdate(IObject iObject, Details trustedDetails) {
        return allowUpdateOrDelete(iObject, trustedDetails);
    }

    public void throwUpdateViolation(IObject iObject) throws SecurityViolation {
        Assert.notNull(iObject);
        throw new SecurityViolation("Updating " + iObject + " not allowed.");
    }

    public boolean allowDelete(IObject iObject, Details trustedDetails) {
        return allowUpdateOrDelete(iObject, trustedDetails);
    }

    public void throwDeleteViolation(IObject iObject) throws SecurityViolation {
        Assert.notNull(iObject);
        throw new SecurityViolation("Deleting " + iObject + " not allowed.");
    }

    private boolean allowUpdateOrDelete(IObject iObject, Details trustedDetails) {
        Assert.notNull(iObject);

        // needs no details info
        if (secSys.hasPrivilegedToken(iObject) || secSys.currentUserIsAdmin()) {
            return true;
        } else if (secSys.isSystemType(iObject.getClass())) {
            return false;
        }

        // previously we were taking the details directly from iObject
        // iObject, however, is in a critical state. Values such as
        // Permissions, owner, and group may have been changed.
        Details d = trustedDetails;

        // this can now only happen if a table doesn't have permissions
        // and there aren't any of those. so let it be updated.
        if (d == null) {
            return true;
        }

        // the owner and group information might be null if the type
        // is intended to be a system-type but isn't marked as one
        // via SecuritySystem.isSystemType(). A NPE here might imply
        // that that information is out of sync.
        Long o = d.getOwner() == null ? null : d.getOwner().getId();
        Long g = d.getGroup() == null ? null : d.getGroup().getId();

        // needs no permissions info
        if (g != null && secSys.leaderOfGroups().contains(g)) {
            return true;
        }

        Permissions p = d.getPermissions();

        // this should never occur.
        if (p == null) {
            throw new InternalException(
                    "Permissions null! Security system "
                            + "failure -- refusing to continue. The Permissions should "
                            + "be set to a default value.");
        }

        // standard
        if (p.isGranted(WORLD, WRITE)) {
            return true;
        }
        if (p.isGranted(USER, WRITE) && o != null
                && o.equals(secSys.currentUserId())) {
            return true;
        }
        if (p.isGranted(GROUP, WRITE) && g != null
                && secSys.memberOfGroups().contains(g)) {
            return true;
        }

        return false;
    }

}
