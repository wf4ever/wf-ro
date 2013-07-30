package org.purl.wf4ever.wf2ro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.List;

import org.junit.Test;
import org.openrdf.rio.RDFFormat;

import pl.psnc.dl.wf4ever.vocabulary.ORE;
import pl.psnc.dl.wf4ever.vocabulary.RO;
import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.io.WorkflowBundleIO;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;

/**
 * The test verifies correct conversion using a mockup converter.
 * 
 * @author piotrekhol
 * 
 */
public class Wf2RoConverterTest {

    /** workflow bundle URI. */
    private static final String HELLO_ANYONE_WFBUNDLE = "http://example.org/ROs/ro1/Hello_Anyone.wfbundle";

    /** the link between the workflow bundle and the main workflow. */
    private static final String HAS_WF_DEF = "http://purl.org/wf4ever/wfdesc#hasWorkflowDefinition";

    /** the main worfklow URI. */
    private static final String WF_URI = "http://ns.taverna.org.uk/2010/workflowBundle/01348671-5aaa-4cc2-84cc-477329b70b0d/workflow/Hello_Anyone/";

    /** Workflow name, in src/test/resources. */
    private static final String HELLO_ANYONE_T2FLOW = "helloanyone.t2flow";


    /**
     * A simple test of the conversion logic.
     * 
     * @throws Exception
     *             any kind of conversion exception
     */
    @Test
    public void testConvert()
            throws Exception {
        WorkflowBundleIO io = new WorkflowBundleIO();
        InputStream helloWorld = getClass().getClassLoader().getResourceAsStream(HELLO_ANYONE_T2FLOW);
        WorkflowBundle wfbundle = io.readBundle(helloWorld, null);

        MockupWf2ROConverter converter = new MockupWf2ROConverter(wfbundle, URI.create(HELLO_ANYONE_T2FLOW));
        converter.convert();
        //        System.out.println(converter.getResources().keySet());
        assertEquals(MockupWf2ROConverter.EXPECTED_ANNOTATIONS.size() + 1, converter.getResourcesAdded().size());

        OntModel model = converter.createManifestModel(null);
        Individual ro = model.getIndividual(converter.createResearchObject(null).toString());
        assertNotNull("RO exists in the manifest", ro);
        // should aggregate the workflow, 2 annotations about it and 2 annotation bodies
        List<RDFNode> aggregatedResources = ro.listPropertyValues(ORE.aggregates).toList();
        //        System.out.println(aggregatedResources);
        assertEquals("Correct number of aggregated resources", MockupWf2ROConverter.EXPECTED_RESOURCES.size()
                + MockupWf2ROConverter.EXPECTED_ANNOTATIONS.size(), aggregatedResources.size());
        for (RDFNode node : aggregatedResources) {
            assertTrue(node.isURIResource());
            Individual ind = node.as(Individual.class);
            assertTrue("Wf or annotation", ind.hasRDFType(RO.Resource) || ind.hasRDFType(RO.AggregatedAnnotation));
            if (ind.hasRDFType(RO.Resource)) {
                assertTrue("Path " + ind.getURI() + " is expected",
                    MockupWf2ROConverter.EXPECTED_RESOURCES.contains(ind.getURI()));
            } else {
                assertTrue("Path " + ind.getURI() + " is expected",
                    MockupWf2ROConverter.EXPECTED_ANNOTATIONS.contains(ind.getURI()));
            }
        }

        checkHasWorkflowDefinition(converter);
        checkHasWorkflowAnnotations(converter);
    }


    /**
     * A helper method for verifying the correct link between the workflow bundle and its main worklow.
     * 
     * @param converter
     *            a mockup converter that should contain the link in its resources
     */
    protected void checkHasWorkflowDefinition(MockupWf2ROConverter converter) {
        String hasWorkflowDefBody = converter.getResources().get(URI.create(MockupWf2ROConverter.BODY_LINK_WFDEF));
        // System.out.println(hasWorkflowDefBody);
        OntModel hasWorkflowDefModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
        hasWorkflowDefModel.read(new StringReader(hasWorkflowDefBody), MockupWf2ROConverter.BODY_LINK_WFDEF,
            RDFFormat.TURTLE.getName().toUpperCase());
        Property hasWfDef = hasWorkflowDefModel.getProperty(HAS_WF_DEF);
        Individual wf = hasWorkflowDefModel.getIndividual(WF_URI);
        assertNotNull("Could not find Workflow " + WF_URI, wf);
        Resource wfDef = wf.getPropertyResourceValue(hasWfDef);
        assertEquals(HELLO_ANYONE_WFBUNDLE, wfDef.getURI());
    }


    /**
     * A helper method for verifying the correct annotations taken from the t2flow.
     * 
     * @param converter
     *            a mockup converter that should contain the link in its resources
     */
    protected void checkHasWorkflowAnnotations(MockupWf2ROConverter converter) {
        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM);
        for (String body : MockupWf2ROConverter.BODY_WF_ANNOTATIONS) {
            model.read(new ByteArrayInputStream(converter.getResources().get(URI.create(body)).getBytes()), body,
                RDFFormat.RDFXML.getName().toUpperCase());
        }
        //        model.write(System.out);
        Individual wf = model.getIndividual(WF_URI);
        assertNotNull("Could not find Workflow " + WF_URI, wf);
        Literal description = model
                .createLiteral("An extension to helloworld.t2flow - this workflow takes a workflow input \"name\" which is combined with the string constant \"Hello, \" using the local worker \"Concatenate two strings\", and outputs the produced string to the workflow output \"greeting\".");
        assertTrue("workflow description is propagated to the RO", wf.hasProperty(DCTerms.description, description));
        Literal title = model.createLiteral("Hello Anyone");
        assertTrue("workflow title is propagated to the RO", wf.hasProperty(DCTerms.title, title));
    }
}
