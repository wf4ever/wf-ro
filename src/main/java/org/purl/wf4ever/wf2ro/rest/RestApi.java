package org.purl.wf4ever.wf2ro.rest;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
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

import org.purl.wf4ever.wf2ro.rest.Job.State;

import com.sun.jersey.multipart.FormDataParam;

/**
 * REST API as defined in http://www.wf4ever-project.org/wiki/display/docs/Wf-RO+transformation+service.
 * 
 * @author piotrekhol
 * 
 */
@Path("jobs")
public class RestApi implements JobsContainer {

    /** Maximum number of concurrent jobs. */
    public static final int MAX_JOBS = 100;

    /** Maximum number of finished jobs kept in memory. */
    public static final int MAX_JOBS_DONE = 100000;

    /** Context. */
    @Context
    private HttpServletRequest request;

    /** URI info. */
    @Context
    private UriInfo uriInfo;

    /** Running jobs. */
    private static Map<UUID, Job> jobs = new ConcurrentHashMap<>(MAX_JOBS);

    /** Statuses of finished jobs. */
    @SuppressWarnings("serial")
    private static Map<UUID, JobStatus> finishedJobs = Collections
            .synchronizedMap(new LinkedHashMap<UUID, JobStatus>() {

                protected boolean removeEldestEntry(Map.Entry<UUID, JobStatus> eldest) {
                    return size() > MAX_JOBS_DONE;
                };
            });


    /**
     * Create a new job.
     * 
     * @param resourceURI
     *            workflow URI
     * @param formatURI
     *            workflow format URI
     * @param roURI
     *            RO URI
     * @param token
     *            RODL access token
     * @return 201 Created
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response createJobMultipartForm(@FormDataParam("resource") URI resourceURI,
            @FormDataParam("format") URI formatURI, @FormDataParam("ro") URI roURI, @FormDataParam("token") String token) {
        return createJob(resourceURI, formatURI, roURI, token);
    }


    /**
     * Create a new job.
     * 
     * @param resourceURI
     *            workflow URI
     * @param formatURI
     *            workflow format URI
     * @param roURI
     *            RO URI
     * @param token
     *            RODL access token
     * @return 201 Created
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createJobForm(@FormParam("resource") URI resourceURI, @FormParam("format") URI formatURI,
            @FormParam("ro") URI roURI, @FormParam("token") String token) {
        return createJob(resourceURI, formatURI, roURI, token);
    }


    /**
     * Create a new job.
     * 
     * @param config
     *            JSON with config params
     * 
     * @return 201 Created
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createJobJson(JobConfig config) {
        return createJob(config.getResource(), config.getFormat(), config.getRo(), config.getToken());
    }


    /**
     * Create a new job.
     * 
     * @param resourceURI
     *            workflow URI
     * @param formatURI
     *            workflow format URI
     * @param roURI
     *            RO URI
     * @param token
     *            RODL access token
     * @return 201 Created
     */
    private Response createJob(URI resourceURI, URI formatURI, URI roURI, String token) {
        if (jobs.size() >= MAX_JOBS) {
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        }
        UUID jobUUID = UUID.randomUUID();
        URI jobURI = uriInfo.getAbsolutePathBuilder().path(jobUUID.toString()).build();
        Job job = new Job(jobUUID, resourceURI, formatURI, roURI, token, this);
        jobs.put(jobUUID, job);
        job.start();
        return Response.created(jobURI).build();
    }


    /**
     * Get job status.
     * 
     * @param uuid
     *            job UUID
     * @return JSON with job status
     * @throws NotFoundException
     *             No job with given UUID
     * @throws CancelledException
     *             The job has already been cancelled
     */
    @GET
    @Path("/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public JobStatus getJobStatus(@PathParam("uuid") UUID uuid)
            throws NotFoundException, CancelledException {
        if (jobs.containsKey(uuid)) {
            if (jobs.get(uuid).getJobState() == State.CANCELLED) {
                throw new CancelledException(uuid);
            } else {
                return jobs.get(uuid).getJobStatus();
            }
        }
        if (finishedJobs.containsKey(uuid)) {
            return finishedJobs.get(uuid);
        }
        throw new NotFoundException(uuid);
    }


    /**
     * Cancel a job. Not sure if it's an abort or an undo.
     * 
     * @param uuid
     *            job UUID
     * @throws NotFoundException
     *             No job with given UUID
     * @throws CancelledException
     *             The job has already been cancelled
     */
    @DELETE
    @Path("/{uuid}")
    public void cancelJob(@PathParam("uuid") UUID uuid)
            throws NotFoundException, CancelledException {
        if (jobs.containsKey(uuid)) {
            if (jobs.get(uuid).getJobState() == State.CANCELLED) {
                throw new CancelledException(uuid);
            } else {
                jobs.get(uuid).cancel();
            }
        } else if (finishedJobs.containsKey(uuid)) {
            finishedJobs.remove(uuid);
        } else {
            throw new NotFoundException(uuid);
        }
    }


    @Override
    public void onJobDone(Job job) {
        finishedJobs.put(job.getUUID(), job.getJobStatus());
        jobs.remove(job.getUUID());
    }
}
