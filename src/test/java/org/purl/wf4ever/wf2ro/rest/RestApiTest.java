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
import org.purl.wf4ever.rosrs.client.common.ROSRService;
import org.purl.wf4ever.wf2ro.rest.Job.Status;
import org.scribe.model.Token;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * @author piotrekhol
 * 
 */
public class RestApiTest
	extends JerseyTest
{

	private static final URI WF_URI = URI
			.create("https://raw.github.com/wf4ever/scufl2-wfdesc/master/src/test/resources/helloworld.t2flow");

	private static final URI TAVERNA_FORMAT = URI.create("http://taverna.sf.net/2008/xml/t2flow");

	private static final URI RO_URI = URI.create("http://sandbox.wf4ever-project.org/rosrs5/ROs/"
			+ UUID.randomUUID().toString() + "/");

	private static final Token TOKEN = new Token("47d5423c-b507-4e1c-8", null);

	private static final long MAX_JOB_TIME_S = 20;

	private WebResource webResource;


	@After
	public void tearDown()
	{
		ROSRService.deleteResearchObject(RO_URI, TOKEN);
	}


	public RestApiTest()
	{
		super("org.purl.wf4ever.wf2ro.rest");
	}


	@Test
	public void test()
		throws InterruptedException
	{
		if (resource().getURI().getHost().equals("localhost")) {
			webResource = resource();
		}
		else {
			webResource = resource().path("wf2ro/");
		}

		Form f = new Form();
		f.add("resource", WF_URI);
		f.add("format", TAVERNA_FORMAT);
		f.add("ro", RO_URI);
		f.add("token", TOKEN.getToken());

		ClientResponse response = webResource.path("jobs").post(ClientResponse.class, f);
		assertEquals(HttpServletResponse.SC_CREATED, response.getStatus());
		URI jobURI = response.getLocation();

		JobStatus status = null;
		for (int i = 0; i < MAX_JOB_TIME_S; i++) {
			System.out.print(".");
			status = webResource.uri(jobURI).get(JobStatus.class);
			System.out.println(webResource.uri(jobURI).get(String.class));
			assertTrue(status.getStatus() == Status.RUNNING || status.getStatus() == Status.DONE);
			assertEquals(WF_URI, status.getResource());
			assertEquals(TAVERNA_FORMAT, status.getFormat());
			assertEquals(RO_URI, status.getRo());
			if (status.getStatus() == Status.DONE) {
				System.out.println();
				break;
			}
			Thread.sleep(1000);
		}
		if (status.getStatus() == Status.RUNNING) {
			fail("The job hasn't finished on time");
		}
		assertNotNull(status.getAdded());
		assertEquals(2, status.getAdded().size());
	}
}
