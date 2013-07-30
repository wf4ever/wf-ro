package org.purl.wf4ever.wf2ro;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Logger;
import org.purl.wf4ever.rosrs.client.exception.ROSRSException;

import pl.psnc.dl.wf4ever.vocabulary.AO;
import pl.psnc.dl.wf4ever.vocabulary.ORE;
import pl.psnc.dl.wf4ever.vocabulary.RO;
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

    /** a constant for the bodies of annotations extracted from the t2flow. */
    public static final String[] BODY_WF_ANNOTATIONS = { "http://example.org/ROs/ro1/.ro/body-wf-1",
            "http://example.org/ROs/ro1/.ro/body-wf-2", "http://example.org/ROs/ro1/.ro/body-wf-3" };

    /** a constant for the resource which has the link between the workflow bundle and the main workflow. */
    public static final String BODY_LINK_WFDEF = "http://example.org/ROs/ro1/.ro/body-link-6";

    /** Logger. */
    private static final Logger LOGGER = Logger.getLogger(MockupWf2ROConverter.class);

    /** map to hold resources. */
    private Map<URI, String> resources = new HashMap<>();

    /** a fake manifest model. */
    private OntModel manifest = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);

    /** RO URI. */
    public static final URI RO_URI = URI.create("http://example.org/ROs/ro1/");

    /** Used for ann bodies. */
    private int annCnt = 0;

    /** Resources expected to be generated. */
    public static final List<String> EXPECTED_RESOURCES = Arrays.asList(
        "http://example.org/ROs/ro1/Hello_Anyone.wfbundle", "http://example.org/ROs/ro1/.ro/body-wf-1",
        "http://example.org/ROs/ro1/.ro/body-wf-2", "http://example.org/ROs/ro1/.ro/body-wf-3",
        "http://example.org/ROs/ro1/.ro/body-wfdesc-4", "http://example.org/ROs/ro1/.ro/body-roevo-5", BODY_LINK_WFDEF);

    /** Annotations expected to be generated. */
    public static final List<String> EXPECTED_ANNOTATIONS = Arrays.asList("http://example.org/ROs/ro1/.ro/ann-wf-1",
        "http://example.org/ROs/ro1/.ro/ann-wf-2", "http://example.org/ROs/ro1/.ro/ann-wf-3",
        "http://example.org/ROs/ro1/.ro/ann-wfdesc-4", "http://example.org/ROs/ro1/.ro/ann-roevo-5",
        "http://example.org/ROs/ro1/.ro/ann-link-6");


    /**
     * Folder entry.
     * 
     * @author piotrekhol
     * 
     */
    class FolderEntry {

        /** ore:proxyFor. */
        private URI proxyFor;

        /** ro:entryName. */
        private String name;


        /**
         * Constructor.
         * 
         * @param proxyFor
         *            ore:proxyFor
         * @param name
         *            ro:entryName
         */
        public FolderEntry(URI proxyFor, String name) {
            this.proxyFor = proxyFor;
            this.name = name;
        }


        public URI getProxyFor() {
            return proxyFor;
        }


        public String getName() {
            return name;
        }


        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FolderEntry)) {
                return false;
            }
            FolderEntry other = (FolderEntry) obj;
            return other.proxyFor.equals(proxyFor) && ((other.name == null && name == null) || other.name.equals(name));
        }


        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 31).append(name).append(proxyFor).toHashCode();
        }


        @Override
        public String toString() {
            return String.format("[Proxy for: %s, name: %s]", proxyFor, name);
        }
    }


    /**
     * Constructor.
     * 
     * @param wfbundle
     *            workflow bundle
     * @param wfUri
     *            workflow URI
     */
    public MockupWf2ROConverter(WorkflowBundle wfbundle, URI wfUri) {
        super(wfbundle, wfUri);
        Individual ro = manifest.createIndividual(RO_URI.toString(), RO.ResearchObject);
        Individual m = manifest.createIndividual(RO_URI.resolve(".ro/manifest.rdf").toString(), RO.ResearchObject);
        m.addProperty(ORE.describes, ro);
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
    protected URI addWorkflowBundle(URI roURI, WorkflowBundle wfbundle, String wfUUID)
            throws IOException, ROSRSException, WriterException {
        URI wfURI = super.addWorkflowBundle(roURI, wfbundle, wfUUID);
        Resource ro = manifest.createResource(roURI.toString());
        Individual res = manifest.createIndividual(wfURI.toString(), RO.Resource);
        ro.addProperty(ORE.aggregates, res);
        return wfURI;
    }


    public Map<URI, String> getResources() {
        return resources;
    }


    @Override
    protected void uploadAggregatedResource(URI researchObject, String path, InputStream in, String contentType)
            throws IOException {
        URI uri = researchObject.resolve(path);
        resources.put(uri, IOUtils.toString(in));
        Resource ro = manifest.createResource(researchObject.toString());
        Individual res = manifest.createIndividual(uri.toString(), RO.Resource);
        ro.addProperty(ORE.aggregates, res);
    }


    @Override
    protected URI uploadAnnotation(URI researchObject, String name, Set<URI> targets, InputStream in, String contentType) {
        annCnt++;
        URI ann = researchObject.resolve(".ro/ann-" + name + "-" + annCnt);
        URI body = researchObject.resolve(".ro/body-" + name + "-" + annCnt);
        try {
            resources.put(body, IOUtils.toString(in));
        } catch (IOException e) {
            LOGGER.error(e);
        }
        Resource ro = manifest.createResource(researchObject.toString());
        Individual res = manifest.createIndividual(ann.toString(), RO.AggregatedAnnotation);
        Resource bodyInd = manifest.createResource(body.toString());
        ro.addProperty(ORE.aggregates, res);
        res.addProperty(AO.body, bodyInd);
        Individual res2 = manifest.createIndividual(body.toString(), RO.Resource);
        ro.addProperty(ORE.aggregates, res2);
        return ann;
    }


    @Override
    protected void aggregateResource(URI researchObject, URI resource) {
        resources.put(resource, null);
        Resource ro = manifest.createResource(researchObject.toString());
        Individual res = manifest.createIndividual(resource.toString(), RO.Resource);
        ro.addProperty(ORE.aggregates, res);
    }


    @Override
    public void readModelFromUri(OntModel model, URI wfdescURI) {
        Individual res = manifest.getIndividual(wfdescURI.toString());
        URI body = URI.create(res.getPropertyResourceValue(AO.body).getURI());

        model.read(new ByteArrayInputStream(resources.get(body).getBytes()), null, "TURTLE");
    }

}
