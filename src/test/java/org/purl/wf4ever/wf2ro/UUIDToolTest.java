package org.purl.wf4ever.wf2ro;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.UUID;

import org.junit.Test;

public class UUIDToolTest {
    @Test
    public void namespaceUUIDfromURL() throws Exception {
        UUID uuid = UUIDTool.namespaceUUID(URI.create("http://example.com/bundle1.robundle"));
        assertEquals("7878e885-327c-5ad4-9868-7338f1f13b3b", uuid.toString());
    }
}
