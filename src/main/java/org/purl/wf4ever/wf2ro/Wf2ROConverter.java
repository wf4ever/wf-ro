/**
 * 
 */
package org.purl.wf4ever.wf2ro;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.purl.wf4ever.rosrs.client.common.ROService;
import org.purl.wf4ever.wfdesc.scufl2.ROEvoSerializer;

import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.io.WorkflowBundleIO;
import uk.org.taverna.scufl2.api.io.WriterException;
import uk.org.taverna.scufl2.rdfxml.RDFXMLReader;

import com.hp.hpl.jena.ontology.OntModel;

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

    /** Workflow serializer/deserializer. */
    private static WorkflowBundleIO bundleIO = new WorkflowBundleIO();

    /** Service URI. */
    protected URI serviceURI;

    /** Resources added so far. */
    private List<URI> resourcesAdded = Collections.synchronizedList(new ArrayList<URI>());

    /** Workflow. */
    private WorkflowBundle wfbundle;

    /** Used to guarantee that the conversion is run only once. */
    private Boolean running = false;


    /**
     * The constructor.
     * 
     * @param serviceURI
     *            The conversion service URI, used e.g. for authoring changes in metadata
     * @param wfbundle
     *            The t2flow/scufl2 workflow that needs to be converted to an RO.
     */
    public Wf2ROConverter(URI serviceURI, WorkflowBundle wfbundle) {
        this.serviceURI = serviceURI;
        this.wfbundle = wfbundle;
    }


    /**
     * The conversion method. Note that there are no ROSRS parameters, since all ROSRS handling is delegated to abstract
     * methods of this class.
     * 
     * This method can be called only once per class instance. Subsequent calls will result in IllegalStateException
     * being thrown.
     */
    public void convert() {
        synchronized (running) {
            if (running) {
                throw new IllegalStateException(
                        "This instance can only be run once. Create another instance for another conversion.");
            }
            running = true;
        }
        UUID wfUUID = getWorkflowBundleUUID(wfbundle);
        URI roURI = createResearchObject(wfUUID);
        URI wfURI;
        try {
            wfURI = addWorkflowBundle(roURI, wfbundle, wfUUID);
            resourcesAdded.add(wfURI);
        } catch (IOException e) {
            LOG.error("Can't upload workflow bundle", e);
            return;
        }
        try {
            resourcesAdded.add(addWfDescAnnotation(roURI, wfbundle, wfURI));
        } catch (IOException e) {
            LOG.error("Can't upload workflow desc", e);
        }
        try {
            resourcesAdded.add(addRoEvoAnnotation(roURI, wfbundle, wfURI));
        } catch (IOException e) {
            LOG.error("Can't upload RO evolution desc", e);
        }
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
     * Upload the workflow bundle to RODL.
     * 
     * @param roURI
     *            research object URI
     * @param wfbundle
     *            the workflow bundle
     * @param wfUUID
     *            workflow bundle UUID
     * @return the workflow bundle URI as in the manifest or null if uploading failed
     * @throws IOException
     *             when there was a problem with getting/uploading the RO resources
     */
    protected URI addWorkflowBundle(URI roURI, final WorkflowBundle wfbundle, UUID wfUUID)
            throws IOException {
        URI wfURI = roURI.resolve(wfUUID.toString());

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
        uploadAggregatedResource(wfURI, RDFXMLReader.APPLICATION_VND_TAVERNA_SCUFL2_WORKFLOW_BUNDLE, in);
        return wfURI;

    }


    /**
     * Generates and adds a workflow bundle history annotation using the roevo ontology.
     * 
     * @param roURI
     *            research object URI
     * @param wfbundle
     *            the workflow bundle
     * @param rodlWfURI
     *            the workflow bundle URI used in the research object
     * @return the annotation body URI
     * @throws IOException
     *             when there was a problem with getting/uploading the RO resources
     */
    protected URI addRoEvoAnnotation(URI roURI, final WorkflowBundle wfbundle, URI rodlWfURI)
            throws IOException {

        OntModel manifest = createManifestModel(roURI);
        URI annotationBodyURI = createAnnotationBodyURI(roURI, rodlWfURI);
        URI annotationURI = createAnnotationURI(manifest, roURI);
        ROService
                .addAnnotationToManifestModel(manifest, roURI, annotationURI, rodlWfURI, annotationBodyURI, serviceURI);
        uploadManifest(roURI, manifest);

        try {
            final PipedInputStream in = new PipedInputStream();
            final PipedOutputStream out = new PipedOutputStream(in);
            new Thread(new Runnable() {

                public void run() {
                    ROEvoSerializer roEvo = new ROEvoSerializer();
                    try {
                        roEvo.workflowHistory(wfbundle.getMainWorkflow(), System.out);
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
            uploadAggregatedResource(annotationBodyURI, TEXT_TURTLE, in);
            return annotationBodyURI;
        } catch (IOException e) {
            manifest = createManifestModel(roURI); // a to nie rzuca wyjatku? dodac try/catch i log
            ROService.deleteAnnotationFromManifest(manifest, annotationURI);
            uploadManifest(roURI, manifest);
            throw new IOException("Can't upload annotation body", e);
        }

    }


    /**
     * Generates and adds a workflow bundle description annotation using the wfdesc ontology.
     * 
     * @param roURI
     *            research object URI
     * @param wfbundle
     *            the workflow bundle
     * @param rodlWfURI
     *            the workflow bundle URI used in the research object
     * @return the annotation body URI
     * @throws IOException
     *             when there was a problem with getting/uploading the RO resources
     */
    protected URI addWfDescAnnotation(URI roURI, final WorkflowBundle wfbundle, URI rodlWfURI)
            throws IOException {
        OntModel manifest = createManifestModel(roURI);
        URI annotationBodyURI = createAnnotationBodyURI(roURI, rodlWfURI);
        URI annotationURI = createAnnotationURI(manifest, roURI);
        ROService
                .addAnnotationToManifestModel(manifest, roURI, annotationURI, rodlWfURI, annotationBodyURI, serviceURI);
        uploadManifest(roURI, manifest);

        try {
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
            uploadAggregatedResource(annotationBodyURI, TEXT_TURTLE, in);
            return annotationBodyURI;
        } catch (IOException e) {
            manifest = createManifestModel(roURI); // a to nie rzuca wyjatku? dodac try/catch i log
            ROService.deleteAnnotationFromManifest(manifest, annotationURI);
            uploadManifest(roURI, manifest);
            throw new IOException("Can't upload annotation body", e);
        }
    }


    /**
     * Create an annotation URI. By default calls {@link ROService#createAnnotationURI(OntModel, URI)}.
     * 
     * @param manifest
     *            the manifest model
     * @param roURI
     *            RO URI
     * @return the annotation URI
     */
    protected URI createAnnotationURI(OntModel manifest, URI roURI) {
        return ROService.createAnnotationURI(manifest, roURI);
    }


    /**
     * Create an annotation body URI. By default calls {@link ROService#createAnnotationBodyURI(URI, URI)}.
     * 
     * @param roURI
     *            RO URI
     * @param rodlWfURI
     *            workflow URI
     * @return annotation body URI
     */
    protected URI createAnnotationBodyURI(URI roURI, URI rodlWfURI) {
        return ROService.createAnnotationBodyURI(roURI, rodlWfURI);
    }


    /**
     * Create a new research object or, if it exists, prepare it for uploading workflows, i.e. preserve the previous
     * workflow versions.
     * 
     * @param wfUUID
     *            UUID of the workflow bundle that will be uploaded later.
     * 
     * @return the research object URI
     */
    protected abstract URI createResearchObject(UUID wfUUID);


    /**
     * Saves an aggregated resource to RODL.
     * 
     * @param resourceURI
     *            resource URI
     * @param contentType
     *            resource content type to be sent as in HTTP request
     * @param in
     *            resource input stream
     * @throws IOException
     *             when there are problems with uploading the resource
     */
    protected abstract void uploadAggregatedResource(URI resourceURI, String contentType, InputStream in)
            throws IOException;


    /**
     * Create Jena model of the manifest.
     * 
     * @param roURI
     *            research object URI
     * @return the Jena model of the manifest
     */
    protected abstract OntModel createManifestModel(URI roURI);


    /**
     * Upload the manifest to RODL.
     * 
     * @param roURI
     *            research object URI
     * @param manifest
     *            the Jena model of the manifest
     */
    protected abstract void uploadManifest(URI roURI, OntModel manifest);


    /**
     * Return the list of resources that have already been added as a result of the conversion.
     * 
     * @return the list of resource URIs
     */
    public List<URI> getResourcesAdded() {
        return resourcesAdded;
    }

}
