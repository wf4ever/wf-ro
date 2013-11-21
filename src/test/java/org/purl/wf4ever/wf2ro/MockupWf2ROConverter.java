package org.purl.wf4ever.wf2ro;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Logger;
import org.purl.wf4ever.rosrs.client.Annotable;
import org.purl.wf4ever.rosrs.client.Annotation;
import org.purl.wf4ever.rosrs.client.Folder;
import org.purl.wf4ever.rosrs.client.ResearchObject;
import org.purl.wf4ever.rosrs.client.exception.ROException;
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

    private ResearchObject ro;

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


    /**
     * Just for tests.
     * 
     * @return Jena model
     */
    public OntModel createManifestModel() {
        return manifest;
    }


    @Override
    protected ResearchObject createResearchObject(UUID wfUUID) {
        ro = new ResearchObject(RO_URI, null);
        return ro;
    }

    protected ResearchObject getRO() {
        if (ro == null) {
            return createResearchObject(UUID.randomUUID());
        }
        return ro;
    }

    @Override
    protected org.purl.wf4ever.rosrs.client.Resource addWorkflowBundle(ResearchObject ro,
            final WorkflowBundle wfbundle, String wfPath)
            throws IOException, ROSRSException, WriterException, ROException {
        org.purl.wf4ever.rosrs.client.Resource wf = super.addWorkflowBundle(ro, wfbundle, wfPath);
        Resource roR = manifest.createResource(ro.getUri().toString());
        Individual res = manifest.createIndividual(wf.getUri().toString(), RO.Resource);
        roR.addProperty(ORE.aggregates, res);
        return wf;
    }


    public Map<URI, String> getResources() {
        return resources;
    }


    @Override
    protected org.purl.wf4ever.rosrs.client.Resource uploadAggregatedResource(ResearchObject ro, String path,
            InputStream in, String contentType)
            throws IOException, ROSRSException, ROException {
        URI uri = ro.getUri().resolve(path);
        resources.put(uri, IOUtils.toString(in));
        Resource roR = manifest.createResource(ro.toString());
        Individual res = manifest.createIndividual(uri.toString(), RO.Resource);
        roR.addProperty(ORE.aggregates, res);
        return new org.purl.wf4ever.rosrs.client.Resource(ro, uri, null, null, null);
    }


    @Override
    protected Annotation uploadAnnotation(ResearchObject ro, String name, Annotable target, InputStream in,
            String contentType)
            throws ROSRSException, ROException {
        annCnt++;
        URI ann = ro.getUri().resolve(".ro/ann-" + name + "-" + annCnt);
        URI body = ro.getUri().resolve(".ro/body-" + name + "-" + annCnt);
        try {
            resources.put(body, IOUtils.toString(in));
        } catch (IOException e) {
            LOGGER.error(e);
        }
        Resource roR = manifest.createResource(ro.getUri().toString());
        Individual res = manifest.createIndividual(ann.toString(), RO.AggregatedAnnotation);
        Resource bodyInd = manifest.createResource(body.toString());
        roR.addProperty(ORE.aggregates, res);
        res.addProperty(AO.body, bodyInd);
        Individual res2 = manifest.createIndividual(body.toString(), RO.Resource);
        roR.addProperty(ORE.aggregates, res2);
        return new Annotation(ro, ann, null, Collections.singleton(body), null, null);
    }


    @Override
    public Folder getExtractMain() {        
        return null;
    }


    @Override
    public Folder getExtractNested() {
        return null;
    }


    @Override
    public Folder getExtractScripts() {
        return null;
    }


    @Override
    public Folder getExtractServices() {
        return null;
    }

}
