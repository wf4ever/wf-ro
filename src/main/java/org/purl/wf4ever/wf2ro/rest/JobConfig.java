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

    /** workflow format MIME type. */
    private String format;

    /** RO URI. */
    private URI ro;

    /** RODL access token. */
    private String token;

    /** Folders to extract into */
    private JobExtractFolders extract;


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
     * @param extract
     *            Which resources to extract to which folders
     */
    public JobConfig(URI resource, String format, URI ro, String token, JobExtractFolders extract) {
        super();
        this.resource = resource;
        this.format = format;
        this.ro = ro;
        this.token = token;
        this.setExtract(extract);
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


    public String getToken() {
        return token;
    }


    public void setToken(String token) {
        this.token = token;
    }


    public JobExtractFolders getExtract() {
        if (extract == null) {
            extract = new JobExtractFolders();
        }
        return extract;
    }


    public void setExtract(JobExtractFolders extract) {
        this.extract = extract;
    }
}
