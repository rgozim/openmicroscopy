/*
 * omeis.providers.re.RGBStrategy
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2004 Open Microscopy Environment
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

package omeis.providers.re;


//Java imports
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;//j.m
import java.util.concurrent.Executors;//j.m
import java.util.concurrent.Future;//j.m

//Third-party libraries

//Application-internal dependencies
//j.mimport ome.util.concur.tasks.CmdProcessor;
//j.mimport ome.util.concur.tasks.Future;

import ome.io.nio.PixelBuffer;
import ome.model.core.Pixels;
import ome.model.display.ChannelBinding;
import ome.model.display.Color;
//j.mimport omeis.env.Env;
import omeis.providers.re.codomain.CodomainChain;
import omeis.providers.re.data.PlaneFactory;
import omeis.providers.re.data.Plane2D;
import omeis.providers.re.data.PlaneDef;
import omeis.providers.re.quantum.QuantizationException;

/** 
 * Transforms a plane within a given pixels set into an <i>RGB</i> image.
 * Up to three wavelengths (channels) can contribute to the final image and
 * each wavelength is mapped to one color out of red, green, or blue.  All
 * this things are specified by the rendering context.
 * <p>When multiple wavelengths have to be combined into the final image (this
 * is the case if the rendering context specifies more than one active channel),
 * this strategy renders each wavelength in a separate thread &#151; this often
 * results in parallel rendering on multi-processor machines.</p>
 * <p>Thread-safety relies on the fact that the rendering context is not going
 * to change during the whole image rendering process.  (This is enforced by
 * the {@link RenderingEngineImpl}; in fact, while the <code>render</code> 
 * method executes, the whole component is locked.)</p>
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author  <br>Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:a.falconi@dundee.ac.uk">
 * 					a.falconi@dundee.ac.uk</a>
 * @version 2.2 
 * <small>
 * (<b>Internal version:</b> $Revision: 1.14 $ $Date: 2005/06/22 17:09:48 $)
 * </small>
 * @since OME2.2
 */
class RGBStrategy
	extends RenderingStrategy
{
    
    /** 
     * Number of pixels on the <i>X1</i>-axis.
     * This is the <i>X</i>-axis in the case of an <i>XY</i> or <i>XZ</i> plane.
     * Otherwise it is the <i>Z</i>-axis &#151; <i>ZY</i> plane.
     */
    private int         sizeX1;
    
    /** 
     * Number of pixels on the X2-axis.
     * This is the <i>Y</i>-axis in the case of an <i>XY</i> or <i>ZY</i> plane.
     * Otherwise it is the <i>Z</i>-axis &#151; <i>XZ</i> plane. 
     */
    private int         sizeX2;
    
    /** The rendering context. */
    private Renderer    renderer;
    
    
    /** 
     * Initialize the <code>sizeX1</code> and <code>sizeX2</code> fields
     * according to the specified {@link PlaneDef#getSlice() slice}.
     * 
     * @param pd Reference to the plane definition defined for the strategy.
     * @param pixels Dimensions of the pixels set.
     */
    private void initAxesSize(PlaneDef pd, Pixels pixels)
    {
        try {
                switch (pd.getSlice()) {
                    case PlaneDef.XY:
                        sizeX1 = pixels.getSizeX().intValue(); // TODO int?
                        sizeX2 = pixels.getSizeY().intValue();
                        break;
                    case PlaneDef.XZ:
                        sizeX1 = pixels.getSizeX().intValue();
                        sizeX2 = pixels.getSizeZ().intValue();
                        break;
                    case PlaneDef.ZY:
                        sizeX1 = pixels.getSizeZ().intValue();
                        sizeX2 = pixels.getSizeY().intValue();
                }   
        } catch(NumberFormatException nfe) {   
            throw new RuntimeException("Invalid slice ID: "+pd.getSlice()+".", 
                                        nfe);
        } 
    }
        
    /** 
     * Extracts a color band from the <code>dataBuf</code> depending on
     * the <code>rgba</code> settings. 
     * 
     * @param dataBuf   Buffer to hold the output image's data.
     * @param color     The color settings of a given wavelength.      
     * @return Returns the byte array corresponding to the color band selected
     *         in <code>color</code>.
     */
    private byte[] getColorBand(RGBBuffer dataBuf, Color color)
    {
        byte[] band = dataBuf.getRedBand();
        if (color.getGreen().intValue() == 255)
            band = dataBuf.getGreenBand();
        else if (color.getBlue().intValue() == 255)
            band = dataBuf.getBlueBand();
        //Else it must have been red.
        return band;
    }
    
    /**
     * Creates a rendering task for each active wavelength.
     * 
     * @param planeDef          The plane to render.
     * @param renderedDataBuf   The buffer into which the rendered data will go.
     * @return An array containing the tasks.
     */
    private RenderRGBWaveTask[] makeRndTasks(PlaneDef planeDef,
                                             RGBBuffer renderedDataBuf) 
    {
        ArrayList tasks = new ArrayList(3);
        
        //Get all objects we need to create the tasks.
        Plane2D wData;
        QuantumManager qManager = renderer.getQuantumManager();
        ChannelBinding[] cBindings = 
                                renderer.getChannelBindings();
        CodomainChain cc = renderer.getCodomainChain();
        PixelBuffer pixels = renderer.getPixels();
        Pixels metadata = renderer.getMetadata();
        RenderingStats performanceStats = renderer.getStats();
        
        //Create a task for each active wavelength.
        for (int w = 0; w < cBindings.length; w++) {
            if (tasks.size() == 3) break;  //We only render 3 w at most.
            if (cBindings[w].getActive().booleanValue()) {
                //Get the raw data.
                performanceStats.startIO(w);
                wData = PlaneFactory.createPlane(planeDef, w, metadata, pixels);
                performanceStats.endIO(w);
                
                //Create a rendering task for this wavelength.
                tasks.add(new RenderRGBWaveTask(
                         getColorBand(renderedDataBuf, cBindings[w].getColor()), 
                         wData, qManager.getStrategyFor(w), cc, 
                         cBindings[w].getColor().getAlpha().intValue(),
                         sizeX1, sizeX2));
            }
        }
        
        //Turn the list into an array an return it.
        RenderRGBWaveTask[] t = new RenderRGBWaveTask[tasks.size()];
        return (RenderRGBWaveTask[]) tasks.toArray(t);
    }
    
    /**
     * Implemented as specified by superclass.
     * @see RenderingStrategy#render(Renderer ctx, PlaneDef planeDef)
     */
    RGBBuffer render(Renderer ctx, PlaneDef planeDef)
		throws IOException, QuantizationException
	{
		//Set the rendering context for the current invocation.
		renderer = ctx;
        RenderingStats performanceStats = renderer.getStats();
				
		//Initialize sizeX1 and sizeX2 according to the plane definition and
		//create the RGB buffer.
		initAxesSize(planeDef, renderer.getMetadata());
        performanceStats.startMalloc();
        RGBBuffer renderedDataBuf = new RGBBuffer(sizeX1, sizeX2);
        performanceStats.endMalloc();
        
        //Process each active wavelength.  If their number N > 1, then 
        //process N-1 async and one in the current thread.  If N = 1, 
        //just use the current thread.
        RenderRGBWaveTask[] tasks = makeRndTasks(planeDef, renderedDataBuf);
        performanceStats.startRendering();
        int n = tasks.length;
        Future[] rndTskFutures = new Future[n];  //[0] unused.
        ExecutorService processor = Executors.newCachedThreadPool();//FIXME fast enough?
        while (0 < --n)
            rndTskFutures[n] = processor.submit(tasks[n]);
        if (n == 0)
            tasks[0].call(); 
        
        //Wait for all forked tasks (if any) to complete.
        processor.shutdown();
        for (n = 1; n < rndTskFutures.length; ++n) {
            try {
                rndTskFutures[n].get();
            } catch (Exception e) {
                if (e instanceof QuantizationException)
                    throw (QuantizationException) e;
                throw (RuntimeException) e;  
                //B/c call() only throws QuantizationException, it must be RE.
            }
        }
        performanceStats.endRendering();
		
		//Done.
		return renderedDataBuf;
	}
    
    /**
     * Implemented as specified by the superclass.
     * @see RenderingStrategy#getImageSize(PlaneDef, Pixels)
     */
    int getImageSize(PlaneDef pd, Pixels pixels)
    {
        initAxesSize(pd, pixels);
        return sizeX1*sizeX2*3;
    }
    
    /**
     * Implemented as specified by the superclass.
     * @see RenderingStrategy#getPlaneDimsAsString(PlaneDef, Pixels)
     */
    String getPlaneDimsAsString(PlaneDef pd, Pixels pixels)
    {
        initAxesSize(pd, pixels);
        return sizeX1+"x"+sizeX2;
    }
	
}
