package org.asu.sma;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SMAPackageTest {
    private String jenkinsHome;
    private String runTestRegex;
    private String pollWait;
    private String maxPoll;
    private ArrayList<String> contents;
    private File testWorkspace;
    private String testWorkspacePath;

    @Before
    public void setUp() throws Exception{
        //Setup the fake workspace and package manifest
        testWorkspace = File.createTempFile("TestWorkspace", "");
        testWorkspace.delete();
        testWorkspace.mkdirs();
        testWorkspacePath = testWorkspace.getPath();

        jenkinsHome = "/var/lib/jenkins";
        runTestRegex = ".*[T|t]est.*";
        pollWait = "30000";
        maxPoll = "20";

        File testContents = new File("src/test/resources/testAddsMods.txt");
        contents = read(testContents);
    }

    @After
    public void tearDown() throws Exception{
        FileUtils.deleteDirectory(testWorkspace);
    }

    @Test
    public void testPackageDestination() throws Exception{
        SMAPackage manifest = new SMAPackage(testWorkspacePath, contents, false);

        assertEquals(testWorkspacePath + "/src/package.xml", manifest.getDestination());
    }

    @Test
    public void testDestructiveDestination() throws Exception{
        SMAPackage manifest = new SMAPackage(testWorkspacePath, contents, true);

        assertEquals(testWorkspacePath + "/src/destructiveChanges.xml", manifest.getDestination());
    }

    @Test
    public void testBuildDestination() throws Exception{
        SMAPackage build = new SMAPackage(testWorkspacePath, contents, jenkinsHome,
                runTestRegex, pollWait, maxPoll, true, true, "user@user.com.test",
                "12345678!", "Sandbox");

        assertEquals(testWorkspacePath + "/build/build.xml", build.getDestination());
    }

    @Test
    public void testPluginHome() throws Exception{
        SMAPackage build = new SMAPackage(testWorkspacePath, contents, jenkinsHome,
                runTestRegex, pollWait, maxPoll, true, true, "user@user.com.test",
                "12345678!", "Sandbox");

        assertEquals(jenkinsHome + "/plugins/sma", build.getPluginHome());
    }

    @Test
    public void testEmptyPassword() throws Exception{
        SMAPackage build = new SMAPackage(testWorkspacePath, contents, jenkinsHome,
                runTestRegex, pollWait, maxPoll, true, true, "user@user.com.test",
                "", "Sandbox");

        assertEquals("{$sf.password}", build.getPassword());
    }

    private static ArrayList<String> read(File file) throws IOException {
        Scanner scanner = new Scanner(file);
        ArrayList<String> results = new ArrayList<String>();

        while(scanner.hasNext()){
            results.add(scanner.nextLine());
        }

        return results;
    }
}
