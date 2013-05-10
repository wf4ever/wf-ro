package org.purl.wf4ever.wf2ro.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.purl.wf4ever.rosrs.client.exception.ROSRSException;
import org.purl.wf4ever.wf2ro.IntegrationTest;
import org.purl.wf4ever.wf2ro.rest.Job.State;

import pl.psnc.dl.wf4ever.vocabulary.ORE;
import uk.org.taverna.scufl2.translator.t2flow.T2FlowReader;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
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
 * @author piotrekhol
 * 
 */
@Category(IntegrationTest.class)
public class RestApiTest extends JerseyTest {

    /** A test HTTP mock server. */
    @Rule
    public static final WireMockRule WIREMOCK_RULE = new WireMockRule(8089); // No-args constructor defaults to port 8080

    /** an example workflow from myExperiment: http://www.myexperiment.org/workflows/2648/download?version=1. */
    private static final URI WF_URI = URI.create("http://localhost:8089/workflow.t2flow");

    /** workflow format MIME type. */
    private static final String TAVERNA_FORMAT = T2FlowReader.APPLICATION_VND_TAVERNA_T2FLOW_XML;

    /** RODL URI. */
    private static final URI ROSRS_URI = URI.create("http://localhost:8089/rodl/ROs/");

    /** RO URI that already exists. */
    private static final URI RO_URI_EXISTING = URI.create("http://localhost:8089/rodl/ROs/1/");

    /** RO URI that can always be created. */
    private static final URI RO_URI_NEW = URI.create("http://localhost:8089/rodl/ROs/2/");

    private static final String TOKEN = "foo";

    /**
     * Maximum time that the test waits for a job to finish. After that the test fails.
     */
    private static final long MAX_JOB_TIME_S = 10;


    @Before
    @Override
    public void setUp()
            throws Exception {
        super.setUp();
        setUpGetWorkflow();
        setUpCreateRo();
        setUpCreateFolder();
        setUpCreateFolderEntry();
        setUpCreateAnnotation();
        setUpCreateResource();
    }


    /**
     * Configure WireMock to handle folder creation.
     * 
     * @throws IOException
     *             if the test resources are not available
     */
    protected void setUpGetWorkflow()
            throws IOException {
        InputStream wf = getClass().getClassLoader().getResourceAsStream("hello_world_190236.t2flow");
        stubFor(get(urlEqualTo("/workflow.t2flow")).willReturn(
            aResponse().withStatus(200).withBody(IOUtils.toByteArray(wf))));
    }


    /**
     * Configure WireMock to return resource maps.
     * 
     * @throws IOException
     *             if the test resources are not available
     */
    protected void setUpCreateRo()
            throws IOException {
        stubFor(get(urlEqualTo("/rodl/ROs/")).willReturn(
            aResponse().withStatus(200).withHeader("Content-Type", "text/uri-list").withBody("" + RO_URI_EXISTING)));
        stubFor(post(urlEqualTo("/rodl/ROs/"))
                .withHeader("Slug", equalTo("2"))
                .withHeader("Accept", equalTo("application/rdf+xml"))
                .willReturn(
                    aResponse().withStatus(201).withHeader("Content-Type", "application/rdf+xml")
                            .withHeader("Location", RO_URI_NEW.toString())));
    }


    /**
     * Configure WireMock to handle folder creation.
     * 
     * @throws IOException
     *             if the test resources are not available
     */
    protected void setUpCreateFolder()
            throws IOException {
        InputStream response = getClass().getClassLoader().getResourceAsStream("rodl/folder-created-response.rdf");
        stubFor(post(urlMatching("/.+")).withHeader("Content-Type", equalTo("application/vnd.wf4ever.folder"))
                .willReturn(
                    aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/rdf+xml")
                            .withHeader("Location", "foo")
                            .withHeader(
                                "Link",
                                "<http://localhost:8089/" + UUID.randomUUID() + ">; rel=\"" + ORE.proxyFor.toString()
                                        + "\"")
                            .withHeader("Link", "<" + "foo" + ">; rel=\"" + ORE.isDescribedBy.toString() + "\"")
                            .withBody(IOUtils.toByteArray(response))));
    }


    /**
     * Configure WireMock to handle folder entries creation.
     * 
     * @throws IOException
     *             if the test resources are not available
     */
    protected void setUpCreateFolderEntry()
            throws IOException {
        stubFor(post(urlMatching("/.+")).withHeader("Content-Type", equalTo("application/vnd.wf4ever.folderentry"))
                .willReturn(
                    aResponse()
                            .withStatus(201)
                            .withHeader("Content-Type", "application/rdf+xml")
                            .withHeader("Location", "foo")
                            .withHeader(
                                "Link",
                                "<http://localhost:8089/" + UUID.randomUUID() + ">; rel=\"" + ORE.proxyFor.toString()
                                        + "\"")
                            .withHeader("Link", "<" + "foo" + ">; rel=\"" + ORE.isDescribedBy.toString() + "\"")
                            .withBody("")));
    }


    /**
     * Configure WireMock to create and delete annotations.
     * 
     * @throws IOException
     *             if the test resources are not available
     */
    protected void setUpCreateAnnotation()
            throws IOException {
        InputStream response = getClass().getClassLoader().getResourceAsStream("rodl/folder-created-response.rdf");
        stubFor(post(urlEqualTo("/rodl/ROs/[12]/")).withHeader("Link", matching(".+annotatesResource.+")).willReturn(
            aResponse()
                    .withStatus(201)
                    .withHeader("Location", "foo".toString())
                    .withHeader("Link",
                        "<http://localhost:8089/" + UUID.randomUUID() + ">; rel=\"" + ORE.proxyFor.toString() + "\"")
                    .withBody(IOUtils.toByteArray(response))));
    }


    /**
     * Configure WireMock to handle creating resources.
     * 
     * @throws IOException
     *             if the test resources are not available
     */
    protected void setUpCreateResource()
            throws IOException {
        InputStream response = getClass().getClassLoader().getResourceAsStream("rodl/resource-created-response.rdf");
        stubFor(post(urlMatching("/.+")).willReturn(
            aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", "text/plain")
                    .withHeader("Location", "foo")
                    .withHeader("Link",
                        "<http://localhost:8089/" + UUID.randomUUID() + ">; rel=\"" + ORE.proxyFor.toString() + "\"")
                    .withBody(IOUtils.toByteArray(response))));
    }


    /**
     * Constructor.
     */
    public RestApiTest() {
        super("org.purl.wf4ever.wf2ro.rest");
    }


    /**
     * Create a job and for it to finish.
     * 
     * @throws InterruptedException
     *             interrupted while waiting for a job to finish
     */
    @Test
    public void testCreateAndWait()
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
        f.add("ro", RO_URI_NEW);
        f.add("token", TOKEN);

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
            assertEquals(RO_URI_NEW, status.getRo());
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
        // this workflow has 3 inner annotations, plus roevo & wfdesc & link, plus the workflow itself, plus 16 folders = 22
        Assert.assertEquals(23, status.getAdded().size());
    }


    /**
     * Create a job posting a JSON and for it to finish.
     * 
     * @throws InterruptedException
     *             interrupted while waiting for a job to finish
     */
    @Test
    public void testCreateAndWaitJson()
            throws InterruptedException {
        WebResource webResource;
        if (resource().getURI().getHost().equals("localhost")) {
            webResource = resource();
        } else {
            webResource = resource().path("wf-ro/");
        }

        JobConfig config = new JobConfig(WF_URI, TAVERNA_FORMAT, RO_URI_NEW, TOKEN);

        ClientResponse response = webResource.path("jobs").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, config);
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
            assertEquals(RO_URI_NEW, status.getRo());
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
        // this workflow has 3 inner annotations, plus roevo & wfdesc & link, plus the workflow itself, plus 16 folders = 22
        Assert.assertEquals(23, status.getAdded().size());
    }


    /**
     * Work on an existing RO with a workflow. The conversion should be successful and the wf should be deleted.
     * 
     * @throws ROSRSException
     *             error creating the ro
     * @throws IOException
     *             error downloading the workflow
     * @throws InterruptedException
     *             interrupted while waiting for a job to finish
     */
    @Test
    public void testExistingRoWithWf()
            throws ROSRSException, IOException, InterruptedException {
        WebResource webResource;
        if (resource().getURI().getHost().equals("localhost")) {
            webResource = resource();
        } else {
            webResource = resource().path("wf-ro/");
        }

        Form f = new Form();
        f.add("resource", WF_URI);
        f.add("format", TAVERNA_FORMAT);
        f.add("ro", RO_URI_EXISTING);
        f.add("token", TOKEN);

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
            assertEquals(RO_URI_EXISTING, status.getRo());
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
        // this workflow has 3 inner annotations, plus roevo & wfdesc & link, plus the workflow itself, plus 16 folders = 22
        Assert.assertEquals(23, status.getAdded().size());
        response.close();
    }


    /**
     * Call the service twice on the same RO.
     * 
     * @throws ROSRSException
     *             error creating the ro
     * @throws IOException
     *             error downloading the workflow
     * @throws InterruptedException
     *             interrupted while waiting for a job to finish
     */
    @Test
    public void testDoubleExecution()
            throws ROSRSException, IOException, InterruptedException {
        WebResource webResource;
        if (resource().getURI().getHost().equals("localhost")) {
            webResource = resource();
        } else {
            webResource = resource().path("wf-ro/");
        }
        Form f = new Form();
        f.add("resource", WF_URI);
        f.add("format", TAVERNA_FORMAT);
        f.add("ro", RO_URI_NEW);
        f.add("token", TOKEN);

        ClientResponse response = webResource.path("jobs").post(ClientResponse.class, f);
        assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
        URI jobURI = response.getLocation();
        response.close();

        JobStatus status = null;

        for (int i = 0; i < MAX_JOB_TIME_S; i++) {
            System.out.print(".");
            status = webResource.uri(jobURI).get(JobStatus.class);
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
        response = webResource.path("jobs").post(ClientResponse.class, f);
        assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
        jobURI = response.getLocation();
        response.close();

        for (int i = 0; i < MAX_JOB_TIME_S; i++) {
            System.out.print(".");
            status = webResource.uri(jobURI).get(JobStatus.class);
            if (status.getStatus() == State.DONE) {
                System.out.println();
                break;
            }
            Thread.sleep(1000);
        }
        System.out.println(webResource.uri(jobURI).get(String.class));
        if (status.getStatus() == State.RUNNING) {
            fail("The 2nd job hasn't finished on time");
        }
        assertNotNull(status.getAdded());
        // this workflow has 3 inner annotations, plus roevo & wfdesc & link, plus the workflow itself, plus 16 folders = 22
        Assert.assertEquals(23, status.getAdded().size());
    }


    /**
     * Create a job posting a JSON and wait for it to finish.
     * 
     * @throws InterruptedException
     *             interrupted while waiting for a job to finish
     */
    @Test
    public void testCreateAndCatchException()
            throws InterruptedException {
        WebResource webResource;
        if (resource().getURI().getHost().equals("localhost")) {
            webResource = resource();
        } else {
            webResource = resource().path("wf-ro/");
        }

        JobConfig config = new JobConfig(WF_URI, TAVERNA_FORMAT, URI.create("http://foobar"), TOKEN);

        ClientResponse response = webResource.path("jobs").type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class, config);
        assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
        URI jobURI = response.getLocation();
        response.close();

        JobStatus status = null;

        for (int i = 0; i < MAX_JOB_TIME_S; i++) {
            System.out.print(".");
            status = webResource.uri(jobURI).get(JobStatus.class);
            if (status.getStatus() != State.RUNNING) {
                System.out.println();
                break;
            }
            Thread.sleep(1000);
        }
        System.out.println(webResource.uri(jobURI).get(String.class));
        if (status.getStatus() == State.RUNNING) {
            fail("The job hasn't finished on time");
        }
        Assert.assertEquals(State.RUNTIME_ERROR, status.getStatus());
        Assert.assertNotNull(status.getReason());
    }

}
