package org.purl.wf4ever.wf2ro;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.purl.wf4ever.rosrs.client.common.Vocab;

import uk.org.taverna.scufl2.api.container.WorkflowBundle;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * 
 * This is a mockup converter that saves all resources in memory
 * 
 * @author piotrekhol
 */
public class MockupWf2ROConverter
	extends Wf2ROConverter
{

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(MockupWf2ROConverter.class);

	private Map<URI, String> resources = new HashMap<>();

	private OntModel manifest = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);

	public static final URI roURI = URI.create("http://example.org/ROs/ro1/");


	public MockupWf2ROConverter(URI serviceURI)
	{
		super(serviceURI);
		Individual ro = manifest.createIndividual(roURI.toString(), Vocab.researchObject);
		Individual m = manifest.createIndividual(roURI.resolve(".ro/manifest.rdf").toString(), Vocab.researchObject);
		m.addProperty(Vocab.describes, ro);
	}


	@Override
	protected void uploadAggregatedResource(URI annotationBodyURI, String contentType, InputStream in)
		throws IOException
	{
		resources.put(annotationBodyURI, IOUtils.toString(in));
	}


	@Override
	protected OntModel createManifestModel(URI roURI)
	{
		return manifest;
	}


	@Override
	protected URI createResearchObject(UUID wfUUID)
	{
		return roURI;
	}


	@Override
	protected URI addWorkflowBundle(URI roURI, WorkflowBundle wfbundle, UUID wfUUID)
		throws IOException
	{
		URI wfURI = super.addWorkflowBundle(roURI, wfbundle, wfUUID);
		Resource ro = manifest.createResource(roURI.toString());
		Individual res = manifest.createIndividual(wfURI.toString(), Vocab.roResource);
		ro.addProperty(Vocab.aggregates, res);
		return wfURI;
	}


	@Override
	protected void uploadManifest(URI roURI, OntModel manifest)
	{
		this.manifest = manifest;
	}


	public Map<URI, String> getResources()
	{
		return resources;
	}

}
