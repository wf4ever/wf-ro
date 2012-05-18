/**
 * 
 */
package org.purl.wf4ever.wf2ro;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.purl.wf4ever.rosrs.client.common.ROSRService;
import org.scribe.model.Token;

import uk.org.taverna.scufl2.rdfxml.RDFXMLReader;

import com.hp.hpl.jena.ontology.OntModel;
import com.sun.jersey.api.client.ClientResponse;

/**
 * @author piotrekhol
 * 
 */
public class RodlConverter
	extends Wf2ROConverter
{

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(RodlConverter.class);

	private URI roURI;

	private Token rodlToken;


	public RodlConverter(URI serviceURI, URI roURI, Token rodlToken)
	{
		super(serviceURI);
		this.roURI = roURI;
		this.rodlToken = rodlToken;
	}


	/* (non-Javadoc)
	 * @see org.purl.wf4ever.wf2ro.Wf2ROConverter#createResearchObject(java.util.UUID)
	 */
	@Override
	protected URI createResearchObject(UUID wfUUID)
	{
		URI rodlURI = roURI.resolve("..");
		String[] segments = roURI.getPath().split("/");
		String roId = segments[segments.length - 1];
		try {
			List<URI> userROs = ROSRService.getROList(rodlURI, rodlToken);
			if (!userROs.contains(roURI)) {
				ClientResponse response = ROSRService.createResearchObject(rodlURI, roId, rodlToken);
				if (response.getClientResponseStatus().getStatusCode() != HttpServletResponse.SC_CREATED) {
					throw new RuntimeException("Wrong response status when creating an RO in RODL: "
							+ response.getEntity(String.class));
				}
			}
		}
		catch (MalformedURLException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
		return roURI;
	}


	/* (non-Javadoc)
	 * @see org.purl.wf4ever.wf2ro.Wf2ROConverter#createAnnotationBodyOutputStream(java.net.URI)
	 */
	@Override
	protected OutputStream createAggregatedResourceOutputStream(final URI resourceURI, String contentType)
		throws IOException
	{
		final PipedInputStream in = new PipedInputStream();
		PipedOutputStream out = new PipedOutputStream(in);
		new Thread(new Runnable() {

			public void run()
			{
				ClientResponse response = ROSRService.uploadResource(resourceURI, in,
					RDFXMLReader.APPLICATION_VND_TAVERNA_SCUFL2_WORKFLOW_BUNDLE, rodlToken);
				if (response.getClientResponseStatus().getStatusCode() != HttpServletResponse.SC_OK
						&& response.getClientResponseStatus().getStatusCode() != HttpServletResponse.SC_CREATED) {
					throw new RuntimeException("Wrong response status when uploading an aggregated resource: "
							+ response.getEntity(String.class));
				}
			}
		}).start();
		return out;
	}


	/* (non-Javadoc)
	 * @see org.purl.wf4ever.wf2ro.Wf2ROConverter#createManifestModel(java.net.URI)
	 */
	@Override
	protected OntModel createManifestModel(URI roURI)
	{
		return ROSRService.createManifestModel(roURI);
	}


	/* (non-Javadoc)
	 * @see org.purl.wf4ever.wf2ro.Wf2ROConverter#uploadManifest(java.net.URI, com.hp.hpl.jena.ontology.OntModel)
	 */
	@Override
	protected void uploadManifest(URI roURI, OntModel manifest)
	{
		ClientResponse response = ROSRService.uploadManifestModel(roURI, manifest, rodlToken);
		if (response.getClientResponseStatus().getStatusCode() != 200) {
			throw new RuntimeException("Wrong response status when uploading the manifest model: "
					+ response.getEntity(String.class));
		}
	}

}
