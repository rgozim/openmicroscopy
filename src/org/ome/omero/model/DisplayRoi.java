package org.ome.omero.model;

import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;


/** @author Hibernate CodeGenerator */
public class DisplayRoi implements Serializable {

    /** identifier field */
    private Integer attributeId;

    /** nullable persistent field */
    private Integer y1;

    /** nullable persistent field */
    private Integer z1;

    /** nullable persistent field */
    private Integer t0;

    /** nullable persistent field */
    private Integer z0;

    /** nullable persistent field */
    private Integer y0;

    /** nullable persistent field */
    private Integer t1;

    /** nullable persistent field */
    private Integer x0;

    /** nullable persistent field */
    private Integer x1;

    /** nullable persistent field */
    private Integer displayOptions;

    /** persistent field */
    private org.ome.omero.model.Image image;

    /** persistent field */
    private org.ome.omero.model.ModuleExecution moduleExecution;

    /** full constructor */
    public DisplayRoi(Integer attributeId, Integer y1, Integer z1, Integer t0, Integer z0, Integer y0, Integer t1, Integer x0, Integer x1, Integer displayOptions, org.ome.omero.model.Image image, org.ome.omero.model.ModuleExecution moduleExecution) {
        this.attributeId = attributeId;
        this.y1 = y1;
        this.z1 = z1;
        this.t0 = t0;
        this.z0 = z0;
        this.y0 = y0;
        this.t1 = t1;
        this.x0 = x0;
        this.x1 = x1;
        this.displayOptions = displayOptions;
        this.image = image;
        this.moduleExecution = moduleExecution;
    }

    /** default constructor */
    public DisplayRoi() {
    }

    /** minimal constructor */
    public DisplayRoi(Integer attributeId, org.ome.omero.model.Image image, org.ome.omero.model.ModuleExecution moduleExecution) {
        this.attributeId = attributeId;
        this.image = image;
        this.moduleExecution = moduleExecution;
    }

    public Integer getAttributeId() {
        return this.attributeId;
    }

    public void setAttributeId(Integer attributeId) {
        this.attributeId = attributeId;
    }

    public Integer getY1() {
        return this.y1;
    }

    public void setY1(Integer y1) {
        this.y1 = y1;
    }

    public Integer getZ1() {
        return this.z1;
    }

    public void setZ1(Integer z1) {
        this.z1 = z1;
    }

    public Integer getT0() {
        return this.t0;
    }

    public void setT0(Integer t0) {
        this.t0 = t0;
    }

    public Integer getZ0() {
        return this.z0;
    }

    public void setZ0(Integer z0) {
        this.z0 = z0;
    }

    public Integer getY0() {
        return this.y0;
    }

    public void setY0(Integer y0) {
        this.y0 = y0;
    }

    public Integer getT1() {
        return this.t1;
    }

    public void setT1(Integer t1) {
        this.t1 = t1;
    }

    public Integer getX0() {
        return this.x0;
    }

    public void setX0(Integer x0) {
        this.x0 = x0;
    }

    public Integer getX1() {
        return this.x1;
    }

    public void setX1(Integer x1) {
        this.x1 = x1;
    }

    public Integer getDisplayOptions() {
        return this.displayOptions;
    }

    public void setDisplayOptions(Integer displayOptions) {
        this.displayOptions = displayOptions;
    }

    public org.ome.omero.model.Image getImage() {
        return this.image;
    }

    public void setImage(org.ome.omero.model.Image image) {
        this.image = image;
    }

    public org.ome.omero.model.ModuleExecution getModuleExecution() {
        return this.moduleExecution;
    }

    public void setModuleExecution(org.ome.omero.model.ModuleExecution moduleExecution) {
        this.moduleExecution = moduleExecution;
    }

    public String toString() {
        return new ToStringBuilder(this)
            .append("attributeId", getAttributeId())
            .toString();
    }

}
