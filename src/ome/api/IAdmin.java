/*
 * ome.api.IAdmin
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

package ome.api;

// Java imports

// Third-party libraries

// Application-internal dependencies

/**
 *  Administration interface providing access to admin-only functionality as 
 *  well as JMX-based server access. All methods require membership in
 *  privileged {@link ExperimenterGroup groups}.
 * 
 * @author <br>
 *         Josh Moore &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:josh.moore@gmx.de"> josh.moore@gmx.de</a>
 * @version 3.0 <small> (<b>Internal version:</b> $Revision: $ $Date: $)
 *          </small>
 * @since OME3.0
 */
public interface IAdmin extends ServiceInterface{
    
    /** uses JMX to refresh the login cache <em>if supported</em>. Some backends
     * may not provide refreshing. This may be called internally during some
     * other administrative tasks. The exact implementation of this depends on
     * the application server and the authentication/authorization backend.
     */
    void synchronizeLoginCache();

}
