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

public class SMABuildGeneratorTest {
    private String testWorkspacePath;
    private ArrayList<String> testContents;
    private File resultBuild, testWorkspace;

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

        resultBuild = new File(testWorkspace, "testBuild.xml");
    }

    @After
    public void tearDown() throws Exception{
        FileUtils.deleteDirectory(testWorkspace);
    }

    @Test
    public void testGenerate() throws Exception {
        SMABuildGenerator.generateBuildFile(resultBuild.getPath(), true, true,
                testWorkspacePath + "/src", testContents);

        ArrayList<String> output = read(resultBuild);
        System.out.println(output);
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
