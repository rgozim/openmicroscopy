/*
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2017 University of Dundee. All rights reserved.
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.env.data.views.calls;

import ome.conditions.MissingPyramidException;
import ome.conditions.ResourceError;
import omero.RType;
import omero.ServerError;
import omero.api.IConfigPrx;
import omero.api.IQueryPrx;
import omero.api.RawPixelsStorePrx;
import omero.api.ThumbnailStorePrx;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.exception.RenderingServiceException;
import omero.gateway.model.DataObject;
import omero.gateway.model.ImageData;
import omero.gateway.model.PixelsData;
import omero.sys.Parameters;
import omero.sys.ParametersI;
import org.openmicroscopy.shoola.env.data.OmeroImageService;
import org.openmicroscopy.shoola.env.data.model.ThumbnailData;
import org.openmicroscopy.shoola.env.data.views.BatchCall;
import org.openmicroscopy.shoola.env.data.views.BatchCallTree;
import org.openmicroscopy.shoola.util.image.geom.Factory;
import org.openmicroscopy.shoola.util.image.io.EncoderException;
import org.openmicroscopy.shoola.util.image.io.WriterImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Command to load a given set of thumbnails.
 * <p>As thumbnails are retrieved from <i>OMERO</i>, they're posted back to the
 * caller through <code>DSCallFeedbackEvent</code>s. Each thumbnail will be
 * posted in a single event; the caller can then invoke the <code>
 * getPartialResult</code> method to retrieve a <code>ThumbnailData</code>
 * object for that thumbnail. The final <code>DSCallOutcomeEvent</code> will
 * have no result.</p>
 * <p>Thumbnails are generated respecting the <code>X/Y</code> ratio of the
 * original image and so that their area doesn't exceed <code>maxWidth*
 * maxHeight</code>, which is specified to the constructor.</p>
 *
 * @author Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author <br>Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:a.falconi@dundee.ac.uk">
 * a.falconi@dundee.ac.uk</a>
 * @version 2.2
 * @since OME2.2
 */
public class ThumbnailLoader
        extends BatchCallTree {

    /**
     * The images for which we need thumbnails.
     */
    private Collection<DataObject> images;

    /**
     * The maximum acceptable width of the thumbnails.
     */
    private int maxWidth;

    /**
     * The maximum acceptable height of the thumbnails.
     */
    private int maxHeight;

    /**
     * The lastly retrieved thumbnail.
     */
    private Object currentThumbnail;

    /**
     * Flag to indicate if the class was invoked for a pixels ID.
     */
    private boolean pixelsCall;

    /**
     * The id of the pixels set this loader is for.
     */
    private long pixelsID;

    /**
     * Collection of user IDs.
     */
    private Set<Long> userIDs;

    /**
     * Helper reference to the image service.
     */
    private OmeroImageService service;

    /**
     * Load the thumbnail as an full size image.
     */
    private boolean asImage;

    /**
     * The security context.
     */
    private SecurityContext ctx;


    /**
     * Use getConfigService() instead of this directly
     */
    private IConfigPrx configService;


    private IConfigPrx getConfigService() throws DSOutOfServiceException {
        if (configService == null) {
            configService = context.getGateway()
                    .getConfigService(ctx);
        } else {
            return configService;
        }
    }

    private void getJobState(long filesetId) throws DSOutOfServiceException, ServerError {
        IQueryPrx querySrv = context.getGateway()
                .getQueryService(ctx);

        String query = "SELECT u " +
                "FROM FilesetJobLink fjl, Job u, JobOriginalFileLink jol " +
                "WHERE :id = fjl.parent.id AND fjl.child = u AND u = jol.parent " +
                "ORDER BY u.id";

        Parameters params = new ParametersI();
        params.map = new HashMap<>();
        params.map.put("id", omero.rtypes.rlong(filesetId));

        List<List<RType>> results = querySrv.projection(query, params);

        System.out.print(results.toString());
    }


    /**
     * Creates a {@link BatchCall} to retrieve rendering control.
     *
     * @return The {@link BatchCall}.
     */
    private BatchCall makeBatchCall() {
        return new BatchCall("Loading thumbnail for: " + pixelsID) {
            public void doCall() throws Exception {
                BufferedImage thumbPix = null;
                try {
                    thumbPix = service.getThumbnail(ctx, pixelsID, maxWidth,
                            maxHeight, -1);

                } catch (RenderingServiceException e) {
                    context.getLogger().error(this,
                            "Cannot retrieve thumbnail from ID: " +
                                    e.getExtendedMessage());
                }
                if (thumbPix == null)
                    thumbPix = Factory.createDefaultImageThumbnail(-1);
                currentThumbnail = thumbPix;
            }
        };
    }

    private void handleBatchCall(IConfigPrx configService, DataObject image, long userId, boolean last) throws Exception {
        PixelsData pxd = image instanceof ImageData ?
                ((ImageData) image).getDefaultPixels() :
                (PixelsData) image;

        // If image has pyramids, check to see if image is ready for loading as a thumbnail.
        if (requiresPixelsPyramid(configService, pxd)) {
            RawPixelsStorePrx rawPixelStore = context.getGateway()
                    .getPixelsStore(ctx);
            try {
                // This method will throw if there is an issue with the pyramid
                // generation (i.e. it's not finished, corrupt)
                rawPixelStore.setPixelsId(pxd.getId(), false);

                // If we get here, load the thumbnail
                loadThumbnail(pxd, userId);
            } catch (MissingPyramidException e) {
                ThumbnailStorePrx store = getThumbnailStore();

                // Thrown if pyramid file is missing
                // create and show a loading symbol
                currentThumbnail = new ThumbnailData(pxd.getImage().getId(),
                        store, userId, valid);

            } catch (ResourceError e) {
                // Thrown if pyramid file is corrupt
                context.getLogger()
                        .debug(this, e.getMessage());

                // Need to think of a code path for this
            }
        } else {
            loadThumbnail(pxd, userId);
        }

        if (last) {
            context.getDataService().closeService(ctx, store);
        }
    }

    PixelsData dataObjectToPixelsData(DataObject image) {
        return image instanceof ImageData ?
                ((ImageData) image).getDefaultPixels() :
                (PixelsData) image;
    }

    java.awt.Image tryGetThumbnail(ThumbnailStorePrx thumbStore, PixelsData pxd, long userId)
            throws ResourceError, DSOutOfServiceException, ServerError {
        try {
            RawPixelsStorePrx rawPixelStore = context.getGateway()
                    .getPixelsStore(ctx);

            // This method will throw if there is an issue with the pyramid
            // generation (i.e. it's not finished, corrupt)
            rawPixelStore.setPixelsId(pxd.getId(), false);

            // If we get here, load the thumbnail
            return loadThumbnail(thumbStore, pxd, userId);
        } catch (MissingPyramidException e) {
            // Thrown if pyramid file is missing
            // create and show a loading symbol
            return Toolkit.getDefaultToolkit()
                    .getImage("ajax-loader.gif");
        }
    }

    /**
     * Loads the thumbnail for {@link #images}<code>[index]</code>.
     *
     * @param pxd    The image the thumbnail for.
     * @param userId The id of the user the thumbnail is for.
     * @param store  The thumbnail store to use.
     */
    private BufferedImage loadThumbnail(ThumbnailStorePrx store, PixelsData pxd, long userId)
            throws ServerError, EncoderException, DSAccessException, DSOutOfServiceException {
        int sizeX = maxWidth, sizeY = maxHeight;
        if (asImage) {
            sizeX = pxd.getSizeX();
            sizeY = pxd.getSizeY();
        } else {
            Dimension d = Factory.computeThumbnailSize(sizeX, sizeY,
                    pxd.getSizeX(), pxd.getSizeY());
            sizeX = d.width;
            sizeY = d.height;
        }

        if (!store.setPixelsId(pxd.getId())) {
            store.resetDefaults();
            store.setPixelsId(pxd.getId());
        }
        if (userId >= 0) {
            long rndDefId = service.getRenderingDef(ctx,
                    pxd.getId(), userId);
            // the user might not have own rendering settings
            // for this image
            if (rndDefId >= 0)
                store.setRenderingDefId(rndDefId);
        }

        return WriterImage.bytesToImage(
                store.getThumbnail(omero.rtypes.rint(sizeX),
                        omero.rtypes.rint(sizeY)));


//        if (thumbPix == null) {
//            valid = false;
//            thumbPix = Factory.createDefaultImageThumbnail(sizeX, sizeY);
//        }
//        currentThumbnail = new ThumbnailData(pxd.getImage().getId(),
//                thumbPix, userID, valid);
    }

    /**
     * Returns whether a pyramid should be used for the given {@link PixelsData}.
     * This usually implies that this is a "Big image" and therefore will
     * need tiling.
     *
     * @param pxd
     * @return
     */
    private boolean requiresPixelsPyramid(PixelsData pxd) throws DSOutOfServiceException, ServerError {
        int maxWidth = Integer.parseInt(getConfigService()
                .getConfigValue("omero.pixeldata.max_plane_width"));
        int maxHeight = Integer.parseInt(getConfigService()
                .getConfigValue("omero.pixeldata.max_plane_height"));
        return pxd.getSizeX() * pxd.getSizeY() > maxWidth * maxHeight;
    }

    /**
     * Adds a {@link BatchCall} to the tree for each thumbnail to retrieve.
     *
     * @see BatchCallTree#buildTree()
     */
    protected void buildTree() {
        if (pixelsCall) {
            add(makeBatchCall());
            return;
        }

        try {
            final int lastIndex = images.size() - 1;
            for (final long userId : userIDs) {
                int k = 0;
                for (DataObject image : images) {
                    // Cast our image to pixels object
                    final PixelsData pxd = dataObjectToPixelsData(image);

                    // Flag to check if we've iterated to the last image
                    final boolean last = lastIndex == k++;

                    // Create a new thumbnail store for each image
                    ThumbnailStorePrx store = service.createThumbnailStore(ctx);

                    // Add a new load thumbnail task to tree
                    add(new BatchCall("Loading thumbnails") {
                        @Override
                        public void doCall() throws Exception {
                            super.doCall();
                            try {
                                // If image has pyramids, check to see if image is ready for loading as a thumbnail.
                                if (requiresPixelsPyramid(pxd)) {
                                    tryGetThumbnail(store, pxd, userId);
                                } else {
                                    loadThumbnail(store, pxd);
                                }
                            } finally {
                                if (last) {
                                    context.getDataService()
                                            .closeService(ctx, store);
                                }
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            context.getLogger().debug(this,
                    e.getMessage());
        }
    }

    /**
     * Returns the lastly retrieved thumbnail.
     * This will be packed by the framework into a feedback event and
     * sent to the provided call observer, if any.
     *
     * @return A {@link ThumbnailData} containing the thumbnail pixels.
     */
    protected Object getPartialResult() {
        return currentThumbnail;
    }

    /**
     * Returns the last loaded thumbnail (important for the BirdsEyeLoader to
     * work correctly). But in fact, thumbnails are progressively delivered with
     * feedback events.
     *
     * @see BatchCallTree#getResult()
     */
    protected Object getResult() {
        return currentThumbnail;
    }

    /**
     * Creates a new instance.
     * If bad arguments are passed, we throw a runtime exception so to fail
     * early and in the caller's thread.
     *
     * @param ctx       The security context.
     * @param imgs      Contains {@link DataObject}s, one
     *                  for each thumbnail to retrieve.
     * @param maxWidth  The maximum acceptable width of the thumbnails.
     * @param maxHeight The maximum acceptable height of the thumbnails.
     * @param userIDs   The users the thumbnail are for.
     */
    public ThumbnailLoader(SecurityContext ctx, Set<DataObject> imgs,
                           int maxWidth, int maxHeight, Set<Long> userIDs) {
        if (imgs == null) throw new NullPointerException("No images.");
        if (maxWidth <= 0)
            throw new IllegalArgumentException(
                    "Non-positive width: " + maxWidth + ".");
        if (maxHeight <= 0)
            throw new IllegalArgumentException(
                    "Non-positive height: " + maxHeight + ".");
        this.ctx = ctx;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        images = imgs;
        this.userIDs = userIDs;
        asImage = false;
        service = context.getImageService();
    }

    /**
     * Creates a new instance.
     * If bad arguments are passed, we throw a runtime exception so to fail
     * early and in the caller's thread.
     *
     * @param ctx    The security context.
     * @param imgs   Contains {@link DataObject}s, one for each thumbnail to
     *               retrieve.
     * @param userID The user the thumbnail are for.
     */
    public ThumbnailLoader(SecurityContext ctx, Collection<DataObject> imgs,
                           long userID) {
        if (imgs == null) throw new NullPointerException("No images.");
        this.ctx = ctx;
        asImage = true;
        images = imgs;
        userIDs = new HashSet<Long>(1);
        userIDs.add(userID);
        service = context.getImageService();
    }

    /**
     * Creates a new instance.
     * If bad arguments are passed, we throw a runtime exception so to fail
     * early and in the caller's thread.
     *
     * @param ctx       The security context.
     * @param imgs      Contains {@link DataObject}s, one for each thumbnail to
     *                  retrieve.
     * @param maxWidth  The maximum acceptable width of the thumbnails.
     * @param maxHeight The maximum acceptable height of the thumbnails.
     * @param userID    The user the thumbnail are for.
     */
    public ThumbnailLoader(SecurityContext ctx, Collection<DataObject> imgs,
                           int maxWidth, int maxHeight, long userID) {
        if (imgs == null) throw new NullPointerException("No images.");
        if (maxWidth <= 0)
            throw new IllegalArgumentException(
                    "Non-positive width: " + maxWidth + ".");
        if (maxHeight <= 0)
            throw new IllegalArgumentException(
                    "Non-positive height: " + maxHeight + ".");
        this.ctx = ctx;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        images = imgs;
        userIDs = new HashSet<Long>(1);
        userIDs.add(userID);
        asImage = false;
        service = context.getImageService();
    }

    /**
     * Creates a new instance.
     * If bad arguments are passed, we throw a runtime exception so to fail
     * early and in the caller's thread.
     *
     * @param ctx       The security context.
     * @param image     The {@link ImageData}, the thumbnail
     * @param maxWidth  The maximum acceptable width of the thumbnails.
     * @param maxHeight The maximum acceptable height of the thumbnails.
     * @param userID    The user the thumbnails are for.
     */
    public ThumbnailLoader(SecurityContext ctx, ImageData image, int maxWidth,
                           int maxHeight, long userID) {
        if (image == null) throw new IllegalArgumentException("No image.");
        if (maxWidth <= 0)
            throw new IllegalArgumentException(
                    "Non-positive width: " + maxWidth + ".");
        if (maxHeight <= 0)
            throw new IllegalArgumentException(
                    "Non-positive height: " + maxHeight + ".");
        this.ctx = ctx;
        userIDs = new HashSet<Long>(1);
        userIDs.add(userID);
        images = new HashSet<DataObject>(1);
        images.add(image);
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        asImage = false;
        service = context.getImageService();
    }

    /**
     * Creates a new instance.
     * If bad arguments are passed, we throw a runtime exception so to fail
     * early and in the caller's thread.
     *
     * @param ctx       The security context.
     * @param pixelsID  The id of the pixel set.
     * @param maxWidth  The m aximum acceptable width of the thumbnails.
     * @param maxHeight The maximum acceptable height of the thumbnails.
     * @param userID    The user the thumbnail are for.
     */
    public ThumbnailLoader(SecurityContext ctx, long pixelsID, int maxWidth,
                           int maxHeight, long userID) {
        if (maxWidth <= 0)
            throw new IllegalArgumentException(
                    "Non-positive id: " + pixelsID + ".");
        if (maxWidth <= 0)
            throw new IllegalArgumentException(
                    "Non-positive width: " + maxWidth + ".");
        if (maxHeight <= 0)
            throw new IllegalArgumentException(
                    "Non-positive height: " + maxHeight + ".");
        this.ctx = ctx;
        pixelsCall = true;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.pixelsID = pixelsID;
        userIDs = new HashSet<Long>(1);
        userIDs.add(userID);
        service = context.getImageService();
    }

    /**
     * Creates a new instance.
     * If bad arguments are passed, we throw a runtime exception so to fail
     * early and in the caller's thread.
     *
     * @param ctx       The security context.
     * @param image     The {@link ImageData}, the thumbnail
     * @param maxWidth  The maximum acceptable width of the thumbnails.
     * @param maxHeight The maximum acceptable height of the thumbnails.
     * @param userIDs   The users the thumbnail are for.
     */
    public ThumbnailLoader(SecurityContext ctx, ImageData image, int maxWidth,
                           int maxHeight, Set<Long> userIDs) {
        if (image == null) throw new IllegalArgumentException("No image.");
        if (maxWidth <= 0)
            throw new IllegalArgumentException(
                    "Non-positive width: " + maxWidth + ".");
        if (maxHeight <= 0)
            throw new IllegalArgumentException(
                    "Non-positive height: " + maxHeight + ".");
        this.ctx = ctx;
        images = new HashSet<DataObject>(1);
        images.add(image);
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.userIDs = userIDs;
        asImage = false;
        service = context.getImageService();
    }

}
