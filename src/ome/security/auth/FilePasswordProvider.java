/*
 *   $Id$
 *
 *   Copyright 2009 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.security.auth;

import java.io.File;
import java.io.FileInputStream;
import java.security.Permissions;
import java.util.Properties;

import ome.security.SecuritySystem;

import org.springframework.util.Assert;

/**
 * Example password provider which uses the given file as password lookup. All
 * entries in the file are of the form: username=password, where password is in
 * whatever encoding is configured for the {@link PasswordProvider provider}.
 * Changing passwords is not supported.
 * 
 * @author Josh Moore, josh at glencoesoftware.com
 * @since 4.0
 */
public class FilePasswordProvider extends ConfigurablePasswordProvider {

    /**
     * Flat file read on each invocation with name, value pairs in Java
     * {@link java.util.Properties} notation.
     */
    final protected File file;

    public FilePasswordProvider(File file) {
        super();
        this.file = file;
        Assert.notNull(file);
    }

    public FilePasswordProvider(File file, boolean ignoreUnknown) {
        super(ignoreUnknown);
        this.file = file;
        Assert.notNull(file);
    }

    @Override
    public boolean hasPassword(String user) {
        Properties p = getProperties();
        return p.containsKey(user);
    }

    @Override
    public Boolean checkPassword(String user, String password) {
        Properties p = getProperties();
        return doCheckPassword(user, password, p);
    }

    protected Boolean doCheckPassword(String user, String password, Properties p) {
        if (!p.containsKey(user)) {
            // Do the default on unknown
            return super.checkPassword(user, password);
        } else {
            String currentPassword = p.getProperty(user);
            return comparePasswords(currentPassword, password);
        }
    }

    protected Properties getProperties() {
        Properties p = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            p.load(fis);
            return p;
        } catch (Exception e) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e2) {
                    log.error("Ignoring exception on fis.close()", e2);
                }
            }
            throw new RuntimeException("Could not read file: " + file);
        }

    }

}
