/*
 * ome.security.SecuritySystem
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

package ome.security;

//Java imports

//Third-party libraries

//Application-internal dependencies
import ome.conditions.SecurityViolation;
import ome.model.IObject;
import ome.model.enums.EventType;
import ome.model.internal.Details;
import ome.model.meta.Event;
import ome.model.meta.Experimenter;
import ome.model.meta.ExperimenterGroup;

/** 
 * various tools needed throughout Omero. 
 * 
 * @author  Josh Moore &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:josh.moore@gmx.de">josh.moore@gmx.de</a>
 * @version 1.0 
 * <small>
 * (<b>Internal version:</b> $Rev$ $Date$)
 * </small>
 * @since 1.0
 */
public interface SecuritySystem
{

	// ~ Checks
	// =========================================================================
	boolean isReady( );
	boolean isSystemType( Class<? extends IObject> klass );	
	
	// ~ Read security
	// =========================================================================
	void enableReadFilter( Object session );
	void disableReadFilter( Object session );
	
	// ~ Write security
	// =========================================================================
	boolean allowCreation( IObject iObject );
	boolean allowUpdate( IObject iObject );
	void throwCreationViolation( IObject iObject ) throws SecurityViolation;
	void throwUpdateViolation( IObject iObject ) throws SecurityViolation;
	
	// ~ Privileged accounts
	// =========================================================================
	long getRootId();
	long getSystemGroupId();
	long getUserGroupId();
	String getRootName();
	String getSystemGroupName();
	String getUserGroupName();
	
	// ~ Details (for OmeroInterceptor)
	// =========================================================================
	Details transientDetails( IObject iObject );
	Details managedDetails( IObject iObject, Details previousDetails );
	
	// ~ CurrentDetails delegation
	// =========================================================================

	Long currentUserId();
	Experimenter currentUser();
	ExperimenterGroup currentGroup();
	Event currentEvent();
	
	boolean emptyDetails( );
	void addLog( String action, Class klass, Long id );
	void newEvent( EventType type );
	Event getCurrentEvent();
	void setCurrentEvent( Event event );
	void clearCurrentDetails();
	void setCurrentDetails();
	
	// ~ Actions
	// =========================================================================
	<T extends IObject> T doAction( T obj, SecureAction action );
}
