package org.purl.wf4ever.wf2ro;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.purl.wf4ever.rosrs.client.common.Vocab;

import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.io.ReaderException;
import uk.org.taverna.scufl2.api.io.WorkflowBundleIO;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
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
        System.out.println(converter.getResources().keySet());

        OntModel model = converter.createManifestModel(null);
        Individual ro = model.getIndividual(converter.createResearchObject(null).toString());
        Assert.assertNotNull("RO exists in the manifest", ro);
        // should aggregate the workflow, 2 annotations about it and 2 annotation bodies
        List<String> resources = Arrays
                .asList(
                    "http://example.org/ROs/ro1/01348671-5aaa-4cc2-84cc-477329b70b0d",
                    "http://example.org/ROs/ro1/01348671-5aaa-4cc2-84cc-477329b70b0d/profile/taverna-2.2.0.rdf",
                    "http://example.org/ROs/ro1/01348671-5aaa-4cc2-84cc-477329b70b0d/META-INF/container.xml",
                    "http://example.org/ROs/ro1/01348671-5aaa-4cc2-84cc-477329b70b0d/history/01348671-5aaa-4cc2-84cc-477329b70b0d.t2flow",
                    "http://example.org/ROs/ro1/01348671-5aaa-4cc2-84cc-477329b70b0d/workflow/Hello_Anyone.rdf",
                    "http://example.org/ROs/ro1/01348671-5aaa-4cc2-84cc-477329b70b0d/mimetype",
                    "http://example.org/ROs/ro1/01348671-5aaa-4cc2-84cc-477329b70b0d/workflowBundle.rdf",
                    "http://example.org/ROs/ro1/01348671-5aaa-4cc2-84cc-477329b70b0d/META-INF/manifest.xml",
                    "http://example.org/ROs/ro1/.ro/body-1", "http://example.org/ROs/ro1/.ro/body-2",
                    "http://example.org/ROs/ro1/.ro/ann-1", "http://example.org/ROs/ro1/.ro/ann-2");
        List<RDFNode> aggregatedResources = ro.listPropertyValues(Vocab.ORE_AGGREGATES).toList();
        System.out.println(aggregatedResources);
        Assert.assertEquals("Correct number of aggregated resources", resources.size(), aggregatedResources.size());
        for (RDFNode node : aggregatedResources) {
            Assert.assertTrue(node.isURIResource());
            Individual ind = node.as(Individual.class);
            Assert.assertTrue("Wf or annotation",
                ind.hasRDFType(Vocab.RO_RESOURCE) || ind.hasRDFType(Vocab.RO_AGGREGATED_ANNOTATION));
            Assert.assertTrue("Path " + ind.getURI() + " is expected", resources.contains(ind.getURI()));
        }
    }
}
