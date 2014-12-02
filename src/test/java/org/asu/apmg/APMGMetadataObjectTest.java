package org.asu.apmg;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class APMGMetadataObjectTest {

    private APMGMetadataObject metadataObject;
    String extension = ".ext";
    String container = "container";
    String member = "Member";
    String metadataType = "MDType";
    String path = "src/container/";
    boolean destructible = true;
    boolean valid = true;

    @Before
    public void setUp() throws Exception {
         metadataObject = new APMGMetadataObject(extension, container, member, metadataType,
                 path, destructible, valid);
    }

    @Test
    public void testGetExtension() throws Exception {
        assertEquals(extension, metadataObject.getExtension());
    }

    @Test
    public void testSetExtension() throws Exception {
        extension = ".ext2";
        metadataObject.setExtension(extension);
        assertEquals(extension, metadataObject.getExtension());
    }

    @Test
    public void testGetContainer() throws Exception {
        assertEquals(container, metadataObject.getContainer());
    }

    @Test
    public void testSetContainer() throws Exception {
        container = "container2";
        metadataObject.setContainer(container);
        assertEquals(container, metadataObject.getContainer());
    }

    @Test
    public void testGetPath() throws Exception {
        assertEquals(path, metadataObject.getPath());
    }

    @Test
    public void testSetPath() throws Exception {
        path = "src/container2/";
        metadataObject.setPath(path);
        assertEquals(path, metadataObject.getPath());
    }

    @Test
    public void testGetMember() throws Exception {
        assertEquals(member, metadataObject.getMember());
    }

    @Test
    public void testSetMember() throws Exception {
        member = "Member2";
        metadataObject.setMember(member);
        assertEquals(member, metadataObject.getMember());
    }

    @Test
    public void testGetMetadataType() throws Exception {
        assertEquals(metadataType, metadataObject.getMetadataType());
    }

    @Test
    public void testSetMetadataType() throws Exception {
        metadataType = "MDType2";
        metadataObject.setMetadataType(metadataType);
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
    public void testSetDestructible() throws Exception {
        destructible = false;
        metadataObject.setDestructible(destructible);
        assertEquals(destructible, metadataObject.isDestructible());
    }
}