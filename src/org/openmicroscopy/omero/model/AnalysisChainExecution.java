package org.openmicroscopy.omero.model;

import org.openmicroscopy.omero.BaseModelUtils;
import ome.util.Filterable;
import ome.util.Filter;


import java.util.*;




/**
 * AnalysisChainExecution generated by hbm2java
 */
public class
AnalysisChainExecution 
implements java.io.Serializable ,
org.openmicroscopy.omero.OMEModel,
ome.util.Filterable {

    // Fields    

     private Integer analysisChainExecutionId;
     private Date timestamp;
     private AnalysisChain analysisChain;
     private Set analysisNodeExecutions;
     private Dataset dataset;
     private Experimenter experimenter;


    // Constructors

    /** default constructor */
    public AnalysisChainExecution() {
    }
    
    /** constructor with id */
    public AnalysisChainExecution(Integer analysisChainExecutionId) {
        this.analysisChainExecutionId = analysisChainExecutionId;
    }
   
    
    

    // Property accessors

    /**
     * 
     */
    public Integer getAnalysisChainExecutionId() {
        return this.analysisChainExecutionId;
    }
    
    public void setAnalysisChainExecutionId(Integer analysisChainExecutionId) {
        this.analysisChainExecutionId = analysisChainExecutionId;
    }

    /**
     * 
     */
    public Date getTimestamp() {
        return this.timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 
     */
    public AnalysisChain getAnalysisChain() {
        return this.analysisChain;
    }
    
    public void setAnalysisChain(AnalysisChain analysisChain) {
        this.analysisChain = analysisChain;
    }

    /**
     * 
     */
    public Set getAnalysisNodeExecutions() {
        return this.analysisNodeExecutions;
    }
    
    public void setAnalysisNodeExecutions(Set analysisNodeExecutions) {
        this.analysisNodeExecutions = analysisNodeExecutions;
    }

    /**
     * 
     */
    public Dataset getDataset() {
        return this.dataset;
    }
    
    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    /**
     * 
     */
    public Experimenter getExperimenter() {
        return this.experimenter;
    }
    
    public void setExperimenter(Experimenter experimenter) {
        this.experimenter = experimenter;
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

  public boolean acceptFilter(Filter filter){


	  // Visiting: AnalysisChainExecutionId ------------------------------------------
	  Integer _AnalysisChainExecutionId = null;
	  try {
	     _AnalysisChainExecutionId = getAnalysisChainExecutionId();
	  } catch (Exception e) {
		 setAnalysisChainExecutionId(null);
	  }
// TODO catch class cast?
	  setAnalysisChainExecutionId((Integer) filter.filter("org.hibernate.mapping.RootClass(org.openmicroscopy.omero.model.AnalysisChainExecution):AnalysisChainExecutionId",_AnalysisChainExecutionId)); 

	  // Visiting: Timestamp ------------------------------------------
	  Date _Timestamp = null;
	  try {
	     _Timestamp = getTimestamp();
	  } catch (Exception e) {
		 setTimestamp(null);
	  }
// TODO catch class cast?
	  setTimestamp((Date) filter.filter("org.hibernate.mapping.RootClass(org.openmicroscopy.omero.model.AnalysisChainExecution):Timestamp",_Timestamp)); 

	  // Visiting: AnalysisChain ------------------------------------------
	  AnalysisChain _AnalysisChain = null;
	  try {
	     _AnalysisChain = getAnalysisChain();
	  } catch (Exception e) {
		 setAnalysisChain(null);
	  }
// TODO catch class cast?
	  setAnalysisChain((AnalysisChain) filter.filter("org.hibernate.mapping.RootClass(org.openmicroscopy.omero.model.AnalysisChainExecution):AnalysisChain",_AnalysisChain)); 

	  // Visiting: AnalysisNodeExecutions ------------------------------------------
	  Set _AnalysisNodeExecutions = null;
	  try {
	     _AnalysisNodeExecutions = getAnalysisNodeExecutions();
	  } catch (Exception e) {
		 setAnalysisNodeExecutions(null);
	  }
// TODO catch class cast?
	  setAnalysisNodeExecutions((Set) filter.filter("org.hibernate.mapping.RootClass(org.openmicroscopy.omero.model.AnalysisChainExecution):AnalysisNodeExecutions",_AnalysisNodeExecutions)); 

	  // Visiting: Dataset ------------------------------------------
	  Dataset _Dataset = null;
	  try {
	     _Dataset = getDataset();
	  } catch (Exception e) {
		 setDataset(null);
	  }
// TODO catch class cast?
	  setDataset((Dataset) filter.filter("org.hibernate.mapping.RootClass(org.openmicroscopy.omero.model.AnalysisChainExecution):Dataset",_Dataset)); 

	  // Visiting: Experimenter ------------------------------------------
	  Experimenter _Experimenter = null;
	  try {
	     _Experimenter = getExperimenter();
	  } catch (Exception e) {
		 setExperimenter(null);
	  }
// TODO catch class cast?
	  setExperimenter((Experimenter) filter.filter("org.hibernate.mapping.RootClass(org.openmicroscopy.omero.model.AnalysisChainExecution):Experimenter",_Experimenter)); 
   	 return true;
  }
  
  public String toString(){
	return "AnalysisChainExecution"+(analysisChainExecutionId==null ? ":Hash"+this.hashCode() : ":"+analysisChainExecutionId);
  }
  


}
