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

import ome.conditions.ResourceError;
import omero.ServerError;
import omero.api.IConfigPrx;
import omero.api.RawPixelsStorePrx;
import omero.api.ThumbnailStorePrx;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.DataObject;
import omero.gateway.model.ImageData;
import omero.gateway.model.PixelsData;
import org.openmicroscopy.shoola.env.data.OmeroImageService;
import org.openmicroscopy.shoola.env.data.model.ThumbnailData;
import org.openmicroscopy.shoola.env.data.views.BatchCall;
import org.openmicroscopy.shoola.env.data.views.BatchCallTree;
import org.openmicroscopy.shoola.util.image.geom.Factory;
import org.openmicroscopy.shoola.util.image.io.EncoderException;
import org.openmicroscopy.shoola.util.image.io.WriterImage;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;
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
     * Collection of user IDs.
     */
    private Set<Long> userIDs;

    /**
     * Helper reference to the image service.
     */
    private OmeroImageService service;

    /**
     * The security context.
     */
    private SecurityContext ctx;

    /**
     * Use getConfigService() instead of this directly
     */
    private IConfigPrx configService;

    /**
     * Load the thumbnail as an full size image.
     */
    private boolean asImage = false;


    private IConfigPrx getConfigService() throws DSOutOfServiceException {
        if (configService == null) {
            configService = context.getGateway()
                    .getConfigService(ctx);
        }
        return configService;
    }

    private void handleBatchCall(ThumbnailStorePrx store, PixelsData pxd, long userId) throws Exception {
        // If image has pyramids, check to see if image is ready for loading as a thumbnail.
        try {
            Image thumbnail;
            if (requiresPixelsPyramid(pxd)) {
                thumbnail = tryGetThumbnail(store, pxd, userId);
            } else {
                thumbnail = loadThumbnail(store, pxd, userId);
            }
            // Convert thumbnail to whatever
            currentThumbnail = new ThumbnailData(pxd.getImage().getId(),
                    thumbnail, userId, true);
        } catch (Exception e) {
            context.getLogger().error(this, e.getMessage());
        }
    }

    private PixelsData dataObjectToPixelsData(DataObject image) {
        return image instanceof ImageData ?
                ((ImageData) image).getDefaultPixels() :
                (PixelsData) image;
    }

    private Image tryGetThumbnail(ThumbnailStorePrx thumbStore, PixelsData pxd, long userId)
            throws DSOutOfServiceException, ServerError, DSAccessException, EncoderException {
        RawPixelsStorePrx rawPixelStore = context.getGateway()
                .getPixelsStore(ctx);

        try {
            // This method will throw if there is an issue with the pyramid
            // generation (i.e. it's not finished, corrupt)
            rawPixelStore.setPixelsId(pxd.getId(), false);
        } catch (omero.MissingPyramidException e) {
            // Thrown if pyramid file is missing
            // create and show a loading .symbol
            return Factory.createDefaultThumbnail("Loading");
        } catch (ResourceError e) {
            context.getLogger().error(this, "Error getting pyramid from server," +
                    " it might be corrupt");
            return Factory.createDefaultThumbnail("Error");
        }

        // If we get here, load the thumbnail
        return loadThumbnail(thumbStore, pxd, userId);
    }

    /**
     * Loads the thumbnail for {@link #images}<code>[index]</code>.
     *
     * @param pxd    The image the thumbnail for.
     * @param userId The id of the user the thumbnail is for.
     * @param store  The thumbnail store to use.
     */
    private BufferedImage loadThumbnail(ThumbnailStorePrx store, PixelsData pxd, long userId)
            throws ServerError, DSAccessException, DSOutOfServiceException, EncoderException {
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

        byte[] data = store.getThumbnail(omero.rtypes.rint(sizeX),
                omero.rtypes.rint(sizeY));
        if (data == null || data.length == 0) {
            // If the thumbnail data is null or empty, we can assume that
            // the thumbnail hasn't been generated yet and it's still in progress.
            return Factory.createDefaultThumbnail("Loading");
        }

        return WriterImage.bytesToImage(data);
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
    @Override
    protected void buildTree() {
        final int lastIndex = images.size() - 1;
        for (final long userId : userIDs) {
            int k = 0;
            for (DataObject image : images) {
                // Cast our image to pixels object
                final PixelsData pxd = dataObjectToPixelsData(image);

                // Flag to check if we've iterated to the last image
                final boolean last = lastIndex == k++;

                // Add a new load thumbnail task to tree
                add(new BatchCall("Loading thumbnails") {
                    @Override
                    public void doCall() throws Exception {
                        super.doCall();
                        ThumbnailStorePrx store = service.createThumbnailStore(ctx);
                        try {
                            handleBatchCall(store, pxd, userId);
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
    @Override
    protected Object getResult() {
        return currentThumbnail;
    }

    /**
     * Creates a new instance.
     * If bad arguments are passed, we throw a runtime exception so to fail
     * early and in the caller's thread.
     *
     * @param ctx       The security context.
     * @param images    Contains {@link DataObject}s, one for each thumbnail to  retrieve.
     * @param userIDs   The users the thumbnails are for.
     * @param maxWidth  The maximum acceptable width of the thumbnails.
     * @param maxHeight The maximum acceptable height of the thumbnails.
     */
    public ThumbnailLoader(SecurityContext ctx, Collection<DataObject> images, Set<Long> userIDs,
                           int maxWidth, int maxHeight) {
        if (images == null || images.isEmpty()) {
            throw new NullPointerException("No images.");
        }

        if (maxWidth <= 0) {
            throw new IllegalArgumentException(
                    "Non-positive width: " + maxWidth + ".");
        }

        if (maxHeight <= 0) {
            throw new IllegalArgumentException(
                    "Non-positive height: " + maxHeight + ".");
        }

        this.images = images;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.userIDs = userIDs;
        this.ctx = ctx;
        this.service = context.getImageService();
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
    public ThumbnailLoader(SecurityContext ctx, Collection<DataObject> imgs, long userID) {
        this(ctx, imgs, Collections.singleton(userID), 0, 0);
        this.asImage = true;
    }

    public ThumbnailLoader(SecurityContext ctx, Collection<DataObject> imgs, long userID,
                           int maxWidth, int maxHeight) {
        this(ctx, imgs, Collections.singleton(userID), maxWidth, maxHeight);
    }

    /**
     * Creates a new instance.
     * If bad arguments are passed, we throw a runtime exception so to fail
     * early and in the caller's thread.
     *
     * @param ctx       The security context.
     * @param image     The {@link ImageData}, the thumbnail
     * @param userIDs   The users the thumbnail are for.
     * @param maxWidth  The maximum acceptable width of the thumbnails.
     * @param maxHeight The maximum acceptable height of the thumbnails.
     */
    public ThumbnailLoader(SecurityContext ctx, ImageData image, Set<Long> userIDs, int maxWidth,
                           int maxHeight) {
        this(ctx, Collections.singleton(image), userIDs, maxWidth, maxHeight);
    }

    /**
     * Creates a new instance.
     * If bad arguments are passed, we throw a runtime exception so to fail
     * early and in the caller's thread.
     *
     * @param ctx       The security context.
     * @param image     The {@link ImageData}, the thumbnail
     * @param userId    The user the thumbnails are for.
     * @param maxWidth  The maximum acceptable width of the thumbnails.
     * @param maxHeight The maximum acceptable height of the thumbnails.
     */
    public ThumbnailLoader(SecurityContext ctx, ImageData image, long userId, int maxWidth,
                           int maxHeight) {
        this(ctx, Collections.singleton(image), Collections.singleton(userId), maxWidth, maxHeight);
    }

}
