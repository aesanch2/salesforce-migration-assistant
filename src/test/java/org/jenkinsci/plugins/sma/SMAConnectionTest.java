package org.jenkinsci.plugins.sma;

import com.google.common.io.Files;
import com.sforce.soap.metadata.CodeCoverageResult;
import com.sforce.soap.metadata.DeployDetails;
import com.sforce.soap.metadata.RunTestsResult;
import com.sforce.soap.metadata.TestLevel;
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

public class SMAConnectionTest
{
    //TODO: need to mock this configuration
    SMAConnection sfConnection;
    String username = "";
    String password = "";
    String securityToken = "";
    String server = "";
    String proxyServer = "";
    String proxyUser = "";
    String proxyPass = "";
    Integer proxyPort;
    File localPath;
    ByteArrayOutputStream boas;

    @Before
    public void setUp() throws Exception
    {
        localPath = Files.createTempDir();

        String apex = "public class TestApex {}";
        StringBuilder sb = new StringBuilder();
        sb.append("<ApexClass xmlns=\"http://soap.sforce.com/2006/04/metadata\">");
        sb.append("<apiVersion>34.0</apiVersion>");
        sb.append("<status>Active</status>");
        sb.append("</ApexClass>");

        Map<String, byte[]> metadata = new HashMap<String, byte[]>();
        metadata.put("classes/TestApex.cls", apex.getBytes());
        metadata.put("classes/TestApex.cls-meta.xml", sb.toString().getBytes());

        List<SMAMetadata> metadataList = new ArrayList<SMAMetadata>();

        for (String s : metadata.keySet())
        {
            if (!s.contains("-meta.xml"))
            {
                metadataList.add(SMAMetadataTypes.createMetadataObject(s, metadata.get(s)));
            }
        }

        SMAPackage packageManifest = new SMAPackage(metadataList, false);
        SMAPackage destructiveChange = new SMAPackage(new ArrayList<SMAMetadata>(), true);

        boas = SMAUtility.zipPackage(metadata, packageManifest, destructiveChange);

        SMAUtility.writeZip(boas, localPath.getPath() + "/testDeploy.zip");


    }

    @Test
    public void testDeployment() throws Exception
    {
        boolean success;

        if (username.isEmpty() || password.isEmpty() || securityToken.isEmpty())
        {
            success = true;
        }
        else
        {
            sfConnection = new SMAConnection(
                    username,
                    password,
                    securityToken,
                    server,
                    "30000",
                    "200",
                    proxyServer,
                    proxyUser,
                    proxyPass,
                    proxyPort
            );

            success = sfConnection.deployToServer(
                    boas,
                    TestLevel.NoTestRun,
                    null,
                    true,
                    true
            );
        }

        Assert.assertTrue(success);
    }

    @Test
    public void testGetCodeCoverageResults() throws Exception
    {
        if (username.isEmpty() || password.isEmpty() || securityToken.isEmpty())
        {
            Assert.assertTrue(true);
        }
        else
        {
            sfConnection = new SMAConnection(
                    username,
                    password,
                    securityToken,
                    server,
                    "30000",
                    "200",
                    proxyServer,
                    proxyUser,
                    proxyPass,
                    proxyPort
            );

            StringBuilder sb = new StringBuilder();
            sb.append(
                    "[SMA] Code Coverage Results\n" +
                            "1st Test.cls -- 80%\n" +
                            "2nd Test.cls -- 80%\n" +
                            "\n" +
                            "Total code coverage for this deployment -- 80%" +
                            "\n"
            );
            String expectedCoverage = sb.toString();
            DeployDetails details = new DeployDetails();
            RunTestsResult testsResult = new RunTestsResult();

            CodeCoverageResult testCCR1 = new CodeCoverageResult();
            testCCR1.setName("1st Test");
            testCCR1.setNumLocations(10);
            testCCR1.setNumLocationsNotCovered(2);
            CodeCoverageResult testCCR2 = new CodeCoverageResult();
            testCCR2.setName("2nd Test");
            testCCR2.setNumLocations(20);
            testCCR2.setNumLocationsNotCovered(4);

            CodeCoverageResult[] expectedCCR = new CodeCoverageResult[]{testCCR1, testCCR2};

            testsResult.setCodeCoverage(expectedCCR);
            details.setRunTestResult(testsResult);

            sfConnection.setDeployDetails(details);

            String actualCoverage = sfConnection.getCodeCoverage();
            Assert.assertEquals(expectedCoverage, actualCoverage);
        }
    }

    @After
    public void tearDown() throws Exception
    {
        localPath.delete();
    }
}
