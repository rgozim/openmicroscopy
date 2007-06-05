/*
 * ome.logic.ConfigImpl
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

/*------------------------------------------------------------------------------
 *
 * Written by:    Josh Moore <josh.moore@gmx.de>
 *
 *------------------------------------------------------------------------------
 */

package ome.logic;

// Java imports
import java.util.Date;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.Interceptors;

// Third-party libraries
import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.annotation.ejb.RemoteBinding;
import org.jboss.annotation.security.SecurityDomain;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

// Application-internal dependencies
import ome.annotations.RevisionDate;
import ome.annotations.RevisionNumber;
import ome.api.IConfig;
import ome.api.ServiceInterface;
import ome.services.util.OmeroAroundInvoke;
import ome.system.Version;

/**
 * implementation of the IConfig service interface.
 * 
 * Also used as the main developer example for developing (stateless) ome.logic
 * implementations. See source code documentation for more.
 * 
 * @author Josh Moore, josh.moore at gmx.de
 * @version $Revision$, $Date$
 * @since 3.0-M3
 * @see IConfig
 */

/*
 * Developer notes: --------------- The two annotations below are activated by
 * setting the subversion properties on this class file. They can be accessed
 * via ome.system.Version
 */
@RevisionDate("$Date$")
@RevisionNumber("$Revision$")
/*
 * Developer notes: --------------- The annotations below (and on the individual
 * methods) are central to the definition of this service. They are used in
 * place of XML configuration files (though the XML deployment descriptors, as
 * they are called, can be used to override the annotations), and will influence
 */
// ~ Service annotations
// =============================================================================
/*
 * Source: EJB3 Specification Purpose: Prevents the Container from managing
 * transactions (CMT), and instead delegates commits and rollbacks to user code.
 * This is, however, managed by Spring (@Transactional below)
 * 
 * @see https://trac.openmicroscopy.org.uk/omero/ticket/427
 */
@TransactionManagement(TransactionManagementType.BEAN)
/*
 * Source: Spring Purpose: Used by EventHandler#checkReadyOnly(MethodInvocation)
 * to deteremine if a method is read-only. No annotation implies ready-only, so
 * it is essential to have this annotation on all write methods.
 * 
 * Only non-container annotation and is a superset of EJB3's
 * @TransactionAttribute. Currently, suffices for our TX needs, but we may
 * eventually have to provide both annotations or to switch to the EJB3 spec
 * annotations and write a new TX-interceptor for Spring.
 * 
 * @see http://www.interface21.com/pitchfork Project Pitchfork.
 */
@Transactional
/*
 * Source: EJB3 Specification Purpose: Marks this service as stateless, which
 * means that instances can be created as needed and given to any client.
 * Concurrent calls are permitted though they may be routed to different
 * servers. The stateful counterpart is simply @Stateful, but it imposes several
 * restrictions on the class, e.g. that all fields must be transient or
 * serializable. On the other hand, all fields for @Stateless instances must be
 * thread-safe, with no state being obviously thread-safetest.
 * 
 * @see https://trac.openmicroscopy.org.uk/omero/ticket/173
 */
@Stateless
/*
 * Source: EJB3 Specification Purpose: Defines which interface will be
 * represented to remote clients of this service.
 */
@Remote(IConfig.class)
/*
 * Source: JBoss-speciifc Purpose: Defines a non-standard name for looking up
 * this service. During the early days of the EJB3 spec, the default value kept
 * changing and so it was easier to define our own. This is also somewhat
 * memorable in comparison to "[earFile]/[ejbName]/remote", however, this
 * doesn't allow Omero to be deployed multiple times in a single server.
 */
@RemoteBinding(jndiBinding = "omero/remote/ome.api.IConfig")
/*
 * Source: EJB3 Specification Purpose: Defines which interface will be
 * represented to remote clients of this service. There need be no relationship
 * between this interface and the @Remote interface. Currently unused, since
 * services don't look up dependencies from JNDI but rather have them injected
 * by Spring.
 */
@Local(IConfig.class)
/*
 * Source: JBoss-specific Purpose: See @RemoteBinding.
 */
@LocalBinding(jndiBinding = "omero/local/ome.api.IConfig")
/*
 * Source: JBoss-specific Purpose: Defines which security manager service is
 * responsible for calls to this service. This value is defined in:
 * 
 * components/app/resourcs/jboss-login.xml
 * 
 * and specifies where the manager should be found in JNDI.
 */
@SecurityDomain("OmeroSecurity")
/*
 * Source: EJB3 Specification Purpose: List of classes (with no-arg
 * constructors) which should serve as interceptors for all calls to this class.
 * Available interceptors are:
 * 
 * @AroundInvoke (interceptor - around every method call) @PostConstruct
 * (callback - after initialization) @PreDestroy (callback - before dropping
 * this instance)
 * 
 * For @Stateful services there are also: @PostActivate (callback - after the
 * instance is deserialized) @PrePassivate (callback - before the instance is
 * serialized)
 * 
 * The SimpleLifecycle does the minimum (calls create() after init and destroy()
 * before destruction) and saves a good deal of extra typing. This can also be
 * achieved by inheritance (i.e. AbstractBean.create() could be marked
 * @PostConstruct); however, only one class in an inheritance hierarchy can be
 * marked with callbacks, and this is overly restrictive for us.
 * 
 * OmeroAroundInvoke applies the OMERO security model to every method
 * invocation as well as applying the list of compile-time determined 
 * HardWiredInterceptors. All of this functionality should be trangential
 * to the functioning of the services *server-side*.
 */
@Interceptors( { OmeroAroundInvoke.class, SimpleLifecycle.class })
/*
 * Stateful differences: -------------------- @Cache(NoPassivationCache.class) --
 * JBoss-specific Purpose: can be used to turn off passivation
 */
public class ConfigImpl extends AbstractLevel2Service implements IConfig {

    /*
     * Stateful differences: -------------------- A stateful service must be
     * marked as Serializable and all fields must be either marked transient, be
     * serializable themselves, or be set to null before serialization. Here
     * we've marked the jdbc field as transient out of habit.
     * 
     * @see https://trac.openmicroscopy.org.uk/omero/ticket/173
     */
    private transient SimpleJdbcTemplate jdbc;

    /**
     * {@link SimpleJdbcTemplate} setter for dependency injection.
     * 
     * @param jdbcTemplate
     * @see ome.services.util.BeanHelper#throwIfAlreadySet(Object, Object)
     */
    /*
     * Developer notes: --------------- Because of the complicated lifecycle of
     * EJBs it is not possible to fully configure them with constructor
     * injection (which is safer). Instead, we have to provide public setters
     * for all properties which need to be injected. And since Java doesn't have
     * the concept of "friends" (yet), this opens up our classes for some weird
     * manipulations. Therefore we've made all bean setters "final" and added a
     * call to "throwIfAlreadySet" which will only allow previously null fields
     * to be set.
     */
    public final void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        getBeanHelper().throwIfAlreadySet(jdbc, jdbcTemplate);
        this.jdbc = jdbcTemplate;
    }

    /*
     * Developer notes: --------------- This method provides the lookup value
     * needed for finding services within the Spring context and, by convention,
     * the value which is to be returned can be found in the file
     * "ome/services/service-<class name>.xml"
     */
    public final Class<? extends ServiceInterface> getServiceInterface() {
        return IConfig.class;
    }

    // ~ Service methods
    // =========================================================================

    /*
     * Source: EJB3 Specification Purpose: defines the role which must have been
     * obtained during authentication and authorization in order to access this
     * method. This works in combination with the class-level @SecurityDomain
     * annotation above to fully define security semantics.
     */
    /**
     * see {@link IConfig#getServerTime()}
     */
    @RolesAllowed("user")
    public Date getServerTime() {
        return new Date();
    }

    /**
     * see {@link IConfig#getDatabaseTime()}
     */
    @RolesAllowed("user")
    // see above
    public Date getDatabaseTime() {
        Date date = jdbc.queryForObject("select now()", Date.class);
        return date;
    }

    /**
     * see {@link IConfig#getConfigValue(String)}
     */
    @RolesAllowed("user")
    // see above
    public String getConfigValue(String key) {
        return null;
    }

    /**
     * see {@link IConfig#setConfigValue(String, String)}
     */
    @RolesAllowed("system")
    // see above
    public void setConfigValue(String key, String value) {
        return;
    }
    
    /**
     * see {@link IConfig#getVersion()}
     */
    @RolesAllowed("user")
    // see above
    public String getVersion() {
        return Version.OMERO;
    }

}
