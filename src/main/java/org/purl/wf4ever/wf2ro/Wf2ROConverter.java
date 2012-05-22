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

import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.io.WorkflowBundleIO;
import uk.org.taverna.scufl2.api.io.WriterException;
import uk.org.taverna.scufl2.rdfxml.RDFXMLReader;

import com.hp.hpl.jena.ontology.OntModel;

/**
 * @author piotrekhol
 * 
 *         This class defines the main logic of workflow-RO conversion. It is abstract
 *         because it leaves the resource upload details to its subclasses.
 */
public abstract class Wf2ROConverter
{

	private static final Logger log = Logger.getLogger(Wf2ROConverter.class);

	private static WorkflowBundleIO bundleIO = new WorkflowBundleIO();

	protected URI serviceURI;

	private List<URI> resourcesAdded = Collections.synchronizedList(new ArrayList<URI>());


	public Wf2ROConverter(URI serviceURI)
	{
		this.serviceURI = serviceURI;
	}


	/**
	 * The conversion method. Note that there are no ROSRS parameters, since all ROSRS
	 * handling is delegated to abstract methods of this class.
	 * 
	 * @param wfbundle
	 *            The t2flow/scufl2 workflow that needs to be converted to an RO.
	 * @return list of URIs of resources that have been added to the RO
	 */
	public List<URI> convert(WorkflowBundle wfbundle)
	{
		UUID wfUUID = getWorkflowBundleUUID(wfbundle);
		URI roURI = createResearchObject(wfUUID);
		URI wfURI = addWorkflowBundle(roURI, wfbundle, wfUUID);
		if (wfURI != null) {
			resourcesAdded.add(wfURI);
		}
		URI wfdescURI = addWfDescAnnotation(roURI, wfbundle, wfURI);
		if (wfdescURI != null) {
			resourcesAdded.add(wfdescURI);
		}
		URI evoURI = addRoEvoAnnotation(roURI, wfbundle, wfURI);
		if (evoURI != null) {
			resourcesAdded.add(evoURI);
		}
		return resourcesAdded;
	}


	private UUID getWorkflowBundleUUID(WorkflowBundle wfbundle)
	{
		URI wfbundleURI = wfbundle.getGlobalBaseURI();
		return UUID.fromString(wfbundleURI.resolve("..").relativize(wfbundleURI).toString().split("/")[0]);
	}


	/**
	 * Upload the workflow bundle to RODL.
	 * 
	 * @param roURI
	 *            research object URI
	 * @param wfbundle
	 * @param wfUUID
	 *            workflow bundle UUID
	 * @return the workflow bundle URI as in the manifest
	 */
	protected URI addWorkflowBundle(URI roURI, final WorkflowBundle wfbundle, UUID wfUUID)
	{
		URI wfURI = roURI.resolve(wfUUID.toString());

		try {
			final PipedInputStream in = new PipedInputStream();
			final PipedOutputStream out = new PipedOutputStream(in);
			new Thread(new Runnable() {

				public void run()
				{
					try {
						bundleIO.writeBundle(wfbundle, out, RDFXMLReader.APPLICATION_VND_TAVERNA_SCUFL2_WORKFLOW_BUNDLE);
					}
					catch (WriterException | IOException e) {
						log.error("Can't download workflow bundle", e);
					}
					finally {
						try {
							if (out != null) {
								out.close();
							}
						}
						catch (IOException e) {
							log.warn("Exception when closing the workflow bundle output stream", e);
						}
					}
				}
			}).start();
			uploadAggregatedResource(wfURI, RDFXMLReader.APPLICATION_VND_TAVERNA_SCUFL2_WORKFLOW_BUNDLE, in);
		}
		catch (IOException e) {
			log.error("Can't upload workflow bundle", e);
		}
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
	 * @return
	 */
	protected URI addRoEvoAnnotation(URI roURI, WorkflowBundle wfbundle, URI rodlWfURI)
	{
		// TODO Auto-generated method stub
		return null;
	}


	/**
	 * Generates and adds a workflow bundle description annotation using the wfdesc
	 * ontology.
	 * 
	 * @param roURI
	 *            research object URI
	 * @param wfbundle
	 *            the workflow bundle
	 * @param rodlWfURI
	 *            the workflow bundle URI used in the research object
	 * @return
	 */
	protected URI addWfDescAnnotation(URI roURI, final WorkflowBundle wfbundle, URI rodlWfURI)
	{
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

				public void run()
				{
					try {
						bundleIO.writeBundle(wfbundle, out, "text/vnd.wf4ever.wfdesc+turtle");
					}
					catch (WriterException | IOException e) {
						log.error("Can't download workflow desc", e);
					}
					finally {
						try {
							if (out != null) {
								out.close();
							}
						}
						catch (IOException e) {
							log.warn("Exception when closing the annotation body output stream", e);
						}
					}
				}
			}).start();
			uploadAggregatedResource(annotationBodyURI, "text/turtle", in);
			return annotationBodyURI;
		}
		catch (IOException e) {
			log.error("Can't upload annotation body", e);
			manifest = createManifestModel(roURI);
			ROService.deleteAnnotationFromManifest(manifest, annotationURI);
			uploadManifest(roURI, manifest);
			return null;
		}
	}


	protected URI createAnnotationURI(OntModel manifest, URI roURI)
	{
		return ROService.createAnnotationURI(manifest, roURI);
	}


	protected URI createAnnotationBodyURI(URI roURI, URI rodlWfURI)
	{
		return ROService.createAnnotationBodyURI(roURI, rodlWfURI);
	}


	/**
	 * Create a new research object or, if it exists, prepare it for uploading workflows,
	 * i.e. preserve the previous workflow versions.
	 * 
	 * @param wfUUID
	 *            UUID of the workflow bundle that will be uploaded later.
	 * 
	 * @return the research object URI
	 */
	protected abstract URI createResearchObject(UUID wfUUID);


	/**
	 * Create an output stream to which an aggregated resource can be saved. The output
	 * stream will be closed after being used.
	 * 
	 * @param resourceURI
	 *            resource URI
	 * @return the output stream
	 * @throws IOException
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


	public List<URI> getResourcesAdded()
	{
		return resourcesAdded;
	}

}
