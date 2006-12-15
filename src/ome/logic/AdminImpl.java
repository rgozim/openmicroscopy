/*
 * ome.logic.AdminImpl
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.logic;

// Java imports
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.Interceptors;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

// Third-party libraries
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jmx.support.JmxUtils;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.hibernate.Criteria;
import org.hibernate.EmptyInterceptor;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.annotation.ejb.RemoteBinding;
import org.jboss.annotation.security.SecurityDomain;
import org.jboss.security.Util;

// Application-internal dependencies
import ome.annotations.NotNull;
import ome.annotations.RevisionDate;
import ome.annotations.RevisionNumber;
import ome.api.IAdmin;
import ome.api.ServiceInterface;
import ome.api.local.LocalAdmin;
import ome.api.local.LocalUpdate;
import ome.conditions.ApiUsageException;
import ome.conditions.InternalException;
import ome.conditions.SecurityViolation;
import ome.conditions.ValidationException;
import ome.model.IObject;
import ome.model.internal.Permissions;
import ome.model.internal.Permissions.Flag;
import ome.model.meta.Event;
import ome.model.meta.Experimenter;
import ome.model.meta.ExperimenterGroup;
import ome.model.meta.GroupExperimenterMap;
import ome.parameters.Filter;
import ome.parameters.Parameters;
import ome.security.ACLVoter;
import ome.security.AdminAction;
import ome.security.SecureAction;
import ome.security.SecuritySystem;
import ome.security.basic.BasicSecuritySystem;
import ome.security.basic.UpdateEventListener;
import ome.services.query.Definitions;
import ome.services.query.Query;
import ome.services.query.QueryParameterDef;
import ome.system.EventContext;
import ome.system.Roles;
import ome.system.SimpleEventContext;
import ome.tools.hibernate.ExtendedMetadata;
import ome.util.Utils;

/**
 * Provides methods for administering user accounts, passwords, as well as
 * methods which require special privileges.
 * 
 * Developer note: As can be expected, to perform these privileged the Admin
 * service has access to several resources that should not be generally used
 * while developing services. Misuse could circumvent security or auditing.
 * 
 * @author Josh Moore, josh.moore at gmx.de
 * @version $Revision$, $Date$
 * @see SecuritySystem
 * @see Permissions
 * @since 3.0-M3
 */
@TransactionManagement(TransactionManagementType.BEAN)
@Transactional
@RevisionDate("$Date$")
@RevisionNumber("$Revision$")
@Stateless
@Remote(IAdmin.class)
@RemoteBinding(jndiBinding = "omero/remote/ome.api.IAdmin")
@Local(IAdmin.class)
@LocalBinding(jndiBinding = "omero/local/ome.api.IAdmin")
@SecurityDomain("OmeroSecurity")
@Interceptors( { SimpleLifecycle.class })
public class AdminImpl extends AbstractLevel2Service implements LocalAdmin {

	protected transient SimpleJdbcTemplate jdbc;

	protected transient ExtendedMetadata em;

	protected transient SessionFactory sf;

	/** injector for usage by the container. Not for general use */
	public final void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
		throwIfAlreadySet(this.jdbc, jdbcTemplate);
		jdbc = jdbcTemplate;
	}

	/** injector for usage by the container. Not for general use */
	public final void setExtendedMetadata(ExtendedMetadata extMetadata) {
		throwIfAlreadySet(this.em, extMetadata);
		em = extMetadata;
	}

	/** injector for usage by the container. Not for general use */
	public final void setSessionFactory(SessionFactory sessionFactory) {
		throwIfAlreadySet(this.sf, sessionFactory);
		sf = sessionFactory;
	}

	@Override
	protected Class<? extends ServiceInterface> getServiceInterface() {
		return IAdmin.class;
	}

	// ~ LOCAL PUBLIC METHODS
	// =========================================================================

	@RolesAllowed("user")
	public Experimenter userProxy(final Long id) {
		if (id == null)
			throw new ApiUsageException("Id argument cannot be null.");

		Experimenter e = iQuery.get(Experimenter.class, id);
		return e;
	}

	@RolesAllowed("user")
	public Experimenter userProxy(final String omeName) {
		if (omeName == null)
			throw new ApiUsageException("omeName argument cannot be null.");

		Experimenter e = iQuery.findByString(Experimenter.class, "omeName",
				omeName);

		if (e == null) {
			throw new ApiUsageException("No such experimenter: " + omeName);
		}

		return e;
	}

	@RolesAllowed("user")
	public ExperimenterGroup groupProxy(Long id) {
		if (id == null)
			throw new ApiUsageException("Id argument cannot be null.");

		ExperimenterGroup g = iQuery.get(ExperimenterGroup.class, id);
		return g;
	}

	@RolesAllowed("user")
	public ExperimenterGroup groupProxy(final String groupName) {
		if (groupName == null)
			throw new ApiUsageException("groupName argument cannot be null.");

		ExperimenterGroup g = iQuery.findByString(ExperimenterGroup.class,
				"name", groupName);

		if (g == null) {
			throw new ApiUsageException("No such group: " + groupName);
		}

		return g;
	}

	@RolesAllowed("user")
	public List<Long> getLeaderOfGroupIds(final Experimenter e) {
		Assert.notNull(e);
		Assert.notNull(e.getId());

		List<Long> groupIds = iQuery.execute(new HibernateCallback() {
			public Object doInHibernate(Session session)
					throws HibernateException, SQLException {
				org.hibernate.Query q = session
						.createQuery("select g.id from ExperimenterGroup g where g.details.owner.id = :id");
				q.setParameter("id", e.getId());
				return q.list();
			}
		});
		return groupIds;
	}

	@RolesAllowed("user")
	public List<Long> getMemberOfGroupIds(final Experimenter e) {
		Assert.notNull(e);
		Assert.notNull(e.getId());

		List<Long> groupIds = iQuery.execute(new HibernateCallback() {
			public Object doInHibernate(Session session)
					throws HibernateException, SQLException {
				org.hibernate.Query q = session
						.createQuery("select m.parent.id from GroupExperimenterMap m "
								+ "where m.child.id = :id");
				q.setParameter("id", e.getId());
				return q.list();
			}
		});
		return groupIds;
	}

	// ~ User accessible interface methods
	// =========================================================================

	@RolesAllowed("user")
	public Experimenter getExperimenter(final Long id) {
		Experimenter e = iQuery.execute(new UserQ(new Parameters().addId(id)));

		if (e == null) {
			throw new ApiUsageException("No such experimenter: " + id);
		}

		return e;
	}

	@RolesAllowed("user")
	public Experimenter lookupExperimenter(final String omeName) {
		Experimenter e = iQuery.execute(new UserQ(new Parameters().addString(
				"name", omeName)));

		if (e == null) {
			throw new ApiUsageException("No such experimenter: " + omeName);
		}

		return e;
	}
	
	@RolesAllowed("user")
	public List<Experimenter> lookupExperimenters()
    {
		return iQuery.findAll(Experimenter.class, null);
    }

	@RolesAllowed("user")
	public ExperimenterGroup getGroup(Long id) {
		ExperimenterGroup g = iQuery.execute(new GroupQ(new Parameters()
				.addId(id)));

		if (g == null) {
			throw new ApiUsageException("No such group: " + id);
		}

		return g;
	}

	@RolesAllowed("user")
	public ExperimenterGroup lookupGroup(final String groupName) {
		ExperimenterGroup g = iQuery.execute(new GroupQ(new Parameters()
				.addString("name", groupName)));

		if (g == null) {
			throw new ApiUsageException("No such group: " + groupName);
		}

		return g;
	}
	
	@RolesAllowed("user")
	public List<ExperimenterGroup> lookupGroups()
    {
		return iQuery.findAll(ExperimenterGroup.class, null);
    }

	@RolesAllowed("user")
	public Experimenter[] containedExperimenters(Long groupId) {
		List<Experimenter> experimenters = 
			iQuery.findAllByQuery("select e from Experimenter as e left outer " +
					"join e.groupExperimenterMap as map left outer join " +
					"map.parent as g where g.id = :id",
					new Parameters().addId(groupId));
		return (Experimenter[]) experimenters.toArray(
			new Experimenter[experimenters.size()]);
	}

	@RolesAllowed("user")
	public ExperimenterGroup[] containedGroups(Long experimenterId) {
		List<ExperimenterGroup> groups = 
			iQuery.findAllByQuery("select g from ExperimenterGroup as g left " +
					"outer join g.groupExperimenterMap as map left outer " +
					"join map.child as e where e.id = :id",
					new Parameters().addId(experimenterId));
		return (ExperimenterGroup[]) groups.toArray(
			new ExperimenterGroup[groups.size()]);
	}

	// ~ System-only interface methods
	// =========================================================================

	@RolesAllowed("system")
	public void synchronizeLoginCache() {
		String string = "omero:service=LoginConfig";
		// using Spring utilities to get MBeanServer
		MBeanServer mbeanServer = JmxUtils.locateMBeanServer();
		getLogger().debug("Acquired MBeanServer.");
		ObjectName name;
		try {
			// defined in app/resources/jboss-service.xml
			name = new ObjectName(string);
			mbeanServer.invoke(name, "flushAuthenticationCaches",
					new Object[] {}, new String[] {});
			getLogger().debug("Flushed authentication caches.");
		} catch (InstanceNotFoundException infe) {
			getLogger().warn(
					string + " not found. Won't synchronize login cache.");
		} catch (Exception e) {
			InternalException ie = new InternalException(e.getMessage());
			ie.setStackTrace(e.getStackTrace());
			throw ie;
		}
	}
	
	@RolesAllowed("system")
    public void updateExperimenter(@NotNull Experimenter experimenter)
    {
    	iUpdate.saveObject(experimenter);
    }
    

	@RolesAllowed("system")
    public void updateGroup(@NotNull ExperimenterGroup group)
	{
		iUpdate.saveObject(group);
	}

	@RolesAllowed("system")
	public long createUser(Experimenter newUser, String defaultGroup) {
		return createExperimenter(newUser, groupProxy(defaultGroup), 
				groupProxy("user"));
	}

	@RolesAllowed("system")
	public long createSystemUser(Experimenter newSystemUser) {
		return createExperimenter(newSystemUser, groupProxy("system"),
				groupProxy("user"));
	}

	@RolesAllowed("system")
	public long createExperimenter(Experimenter experimenter,
			ExperimenterGroup defaultGroup, ExperimenterGroup...otherGroups) {
		// TODO check that no other group is default

		Experimenter e = copyUser(experimenter);

		if (defaultGroup == null || defaultGroup.getId() == null) {
			throw new ApiUsageException("Default group may not be null.");
		}

		SecureAction action = new SecureAction() {
			public <T extends IObject> T updateObject(T obj) {
				return iUpdate.saveAndReturnObject(obj);
			}
		};

		e = getSecuritySystem().doAction(e, action);

		final GroupExperimenterMap defaultGroupMap = new GroupExperimenterMap();
		defaultGroupMap.link(groupProxy(defaultGroup.getId()), userProxy(e
				.getId()));
		defaultGroupMap.setDefaultGroupLink(Boolean.TRUE);
		defaultGroupMap.setDetails(getSecuritySystem().newTransientDetails(
				defaultGroupMap));
		getSecuritySystem().doAction(defaultGroupMap, action);

		if (null != otherGroups) {
			for (ExperimenterGroup group : otherGroups) {
				if (group == null)
					continue;
				if (group.getId() == null) {
					throw new ApiUsageException(
							"Groups must be previously saved during "
									+ "Experimenter creation.");
				}
				GroupExperimenterMap groupMap = new GroupExperimenterMap();
				groupMap.link(groupProxy(group.getId()), userProxy(e.getId()));
				groupMap.setDefaultGroupLink(Boolean.FALSE);
				groupMap.setDetails(getSecuritySystem().newTransientDetails(
						groupMap));
				getSecuritySystem().doAction(groupMap, action);
			}
		}

		changeUserPassword(e.getOmeName(), " ");
		return e.getId();
	}

	@RolesAllowed("system")
	public long createGroup(ExperimenterGroup group) {
		group = copyGroup(group);
		ExperimenterGroup g = getSecuritySystem().doAction(group,
				new SecureAction() {
					public <T extends IObject> T updateObject(T obj) {
						return iUpdate.saveAndReturnObject(obj);
					}
				});
		return g.getId();
	}

	@RolesAllowed("system")
	public void addGroups(Experimenter user, ExperimenterGroup... groups) {
		if (user == null)
			return; // Handled by annotations
		if (groups == null)
			return;

		Experimenter foundUser = userProxy(user.getId());
		for (ExperimenterGroup group : groups) {
			ExperimenterGroup foundGroup = groupProxy(group.getId());
			GroupExperimenterMap map = new GroupExperimenterMap();
			map.link(foundGroup, foundUser);
			map.setDetails(getSecuritySystem().newTransientDetails(map));
			getSecuritySystem().doAction(map, new SecureAction() {
				public <T extends IObject> T updateObject(T obj) {
					return iUpdate.saveAndReturnObject(obj);
				}
			});
		}
		iUpdate.flush();
	}

	@RolesAllowed("system")
	public void removeGroups(Experimenter user, ExperimenterGroup... groups) {
		if (user == null)
			return;
		if (groups == null)
			return;

		Experimenter foundUser = getExperimenter(user.getId());
		List<Long> toRemove = new ArrayList<Long>();

		for (ExperimenterGroup g : groups) {
			if (g.getId() != null)
				toRemove.add(g.getId());
		}
		for (GroupExperimenterMap map : (List<GroupExperimenterMap>) foundUser
				.collectGroupExperimenterMap(null)) {
			if (toRemove.contains(map.parent().getId())) {
				map.child().removeGroupExperimenterMap(map, false);
				map.parent().removeGroupExperimenterMap(map, false);
				getSecuritySystem().doAction(map, new SecureAction() {
					public <T extends IObject> T updateObject(T obj) {
						iUpdate.deleteObject(obj);
						return null;
					}
				});
			}
		}
		iUpdate.flush();
	}

	@RolesAllowed("system")
	public void setDefaultGroup(Experimenter user, ExperimenterGroup group) {
		if (user == null)
			return;
		if (group == null)
			return;

		if (group.getId() == null) {
			throw new ApiUsageException("Group argument to setDefaultGroup "
					+ "must be managed (i.e. have an id)");
		}

		boolean newDefaultSet = false;
		Experimenter foundUser = getExperimenter(user.getId());
		for (GroupExperimenterMap map : (List<GroupExperimenterMap>) foundUser
				.collectGroupExperimenterMap(null)) {
			if (map.parent().getId().equals(group.getId())) {
				map.setDefaultGroupLink(Boolean.TRUE);
				newDefaultSet = true;
			} else {
				map.setDefaultGroupLink(Boolean.FALSE);
			}
		}

		if (!newDefaultSet) {
			throw new ApiUsageException("Group " + group.getId() + " was not "
					+ "found for user " + user.getId());
		}

		iUpdate.flush();
	}

	@RolesAllowed("system")
	public void setGroupOwner(ExperimenterGroup group, Experimenter owner) {
		if (owner == null) return;
		if (group == null) return;

		if (group.getId() == null) {
			throw new ApiUsageException("Group argument to setGroupOwner "
					+ "must be managed (i.e. have an id)");
		}

		// TODO add an @Managed annotation
		if (owner.getId() == null) {
			throw new ApiUsageException("Owner argument to setGroupOwner "
					+ "must be managed (i.e. have an id)");
		}

		Experimenter foundUser = userProxy(owner.getId());
		ExperimenterGroup foundGroup = groupProxy(group.getId());
		foundGroup.getDetails().setOwner(foundUser);
		iUpdate.flush();
	}
	
	@RolesAllowed("system")
	public ExperimenterGroup getDefaultGroup(@NotNull
	Long experimenterId) {
		ExperimenterGroup g = iQuery.findByQuery(
				"select g from ExperimenterGroup g "
						+ "join fetch g.groupExperimenterMap m "
						+ "join fetch m.child  e " + "where e.id = :id "
						+ "and m.defaultGroupLink = true", new Parameters()
						.addId(experimenterId));
		if (g == null) {
			throw new ValidationException("The user " + experimenterId
					+ " has no default group set.");
		}
		return g;
	}

	@RolesAllowed("system")
	public void deleteExperimenter(Experimenter user) {
		Experimenter e = userProxy(user.getId());
		int count = jdbc.update(
				"delete from password where experimenter_id = ?", e.getId());

		if (count == 0) {
			getLogger().info(
					"No password found for user " + e.getOmeName()
							+ ". Cannot delete.");
		}

		iUpdate.deleteObject(e);
	}

	// ~ chown / chgrp / chmod
	// =========================================================================

	@RolesAllowed("user")
	public void changeOwner(IObject iObject, String omeName) {
		// should take an Owner
		IObject copy = iQuery.get(iObject.getClass(), iObject.getId());
		Experimenter owner = userProxy(omeName);
		copy.getDetails().setOwner(owner);
		iUpdate.saveObject(copy);
	}

	@RolesAllowed("user")
	public void changeGroup(IObject iObject, String groupName) {
		final LocalUpdate update = iUpdate;
		// should take a group
		final IObject copy = iQuery.get(iObject.getClass(), iObject.getId());
		final ExperimenterGroup group = groupProxy(groupName);
		
		// do check TODO refactor
		final EventContext ec = getSecuritySystem().getEventContext();
		if ( ! ec.getMemberOfGroupsList().contains(group.getId()) && 
				! ec.isCurrentUserAdmin() )
			throw new SecurityViolation("Cannot change group for:"+iObject);
		
		// make change.
		copy.getDetails().setGroup(group);
		getSecuritySystem().doAction(copy, new SecureAction() {
			public IObject updateObject(IObject obj) {
				update.flush();
				return null;
			}
		});
	}

	/**
	 * the implementation of this method is somewhat tricky in that
	 * {@link Permissions} changes must be allowed even when other updates are
	 * not. Therefore, we must manually check if the object belongs to this user
	 * or is admin (before the call to {@link SecuritySystem#runAsAdmin(AdminAction)}
	 * 
	 * This logic is duplicated in {@link BasicSecuritySystem#checkManagedDetails(IObject, ome.model.internal.Details)}.
	 * 
	 * @see IAdmin#changePermissions(IObject, Permissions)
	 * @see <a
	 *      href="http://trac.openmicroscopy.org.uk/omero/ticket/293">ticket:293</a>
	 */
	@RolesAllowed("user")
	public void changePermissions(final IObject iObject, final Permissions perms) {

		final ACLVoter aclVoter = getSecuritySystem().getACLVoter(); // TODO inject
		final IObject[] copy = new IObject[1];

		// first load the instance.
		getSecuritySystem().runAsAdmin(new AdminAction(){
			public void runAsAdmin() {
				copy[0] = iQuery.get(iObject.getClass(), iObject.getId());
			}
		});

		// now check for ownership _outside_ of runAsAdmin
		if (!aclVoter.allowChmod(copy[0]))
			throw new SecurityViolation("Cannot change permissions for:"+
					copy[0]);
		
		// if we reach here, ok to save.
		getSecuritySystem().runAsAdmin(new AdminAction(){
			public void runAsAdmin() {
				copy[0].getDetails().setPermissions(perms);
				iUpdate.flush();
			}
		});
	}

	@RolesAllowed("system")
	public boolean[] unlock(final IObject... iObjects) {
		// do nothing if possible
		if (iObjects == null | iObjects.length < 1)
			return new boolean[] {};

		// create a new session. It's important that we pass in the empty
		// interceptor here, otherwise even root wouldn't be allowed to unlock
		// the instance.
		Session s = SessionFactoryUtils.getNewSession(sf,
				EmptyInterceptor.INSTANCE);
		
		// similarly, we need to disable certain backend systems. first we
		// disable the UpdateEventListener because it wants to set entities
		// from a different session on these IObjects. 
		// See: https://trac.openmicroscopy.org.uk/omero/ticket/366
		getSecuritySystem().disable(UpdateEventListener.UPDATE_EVENT);
		
		try 
		{
			Long eventId = getSecuritySystem().getEventContext().getCurrentEventId(); 
			Event updateEvent = (Event) s.get(Event.class, eventId);

			try {
				boolean[] isUnlocked = new boolean[iObjects.length];
				for (int i = 0; i < iObjects.length; i++) {
					IObject orig = iObjects[i];
	
					// do nothing if possible again.
					if (orig == null || orig.getId() == null) {
						isUnlocked[i] = true;
						continue;
					}
	
					// get the original to operate on
					final IObject object = (IObject) s.load(orig.getClass(), orig
							.getId());
	
					// if it's not locked, we don't need to look further.
					if (!object.getDetails().getPermissions().isSet(Flag.LOCKED)) {
						isUnlocked[i] = true;
						continue;
					}
	
					// since it's a managed entity it's class.getName() might
					// contain
					// some byte-code generation string
					final Class<? extends IObject> klass = Utils.trueClass(object
							.getClass());
	
					final long id = object.getId().longValue();
	
					// the values that could possibly link to this instance.
					String[][] checks = em.getLockChecks(klass);
	
					// reporting
					long total = 0L;
					Map<String, Long> counts = new HashMap<String, Long>();
	
					// run the individual queries
					for (String[] check : checks) {
						final String hql = String.format(
								"select count(*) from %s where %s%s = :id ",
								check[0], check[1], ".id");
						org.hibernate.Query q = s.createQuery(hql);
						q.setLong("id", id);
						Long count = (Long) q.iterate().next();
	
						if (count != null && count.longValue() > 0) {
							total += count.longValue();
							counts.put(hql, count);
						}
					}
	
					// reporting
					if (getLogger().isDebugEnabled()) {
						getLogger().debug(counts);
					}
	
					// if there are no links, the we can unlock
					// the actual unlocking happens on flush below.
					if (total == 0) {
						object.getDetails().getPermissions().unSet(Flag.LOCKED);
						object.getDetails().setUpdateEvent( updateEvent );
						isUnlocked[i] = true;
					} else {
						isUnlocked[i] = false;
					}
	
				}
				return isUnlocked;
			}
	
			finally {
				s.flush();
				s.disconnect();
				s.close();
			}
			
		} finally { 
			getSecuritySystem().enable(UpdateEventListener.UPDATE_EVENT);
		}
		
	}

	// ~ Passwords
	// =========================================================================

	@RolesAllowed("user")
	public void changePassword(String newPassword) {
		internalChangeUserPasswordById(
				getSecuritySystem().getEventContext().getCurrentUserId(),
				newPassword);
	}

	@RolesAllowed("system")
	public void changeUserPassword(String omeName, String newPassword) {
		Experimenter e = lookupExperimenter(omeName);
		internalChangeUserPasswordById(e.getId(), newPassword);
	}

	// ~ Security context
	// =========================================================================

	@RolesAllowed("user")
	public Roles getSecurityRoles() {
		return getSecuritySystem().getSecurityRoles();
	}

	@RolesAllowed("user")
	public EventContext getEventContext() {
		return new SimpleEventContext(getSecuritySystem().getEventContext());
	}

	// ~ Helpers
	// =========================================================================

	protected Experimenter copyUser(Experimenter e) {
		if (e.getOmeName() == null) {
			throw new ValidationException("OmeName may not be null.");
		}
		Experimenter copy = new Experimenter();
		copy.setOmeName(e.getOmeName());
		copy.setFirstName(e.getOmeName());
		copy.setLastName(e.getLastName());
		copy.setEmail(e.getEmail());
		if (e.getDetails() != null && e.getDetails().getPermissions() != null) {
			copy.getDetails().setPermissions(e.getDetails().getPermissions());
		}
		// TODO make ShallowCopy-like which ignores collections and details.
		// if possible, values should be validated. i.e. iTypes should say what
		// is non-null
		return copy;
	}

	protected ExperimenterGroup copyGroup(ExperimenterGroup g) {
		if (g.getName() == null) {
			throw new ValidationException("Group name may not be null.");
		}
		ExperimenterGroup copy = new ExperimenterGroup();
		copy.setDescription(g.getDescription());
		copy.setName(g.getName());
		copy.setDetails(getSecuritySystem().newTransientDetails(g));
		// TODO see shallow copy comment on copy user
		return copy;
	}

	// ~ Password access
	// =========================================================================

	protected void internalChangeUserPasswordById(Long id, String password) {
		int results = jdbc.update("update password set hash = ? "
				+ "where experimenter_id = ? ", preparePassword(password), id);
		if (results < 1) {
			results = jdbc.update("insert into password values (?,?) ", id,
					preparePassword(password));
		}
		synchronizeLoginCache();
	}

	protected String preparePassword(String newPassword) {
		// This allows setting passwords to "null" - locked account.
		return newPassword == null ? null
		// This allows empty passwords to be considered "open-access"
				: newPassword.trim().length() == 0 ? newPassword
				// Regular MD5 digest.
						: passwordDigest(newPassword);
	}

	protected String passwordDigest(String clearText) {
		if (clearText == null) {
			throw new ApiUsageException("Value for digesting may not be null");
		}

		// These constants are also defined in app/resources/jboss-login.xml
		// and this method is called from {@link JBossLoginModule}
		String hashedText = Util.createPasswordHash("MD5", "base64",
				"ISO-8859-1", null, clearText, null);

		if (hashedText == null) {
			throw new InternalException("Failed to obtain digest.");
		}
		return hashedText;
	}

	// ~ Queries for pulling full experimenter/experimenter group graphs
	// =========================================================================

	static abstract class BaseQ<T> extends Query<T> {
		static Definitions defs = new Definitions(new QueryParameterDef("name",
				String.class, true), new QueryParameterDef("id", Long.class,
				true));

		public BaseQ(Parameters params) {
			super(defs, new Parameters(new Filter().unique()).addAll(params));
		}

	}

	static class UserQ extends BaseQ<Experimenter> {
		public UserQ(Parameters params) {
			super(params);
		}

		@Override
		protected void buildQuery(Session session) throws HibernateException,
				SQLException {
			Criteria c = session.createCriteria(Experimenter.class);

			Criteria m = c.createCriteria("groupExperimenterMap",
					Query.LEFT_JOIN);
			Criteria g = m.createCriteria("parent", Query.LEFT_JOIN);

			if (value("name") != null) {
				c.add(Restrictions.eq("omeName", value("name")));
			}

			else if (value("id") != null) {
				c.add(Restrictions.eq("id", value("id")));
			}

			else {
				throw new InternalException(
						"Name and id are both null for user query.");
			}
			setCriteria(c);

		}
	}

	static class GroupQ extends BaseQ<ExperimenterGroup> {
		public GroupQ(Parameters params) {
			super(params);
		}

		@Override
		protected void buildQuery(Session session) throws HibernateException,
				SQLException {
			Criteria c = session.createCriteria(ExperimenterGroup.class);
			Criteria m = c.createCriteria("groupExperimenterMap",
					Query.LEFT_JOIN);
			Criteria e = m.createCriteria("child", Query.LEFT_JOIN);

			if (value("name") != null) {
				c.add(Restrictions.eq("name", value("name")));
			}

			else if (value("id") != null) {
				c.add(Restrictions.eq("id", value("id")));
			}

			else {
				throw new InternalException(
						"Name and id are both null for group query.");
			}
			setCriteria(c);

		}
	}
}
