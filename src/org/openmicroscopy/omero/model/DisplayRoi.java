package org.openmicroscopy.omero.model;

import org.openmicroscopy.omero.BaseModelUtils;


import java.util.*;




/**
 * DisplayRoi generated by hbm2java
 */
public class
DisplayRoi 
implements java.io.Serializable ,
org.openmicroscopy.omero.OMEModel {

    // Fields    

     private Integer attributeId;
     private Integer y1;
     private Integer z1;
     private Integer t0;
     private Integer z0;
     private Integer y0;
     private Integer t1;
     private Integer x0;
     private Integer x1;
     private Image image;
     private ModuleExecution moduleExecution;
     private DisplayOption displayOption;


    // Constructors

    /** default constructor */
    public DisplayRoi() {
    }
    
    /** constructor with id */
    public DisplayRoi(Integer attributeId) {
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
    public Integer getY1() {
        return this.y1;
    }
    
    public void setY1(Integer y1) {
        this.y1 = y1;
    }

    /**
     * 
     */
    public Integer getZ1() {
        return this.z1;
    }
    
    public void setZ1(Integer z1) {
        this.z1 = z1;
    }

    /**
     * 
     */
    public Integer getT0() {
        return this.t0;
    }
    
    public void setT0(Integer t0) {
        this.t0 = t0;
    }

    /**
     * 
     */
    public Integer getZ0() {
        return this.z0;
    }
    
    public void setZ0(Integer z0) {
        this.z0 = z0;
    }

    /**
     * 
     */
    public Integer getY0() {
        return this.y0;
    }
    
    public void setY0(Integer y0) {
        this.y0 = y0;
    }

    /**
     * 
     */
    public Integer getT1() {
        return this.t1;
    }
    
    public void setT1(Integer t1) {
        this.t1 = t1;
    }

    /**
     * 
     */
    public Integer getX0() {
        return this.x0;
    }
    
    public void setX0(Integer x0) {
        this.x0 = x0;
    }

    /**
     * 
     */
    public Integer getX1() {
        return this.x1;
    }
    
    public void setX1(Integer x1) {
        this.x1 = x1;
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

    /**
     * 
     */
    public DisplayOption getDisplayOption() {
        return this.displayOption;
    }
    
    public void setDisplayOption(DisplayOption displayOption) {
        this.displayOption = displayOption;
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
