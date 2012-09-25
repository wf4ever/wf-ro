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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.purl.wf4ever.rosrs.client.common.ROSRSException;
import org.purl.wf4ever.wfdesc.scufl2.ROEvoSerializer;

import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.io.WorkflowBundleIO;
import uk.org.taverna.scufl2.api.io.WriterException;
import uk.org.taverna.scufl2.rdfxml.RDFXMLReader;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;

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

    /** Resources added so far. */
    private List<URI> resourcesAdded = Collections.synchronizedList(new ArrayList<URI>());

    /** Workflow. */
    private WorkflowBundle wfbundle;

    /** Used to guarantee that the conversion is run only once. */
    private Boolean running = false;


    /**
     * The constructor.
     * 
     * @param wfbundle
     *            The t2flow/scufl2 workflow that needs to be converted to an RO.
     */
    public Wf2ROConverter(WorkflowBundle wfbundle) {
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
        URI roURI = null;
        try {
            roURI = createResearchObject(wfUUID);
        } catch (ROSRSException e) {
            LOG.error("Can't create RO", e);
            return;
        }
        URI wfURI;
        try {
            wfURI = addWorkflowBundle(roURI, wfbundle, wfUUID);
            resourcesAdded.add(wfURI);
        } catch (IOException | ROSRSException e) {
            LOG.error("Can't upload workflow bundle", e);
            return;
        }
        URI wfdescURI = null;
        try {
            wfdescURI = addWfDescAnnotation(roURI, wfbundle, wfURI);
            resourcesAdded.add(wfdescURI);
        } catch (IOException | ROSRSException e) {
            LOG.error("Can't upload workflow desc", e);
        }
        try {
            resourcesAdded.add(addRoEvoAnnotation(roURI, wfbundle, wfURI));
        } catch (IOException | ROSRSException e) {
            LOG.error("Can't upload RO evolution desc", e);
        }
        try {
            if (wfdescURI != null) {
                aggregateWorkflowDependencies(roURI, wfdescURI);
            } else {
                LOG.error("Can't read the workflow desc");
            }
        } catch (ROSRSException e) {
            LOG.error("Can't aggregate resources", e);
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
     * @throws ROSRSException
     */
    protected URI addWorkflowBundle(URI roURI, final WorkflowBundle wfbundle, UUID wfUUID)
            throws IOException, ROSRSException {
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
        uploadAggregatedResource(roURI, wfUUID.toString(), in,
            RDFXMLReader.APPLICATION_VND_TAVERNA_SCUFL2_WORKFLOW_BUNDLE);
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
     * @throws ROSRSException
     */
    protected URI addRoEvoAnnotation(URI roURI, final WorkflowBundle wfbundle, URI rodlWfURI)
            throws IOException, ROSRSException {
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
        return uploadAnnotation(roURI, Arrays.asList(rodlWfURI), in, TEXT_TURTLE);
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
     * @throws ROSRSException
     *             when there was a problem with getting/uploading the RO resources
     */
    protected URI addWfDescAnnotation(URI roURI, final WorkflowBundle wfbundle, URI rodlWfURI)
            throws IOException, ROSRSException {
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
        return uploadAnnotation(roURI, Arrays.asList(rodlWfURI), in, TEXT_TURTLE);
    }


    /**
     * Search for external resources in the wfdesc description and aggregate them in the RO.
     * 
     * @param researchObject
     *            research object URI
     * @param wfdescURI
     *            workflow description URI
     * @throws ROSRSException
     *             when there was a problem with uploading the RO resources
     */
    private void aggregateWorkflowDependencies(URI researchObject, URI wfdescURI)
            throws ROSRSException {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
        readModelFromUri(model, wfdescURI);
        Property wsdlUri = model.createProperty("http://purl.org/wf4ever/wf4ever#wsdlURI");
        List<RDFNode> wsdls = model.listObjectsOfProperty(wsdlUri).toList();
        for (RDFNode wsdl : wsdls) {
            if (wsdl.isLiteral()) {
                if ("http://www.w3.org/2001/XMLSchema#anyURI".equals(wsdl.asLiteral().getDatatypeURI())) {
                    URI wsdlURI = URI.create(wsdl.asLiteral().getString());
                    aggregateResource(researchObject, wsdlURI);
                } else {
                    LOG.error("The WSDL URI was not a Literal, skipping: " + wsdl.toString());
                }
            } else {
                LOG.error("The WSDL URI was not a Literal, skipping: " + wsdl.toString());
            }
        }
    }


    /**
     * Read a model given a URI. RDF/XML is assumed.
     * 
     * @param model
     *            Ont model
     * @param wfdescURI
     *            RDF/XML source URI
     */
    public abstract void readModelFromUri(OntModel model, URI wfdescURI);


    /**
     * Create a new research object or, if it exists, prepare it for uploading workflows, i.e. preserve the previous
     * workflow versions.
     * 
     * @param wfUUID
     *            UUID of the workflow bundle that will be uploaded later.
     * 
     * @return the research object URI
     * @throws ROSRSException
     */
    protected abstract URI createResearchObject(UUID wfUUID)
            throws ROSRSException;


    /**
     * Saves an aggregated resource to RODL.
     * 
     * @param researchObject
     *            research object URI
     * @param path
     *            the resource path
     * @param contentType
     *            resource content type to be sent as in HTTP request
     * @param in
     *            resource input stream
     * @throws IOException
     *             when there are problems with uploading the resource
     * @throws ROSRSException
     */
    protected abstract void uploadAggregatedResource(URI researchObject, String path, InputStream in, String contentType)
            throws IOException, ROSRSException;


    /**
     * Saves an URI as an aggregated resource of an RO in RODL.
     * 
     * @param researchObject
     *            research object URI
     * @param resource
     *            resource URI
     * @throws ROSRSException
     *             when there are problems with uploading the resource
     * @throws ROSRSException
     */
    protected abstract void aggregateResource(URI researchObject, URI resource)
            throws ROSRSException;


    /**
     * Saves a resource in RODL as an annotation body of another resource.
     * 
     * @param researchObject
     *            research object URI
     * @param targets
     *            list of URIs of resources that are annotated
     * @param contentType
     *            content type
     * @param in
     *            resource input stream
     * @return annotation body URI
     * @throws ROSRSException
     *             when there are problems with uploading the resource
     * @throws IOException
     */
    protected abstract URI uploadAnnotation(URI researchObject, List<URI> targets, InputStream in, String contentType)
            throws ROSRSException, IOException;


    /**
     * Create Jena model of the manifest.
     * 
     * @param roURI
     *            research object URI
     * @return the Jena model of the manifest
     */
    protected abstract OntModel createManifestModel(URI roURI);


    /**
     * Return the list of resources that have already been added as a result of the conversion.
     * 
     * @return the list of resource URIs
     */
    public List<URI> getResourcesAdded() {
        return resourcesAdded;
    }

}
