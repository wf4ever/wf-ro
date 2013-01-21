package org.purl.wf4ever.wf2ro;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.purl.wf4ever.wf2ro.MockupWf2ROConverter.FolderEntry;

import pl.psnc.dl.wf4ever.vocabulary.ORE;
import pl.psnc.dl.wf4ever.vocabulary.RO;
import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.io.ReaderException;
import uk.org.taverna.scufl2.api.io.WorkflowBundleIO;

import com.google.common.collect.Multimap;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * The test verifies correct conversion using a mockup converter.
 * 
 * @author piotrekhol
 * 
 */
public class Wf2RoConverterTest {

    private static final String HELLO_ANYONE_WFBUNDLE = "http://example.org/ROs/ro1/folder1/Hello_Anyone.wfbundle";

	private static final String HAS_WF_DEF = "http://purl.org/wf4ever/wfdesc#hasWorkflowDefinition";

	private static final String WF_URI = "http://ns.taverna.org.uk/2010/workflowBundle/01348671-5aaa-4cc2-84cc-477329b70b0d/workflow/Hello_Anyone/";

	private static final String TURTLE = "TURTLE";

	/** Workflow name, in src/test/resources. */
    private static final String HELLO_ANYONE_T2FLOW = "helloanyone.t2flow";

    /** Workflow name, in src/test/resources. */
    @SuppressWarnings("unused")
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

        MockupWf2ROConverter converter = new MockupWf2ROConverter(wfbundle, URI.create(HELLO_ANYONE_T2FLOW));
        converter.convert();
        System.out.println(converter.getResources().keySet());
        Assert.assertEquals(MockupWf2ROConverter.EXPECTED_ANNOTATIONS.size() + 1
                + MockupWf2ROConverter.EXPECTED_FOLDERS.size(), converter.getResourcesAdded().size());

        OntModel model = converter.createManifestModel(null);
        Individual ro = model.getIndividual(converter.createResearchObject(null).toString());
        Assert.assertNotNull("RO exists in the manifest", ro);
        // should aggregate the workflow, 2 annotations about it and 2 annotation bodies
        List<RDFNode> aggregatedResources = ro.listPropertyValues(ORE.aggregates).toList();
        System.out.println(aggregatedResources);
        Assert.assertEquals("Correct number of aggregated resources", MockupWf2ROConverter.EXPECTED_RESOURCES.size()
                + MockupWf2ROConverter.EXPECTED_ANNOTATIONS.size(), aggregatedResources.size());
        for (RDFNode node : aggregatedResources) {
            Assert.assertTrue(node.isURIResource());
            Individual ind = node.as(Individual.class);
            Assert.assertTrue("Wf or annotation",
                ind.hasRDFType(RO.Resource) || ind.hasRDFType(RO.AggregatedAnnotation));
            if (ind.hasRDFType(RO.Resource)) {
                Assert.assertTrue("Path " + ind.getURI() + " is expected",
                    MockupWf2ROConverter.EXPECTED_RESOURCES.contains(ind.getURI()));
            } else {
                Assert.assertTrue("Path " + ind.getURI() + " is expected",
                    MockupWf2ROConverter.EXPECTED_ANNOTATIONS.contains(ind.getURI()));
            }
        }
        
        
        checkHasWorkflowDefinition(converter);
        
        List<URI> folders = converter.getFolders();
        Assert.assertEquals(MockupWf2ROConverter.EXPECTED_FOLDERS.size(), folders.size());
        for (URI uri : MockupWf2ROConverter.EXPECTED_FOLDERS) {
            Assert.assertTrue(folders.contains(uri));
        }
        Multimap<URI, FolderEntry> entries = converter.getEntries();
        Assert.assertEquals(MockupWf2ROConverter.EXPECTED_ENTRIES.size(), entries.size());
        for (Map.Entry<URI, MockupWf2ROConverter.FolderEntry> e : MockupWf2ROConverter.EXPECTED_ENTRIES.entries()) {
            FolderEntry expected = e.getValue();
            Collection<FolderEntry> found = entries.get(e.getKey());
            Assert.assertNotNull(found);
            Assert.assertTrue(found.toString() + " contains " + expected.toString(), found.contains(expected));
        }
    }


	protected void checkHasWorkflowDefinition(MockupWf2ROConverter converter) {
		String hasWorkflowDefBody = converter.getResources().get(
				URI.create(MockupWf2ROConverter.BODY_LINK_WFDEF));
		// System.out.println(hasWorkflowDefBody);
		OntModel hasWorkflowDefModel = ModelFactory
				.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
		hasWorkflowDefModel.read(new StringReader(hasWorkflowDefBody),
				MockupWf2ROConverter.RO_URI.toASCIIString(), TURTLE);
		Property hasWfDef = hasWorkflowDefModel.getProperty(HAS_WF_DEF);
		Individual wf = hasWorkflowDefModel.getIndividual(WF_URI);
		assertNotNull("Could not find Workflow " + WF_URI, wf);
		Resource wfDef = wf.getPropertyResourceValue(hasWfDef);
		assertEquals(HELLO_ANYONE_WFBUNDLE, wfDef.getURI());
	}
}
