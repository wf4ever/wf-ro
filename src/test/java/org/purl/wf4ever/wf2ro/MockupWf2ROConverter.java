package org.purl.wf4ever.wf2ro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.purl.wf4ever.rosrs.client.common.Vocab;

import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.io.WorkflowBundleIO;
import uk.org.taverna.scufl2.api.io.WriterException;
import uk.org.taverna.scufl2.rdfxml.RDFXMLReader;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class MockupWf2ROConverter
	extends Wf2ROConverter
{

	private static final Logger log = Logger.getLogger(MockupWf2ROConverter.class);

	private Map<URI, OutputStream> resources = new HashMap<>();

	private WorkflowBundleIO io = new WorkflowBundleIO();

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
	protected OutputStream createAnnotationBodyOutputStream(URI annotationBodyURI)
	{
		resources.put(annotationBodyURI, new ByteArrayOutputStream());
		return resources.get(annotationBodyURI);
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
	{
		URI wfURI = roURI.resolve(wfUUID.toString());
		OutputStream out = new ByteArrayOutputStream();
		try {
			io.writeBundle(wfbundle, out, RDFXMLReader.APPLICATION_VND_TAVERNA_SCUFL2_WORKFLOW_BUNDLE);
		}
		catch (WriterException | IOException e) {
			log.error("Can't save the workflow bundle", e);
		}
		resources.put(wfURI, out);
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


	public Map<URI, OutputStream> getResources()
	{
		return resources;
	}

}
