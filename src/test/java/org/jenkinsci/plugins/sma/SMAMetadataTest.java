package org.jenkinsci.plugins.sma;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SMAMetadataTest {

    private SMAMetadata metadataObject;
    String extension = ".ext";
    String container = "container";
    String member = "Member";
    String metadataType = "MDType";
    String path = "src/container/";
    boolean destructible = true;
    boolean valid = true;
    boolean metaxml = true;
    String body = "";

    @Before
    public void setUp() throws Exception {
         metadataObject = new SMAMetadata(extension, container, member, metadataType,
                 path, destructible, valid, metaxml, body.getBytes());
    }

    @Test
    public void testGetExtension() throws Exception {
        assertEquals(extension, metadataObject.getExtension());
    }

    @Test
    public void testGetContainer() throws Exception {
        assertEquals(container, metadataObject.getContainer());
    }

    @Test
    public void testGetPath() throws Exception {
        assertEquals(path, metadataObject.getPath());
    }

    @Test
    public void testGetMember() throws Exception {
        assertEquals(member, metadataObject.getMember());
    }

    @Test
    public void testGetMetadataType() throws Exception {
        assertEquals(metadataType, metadataObject.getMetadataType());
    }

    @Test
    public void testIsDestructible() throws Exception {
        if (destructible){
            assertTrue(metadataObject.isDestructible());
        }else{
            assertTrue(!metadataObject.isDestructible());
        }
    }

    @Test
    public void testHasMetaxml() throws Exception {
        if (metaxml){
            assertTrue(metadataObject.hasMetaxml());
        }else{
            assertTrue(!metadataObject.hasMetaxml());
        }
    }
}