/*
 *   $Id$
 *
 *   Copyright 2010 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.scripts;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ome.conditions.InternalException;
import ome.conditions.OptimisticLockException;
import ome.model.core.OriginalFile;
import ome.model.meta.ExperimenterGroup;
import ome.services.util.Executor;
import ome.system.Principal;
import ome.system.Roles;
import ome.system.ServiceFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.EmptyFileFilter;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.type.StringType;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcOperations;
import org.springframework.transaction.annotation.Transactional;

/**
 * Strategy used by the ScriptRepository for registering, loading, and saving
 * files.
 *
 * @since Beta4.2
 */
public class ScriptRepoHelper {

    /**
     * Id used by all script repositories. Having a well defined string allows
     * for various repositories to all provide the same functionality.
     */
    public final static String SCRIPT_REPO = "ScriptRepo";

    /**
     * {@link IOFileFilter} instance used during {@link #iterate()} to find the
     * matching scripts in the given directory.
     */
    public final static IOFileFilter SCRIPT_FILTER = new AndFileFilter(Arrays
            .asList(new FileFilter[] { EmptyFileFilter.NOT_EMPTY,
                    HiddenFileFilter.VISIBLE, CanReadFileFilter.CAN_READ,
                    new WildcardFileFilter("*.py") }));

    private final String uuid;

    private final File dir;

    private final Executor ex;

    private final Principal p;

    private final Roles roles;

    protected final Log log = LogFactory.getLog(getClass());

    /**
     * @see #ScriptRepoHelper(String, File, Executor, Principal)
     */
    public ScriptRepoHelper(Executor ex, String sessionUuid, Roles roles) {
        this(new File(getDefaultScriptDir()), ex, new Principal(sessionUuid),
                roles);
        loadAll(true);
    }

    /**
     * @see #ScriptRepoHelper(String, File, Executor, Principal)
     */
    public ScriptRepoHelper(File dir, Executor ex, Principal p, Roles roles) {
        this(SCRIPT_REPO, dir, ex, p, roles);
    }

    /**
     *
     * @param uuid
     *            Allows setting a non-default uuid for this script service.
     *            Primarily for testing, since services rely on the repository
     *            name for finding one another.
     * @param dir
     *            The directory used by the repo as its root. Other constructors
     *            use {@link #getDefaultScriptDir()} internally.
     * @param ex
     * @param p
     */
    public ScriptRepoHelper(String uuid, File dir, Executor ex, Principal p,
            Roles roles) {
        this.roles = roles;
        this.uuid = uuid;
        this.dir = dir;
        this.ex = ex;
        this.p = p;
        if (dir == null) {
            throw new InternalException("Null dir!");
        }
        if (!dir.exists()) {
            throw new InternalException("Does not exist: "
                    + dir.getAbsolutePath());
        }
        if (!dir.canRead()) {
            throw new InternalException("Cannot read: " + dir.getAbsolutePath());
        }
    }

    /**
     * Directory which will be used as the root of this repository if no
     * directory is passed to a constructor. Equivalent to "lib/scripts" from
     * the current directory.
     */
    public static String getDefaultScriptDir() {
        File current = new File(".");
        File lib = new File(current, "lib");
        File scripts = new File(lib, "scripts");
        return scripts.getAbsolutePath();
    }

    /**
     * Returns the actual root of this repository.
     *
     * @see #getDefaultScriptDir()
     */
    public String getScriptDir() {
        return dir.getAbsolutePath();
    }

    /**
     * Uuid of this repository. In the normal case, this will equal
     * {@link #SCRIPT_REPO}.
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Returns the number of files which match {@link #SCRIPT_FILTER} in
     * {@link #dir}. Uses {@link #iterate()} internally.
     */
    public int countOnDisk() {
        int size = 0;
        Iterator<File> it = iterate();
        while (it.hasNext()) {
            File f = it.next();
            if (f.canRead() && f.isFile() && !f.isHidden()) {
                size++;
            }
        }
        return size;
    }

    public int countInDb() {
        return (Integer) ex.executeStateless(new Executor.SimpleStatelessWork(
                this, "countInDb") {
            @Transactional(readOnly = true)
            public Object doWork(SimpleJdbcOperations jdbc) {
                return countInDb(jdbc);
            }
        });
    }

    public int countInDb(SimpleJdbcOperations jdbc) {
        return jdbc.queryForInt("select count(id) from originalfile "
                + "where repo = ? and mimetype = 'text/x-python", uuid);
    }

    @SuppressWarnings("unchecked")
    public List<Long> idsInDb() {
        return (List<Long>) ex
                .executeStateless(new Executor.SimpleStatelessWork(this,
                        "idsInDb") {
                    @Transactional(readOnly = true)
                    public Object doWork(SimpleJdbcOperations jdbc) {
                        return idsInDb(jdbc);
                    }
                });
    }

    public List<Long> idsInDb(SimpleJdbcOperations jdbc) {
        try {
            return (List<Long>) jdbc.query("select id from originalfile "
                    + "where repo = ? and mimetype = 'text/x-python'", new RowMapper<Long>() {
                public Long mapRow(ResultSet arg0, int arg1)
                        throws SQLException {
                    return arg0.getLong(1);
                }
            }, uuid);
        } catch (EmptyResultDataAccessException e) {
            return Collections.emptyList();
        }
    }

    public boolean isInRepo(final long id) {
        return (Boolean) ex.executeStateless(new Executor.SimpleStatelessWork(
                this, "isInRepo", id) {
            @Transactional(readOnly = true)
            public Object doWork(SimpleJdbcOperations jdbc) {
                return isInRepo(jdbc, id);
            }
        });
    }

    public boolean isInRepo(SimpleJdbcOperations jdbc, final long id) {
        try {
            int count = jdbc.queryForInt("select count(id) from originalfile "
                    + "where repo = ? and id = ? and mimetype = 'text/x-python'",
                    uuid, id);
            return count > 0;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }

    public Long findInDb(final String path, final boolean scriptsOnly) {
        RepoFile repoFile = new RepoFile(dir, path);
        return findInDb(repoFile, scriptsOnly);
    }

    public Long findInDb(final RepoFile file, final boolean scriptsOnly) {
        return (Long) ex.executeStateless(new Executor.SimpleStatelessWork(
                this, "findInDb", file, scriptsOnly) {
            @Transactional(readOnly = true)
            public Object doWork(SimpleJdbcOperations jdbc) {
                return findInDb(jdbc, file, scriptsOnly);
            }
        });
    }

    /**
     * Looks to see if a path is contained in the repository.
     */
    public Long findInDb(SimpleJdbcOperations jdbc, RepoFile repoFile, boolean scriptsOnly) {
        try {
            return jdbc.queryForLong("select id from originalfile "
                    + "where repo = ? and path = ? and name = ? " + scriptsOnly(scriptsOnly),
                    uuid, repoFile.dirname(), repoFile.basename());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Iterator<File> iterate() {
        return FileUtils.iterateFiles(dir, SCRIPT_FILTER, TrueFileFilter.TRUE);
    }

    /**
     * Walks all files in the repository (via {@link #iterate()} and adds them
     * if not found in the database.
     *
     * If modificationCheck is true, then a change in the sha1 for a file in
     * the repository will cause the old file to be removed from the repository
     * <pre>(uuid == null)</pre> and a new file created in its place.
     *
     * @param modificationCheck
     * @return
     */
    public List<OriginalFile> loadAll(boolean modificationCheck) {
        final Iterator<File> it = iterate();
        final List<OriginalFile> rv = new ArrayList<OriginalFile>();
        File f = null;
        RepoFile file = null;
        while (it.hasNext()) {
            f = it.next();
            file = new RepoFile(dir, f);
            Long id = findInDb(file, false); // non-scripts count
            String sha1 = null;
            OriginalFile ofile = null;
            if (id == null) {
                ofile = addOrReplace(file, null);
            } else {

                ofile = load(id, true); // checks for type & repo
                if (ofile == null) {
                    continue; // wrong type or similar
                }

                if (modificationCheck) {
                    sha1 = file.sha1();
                    if (!sha1.equals(ofile.getSha1())) {
                        ofile = addOrReplace(file, id);
                    }
                }
            }
            rv.add(ofile);
        }
        return rv;
    }

    public OriginalFile addOrReplace(final RepoFile repoFile, final Long old) {
        return (OriginalFile) ex.execute(p, new Executor.SimpleWork(this,
                "addOrReplace", repoFile, old) {
            @Transactional(readOnly = false)
            public Object doWork(Session session, ServiceFactory sf) {
                if (old != null) {
                    session.createSQLQuery(
                            "update originalfile set repo = ? where id = ?")
                            .setParameter(0, null, new StringType())
                            .setParameter(1, old).executeUpdate();
                }

                OriginalFile ofile = new OriginalFile();
                return update(repoFile, session, sf, ofile);
            }
        });
    }

    public OriginalFile update(final RepoFile repoFile, final Long id) {
        return (OriginalFile) ex.execute(p, new Executor.SimpleWork(this,
                "update", repoFile, id) {
            @Transactional(readOnly = false)
            public Object doWork(Session session, ServiceFactory sf) {
                OriginalFile ofile = load(id, session, true);
                return update(repoFile, session, sf, ofile);
            }
        });
    }

    private OriginalFile update(final RepoFile repoFile, Session session,
            ServiceFactory sf, OriginalFile ofile) {
        ofile.setPath(repoFile.dirname());
        ofile.setName(repoFile.basename());
        ofile.setSha1(repoFile.sha1());
        ofile.setSize(repoFile.length());
        ofile.setMimetype("text/x-python");
        ofile.getDetails().setGroup(
                new ExperimenterGroup(roles.getUserGroupId(), false));
        ofile = sf.getUpdateService().saveAndReturnObject(ofile);

        session.createSQLQuery(
                "update originalfile set repo = ? where id = ?")
                .setParameter(0, uuid).setParameter(1, ofile.getId())
                .executeUpdate();
        return ofile;
    }

    public String read(String path) throws IOException {
        final RepoFile repo = new RepoFile(dir, path);
        return FileUtils.readFileToString(repo.file());
    }

    public RepoFile write(String path, String text) throws IOException {
        RepoFile repo = new RepoFile(dir, path);
        return write(repo, text);
    }

    public RepoFile write(RepoFile repo, String text) throws IOException {
        FileUtils.writeStringToFile(repo.file(), text); // truncates itself. ticket:2337
        return repo;
    }

    public OriginalFile load(final long id, final boolean check) {
        return (OriginalFile) ex.execute(p, new Executor.SimpleWork(this,
                "load", id) {
            @Transactional(readOnly = true)
            public Object doWork(Session session, ServiceFactory sf) {
                return load(id, session, check);
            }
        });
    }

    public OriginalFile load(final long id, Session s, boolean check) {
        if (check) {
            String repo = (String) s.createSQLQuery(
                    "select repo from OriginalFile where id = ? " +
                    "and mimetype = 'text/x-python'")
                    .setParameter(0, id)
                    .uniqueResult();
            if (!uuid.equals(repo)) {
                return null;
            }
        }
        return (OriginalFile) s.get(OriginalFile.class, id);
    }

    /**
     * Checks if
     */
    public void modificationCheck() {
        loadAll(true);
    }

    public boolean delete(long id) {

        final OriginalFile file = load(id, true);
        if (file == null) {
            return false;
        }

        ex.execute(p, new Executor.SimpleWork(this, "delete", id) {
            @Transactional(readOnly = false)
            public Object doWork(Session session, ServiceFactory sf) {
                sf.getUpdateService().deleteObject(file);
                return null;
            }
        });

        FileUtils.deleteQuietly(new File(dir, file.getPath()));

        return true;
    }

    // Helpers
    // =========================================================================

    private String scriptsOnly(boolean scriptsOnly) {
        if (scriptsOnly) {
            return "and mimetype = 'text/x-python'";
        }
        return "";
    }

}
