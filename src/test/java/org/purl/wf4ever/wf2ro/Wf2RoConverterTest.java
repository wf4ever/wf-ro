package org.purl.wf4ever.wf2ro;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import uk.org.taverna.scufl2.api.container.WorkflowBundle;
import uk.org.taverna.scufl2.api.io.ReaderException;
import uk.org.taverna.scufl2.api.io.WorkflowBundleIO;

public class Wf2RoConverterTest
{

	private static final String helloWorldT2Flow = "helloworld.t2flow";


	@Test
	public void testConvert()
		throws ReaderException, IOException
	{
		WorkflowBundleIO io = new WorkflowBundleIO();
		InputStream helloWorld = getClass().getClassLoader().getResourceAsStream(helloWorldT2Flow);
		WorkflowBundle wfbundle = io.readBundle(helloWorld, null);

		Wf2ROConverter converter = new MockupWf2ROConverter();
		converter.convert(wfbundle);
	}

}
