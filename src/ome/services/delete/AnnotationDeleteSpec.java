/*
 *   $Id$
 *
 *   Copyright 2010 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.delete;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ome.api.IDelete;
import ome.model.annotations.Annotation;
import ome.security.basic.CurrentDetails;
import ome.system.EventContext;
import ome.tools.hibernate.ExtendedMetadata;
import ome.tools.hibernate.QueryBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.beans.FatalBeanException;

/**
 * {@link DeleteSpec} specialized for deleting annotations. Adds options which
 * classes to delete as well as which namespaces.
 *
 *
 * @author Josh Moore, josh at glencoesoftware.com
 * @since Beta4.2.1
 * @see IDelete
 */
public class AnnotationDeleteSpec extends BaseDeleteSpec {

    private final static Log log = LogFactory
            .getLog(AnnotationDeleteSpec.class);

    /**
     * Collection of namespace values (or LIKE values if they contain '%' or
     * '?') which will be deleted automatically.
     */
    public static final Set<String> nsIncludes = Collections
            .unmodifiableSet(new HashSet<String>(Arrays.asList("foo")));

    /**
     * Collection of namespace values (defined as {@link #nsIncludes}) which
     * will be omitted from deletes by default.
     */
    public static final Set<String> nsExcludes = Collections
            .unmodifiableSet(new HashSet<String>(Arrays.asList("foo")));

    /**
     * Whether or not the type of types[i] is abstract. If true, no deletion
     * will take place, since otherwise duplicate multiple deletions would be
     * attempted for each id.
     */
    private final boolean[] isAbstract;

    /**
     * Type of each sub-step. Annotation.class.isAssignableFrom(types[i]) is
     * <code>true</code>.
     */
    private final Class<?>[] types;

    /**
     * Original string contained in the options {@link Map} before parsing. The
     * value may come from one of several sources, e.g. for a FileAnnotation the
     * keys checked are:
     * <pre>
     *     /abspath/FileAnnotation
     *     /FileAnnotation
     *     /abspath/TypeAnnotation
     *     /TypeAnnotation
     *     /abspath/Annotation
     *     Gq/Annotation
     * </pre>
     */
    private final String[] rawOption;

    /**
     * Key from the options map which was the source of the rawOption value of
     * the same index. I.e. if rawOption[2] contains "KEEP", then optSource[2]
     * should point to an entry in options which equals "KEEP".
     */
    private final String[] optSource;

    /**
     * exclude values parsed out from the user options.
     */
    private final String[] excludes;

    /**
     * Default excludes values as determined at Spring configuration time. These
     * are assigned if none are passed via the options during initialize.
     */
    private final Map<String, String> excludeMap = new HashMap<String, String>();

    //
    // Initialization-time values
    //

    /**
     * Creates a new instance.
     *
     * @param entries
     *            The entries to handle.
     */
    public AnnotationDeleteSpec(List<String> entries) {
        this(null, entries);
    }

    /**
     * Creates a new instance.
     *
     * @param excludeMap
     *            The namespaces to exclude.
     * @param entries
     *            The entries to handle.
     */
    public AnnotationDeleteSpec(Map<String, String> excludeMap,
            List<String> entries) {
        super(entries);
        isAbstract = new boolean[entries.size()];
        types = new Class[entries.size()];
        excludes = new String[entries.size()];
        rawOption = new String[entries.size()];
        optSource = new String[entries.size()];
        if (excludeMap != null) {
            this.excludeMap.putAll(excludeMap);
        }
    }

    /**
     * Returns the value from "...;excludes=VALUEHERE" parsed out of the options
     * map during {@link #initialize(long, String, Map<String, String>)}.
     *
     * Used primarily for testing.
     */
    public String getExclude(int step) {
        return excludes[step];
    }

    /**
     * Performs sanity checks on the annotation entries found in {@link ExtendedMetadata}.
     * Primarily, this prevents new annotation types from not being properly specified
     * in spec.xml.
     */
    @Override
    public void setExtendedMetadata(ExtendedMetadata em) {
        super.setExtendedMetadata(em);

        // First calculate the number of unique top-level paths
        List<String> uniquePaths = new ArrayList<String>();
        for (DeleteEntry entry : entries) {
            String topLevel = entry.path("")[0];
            if (!uniquePaths.contains(topLevel)) {
                uniquePaths.add(topLevel);
            }
        }

        // Now we check if this represents all the annotation types
        // in the system.
        Set<Class<Annotation>> types = em.getAnnotationTypes();
        if (types.size() != uniquePaths.size()) {
            throw new FatalBeanException(
                    "Mismatch between anntotations defined and those found: "
                            + entries + "<> " + em.getAnnotationTypes());
        }

        TYPE: for (Class<Annotation> type : types) {
            String simpleName = type.getSimpleName();
            for (int i = 0; i < entries.size(); i++) {
                DeleteEntry entry = entries.get(i);
                if (entry.path("").length > 1) {
                    // This not an annotation, but some subpath
                    // ignore it.
                    continue;
                }
                if (simpleName.equals(entry.getName().substring(1))) {
                    this.types[i] = type;
                    if (Modifier.isAbstract(type.getModifiers())) {
                        this.isAbstract[i] = true;
                    }
                    continue TYPE;
                }
            }
            throw new FatalBeanException("Could not find entry: " + simpleName);
        }
    }

    @Override
    public int initialize(long id, String superspec, Map<String, String> dontmodify)
        throws DeleteException {

        Map<String, String> options = null;
        if (dontmodify != null) {
            options = new HashMap<String, String>();
            options.putAll(dontmodify);
        }

        // Before the calculation of ops and other initialization events
        // happen, we apply all the options which match annotation superclasses,
        // to the related subclasses and strip off any annotation-specific
        // clauses, e.g. KEEP;excludes=%companionFile will be passed as "KEEP"
        if (options != null) {
            for (int i = 0; i < types.length; i++) {
                DeleteEntry entry = entries.get(i);
                Class<?> type = types[i];

                // If the type is null, then this is not an annotation but
                // but object under an annotation,like
                // /FileAnnotation/OriginalFile.
                if (type == null) {
                    continue;
                }

                String simpleName = type.getSimpleName();
                String last = "/" + simpleName;
                String[] path = entry.path(superspec);
                String absolute = "/" + StringUtils.join(path, "/");

                if (options.containsKey(absolute)) {
                    rawOption[i] = options.get(absolute);
                    optSource[i] = absolute;

                } else if (options.containsKey(last)) {
                    rawOption[i] = options.get(last);
                    optSource[i] = last;
                }

                // Here we make sure that an absolute will be set for the source
                // when replacing values in the options map below.
                if (optSource[i] == null) {
                    optSource[i] = absolute;
                }
            }

            // Here we go from the bottom (most subclass) to the top of the
            // hierarchy, taking the first non-null value we find from a
            // superclass
            for (int i = 0; i < types.length; i++) {
                Class<?> type = types[i];

                if (type == null) {
                    continue;
                }


                NULLCHECK: while (rawOption[i] == null
                        && !Annotation.class.equals(type)) {
                    type = type.getSuperclass();
                    for (int j = 0; j < types.length; j++) {
                        if (type.equals(types[j])) {
                            rawOption[i] = rawOption[j]; // May also be null
                            continue NULLCHECK;
                        }
                    }
                    throw new FatalBeanException("Couldn't find supertype: "
                            + type);
                }

            }

            // Now that we've parsed out the raw options based on the
            // annotation hierarchy, we break off the first item and if not
            // empty, that becomes the Op which is passed onto
            // super.initialize
            for (int i = 0; i < rawOption.length; i++) {
                String raw = rawOption[i];
                if (raw != null) {
                    String[] parts = raw.split(";");
                    if (parts[0].length() > 0) {
                        options.put(optSource[i], parts[0]);
                    } else {
                        options.remove(optSource[i]);
                    }
                    for (int j = 1; j < parts.length; j++) {
                        if (parts[j].startsWith("excludes")) {
                            if (!"KEEP".equals(parts[0])) {
                                // Currently only supporting KEEP;excludes=
                                // see queryBackupIds if this changes.
                                throw new FatalBeanException("Currently excludes only compatible with KEEP:" + raw);
                            }
                            int idx = parts[j].indexOf("=");
                            excludes[i] = parts[j].substring(idx + 1);
                        }
                    }
                }
            }

            for (int i = 0; i < excludes.length; i++) {
                if (types[i] != null) {
                    String defaultExclude = excludeMap.get("/" + types[i].getSimpleName());
                    if (excludes[i] == null && defaultExclude != null) {
                        excludes[i] = defaultExclude;
                    }
                }
            }

        }

        // Then all the options into structures for
        //

        int steps = super.initialize(id, superspec, options);
        return steps;
    }
    
    @Override
    protected List<Long> queryBackupIds(Session session, int step, DeleteEntry subpath,
            QueryBuilder and) throws DeleteException {

        if (and != null) {
            throw new DeleteException(true, "Unexpected non-null and: " + and);
        }

        // Copying the entry since we cannot currently find relationships
        // to subclasses, i.e. getRelationship(ImageAnnotationLink,
        // LongAnnotation)==null.
        final DeleteEntry dontuseentry = entries.get(step);
        final String[] dontuse = dontuseentry.path("");
        final String klass = dontuse[0];
        dontuse[0] = "/Annotation"; // Reset the value.
        final String newpath = StringUtils.join(dontuse, "/");
        final DeleteEntry copy = new DeleteEntry(this, newpath, subpath);

        final String[] sub = copy.path(superspec);
        final int which = sub.length - dontuse.length;
        final String alias = "ROOT" + which;

        and = new QueryBuilder();
        and.skipFrom();
        and.skipWhere();
        and.append(alias + ".class = " + klass);

        // If we have excludes and this is a KEEP, we only want
        // to query for those annotations which match the excludes value.
        if (DeleteEntry.Op.KEEP.equals(copy.getOp())) {
            String exclude = excludes[step];
            if (excludes[step] != null && excludes[step].length() > 0) {
                final String[] parts = exclude.split(",");
                and.appendSpace();
                and.and(alias + ".ns in (:excludes)");
                and.paramList("excludes", Arrays.asList(parts));
                log.debug("Exclude statement: " + and.toString());
            } else {
                // If there is no excludes, then none of this type should
                // be loaded.
                return Arrays.asList(); // EARLY EXIT!
            }
        }
        return super.queryBackupIds(session, step, copy, and);
    }

    /**
     * Returns true to prevent skipping of the entire subspec if there were any
     * "excludes" passed in with the options. These namespaces must be deleted
     * anyway.
     */
    @Override
    public boolean overrideKeep() {
        for (int step = 0; step < excludes.length; step++) {
            if (excludes[step] != null && excludes[step].length() > 0) {
                // If we have an exclude, we have to perform the delete anyway
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String delete(Session session, int step, DeleteIds deleteIds)
            throws DeleteException {

        if (isAbstract[step]) {
            if (log.isDebugEnabled()) {
                log.debug("Step " + step + " is abstract.");
            }
            return "";
        }
        
        // If this is a top-level annotation delete then the first thing we
        // do is delete all the links to it.
        if (superspec == null || superspec.length() == 0) {
            List<Long> foundIds = deleteIds.getFoundIds(this, step);
            if (foundIds != null && foundIds.size() > 0) {
                QueryBuilder qb = new QueryBuilder();
                qb.delete("ome.model.IAnnotationLink");
                qb.where();
                qb.and("child.id in (:ids)");
                qb.paramList("ids", foundIds);
                // ticket:2962
                DeleteEntry.permissionsClause(getCurrentDetails(), qb);
                
                Query q = qb.query(session);
                int count = q.executeUpdate();
                log.info("Deleted " + count + " annotation links");
            }
        }

        return super.delete(session, step, deleteIds);

    }

}
