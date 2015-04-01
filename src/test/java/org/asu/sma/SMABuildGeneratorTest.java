package org.asu.sma;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import static org.junit.Assert.assertTrue;

public class SMABuildGeneratorTest {
    private String testWorkspacePath;
    private ArrayList<String> testContents;
    private File testWorkspace;
    private static final ClassLoader loader = SMABuildGeneratorTest.class.getClassLoader();

    @Before
    public void setUp() throws Exception {
        //Setup the fake workspace and build file
        testWorkspace = File.createTempFile("TestWorkspace", "");
        testWorkspace.delete();
        testWorkspace.mkdirs();
        testWorkspacePath = testWorkspace.getPath();

        testContents = new ArrayList<String>();
        testContents.add("src/classes/Test_Metadata.cls");
        testContents.add("src/classes/test_metadata.cls");
        testContents.add("src/classes/MetadataTest.cls");
        testContents.add("src/classes/Metadata_Test.cls");
        testContents.add("src/classes/Metadata_test_.cls");
        testContents.add("src/classes/Test_Metadata.cls-meta.xml");
    }

    @After
    public void tearDown() throws Exception{
        FileUtils.deleteDirectory(testWorkspace);
    }

    @Test
    public void testGenerate() throws Exception {
        String jenkinsHome = "/var/lib/jenkins";
        String runTestRegex = ".*[T|t]est.*";
        String pollWait = "30000";
        String maxPoll = "20";
        boolean generateUnitTests = true;
        boolean validateOnly = true;

        SMAPackage buildPackage = new SMAPackage(testWorkspacePath, testContents,
                jenkinsHome, runTestRegex, pollWait, maxPoll, generateUnitTests, validateOnly,
                "user@user.com.test", "${sf.password}", "Sandbox");
        SMABuildGenerator.generateBuildFile(buildPackage, true);


        boolean unitTestGenerated = false;
        File resultBuild = new File(buildPackage.getDestination());
        ArrayList<String> resultOutput = read(resultBuild);
        for(String output : resultOutput) {
            if(output.contains("runTest")){
                unitTestGenerated = true;
            }
        }
        assertTrue(unitTestGenerated);

        unitTestGenerated = false;
        SMABuildGenerator.generateBuildFile(buildPackage, false);
        File noUnitTests = new File(buildPackage.getDestination());
        ArrayList<String> noUnitTestsOutput = read(noUnitTests);
        for(String noUnitTestOut : noUnitTestsOutput) {
            if(noUnitTestOut.contains("runTest")){
                unitTestGenerated = true;
            }
        }
        assertTrue(!unitTestGenerated);
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
