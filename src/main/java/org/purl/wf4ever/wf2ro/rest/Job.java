/**
 * 
 */
package org.purl.wf4ever.wf2ro.rest;

import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.purl.wf4ever.wf2ro.RodlConverter;
import org.scribe.model.Token;

/**
 * @author piotrekhol
 * 
 */
public class Job
	extends Thread
{

	private static final Logger log = Logger.getLogger(Job.class);

	public enum Status {
		RUNNING, DONE, CANCELLED
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
		//TODO run

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


	public void cancel()
	{
		//FIXME not sure if that's how we want to cancel this thread
		this.interrupt();
		this.status = Status.CANCELLED;
	}

}
