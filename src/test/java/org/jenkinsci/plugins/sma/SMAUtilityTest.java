package org.jenkinsci.plugins.sma;

import com.google.common.io.Files;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SMAUtilityTest
{
    File localPath;
    Map<String, byte[]> metadata;
    SMAPackage packageManifest;
    SMAPackage destructiveChange;

    @Before
    public void setUp() throws Exception
    {
        localPath = Files.createTempDir();

        String[] strings = {"TestContents", "TestXML"};

        metadata = new HashMap<String, byte[]>();
        metadata.put("classes/TestApex.cls", strings[0].getBytes());
        metadata.put("classes/TestApex.cls-meta.xml", strings[1].getBytes());
        metadata.put("pages/TestPages.page", strings[0].getBytes());
        metadata.put("pages/TestPages.page-meta.xml", strings[1].getBytes());
        metadata.put("triggers/TestTrigger.trigger", strings[0].getBytes());
        metadata.put("triggers/TestTrigger.trigger-meta.xml", strings[1].getBytes());
        
        List<SMAMetadata> metadataList = new ArrayList<SMAMetadata>();
        
        for (String s : metadata.keySet())
        {
            if (!s.contains("-meta.xml"))
            {
                metadataList.add(SMAMetadataTypes.createMetadataObject(s, metadata.get(s)));
            }
        }
        
        packageManifest = new SMAPackage(metadataList, false);
        destructiveChange = new SMAPackage(metadataList, true);
    }

    @After
    public void tearDown() throws Exception
    {
        localPath.delete();
    }

    @Test
    public void testZipPackage() throws Exception
    {
        ByteArrayOutputStream testStream = new ByteArrayOutputStream();

        testStream = SMAUtility.zipPackage(metadata, packageManifest, destructiveChange);

        System.out.println(testStream);

        Assert.assertNotNull(testStream);
    }

    @Test
    public void testWriteZip() throws Exception
    {
        ByteArrayOutputStream testStream = new ByteArrayOutputStream();

        testStream = SMAUtility.zipPackage(metadata, packageManifest, destructiveChange);

        SMAUtility.writeZip(testStream, localPath.getPath() + "/streamToZip.zip");

        File zipFile = new File(localPath.getPath() + "/streamToZip.zip");

        Assert.assertTrue(zipFile.exists());
    }
}
