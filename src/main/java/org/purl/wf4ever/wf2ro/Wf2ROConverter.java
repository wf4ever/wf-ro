/**
 * 
 */
package org.purl.wf4ever.wf2ro;

import java.net.URI;
import java.util.UUID;

import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.core.Workflow;

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
		OntModel manifest = createManifestModel();
		URI annotationBodyURI = generateAnnotationBodyURI(roURI);
		addAnnotation(manifest, roURI, rodlWfURI, annotationBodyURI);

	}


	private static URI generateAnnotationBodyURI(URI roURI)
	{
		// TODO Auto-generated method stub
		return null;
	}


	protected void deleteServiceAnnotations(URI rodlWfURI)
	{
	}


	protected void addAnnotation(OntModel manifest, URI roURI, URI rodlWfURI, URI annotationBodyURI)
	{
		// TODO Auto-generated method stub

	}


	protected abstract URI getROURI();


	protected abstract URI addWorkflow(UUID wfUUID);


	protected abstract OntModel createManifestModel();
}
