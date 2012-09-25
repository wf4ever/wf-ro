/**
 * 
 */
package org.purl.wf4ever.wf2ro.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Test;
import org.purl.wf4ever.rosrs.client.common.ROSRSException;
import org.purl.wf4ever.rosrs.client.common.ROSRService;
import org.purl.wf4ever.wf2ro.MockupWf2ROConverter;
import org.purl.wf4ever.wf2ro.rest.Job.State;
import org.scribe.model.Token;

import uk.org.taverna.scufl2.translator.t2flow.T2FlowReader;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * This test verifies the correctness of the REST API. It creates 2 jobs, one of which is cancelled. The other job is
 * expected to finish within predefined time, e.g. 120 seconds.
 * 
 * This test can only be run as a maven run (goal=test) and requires run parameters, as described in
 * http://jersey.java.net/nonav/documentation/latest/test-framework.html
 * 
 * To run the test using the embedded Grizzly 2 server, launched at run time, set: jersey.test.containerFactory =
 * com.sun.jersey.test.framework.spi.container.grizzly2.GrizzlyTestContainerFactory jersey.test.port = 8080 (or other)
 * 
 * @author piotrekhol
 * 
 */
public class RestApiTest extends JerseyTest {

    /** an example workflow from myExperiment. */
    private static final URI WF_URI = URI.create("http://www.myexperiment.org/workflows/2648/download?version=1");

    /** workflow format URI. */
    private static final URI TAVERNA_FORMAT = URI.create(T2FlowReader.APPLICATION_VND_TAVERNA_T2FLOW_XML);

    /** RO URI, with a random UUID as ro id. */
    private static final URI RO_URI = URI.create("http://sandbox.wf4ever-project.org/rodl/ROs/"
            + UUID.randomUUID().toString() + "/");

    /** RO URI, with a random UUID as ro id. */
    private static final URI RO2_URI = URI.create("http://sandbox.wf4ever-project.org/rodl/ROs/"
            + UUID.randomUUID().toString() + "/");

    /** RODL access token, currently assigned to Piotr. */
    private static final Token TOKEN = new Token("47d5423c-b507-4e1c-8", null);

    /**
     * Maximum time that the test waits for a job to finish. After that the test fails.
     */
    private static final long MAX_JOB_TIME_S = 240;


    @After
    @Override
    public void tearDown()
            throws ROSRSException {
        ROSRService.deleteResearchObject(RO_URI, TOKEN);
        ROSRService.deleteResearchObject(RO2_URI, TOKEN);
    }


    /**
     * Constructor.
     */
    public RestApiTest() {
        super("org.purl.wf4ever.wf2ro.rest");
    }


    /**
     * Create 2 jobs, cancel one and wait for the 2nd.
     * 
     * @throws InterruptedException
     *             interrupted while waiting for a job to finish
     */
    @Test
    public void test()
            throws InterruptedException {
        WebResource webResource;
        if (resource().getURI().getHost().equals("localhost")) {
            webResource = resource();
        } else {
            webResource = resource().path("wf-ro/");
        }

        Form f = new Form();
        f.add("resource", WF_URI);
        f.add("format", TAVERNA_FORMAT);
        f.add("ro", RO_URI);
        f.add("token", TOKEN.getToken());

        //        JobConfig config = new JobConfig(WF_URI, TAVERNA_FORMAT, RO2_URI, TOKEN.getToken());

        ClientResponse response = webResource.path("jobs").post(ClientResponse.class, f);
        assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
        URI jobURI = response.getLocation();
        response.close();

        JobStatus status = null;

        for (int i = 0; i < MAX_JOB_TIME_S; i++) {
            System.out.print(".");
            status = webResource.uri(jobURI).get(JobStatus.class);
            assertTrue("Status is: " + status.getStatus().toString(),
                status.getStatus() == State.RUNNING || status.getStatus() == State.DONE);
            assertEquals(WF_URI, status.getResource());
            assertEquals(TAVERNA_FORMAT, status.getFormat());
            assertEquals(RO_URI, status.getRo());
            if (status.getStatus() == State.DONE) {
                System.out.println();
                break;
            }
            Thread.sleep(1000);
        }
        System.out.println(webResource.uri(jobURI).get(String.class));
        if (status.getStatus() == State.RUNNING) {
            fail("The job hasn't finished on time");
        }
        assertNotNull(status.getAdded());
        assertEquals(MockupWf2ROConverter.EXPECTED_RESOURCES.size(), status.getAdded().size());
    }
}
