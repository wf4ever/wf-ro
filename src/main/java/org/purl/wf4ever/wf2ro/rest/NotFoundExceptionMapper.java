package org.purl.wf4ever.wf2ro.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps <code>NotFoundException</code> to <code>404 (Not Found)</code> HTTP response.
 * 
 * @author piotrekhol
 * 
 */
@Provider
public class NotFoundExceptionMapper
	implements ExceptionMapper<NotFoundException>
{

	@Override
	public Response toResponse(NotFoundException e)
	{
		return Response.status(Status.NOT_FOUND).type("text/plain").entity(e.getMessage()).build();
	}

}
