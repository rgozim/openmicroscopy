/*
 *   Copyright 20078 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.delete;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import ome.api.IDelete;
import ome.api.ThumbnailStore;
import ome.conditions.ApiUsageException;
import ome.conditions.SecurityViolation;
import ome.formats.MockedOMEROImportFixture;
import ome.model.annotations.CommentAnnotation;
import ome.model.annotations.ImageAnnotationLink;
import ome.model.containers.Dataset;
import ome.model.containers.DatasetImageLink;
import ome.model.core.Channel;
import ome.model.core.Image;
import ome.model.core.LogicalChannel;
import ome.model.core.Pixels;
import ome.model.meta.Experimenter;
import ome.model.meta.ExperimenterGroup;
import ome.model.screen.Plate;
import ome.model.screen.Well;
import ome.model.screen.WellSample;
import ome.parameters.Parameters;
import ome.server.itests.AbstractManagedContextTest;

import org.springframework.util.ResourceUtils;
import org.testng.annotations.Test;

@Test(groups = { "integration" })
public class DeleteServiceTest extends AbstractManagedContextTest {

    @Test
    public void testSimpleDelete() throws Exception {
        Image i = makeImage(true);
        IDelete srv = this.factory.getServiceByClass(IDelete.class);
        srv.deleteImage(i.getId(), true);
    }

    @Test
    public void testDeleteWithOtherDatasets() throws Exception {
        Image i = makeImage(true);
        Dataset d = new Dataset();
        d.setName("deletes");
        DatasetImageLink link = new DatasetImageLink();
        link.link(d, i.proxy());
        this.factory.getUpdateService().saveObject(link);
        IDelete srv = this.factory.getServiceByClass(IDelete.class);
        srv.deleteImage(i.getId(), true);
    }

    public void testDeleteWithAnnotation() throws Exception {
        Image i = makeImage(true);
        CommentAnnotation ta = new CommentAnnotation();
        ta.setNs("");
        ta.setTextValue("");
        ImageAnnotationLink link = new ImageAnnotationLink();
        link.link(i.proxy(), ta);
        this.factory.getUpdateService().saveObject(link);
        IDelete srv = this.factory.getServiceByClass(IDelete.class);
        srv.deleteImage(i.getId(), true);
    }

    @Test(expectedExceptions = ApiUsageException.class)
    public void testDeleteWithOtherDatasetsNoForce() throws Exception {
        Image i = makeImage(true);
        Dataset d = new Dataset();
        d.setName("deletes");
        DatasetImageLink link = new DatasetImageLink();
        link.link(d, i.proxy());
        this.factory.getUpdateService().saveObject(link);
        IDelete srv = this.factory.getServiceByClass(IDelete.class);
        srv.deleteImage(i.getId(), false);
    }

    public void testDeletePreviewList() throws Exception {
        Image i = makeImage(true);
        Dataset d = new Dataset();
        d.setName("deletes");
        DatasetImageLink link = new DatasetImageLink();
        link.link(d, i.proxy());
        this.factory.getUpdateService().saveObject(link);
        IDelete srv = this.factory.getServiceByClass(IDelete.class);
        assertTrue(srv.previewImageDelete(i.getId(), false).size() > 1);
    }

    public void testDeleteByIds() throws Exception {
        Image i1 = makeImage(false);
        Image i2 = makeImage(false);
        Image i3 = makeImage(false);
        IDelete srv = this.factory.getDeleteService();
        srv.deleteImages(new HashSet<Long>(Arrays.asList(i1.getId(),
                i2.getId(), i3.getId())), false);
    }

    @Test(expectedExceptions = ApiUsageException.class)
    public void testDeleteByIdsWithOtherDatasetNoForce() throws Exception {
        Image i1 = makeImage(false);
        Image i2 = makeImage(false);
        Image i3 = makeImage(false);
        Dataset containsAll = new Dataset("containsAll");
        containsAll.linkImage(i1);
        containsAll.linkImage(i2);
        containsAll.linkImage(i3);
        iUpdate.saveObject(containsAll);
        IDelete srv = this.factory.getDeleteService();
        srv.deleteImages(new HashSet<Long>(Arrays.asList(i1.getId(),
                i2.getId(), i3.getId())), false);
    }

    public void testDeleteByIdsWithOtherDatasetForce() throws Exception {
        Image i1 = makeImage(false);
        Image i2 = makeImage(false);
        Image i3 = makeImage(false);
        Dataset containsAll = new Dataset("containsAll");
        containsAll.linkImage(i1);
        containsAll.linkImage(i2);
        containsAll.linkImage(i3);
        iUpdate.saveObject(containsAll);
        IDelete srv = this.factory.getDeleteService();
        srv.deleteImages(new HashSet<Long>(Arrays.asList(i1.getId(),
                i2.getId(), i3.getId())), true);
    }

    public void testDeleteByDataset() throws Exception {
        Image i1 = makeImage(false);
        Image i2 = makeImage(false);
        Image i3 = makeImage(false);
        Dataset containsAll = new Dataset("containsAll");
        containsAll.linkImage(i1);
        containsAll.linkImage(i2);
        containsAll.linkImage(i3);
        iUpdate.saveObject(containsAll);
        IDelete srv = this.factory.getDeleteService();
        srv.deleteImagesByDataset(containsAll.getId(), false);
    }

    public void testDeleteByDatasetWithOtherDatasetForce() throws Exception {
        Image i1 = makeImage(true);
        Image i2 = makeImage(true);
        Image i3 = makeImage(true);
        Dataset containsAll = new Dataset("containsAll");
        containsAll.linkImage(i1);
        containsAll.linkImage(i2);
        containsAll.linkImage(i3);
        iUpdate.saveObject(containsAll);
        IDelete srv = this.factory.getDeleteService();
        srv.deleteImagesByDataset(containsAll.getId(), true);
    }

    @Test(expectedExceptions = ApiUsageException.class)
    public void testDeleteByDatasetWithOtherDatasetNoForce() throws Exception {
        Image i1 = makeImage(true);
        Image i2 = makeImage(true);
        Image i3 = makeImage(true);
        Dataset containsAll = new Dataset("containsAll");
        containsAll.linkImage(i1);
        containsAll.linkImage(i2);
        containsAll.linkImage(i3);
        iUpdate.saveObject(containsAll);
        IDelete srv = this.factory.getDeleteService();
        srv.deleteImagesByDataset(containsAll.getId(), false);
    }

    @Test
    public void testDeleteHandlesMultiLinkedLogicalChannels() throws Exception {
        Image i1 = makeImage(true);
        Image i2 = makeImage(true);

        String sql = "select lc from LogicalChannel lc "
                + "join fetch lc.channels ch join ch.pixels p join p.image i where i.id = ";
        List<LogicalChannel> lcs1 = iQuery.findAllByQuery(sql + i1.getId(),
                null);
        Channel c1 = lcs1.get(0).iterateChannels().next();
        List<LogicalChannel> lcs2 = iQuery.findAllByQuery(sql + i2.getId(),
                null);
        c1.setLogicalChannel(lcs2.get(0));
        iUpdate.saveObject(c1);

        IDelete srv = this.factory.getDeleteService();
        srv.deleteImage(i1.getId(), true);
    }

    // Multiuser tests
    // =========================================================================

    @Test(expectedExceptions = SecurityViolation.class)
    public void testDeleteByDatasetWithTwoImagesFromDifferentUsers()
            throws Exception {

        Experimenter e1 = loginNewUser();
        Image i1 = makeImage(false);

        Experimenter e2 = loginNewUser();
        Image i2 = makeImage(false);

        Dataset containsAll = new Dataset("containsAll");
        containsAll.linkImage(i1);
        containsAll.linkImage(i2);
        iUpdate.saveObject(containsAll);

        IDelete srv = this.factory.getDeleteService();
        srv.deleteImagesByDataset(containsAll.getId(), true);
    }

    public void testDeleteImageAfterViewedByAnotherUser() throws Exception {

        Experimenter e1 = loginNewUser();
        Image i1 = makeImage(false);
        Pixels p1 = i1.iteratePixels().next();

        Experimenter e2 = loginNewUserInOtherUsersGroup(e1);

        ThumbnailStore tb = this.factory.createThumbnailService();
        tb.setPixelsId(p1.getId());
        tb.resetDefaults();
        tb.setPixelsId(p1.getId());
        tb.createThumbnails();

        loginUser(e1.getOmeName());
        factory.getDeleteService().deleteImage(i1.getId(), false);
    }

    public void testDeleteSettingsAfterViewedByAnotherUser() throws Exception {

        Experimenter e1 = loginNewUser();
        Image i1 = makeImage(false);
        Pixels p1 = i1.iteratePixels().next();

        Experimenter e2 = loginNewUserInOtherUsersGroup(e1);

        // In 4.0, default permissions were made private which prevents this
        // test from being carried out as a regular user. Now testing as
        // admin.
        loginRoot();
        iAdmin.addGroups(e2, new ExperimenterGroup(0L, false));
        loginUser(e2.getOmeName());

        ThumbnailStore tb = this.factory.createThumbnailService();
        tb.setPixelsId(p1.getId());
        tb.resetDefaults();
        tb.setPixelsId(p1.getId());
        tb.createThumbnails();

        loginUser(e1.getOmeName());
        factory.getDeleteService().deleteSettings(i1.getId());
    }

    public void testDeleteSettingsAfterViewedByRoot() throws Exception {

        Experimenter e1 = loginNewUser();
        Image i1 = makeImage(false);
        Pixels p1 = i1.iteratePixels().next();

        loginRoot();

        ThumbnailStore tb = this.factory.createThumbnailService();
        tb.setPixelsId(p1.getId());
        tb.resetDefaults();
        tb.setPixelsId(p1.getId());
        tb.createThumbnails();

        loginUser(e1.getOmeName());
        factory.getDeleteService().deleteSettings(i1.getId());
    }

    @Test(groups = "ticket:1228")
    public void testDeleteWithProjectionRemovesRelatedTo() throws Exception {

        Experimenter e1 = loginNewUser();
        Image i1 = makeImage(false);
        Pixels p1 = i1.getPixels(0);

        Image i2 = makeImage(false);
        Pixels p2 = i2.getPixels(0);

        p2.setRelatedTo(p1);
        assertEquals(p1.getId(), iUpdate.saveAndReturnObject(p2).getRelatedTo()
                .getId());

        /*
         * Leads to deadlock: IProjection prj =
         * this.factory.getProjectionService(); prj.projectPixels(p1.getId(),
         * p1.getPixelsType(), IProjection.MAXIMUM_INTENSITY, 0, p1.getSizeT(),
         * // T Arrays.asList(0), 0, 0, p1.getSizeZ(), // Z "projection");
         */
        factory.getDeleteService().deleteImage(i1.getId(), true);
    }

    // 4.1 - Plates
    // =========================================================================

    @Test(groups = "ticket:1228")
    public void testDeleteByPlate() throws Exception {

        String name = "2007.08.02.16.43.24.xdce";
        Experimenter e1 = loginNewUser();
        try {
            makeImage("classpath:"+name, false);

        } catch (FileNotFoundException fnfe) {
            // Partial support
        }
        Plate p = iQuery.findByQuery("select p from Plate p "
                + "where name like :name " + "order by id desc",
                new Parameters().page(0, 1).addString("name",name));
        
        factory.getDeleteService().deletePlate(p.getId());
    }

}
