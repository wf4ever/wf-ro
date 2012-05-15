/**
 * 
 */
package org.purl.wf4ever.wf2ro;

import java.io.OutputStream;
import java.net.URI;
import java.util.UUID;

import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.core.Workflow;
import uk.org.taverna.scufl2.wfdesc.WfdescSerialiser;

import com.hp.hpl.jena.ontology.OntModel;

/**
 * @author piotrekhol
 * 
 */
public abstract class Wf2ROConverter
{

	public void convert(WorkflowBundle wfbundle)
	{
		for (Workflow workflow : wfbundle.getWorkflows()) {
			URI wfURI = workflow.getWorkflowIdentifier();
			UUID wfUUID = UUID.fromString(wfURI.resolve("..").relativize(wfURI).toString().split("/")[0]);
			URI rodlWfURI = addWorkflow(wfUUID);
			deleteServiceAnnotations(rodlWfURI);
			addWfDescAnnotation(workflow, rodlWfURI);
			addRoEvoAnnotation(workflow, rodlWfURI);
		}
	}


	protected void addRoEvoAnnotation(Workflow workflow, URI rodlWfURI)
	{
		// TODO Auto-generated method stub

	}


	protected void addWfDescAnnotation(Workflow workflow, URI rodlWfURI)
	{
		URI roURI = getROURI();
		OntModel manifest = createManifestModel(roURI);
		URI annotationBodyURI = generateAnnotationBodyURI(roURI, rodlWfURI);
		addAnnotation(roURI, manifest, rodlWfURI, annotationBodyURI);
		uploadManifest(roURI, manifest);

		OutputStream out = createAnnotationBodyOutputStream(annotationBodyURI);
		WfdescSerialiser serializer = new WfdescSerialiser();
		//		serializer.save(wfBundle, out);
	}


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


	protected void uploadManifest(URI roURI, OntModel manifest)
	{
		// TODO Auto-generated method stub

	}


	protected void deleteServiceAnnotations(URI rodlWfURI)
	{
	}


	protected void addAnnotation(URI roURI, OntModel manifest, URI rodlWfURI, URI annotationBodyURI)
	{
		// TODO Auto-generated method stub

	}


	protected abstract URI getROURI();


	protected abstract URI addWorkflow(UUID wfUUID);


	protected abstract OutputStream createAnnotationBodyOutputStream(URI annotationBodyURI);


	protected abstract OntModel createManifestModel(URI roURI);
}
