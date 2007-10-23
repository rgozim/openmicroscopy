/*
 *   $Id$
 *
 *   Copyright 2007 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.api;

import java.sql.Timestamp;

import ome.conditions.ApiUsageException;
import ome.model.jobs.Job;
import ome.model.jobs.JobStatus;

/**
 * Allows submission of asynchronous jobs.
 * <p>
 * NOTE: The calling order for the service is as follows:
 * <ol>
 * <li>submit({@link Job}) <em>or</em> attach(long)</li>
 * <li>any of the other methods</li>
 * <li>close()</li>
 * </ol>
 * </p>
 * 
 * @author Josh Moore, josh at glencoesoftware.com
 * @since 3.0-Beta2
 */
public interface JobHandle extends StatefulServiceInterface {

    public final static String SUBMITTED = "Submitted";
    public final static String RESUBMITTED = "Resubmitted";
    public final static String QUEUED = "Queued";
    public final static String REQUEUED = "Requeued";
    public final static String RUNNING = "Running";
    public final static String ERROR = "Error";
    public final static String WAITING = "Waiting";
    public final static String FINISHED = "Finished";
    public final static String CANCELLED = "Cancelled";

    long submit(Job job);

    /**
     * @return the current {@link JobStatus} for the {@link Job id}
     * @throws ApiUsageException
     *             if the {@link Job id} does not exist.
     */
    JobStatus attach(long jobId) throws ApiUsageException;

    /**
     * @return the current {@link Job}
     */
    Job getJob();

    /**
     * @return the current {@link JobStatus}. Will never return null.
     */
    JobStatus jobStatus();

    /**
     * @return null if the {@link Job} is not finished, otherwise the
     *         {@link Timestamp} for when it completed.
     */
    Timestamp jobFinished();

    /**
     * @return current message for job. May be set during processing.
     */
    String jobMessage();

    /**
     * Returns true if the {@link Job} is running, i.e. has an attached
     * {@link Process}.
     */
    boolean jobRunning();

    /**
     * Returns true if the {@link Job} has thrown an error.
     */
    boolean jobError();

    /**
     * Marks a job for cancellation. Not every processor will check for the
     * cancelled flag for a running job, but no non-running job will start if it
     * has been cancelled.
     */
    void cancelJob();

    /**
     * Free the resources for this {@link JobHandle handle}. This
     * <em>does not</em> cancel or otherwise alter the {@link Job}.
     */
    void close();

}
