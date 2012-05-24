/**
 * 
 */
package org.purl.wf4ever.wf2ro;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.purl.wf4ever.rosrs.client.common.ROSRService;
import org.scribe.model.Token;

import com.hp.hpl.jena.ontology.OntModel;
import com.sun.jersey.api.client.ClientResponse;

/**
 * This class implements a Wf-RO converter uploading all created resources to the RODL.
 * 
 * @author piotrekhol
 */
public class RodlConverter
	extends Wf2ROConverter
{

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(RodlConverter.class);

	private URI roURI;

	private Token rodlToken;


	/**
	 * @param serviceURI
	 *            The URI under which the conversion service is available
	 * @param roURI
	 *            research object URI, will be created if doesn't exist
	 * @param rodlToken
	 *            the RODL access token for updating the RO
	 */
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
		URI rodlURI = roURI.resolve("../..");
		String[] segments = roURI.getPath().split("/");
		String roId = segments[segments.length - 1];
		try {
			List<URI> userROs = ROSRService.getROList(rodlURI, rodlToken);
			if (!userROs.contains(roURI)) {
				ClientResponse response = ROSRService.createResearchObject(rodlURI, roId, rodlToken);
				int code = response.getClientResponseStatus().getStatusCode();
				String body = response.getEntity(String.class);
				response.close();
				if (code != HttpServletResponse.SC_CREATED) {
					throw new RuntimeException("Wrong response status when creating an RO in RODL: " + body);
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
	protected void uploadAggregatedResource(URI resourceURI, String contentType, InputStream in)
		throws IOException
	{
		ClientResponse response = ROSRService.uploadResource(resourceURI, in, contentType, rodlToken);
		int code = response.getClientResponseStatus().getStatusCode();
		String body = response.getEntity(String.class);
		response.close();
		if (code != HttpServletResponse.SC_OK && code != HttpServletResponse.SC_CREATED) {
			throw new RuntimeException("Wrong response status when uploading an aggregated resource " + resourceURI
					+ ": " + body);
		}
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
		int code = response.getClientResponseStatus().getStatusCode();
		String body = response.getEntity(String.class);
		response.close();
		if (code != 200) {
			throw new RuntimeException("Wrong response status when uploading the manifest model: " + body);
		}
	}

}
