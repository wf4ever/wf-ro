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

import org.purl.wf4ever.rosrs.client.common.ROSRService;
import org.scribe.model.Token;

import uk.org.taverna.scufl2.api.container.WorkflowBundle;

import com.hp.hpl.jena.ontology.OntModel;
import com.sun.jersey.api.client.ClientResponse;

/**
 * This class implements a Wf-RO converter uploading all created resources to the RODL.
 * 
 * @author piotrekhol
 */
public class RodlConverter extends Wf2ROConverter {

    /** RO URI. */
    private final URI roURI;

    /** RODL access token. */
    private final Token rodlToken;


    /**
     * Constructor.
     * 
     * @param serviceURI
     *            The URI under which the conversion service is available
     * @param wfbundle
     *            the workflow bundle
     * @param roURI
     *            research object URI, will be created if doesn't exist
     * @param rodlToken
     *            the RODL access token for updating the RO
     */
    public RodlConverter(URI serviceURI, WorkflowBundle wfbundle, URI roURI, Token rodlToken) {
        super(serviceURI, wfbundle);
        this.roURI = roURI;
        this.rodlToken = rodlToken;
    }


    @Override
    protected URI createResearchObject(UUID wfUUID) {
        URI rodlURI = roURI.resolve("../.."); // zrobic z tego metode i stala
        String[] segments = roURI.getPath().split("/"); // stala?
        String roId = segments[segments.length - 1]; // URI utils
        try {
            List<URI> userROs = ROSRService.getROList(rodlURI, rodlToken);
            if (!userROs.contains(roURI)) {
                // wyodrebnic?
                ClientResponse response = ROSRService.createResearchObject(rodlURI, roId, rodlToken);
                int code = response.getClientResponseStatus().getStatusCode();
                String body = response.getEntity(String.class);
                response.close(); // to do finally
                if (code != HttpServletResponse.SC_CREATED) {
                    throw new RuntimeException("Wrong response status when creating an RO in RODL: " + body); // zadeklarowac wlasny exception
                }
            }
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e); // tu tez
        }
        return roURI;
    }


    @Override
    protected void uploadAggregatedResource(URI resourceURI, String contentType, InputStream in)
            throws IOException {
        ClientResponse response = ROSRService.uploadResource(resourceURI, in, contentType, rodlToken);
        int code = response.getClientResponseStatus().getStatusCode();
        String body = response.getEntity(String.class);
        response.close(); //jw
        if (code != HttpServletResponse.SC_OK && code != HttpServletResponse.SC_CREATED) {
            throw new RuntimeException("Wrong response status when uploading an aggregated resource " + resourceURI
                    + ": " + body); // i jeszcze tu
        }
    }


    @Override
    protected OntModel createManifestModel(URI roURI) {
        return ROSRService.createManifestModel(roURI);
    }


    @Override
    protected void uploadManifest(URI roURI, OntModel manifest) {
        ClientResponse response = ROSRService.uploadManifestModel(roURI, manifest, rodlToken);
        int code = response.getClientResponseStatus().getStatusCode();
        String body = response.getEntity(String.class);
        response.close(); //jw
        if (code != 200) {
            throw new RuntimeException("Wrong response status when uploading the manifest model: " + body); //jw
        }
    }

}
