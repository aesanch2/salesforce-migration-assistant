package org.jenkinsci.plugins.sma;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class SMAPackageTest
{
    private String jenkinsHome;
    private String runTestRegex;
    private String pollWait;
    private String maxPoll;
    private List<SMAMetadata> contents;
    private File testWorkspace;
    private String testWorkspacePath;

    @Before
    public void setUp() throws Exception
    {
        //Setup the fake workspace and package manifest
        testWorkspace = File.createTempFile("TestWorkspace", "");
        testWorkspace.delete();
        testWorkspace.mkdirs();
        testWorkspacePath = testWorkspace.getPath();

        String emptyString = "";

        SMAMetadata apex = SMAMetadataTypes.createMetadataObject("/src/classes/TestApex.cls", emptyString.getBytes());
        SMAMetadata trigger = SMAMetadataTypes.createMetadataObject("/src/triggers/TestTrigger.trigger", emptyString.getBytes());
        SMAMetadata page = SMAMetadataTypes.createMetadataObject("/src/pages/TestPage.page", emptyString.getBytes());
        SMAMetadata workflow = SMAMetadataTypes.createMetadataObject("/src/workflows/TestWorkflow.workflow", emptyString.getBytes());

        contents = Arrays.asList(apex, trigger, page, workflow);
    }

    @Test
    public void testPackage() throws Exception
    {
        SMAPackage testPackage = new SMAPackage(contents, false);

        System.out.println(testPackage.getPackage());

        Assert.assertTrue(testPackage.getPackage().contains("Workflow"));
    }

    @Test
    public void testDestructiveChange() throws Exception
    {
        SMAPackage testPackage = new SMAPackage(contents, true);

        System.out.println(testPackage.getPackage());

        Assert.assertTrue(!testPackage.getPackage().contains("Workflow"));
    }

    @After
    public void tearDown() throws Exception
    {
        FileUtils.deleteDirectory(testWorkspace);
    }
}
