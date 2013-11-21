package org.purl.wf4ever.wf2ro.rest;

import java.net.URI;

/**
 * Which resources to extract, and to which folder in the RO to aggregate them.
 * <p>
 * Each field specifies the destination ro:Folder where to upload and aggregate
 * the extracted resource. If a folder is <code>null</code>, then that resource
 * type is not extracted.
 * <p>
 * The folder URI will also form basis for the suggested Slug when uploading the
 * resource, so for instance if nested is
 * <code>http://example.com/ROs/ro15/nestings/</code> then the workflow
 * <code>HelloThere.wfbundle</code> would be uploaded with the slug
 * <code>nestings/HelloThere.wfbundle</code>.
 * 
 * @author Stian Soiland-Reyes
 * 
 */
public class JobExtractFolders {

    /**
     * 
     */
    public JobExtractFolders() {

    }

    /**
     * 
     * @param main
     *            RO Folder where to store the extracted main workflow
     * @param nested
     *            RO Folder where to store the extracted nested workflows
     * @param scripts
     *            RO Folder where to store the extracted scripts
     * @param services
     *            RO Folder where to store the extracted services
     */
    public JobExtractFolders(URI main, URI nested, URI scripts, URI services) {
        this.main = main;
        this.nested = nested;
        this.scripts = scripts;
        this.services = services;
    }

    /**
     * @return RO Folder where to store the extracted main workflow
     */
    public URI getMain() {
        return main;
    }

    /**
     * @param main
     *            RO Folder where to store the extracted main workflow
     */
    public void setMain(URI main) {
        this.main = main;
    }

    /**
     * @return RO Folder where to store the extracted nested workflows
     */
    public URI getNested() {
        return nested;
    }

    /**
     * @param nested
     *            RO Folder where to store the extracted nested workflows
     */
    public void setNested(URI nested) {
        this.nested = nested;
    }

    /**
     * @return RO Folder where to store the extracted scripts
     */
    public URI getScripts() {
        return scripts;
    }

    /**
     * @param scripts
     *            RO Folder where to store the extracted scripts
     */
    public void setScripts(URI scripts) {
        this.scripts = scripts;
    }

    /**
     * @return RO Folder where to store the extracted web services
     */
    public URI getServices() {
        return services;
    }

    /**
     * @param services
     *            RO Folder where to store the extracted web services
     */
    public void setServices(URI services) {
        this.services = services;
    }

    private URI main;
    private URI nested;
    private URI scripts;
    private URI services;
}