package org.purl.wf4ever.wf2ro.rest;

import java.util.UUID;

public class NotFoundException
	extends Exception
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5595815044424772734L;


	public NotFoundException(UUID uuid)
	{
		super("No job with id " + uuid);
	}
}