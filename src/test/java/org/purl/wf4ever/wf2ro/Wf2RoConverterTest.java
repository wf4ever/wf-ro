package org.purl.wf4ever.wf2ro;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import junit.framework.Assert;

import org.junit.Test;
import org.purl.wf4ever.rosrs.client.common.Vocab;

import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.io.ReaderException;
import uk.org.taverna.scufl2.api.io.WorkflowBundleIO;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.DCTerms;

/**
 * The test verifies correct conversion using a mockup converter.
 * 
 * @author piotrekhol
 * 
 */
public class Wf2RoConverterTest {

    /** Workflow name, in src/test/resources. */
    private static final String HELLO_WORLD_T2FLOW = "helloworld.t2flow";

    /** Workflow UUID. */
    private static final String WF_UUID = "8781d5f4-d0ba-48a8-a1d1-14281bd8a917";

    /** Service URI. */
    private static final URI SERVICE_URI = URI.create("http://transformers");


    /**
     * A simple test of the conversion logic.
     * 
     * @throws ReaderException
     *             when the workflow couldn't be read
     * @throws IOException
     *             when the workflow couldn't be read
     */
    @Test
    public void testConvert()
            throws ReaderException, IOException {
        WorkflowBundleIO io = new WorkflowBundleIO();
        InputStream helloWorld = getClass().getClassLoader().getResourceAsStream(HELLO_WORLD_T2FLOW);
        WorkflowBundle wfbundle = io.readBundle(helloWorld, null);

        Wf2ROConverter converter = new MockupWf2ROConverter(SERVICE_URI, wfbundle);
        converter.convert();

        URI wfURI = converter.createResearchObject(null).resolve(WF_UUID);

        OntModel model = converter.createManifestModel(null);
        Individual ro = model.getIndividual(converter.createResearchObject(null).toString());
        Assert.assertNotNull("RO exists in the manifest", ro);
        // should aggregate the workflow and 2 annotations about it
        NodeIterator it = ro.listPropertyValues(Vocab.aggregates);
        // 2 because the ro evo has not been added yet
        for (int i = 0; i < /*3*/2; i++) {
            Assert.assertTrue("RO aggregates 3x wf or annotation (" + i + ")", it.hasNext());
            RDFNode node = it.next();
            Assert.assertTrue(node.isURIResource());
            Individual ind = node.as(Individual.class);
            Assert.assertTrue("Wf or annotation",
                ind.hasRDFType(Vocab.roResource) || ind.hasRDFType(Vocab.aggregatedAnnotation));
            if (ind.hasRDFType(Vocab.roResource)) {
                Assert.assertEquals("Wf URI is correct", wfURI.toString(), ind.getURI());
                //TODO validate that the serialized wf is correct
            } else {
                RDFNode targetNode = ind.getPropertyValue(Vocab.annotatesAggregatedResource);
                Assert.assertTrue(targetNode.isURIResource());
                Assert.assertEquals("Annotation target URI is correct", wfURI.toString(), targetNode.asResource()
                        .getURI());
                RDFNode authorNode = ind.getPropertyValue(DCTerms.creator);
                Assert.assertTrue(authorNode.isURIResource());
                Assert.assertEquals("Annotation creator URI is correct", SERVICE_URI.toString(), authorNode
                        .asResource().getURI());
                //TODO verify that the annotation body is correct
            }
        }
        Assert.assertFalse("RO aggregates 3x wf or annotation", it.hasNext());
    }
}
