package org.purl.wf4ever.wf2ro.rest;

import java.net.URI;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.purl.wf4ever.wf2ro.rest.Job.Status;

/**
 * Job status as JSON
 * 
 * @author piotrekhol
 * 
 */
@XmlRootElement
public class JobStatus
{

	private URI resource;

	private URI format;

	private URI ro;

	private Status status;

	private List<URI> added;


	public JobStatus()
	{

	}


	public JobStatus(URI resource, URI format, URI ro, Status status, List<URI> added)
	{
		super();
		this.resource = resource;
		this.format = format;
		this.ro = ro;
		this.status = status;
		this.added = added;
	}


	public URI getResource()
	{
		return resource;
	}


	public void setResource(URI resource)
	{
		this.resource = resource;
	}


	public URI getFormat()
	{
		return format;
	}


	public void setFormat(URI format)
	{
		this.format = format;
	}


	public URI getRo()
	{
		return ro;
	}


	public void setRo(URI ro)
	{
		this.ro = ro;
	}


	public Status getStatus()
	{
		return status;
	}


	public void setStatus(Status status)
	{
		this.status = status;
	}


	public List<URI> getAdded()
	{
		return added;
	}


	public void setAdded(List<URI> added)
	{
		this.added = added;
	}

}
