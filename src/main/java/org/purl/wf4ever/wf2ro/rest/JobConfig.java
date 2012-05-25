package org.purl.wf4ever.wf2ro.rest;

import java.net.URI;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Parameters provided in JSON when creating a job.
 * 
 * @author piotrekhol
 * 
 */
@XmlRootElement
public class JobConfig {

    /** workflow URI. */
    private URI resource;

    /** workflow format URI. */
    private URI format;

    /** RO URI. */
    private URI ro;

    /** RODL access token. */
    private String token;


    /**
     * Default empty constructor.
     */
    public JobConfig() {

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
     * @param token
     *            RODL access token
     */
    public JobConfig(URI resource, URI format, URI ro, String token) {
        super();
        this.resource = resource;
        this.format = format;
        this.ro = ro;
        this.token = token;
    }


    public URI getResource() {
        return resource;
    }


    public void setResource(URI resource) {
        this.resource = resource;
    }


    public URI getFormat() {
        return format;
    }


    public void setFormat(URI format) {
        this.format = format;
    }


    public URI getRo() {
        return ro;
    }


    public void setRo(URI ro) {
        this.ro = ro;
    }


    public String getToken() {
        return token;
    }


    public void setToken(String token) {
        this.token = token;
    }
}
