package ome.model.core;

import ome.util.BaseModelUtils;


import java.util.*;




/**
 *        @hibernate.class
 *         table="pixel"
 *     
 */
public class
Pixel 
implements java.io.Serializable ,
ome.api.OMEModel
, ome.NewModel 
{

    // Fields    

     private Integer id;
     private Integer version;
     private byte[] permissions;
     private ome.model.meta.Experimenter owner;
     private ome.model.meta.Event creationEvent;
     private ome.model.meta.Event updateEvent;
     private Set roi5ds;
     private Set maps;


    // Constructors

    /** default constructor */
    public Pixel() {
    }
    
    /** constructor with id */
    public Pixel(Integer id) {
        this.id = id;
    }
   
    
    

    // Property accessors

    /**
     *      *            @hibernate.id
     *             generator-class="sequence"
     *             type="java.lang.Integer"
     *             column="id"
     *            @hibernate.generator-param
     * 	        name="sequence"
     * 	        value="pixel_seq"
     *         
     */
    public Integer getId() {
        return this.id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     *      *            @hibernate.version
     *             column="version"
     *             unsaved-value="negative"
     *         
     */
    public Integer getVersion() {
        return this.version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * 
     */
    public byte[] getPermissions() {
        return this.permissions;
    }
    
    public void setPermissions(byte[] permissions) {
        this.permissions = permissions;
    }

    /**
     * 
     */
    public ome.model.meta.Experimenter getOwner() {
        return this.owner;
    }
    
    public void setOwner(ome.model.meta.Experimenter owner) {
        this.owner = owner;
    }

    /**
     * 
     */
    public ome.model.meta.Event getCreationEvent() {
        return this.creationEvent;
    }
    
    public void setCreationEvent(ome.model.meta.Event creationEvent) {
        this.creationEvent = creationEvent;
    }

    /**
     * 
     */
    public ome.model.meta.Event getUpdateEvent() {
        return this.updateEvent;
    }
    
    public void setUpdateEvent(ome.model.meta.Event updateEvent) {
        this.updateEvent = updateEvent;
    }

    /**
     *      *            @hibernate.set
     *             lazy="true"
     *             inverse="true"
     *             cascade="all"
     *            @hibernate.collection-key
     *             column="pixel"
     *            @hibernate.collection-one-to-many
     *             class="ome.model.core.Roi5D"
     *         
     */
    public Set getRoi5ds() {
        return this.roi5ds;
    }
    
    public void setRoi5ds(Set roi5ds) {
        this.roi5ds = roi5ds;
    }

    /**
     *      *            @hibernate.set
     *             lazy="true"
     *             inverse="true"
     *             cascade="all"
     *            @hibernate.collection-key
     *             column="pixel"
     *            @hibernate.collection-one-to-many
     *             class="ome.model.core.Map"
     *         
     */
    public Set getMaps() {
        return this.maps;
    }
    
    public void setMaps(Set maps) {
        this.maps = maps;
    }





	/** utility methods. Container may re-assign this. */	
	protected static BaseModelUtils _utils = 
		new BaseModelUtils();
	public BaseModelUtils getUtils(){
		return _utils;
	}
	public void setUtils(BaseModelUtils utils){
		_utils = utils;
	}



}
