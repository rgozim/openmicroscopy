package ome.services.query;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ome.model.annotations.DatasetAnnotation;
import ome.model.annotations.ImageAnnotation;
import ome.model.containers.Dataset;
import ome.model.core.Image;
import ome.parameters.Parameters;
import static ome.parameters.Parameters.*;
import ome.util.builders.PojoOptions;

public class PojosFindAnnotationsQueryDefinition extends Query
{

    static Definitions defs = new Definitions(
        new IdsQueryParameterDef(),
        new OptionsQueryParameterDef(),
        new ClassQueryParameterDef(),
        new QueryParameterDef("annotatorIds",Collection.class,true));
        
    public PojosFindAnnotationsQueryDefinition(Parameters parameters)
    {
        super( defs, parameters );
        // TODO set local fields here.
    }

    public final static Map<Class,Class> typeToAnnotationType 
    = new HashMap<Class,Class>();

    public final static Map<Class,String> annotationTypeToPath
    = new HashMap<Class,String>();

    static {
        typeToAnnotationType.put(Image.class,ImageAnnotation.class);
        annotationTypeToPath.put(ImageAnnotation.class,"image");
        typeToAnnotationType.put(Dataset.class,DatasetAnnotation.class);
        annotationTypeToPath.put(DatasetAnnotation.class,"dataset");
        //  TODO this should come from meta-analysis as with hierarchy
    }
    
    @Override
    protected void buildQuery(Session session) 
        throws HibernateException, SQLException
    {
        PojoOptions po = new PojoOptions((Map) value(OPTIONS));

        Class k = (Class) value(CLASS);
        if ( ! typeToAnnotationType.containsKey( k ))
        {
            throw new IllegalArgumentException(
                    "Class "+k+" is not accepted by "+
                    PojosFindAnnotationsQueryDefinition.class.getName()
                    );
        }
        
        Class target = typeToAnnotationType.get((Class) value(CLASS));
        String path = annotationTypeToPath.get(target);
        
        //TODO refactor into CriteriaUtils
        Criteria ann = session.createCriteria(target);
        ann.createAlias("details.owner", "ann_owner");
        ann.createAlias("details.creationEvent", "ann_create");
        ann.createAlias("details.updateEvent", "ann_update");
        ann.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        ann.add(Restrictions.in(path+".id",(Collection) value(IDS)));
        
        Criteria obj = ann.createCriteria(path,LEFT_JOIN);
        obj.createAlias("details.owner", "obj_owner");
        obj.createAlias("details.creationEvent", "obj_create");
        obj.createAlias("details.updateEvent", "obj_update");
        
        if (check("annotatorIds"))
        {
            Collection annotatorIds = (Collection) value("annotatorIds");
            if (annotatorIds != null && annotatorIds.size() > 0)
                ann.add(Restrictions.in("details.id", annotatorIds ));
        }
        setCriteria( ann );
    }


}