/*
 * org.openmicroscopy.omero.logic.HierarchyBrowsingImpl
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2005 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *------------------------------------------------------------------------------
 */

/*------------------------------------------------------------------------------
 *
 * Written by:    Josh Moore <josh.moore@gmx.de>
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.omero.logic;

//Java imports
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

//Third-party libraries
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//Application-internal dependencies

import org.openmicroscopy.omero.OMEModel;
import org.openmicroscopy.omero.interfaces.HierarchyBrowsing;
import org.openmicroscopy.omero.model.Category;
import org.openmicroscopy.omero.model.CategoryGroup;
import org.openmicroscopy.omero.model.Classification;
import org.openmicroscopy.omero.model.Dataset;
import org.openmicroscopy.omero.model.DatasetAnnotation;
import org.openmicroscopy.omero.model.Image;
import org.openmicroscopy.omero.model.ImageAnnotation;
import org.openmicroscopy.omero.model.Project;


/**
 * implementation of the HierarchyBrowsing service. A single service
 * object is configured through IoC (most likely by Spring) and is
 * available for all calls.
 * 
 * @author Josh Moore, <a href="mailto:josh.moore@gmx.de">josh.moore@gmx.de</a>
 * @version 1.0
 * <small>
 * (<b>Internal version:</b> $Rev$ $Date$)
 * </small>
 * @since OMERO 1.0
 * @DEV.TODO add queries.hbm.xml to pre-processed (cached) files.
 */
public class HierarchyBrowsingImpl implements HierarchyBrowsing {

    private static Log log = LogFactory.getLog(HierarchyBrowsingImpl.class);

    AnnotationDao annotationDao;

    ContainerDao containerDao;

    public void setAnnotationDao(AnnotationDao dao) {
        this.annotationDao = dao;
    }

    public void setContainerDao(ContainerDao dao) {
        this.containerDao = dao;
    }

    /**
     * @see org.openmicroscopy.omero.interfaces.HierarchyBrowsing#loadPDIHierarchy(java.lang.Class, int)
     */
    public OMEModel loadPDIHierarchy(final Class arg0, final int arg1) {
    	return loadPDI(arg0,arg1,-1,false);
    }
    
    /**
     * @see org.openmicroscopy.omero.interfaces.HierarchyBrowsing#loadPDIAnnotatedHierarchy(java.lang.Class, int, int)
     */
    public OMEModel loadPDIAnnotatedHierarchy(Class arg0, int arg1, int arg2) {
        return loadPDI(arg0,arg1,arg2,true);
    }
    	
    protected OMEModel loadPDI(Class arg0, int arg1, int arg2, boolean arg3){

    	// CONTRACT
        if (!Project.class.equals(arg0) && !Dataset.class.equals(arg0)) {
            throw new IllegalArgumentException(
                    "Class parameter for loadPDIHierarchy() must be Project or Dataset, not "
                            + arg0);
        }

        return containerDao.loadHierarchy(arg0, arg1, arg2, false);

    }
    

    
    /**
     * @see org.openmicroscopy.omero.interfaces.HierarchyBrowsing#loadCGCIHierarchy(java.lang.Class,int)
     */
    public OMEModel loadCGCIHierarchy(final Class arg0, final int arg1) {
       	return loadCGCI(arg0,arg1,-1,false);
    }
    
    /**
     * @see org.openmicroscopy.omero.interfaces.HierarchyBrowsing#loadPDIAnnotatedHierarchy(java.lang.Class, int, int)
     */
    public OMEModel loadCGCIAnnotatedHierarchy(Class arg0, int arg1, int arg2) {
        return loadCGCI(arg0,arg1,arg2,true);
    }
    	
    protected OMEModel loadCGCI(Class arg0, int arg1, int arg2, boolean arg3){
 
        // CONTRACT
        if (!CategoryGroup.class.equals(arg0) && !Category.class.equals(arg0)) {
            throw new IllegalArgumentException(
                    "Class parameter for loadCGCIHierarchy() must be CategoryGroup or Category, not "
                            + arg0);
        }

        return containerDao.loadHierarchy(arg0, arg1, arg2, arg3);

    }

    /**
     * @see org.openmicroscopy.omero.interfaces.HierarchyBrowsing#findPDIHierarchies(java.util.Set)
     */
    public Set findPDIHierarchies(final Set arg0) {
    	return findPDI(arg0,-1,false);
    }

    /**
     * @see org.openmicroscopy.omero.interfaces.HierarchyBrowsing#findPDIHierarchies(java.util.Set)
     */
    public Set findPDIAnnotatedHierarchies(final Set arg0, final int experimenterId) {
    	return findPDI(arg0,experimenterId,true);
    }
    
    protected Set findPDI(Set arg0, int experimenterId, boolean annotated) {
        // CONTRACT
        if (null == arg0 || arg0.size() == 0) {
            return new HashSet();
        }

        List result = containerDao.findPDIHierarchies(arg0, experimenterId, annotated);
        Set imagesAll = new HashSet(result);

        if (null == imagesAll || imagesAll.size() == 0) {
            if (log.isDebugEnabled()) {
                log.debug("findPDIHierarchies() -- no results found:\n"
                        + arg0.toString());
            }
            return new HashSet();
        }

        // LOGIC
        Set hierarchies = new HashSet();
        Iterator i = imagesAll.iterator();
        while (i.hasNext()) {
            Image img = (Image) i.next();
            Set datasets = img.getDatasets();

            if (datasets == null || datasets.size() < 1) {
                hierarchies.add(img);
            } else {
                Iterator d = datasets.iterator();
                while (d.hasNext()) {
                    Dataset ds = (Dataset) d.next();

                    if (!(ds.getImages() instanceof HashSet))
                        ds.setImages(new HashSet());
                    ds.getImages().add(img);

                    Set projects = ds.getProjects();
                    if (projects == null || projects.size() < 1) {
                        hierarchies.add(ds);
                    } else {
                        Iterator p = projects.iterator();
                        while (p.hasNext()) {
                            Project prj = (Project) p.next();

                            if (!(prj.getDatasets() instanceof HashSet))
                                prj.setDatasets(new HashSet());
                            prj.getDatasets().add(ds);

                            hierarchies.add(prj);
                        }
                    }

                }
            }
        }

        return hierarchies;

    }

    /** 
     * @see org.openmicroscopy.omero.interfaces.HierarchyBrowsing#findCGCIHierarchies(java.util.Set)
     */
    public Set findCGCIHierarchies(Set arg0) {
    	return findCGCI(arg0,-1,false);
    }

    /** 
     * @see org.openmicroscopy.omero.interfaces.HierarchyBrowsing#findCGCIAnnotatedHierarchies(java.util.Set)
     */
    public Set findCGCIAnnotatedHierarchies(Set arg0, int experimenterId) {
    	return findCGCI(arg0,experimenterId,true);
    }

    /** 
     * @see org.openmicroscopy.omero.interfaces.HierarchyBrowsing#findCGCIExcludedHierarchies(java.util.Set)
     */
    public Set findCGCIExcludedHierarchies(Set arg0) {
    	//experimenter
    	//Anntoations?
    	// make findCGCIHierchies(...,boolean invert)
    	// or
    	// first get inverse of all iamges from imageDao
    	throw new RuntimeException("not implemented");
    }
    
    protected Set findCGCI(Set arg0, int experimenterId, boolean annotated) {

    	// CONTRACT
        if (null == arg0 || arg0.size() == 0) {
            return new HashSet();
        }

        List result = containerDao.findCGCIHierarchies(arg0,experimenterId,annotated);
        Set imagesAll = new HashSet(result);

        if (null == imagesAll || imagesAll.size() == 0) {
            if (log.isDebugEnabled()) {
                log.debug("findCGCIHierarchies() -- no results found:\n"
                        + arg0.toString());
            }
            return new HashSet();
        }

        // LOGIC
        Set hierarchies = new HashSet();
        Iterator i = imagesAll.iterator();
        while (i.hasNext()) {
            Image img = (Image) i.next();
            Set classifications = img.getClassifications();

            if (classifications == null || classifications.size() < 1) {
                hierarchies.add(img);
            } else {
                Iterator c = classifications.iterator();
                while (c.hasNext()) {
                    Classification cla = (Classification) c.next();

                    cla.setImage(img);

                    Category ca = cla.getCategory();
                    if (null == ca) {
                        hierarchies.add(cla);
                    } else {
                        if (!(ca.getClassifications() instanceof HashSet))
                            ca.setClassifications(new HashSet());
                        ca.getClassifications().add(cla);
                        
                        CategoryGroup cg = ca.getCategoryGroup();
                        if (cg == null) {
                            hierarchies.add(ca);
                        } else {
                            if (!(cg.getCategories() instanceof HashSet))
                                cg.setCategories(new HashSet());
                            cg.getCategories().add(ca);
                            hierarchies.add(cg);
                        }
                    }
                }
            }
        }

        return hierarchies;
    }

    /** 
     * @see org.openmicroscopy.omero.interfaces.HierarchyBrowsing#findImageAnnotations(java.util.Set)
     */
    public Map findImageAnnotations(Set arg0) {

        // CONTRACT
        if (null == arg0 || arg0.size() == 0) {
            return new HashMap();
        }

        List result = annotationDao.findImageAnnotations(arg0);

        return sortImageAnnotations(result);

    }

    /** 
     * @see org.openmicroscopy.omero.interfaces.HierarchyBrowsing#findImageAnnotations(java.util.Set,int)
     */
    public Map findImageAnnotationsForExperimenter(final Set arg0,
            int arg1) {

        // CONTRACT
        if (null == arg0 || arg0.size() == 0) {
            return new HashMap();
        }

        List result = annotationDao.findImageAnnotationsForExperimenter(arg0,
                arg1);
        return sortImageAnnotations(result);

    }

    Map sortImageAnnotations(List l) {

        Set result = new HashSet(l);

        if (null == result || result.size() == 0) {
            return new HashMap();
        }

        Map map = new HashMap();

        // SORT
        Iterator i = result.iterator();
        while (i.hasNext()) {
            ImageAnnotation ann = (ImageAnnotation) i.next();
            Integer img_id = ann.getImage().getImageId();
            if (!map.containsKey(img_id)) {
                map.put(img_id, new HashSet());
            }
            ((Set) map.get(img_id)).add(ann);
        }

        return map;
    }

    /** 
     * @see org.openmicroscopy.omero.interfaces.HierarchyBrowsing#findDatasetAnnotations(java.util.Set)
     */
    public Map findDatasetAnnotations(Set arg0) {

        // CONTRACT
        if (null == arg0 || arg0.size() == 0) {
            return new HashMap();
        }

        List result = annotationDao.findDataListAnnotations(arg0);
        return sortDatasetAnnotations(result);

    }

    /** 
     * @see org.openmicroscopy.omero.interfaces.HierarchyBrowsing#findDatasetAnnotations(java.util.Set, int)
     */
    public Map findDatasetAnnotationsForExperimenter(Set arg0,
            int arg1) {

        // CONTRACT
        if (null == arg0 || arg0.size() == 0) {
            return new HashMap();
        }

        List result = annotationDao.findDataListAnnotationForExperimenter(arg0,
                arg1);
        return sortDatasetAnnotations(result);

    }

    Map sortDatasetAnnotations(List l) {

        Set result = new HashSet(l);

        if (null == result || result.size() == 0) {
            return new HashMap();
        }

        Map map = new HashMap();

        // SORT
        Iterator i = result.iterator();
        while (i.hasNext()) {
            DatasetAnnotation ann = (DatasetAnnotation) i.next();
            Integer ds_id = ann.getDataset().getDatasetId();
            if (!map.containsKey(ds_id)) {
                map.put(ds_id, new HashSet());
            }
            ((Set) map.get(ds_id)).add(ann);
        }

        return map;
    }

}