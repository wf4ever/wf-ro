/**
 * 
 */
package org.purl.wf4ever.wf2ro;

import java.io.OutputStream;
import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;

import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.io.WriterException;
import uk.org.taverna.scufl2.wfdesc.WfdescSerialiser;

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


	/**
	 * The conversion method. Note that there are no ROSRS parameters, since all ROSRS
	 * handling is delegated to abstract methods of this class.
	 * 
	 * @param wfbundle
	 *            The t2flow/scufl2 workflow that needs to be converted to an RO.
	 */
	public void convert(WorkflowBundle wfbundle)
	{
		UUID wfUUID = getWorkflowBundleUUID(wfbundle);
		URI roURI = createResearchObject(wfUUID);
		URI wfURI = addWorkflowBundle(roURI, wfUUID);
		addWfDescAnnotation(roURI, wfbundle, wfURI);
		addRoEvoAnnotation(roURI, wfbundle, wfURI);
	}


	private UUID getWorkflowBundleUUID(WorkflowBundle wfbundle)
	{
		URI wfbundleURI = wfbundle.getGlobalBaseURI();
		return UUID.fromString(wfbundleURI.resolve("..").relativize(wfbundleURI).toString().split("/")[0]);
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
	 */
	protected void addRoEvoAnnotation(URI roURI, WorkflowBundle wfbundle, URI rodlWfURI)
	{
		// TODO Auto-generated method stub

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
	 */
	protected void addWfDescAnnotation(URI roURI, WorkflowBundle wfbundle, URI rodlWfURI)
	{
		OntModel manifest = createManifestModel(roURI);
		URI annotationBodyURI = generateAnnotationBodyURI(roURI, rodlWfURI);
		URI annotationURI = addAnnotation(roURI, manifest, rodlWfURI, annotationBodyURI);
		uploadManifest(roURI, manifest);

		OutputStream out = createAnnotationBodyOutputStream(annotationBodyURI);
		WfdescSerialiser serializer = new WfdescSerialiser();
		try {
			serializer.save(wfbundle, out);
		}
		catch (WriterException e) {
			log.error("Can't write wfdesc description", e);

			manifest = createManifestModel(roURI);
			deleteAnnotation(roURI, manifest, annotationURI);
			uploadManifest(roURI, manifest);
		}
	}


	/**
	 * Add an annotation to the manifest. You need to add the annotation body separately,
	 * after uploading the manifest to RODL.
	 * 
	 * In the future should be done by the RODL and supported via ROSR API.
	 * 
	 * @param roURI
	 *            research object URI
	 * @param manifest
	 *            the Jena model of the manifest
	 * @param rodlWfURI
	 *            workflow bundle URI as in the manifest. This will be the annotation
	 *            target.
	 * @param annotationBodyURI
	 *            the URI of the annotation body that will be uploaded later
	 * @return the annotation URI
	 */
	protected URI addAnnotation(URI roURI, OntModel manifest, URI rodlWfURI, URI annotationBodyURI)
	{
		// TODO Auto-generated method stub
		return null;
	}


	/**
	 * Delete the annotation from the manifest. Does not delete the annotation body.
	 * 
	 * In the future should be done by the RODL and supported via ROSR API.
	 * 
	 * @param roURI
	 *            research object URI
	 * @param manifest
	 *            the Jena model of the manifest
	 * @param annotationURI
	 *            the annotation URI
	 */
	protected void deleteAnnotation(URI roURI, OntModel manifest, URI annotationURI)
	{
		// TODO Auto-generated method stub

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
	 * Upload the workflow bundle to RODL.
	 * 
	 * @param roURI
	 *            research object URI
	 * @param wfUUID
	 *            workflow bundle UUID
	 * @return the workflow bundle URI as in the manifest
	 */
	protected abstract URI addWorkflowBundle(URI roURI, UUID wfUUID);


	/**
	 * Create an output stream to which an annotation body can be saved. The output stream
	 * will be closed after being used.
	 * 
	 * @param annotationBodyURI
	 *            annotation body URI
	 * @return the output stream
	 */
	protected abstract OutputStream createAnnotationBodyOutputStream(URI annotationBodyURI);


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
	 * Generate a URI for an annotation body of a resource. The URI template is
	 * ["ro"|resource_name] + "-" + random_string.
	 * 
	 * @param roURI
	 *            research object URI
	 * @param targetURI
	 *            the annotation body target URI
	 * @return an annotation body URI
	 */
	protected static URI generateAnnotationBodyURI(URI roURI, URI targetURI)
	{
		String targetName;
		if (targetURI.equals(roURI))
			targetName = "ro";
		else
			targetName = targetURI.resolve(".").relativize(targetURI).toString();
		String randomBit = "" + Math.abs(UUID.randomUUID().getLeastSignificantBits());

		return roURI.resolve(".ro/" + targetName + "-" + randomBit + ".rdf");
	}
}
