/*
 *   $Id$
 *
 *   Copyright 2010 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ome.conditions.InternalException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcOperations;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Single wrapper for all JDBC activities.
 *
 * This interface is meant to replace <em>all</em> uses of both
 * {@link SimpleJdbcTemplate} and
 * {@link org.hibernate.Session#createSQLQuery(String)} for the entire OMERO
 * code base.
 *
 * @author Josh Moore, josh at glencoesoftware.com
 * @since 4.2.1
 * @see <a href="http://trac.openmicroscopy.org.uk/omero/ticket/73">#73</a>
 * @see <a href="http://trac.openmicroscopy.org.uk/omero/ticket/2684">#2684</a>
 */
public interface SqlAction {

    public static class IdRowMapper implements RowMapper<Long> {
        public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getLong(1);
        }
    }

    public static class LoggingSqlAction implements MethodInterceptor {

        final private static Log log = LogFactory.getLog(SqlAction.class);

        public Object invoke(MethodInvocation arg0) throws Throwable {
            log.info(String.format("%s.%s(%s)",
                    arg0.getThis(),
                    arg0.getMethod().getName(),
                    Arrays.deepToString(arg0.getArguments())));
            return arg0.proceed();
        }

    }

    /**
     * Returns true if the given string is the UUID of a session that is
     * currently active.
     *
     * @param sessionUUID
     *            NOT NULL.
     * @return
     */
    boolean activeSession(String sessionUUID);

    String fileRepo(long fileId);

    /**
     * Similar to {@link #fileRepo(long)}, but only returns values for files
     * which are also scripts.
     *
     * @param fileId
     * @return
     */
    String scriptRepo(long fileId);

    int synchronizeJobs(List<Long> ids);

    Long findRepoFile(String uuid, String dirname, String basename,
            String string);

    String findRepoFilePath(String uuid, long id);

    List<Long> findRepoPixels(String uuid, String dirname, String basename);

    Long findRepoImageFromPixels(long id);

    int repoScriptCount(String uuid);

    Long nextSessionId();

    List<Long> fileIdsInDb(String uuid);

    Map<String, Object> repoFile(long value);

    long countFormat(String name);

    int insertFormat(String name);

    int closeSessions(String uuid);

    int closeNodeSessions(String uuid);

    int closeNode(String uuid);

    long nodeId(String internal_uuid);

    int insertSession(Map<String, Object> params);

    Long sessionId(String uuid);

    int isFileInRepo(String uuid, long id);

    int removePassword(Long id);

    Date now();

    int updateConfiguration(String key, String value);

    String dbVersion();

    String configValue(String key);

    String dbUuid();

    long selectCurrentEventLog(String key);

    void setCurrentEventLog(long id, String key);

    void delCurrentEventLog(String key);

    long nextValue(String segmentName, int incrementSize);

    long currValue(String segmentName);

    void insertLogs(List<Object[]> batchData);

    List<Map<String, Object>> roiByImageAndNs(final long imageId,
            final String ns);

    List<Long> getShapeIds(long roiId);

    String dnForUser(Long id);

    List<Map<String, Object>> dnExperimenterMaps();

    void setUserDn(Long experimenterID, String dn);

    boolean setUserPassword(Long experimenterID, String password);

    String getPasswordHash(Long experimenterID);

    Long getUserId(String userName);

    List<String> getUserGroups(String userName);

    void setFileRepo(long id, String repoId);

    void setPixelsNamePathRepo(long pixId, String name, String path,
            String repoId);

    // TODO this should probably return an iterator.
    List<Long> getDeletedIds(String entityType);

    void createSavepoint(String savepoint);

    void releaseSavepoint(String savepoint);

    void rollbackSavepoint(String savepoint);

    void deferConstraints();

    //
    // Previously PgArrayHelper
    //

    /**
     * Returns only the (possibly empty) keys which are set on the given
     * original file. If the given original file cannot be found, null is
     * returned.
     */
    List<String> getPixelsParamKeys(long id) throws InternalException;

    /**
     * Loads all the (possibly empty) params for the given original file. If the
     * id is not found, null is returned.
     */
    Map<String, String> getPixelsParams(final long id) throws InternalException;

    /**
     * Resets the entire original file "params" field.
     */
    int setPixelsParams(final long id, Map<String, String> params);

    /**
     * Appends "{key, value}" onto the original file "params" field or replaces
     * the value if already present.
     */
    int setPixelsParam(final long id, final String key, final String value);

    /**
     * Returns only the (possibly empty) keys which are set on the given
     * original file. If the given original file cannot be found, null is
     * returned.
     */
    List<String> getFileParamKeys(long id) throws InternalException;

    /**
     * Loads all the (possibly empty) params for the given original file. If the
     * id is not found, null is returned.
     */
    Map<String, String> getFileParams(final long id) throws InternalException;

    /**
     * Resets the entire original file "params" field.
     */
    int setFileParams(final long id, Map<String, String> params);

    /**
     * Appends "{key, value}" onto the original file "params" field or replaces
     * the value if already present.
     */
    int setFileParam(final long id, final String key, final String value);

    Set<String> currentUserNames();

    int changeGroupPermissions(Long id, Long internal);

    int changeTablePermissionsForGroup(String table, Long id, Long internal);

    //
    // End PgArrayHelper
    //

    /**
     * Base implementation which can be used
     */
    public static abstract class Impl implements SqlAction {

        protected abstract SimpleJdbcOperations _jdbc();

        protected abstract String _lookup(String key);

        //
        // SECURITY
        //

        public int closeNodeSessions(String uuid) {
            return _jdbc().update(
                    _lookup("update_node_sessions"), uuid); //$NON-NLS-1$
        }

        public int closeNode(String uuid) {
            return _jdbc().update(
                    _lookup("update_node"), uuid); //$NON-NLS-1$
        }


        public boolean setUserPassword(Long experimenterID, String password) {
            int results = _jdbc().update(_lookup("update_password"), //$NON-NLS-1$
                    password, experimenterID);
            if (results < 1) {
                results = _jdbc().update(_lookup("insert_password"), //$NON-NLS-1$
                        experimenterID, password, null);
            }
            return results >= 1;
        }

        public int changeGroupPermissions(Long id, Long internal) {
            return _jdbc().update(_lookup("update_permissions_for_group"),
                    internal, id);
        }

        public int changeTablePermissionsForGroup(String table, Long id, Long internal) {
            String sql = _lookup("update_permissions_for_table");
            sql = String.format(sql, table);
            return _jdbc().update(sql, internal, id);
        }

        //
        // FILES
        //

        public String fileRepo(long fileId) {
            return _jdbc().queryForObject(
                    _lookup("file_repo"), String.class, //$NON-NLS-1$
                    fileId);
        }

        public String scriptRepo(long fileId) {
            return _jdbc().queryForObject(
                    _lookup("file_repo_of_script"), String.class, //$NON-NLS-1$
                    fileId);
        }

        //
        // CONFIGURATION
        //

        public long selectCurrentEventLog(String key) {
            String value = _jdbc().queryForObject(
                _lookup("log_loader_query"), String.class, key); //$NON-NLS-1$
            return Long.valueOf(value);
        }

        public void setCurrentEventLog(long id, String key) {

            int count = _jdbc().update(
                _lookup("log_loader_update"), Long.toString(id), //$NON-NLS-1$
                key);

            if (count == 0) {
                _jdbc().update(
                        _lookup("log_loader_insert"),  //$NON-NLS-1$
                        key, Long.toString(id));
            }
        }

        public void delCurrentEventLog(String key) {
            _jdbc().update(
                _lookup("log_loader_delete"), key); //$NON-NLS-1$

        }
    }

}
