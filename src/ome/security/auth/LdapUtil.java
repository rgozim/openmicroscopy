/*
 *   $Id$
 *
 *   Copyright 2007 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.security.auth;

import java.util.List;
import java.util.Map;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcOperations;

/**
 * Static methods for dealing with LDAP (DN) and the "password" table. Used
 * primarily by {@link ome.security.JBossLoginModule}
 *
 * @author Aleksandra Tarkowska, A.Tarkowska at dundee.ac.uk
 * @see SecuritySystem
 * @see ome.logic.LdapImpl
 * @since 3.0-Beta3
 */
public class LdapUtil {

    private final SimpleJdbcOperations jdbc;

    private final boolean config;

    public LdapUtil(SimpleJdbcOperations jdbc, boolean config) {
        this.jdbc = jdbc;
        this.config = config;
    }

    public boolean getConfig() {
        return this.config;
    }

	public void setDNById(Long id, String dn) {
		int results = jdbc
				.update(
						"update password set dn = ? where experimenter_id = ? ",
						dn, id);
		if (results < 1) {
			results = jdbc.update("insert into password values (?,?,?) ", id,
					null, dn);
		}
	}

	public List<Map<String, Object>> lookupLdapAuthExperimenters() {
		return jdbc
				.queryForList(
						"select dn, experimenter_id from password where dn is not null ");
	}

	public String lookupLdapAuthExperimenter(Long id) {
		String s;

		try {
			s = jdbc
					.queryForObject(
							"select dn from password where dn is not null and experimenter_id = ? ",
							String.class, id);
		} catch (EmptyResultDataAccessException e) {
			s = null;
		}

		return s;
	}
}
