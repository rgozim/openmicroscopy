/*
 * ome.server.itests.SecurityTest
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
package ome.server.itests;

//Java imports

//Third-party libraries
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.LazyInitializationException;
import org.hibernate.collection.AbstractPersistentCollection;
import org.hibernate.collection.PersistentSet;
import org.hibernate.mapping.PersistentClass;

//Application-internal dependencies
import ome.api.Write;
import ome.model.Category;
import ome.model.CategoryGroup;
import ome.model.Project;
import ome.testing.AbstractOmeroHierarchyBrowserIntegrationTest;
import ome.testing.OMEData;
import ome.testing.OMEPerformanceData;
import ome.util.Utils;

/** 
 * tests on the security system
 * 
 * @author  Josh Moore &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:josh.moore@gmx.de">josh.moore@gmx.de</a>
 * @version 1.0 
 * <small>
 * (<b>Internal version:</b> $Rev$ $Date$)
 * </small>
 * @since 1.0
 */
public class SecurityTest
        extends
            AbstractOmeroHierarchyBrowserIntegrationTest {

    private static Log log = LogFactory.getLog(SecurityTest.class);
    
    /**
     * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#getConfigLocations()
     */
    protected String[] getConfigLocations() {

        return ConfigHelper.getConfigLocations();
    }
    
    public SecurityTest(String name) {
        super(name,new OMEPerformanceData());
    }

    public SecurityTest(OMEData data) {
        super("OmeroGrinderTest with Data",data);
    }
 
    Write w;
    public void setWriteService(Write writeService){
    	w = writeService; 
    }
    
    @Override
    protected void onSetUp() throws Exception {
    	ome.security.Utils.setUserAuth();
    }
    
    public void testWritingAsRoleUser(){
    	try {
    		w.createDatasetAnnotation(1,"SecurityTest");
    		fail("Writes should fail as user");
    	} catch (Exception e){
    		// We want this to fail.
    		log.info("Caught expected exception:"+e.getMessage());
    	}
    }
    
}
