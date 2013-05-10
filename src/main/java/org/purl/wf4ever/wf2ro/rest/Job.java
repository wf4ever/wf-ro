package org.purl.wf4ever.wf2ro.rest;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.purl.wf4ever.rosrs.client.exception.ROSRSException;
import org.purl.wf4ever.wf2ro.RodlConverter;

import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.io.ReaderException;
import uk.org.taverna.scufl2.api.io.WorkflowBundleIO;

/**
 * Represents a conversion job. It runs in a separate thread.
 * 
 * @author piotrekhol
 */
public class Job extends Thread {

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(Job.class);


    /**
     * The job state.
     * 
     * @author piotrekhol
     * 
     */
    public enum State {
        /** The job has started and is running. */
        RUNNING,
        /** The job has finished succesfully. */
        DONE,
        /** The job has been cancelled by the user. */
        CANCELLED,
        /** The resource to be formated is invalid. */
        INVALID_RESOURCE,
        /** There has been an unexpected error during conversion. */
        RUNTIME_ERROR;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        };
    }


    /** Job UUID. */
    private UUID uuid;

    /** Job state. */
    private State state;

    /** Workflow URI. */
    private URI resource;

    /** Workflow format MIME type. */
    private String format;

    /** RO URI. */
    private URI ro;


    public String getReason() {
        return reason;
    }


    /** RODL access token. */
    private String token;

    /** Object holding reference to the job. */
    private JobsContainer container;

    /** The converter. */
    private RodlConverter converter;

    /** Reason for the state, i.e. exception message. */
    private String reason;


    /**
     * Constructor.
     * 
     * @param jobUUID
     *            job identifier assigned by its container
     * @param resource
     *            URI of the workflow to be converted
     * @param format
     *            URI of workflow format
     * @param ro
     *            RO URI, for the converter
     * @param token
     *            RODL access token
     * @param container
     *            the object that created this job
     */
    public Job(UUID jobUUID, URI resource, String format, URI ro, String token, JobsContainer container) {
        this.uuid = jobUUID;
        this.resource = resource;
        this.format = format;
        this.ro = ro;
        this.token = token;
        this.container = container;
        state = State.RUNNING;

        LOG.debug(String.format("Created a new job:\n\tuuid = %s\n\tresource = %s\n\tformat = %s\n\tro=%s\t\n",
            jobUUID, resource, format, ro));

        setDaemon(true);
    }


    @Override
    public void run() {
        WorkflowBundleIO io = new WorkflowBundleIO();
        try {
            WorkflowBundle wfbundle = io.readBundle(resource.toURL(), format.toString());
            converter = new RodlConverter(wfbundle, resource, ro, this.token);
            converter.convert();
            state = State.DONE;
        } catch (ReaderException | IOException e) {
            LOG.error("Can't download the workflow", e);
            state = State.INVALID_RESOURCE;
            reason = "Can't download the workflow: " + e.getMessage();
        } catch (ROSRSException e) {
            LOG.error("ROSRS exception", e);
            state = State.RUNTIME_ERROR;
            reason = "ROSRS exception: " + e.getMessage();
        } catch (Throwable e) {
            LOG.error("Unexpected exception during conversion", e);
            state = State.RUNTIME_ERROR;
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            reason = errors.toString();
        }

        container.onJobDone(this);
    }


    public UUID getUUID() {
        return uuid;
    }


    public State getJobState() {
        return state;
    }


    public JobStatus getJobStatus() {
        return new JobStatus(resource, format, ro, state, converter != null ? converter.getResourcesAdded() : null,
                reason);
    }


    /**
     * Cancel the job. The job is aborted but not undone.
     */
    public void cancel() {
        //FIXME not sure if that's how we want to cancel this thread
        this.interrupt();
        this.state = State.CANCELLED;
    }

}
