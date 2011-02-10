/*
 * $Id$
 *
 *   Copyright 2006-2011 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package integration.schema;

//Java imports
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//Third-party libraries
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//Application-internal dependencies
import integration.AbstractTest;
import integration.XMLMockObjects;
import integration.XMLWriter;

/** 
 * Collections of tests.
 * Checks if the downgrade from current schema to 2003-FC schema works.
 *
 * @author Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since 3.0-Beta4
 */
public class SchemaCurrent_TO_2003_FC_Test 
	extends AbstractTest
{

	/** The collection of files that have to be deleted. */
	private List<File> files;
	
	/** The schema file. Decide where to put it. */
	private File STYLESHEET = new File("2010-06-TO-2003-FC.xsl");
	
	/** The schema file. Decide where to put it. */
	private File SCHEMA_2003_FC = new File("2010-06-TO-2003-FC.xsl");
	
	/** The path in front of ID. */
	private String XSLT_PATH_ID = "xslt.fix";
	
	/**
	 * Checks if the <code>Image</code> tag was correctly transformed.
	 * 
	 * @param destNode The node from the transformed file.
	 * @param srcNode The Image node from the source file
	 */
	private void checkImageNode(Node destNode, Node srcNode)
	{
		String IMAGE_ID_PATH = XSLT_PATH_ID+":Image:XSLT:";
		assertNotNull(destNode);
		assertNotNull(srcNode);
		NamedNodeMap attributesSrc = srcNode.getAttributes();
		String nameSrc = "";
		String idSrc = "";
		Node n;
		String name;
		for (int i = 0; i < attributesSrc.getLength(); i++) {
			n = attributesSrc.item(i);
			name = n.getNodeName();
			if (name.equals(XMLWriter.ID_ATTRIBUTE))
				idSrc = n.getNodeValue();
			else if (name.equals(XMLWriter.NAME_ATTRIBUTE))
				nameSrc = n.getNodeValue();
		}
		
		NamedNodeMap attributes = destNode.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			n = attributes.item(i);
			if (n != null) {
				name = n.getNodeName();
				if (name.equals(XMLWriter.ID_ATTRIBUTE))
					assertEquals(n.getNodeValue(), IMAGE_ID_PATH+idSrc);
				else if (name.equals(XMLWriter.NAME_ATTRIBUTE))
					assertEquals(n.getNodeValue(), nameSrc);
			}
		}
		Node pixelsNode = null;
		NodeList list = srcNode.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			n = list.item(i);
			if (n != null) {
				name = n.getNodeName();
				if (name.contains(XMLWriter.PIXELS_TAG))
					pixelsNode = n;
			}
		}
		list = destNode.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			n = list.item(i);
			if (n != null) {
				name = n.getNodeName();
				if (name.contains(XMLWriter.PIXELS_TAG))
					checkPixelsNode(n, pixelsNode);
			}
		}
	}
	
	/**
	 * Checks if the <code>Pixels</code> tag was correctly transformed.
	 * 
	 * @param destNode The node from the transformed file.
	 * @param srcNode The Image node from the source file
	 */
	private void checkPixelsNode(Node destNode, Node srcNode)
	{
		String PIXELS_ID_PATH = XSLT_PATH_ID+":Pixels:XSLT:";
		NamedNodeMap attributesSrc = srcNode.getAttributes();
		String nameSrc = "";
		String idSrc = "";
		String sizeX = "";
		String sizeY = "";
		String sizeZ = "";
		String sizeC = "";
		String sizeT = "";
		String pixelsType = "";
		String dimensionOrder = "";
		Node n;
		String name;
		for (int i = 0; i < attributesSrc.getLength(); i++) {
			n = attributesSrc.item(i);
			name = n.getNodeName();
			if (name.equals(XMLWriter.ID_ATTRIBUTE))
				idSrc = n.getNodeValue();
			else if (name.equals(XMLWriter.NAME_ATTRIBUTE))
				nameSrc = n.getNodeValue();
			else if (name.equals(XMLWriter.SIZE_X_ATTRIBUTE))
				sizeX = n.getNodeValue();
			else if (name.equals(XMLWriter.SIZE_Y_ATTRIBUTE))
				sizeY = n.getNodeValue();
			else if (name.equals(XMLWriter.SIZE_Z_ATTRIBUTE))
				sizeZ = n.getNodeValue();
			else if (name.equals(XMLWriter.SIZE_C_ATTRIBUTE))
				sizeC = n.getNodeValue();
			else if (name.equals(XMLWriter.SIZE_T_ATTRIBUTE))
				sizeT = n.getNodeValue();
			else if (name.equals(XMLWriter.PIXELS_TYPE_ATTRIBUTE))
				pixelsType = n.getNodeValue();
			else if (name.equals(XMLWriter.DIMENSION_ORDER_ATTRIBUTE))
				dimensionOrder = n.getNodeValue();
		}
		String bigEndianDst = "";
		NamedNodeMap attributes = destNode.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			n = attributes.item(i);
			if (n != null) {
				name = n.getNodeName();
				if (name.equals(XMLWriter.ID_ATTRIBUTE))
					assertEquals(n.getNodeValue(), PIXELS_ID_PATH+idSrc);
				else if (name.equals(XMLWriter.NAME_ATTRIBUTE))
					assertEquals(n.getNodeValue(), nameSrc);
				else if (name.equals(XMLWriter.SIZE_X_ATTRIBUTE))
					assertEquals(n.getNodeValue(), sizeX);
				else if (name.equals(XMLWriter.SIZE_Y_ATTRIBUTE))
					assertEquals(n.getNodeValue(), sizeY);
				else if (name.equals(XMLWriter.SIZE_Z_ATTRIBUTE))
					assertEquals(n.getNodeValue(), sizeZ);
				else if (name.equals(XMLWriter.SIZE_C_ATTRIBUTE))
					assertEquals(n.getNodeValue(), sizeC);
				else if (name.equals(XMLWriter.SIZE_T_ATTRIBUTE))
					assertEquals(n.getNodeValue(), sizeT);
				else if (name.equals(XMLWriter.PIXELS_TYPE_ATTRIBUTE))
					assertEquals(n.getNodeValue(), pixelsType);
				else if (name.equals(XMLWriter.DIMENSION_ORDER_ATTRIBUTE))
					assertEquals(n.getNodeValue(), dimensionOrder);
				else if (name.equals(XMLWriter.BIG_ENDIAN_ATTRIBUTE))
					bigEndianDst = n.getNodeValue();
			}
		}
		//check the tag now.
		NodeList list = srcNode.getChildNodes();
		List<Node> binDataNodeSrc = new ArrayList<Node>();
		for (int i = 0; i < list.getLength(); i++) {
			n = list.item(i);
			if (n != null) {
				name = n.getNodeName();
				if (name.contains(XMLWriter.BIN_DATA_TAG))
					binDataNodeSrc.add(n);
			}
		}
		list = destNode.getChildNodes();
		List<Node> binDataNodeDest = new ArrayList<Node>();
		for (int i = 0; i < list.getLength(); i++) {
			n = list.item(i);
			if (n != null) {
				name = n.getNodeName();
				if (name.contains(XMLWriter.BIN_DATA_TAG))
					binDataNodeDest.add(n);
			}
		}
		assertTrue(binDataNodeSrc.size() > 0);
		assertEquals(binDataNodeSrc.size(), binDataNodeDest.size());
		for (int i = 0; i < binDataNodeDest.size(); i++) {
			checkBinDataNode(binDataNodeDest.get(i), binDataNodeSrc.get(i));
		}
		n = binDataNodeSrc.get(0);
		attributesSrc = n.getAttributes();
		//now check that 
		for (int i = 0; i < attributesSrc.getLength(); i++) {
			n = attributesSrc.item(i);
			if (n != null) {
				name = n.getNodeName();
				if (name.contains(XMLWriter.BIG_ENDIAN_ATTRIBUTE))
					assertEquals(n.getNodeValue(), bigEndianDst);
			}
		}
	}
	
	/**
	 * Checks if the <code>Image</code> tag was correctly transformed.
	 * 
	 * @param destNode The node from the transformed file.
	 * @param srcNode The Image node from the source file
	 */
	private void checkBinDataNode(Node destNode, Node srcNode)
	{
		assertNotNull(destNode);
		assertNotNull(srcNode);
		NamedNodeMap attributesSrc = srcNode.getAttributes();
		String compression = "";
		Node n;
		String name;
		for (int i = 0; i < attributesSrc.getLength(); i++) {
			n = attributesSrc.item(i);
			name = n.getNodeName();
			if (name.equals(XMLWriter.COMPRESSION_ATTRIBUTE))
				compression = n.getNodeValue();
		}
		
		NamedNodeMap attributes = destNode.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			n = attributes.item(i);
			if (n != null) {
				name = n.getNodeName();
				if (name.equals(XMLWriter.COMPRESSION_ATTRIBUTE))
					assertEquals(n.getNodeValue(), compression);
			}
		}
		assertEquals(destNode.getTextContent(), srcNode.getTextContent());
	}
	
	/**
	 * Overridden to initialize the list.
	 * @see AbstractTest#setUp()
	 */
    @Override
    @BeforeClass
    protected void setUp() 
    	throws Exception
    {
    	super.setUp();
    	files = new ArrayList<File>();
    }
    
	/**
	 * Overridden to delete the files.
	 * @see AbstractTest#tearDown()
	 */
    @Override
    @AfterClass
    public void tearDown() 
    	throws Exception
    {
    	Iterator<File> i = files.iterator();
    	while (i.hasNext()) {
			i.next().delete();
		}
    	files.clear();
    }
	
	/**
     * Tests the XSLT used to downgrade from current schema to 2003-FC.
     * An XML file with an image is created and the stylesheet is applied.
     * @throws Exception Thrown if an error occurred.
     */
    @Test(enabled = false)
	public void testDowngradeTo2003FCImageNoMetadata()
		throws Exception
	{
		File f = File.createTempFile("testDowngradeTo2003FCImageNoMetadata",
				"."+OME_XML_FORMAT);
		files.add(f);
		File output = File.createTempFile(
				"testDowngradeTo2003FCImageNoMetadataOutput",
				"."+OME_XML_FORMAT);
		files.add(output);
		XMLMockObjects xml = new  XMLMockObjects();
		XMLWriter writer = new XMLWriter();
		writer.writeFile(f, xml.createImage(), true);
		transformFile(f, output, STYLESHEET);
		Document doc = parseFile(output, SCHEMA_2003_FC);
		assertNotNull(doc);
		//Should only have one root node i.e. OME node
		NodeList list = doc.getChildNodes();
		assertEquals(list.getLength(), 1);
		Node root = list.item(0);
		assertEquals(root.getNodeName(), XMLWriter.OME_TAG);
		//now analyse the root node
		list = root.getChildNodes();
		String name;
		Node n;
		Document docSrc = parseFile(f, null);
		Node rootSrc = docSrc.getChildNodes().item(0);
		Node imageNode = null;
		NodeList listSrc = rootSrc.getChildNodes();
		for (int i = 0; i < listSrc.getLength(); i++) {
			n = listSrc.item(i);
			name = n.getNodeName();
			if (name != null) {
				if (name.contains(XMLWriter.IMAGE_TAG))
					imageNode = n;
			}
		}
		
		for (int i = 0; i < list.getLength(); i++) {
			n = list.item(i);
			name = n.getNodeName();
			if (name != null) {
				//TODO: add other node
				if (name.contains(XMLWriter.IMAGE_TAG) && imageNode != null)
					checkImageNode(n, imageNode);
			}
		}
	}
	
}
