package org.openmicroscopy.omero.model;

import org.openmicroscopy.omero.BaseModelUtils;


import java.util.*;




/**
 * ImageAnnotation generated by hbm2java
 */
public class
ImageAnnotation 
implements java.io.Serializable ,
org.openmicroscopy.omero.OMEModel {

    // Fields    

     private Integer attributeId;
     private Integer theT;
     private String content;
     private Integer theC;
     private Integer theZ;
     private Boolean valid;
     private Image image;
     private ModuleExecution moduleExecution;


    // Constructors

    /** default constructor */
    public ImageAnnotation() {
    }
    
    /** constructor with id */
    public ImageAnnotation(Integer attributeId) {
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
    public Integer getTheT() {
        return this.theT;
    }
    
    public void setTheT(Integer theT) {
        this.theT = theT;
    }

    /**
     * 
     */
    public String getContent() {
        return this.content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 
     */
    public Integer getTheC() {
        return this.theC;
    }
    
    public void setTheC(Integer theC) {
        this.theC = theC;
    }

    /**
     * 
     */
    public Integer getTheZ() {
        return this.theZ;
    }
    
    public void setTheZ(Integer theZ) {
        this.theZ = theZ;
    }

    /**
     * 
     */
    public Boolean getValid() {
        return this.valid;
    }
    
    public void setValid(Boolean valid) {
        this.valid = valid;
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
