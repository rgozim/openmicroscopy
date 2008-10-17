/*
 * ome.logic.AdminImpl
 *
 *   Copyright 2007 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.logic;

// Java imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.Interceptors;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.InitialLdapContext;

import ome.annotations.RevisionDate;
import ome.annotations.RevisionNumber;
import ome.api.IAdmin;
import ome.api.ILdap;
import ome.api.ServiceInterface;
import ome.api.local.LocalLdap;
import ome.conditions.ApiUsageException;
import ome.model.internal.Permissions;
import ome.model.meta.Experimenter;
import ome.model.meta.ExperimenterGroup;
import ome.security.LdapUtil;
import ome.security.SecuritySystem;
import ome.services.util.OmeroAroundInvoke;
import ome.system.OmeroContext;

import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.annotation.ejb.RemoteBinding;
import org.jboss.annotation.ejb.RemoteBindings;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides methods for administering user accounts, passwords, as well as
 * methods which require special privileges.
 * 
 * Developer note: As can be expected, to perform these privileged the Admin
 * service has access to several resources that should not be generally used
 * while developing services. Misuse could circumvent security or auditing.
 * 
 * @author Aleksandra Tarkowska, A.Tarkowska@dundee.ac.uk
 * @version $Revision: 1552 $, $Date: 2007-05-23 09:43:33 +0100 (Wed, 23 May
 *          2007) $
 * @see SecuritySystem
 * @see Permissions
 * @since 3.0-M3
 */
@TransactionManagement(TransactionManagementType.BEAN)
@Transactional(readOnly = true)
@RevisionDate("$Date: 2007-05-23 09:43:33 +0100 (Wed, 23 May 2007) $")
@RevisionNumber("$Revision: 1552 $")
@Stateless
@Remote(ILdap.class)
@RemoteBindings( {
        @RemoteBinding(jndiBinding = "omero/remote/ome.api.ILdap"),
        @RemoteBinding(jndiBinding = "omero/secure/ome.api.ILdap", clientBindUrl = "sslsocket://0.0.0.0:3843") })
@Local(ILdap.class)
@LocalBinding(jndiBinding = "omero/local/ome.api.local.LocalLdap")
@Interceptors( { OmeroAroundInvoke.class, SimpleLifecycle.class })
public class LdapImpl extends AbstractLevel2Service implements LocalLdap {

    protected transient LdapTemplate ldapTemplate;

    protected transient SimpleJdbcTemplate jdbc;

    protected transient String groups;

    protected transient String attributes;

    protected transient String values;

    protected transient boolean config;

    protected transient IAdmin adminService;

    /** injector for usage by the container. Not for general use */
    public final void setLdapTemplate(LdapTemplate ldapTemplate) {
        getBeanHelper().throwIfAlreadySet(this.ldapTemplate, ldapTemplate);
        this.ldapTemplate = ldapTemplate;
    }

    /** injector for usage by the container. Not for general use */
    public final void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
        getBeanHelper().throwIfAlreadySet(this.jdbc, jdbcTemplate);
        jdbc = jdbcTemplate;
    }

    /** injector for usage by the container. Not for general use */
    public final void setGroups(String groups) {
        getBeanHelper().throwIfAlreadySet(this.groups, groups);
        this.groups = groups;
    }

    /** injector for usage by the container. Not for general use */
    public final void setAttributes(String attributes) {
        getBeanHelper().throwIfAlreadySet(this.attributes, attributes);
        this.attributes = attributes;
    }

    /** injector for usage by the container. Not for general use */
    public final void setValues(String values) {
        getBeanHelper().throwIfAlreadySet(this.values, values);
        this.values = values;
    }

    /** injector for usage by the container. Not for general use */
    public final void setConfig(boolean config) {
        this.config = config;
    }

    /** injector for usage by the container. Not for general use */
    public void setAdminService(IAdmin adminService) {
        getBeanHelper().throwIfAlreadySet(this.adminService, adminService);
        this.adminService = adminService;
    }

    // ~ System-only interface methods
    // =========================================================================

    @RolesAllowed("system")
    public List<Experimenter> searchAll() {
        EqualsFilter filter = new EqualsFilter("objectClass", "person");
        return ldapTemplate.search(DistinguishedName.EMPTY_PATH, filter
                .encode(), new PersonContextMapper());
    }

    @RolesAllowed("system")
    public List<Experimenter> searchByAttribute(String dns, String attr,
            String value) {
        DistinguishedName dn;
        if (dns == null) {
            dn = DistinguishedName.EMPTY_PATH;
        } else {
            dn = new DistinguishedName(dns);
        }

        if (attr != null && !attr.equals("") && value != null
                && !value.equals("")) {
            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectClass", "person"));
            filter.and(new EqualsFilter(attr, value));

            return ldapTemplate.search(dn, filter.encode(),
                    new PersonContextMapper());
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    @RolesAllowed("system")
    public Experimenter searchByDN(String dns) {
        DistinguishedName dn = new DistinguishedName(dns);
        return (Experimenter) ldapTemplate
                .lookup(dn, new PersonContextMapper());
    }

    @RolesAllowed("system")
    public String findDN(String username) {
        DistinguishedName dn;
        AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter("objectClass", "person"));
        filter.and(new EqualsFilter("cn", username));
        List<Experimenter> p = ldapTemplate.search("", filter.encode(),
                new PersonContextMapper());
        if (p.size() == 1) {
            Experimenter exp = p.get(0);
            dn = new DistinguishedName(exp.retrieve("LDAP_DN").toString());
        } else {
            throw new ApiUsageException(
                    "Cannot find DistinguishedName or more then one 'cn' under the specified base");
        }
        return dn.toString();
    }

    @RolesAllowed("system")
    public Experimenter findExperimenter(String username) {
        AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter("objectClass", "person"));
        filter.and(new EqualsFilter("cn", username));
        List<Experimenter> p = ldapTemplate.search("", filter.encode(),
                new PersonContextMapper());
        Experimenter exp = null;
        if (p.size() == 1) {
            exp = p.get(0);
        } else {
            throw new ApiUsageException(
                    "Cannot find DistinguishedName. More then one 'cn' under the specified base");
        }
        return exp;
    }

    @RolesAllowed("system")
    public List<String> searchDnInGroups(String attr, String value) {
        if (attr != null && !attr.equals("") && value != null
                && !value.equals("")) {
            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("objectClass", "groupOfNames"));
            filter.and(new EqualsFilter(attr, value));
            return ldapTemplate.search("", filter.encode(),
                    new GroupAttributMapper());
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    @RolesAllowed("system")
    public List<Experimenter> searchByAttributes(String dn,
            String[] attributes, String[] values) {
        if (attributes.length != values.length) {
            return Collections.EMPTY_LIST;
        }
        AndFilter filter = new AndFilter();
        for (int i = 0; i < attributes.length; i++) {
            filter.and(new EqualsFilter(attributes[i], values[i]));
        }
        return ldapTemplate.search(new DistinguishedName(dn), filter.encode(),
                new PersonContextMapper());
    }

    @RolesAllowed("system")
    public List<ExperimenterGroup> searchGroups() {
        // TODO Auto-generated method stub
        return null;
    }

    @RolesAllowed("system")
    @Transactional(readOnly = false)
    public void setDN(Long experimenterID, String dn) {
        LdapUtil.setDNById(jdbc, experimenterID, dn);
    }

    // Getters and Setters for requiroments
    // =========================================================================

    @RolesAllowed("system")
    public boolean getSetting() {
        return this.config;
    }

    @RolesAllowed("system")
    public List<String> getReqGroups() {
        if (this.groups.equals("")) {
            return Collections.EMPTY_LIST;
        }
        return Arrays.asList(this.groups.split(","));
    }

    @RolesAllowed("system")
    public String[] getReqAttributes() {
        if (this.attributes.equals("")) {
            return new String[] {};
        }
        return this.attributes.split(",");
    }

    @RolesAllowed("system")
    public String[] getReqValues() {
        if (this.values.equals("")) {
            return new String[] {};
        }
        return this.values.split(",");
    }

    @RolesAllowed("system")
    public void setReqAttributes(String[] arg0) {
        // TODO Auto-generated method stub

    }

    @RolesAllowed("system")
    public void setReqGroups(List<String> arg0) {
        // TODO Auto-generated method stub

    }

    @RolesAllowed("system")
    public void setReqValues(String[] arg0) {
        // TODO Auto-generated method stub

    }

    // ~ LOCAL PUBLIC METHODS
    // ========================================================================

    // ~ AttributesMapper
    // =========================================================================

    public static class UidAttributMapper implements AttributesMapper {

        @RolesAllowed("system")
        public Object mapFromAttributes(Attributes attributes)
                throws NamingException {
            ArrayList l = new ArrayList();
            for (NamingEnumeration ae = attributes.getAll(); ae
                    .hasMoreElements();) {
                Attribute attr = (Attribute) ae.next();
                String attrId = attr.getID();
                for (Enumeration vals = attr.getAll(); vals.hasMoreElements();) {
                    DistinguishedName dn = new DistinguishedName((String) vals
                            .nextElement());
                    if (attrId.equals("memberUid")) {
                        l.add(dn);
                    }
                }
            }
            return l;
        }

    }

    public static class GroupAttributMapper implements AttributesMapper {

        @RolesAllowed("system")
        public Object mapFromAttributes(Attributes attributes)
                throws NamingException {
            String groupName = null;
            if (attributes.get("cn") != null) {
                groupName = (String) attributes.get("cn").get();
            }
            return groupName;
        }

    }

    // ~ ContextMapper
    // =========================================================================

    public class PersonContextMapper implements ContextMapper {

        private DistinguishedName dn = new DistinguishedName();

        public DistinguishedName getDn() {
            return dn;
        }

        public void setDn(DistinguishedName dn) {
            this.dn = dn;
        }

        @RolesAllowed("system")
        public Object mapFromContext(Object ctx) {
            DirContextAdapter context = (DirContextAdapter) ctx;
            DistinguishedName dn = new DistinguishedName(context.getDn());
            try {
                dn.addAll(0, new DistinguishedName(getBase()));
            } catch (InvalidNameException e) {
                return null;
            }
            setDn(dn);

            Experimenter person = new Experimenter();
            if (context.getStringAttribute("cn") != null) {
                person.setOmeName(context.getStringAttribute("cn"));
            }
            if (context.getStringAttribute("sn") != null) {
                person.setLastName(context.getStringAttribute("sn"));
            }
            if (context.getStringAttribute("givenName") != null) {
                person.setFirstName(context.getStringAttribute("givenName"));
            }
            if (context.getStringAttribute("mail") != null) {
                person.setEmail(context.getStringAttribute("mail"));
            }
            person.putAt("LDAP_DN", dn.toString());
            return person;
        }

    }

    public Class<? extends ServiceInterface> getServiceInterface() {
        return ILdap.class;
    }

    /**
     * Gets base from the OmeroContext -> Bean: contextSource
     * 
     * @return String
     */
    public String getBase() {
        String base = null;
        LdapContextSource ctx = (LdapContextSource) OmeroContext
                .getManagedServerContext().getBean("contextSource");
        try {
            base = ctx.getReadOnlyContext().getNameInNamespace();
        } catch (NamingException e) {
            throw new ApiUsageException(
                    "Cannot get BASE from ContextSource. Naming exception! "
                            + e.toString());
        }
        return base;

    }

    // ~ LocalLdap - Authentication
    // =========================================================================

    /**
     * Creates the initial context with no connection request controls
     * 
     * @return {@link javax.naming.ldap.LdapContext}
     */
    protected boolean isAuthContext(String username, String password) {
        // Set up environment for creating initial context
        LdapContextSource ctx = (LdapContextSource) OmeroContext
                .getManagedServerContext().getBean("contextSource");
        Hashtable<String, String> env = new Hashtable<String, String>(5, 0.75f);
        try {
            env = (Hashtable<String, String>) ctx.getReadOnlyContext()
                    .getEnvironment();

            if (username != null && !username.equals("")) {
                env.put(Context.SECURITY_PRINCIPAL, username);
                if (password != null) {
                    env.put(Context.SECURITY_CREDENTIALS, password);
                }
            }
            new InitialLdapContext(env, null);
            return true;
        } catch (AuthenticationException authEx) {
            throw new ApiUsageException("Authentication falilure! "
                    + authEx.toString());
        } catch (NamingException e) {
            throw new ApiUsageException("Naming exception! " + e.toString());
        }
    }

    /**
     * Valids password for base. Base is user's DN. When context was created
     * successful specyfied requrements are valid.
     * 
     * @return boolean
     */
    @RolesAllowed("system")
    public boolean validatePassword(String base, String password) {
        if (isAuthContext(base, password)) {
            // Check requiroments
            return validateRequiroments(base);
        }
        return false;
    }

    /**
     * Gets user from LDAP for checking him by requirements and setting his
     * details on DB
     * 
     * @return {@link ome.system.ServiceFactory}
     */
    @Transactional(readOnly = false)
    @RolesAllowed("system")
    public boolean createUserFromLdap(String username, String password) {
        // Find user by DN
        Experimenter exp = findExperimenter(username);
        DistinguishedName dn = new DistinguishedName(exp.retrieve("LDAP_DN")
                .toString());

        // DistinguishedName converted toString includes spaces
        if (!validateRequiroments(dn.toString())) {
            return false;
        }

        // Valid user's password
        boolean access = validatePassword(dn.toString(), password);

        if (access) {
            // If validation is successful create new user in DB

            long id = adminService.createExperimenter(exp, adminService
                    .lookupGroup("default"), adminService.lookupGroup("user"));

            // Set user's DN in PASSWORD table (add sufix on the beginning)
            setDN(id, dn.toString());
        }
        return access;
    }

    /**
     * Valids specyfied requirements for base (groups, attributes)
     * 
     * @return boolean
     */
    @RolesAllowed("system")
    public boolean validateRequiroments(String base) {
        boolean result = false;

        // list of groups
        List<String> groups = getReqGroups();
        // List of attributes
        String[] attrs = getReqAttributes();
        // List of attributes
        String[] vals = getReqValues();

        if (attrs.length != vals.length) {
            throw new ApiUsageException(
                    "Configuration exception. Attributes should have value on the omero.properties.");
        }

        // if groups
        if (groups.size() > 0) {
            List usergroups = searchDnInGroups("member", base);
            result = isInGroups(groups, usergroups);
        } else {
            result = true;
        }

        // if attributes
        if (result) {

            if (attrs.length > 0) {
                // cut DN
                DistinguishedName dn = new DistinguishedName(base);
                DistinguishedName baseDn = new DistinguishedName(getBase());
                for (int i = 0; i < baseDn.size(); i++) {
                    dn.removeFirst();
                }

                List<Experimenter> l = searchByAttributes(dn.toString(), attrs,
                        vals);
                if (l.size() <= 0) {
                    result = false;
                } else {
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * Checks that user's group list contains require groups. If one of user's
     * groups is on require groups' list will return true.
     * 
     * @return boolean
     */
    @RolesAllowed("system")
    public boolean isInGroups(List groups, List usergroups) {
        // user is not in groups
        if (usergroups.size() <= 0) {
            return false;
        }
        boolean flag = false;
        // checks containing
        for (int i = 0; i < usergroups.size(); i++) {
            if (groups.contains(usergroups.get(i))) {
                flag = true;
            }
        }
        return flag;
    }

}
