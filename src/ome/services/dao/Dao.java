package ome.services.dao;

import java.lang.reflect.Type;
import java.util.List;

import ome.api.IQuery;
import ome.conditions.ApiUsageException;
import ome.model.IObject;

public class Dao<T extends IObject> {

    final IQuery iQuery;
    
    final Class type;
    
    public Dao(IQuery q)
    {
        this.iQuery = q;
        Type unknownType = Dao.class.getTypeParameters()[0].getBounds()[0];
        if ( unknownType instanceof Class)
        {
            type = (Class) unknownType;
        } 
        
        else 
        {
            throw new ApiUsageException(
                    "Generic type for Dao instances must be a concrete subclass of IObject");
        }
    }
    
    @SuppressWarnings("unchecked")
	T findEntity( Class klass, long id)
    { 
        return (T) iQuery.find(null,id); 
    }
    
    @SuppressWarnings("unchecked")
	List <T> findAll( Class klass ) 
    { 
        return iQuery.findAll(klass,null); 
    }	
	
}
