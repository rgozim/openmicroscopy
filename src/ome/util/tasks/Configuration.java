/*
 * ome.util.tasks.Configuration
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.util.tasks;

// Java imports
import java.lang.reflect.Constructor;
import java.util.Properties;

// Third-party libraries

// Application-internal dependencies
import ome.annotations.RevisionDate;
import ome.annotations.RevisionNumber;
import ome.system.Login;
import ome.system.Server;
import ome.system.ServiceFactory;

import static ome.util.tasks.Configuration.Keys.*;

/**
 * Helper for creating any task from String properties, and can even instantiate
 * {@link ServiceFactory} and {@link Task} objects if given the proper
 * arguments.
 * 
 * Understands the parameters:
 * <ul>
 * <li>task</li>
 * <li>omero.user (from etc/local.properties)</li>
 * <li>omero.group (from etc/local.properties)</li>
 * <li>omero.type (from etc/local.properties)</li>
 * <li>omero.pass (from etc/local.properties)</li>
 * <li>server.host (from etc/local.properties)</li>
 * <li>server.port (from etc/local.properties)</li>
 * </ul>
 * 
 * To login as a root, for example, {@link Properties} of the form: <code>
 * {task=mytask, user=root, group=system, type=Task, pass=SECRET}
 * </code>
 * 
 * @author Josh Moore, josh.moore at gmx.de
 * @version $Revision$, $Date$
 * @see Configuration
 * @see Task
 * @since 3.0-Beta1
 */
@RevisionDate("$Date$")
@RevisionNumber("$Revision$")
public class Configuration {

    /** Default package in which {@link Task} lookups will be performed. */
    public final static String DEFAULTPKG = "ome.util.tasks";

    /**
     * Enumeration of the string values which will be used directly by
     * {@link Configuration}.
     */
    public enum Keys {
        task
    }

    final Properties properties;

    Class<Task> taskClass;

    /**
     * Sole constructor. Performs the necessary parsing of the
     * {@link Properties} values.
     * 
     * @param props
     *            Not null.
     */
    public Configuration(Properties props) {

        if (props == null) {
            throw new IllegalArgumentException("Argument cannot be null.");
        }

        this.properties = props;

        if (p(task) == null) {
            throw new IllegalArgumentException("task must be provided.");
        }

        taskClass = parseTask(p(task), "");
        if (taskClass == null) {
            taskClass = parseTask(p(task), DEFAULTPKG);
        }
        if (taskClass == null) {
            throw new IllegalArgumentException("Cannot find task class for:"
                    + p(task));
        }

    }

    /**
     * Returns the Properties instance provided during constuction.
     */
    public Properties getProperties() {
        return properties; // TODO should copy?
    }

    /**
     * Returns the {@link Class} found in the {@link Properties}
     */
    public Class<Task> getTaskClass() {
        return taskClass;
    }

    /**
     * Creates a {@link ServiceFactory} instance based on the values of
     * {@link #getServer()} and {@link #getLogin()} (such that a subclass could
     * override these methods to influence the ServiceFactory).
     * 
     * @return a non-null {@link ServiceFactory} instance.
     */
    public ServiceFactory createServiceFactory() {
        return new ServiceFactory(getProperties());
    }

    /**
     * Creates a new {@link Task} based on the values of
     * {@link #getProperties()}, {@link #getTaskClass()}, and
     * {@link #createServiceFactory()}.
     * 
     * @return a non-null {@link Task} instance, ready for execution.
     * @throws RuntimeException
     *             if anything happens during the reflection-based creation of
     *             the {@link Task}
     */
    public Task createTask() {
        Constructor<Task> ctor;
        try {
            ctor = getTaskClass().getConstructor(ServiceFactory.class,
                    Properties.class);
            Task newTask = ctor.newInstance(createServiceFactory(),
                    getProperties());
            return newTask;
        } catch (Exception e) {
            if (RuntimeException.class.isAssignableFrom(e.getClass())) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    // ~ Helpers
    // =========================================================================

    /**
     * Returns the property for the {@link Keys#toString()} value of the
     * argument.
     */
    protected String p(Keys key) {
        return properties.getProperty(key.toString());
    }

    /**
     * Adds the package to the task name, and returns the class if found.
     * Otherwise, null.
     */
    protected Class<Task> parseTask(String task, String pkg) {
        StringBuilder fqn = new StringBuilder(64);
        if (pkg != null && pkg.length() > 0 ) {
            fqn.append(pkg);
            fqn.append(".");
        }
        fqn.append(task);
        try {
            return (Class<Task>) Class.forName(fqn.toString());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
