package org.purl.wf4ever.wf2ro.rest;

import java.util.UUID;

/**
 * The requested job does not exist.
 * 
 * @author piotrekhol
 * 
 */
public class NotFoundException extends Exception {

    /**
     * id.
     */
    private static final long serialVersionUID = 5595815044424772734L;


    /**
     * Constructor.
     * 
     * @param uuid
     *            job UUID
     */
    public NotFoundException(UUID uuid) {
        super("No job with id " + uuid);
    }
}
