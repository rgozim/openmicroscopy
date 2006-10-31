package ome.api;

import ome.model.core.Pixels;
import ome.model.display.RenderingDef;

/** 
 * Provides methods for dealing with thumbnails. Provision is provided to
 * retrieve thumbnails using the on-disk cache (provided by <i>ROMIO</i>) or
 * on the fly.
 * 
 * @author  Chris Allan &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:callan@blackcat.ca">callan@blackcat.ca</a>
 * @version 3.0 
 * <small>
 * (<b>Internal version:</b> $Rev$ $Date$)
 * </small>
 * @since 3.0
 */
public interface IThumb extends ServiceInterface
{
	/**
	 * Retrieves the a thumbnail for a pixels set using a given set of rendering
	 * settings (RenderingDef). If the thumbnail exists on-disk cache it will
	 * be returned directly, otherwise it will be created directly as in {@link
	 * #getThumbDirect()}.
	 * 
	 * @param pixels the pixels set.
	 * @param sizeX the X-axis width of the thumbnail. <code>null</code>
	 * specifies the default size of 48.
	 * @param sizeY the Y-axis width of the thumbnail. <code>null</code>
	 * specifies the default size of 48.
	 * @param def the rendering settings. <code>null</code> specifies the user's
	 * currently active rendering settings to be used.
	 * @throws ApiUsageException if:
	 * <ul>
	 *   <li><i>sizeX</i> pixels.sizeX</li>
	 *   <li><i>sizeX</i> is negative</li>
	 *   <li><i>sizeY</i> > pixels.sizeY</li>
	 *   <li><i>sizeY</i> is negative</li>
	 * </ul>
	 * @return a JPEG thumbnail byte buffer.
	 * @see getThumbnailDirect()
	 */
	public byte[] getThumbnail(Pixels pixels, RenderingDef def,
	                           Integer sizeX, Integer sizeY);
	
	/**
	 * Retrieves the a thumbnail for a pixels set using a given set of rendering
	 * settings (RenderingDef). If the thumbnail exists on-disk cache it will
	 * be returned directly, otherwise it will be created directly as in {@link
	 * #getThumbDirect()}. The longest side of the image will be used to
	 * calculate the size for the smaller side in order to keep the aspect ratio
	 * of the original image.
	 * 
	 * @param pixels the pixels set.
	 * @param size the size of the longest side of the thumbnail requested.
	 * <code>null</code> specifies the default size of 48.
	 * @param def the rendering settings. <code>null</code> specifies the user's
	 * currently active rendering settings to be used.
	 * @throws ApiUsageException if:
	 * <ul>
	 *   <li><i>size</i> > pixels.sizeX and pixels.sizeY</li>
	 * </ul>
	 * @return a JPEG thumbnail byte buffer.
	 * @see getThumbnail()
	 */
	public byte[] getThumbnailByLongestSide(Pixels pixels, RenderingDef def,
	                                        Integer size);
	
	
	/**
	 * Retrieves the a thumbnail for a pixels set using a given set of rendering
	 * settings (RenderingDef). The Thumbnail will always be created directly,
	 * ignoring the on-disk cache.
	 * 
	 * @param pixels the pixels set.
	 * @param sizeX the X-axis width of the thumbnail. <code>null</code>
	 * specifies the default size of 48.
	 * @param sizeY the Y-axis width of the thumbnail. <code>null</code>
	 * specifies the default size of 48.
	 * @param def the rendering settings. <code>null</code> specifies the user's
	 * currently active rendering settings to be used.
	 * @throws ApiUsageException if:
	 * <ul>
	 *   <li><i>sizeX</i> pixels.sizeX</li>
	 *   <li><i>sizeX</i> is negative</li>
	 *   <li><i>sizeY</i> > pixels.sizeY</li>
	 *   <li><i>sizeY</i> is negative</li>
	 * </ul>
	 * @return a JPEG thumbnail byte buffer.
	 * @see getThumbnail()
	 */
	public byte[] getThumbnailDirect(Pixels pixels, RenderingDef def,
                                     Integer sizeX, Integer sizeY);
	
	/**
	 * Retrieves the a thumbnail for a pixels set using a given set of rendering
	 * settings (RenderingDef). The Thumbnail will always be created directly,
	 * ignoring the on-disk cache. The longest side of the image will be used to
	 * calculate the size for the smaller side in order to keep the aspect ratio
	 * of the original image.
	 * 
	 * @param pixels the pixels set.
	 * @param size the size of the longest side of the thumbnail requested.
	 * <code>null</code> specifies the default size of 48.
	 * @param def the rendering settings. <code>null</code> specifies the user's
	 * currently active rendering settings to be used.
	 * @throws ApiUsageException if:
	 * <ul>
	 *   <li><i>size</i> > pixels.sizeX and pixels.sizeY</li>
	 * </ul>
	 * @return a JPEG thumbnail byte buffer.
	 * @see getThumbnailDirect()
	 */
	public byte[] getThumbnailByLongestSideDirect(Pixels pixels,
	                                              RenderingDef def,
                                                  Integer size);
	
	/**
	 * Creates a thumbnail for a pixels set using a given set of rendering
	 * settings (RenderingDef) in the on-disk cache.
	 * 
	 * @param pixels the pixels set.
	 * @param sizeX the X-axis width of the thumbnail. <code>null</code>
	 * specifies the default size of 48.
	 * @param sizeY the Y-axis width of the thumbnail. <code>null</code>
	 * specifies the default size of 48.
	 * @param def the rendering settings. <code>null</code> specifies the user's
	 * currently active rendering settings to be used.
	 * @throws ApiUsageException if:
	 * <ul>
	 *   <li><i>sizeX</i> pixels.sizeX</li>
	 *   <li><i>sizeX</i> is negative</li>
	 *   <li><i>sizeY</i> > pixels.sizeY</li>
	 *   <li><i>sizeY</i> is negative</li>
	 * </ul>
	 * @see getThumb()
	 * @see getThumbDirect()
	 */
	public void createThumbnail(Pixels pixels, RenderingDef def,
                                Integer sizeX, Integer sizeY);
	
	/**
	 * Creates thumbnails for a pixels set using a given set of rendering
	 * settings (RenderingDef) in the on-disk cache for <b>every</b> sizeX/sizeY
	 * combination already cached.
	 * 
	 * @param pixels the pixels set.
	 * @param def the rendering settings. <code>null</code> specifies the user's
	 * currently active rendering settings to be used.
	 * @see getThumb()
	 * @see getThumbDirect()
	 */
	public void createThumbnails(Pixels pixels, RenderingDef def);
	
	/**
	 * Checks if a thumbnail of a particular size exists for a pixels set.
	 * 
	 * @param pixels the pixels set.
	 * @param sizeX the X-axis width of the thumbnail. <code>null</code>
	 * specifies use the default size of 48.
	 * @param sizeY the Y-axis width of the thumbnail. <code>null</code>
	 * specifies user the default size of 48.
	 * @throws ApiUsageException if:
	 * <ul>
	 *   <li><i>sizeX</i> is negative</li>
	 *   <li><i>sizeY</i> is negative</li>
	 * </ul>
	 * @see getThumb()
	 * @see getThumbDirect()
	 */
	public boolean thumbnailExists(Pixels pixels, Integer sizeX, Integer sizeY);
}
