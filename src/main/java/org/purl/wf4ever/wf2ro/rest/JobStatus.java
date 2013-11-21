package org.purl.wf4ever.wf2ro.rest;

import java.net.URI;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.purl.wf4ever.wf2ro.rest.Job.State;

/**
 * Job status as JSON.
 * 
 * @author piotrekhol
 * 
 */
@XmlRootElement
public class JobStatus {

    /** workflow URI. */
    private URI resource;

    /** workflow format MIME type. */
    private String format;

    /** RO URI. */
    private URI ro;

    /** job state. */
    private State status;

    /** resources already uploaded. */
    private List<URI> added;

    /** reason for the status, i.e. exception message. */
    private String reason;

    private JobExtractFolders extract;


    /**
     * Default empty constructor.
     */
    public JobStatus() {

    }


    /**
     * Constructor.
     * 
     * @param resource
     *            workflow URI
     * @param format
     *            workflow format URI
     * @param extract
     *            Which resources to extract to which folders
     * @param ro
     *            RO URI
     * @param state
     *            job state
     * @param added
     *            resources added
     * @param reason
     *            reason for the status, i.e. exception message
     */
    public JobStatus(URI resource, String format, JobExtractFolders extract, URI ro, State state, List<URI> added, String reason) {
        super();
        this.resource = resource;
        this.format = format;
        this.setExtract(extract);
        this.ro = ro;
        this.status = state;
        this.added = added;
        this.reason = reason;
    }


    public URI getResource() {
        return resource;
    }


    public void setResource(URI resource) {
        this.resource = resource;
    }


    public String getFormat() {
        return format;
    }


    public void setFormat(String format) {
        this.format = format;
    }


    public URI getRo() {
        return ro;
    }


    public void setRo(URI ro) {
        this.ro = ro;
    }


    public State getStatus() {
        return status;
    }


    public void setStatus(State status) {
        this.status = status;
    }


    public List<URI> getAdded() {
        return added;
    }


    public void setAdded(List<URI> added) {
        this.added = added;
    }


    public String getReason() {
        return reason;
    }


    public void setReason(String reason) {
        this.reason = reason;
    }


    public JobExtractFolders getExtract() {
        return extract;
    }


    public void setExtract(JobExtractFolders extract) {
        this.extract = extract;
    }

}
