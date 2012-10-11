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
     * @param ro
     *            RO URI
     * @param state
     *            job state
     * @param added
     *            resources added
     */
    public JobStatus(URI resource, String format, URI ro, State state, List<URI> added) {
        super();
        this.resource = resource;
        this.format = format;
        this.ro = ro;
        this.status = state;
        this.added = added;
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

}
