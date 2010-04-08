/*
 *   $Id$
 *
 *   Copyright 2009 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.security.auth;


/**
 * Central {@link PasswordProvider} which uses the "password" table in the
 * central OMERO database.
 * 
 * @author Josh Moore, josh at glencoesoftware.com
 * @since 4.0
 */

public class JdbcPasswordProvider extends ConfigurablePasswordProvider {

    public JdbcPasswordProvider(PasswordUtil util) {
        super(util);
    }

    public JdbcPasswordProvider(PasswordUtil util, boolean ignoreUnknown) {
        super(util);
    }

    @Override
    public boolean hasPassword(String user) {
        Long id = util.userId(user);
        return id != null;
    }

    /**
     * Retrieves password from the database and calls
     * {@link ConfigurablePasswordProvider#comparePasswords(String, String)}.
     * Uses default logic if user is unknown.
     */
    @Override
    public Boolean checkPassword(String user, String password) {
        Long id = util.userId(user);

        // If user doesn't exist, use the default settings for
        // #ignoreUknown.

        if (id == null) {
            return super.checkPassword(user, password);
        } else {
            String trusted = util.getUserPasswordHash(id);
            return comparePasswords(trusted, password);
        }
    }

    @Override
    public void changePassword(String user, String password)
            throws PasswordChangeException {
        Long id = util.userId(user);
        if (id == null) {
            throw new PasswordChangeException("Couldn't find id");
        }
        util.changeUserPasswordById(id, password);
    }

}
