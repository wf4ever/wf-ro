/**
 * 
 */
package org.purl.wf4ever.wf2ro;

import java.io.IOException;
import java.io.InputStream;
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
     * @param wfbundle
     *            the workflow bundle
     * @param roURI
     *            research object URI, will be created if doesn't exist
     * @param rodlToken
     *            the RODL access token for updating the RO
     */
    public RodlConverter(WorkflowBundle wfbundle, URI roURI, Token rodlToken) {
        super(wfbundle);
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
        } catch (URISyntaxException e) {
            throw new RuntimeException(e); // tu tez
        }
        return roURI;
    }


    @Override
    protected void uploadAggregatedResource(URI researchObject, String path, InputStream in, String contentType)
            throws IOException {
        ClientResponse response = ROSRService.createResource(researchObject, path, in, contentType, rodlToken);
        int code = response.getClientResponseStatus().getStatusCode();
        String body = response.getEntity(String.class);
        response.close(); //jw
        if (code != HttpServletResponse.SC_CREATED) {
            throw new RuntimeException("Wrong response status when uploading an aggregated resource " + path + ": "
                    + body); // i jeszcze tu
        }
    }


    @Override
    protected OntModel createManifestModel(URI roURI) {
        return ROSRService.createManifestModel(roURI);
    }


    @Override
    protected URI uploadAnnotation(URI researchObject, List<URI> targets, InputStream in, String contentType)
            throws IOException {
        String bodyPath = ROSRService.createAnnotationBodyPath(targets.get(0).resolve(".").relativize(targets.get(0))
                .toString());
        ClientResponse response = ROSRService.addAnnotation(researchObject, targets, bodyPath, in, contentType,
            rodlToken);
        return response.getLocation();
    }

}
