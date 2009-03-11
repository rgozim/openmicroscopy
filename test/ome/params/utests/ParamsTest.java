/*
 *   $Id$
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.params.utests;

import org.testng.annotations.*;
import junit.framework.TestCase;

import ome.parameters.Filter;
import ome.parameters.Parameters;

public class ParamsTest extends TestCase {

    @Test
    public void test_ParamsWithFilter() throws Exception {
        Parameters p = new Parameters(new Filter().unique());
        assertTrue(p.isUnique());
    }

    @Test
    public void test_ParamsWithCopy() throws Exception {
        Parameters p = new Parameters();
        p.addBoolean("TEST", Boolean.TRUE);
        Parameters p2 = new Parameters(p);
        assertTrue(p2.keySet().contains("TEST"));
    }

}
