package org.purl.wf4ever.wf2ro.exceptions.mappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.purl.wf4ever.wf2ro.exceptions.BadRequestException;

/**
 * Maps <code>BadRequestException</code> to <code>400 (Bad Request)</code> HTTP response.
 * 
 * @author piotrhol
 * 
 */
@Provider
public class BadRequestExceptionMapper implements ExceptionMapper<BadRequestException> {

    @Override
    public Response toResponse(BadRequestException e) {
        return Response.status(Status.BAD_REQUEST).type("text/plain").entity(e.getMessage()).build();
    }

}
