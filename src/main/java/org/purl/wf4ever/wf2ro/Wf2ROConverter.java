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
			UUID wfUUID = UUID.fromString(wfURI.resolve("..").relativize(wfURI).toASCIIString().replace("/", ""));
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
		// TODO Auto-generated method stub

	}


	protected void deleteServiceAnnotations(URI rodlWfURI)
	{
		OntModel manifest = createManifestModel();
	}


	protected abstract URI addWorkflow(UUID wfUUID);


	protected abstract OntModel createManifestModel();
}
