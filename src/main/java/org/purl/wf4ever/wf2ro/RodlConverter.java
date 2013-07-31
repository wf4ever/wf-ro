/**
 * 
 */
package org.purl.wf4ever.wf2ro;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.purl.wf4ever.rosrs.client.Annotable;
import org.purl.wf4ever.rosrs.client.Annotation;
import org.purl.wf4ever.rosrs.client.ROSRService;
import org.purl.wf4ever.rosrs.client.ResearchObject;
import org.purl.wf4ever.rosrs.client.Resource;
import org.purl.wf4ever.rosrs.client.exception.ROException;
import org.purl.wf4ever.rosrs.client.exception.ROSRSException;

import uk.org.taverna.scufl2.api.container.WorkflowBundle;

/**
 * This class implements a Wf-RO converter uploading all created resources to the RODL.
 * 
 * @author piotrekhol
 */
public class RodlConverter extends Wf2ROConverter {

    /** Logger. */
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
        super(wfbundle, wfUri);
        URI rodlURI = roURI.resolve(".."); // zrobic z tego metode i stala
        this.rosrs = new ROSRService(rodlURI, rodlToken);
        this.roURI = roURI;
    }


    @Override
    protected ResearchObject createResearchObject(UUID wfUUID)
            throws ROSRSException {
        try {
            rosrs.getResourceHead(roURI);
            LOG.debug("Research object " + roURI + " returned status 200 OK, will use this one");
            return new ResearchObject(roURI, rosrs);
        } catch (ROSRSException e) {
            LOG.debug("Research object " + roURI + " returned status " + e.getStatus() + ", will create a new one");
            Path name = Paths.get(roURI.getPath()).getFileName();
            if (name == null) {
                throw new IllegalStateException("Can't extract the slug from URI " + roURI);
            }
            String slug = name.toString();
            if (slug.endsWith("/")) {
                slug = slug.substring(0, slug.length() - 1);
            }
            return ResearchObject.create(rosrs, slug);
        }
    }


    @Override
    protected Resource uploadAggregatedResource(ResearchObject ro, String path, InputStream in, String contentType)
            throws IOException, ROSRSException, ROException {
        return ro.aggregate(path, in, contentType);
    }


    @Override
    protected Annotation uploadAnnotation(ResearchObject ro, String name, Annotable target, InputStream in,
            String contentType)
            throws ROSRSException, ROException {
        String bodyPath = createAnnotationBodyPath(target.getName() + "-" + name);
        return target.annotate(bodyPath, in, contentType);
    }


    /**
     * Generate a path for an annotation body of a resource. The template is ["ro"|resource_name] + "-" + random_string.
     * 
     * @param targetName
     *            the last segment or full URI
     * @return an annotation body path relative to the RO URI
     */
    private static String createAnnotationBodyPath(String targetName) {
        if (targetName.endsWith("/")) {
            targetName = targetName.substring(0, targetName.length() - 1);
        }
        String randomBit = "" + Math.abs(UUID.randomUUID().getLeastSignificantBits());
        return ".ro/" + targetName + "-" + randomBit + ".ttl";
    }

}
