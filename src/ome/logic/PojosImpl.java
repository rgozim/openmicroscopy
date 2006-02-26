/*
 * ome.logic.PojosImpl
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

package ome.logic;

//Java imports
import java.util.Collection;
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
import ome.annotations.NotNull;
import ome.annotations.Validate;
import ome.api.IPojos;
import ome.model.ILink;
import ome.model.IObject;
import ome.model.containers.Category;
import ome.model.containers.CategoryGroup;
import ome.model.containers.Dataset;
import ome.model.core.Image;
import ome.model.containers.Project;
import ome.model.meta.Experimenter;
import ome.services.query.CollectionCountQueryDefinition;
import ome.services.query.PojosCGCPathsQueryDefinition;
import ome.services.query.PojosFindAnnotationsQueryDefinition;
import ome.services.query.PojosFindHierarchiesQueryDefinition;
import ome.services.query.PojosGetImagesQueryDefinition;
import ome.services.query.PojosLoadHierarchyQueryDefinition;
import ome.services.query.PojosQP;
import ome.services.query.QP;
import ome.services.query.Query;
import ome.services.util.CountCollector;
import ome.tools.AnnotationTransformations;
import ome.tools.HierarchyTransformations;
import ome.tools.lsid.LsidUtils;
import ome.util.builders.PojoOptions;


/**
 * implementation of the Pojos service interface
 * 
 * @author Josh Moore, <a href="mailto:josh.moore@gmx.de">josh.moore@gmx.de</a>
 * @version 1.0
 * <small>
 * (<b>Internal version:</b> $Rev$ $Date$)
 * </small>
 * @since OMERO 2.0
 */
public class PojosImpl extends AbstractLevel2Service implements IPojos {

    private static Log log = LogFactory.getLog(PojosImpl.class);

    @Override
    protected String getName()
    {
        return IPojos.class.getName();
    }
    
    // ~ READ
    // =========================================================================
    
    public Set loadContainerHierarchy(Class rootNodeType, 
            @Validate(Long.class) Set rootNodeIds, Map options) {
        
        PojoOptions po = new PojoOptions(options);
        
        if (null==rootNodeIds && !po.isExperimenter()) 
        	throw new IllegalArgumentException(
        			"Set of ids for loadContainerHierarchy() may not be null " +
                    "if experimenter and group options are null.");

        if (! Project.class.equals(rootNodeType) 
                && ! Dataset.class.equals(rootNodeType) 
                && ! CategoryGroup.class.equals(rootNodeType) 
                && ! Category.class.equals(rootNodeType))

            throw new IllegalArgumentException(
                "Class parameter for loadContainerIHierarchy() must be in " +
                "{Project,Dataset,Category,CategoryGroup}, not "
                        + rootNodeType);

        Query q = queryFactory.lookup(
                PojosLoadHierarchyQueryDefinition.class.getName(),
                PojosQP.klass(rootNodeType),
                PojosQP.ids(rootNodeIds),
                PojosQP.options(po.map())); // TODO Move PojosQP to PojosOptions
        List l = (List) iQuery.execute(q);

        collectCounts(l, po);
    	
        return new HashSet(l);
        
	}
    
	public Set findContainerHierarchies(@NotNull Class rootNodeType, 
            @NotNull @Validate(Long.class) Set imageIds, Map options) {
		
		PojoOptions po = new PojoOptions(options);
        
        Query q = queryFactory.lookup(
                PojosFindHierarchiesQueryDefinition.class.getName(),
                PojosQP.klass(rootNodeType),
                PojosQP.ids(imageIds),
                PojosQP.options(po.map()));

        List l = (List) iQuery.execute(q);
        collectCounts(l,po);


        //
        // Destructive changes below this point.
        //
        for (Object object : l)
        {
            iQuery.evict(object);    
        }

        // TODO; this if-else statement could be removed if Transformations 
        // did their own dispatching 
        // TODO: logging, null checking. daos should never return null 
        // TODO then size!
		if (Project.class.equals(rootNodeType)) {
			if (imageIds.size()==0){
				return new HashSet();
			}
            
			return HierarchyTransformations.invertPDI(new HashSet(l)); 
			
		}

		else if (CategoryGroup.class.equals(rootNodeType)){
			if (imageIds.size()==0){
				return new HashSet();
			}
			
			return HierarchyTransformations.invertCGCI(new HashSet(l)); 
		}
		
		else {throw new IllegalArgumentException(
	                "Class parameter for findContainerHierarchies() must be" +
                    " in {Project,CategoryGroup}, not " + rootNodeType);
		}
		
	}

	public Map findAnnotations(Class rootNodeType, 
            @NotNull @Validate(Long.class) Set rootNodeIds, 
            @Validate(Long.class) Set annotatorIds, Map options) {
		
        if (rootNodeIds.size()==0)
            return new HashMap();

		PojoOptions po = new PojoOptions(options);
		
        Query q = queryFactory.lookup(
                PojosFindAnnotationsQueryDefinition.class.getName(),
                PojosQP.klass(rootNodeType),
                PojosQP.ids(rootNodeIds),
                PojosQP.Set("annotatorIds",annotatorIds),
                PojosQP.options(po.map()));

        List l = (List) iQuery.execute(q);
        // no count collection

        //
        // Destructive changes below this point.
        //
        for (Object object : l)
        {
            iQuery.evict(object);    
        }

        // TODO these here or in Query Definition? Does it belong to API or to query?
        if (Dataset.class.equals(rootNodeType)){ 
            return AnnotationTransformations.sortDatasetAnnotatiosn(new HashSet(l));
        } 
        else if (Image.class.equals(rootNodeType)){
            return AnnotationTransformations.sortImageAnnotatiosn(new HashSet(l));
        }
        else { 
            throw new IllegalArgumentException(
                    "Class parameter for findAnnotation() must be in " +
                    "{Dataset,Image}, not "+ rootNodeType);
        }
        

	}

	public Set findCGCPaths(@NotNull @Validate(Long.class) Set imgIds, 
            String algorithm, Map options) {

		if (imgIds.size()==0){
			return new HashSet();
		}

		if (! IPojos.ALGORITHMS.contains(algorithm)) {
			throw new IllegalArgumentException(
					"No such algorithm known:"+algorithm);
		}
		
		PojoOptions po = new PojoOptions(options);

        Query q = queryFactory.lookup(
                PojosCGCPathsQueryDefinition.class.getName(),
                PojosQP.ids(imgIds),
                PojosQP.String("algorithm",algorithm),
                PojosQP.options(po.map()));

        
		List<List> result_set = (List) iQuery.execute(q);

        Map<CategoryGroup,Set<Category>> map 
        = new HashMap<CategoryGroup,Set<Category>>();
        Set<CategoryGroup> returnValues = new HashSet<CategoryGroup>();
        
        // Parse
        for (List result_row : result_set)
        {
            CategoryGroup cg = (CategoryGroup) result_row.get(0);
            Category c = (Category) result_row.get(1);

            if (!map.containsKey(cg)) map.put(cg,new HashSet<Category>());
            map.get(cg).add(c);
        }

        //
        // Destructive changes below this point.
        //

        
        for (CategoryGroup cg : map.keySet())
        {
            for (Category c : map.get(cg))
            {
                iQuery.evict(cg); // FIXME does this suffice?
                cg.addCategory(c);
            }
            returnValues.add(cg);
        }

        collectCounts(returnValues,po);
        return returnValues;
		
	}

	public Set getImages(@NotNull Class rootNodeType, 
            @NotNull @Validate(Long.class) Set rootNodeIds, Map options) {
		
		if (rootNodeIds.size()==0){
			return new HashSet();
		}

		PojoOptions po = new PojoOptions(options);

        Query q = queryFactory.lookup(
                PojosGetImagesQueryDefinition.class.getName(),
                PojosQP.klass(rootNodeType),
                PojosQP.ids(rootNodeIds),
                PojosQP.options(po.map()));

        List l = (List) iQuery.execute(q);
        collectCounts(l,po);
        return new HashSet(l);
		
	}

	public Set getUserImages(Map options) {
		
		PojoOptions po = new PojoOptions(options);
		
		if (!po.isExperimenter() ) { // FIXME && !po.isGroup()){
			throw new IllegalArgumentException(
					"experimenter or group option " +
                    "is required for getUserImages().");
		}
	
        
        Query q = queryFactory.lookup(" select i from Image i " +
                "where i.details.owner.id = :id ",
                PojosQP.Long("id",po.getExperimenter()));
        List l = (List) iQuery.execute(q);
        collectCounts(l,po);
		return new HashSet(l);
		
	}
    
    public Map getUserDetails(@NotNull @Validate(String.class) Set names, 
            Map options)
    {
        
        List results;
        Map<String, Experimenter> map = new HashMap<String, Experimenter>();
        
        /* query only if we have some ids */
        if (names.size() > 0)
        {
            Map<String, Set> params = new HashMap<String, Set>();
            params.put("name_list",names);
        
            results = iQuery.queryListMap(
                    "select e from Experimenter e " +
                    "left outer join fetch e.groupExperimenterMap gs " +
                    "left outer join fetch gs.child g " +
                    "where e.omeName in ( :name_list )",
                    params
            );
            
            for (Object object : results)
            {
                Experimenter e = (Experimenter) object;
                map.put(e.getOmeName(),e);
            }
        }
        
        /* ensures all ids appear in map */
        for (Object object : names)
        {
            String name = (String) object;
            if (! map.containsKey(name)){
                map.put(name,null);
            }
        }        
        
        return map;
        
    }
    
    public Map getCollectionCount(@NotNull String type, @NotNull String property, 
            @NotNull @Validate(Long.class) Set ids, Map options)
    {
        
        String parsedProperty = LsidUtils.parseField(property);
        
        checkType(type);
        checkProperty(type,parsedProperty);
        
        String query = "select size(table."+parsedProperty+") from "+type+" table where table.id = ?";
        // FIXME: optimize by doing new list(id,size(table.property)) ... group by id
        for (Iterator iter = ids.iterator(); iter.hasNext();)
        {
            Long id = (Long) iter.next();
            Integer count = (Integer) iQuery.queryUnique(query,new Object[]{id});
            results.put(id,count);
        }
        
        return results;
    }

    public Collection retrieveCollection(IObject arg0, String arg1, Map arg2)
    {
        IObject context = (IObject) iQuery.getById(arg0.getClass(),arg0.getId());
        Collection c = (Collection) context.retrieve(arg1); // FIXME not type.o.null safe
        iQuery.initialize(c);
        return c;
    }

    // ~ WRITE
    // =========================================================================
    
    public IObject createDataObject(IObject arg0, Map arg1)
    {
       return iUpdate.saveAndReturnObject(arg0);
    }

    public IObject[] createDataObjects(IObject[] arg0, Map arg1)
    {
        return iUpdate.saveAndReturnArray(arg0); 
    }

    public void unlink(ILink[] arg0, Map arg1)
    {
        deleteDataObjects(arg0,arg1);
    }

    public ILink[] link(ILink[] arg0, Map arg1)
    {
        return (ILink[])iUpdate.saveAndReturnArray(arg0);
    }

    public IObject updateDataObject(IObject arg0, Map arg1)
    {
        return iUpdate.saveAndReturnObject(arg0);
    }

    public IObject[] udpateDataObjects(IObject[] arg0, Map arg1)
    {
        return iUpdate.saveAndReturnArray(arg0);
    }

    public void deleteDataObject(IObject row, Map arg1)
    {
        iUpdate.deleteObject(row);
    }

    public void deleteDataObjects(IObject[] rows, Map options)
    {
        for (IObject object : rows)
        {
            deleteDataObject(object,options);    
        }
        
    }

    //  ~ Helpers
    // =========================================================================
    
    /**
     * Determines collection counts for all <code>String[] fields</code> in 
     * the options.
     * 
     * TODO possibly move to CountCollector itself. It'll need an IQuery then.
     * or is it a part of the Pojo QueryDefinitions ?
     */
    private void collectCounts(Collection queryResults, PojoOptions po)
    {
        if (po.hasCountFields() && po.isCounts())
        {
            CountCollector c = new CountCollector(po.countFields());
            c.collect(queryResults);
            for (String key : po.countFields())
            {
                Query q_c = queryFactory.lookup(
                        /* TODO po.map() here */
                        CollectionCountQueryDefinition.class.getName(),
                        PojosQP.String("field",key),
                        PojosQP.ids(c.getIds(key))
                        );
                List l_c = (List) iQuery.execute(q_c);
                for (Object o : l_c)
                {
                   Object[] results = (Object[]) o;
                   Long id = (Long) results[0];
                   Integer count = (Integer) results[1];
                   c.addCounts(key,id,count);
                }
                
            }
        }
    }
    
    final static Map results = new HashMap();
    final static String alphaNumeric = "^\\w+$";
    final static String alphaNumericDotted = "^\\w[.\\w]+$"; // TODO annotations

    protected void checkType(String type)
    {
        if (!type.matches(alphaNumericDotted))
        {
            throw new IllegalArgumentException(
                    "Type argument to getCollectionCount may ONLY be " +
                    "alpha-numeric with dots ("+alphaNumericDotted+")");
        }

        if (!iQuery.checkType(type)) 
        {
            throw new IllegalArgumentException(type +" is an unknown type.");
        }
    }
    
    protected void checkProperty(String type, String property)
    {
        
        if (!property.matches(alphaNumeric))
        {
            throw new IllegalArgumentException("Property argument to " +
                    "getCollectionCount may ONLY be alpha-numeric ("+
                    alphaNumeric+")");
        }
            
    
        if (!iQuery.checkProperty(type,property))
        {
            throw new IllegalArgumentException(type+"."+property+
                    " is an unknown property on type "+type);
        }
    
    }
    
}

