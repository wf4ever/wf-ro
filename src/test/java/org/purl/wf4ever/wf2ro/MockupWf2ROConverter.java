package org.purl.wf4ever.wf2ro;

import java.io.OutputStream;
import java.net.URI;
import java.util.UUID;

import com.hp.hpl.jena.ontology.OntModel;

public class MockupWf2ROConverter
	extends Wf2ROConverter
{

	@Override
	protected URI addWorkflow(UUID wfUUID)
	{
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected URI getROURI()
	{
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected OutputStream createAnnotationBodyOutputStream(URI annotationBodyURI)
	{
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected OntModel createManifestModel(URI roURI)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
