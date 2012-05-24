/**
 * 
 */
package org.purl.wf4ever.wf2ro.rest;

import java.util.UUID;

/**
 * Job that was requested has been cancelled
 * 
 * @author piotrekhol
 * 
 */
public class CancelledException
	extends Exception
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -146645361323967323L;


	public CancelledException(UUID uuid)
	{
		super("The job with id " + uuid + " has been cancelled");
	}

}
