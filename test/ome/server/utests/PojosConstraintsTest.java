/*
 * ome.server.utests.PojosConstraintsTest
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.server.utests;

// Java imports
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ome.api.IPojos;
import ome.conditions.ApiUsageException;
import ome.logic.PojosImpl;
import ome.model.containers.Dataset;
import ome.model.containers.Project;
import ome.model.core.Image;
import ome.security.basic.CurrentDetails;
import ome.services.util.ServiceHandler;
import ome.util.builders.PojoOptions;

import org.jmock.MockObjectTestCase;
import org.springframework.aop.framework.ProxyFactory;
import org.testng.annotations.Configuration;
import org.testng.annotations.Test;

/**
 * @author Josh Moore &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:josh.moore@gmx.de">josh.moore@gmx.de</a>
 * @version 1.0 <small> (<b>Internal version:</b> $Rev$ $Date$) </small>
 * @since Omero 2.0
 */
public class PojosConstraintsTest extends MockObjectTestCase {
    protected PojosImpl impl;

    protected IPojos manager;

    @Override
    @Configuration(beforeTestMethod = true)
    protected void setUp() throws Exception {
        super.setUp();
        impl = new PojosImpl();
        ProxyFactory factory = new ProxyFactory(impl);
        ServiceHandler serviceHandler = new ServiceHandler(new CurrentDetails());
        factory.addAdvice(serviceHandler);
        manager = (IPojos) factory.getProxy();
    }

    @Override
    @Configuration(afterTestMethod = true)
    protected void tearDown() throws Exception {
        manager = null;
    }

    @Test
    public void testFindAnnotations() throws Exception {
        T t = new T(ApiUsageException.class) {
            @Override
            public void doTest(Object[] arg) {
                manager.findAnnotations((Class) arg[0], (Set) arg[1],
                        (Set) arg[2], (Map) arg[3]);
            }
        };

        // param1: not null or wrong type
        t.blowup(true, null, new HashSet(), new HashSet(), new HashMap());
        t.blowup(true, Project.class, null, new HashSet(), new HashMap());
        t.blowup(false, Image.class, new HashSet(), new HashSet(),
                new HashMap()); // FIXME should check for empty sets.
        t.blowup(false, Dataset.class, new HashSet(), new HashSet(),
                new HashMap());

        // param2: not null
        t.blowup(true, Dataset.class, null, new HashSet(), new HashMap());

        // eek
        t.blowup(false, Dataset.class, new HashSet(), new HashSet(), null);

    }

    @Test
    public void testFindContainerHierarchies() throws Exception {
        T t = new T(ApiUsageException.class) {
            @Override
            public void doTest(Object[] arg) {
                manager.findContainerHierarchies((Class) arg[0], (Set) arg[1],
                        (Map) arg[2]);
            }
        };

        // param1: not null or wrong type
        t.blowup(true, null, new HashSet(), new HashMap());
        t.blowup(true, Dataset.class, new HashSet(), new HashMap());
        t.blowup(true, Image.class, new HashSet(), new HashMap());
        // FIXMEt.blowup(false,Project.class,new HashSet(), new HashMap());
        // FIXMEt.blowup(false,CategoryGroup.class,new HashSet(), new
        // HashMap());

        // param2:
        t.blowup(true, Project.class, null, new HashMap());
        // FIXMEt.blowup(false,Project.class,new HashSet(),new HashMap());

    }

    @Test
    public void testGetImages() throws Exception {
        T t = new T(ApiUsageException.class) {
            @Override
            public void doTest(Object[] arg) {
                manager.getImages((Class) arg[0], (Set) arg[1], (Map) arg[2]);
            }
        };

        // param1: not null
        t.blowup(true, null, new HashSet(), new HashMap());
        t.blowup(false, Dataset.class, new HashSet(), new HashMap());

    }

    @Test
    public void testGetUserImages() throws Exception {
        T t = new T(ApiUsageException.class) {
            @Override
            public void doTest(Object[] arg) {
                manager.getUserImages((Map) arg[0]);
            }
        };

        t.blowup(true, new PojoOptions().allExps().map());
        // TODO not in unit test t.blowup(false,new
        // PojoOptions().exp(1L).map());

    }

    @Test
    public void testLoadContainerHierary() throws Exception {
        Set ids;
        Map options;
        T t = new T(ApiUsageException.class) {
            @Override
            public void doTest(Object[] arg) {
                manager.loadContainerHierarchy((Class) arg[0], (Set) arg[1],
                        (Map) arg[2]);
            }
        };

        // param1: wrong or null class type
        ids = new HashSet<Integer>(Arrays.asList(1, 2, 3));
        options = new HashMap();
        t.blowup(true, null, ids, options);
        t.blowup(true, Image.class, ids, options);
        // FIXME do all blowup(false,...) belong in itests
        // t.blowup(false,Project.class,new HashSet(),options);

        // param2: not null unless there's an experimenter
        // FIXMEt.blowup(false,Project.class,null,new
        // PojoOptions().exp(1).map());
        t.blowup(true, Project.class, null, new HashMap());
        // FIXMEt.blowup(false,Project.class,new HashSet(),new
        // HashMap());//empty set is ok? TODO

        // param3: no constraints.

    }

    /**
     * part of the testing framework. Allow imlementers to specifiy a method to
     * be tested < <code>doTest</code> and then call it with an
     * {@see #blowup(boolean, Object[]) blowup}. Note: essentially a closure to
     * make calling this thing easy.
     */
    private static abstract class T {
        private Class t = null;

        public T() {
        }

        public T(Class thrown) {
            t = thrown;
        }

        public abstract void doTest(Object[] arg);

        public void setException(Class type) {
            t = type;
        }

        public void blowup(boolean exceptionExpected, Object... arg)
                throws Exception {
            boolean failed = false;
            try {
                doTest(arg);
                if (exceptionExpected) {
                    failed = true;
                    fail("Expected an exception here");
                }
            } catch (Exception e) {
                if (failed) {
                    throw e;
                }

                if (!exceptionExpected || t != null
                        && !t.isAssignableFrom(e.getClass())) {
                    throw new RuntimeException("Exception type " + e.getClass()
                            + " not expected. Rethrowing", e);
                }
            }
        }

    }

}
