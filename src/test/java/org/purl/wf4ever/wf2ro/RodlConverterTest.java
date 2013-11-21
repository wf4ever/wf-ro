package org.purl.wf4ever.wf2ro;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Test;

public class RodlConverterTest {

    @Test
    public void uriToSlug() throws Exception {
        assertEquals("", RodlConverter.uriToSlug(URI.create("http://example.com/")));
        assertEquals("", RodlConverter.uriToSlug(URI.create("http://example.com")));
        assertEquals("path", RodlConverter.uriToSlug(URI.create("http://example.com/path")));
        assertEquals("path", RodlConverter.uriToSlug(URI.create("http://example.com/nested/path")));
        assertEquals("path", RodlConverter.uriToSlug(URI.create("http://example.com/nested/path#anchor")));
        assertEquals("slash", RodlConverter.uriToSlug(URI.create("http://example.com/path/with/slash/")));
        assertEquals("slash", RodlConverter.uriToSlug(URI.create("http://example.com/path/with/slash/#anchor")));
        assertEquals("relative", RodlConverter.uriToSlug(URI.create("relative")));
        assertEquals("path", RodlConverter.uriToSlug(URI.create("relative/path")));
        assertEquals("", RodlConverter.uriToSlug(URI.create("/")));
        assertEquals("", RodlConverter.uriToSlug(URI.create("")));
        assertEquals("slashes", RodlConverter.uriToSlug(URI.create("http://example.com/path/with//slashes////")));
    }
}
