/*
 * ome.system.Roles
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.system;

//Java imports
import java.io.Serializable;

//Third-party libraries

//Application-internal dependencies
import ome.annotations.RevisionDate;
import ome.annotations.RevisionNumber;
import ome.model.meta.Experimenter;
import ome.model.meta.ExperimenterGroup;

/**
 * encapsulates the naming scheme for critical system groups and accounts. 
 * 
 * These values are also used during install to initialize the database.
 * 
 * @author Josh Moore, josh.moore at gmx.de
 * @version $Revision$, $Date$
 * @see ome.model.meta.Experimenter
 * @see ome.model.meta.ExperimenterGroup
 * @since 3.0-M3
 */
@RevisionDate("$Date$")
@RevisionNumber("$Revision$")
public final class Roles implements Serializable
{

	private static final long serialVersionUID = -7130017567693194758L;

	private final long 		rId;
    private final String 	rName;
    private final long 		sgId;
    private final String 	sgName;
    private final long 		ugId;
    private final String 	ugName;
	
    /** default constructor which assigns hard-coded values to all roles */
    public Roles()
    {
    	this.rId = 0L;
    	this.rName = "root";
    	this.sgId = 0L;
    	this.sgName = "system";
    	this.ugId = 1L;
    	this.ugName = "user";
    }
    
    /** constructor which allows full specification of all roles */
    public Roles( 
    		long rootId,
    		String rootName,
    		long systemGroupId,
    		String systemGroupName,
    		long userGroupId,
    		String userGroupName
    		)
    {
    	this.rId = rootId;
    	this.rName = rootName;
    	this.sgId = systemGroupId;
    	this.sgName = systemGroupName;
    	this.ugId = userGroupId;
    	this.ugName = userGroupName;
    }
    
    // ~ Checks
	// =========================================================================

    public boolean isRootUser(Experimenter user) {
		return user == null || user.getId() == null ? false : user.getId()
				.equals(getRootId());
	}
    
    public boolean isUserGroup(ExperimenterGroup group) {
		return group == null || group.getId() == null ? false : group.getId()
				.equals(getUserGroupId());
	}
    
    public boolean isSystemGroup(ExperimenterGroup group) {
		return group == null || group.getId() == null ? false : group.getId()
				.equals(getSystemGroupId());
	}
	
    // ~ Accessors
	// =========================================================================

	/**
	 * @return the id of the root user
	 */
	public long getRootId() {
		return rId;
	}

	/**
	 * @return the {@link Experimenter#getOmeName()} of the root user
	 */
	public String getRootName() {
		return rName;
	}

	/**
	 * @return the id of the system group
	 */
	public long getSystemGroupId() {
		return sgId;
	}

	/**
	 * @return the {@link ExperimenterGroup#getName()} of the system group
	 */
	public String getSystemGroupName() {
		return sgName;
	}

	/**
	 * @return the id of the user group
	 */
	public long getUserGroupId() {
		return ugId;
	}

	/**
	 * @return the {@link ExperimenterGroup#getName()} of the user group
	 */
	public String getUserGroupName() {
		return ugName;
	}
    
}
