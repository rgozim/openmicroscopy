package ome.server.itests;

import java.util.Date;
import java.util.UUID;

import org.testng.annotations.Test;

import ome.model.containers.Project;
import ome.model.meta.Experimenter;
import ome.parameters.Parameters;


public class EventStateChangeTest extends AbstractManagedContextTest
{

    Long    id;

    Project p       = new Project();

    String  name    = "StateChange:" + new Date();

    Long    expId;

    Integer expVersion;

    String  expName;

    @Test
    public void test_just_experimenter() throws Exception
    {
    
        Experimenter e = getExperimenter( "root" );
        iUpdate.saveObject( e );
        System.out.println("XXXXXXXXXXXXXXXEventStateChangeTest.test_just_experimenter()");
        
    }
    
    @Test( groups = { "broken", "versions", "ticket:118" } )
    public void test_experimenter_shouldnt_increment_on_update()
            throws Exception
    {
    	Experimenter e = new Experimenter();
    	e.setOmeName(UUID.randomUUID().toString());
    	e.setFirstName("EventState");
    	e.setLastName("ChangeTest");
    	expId = iAdmin.createUser(e);
    	loginUser(e.getOmeName());
    	
        e = getExperimenter(e.getOmeName());
        expVersion = e.getVersion();

        p.setName(name);
        id = iUpdate.saveAndReturnObject(p).getId();
        p = (Project) iQuery.findByQuery("from Project p "
                + " join fetch p.details.owner " + " where p.id = :id ",
                new Parameters().addId(id));
        
        p.setName(p.getName() + " updated.");
        p = (Project) this.iUpdate.saveAndReturnObject(p);
        Experimenter e2 = getExperimenter(expName);
        assertTrue(expVersion.equals(e2.getVersion()));
    }

    // ~ Helpers
    // =========================================================================
    private Experimenter getExperimenter(String expName)
    {
        Experimenter e = (Experimenter) iQuery.findByString(
                Experimenter.class, "omeName", expName);
        return e;
    }

}
