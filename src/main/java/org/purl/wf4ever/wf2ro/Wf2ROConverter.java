/**
 * 
 */
package org.purl.wf4ever.wf2ro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.ObjectInputStream.GetField;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.openrdf.rio.RDFFormat;
import org.purl.wf4ever.rosrs.client.Annotable;
import org.purl.wf4ever.rosrs.client.Annotation;
import org.purl.wf4ever.rosrs.client.Folder;
import org.purl.wf4ever.rosrs.client.FolderEntry;
import org.purl.wf4ever.rosrs.client.ResearchObject;
import org.purl.wf4ever.rosrs.client.Resource;
import org.purl.wf4ever.rosrs.client.exception.ROException;
import org.purl.wf4ever.rosrs.client.exception.ROSRSException;
import org.purl.wf4ever.wfdesc.scufl2.ROEvoSerializer;

import uk.org.taverna.scufl2.api.common.NamedSet;
import uk.org.taverna.scufl2.api.common.URITools;
import uk.org.taverna.scufl2.api.configurations.Configuration;
import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.core.Workflow;
import uk.org.taverna.scufl2.api.io.WorkflowBundleIO;
import uk.org.taverna.scufl2.api.io.WriterException;
import uk.org.taverna.scufl2.api.profiles.Profile;
import uk.org.taverna.scufl2.rdfxml.RDFXMLReader;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

/**
 * This class defines the main logic of workflow-RO conversion. It is abstract because it leaves the resource upload
 * details to its subclasses.
 * 
 * @author piotrekhol
 * 
 */
public abstract class Wf2ROConverter {

    /** text/turtle. */
    private static final String TEXT_TURTLE = "text/turtle";

    /** Wfdesc mime type. */
    private static final String TEXT_VND_WF4EVER_WFDESC_TURTLE = "text/vnd.wf4ever.wfdesc+turtle";

    /** Logger. */
    private static final Logger LOG = Logger.getLogger(Wf2ROConverter.class);

    /** Workflow bundle serializer/deserializer. */
    private static WorkflowBundleIO bundleIO = new WorkflowBundleIO();

    /** Scufl2 URI helpers. */
    private static URITools uriTools = new URITools();

    /** Resources added so far. */
    private List<URI> resourcesAdded = Collections.synchronizedList(new ArrayList<URI>());

    /** Workflow bundle. */
    private WorkflowBundle wfbundle;

    /** Used to guarantee that the conversion is run only once. */
    private Boolean running = false;

    /** The original workflow URI. */
    protected URI originalWfUri;


    /**
     * The constructor.
     * 
     * @param wfbundle
     *            The t2flow/scufl2 workflow that needs to be converted to an RO.
     * @param wfUri
     *            workflow URI
     */
    public Wf2ROConverter(WorkflowBundle wfbundle, URI wfUri) {
        this.wfbundle = wfbundle;
        this.originalWfUri = wfUri;
    }


    /**
     * The conversion method. Note that there are no ROSRS parameters, since all ROSRS handling is delegated to abstract
     * methods of this class.
     * 
     * This method can be called only once per class instance. Subsequent calls will result in IllegalStateException
     * being thrown.
     * 
     * @throws Exception
     *             any kind of conversion problem
     */
    public void convert()
            throws Exception {
        synchronized (running) {
            if (running) {
                throw new IllegalStateException(
                        "This instance can only be run once. Create another instance for another conversion.");
            }
            running = true;
        }
        UUID wfUUID = getWorkflowBundleUUID(wfbundle);
        String wfname = wfbundle.getMainWorkflow().getName() + ".wfbundle";
        ResearchObject ro = createResearchObject(wfUUID);
        Resource wfbundleAggregated = addWorkflowBundle(ro, wfbundle, wfname);
        Folder mainFolder = getExtractMain();
        if (mainFolder != null) {
            // Add to the main folder
            mainFolder.addEntry(wfbundleAggregated, wfname);
            // TODO: Handle conflicts if already exists
        }
        
        resourcesAdded.add(wfbundleAggregated.getUri());
        try {
            extractAnnotations(ro, wfbundleAggregated, wfbundle, resourcesAdded);
        } catch (IOException | ROSRSException e) {
            LOG.error("Can't extract annotations from workflow", e);
        }

        try {
            resourcesAdded.add(addWfDescAnnotation(ro, wfbundle, wfbundleAggregated).getUri());
        } catch (IOException | ROSRSException e) {
            LOG.error("Can't upload workflow desc", e);
        }
        try {
            resourcesAdded.add(addRoEvoAnnotation(ro, wfbundle, wfbundleAggregated).getUri());
        } catch (IOException | ROSRSException e) {
            LOG.error("Can't upload RO evolution desc", e);
        }
        try {
            URI internalWorkflowURI = uriTools.uriForBean(wfbundle.getMainWorkflow());
            resourcesAdded.add(addLinkAnnotation(ro, originalWfUri, wfbundleAggregated, internalWorkflowURI).getUri());
        } catch (ROSRSException e) {
            LOG.error("Can't upload the link annotation", e);
        }
        uploadNestedWorkflows(ro);
        uploadScripts(ro);
    }

    private void uploadScripts(ResearchObject ro) throws IOException, ROSRSException, ROException {
        Folder folder = getExtractScripts();
        if (folder == null) {
            return;
        }
        for (Profile p : wfbundle.getProfiles()) {
            for (Configuration conf : p.getConfigurations()) {
                String script = conf.getJson().path("script").asText();
                if (script.isEmpty()) {
                    // TODO: Also support ExternalTool command line
                    continue;
                }
                // Find unique identifier for script - we'll hash its content
                String sha = utf8sha(script);
                
                if (hasFolderEntryWithNameContaining(folder, sha)) {
                    // Another script with the same hash exists - first one wins
                    // TODO: Add more annotations about dct:hasPart or similar?
                    continue;
                }

                // We'll use a slightly nicer name, include the sha hash
                String name = conf.getName() + "-" + sha + ".txt";
                URI slug = slugForFolder(ro, folder).resolve(name);                
                // Upload script
                ByteArrayInputStream scriptStream = new ByteArrayInputStream(script.getBytes(UTF8));
                Resource uploadedScript = uploadAggregatedResource(ro, 
                         slug.toASCIIString(), scriptStream, "text/plain");
                addToFolder(folder, uploadedScript, name);                    
                addLinkAnnotation(ro, originalWfUri, uploadedScript, null);                    
            }
        }
    }

    private boolean hasFolderEntryWithNameContaining(Folder folder, String partialFilename) throws ROSRSException {
        if (partialFilename.isEmpty()) {
            return false;
        }
        if (! folder.isLoaded()) {
            folder.load(false);            
        }
        for (FolderEntry entry : folder.getFolderEntries().values()) {
            if (entry.getName().contains(partialFilename)) {
                return true;
            }
        }
        return false;
    }

    private URI slugForFolder(ResearchObject ro, Folder folder) {
        URI slugBase = ro.getUri().relativize(folder.getUri());
        if (slugBase.isAbsolute()) {
            // no common base
            slugBase = URI.create("");
        } else if (! slugBase.getPath().endsWith("/")) {
            // ensure ends with /
            slugBase = slugBase.resolve(slugBase.getPath() + "/");                    
        }
        return slugBase;
    }

    
    
    private Resource getFolderEntry(Folder folder, String entryName) throws ROSRSException {
        if (! folder.isLoaded()) {
            folder.load(false);            
        }
        for (FolderEntry entry : folder.getFolderEntries().values()) {
            if (entry.getName().equals(entryName)) {
                return entry.getResource();
            }
        }
        return null;
    }

    private static Charset UTF8 = Charset.forName("UTF-8");

    private String utf8sha(String script) {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        sha1.update(script.getBytes(UTF8));
        byte[] digest = sha1.digest();
        return Hex.encodeHexString(digest);
    }


    private void uploadNestedWorkflows(ResearchObject ro) throws IOException, ROSRSException, WriterException, ROException {
        Folder folder = getExtractNested();
        if (folder == null) {
            return;
        }
        Workflow mainWf = wfbundle.getMainWorkflow();
        try {
            for (Workflow otherWf : wfbundle.getWorkflows()) {
                if (otherWf == mainWf) {
                   continue;
                }
                // Cheaky way to save out nested workflows (and any of their
                // nested wfs, annotations and configurations).
                //
                // Note that this is not ideal, it will include lots of
                // unreferenced workflows and configurations, but is easier than
                // doing a recursive copy
                wfbundle.setMainWorkflow(otherWf);
                
                // Try to extract the UUID
                String id = Workflow.WORKFLOW_ROOT.relativize(otherWf.getIdentifier())
                        .toASCIIString().replace("/", "");
                UUID uuid;
                try {
                    uuid = UUID.fromString(id);
                } catch (IllegalArgumentException ex) {
                    // Fallback, generate a name UUID from the URL                
                    uuid = UUIDTool.namespaceUUID(otherWf.getIdentifier());
                }            
                
                if (hasFolderEntryWithNameContaining(folder, uuid.toString())) {
                    // Another workflow with the same id exists - first one wins
                    continue;
                }
                
                // A long, but unique name 
                String name = otherWf.getName() + "-" + uuid + ".wfbundle";
                URI slug = slugForFolder(ro, folder).resolve(name);
                Resource nestedWf = addWorkflowBundle(ro, wfbundle, slug.toString());
                addToFolder(folder, nestedWf, name);                
                addLinkAnnotation(ro, originalWfUri, nestedWf, otherWf.getIdentifier());
                
            } 
        } finally {
            // Restore wfbundle main workflow in case it is used elsewhere
            wfbundle.setMainWorkflow(mainWf);
        }
        
    }

    private void addToFolder(Folder folder, Resource resource, String entryName) throws ROSRSException, ROException {
        if (! folder.isLoaded()) {
            folder.load(false);
        }
        folder.addEntry(resource, entryName);
    }

    /**
     * Extract workflow UUID.
     * 
     * @param wfbundle
     *            workflow bundle
     * @return workflow UUID
     */
    private UUID getWorkflowBundleUUID(WorkflowBundle wfbundle) {
        URI wfbundleURI = wfbundle.getGlobalBaseURI();
        return UUID.fromString(wfbundleURI.resolve("..").relativize(wfbundleURI).toString().split("/")[0]);
    }


    /**
     * Upload the workflow bundle and the resources it contains to RODL.
     * 
     * @param ro
     *            research object URI
     * @param wfbundle
     *            the workflow bundle
     * @param wfPath
     *            workflow bundle path
     * @return the workflow bundle URI as in the manifest or null if uploading failed
     * @throws IOException
     *             when there was a problem with getting/uploading the RO resources
     * @throws ROSRSException
     *             ROSR service error
     * @throws WriterException
     *             workflow bundle error
     * @throws ROException
     *             when the manifest is incorrect
     */
    protected Resource addWorkflowBundle(ResearchObject ro, final WorkflowBundle wfbundle, String wfPath)
            throws IOException, ROSRSException, WriterException, ROException {
        //save the scufl2
        final PipedInputStream in = new PipedInputStream();
        final PipedOutputStream out = new PipedOutputStream(in);
        new Thread(new Runnable() {

            public void run() {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                try {
                    bundleIO.writeBundle(wfbundle, out, RDFXMLReader.APPLICATION_VND_TAVERNA_SCUFL2_WORKFLOW_BUNDLE);
                } catch (WriterException | IOException e) {
                    LOG.error("Can't download workflow bundle", e);
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        LOG.warn("Exception when closing the workflow bundle output stream", e);
                    }
                }
            }
        }).start();
        return uploadAggregatedResource(ro, wfPath, in, RDFXMLReader.APPLICATION_VND_TAVERNA_SCUFL2_WORKFLOW_BUNDLE);
    }


    /**
     * Extract annotations from workflow bundle and upload them as RO annotations. Annotation bodies are copied as RO
     * annotation bodies, annotation targets are referenced.
     * 
     * Update: only annotations of the workflow bundle or the main workflow are uploaded, with the workflow bundle as
     * their target. Others are skipped because they point to resources that are not aggregated.
     * 
     * @param ro
     *            RO URI
     * @param wfbundleAggregated
     *            workflow bundle ROSRS URI
     * @param wfbundle
     *            workflow bundle
     * @param resourcesAdded2
     *            list of resources to which the annotations URIs will be added
     * @throws IOException
     *             cannot read the annotation body
     * @throws ROSRSException
     *             ROSR service error
     * @throws ROException
     *             when the manifest is incorrect
     */
    private void extractAnnotations(ResearchObject ro, Resource wfbundleAggregated, final WorkflowBundle wfbundle,
            List<URI> resourcesAdded2)
            throws IOException, ROSRSException, ROException {
        //search for annotations
        URITools tools = new URITools();
        NamedSet<uk.org.taverna.scufl2.api.annotation.Annotation> annotations = wfbundle.getAnnotations();
        for (uk.org.taverna.scufl2.api.annotation.Annotation annotation : annotations) {
            if (annotation.getTarget().equals(wfbundle) || annotation.getTarget().equals(wfbundle.getMainWorkflow())) {
                LOG.debug(String.format("Uploading annotation for %s taken from %s", wfbundleAggregated.getUri(),
                    annotation.getBody()));
                Model annBody = ModelFactory.createDefaultModel();
                String annotationBody = annotation.getBody().toASCIIString();
                try (InputStream wfAnnBody = wfbundle.getResources().getResourceAsInputStream(
                    annotationBody)) {
//                    System.out.println(annotationBody);
//                    System.out.println(wfbundle.getResources().getResourceAsString(annotationBody));
                    annBody.read(wfAnnBody, wfbundle.getGlobalBaseURI().resolve(annotation.getBody()).toASCIIString());
                }
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    annBody.write(out);
                    try (ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray())) {
                        resourcesAdded2.add(uploadAnnotation(ro, "wf", wfbundleAggregated, in, "application/rdf+xml")
                                .getUri());
                    }
                }
            } else {
                URI target = tools.uriForBean(annotation.getTarget());
                LOG.debug(String.format("Skipping annotation for %s taken from %s", target, annotation.getBody()));
            }
        }
    }


    /**
     * Generates and adds a workflow bundle history annotation using the roevo ontology.
     * 
     * @param ro
     *            research object URI
     * @param wfbundle
     *            the workflow bundle
     * @param wfbundleAggregated
     *            the workflow bundle URI used in the research object
     * @return the annotation body URI
     * @throws IOException
     *             when there was a problem with getting/uploading the RO resources
     * @throws ROSRSException
     *             ROSR service error
     * @throws ROException
     *             when the manifest is incorrect
     */
    protected Annotation addRoEvoAnnotation(ResearchObject ro, final WorkflowBundle wfbundle,
            Resource wfbundleAggregated)
            throws IOException, ROSRSException, ROException {
        final PipedInputStream in = new PipedInputStream();
        final PipedOutputStream out = new PipedOutputStream(in);
        new Thread(new Runnable() {

            public void run() {
                ROEvoSerializer roEvo = new ROEvoSerializer();
                try {
                    roEvo.workflowHistory(wfbundle.getMainWorkflow(), out);
                } catch (WriterException e) {
                    LOG.error("Can't download workflow desc", e);
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        LOG.warn("Exception when closing the annotation body output stream", e);
                    }
                }
            }
        }).start();
        return uploadAnnotation(ro, "roevo", wfbundleAggregated, in, TEXT_TURTLE);
    }


    /**
     * Generates and adds a workflow bundle description annotation using the wfdesc ontology.
     * 
     * @param ro
     *            research object URI
     * @param wfbundle
     *            the workflow bundle
     * @param wfbundleAggregated
     *            the workflow bundle URI used in the research object
     * @return the annotation body URI
     * @throws IOException
     *             when there was a problem with getting/uploading the RO resources
     * @throws ROSRSException
     *             when there was a problem with getting/uploading the RO resources
     * @throws ROException
     *             when the manifest is incorrect
     */
    protected Annotation addWfDescAnnotation(ResearchObject ro, final WorkflowBundle wfbundle,
            Resource wfbundleAggregated)
            throws IOException, ROSRSException, ROException {
        final PipedInputStream in = new PipedInputStream();
        final PipedOutputStream out = new PipedOutputStream(in);
        new Thread(new Runnable() {

            public void run() {
                try {
                    bundleIO.writeBundle(wfbundle, out, TEXT_VND_WF4EVER_WFDESC_TURTLE);
                } catch (WriterException | IOException e) {
                    LOG.error("Can't download workflow desc", e);
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        LOG.warn("Exception when closing the annotation body output stream", e);
                    }
                }
            }
        }).start();
        return uploadAnnotation(ro, "wfdesc", wfbundleAggregated, in, TEXT_TURTLE);
    }


    /**
     * Generates and adds an annotation linking the aggregated workflow bundle (ROSRS URI) with its main workflow
     * (internal URI).
     * 
     * @param ro
     *            research object URI
     * @param originalWfUri
     *            original workflow Uri
     * @param wfbundleAggregated
     *            the workflow bundle URI
     * @param workflowIdentifier
     *            the workflow internal URI
     * @return the annotation body URI
     * @throws ROSRSException
     *             when there was a problem with getting/uploading the RO resources
     * @throws ROException
     *             when the manifest is incorrect
     */
    protected Annotation addLinkAnnotation(ResearchObject ro, URI originalWfUri, Resource wfbundleAggregated,
            URI workflowIdentifier)
            throws ROSRSException, ROException {
        Model model = ModelFactory.createDefaultModel();
        com.hp.hpl.jena.rdf.model.Resource originalR = model.createResource(originalWfUri.toString());
        com.hp.hpl.jena.rdf.model.Resource wfbundleR = model.createResource(wfbundleAggregated.getUri().toString());
        //FIXME move that property to rodl-common
        Property importedFrom = model.createProperty("http://purl.org/pav/importedFrom");
        Property derivedFrom = model.createProperty("http://purl.org/pav/derivedFrom");

        if (workflowIdentifier != null) {
            model.add(wfbundleR, importedFrom, originalR);
            Property hasWorkflowDef = model.createProperty("http://purl.org/wf4ever/wfdesc#hasWorkflowDefinition");
            com.hp.hpl.jena.rdf.model.Resource mainwfR = model.createResource(workflowIdentifier.toString());
            model.add(mainwfR, hasWorkflowDef, wfbundleR);
        } else {
            model.add(wfbundleR, derivedFrom, originalR);            
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        model.write(out, "TURTLE");
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        return uploadAnnotation(ro, "link", wfbundleAggregated, in, RDFFormat.TURTLE.getDefaultMIMEType());
    }


    /**
     * Create a new research object or, if it exists, prepare it for uploading workflows, i.e. preserve the previous
     * workflow versions.
     * 
     * @param wfUUID
     *            UUID of the workflow bundle that will be uploaded later.
     * 
     * @return the research object
     * @throws ROSRSException
     *             ROSR service error
     */
    protected abstract ResearchObject createResearchObject(UUID wfUUID)
            throws ROSRSException;


    /**
     * Saves an aggregated resource to RODL.
     * 
     * @param ro
     *            research object URI
     * @param path
     *            the resource path
     * @param contentType
     *            resource content type to be sent as in HTTP request
     * @param in
     *            resource input stream
     * @return aggregated resource
     * @throws IOException
     *             when there are problems with uploading the resource
     * @throws ROSRSException
     *             ROSR service error
     * @throws ROException
     *             when the manifest is incorrect
     */
    protected abstract Resource uploadAggregatedResource(ResearchObject ro, String path, InputStream in,
            String contentType)
            throws IOException, ROSRSException, ROException;


    /**
     * Saves a resource in RODL as an annotation body of another resource.
     * 
     * @param ro
     *            research object URI
     * @param name
     *            annotation name to use in filename
     * @param target
     *            resource that is annotated
     * @param contentType
     *            content type
     * @param in
     *            resource input stream
     * @return annotation body URI
     * @throws ROSRSException
     *             when there are problems with uploading the resource
     * @throws ROException
     *             when the manifest is incorrect
     */
    protected abstract Annotation uploadAnnotation(ResearchObject ro, String name, Annotable target, InputStream in,
            String contentType)
            throws ROSRSException, ROException;


    /**
     * Return the list of resources that have already been added as a result of the conversion.
     * 
     * @return the list of resource URIs
     */
    public List<URI> getResourcesAdded() {
        return resourcesAdded;
    }
    
    
    /**
     * @return RO Folder where to extract main workflow, or
     *         <code>null</code> to not add extracted main workflow to any
     *         folder (the main workflow is still extracted)
     */
    public abstract Folder getExtractMain();

    /**
     * @return RO Folder where to extract nested workflows, or
     *         <code>null</code> to not extract.
     */
    public abstract Folder getExtractNested();

    /**
     * @return RO Folder where to extract scripts, or <code>null</code>
     *         to not extract.
     */
    public abstract Folder getExtractScripts();

    /**
     * @return RO Folder where to extract services, or <code>null</code>
     *         to not extract
     */
    public abstract Folder getExtractServices();

}
