/**
 * 
 */
package org.purl.wf4ever.wf2ro;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.purl.wf4ever.rosrs.client.common.ROSRSException;
import org.purl.wf4ever.rosrs.client.common.ROSRService;
import org.purl.wf4ever.rosrs.client.common.Utils;

import pl.psnc.dl.wf4ever.vocabulary.ORE;
import uk.org.taverna.scufl2.api.container.WorkflowBundle;

import com.google.common.collect.Multimap;
import com.hp.hpl.jena.ontology.OntModel;
import com.sun.jersey.api.client.ClientResponse;

/**
 * This class implements a Wf-RO converter uploading all created resources to the RODL.
 * 
 * @author piotrekhol
 */
public class RodlConverter extends Wf2ROConverter {

    /** Logger. */
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(RodlConverter.class);

    /** RO URI. */
    private final URI roURI;

    /** RODL client. */
    private final ROSRService rosrs;


    /**
     * Constructor.
     * 
     * @param wfbundle
     *            the workflow bundle
     * @param wfUri
     *            workflow URI
     * @param roURI
     *            research object URI, will be created if doesn't exist
     * @param rodlToken
     *            the RODL access token for updating the RO
     */
    public RodlConverter(WorkflowBundle wfbundle, URI wfUri, URI roURI, String rodlToken) {
        super(wfbundle, wfUri, "folders.properties");
        URI rodlURI = roURI.resolve("../.."); // zrobic z tego metode i stala
        this.rosrs = new ROSRService(rodlURI, rodlToken);
        this.roURI = roURI;
    }


    @Override
    protected URI createResearchObject(UUID wfUUID)
            throws ROSRSException {
        String[] segments = roURI.getPath().split("/"); // stala?
        String roId = segments[segments.length - 1]; // URI utils
        try {
            List<URI> userROs = rosrs.getROList(false);
            if (!userROs.contains(roURI)) {
                rosrs.createResearchObject(roId);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e); // tu tez
        }
        return roURI;
    }


    @Override
    protected void uploadAggregatedResource(URI researchObject, String path, InputStream in, String contentType)
            throws ROSRSException {
        rosrs.createResource(researchObject, path, in, contentType);
    }


    @Override
    protected OntModel createManifestModel(URI roURI) {
        return ROSRService.createManifestModel(roURI);
    }


    @Override
    protected URI uploadAnnotation(URI researchObject, String name, List<URI> targets, InputStream in,
            String contentType)
            throws ROSRSException {
        String bodyPath = ROSRService.createAnnotationBodyPath(targets.get(0).resolve(".").relativize(targets.get(0))
                .toString()
                + "-" + name);
        ClientResponse response = rosrs.addAnnotation(researchObject, targets, bodyPath, in, contentType);
        return response.getLocation();
    }


    @Override
    protected void aggregateResource(URI researchObject, URI resource)
            throws ROSRSException {
        rosrs.aggregateResource(researchObject, resource);
    }


    @Override
    public void readModelFromUri(OntModel model, URI wfdescURI) {
        model.read(wfdescURI.toString());
    }


    @Override
    protected URI createFolder(URI roURI, String path)
            throws ROSRSException {
        ClientResponse response = rosrs.createFolder(roURI, path);
        if (response.getStatus() == HttpStatus.SC_CONFLICT) {
            response.close();
            return UriBuilder.fromUri(roURI).path(path).build();
        }
        Multimap<String, URI> links = Utils.getLinkHeaders(response.getHeaders().get("Link"));
        Iterator<URI> it = links.get(ORE.proxyFor.getURI()).iterator();
        response.close();
        if (!it.hasNext()) {
            throw new ROSRSException("Missing proxyFor header", response.getStatus(), response
                    .getClientResponseStatus().getReasonPhrase());
        }
        return it.next();
    }


    @Override
    protected URI addFolderEntry(URI folder, URI proxyFor, String name)
            throws IOException, ROSRSException {
        ClientResponse response = rosrs.addFolderEntry(folder, proxyFor, name);
        URI entry = response.getStatus() == HttpStatus.SC_CREATED ? response.getLocation() : null;
        response.close();
        return entry;
    }

}
