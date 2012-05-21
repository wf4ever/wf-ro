/**
 * 
 */
package org.purl.wf4ever.wf2ro.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

	private static final String WF_URI = "https://raw.github.com/wf4ever/scufl2-wfdesc/master/src/test/resources/helloworld.t2flow";

	private static final String TAVERNA_FORMAT = "http://taverna.sf.net/2008/xml/t2flow";

	private static final String RO_URI = "http://sandbox.wf4ever-project.org/rosrs5/ROs/"
			+ UUID.randomUUID().toString() + "/";

	private static final Token TOKEN = new Token("47d5423c-b507-4e1c-8", null);

	private WebResource webResource;


	@After
	public void tearDown()
	{
		ROSRService.deleteResearchObject(URI.create(RO_URI), TOKEN);
	}


	public RestApiTest()
	{
		super("org.purl.wf4ever.wf2ro.rest");
	}


	@Test
	public void test()
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

		JobStatus status = webResource.uri(jobURI).get(JobStatus.class);
		assertTrue(status.getStatus() == Status.RUNNING || status.getStatus() == Status.DONE);
	}
}
