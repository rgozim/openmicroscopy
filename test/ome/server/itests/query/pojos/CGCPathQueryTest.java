/*
 *   $Id$
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.server.itests.query.pojos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import ome.api.IPojos;
import ome.conditions.ApiUsageException;
import ome.parameters.Parameters;
import ome.server.itests.AbstractManagedContextTest;
import ome.services.query.PojosCGCPathsQueryDefinition;
import ome.util.builders.PojoOptions;

/*
 TODO FIXME NEW Data with only one in each group (not all images in all categories in cg e.g.)
 */

public class CGCPathQueryTest extends AbstractManagedContextTest {
    PojosCGCPathsQueryDefinition q;

    List list;

    protected void creation_fails(Parameters parameters) {
        try {
            q = new PojosCGCPathsQueryDefinition(parameters);
            fail("Should have failed!");
        } catch (IllegalArgumentException e) {
        } catch (ApiUsageException e) {
        }
    }

    @Test
    public void test_illegal_arguments() throws Exception {

        creation_fails(new Parameters().addIds(null) // Null
                .addAlgorithm(IPojos.CLASSIFICATION_NME).addOptions(null));

        creation_fails(new Parameters().addIds(new ArrayList()) // Empty!
                .addAlgorithm(IPojos.CLASSIFICATION_NME).addOptions(null));
        creation_fails(new Parameters().addIds(Arrays.asList(1L)).addAlgorithm(
                null) // Null here
                .addOptions(null));
        creation_fails(new Parameters().addIds(Arrays.asList(1)) // Integer
                                                                    // not Long
                .addAlgorithm(IPojos.CLASSIFICATION_NME).addOptions(null));
    }

    @Test
    public void test_simple_usage() throws Exception {
        Long doesntExist = -1L;
        q = new PojosCGCPathsQueryDefinition(new Parameters().addIds(
                Arrays.asList(doesntExist)).addAlgorithm(
                IPojos.DECLASSIFICATION).addOptions(null));

        list = (List) iQuery.execute(q);

        q = new PojosCGCPathsQueryDefinition(new Parameters().addIds(
                Arrays.asList(doesntExist)).addAlgorithm(
                IPojos.CLASSIFICATION_NME).addOptions(null));

        list = (List) iQuery.execute(q);

        q = new PojosCGCPathsQueryDefinition(new Parameters().addIds(
                Arrays.asList(doesntExist)).addAlgorithm(
                IPojos.CLASSIFICATION_ME).addOptions(null));

        list = (List) iQuery.execute(q);

    }

    @Test(groups = "broken")
    public void test_declassification() throws Exception {
        Parameters declassification = new Parameters()
                .addAlgorithm(IPojos.DECLASSIFICATION);
        run_tests(declassification, 0, 4, 4, 0, 4);
    }

    @Test(groups = "broken")
    public void test_classification_nme() throws Exception {
        Parameters classificationNME = new Parameters()
                .addAlgorithm(IPojos.CLASSIFICATION_NME);
        run_tests(classificationNME, 16, 12, 12, 8, 4); // TODO need more
    }

    @Test(groups = "broken")
    public void test_classification_me() throws Exception {
        Parameters classificationME = new Parameters()
                .addAlgorithm(IPojos.CLASSIFICATION_ME);
        run_tests(classificationME, 16, 12, 12, 0, 12); // TODO need more
    }

    private void run_tests(Parameters algorithm, int... sizes) {
        PojoOptions po = new PojoOptions().exp(10000L);
        Parameters notRootOptions = new Parameters().addOptions(po.map());
        Parameters noOptions = new Parameters().addOptions(null);

        // No categories for image.
        q = new PojosCGCPathsQueryDefinition(new Parameters(noOptions).addAll(
                algorithm).addIds(Arrays.asList(5050L)));

        list = (List) iQuery.execute(q);
        assertListSize(sizes[0]);

        // Well-defined categories (root).
        q = new PojosCGCPathsQueryDefinition(new Parameters(noOptions).addAll(
                algorithm).addIds(Arrays.asList(5051L)));

        list = (List) iQuery.execute(q);
        assertListSize(sizes[1]);

        // Well-defined categories (user).
        q = new PojosCGCPathsQueryDefinition(new Parameters(noOptions).addAll(
                algorithm).addIds(Arrays.asList(5551L)));

        list = (List) iQuery.execute(q);
        assertListSize(sizes[2]);

        // Filtering out root on root's objects.
        q = new PojosCGCPathsQueryDefinition(new Parameters(notRootOptions)
                .addAll(algorithm) // Not root
                .addIds(Arrays.asList(5051L))); // Belongs to root.

        list = (List) iQuery.execute(q);
        assertListSize(sizes[3]);

        // Filtering out root on user's objects.
        q = new PojosCGCPathsQueryDefinition(new Parameters(notRootOptions)
                .addAll(algorithm) // Not root
                .addIds(Arrays.asList(5551L))); // Belongs to user.

        list = (List) iQuery.execute(q);
        assertListSize(sizes[4]);

    }

    private void assertListSize(int size) {
        assertTrue(String.format("List.size() != %d but was %s", size, list
                .size()), list.size() == size);
    }

}
