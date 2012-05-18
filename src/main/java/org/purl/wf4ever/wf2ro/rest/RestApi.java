package org.purl.wf4ever.wf2ro.rest;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.multipart.FormDataParam;

@Path("jobs")
public class RestApi
	implements JobsContainer
{

	/**
	 * Maximum number of concurrent jobs.
	 */
	public static final int MAX_JOBS = 100;

	@Context
	private HttpServletRequest request;

	@Context
	private UriInfo uriInfo;

	private static Map<UUID, Job> jobs = new ConcurrentHashMap<>(MAX_JOBS);


	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response createJob(@FormDataParam("resource")
	URI resourceURI, @FormDataParam("format")
	URI formatURI, @FormDataParam("ro")
	URI roURI, @FormDataParam("token")
	String token)
	{
		if (jobs.size() >= MAX_JOBS) {
			return Response.status(Status.SERVICE_UNAVAILABLE).build();
		}
		UUID jobUUID = UUID.randomUUID();
		URI jobURI = uriInfo.getAbsolutePathBuilder().path(jobUUID.toString()).build();
		Job job = new Job(uriInfo.getBaseUri(), jobUUID, resourceURI, formatURI, roURI, token, this);
		jobs.put(jobUUID, job);
		job.start();
		return Response.created(jobURI).build();
	}


	@GET
	@Path("/{uuid}")
	@Produces(MediaType.APPLICATION_JSON)
	public JobStatus getJobStatus(@PathParam("uuid")
	UUID uuid)
		throws NotFoundException
	{
		if (!jobs.containsKey(uuid)) {
			throw new NotFoundException(uuid);
		}
		return jobs.get(uuid).getJobStatus();
	}


	@Override
	public void onJobDeleted(Job job)
	{
		jobs.remove(job.getUUID());
	}
}
