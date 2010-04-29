package ome.formats.utests;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.annotations.Test;

import ome.conditions.ApiUsageException;
import ome.formats.OMEROMetadataStore;
import ome.model.acquisition.Detector;
import ome.model.acquisition.DetectorSettings;
import ome.model.acquisition.Dichroic;
import ome.model.acquisition.Filter;
import ome.model.acquisition.FilterSet;
import ome.model.acquisition.Instrument;
import ome.model.acquisition.Laser;
import ome.model.acquisition.LightPath;
import ome.model.acquisition.LightSettings;
import ome.model.core.Channel;
import ome.model.core.Image;
import ome.model.core.LogicalChannel;
import ome.model.core.Pixels;
import ome.model.screen.Plate;
import ome.model.screen.Reagent;
import ome.model.screen.Screen;
import ome.model.screen.Well;
import junit.framework.TestCase;

public class GenericReferenceTest extends TestCase
{
	private OMEROMetadataStore store;
	
	private static final int IMAGE_INDEX = 0;
	
	private static final int PIXELS_INDEX = 0;
	
	private static final int LOGICAL_CHANNEL_INDEX = 0;
	
	private static final int INSTRUMENT_INDEX = 0;
	
	private static final int LASER_INDEX = 0;
	
	private static final int FILTER_INDEX = 0;
	
	private static final int FILTER_SET_INDEX = 0;
	
	private static final int DETECTOR_INDEX = 0;
	
	private static final int DICHROIC_INDEX = 0;
	
	private static final int SCREEN_INDEX = 0;
	
	private static final int PLATE_INDEX = 0;
	
	private static final int WELL_INDEX = 0;
	
	private static final int REAGENT_INDEX = 0;
	
	private Image image;
	
	private Pixels pixels;
	
	private Channel channel;
	
	private LogicalChannel logicalChannel;
	
	private Instrument instrument;
	
	private Laser laser;
	
	private Filter filter;
	
	private FilterSet filterSet;
	
	private Detector detector;
	
	private Dichroic dichroic;
	
	private LightSettings lightSettings;
	
	private DetectorSettings detectorSettings;
	
	private Screen screen;
	
	private Plate plate;
	
	private Well well;
	
	private Reagent reagent;
	
	@Override
	protected void setUp() throws Exception
	{
        store = new OMEROMetadataStore();
        
		// Update Image
        image = new Image();
        Map<String, Integer> imageIndexes = 
            new LinkedHashMap<String, Integer>();
        imageIndexes.put("imageIndex", IMAGE_INDEX);
        String imageLSID = "Image:0";
        store.updateObject(imageLSID, image, imageIndexes);
        
		// Update Pixels
        pixels = new Pixels();
        Map<String, Integer> pixelsIndexes = 
            new LinkedHashMap<String, Integer>();
        pixelsIndexes.put("imageIndex", IMAGE_INDEX);
        pixelsIndexes.put("pixelsIndex", PIXELS_INDEX);
        String pixelsLSID = "Pixels:0:0";
        store.updateObject(pixelsLSID, pixels, pixelsIndexes);
        
		// Update Channel
        channel = new Channel();
        Map<String, Integer> channelIndexes = 
            new LinkedHashMap<String, Integer>();
        channelIndexes.put("imageIndex", IMAGE_INDEX);
        channelIndexes.put("logicalChannelIndex", LOGICAL_CHANNEL_INDEX);
        String channelLSID = "Channel:0:0";
        store.updateObject(channelLSID, channel, channelIndexes);
        
		// Update LogicalChannel
        logicalChannel = new LogicalChannel();
        Map<String, Integer> logicalChannelIndexes = 
            new LinkedHashMap<String, Integer>();
        logicalChannelIndexes.put("imageIndex", IMAGE_INDEX);
        logicalChannelIndexes.put("logicalChannelIndex", LOGICAL_CHANNEL_INDEX);
        String logicalChannelLSID = "LogicalChannel:0:0";
        store.updateObject(logicalChannelLSID, logicalChannel,
        		           logicalChannelIndexes);
        
        // Update Instrument
        instrument = new Instrument();
        Map<String, Integer> instrumentIndexes = 
            new LinkedHashMap<String, Integer>();
        instrumentIndexes.put("instrumentIndex", INSTRUMENT_INDEX);
        String instrumentLSID = "Instrument:0";
        store.updateObject(instrumentLSID, instrument, instrumentIndexes);
        
        // Update Laser
        laser = new Laser();
        Map<String, Integer> laserIndexes = 
            new LinkedHashMap<String, Integer>();
        laserIndexes.put("instrumentIndex", INSTRUMENT_INDEX);
        laserIndexes.put("lightSourceIndex", LASER_INDEX);
        String laserLSID = "Laser:0:0";
        store.updateObject(laserLSID, laser, laserIndexes);
        
        // Update Filter
        filter = new Filter();
        Map<String, Integer> filterIndexes = 
            new LinkedHashMap<String, Integer>();
        filterIndexes.put("instrumentIndex", INSTRUMENT_INDEX);
        filterIndexes.put("filterIndex", FILTER_INDEX);
        String filterLSID = "Filter:0:0";
        store.updateObject(filterLSID, filter, filterIndexes);
        
        // Update FilterSet
        filterSet = new FilterSet();
        Map<String, Integer> filterSetIndexes = 
            new LinkedHashMap<String, Integer>();
        filterSetIndexes.put("instrumentIndex", INSTRUMENT_INDEX);
        filterSetIndexes.put("filterSetIndex", FILTER_SET_INDEX);
        String filterSetLSID = "FilterSet:0:0";
        store.updateObject(filterSetLSID, filterSet, filterSetIndexes);

        // Update Detector
        detector = new Detector();
        Map<String, Integer> detectorIndexes = 
            new LinkedHashMap<String, Integer>();
        detectorIndexes.put("instrumentIndex", INSTRUMENT_INDEX);
        detectorIndexes.put("detectorIndex", DETECTOR_INDEX);
        String detectorLSID = "Detector:0:0";
        store.updateObject(detectorLSID, detector, detectorIndexes);
        
        // Update Dichroic
        dichroic = new Dichroic();
        Map<String, Integer> dichroicIndexes = 
            new LinkedHashMap<String, Integer>();
        dichroicIndexes.put("instrumentIndex", INSTRUMENT_INDEX);
        dichroicIndexes.put("dichroicIndex", DICHROIC_INDEX);
        String dichroicLSID = "Dichroic:0:0";
        store.updateObject(dichroicLSID, dichroic, dichroicIndexes);
        
        // Update LightSettings
        lightSettings = new LightSettings();
        Map<String, Integer> lightSettingsIndexes = 
            new LinkedHashMap<String, Integer>();
        lightSettingsIndexes.put("imageIndex", IMAGE_INDEX);
        lightSettingsIndexes.put("logicalChannelIndex", LOGICAL_CHANNEL_INDEX);
        String lightSettingsLSID = "LightSourceSettings:0:0";
        store.updateObject(lightSettingsLSID, lightSettings,
        		           lightSettingsIndexes);
        
        // Update DetectorSettings
        detectorSettings = new DetectorSettings();
        Map<String, Integer> detectorSettingsIndexes = 
            new LinkedHashMap<String, Integer>();
        detectorSettingsIndexes.put("imageIndex", IMAGE_INDEX);
        detectorSettingsIndexes.put("logicalChannelIndex",
        		                    LOGICAL_CHANNEL_INDEX);
        String detectorSettingsLSID = "DetectorSettings:0:0";
        store.updateObject(detectorSettingsLSID, detectorSettings,
        		           detectorSettingsIndexes);
        
		// Update Screen
		screen = new Screen();
        Map<String, Integer> screenIndexes = 
            new LinkedHashMap<String, Integer>();
        screenIndexes.put("screenIndex", SCREEN_INDEX);
        store.updateObject("Screen:0", screen, screenIndexes);
        
		// Update Plate
        plate = new Plate();
        Map<String, Integer> plateIndexes = 
            new LinkedHashMap<String, Integer>();
        plateIndexes.put("screenIndex", SCREEN_INDEX);
        plateIndexes.put("plateIndex", PLATE_INDEX);
        store.updateObject("Plate:0:0", plate, plateIndexes);
        
		// Update Well
        well = new Well();
        Map<String, Integer> wellIndexes = 
            new LinkedHashMap<String, Integer>();
        wellIndexes.put("screenIndex", SCREEN_INDEX);
        wellIndexes.put("plateIndex", PLATE_INDEX);
        wellIndexes.put("wellIndex", WELL_INDEX);
        store.updateObject("Well:0:0:0", well, wellIndexes);
        
		// Update Reagent
        reagent = new Reagent();
        Map<String, Integer> reagentIndexes = 
            new LinkedHashMap<String, Integer>();
        reagentIndexes.put("screenIndex", SCREEN_INDEX);
        reagentIndexes.put("reagentIndex", REAGENT_INDEX);
        store.updateObject("Reagent:0:0", reagent, reagentIndexes);
	}
	
	public void testImageInstrumentReference()
	{
	    Map<String, String[]> referenceCache = new HashMap<String, String[]>();
	    referenceCache.put("Image:0", new String[] { "Instrument:0" });
	    store.updateReferences(referenceCache);
	    assertEquals(image.getInstrument(), instrument);
	}
	
	public void testWellReagentReference()
	{
	    Map<String, String[]> referenceCache = new HashMap<String, String[]>();
	    referenceCache.put("Well:0:0:0", new String[] { "Reagent:0:0" });
	    store.updateReferences(referenceCache);
	    assertNotNull(well.linkedReagentList());
	    assertEquals(1, well.linkedReagentList().size());
	    assertEquals(well.linkedReagentList().get(0), reagent);
	}
	
	public void testLaserLaserReference()
	{
	    Map<String, String[]> referenceCache = new HashMap<String, String[]>();
	    referenceCache.put("Laser:0:0", new String[] { "Laser:0:0" });
	    store.updateReferences(referenceCache);
	    assertEquals(laser.getPump(), laser);
	}
	
	public void testLightSettingsLightSourceReference()
	{
	    Map<String, String[]> referenceCache = new HashMap<String, String[]>();
	    referenceCache.put("LightSourceSettings:0:0", 
	    		           new String[] { "Laser:0:0" });
	    store.updateReferences(referenceCache);
	    assertEquals(lightSettings.getLightSource(), laser);
	}
	
	public void testDetectorSettingsDetectorReference()
	{
	    Map<String, String[]> referenceCache = new HashMap<String, String[]>();
	    referenceCache.put("DetectorSettings:0:0", 
	    		           new String[] { "Detector:0:0" });
	    store.updateReferences(referenceCache);
	    assertEquals(detectorSettings.getDetector(), detector);
	}
	
	public void testLogicalChannelFilterSetReference()
	{
	    Map<String, String[]> referenceCache = new HashMap<String, String[]>();
	    referenceCache.put("LogicalChannel:0:0", 
	    		           new String[] { "FilterSet:0:0" });
	    store.updateReferences(referenceCache);
	    assertEquals(logicalChannel.getFilterSet(), filterSet);
	}
	
	public void testLogicalChannelFilterReference()
	{
		try
		{
			Map<String, String[]> referenceCache =
				new HashMap<String, String[]>();
			referenceCache.put("LogicalChannel:0:0", 
					new String[] { "Filter:0:0" });
			store.updateReferences(referenceCache);
			fail("Did not throw ApiUsageException.");
		}
		catch (ApiUsageException e)
		{
			return;
		}
	}
	
	public void testLogicalChannelSecondaryEmissionFilterReference()
	{
		Map<String, String[]> referenceCache =
			new HashMap<String, String[]>();
		referenceCache.put("LogicalChannel:0:0", 
				new String[] { "Filter:0:0:OMERO_EMISSION_FILTER" });
		store.updateReferences(referenceCache);
		LightPath lightPath = logicalChannel.getLightPath();
		assertEquals(0, lightPath.sizeOfExcitationFilterLink());
		assertEquals(1, lightPath.sizeOfEmissionFilterLink());
		assertEquals(lightPath.linkedEmissionFilterIterator().next(), filter);
	}
	
	public void testLogicalChannelSecondaryExcitationFilterReference()
	{
		Map<String, String[]> referenceCache =
			new HashMap<String, String[]>();
		referenceCache.put("LogicalChannel:0:0", 
				new String[] { "Filter:0:0:OMERO_EXCITATION_FILTER" });
		store.updateReferences(referenceCache);
        LightPath lightPath = logicalChannel.getLightPath();
        assertEquals(1, lightPath.sizeOfExcitationFilterLink());
        assertEquals(0, lightPath.sizeOfEmissionFilterLink());
        assertEquals(lightPath.linkedExcitationFilterIterator().next(), filter);
	}
	
	public void testFilterSetDichroicReference()
	{
	    Map<String, String[]> referenceCache = new HashMap<String, String[]>();
	    referenceCache.put("FilterSet:0:0", 
	    		           new String[] { "Dichroic:0:0" });
	    store.updateReferences(referenceCache);
	    assertEquals(filterSet.getDichroic(), dichroic);
	}
}
