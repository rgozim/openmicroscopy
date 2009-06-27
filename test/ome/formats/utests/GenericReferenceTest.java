package ome.formats.utests;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import ome.formats.OMEROMetadataStore;
import ome.model.acquisition.Instrument;
import ome.model.core.Image;
import junit.framework.TestCase;

public class GenericReferenceTest extends TestCase
{
	private OMEROMetadataStore store;
	
	private static final int INSTRUMENT_INDEX = 0;
	
	private static final int IMAGE_INDEX = 0;
	
	@Override
	protected void setUp() throws Exception
	{
        store = new OMEROMetadataStore();
        
        Image image = new Image();
        Map<String, Integer> imageIndexes = 
            new LinkedHashMap<String, Integer>();
        imageIndexes.put("imageIndex", IMAGE_INDEX);
        String imageLSID = "Image:0";
        Instrument instrument = new Instrument();
        Map<String, Integer> instrumentIndexes = 
            new LinkedHashMap<String, Integer>();
        instrumentIndexes.put("instrumentIndex", INSTRUMENT_INDEX);
        String instrumentLSID = "Instrument:0";
        
        store.updateObject(imageLSID, image, imageIndexes);
        store.updateObject(instrumentLSID, instrument, instrumentIndexes);
	}
	
	public void testAddReference()
	{
	    Map<String, String[]> referenceCache = new HashMap<String, String[]>();
	    referenceCache.put("Image:0", new String[] { "Instrument:0"});
	    store.updateReferences(referenceCache);
	}
}
