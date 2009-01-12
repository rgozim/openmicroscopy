/*
 *   Copyright 2008 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.delete;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.Interceptors;

import ome.api.IDelete;
import ome.api.ServiceInterface;
import ome.api.local.LocalAdmin;
import ome.conditions.ApiUsageException;
import ome.conditions.SecurityViolation;
import ome.conditions.ValidationException;
import ome.logic.AbstractLevel2Service;
import ome.logic.SimpleLifecycle;
import ome.model.IObject;
import ome.model.annotations.ImageAnnotationLink;
import ome.model.containers.CategoryImageLink;
import ome.model.containers.DatasetImageLink;
import ome.model.core.Channel;
import ome.model.core.Image;
import ome.model.core.LogicalChannel;
import ome.model.core.Pixels;
import ome.model.display.ChannelBinding;
import ome.model.display.RenderingDef;
import ome.model.internal.Details;
import ome.parameters.Parameters;
import ome.security.AdminAction;
import ome.security.SecuritySystem;
import ome.services.util.OmeroAroundInvoke;
import ome.system.EventContext;
import ome.util.CBlock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.annotation.ejb.RemoteBinding;
import org.jboss.annotation.ejb.RemoteBindings;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;

/**
 * Strict implementation of the {@link IDelete} service interface which will use
 * the {@link SecuritySystem} via
 * {@link ome.security.SecuritySystem#runAsAdmin(AdminAction)} to forcibly
 * delete instances.
 * 
 * @author Josh Moore, josh at glencoesoftware.com
 * @since 3.0-Beta3
 * @see IDelete
 */
@TransactionManagement(TransactionManagementType.BEAN)
@Transactional
@Stateless
@Remote(IDelete.class)
@RemoteBindings( {
        @RemoteBinding(jndiBinding = "omero/remote/ome.api.IDelete"),
        @RemoteBinding(jndiBinding = "omero/secure/ome.api.IDelete", clientBindUrl = "sslsocket://0.0.0.0:3843") })
@Local(IDelete.class)
@LocalBinding(jndiBinding = "omero/local/ome.api.IDelete")
@Interceptors( { OmeroAroundInvoke.class, SimpleLifecycle.class })
public class DeleteBean extends AbstractLevel2Service implements IDelete {

    public final static Log log = LogFactory.getLog(DeleteBean.class);

    /**
     * Loads an {@link Image} graph including: Pixels, Channel, LogicalChannel,
     * StatsInfo, PlaneInfo, Thumbnails, file maps, OriginalFiles,
     * and Settings
     */
    public final static String IMAGE_QUERY = "select i from Image as i "
            + "left outer join fetch i.pixels as p "
            + "left outer join fetch p.channels as c "
            + "left outer join fetch c.logicalChannel as lc "
            + "left outer join fetch c.statsInfo as sinfo "
            + "left outer join fetch p.planeInfo as pinfo "
            + "left outer join fetch p.thumbnails as thumb "
            + "left outer join fetch p.pixelsFileMaps as map "
            + "left outer join fetch map.parent as ofile "
            + "left outer join fetch p.settings as setting "
            + "where i.id = :id";

    LocalAdmin admin;

    public final Class<? extends ServiceInterface> getServiceInterface() {
        return IDelete.class;
    }

    public void setAdminService(LocalAdmin adminService) {
        getBeanHelper().throwIfAlreadySet(admin, adminService);
        this.admin = adminService;
    }

    // ~ Service Methods
    // =========================================================================

    @RolesAllowed("user")
    public List<IObject> checkImageDelete(final long id, final boolean force) {

        final QueryConstraints constraints = new QueryConstraints(admin,
                iQuery, id, force);
        sec.runAsAdmin(constraints);
        return constraints.getResults();
    }

    /**
     * This uses {@link #IMAGE_QUERY} to load all the subordinate metdata of the
     * {@link Image} which will be deleted.
     */
    @RolesAllowed("user")
    public List<IObject> previewImageDelete(long id, boolean force) {
        final UnloadedCollector delete = new UnloadedCollector(iQuery, admin,
                false);
        Image[] holder = new Image[1];
        getImageAndCount(holder, id, delete);
        return delete.list;
    }

    @RolesAllowed("user")
    public void deleteImage(final long id, final boolean force)
            throws SecurityViolation, ValidationException {

        final EventContext ec = admin.getEventContext();

        final List<IObject> constraints = checkImageDelete(id, force);
        if (constraints.size() > 0) {
            throw new ApiUsageException(
                    "Image has following constraints and cannot be deleted:"
                            + constraints
                            + "\nIt is possible to check for a "
                            + "non-empty constraints list via checkImageDelete.");
        }

        final UnloadedCollector delete = new UnloadedCollector(iQuery, admin,
                false);
        final Image[] holder = new Image[1];
        getImageAndCount(holder, id, delete);
        final Image i = holder[0];

        Details d = i.getDetails();
        long user = d.getOwner().getId();
        long group = d.getGroup().getId();

        boolean root = ec.isCurrentUserAdmin();
        List<Long> leaderof = ec.getLeaderOfGroupsList();
        boolean pi = leaderof.contains(group);
        boolean own = ec.getCurrentUserId().equals(user);

        if (!own && !root && !pi) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("User %d attempted to delete "
                        + "Image %d belonging to User %d", ec
                        .getCurrentUserId(), i.getId(), user));
            }
            throw new SecurityViolation(String.format(
                    "User %s cannot delete image %d", ec.getCurrentUserName(),
                    i.getId()));
        }

        iQuery.execute(new HibernateCallback() {

            public Object doInHibernate(Session session)
                    throws HibernateException, SQLException {
                session.clear();
                return null;
            }

        });

        for (final IObject object : delete.list) {
            try {
                sec.runAsAdmin(new AdminAction() {
                    public void runAsAdmin() {
                        iUpdate.deleteObject(object);
                    }

                });
            } catch (ValidationException ve) {
                // TODO could collect these and throw at once; on the other
                // hand once one fails, there's probably going to be
                // interrelated
                // issues
                // TODO Could use another exception here
                ValidationException div = new ValidationException(
                        "Delete failed since related object could not be deleted: "
                                + object);
                throw div;
            }
        }

    }

    @RolesAllowed("user")
    public void deleteImages(java.util.Set<Long> ids, boolean force)
            throws SecurityViolation, ValidationException, ApiUsageException {

        if (ids == null || ids.size() == 0) {
            return; // EARLY EXIT!
        }

        for (Long id : ids) {
            try {
                deleteImage(id, force);
            } catch (SecurityViolation sv) {
                throw new SecurityViolation("Error while deleting image " + id
                        + "\n" + sv.getMessage());
            } catch (ValidationException ve) {
                throw new ValidationException("Error while deleting image "
                        + id + "\n" + ve.getMessage());
            } catch (ApiUsageException aue) {
                throw new ApiUsageException("Error while deleting image " + id
                        + "\n" + aue.getMessage());
            }
        }

    };

    @RolesAllowed("user")
    public void deleteImagesByDataset(long datasetId, boolean force)
            throws SecurityViolation, ValidationException, ApiUsageException {

        List<DatasetImageLink> links = iQuery.findAllByQuery(
                "select link from DatasetImageLink link "
                        + "left outer join fetch link.parent "
                        + "left outer join fetch link.child "
                        + "where link.parent.id = :id", new Parameters()
                        .addId(datasetId));
        Set<Long> ids = new HashSet<Long>();
        for (DatasetImageLink link : links) {
            ids.add(link.child().getId());
            link.child().unlinkDataset(link.parent());
            iUpdate.deleteObject(link);
        }
        deleteImages(ids, force);
    };

    // Implementation
    // =========================================================================

    /**
     * Uses the locally defined query to load an {@link Image} and calls
     * {@link #collect(UnloadedCollector, Image)} in order to define a list of
     * what will be deleted.
     * 
     * This method fulfills the {@link #previewImageDelete(long, boolean)}
     * contract and as such is used by {@link #deleteImage(long, boolean)} in
     * order to fulfill its contract.
     */
    protected void getImageAndCount(final Image[] images, final long id,
            final UnloadedCollector delete) {
        sec.runAsAdmin(new AdminAction() {
            public void runAsAdmin() {
                images[0] = iQuery.findByQuery(IMAGE_QUERY, new Parameters()
                        .addId(id));
                if (images[0] == null) {
                    throw new ApiUsageException("Cannot find image: "+id);
                }
                collect(delete, images[0]);
            }
        });
    }

    /**
     * Walks the {@link Image} graph collecting unloaded instances of all
     * entities for later delete.
     */
    protected void collect(final UnloadedCollector delete, final Image i) {

        i.collectPixels(new CBlock<Pixels>() {

            public Pixels call(IObject object) {
                Pixels p = (Pixels) object;

                p.eachLinkedOriginalFile(delete);
                p.collectPlaneInfo(delete);

                for (RenderingDef rdef : p
                        .collectSettings((CBlock<RenderingDef>) null)) {

                    for (ChannelBinding binding : rdef
                            .unmodifiableWaveRendering()) {
                        delete.call(binding);
                    }
                    delete.call(rdef);
                    delete.call(rdef.getQuantization());
                }

                p.collectThumbnails(delete);

                // Why do we set channel to null here and not waveRendering
                // above?
                List<Channel> channels = p
                        .collectChannels((CBlock<Channel>) null);
                for (int i = 0; i < channels.size(); i++) {
                    Channel channel = channels.set(i, null);
                    delete.call(channel);
                    delete.call(channel.getStatsInfo());
                    delete.call(channel.getLogicalChannel());
                    LogicalChannel lc = channel.getLogicalChannel();
                    // delete.call(lc.getLightSource());
                    // // TODO lightsource
                    // delete.call(lc.getAuxLightSource());
                    // // TODO lightsource
                    // delete.call(lc.getOtf());
                    // delete.call(lc.getDetectorSettings());
                    // DetectorSettings ds = lc.getDetectorSettings();
                    // delete.call(ds.getDetector());
                }

                delete.call(p);

                return null;
            }

        });

        for (DatasetImageLink link : i
                .collectDatasetLinks((CBlock<DatasetImageLink>) null)) {
            i.removeDatasetImageLink(link, true);
            delete.call(link);
        }

        for (ImageAnnotationLink link : i
                .collectAnnotationLinks((CBlock<ImageAnnotationLink>) null)) {
            i.removeImageAnnotationLink(link, true);
            delete.call(link);
        }

        for (CategoryImageLink link : i
                .collectCategoryLinks((CBlock<CategoryImageLink>) null)) {
            i.removeCategoryImageLink(link, true);
            delete.call(link);
        }

        delete.call(i);

    }
}
