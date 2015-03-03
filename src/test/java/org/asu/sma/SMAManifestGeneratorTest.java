package org.asu.sma;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SMAManifestGeneratorTest {
    
    private String testWorkspacePath;
    private File testWorkspace;

    @Before
    public void setUp() throws Exception {
        //Setup the fake workspace and package manifest
        testWorkspace = File.createTempFile("TestWorkspace", "");
        testWorkspace.delete();
        testWorkspace.mkdirs();
        testWorkspacePath = testWorkspace.getPath();
    }

    @After
    public void tearDown() throws Exception{
        FileUtils.deleteDirectory(testWorkspace);
    }

    @Test
    public void testGenerateDestructive() throws Exception {
        File expectedPackage = new File("src/test/resources/testPackage.xml");

        //Read the testGitDiff and put the results into a list
        File testGitDiff = new File("src/test/resources/testDeletes.txt");
        ArrayList<String> testDiffs = read(testGitDiff);
        SMAPackage testPackage = new SMAPackage(testWorkspacePath, testDiffs, true);
        SMAManifestGenerator.generateManifest(testPackage);

        File resultPackage = new File(testPackage.getDestination());
        ArrayList<String> expectedOutput = read(expectedPackage);
        ArrayList<String> resultOutput = read(resultPackage);
        assertEquals(expectedOutput, resultOutput);
    }

    @Test
    public void testGenerate() throws Exception {
        File expectedPackage = new File("src/test/resources/testPackage.xml");

        //Read the testGitDiff and put the results into a list
        File testGitDiff = new File("src/test/resources/testAddsMods.txt");
        ArrayList<String> testDiffs = read(testGitDiff);
        SMAPackage testPackage = new SMAPackage(testWorkspacePath, testDiffs, false);
        SMAManifestGenerator.generateManifest(testPackage);

        File resultPackage = new File(testPackage.getDestination());
        ArrayList<String> expectedOutput = read(expectedPackage);
        ArrayList<String> resultOutput = read(resultPackage);
        assertEquals(expectedOutput, resultOutput);
    }

    private static ArrayList<String> read(File file) throws IOException{
        Scanner scanner = new Scanner(file);
        ArrayList<String> results = new ArrayList<String>();

        while(scanner.hasNext()){
            results.add(scanner.nextLine());
        }

        return results;
    }

    @Test
    public void testGetPathToResource() throws Exception {
        ClassLoader loader = SMAManifestGenerator.SMAMetadataXMLDocument.class.getClassLoader();
        String expected = loader.getResource("org/asu/sma/salesforceMetadata.xml").toString();
        assertEquals(expected, SMAManifestGenerator.SMAMetadataXMLDocument.getPathToResource());
    }

    @Test
    public void testGetAPIVersion() throws Exception {
        String API = "32.0";
        assertEquals(API, SMAManifestGenerator.SMAMetadataXMLDocument.getAPIVersion());
    }

    @Test
    public void testGetDoc() throws Exception{
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dbBuilder = dbFactory.newDocumentBuilder();
        Document doc = dbBuilder.parse("src/main/resources/org/asu/sma/salesforceMetadata.xml");

        assertEquals(doc.toString(), SMAManifestGenerator.SMAMetadataXMLDocument.getDoc().toString());
    }

    @Test
    public void testCreateMetadataObject() throws Exception {
        //Test any metadata type. We can make the assumption that if one works, they all work.
        String extension = "object";
        String container = "objects";
        String metadata = "CustomObject";
        String member = "Test";
        String path = "src/objects/";

        String gitData = "src/objects/Test.object";
        SMAMetadata testMD = SMAManifestGenerator.SMAMetadataXMLDocument.createMetadataObject(gitData);

        assertEquals(extension, testMD.getExtension());
        assertEquals(container, testMD.getContainer());
        assertEquals(metadata, testMD.getMetadataType());
        assertEquals(member, testMD.getMember());
        assertEquals(path, testMD.getPath());
        assertTrue(testMD.isDestructible());
    }
}