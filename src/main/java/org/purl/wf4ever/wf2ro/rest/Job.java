package org.purl.wf4ever.wf2ro.rest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.purl.wf4ever.wf2ro.RodlConverter;
import org.scribe.model.Token;

import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.io.ReaderException;
import uk.org.taverna.scufl2.api.io.WorkflowBundleIO;

/**
 * Represents a conversion job. It runs in a separate thread.
 * 
 * @author piotrekhol
 * 
 */
public class Job
	extends Thread
{

	private static final Logger log = Logger.getLogger(Job.class);

	public enum Status {
		RUNNING, DONE, CANCELLED, INVALID_RESOURCE, RUNTIME_ERROR;

		public String toString()
		{
			return this.toString().toLowerCase();
		};
	}

	private static final long EXPIRATION_PERIOD = 600 * 1000;

	private UUID uuid;

	private Status status;

	private URI resource;

	private URI format;

	private URI ro;

	private Token token;

	private JobsContainer container;

	private RodlConverter converter;


	/**
	 * 
	 * @param service
	 *            Service URI for the converter
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
	public Job(URI service, UUID jobUUID, URI resource, URI format, URI ro, String token, JobsContainer container)
	{
		this.uuid = jobUUID;
		this.resource = resource;
		this.format = format;
		this.ro = ro;
		this.token = new Token(token, null);
		this.container = container;
		status = Status.RUNNING;

		this.converter = new RodlConverter(service, ro, this.token);

		setDaemon(true);
	}


	@Override
	public void run()
	{
		WorkflowBundleIO io = new WorkflowBundleIO();
		try {
			WorkflowBundle wfbundle = io.readBundle(resource.toURL(), format.toString());
			converter.convert(wfbundle);
		}
		catch (ReaderException | IOException e) {
			log.error("Can't download the resource", e);
			status = Status.INVALID_RESOURCE;
		}
		catch (Exception e) {
			log.error("Unexpected exception during conversion", e);
			status = Status.RUNTIME_ERROR;
		}

		status = Status.DONE;
		try {
			sleep(EXPIRATION_PERIOD);
		}
		catch (InterruptedException e) {
			log.error("Wait interrupted", e);
		}
		container.onJobDeleted(this);
	}


	public UUID getUUID()
	{
		return uuid;
	}


	public Status getStatus()
	{
		return status;
	}


	public JobStatus getJobStatus()
	{
		return new JobStatus(resource, format, ro, status, converter.getResourcesAdded());
	}


	/**
	 * Cancel the job. The job is aborted but not undone.
	 */
	public void cancel()
	{
		//FIXME not sure if that's how we want to cancel this thread
		this.interrupt();
		this.status = Status.CANCELLED;
	}

}
