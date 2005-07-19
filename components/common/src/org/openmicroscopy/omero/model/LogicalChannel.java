package org.openmicroscopy.omero.model;

import org.openmicroscopy.omero.BaseModelUtils;


import java.util.*;




/**
 * LogicalChannel generated by hbm2java
 */
public class
LogicalChannel 
implements java.io.Serializable ,
org.openmicroscopy.omero.OMEModel {

    // Fields    

     private Integer attributeId;
     private String photometricInterpretation;
     private String mode;
     private Float auxLightAttenuation;
     private Integer exWave;
     private Float detectorOffset;
     private String auxTechnique;
     private String fluor;
     private String contrastMethod;
     private Float detectorGain;
     private String name;
     private Integer samplesPerPixel;
     private Float lightAttenuation;
     private Integer emWave;
     private Integer auxLightWavelength;
     private String illuminationType;
     private Float ndFilter;
     private Integer pinholeSize;
     private Integer lightWavelength;
     private Set channelComponents;
     private Image image;
     private ModuleExecution moduleExecution;


    // Constructors

    /** default constructor */
    public LogicalChannel() {
    }
    
    /** constructor with id */
    public LogicalChannel(Integer attributeId) {
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
    public String getPhotometricInterpretation() {
        return this.photometricInterpretation;
    }
    
    public void setPhotometricInterpretation(String photometricInterpretation) {
        this.photometricInterpretation = photometricInterpretation;
    }

    /**
     * 
     */
    public String getMode() {
        return this.mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * 
     */
    public Float getAuxLightAttenuation() {
        return this.auxLightAttenuation;
    }
    
    public void setAuxLightAttenuation(Float auxLightAttenuation) {
        this.auxLightAttenuation = auxLightAttenuation;
    }

    /**
     * 
     */
    public Integer getExWave() {
        return this.exWave;
    }
    
    public void setExWave(Integer exWave) {
        this.exWave = exWave;
    }

    /**
     * 
     */
    public Float getDetectorOffset() {
        return this.detectorOffset;
    }
    
    public void setDetectorOffset(Float detectorOffset) {
        this.detectorOffset = detectorOffset;
    }

    /**
     * 
     */
    public String getAuxTechnique() {
        return this.auxTechnique;
    }
    
    public void setAuxTechnique(String auxTechnique) {
        this.auxTechnique = auxTechnique;
    }

    /**
     * 
     */
    public String getFluor() {
        return this.fluor;
    }
    
    public void setFluor(String fluor) {
        this.fluor = fluor;
    }

    /**
     * 
     */
    public String getContrastMethod() {
        return this.contrastMethod;
    }
    
    public void setContrastMethod(String contrastMethod) {
        this.contrastMethod = contrastMethod;
    }

    /**
     * 
     */
    public Float getDetectorGain() {
        return this.detectorGain;
    }
    
    public void setDetectorGain(Float detectorGain) {
        this.detectorGain = detectorGain;
    }

    /**
     * 
     */
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     */
    public Integer getSamplesPerPixel() {
        return this.samplesPerPixel;
    }
    
    public void setSamplesPerPixel(Integer samplesPerPixel) {
        this.samplesPerPixel = samplesPerPixel;
    }

    /**
     * 
     */
    public Float getLightAttenuation() {
        return this.lightAttenuation;
    }
    
    public void setLightAttenuation(Float lightAttenuation) {
        this.lightAttenuation = lightAttenuation;
    }

    /**
     * 
     */
    public Integer getEmWave() {
        return this.emWave;
    }
    
    public void setEmWave(Integer emWave) {
        this.emWave = emWave;
    }

    /**
     * 
     */
    public Integer getAuxLightWavelength() {
        return this.auxLightWavelength;
    }
    
    public void setAuxLightWavelength(Integer auxLightWavelength) {
        this.auxLightWavelength = auxLightWavelength;
    }

    /**
     * 
     */
    public String getIlluminationType() {
        return this.illuminationType;
    }
    
    public void setIlluminationType(String illuminationType) {
        this.illuminationType = illuminationType;
    }

    /**
     * 
     */
    public Float getNdFilter() {
        return this.ndFilter;
    }
    
    public void setNdFilter(Float ndFilter) {
        this.ndFilter = ndFilter;
    }

    /**
     * 
     */
    public Integer getPinholeSize() {
        return this.pinholeSize;
    }
    
    public void setPinholeSize(Integer pinholeSize) {
        this.pinholeSize = pinholeSize;
    }

    /**
     * 
     */
    public Integer getLightWavelength() {
        return this.lightWavelength;
    }
    
    public void setLightWavelength(Integer lightWavelength) {
        this.lightWavelength = lightWavelength;
    }

    /**
     * 
     */
    public Set getChannelComponents() {
        return this.channelComponents;
    }
    
    public void setChannelComponents(Set channelComponents) {
        this.channelComponents = channelComponents;
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
