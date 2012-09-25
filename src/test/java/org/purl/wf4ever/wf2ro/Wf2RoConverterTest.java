package org.purl.wf4ever.wf2ro;

import java.io.IOException;
import java.io.InputStream;

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

/**
 * The test verifies correct conversion using a mockup converter.
 * 
 * @author piotrekhol
 * 
 */
public class Wf2RoConverterTest {

    /** Workflow name, in src/test/resources. */
    private static final String HELLO_ANYONE_T2FLOW = "helloanyone.t2flow";

    /** Workflow name, in src/test/resources. */
    private static final String WF2470_T2FLOW = "workflow2470.t2flow";


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
        InputStream helloWorld = getClass().getClassLoader().getResourceAsStream(HELLO_ANYONE_T2FLOW);
        WorkflowBundle wfbundle = io.readBundle(helloWorld, null);

        MockupWf2ROConverter converter = new MockupWf2ROConverter(wfbundle);
        converter.convert();

        OntModel model = converter.createManifestModel(null);
        Individual ro = model.getIndividual(converter.createResearchObject(null).toString());
        Assert.assertNotNull("RO exists in the manifest", ro);
        // should aggregate the workflow, 2 annotations about it and 2 annotation bodies
        NodeIterator it = ro.listPropertyValues(Vocab.ORE_AGGREGATES);
        for (int i = 0; i < 5; i++) {
            Assert.assertTrue("RO aggregates 5x wf or annotation (" + i + ")", it.hasNext());
            RDFNode node = it.next();
            Assert.assertTrue(node.isURIResource());
            Individual ind = node.as(Individual.class);
            Assert.assertTrue("Wf or annotation",
                ind.hasRDFType(Vocab.RO_RESOURCE) || ind.hasRDFType(Vocab.RO_AGGREGATED_ANNOTATION));
        }
        System.out.println(converter.getResources());
        Assert.assertFalse("RO aggregates 3x wf or annotation", it.hasNext());
    }
}
