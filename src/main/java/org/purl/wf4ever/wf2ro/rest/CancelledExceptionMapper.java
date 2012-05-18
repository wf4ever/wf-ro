package org.purl.wf4ever.wf2ro.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps <code>CancelledException</code> to <code>410 (Gone)</code> HTTP response.
 * 
 * @author piotrekhol
 * 
 */
@Provider
public class CancelledExceptionMapper
	implements ExceptionMapper<CancelledException>
{

	@Override
	public Response toResponse(CancelledException e)
	{
		return Response.status(Status.GONE).type("text/plain").entity(e.getMessage()).build();
	}

}
