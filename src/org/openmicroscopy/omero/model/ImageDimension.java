package org.openmicroscopy.omero.model;

import org.openmicroscopy.omero.BaseModelUtils;


import java.util.*;




/**
 * ImageDimension generated by hbm2java
 */
public class
ImageDimension 
implements java.io.Serializable ,
org.openmicroscopy.omero.OMEModel {

    // Fields    

     private Integer attributeId;
     private Float pixelSizeC;
     private Float pixelSizeT;
     private Float pixelSizeX;
     private Float pixelSizeY;
     private Float pixelSizeZ;
     private Image image;
     private ModuleExecution moduleExecution;


    // Constructors

    /** default constructor */
    public ImageDimension() {
    }
    
    /** constructor with id */
    public ImageDimension(Integer attributeId) {
        this.attributeId = attributeId;
    }
   
    
    

    // Property accessors

    /**
     * 
     */
    public Integer getAttributeId() {
        return this.attributeId;
    }
    
    public void setAttributeId(Integer attributeId) {
        this.attributeId = attributeId;
    }

    /**
     * 
     */
    public Float getPixelSizeC() {
        return this.pixelSizeC;
    }
    
    public void setPixelSizeC(Float pixelSizeC) {
        this.pixelSizeC = pixelSizeC;
    }

    /**
     * 
     */
    public Float getPixelSizeT() {
        return this.pixelSizeT;
    }
    
    public void setPixelSizeT(Float pixelSizeT) {
        this.pixelSizeT = pixelSizeT;
    }

    /**
     * 
     */
    public Float getPixelSizeX() {
        return this.pixelSizeX;
    }
    
    public void setPixelSizeX(Float pixelSizeX) {
        this.pixelSizeX = pixelSizeX;
    }

    /**
     * 
     */
    public Float getPixelSizeY() {
        return this.pixelSizeY;
    }
    
    public void setPixelSizeY(Float pixelSizeY) {
        this.pixelSizeY = pixelSizeY;
    }

    /**
     * 
     */
    public Float getPixelSizeZ() {
        return this.pixelSizeZ;
    }
    
    public void setPixelSizeZ(Float pixelSizeZ) {
        this.pixelSizeZ = pixelSizeZ;
    }

    /**
     * 
     */
    public Image getImage() {
        return this.image;
    }
    
    public void setImage(Image image) {
        this.image = image;
    }

    /**
     * 
     */
    public ModuleExecution getModuleExecution() {
        return this.moduleExecution;
    }
    
    public void setModuleExecution(ModuleExecution moduleExecution) {
        this.moduleExecution = moduleExecution;
    }





	/** utility methods. Container may re-assign this. */	
	protected static org.openmicroscopy.omero.BaseModelUtils _utils = 
		new org.openmicroscopy.omero.BaseModelUtils();
	public BaseModelUtils getUtils(){
		return _utils;
	}
	public void setUtils(BaseModelUtils utils){
		_utils = utils;
	}



}
