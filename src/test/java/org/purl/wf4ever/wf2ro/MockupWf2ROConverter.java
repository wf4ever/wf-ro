package org.purl.wf4ever.wf2ro;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.purl.wf4ever.rosrs.client.common.ROSRSException;
import org.purl.wf4ever.rosrs.client.common.Vocab;

import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.io.WriterException;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * This is a mockup converter that saves all resources in memory.
 * 
 * @author piotrekhol
 */
public class MockupWf2ROConverter extends Wf2ROConverter {

    /** map to hold resources. */
    private Map<URI, String> resources = new HashMap<>();

    /** a fake manifest model. */
    private OntModel manifest = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);

    /** RO URI. */
    public static final URI RO_URI = URI.create("http://example.org/ROs/ro1/");


    /**
     * Constructor.
     * 
     * @param wfbundle
     *            workflow bundle
     */
    public MockupWf2ROConverter(WorkflowBundle wfbundle) {
        super(wfbundle);
        Individual ro = manifest.createIndividual(RO_URI.toString(), Vocab.RO_RESEARCH_OBJECT);
        Individual m = manifest.createIndividual(RO_URI.resolve(".ro/manifest.rdf").toString(),
            Vocab.RO_RESEARCH_OBJECT);
        m.addProperty(Vocab.ORE_DESCRIBES, ro);
    }


    @Override
    protected OntModel createManifestModel(URI roURI) {
        return manifest;
    }


    @Override
    protected URI createResearchObject(UUID wfUUID) {
        return RO_URI;
    }


    @Override
    protected URI addWorkflowBundle(URI roURI, WorkflowBundle wfbundle, UUID wfUUID)
            throws IOException, ROSRSException, WriterException {
        URI wfURI = super.addWorkflowBundle(roURI, wfbundle, wfUUID);
        Resource ro = manifest.createResource(roURI.toString());
        Individual res = manifest.createIndividual(wfURI.toString(), Vocab.RO_RESOURCE);
        ro.addProperty(Vocab.ORE_AGGREGATES, res);
        return wfURI;
    }


    public Map<URI, String> getResources() {
        return resources;
    }


    @Override
    protected void uploadAggregatedResource(URI researchObject, String path, InputStream in, String contentType)
            throws IOException {
    }


    @Override
    protected URI uploadAnnotation(URI researchObject, List<URI> targets, InputStream in, String contentType)
            throws IOException {
        URI ann = researchObject.resolve(".ro/ann-" + UUID.randomUUID().toString());
        URI body = researchObject.resolve(".ro/body-" + UUID.randomUUID().toString());
        resources.put(body, IOUtils.toString(in));
        Resource ro = manifest.createResource(researchObject.toString());
        Individual res = manifest.createIndividual(ann.toString(), Vocab.RO_AGGREGATED_ANNOTATION);
        Resource bodyInd = manifest.createResource(body.toString());
        ro.addProperty(Vocab.ORE_AGGREGATES, res);
        res.addProperty(Vocab.AO_BODY, bodyInd);
        Individual res2 = manifest.createIndividual(body.toString(), Vocab.RO_RESOURCE);
        ro.addProperty(Vocab.ORE_AGGREGATES, res2);
        return ann;
    }


    @Override
    protected void aggregateResource(URI researchObject, URI resource) {
        resources.put(resource, null);
        Resource ro = manifest.createResource(researchObject.toString());
        Individual res = manifest.createIndividual(resource.toString(), Vocab.RO_RESOURCE);
        ro.addProperty(Vocab.ORE_AGGREGATES, res);
    }


    @Override
    public void readModelFromUri(OntModel model, URI wfdescURI) {
        Individual res = manifest.getIndividual(wfdescURI.toString());
        URI body = URI.create(res.getPropertyResourceValue(Vocab.AO_BODY).getURI());

        model.read(new ByteArrayInputStream(resources.get(body).getBytes()), null);
    }

}
