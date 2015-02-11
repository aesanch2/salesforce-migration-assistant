package org.asu.sma;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Utility class that generates the build XML file
 * @author aesanch2
 */
public class SMABuildGenerator {

    private static final Logger LOG = Logger.getLogger(SMABuildGenerator.class.getName());


    /**
     * Generates an xml file for salesforce deployments. Can also generate unit tests for the default namespace.
     * @param buildLocation
     * @param generateUnitTests
     * @param deployRoot
     * @param repoContents
     */
    public static void generateBuildFile(String buildLocation, Boolean generateUnitTests,
                                         Boolean validate, String deployRoot,
                                         ArrayList<String> repoContents,
                                         String jenkinsPluginHome){
        try{
            //Create the build file
            DocumentBuilderFactory antFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder antBuilder = antFactory.newDocumentBuilder();
            Document build = antBuilder.newDocument();

            //Set the project definition
            Element projectRoot = build.createElement("project");
            projectRoot.setAttribute("xmlns:sf", "antlib:com.salesforce");
            projectRoot.setAttribute("basedir", ".");
            projectRoot.setAttribute("default", "build");
            projectRoot.setAttribute("name", "SMA Build File");
            build.appendChild(projectRoot);

            //Set the properties
            Element environment = build.createElement("property");
            environment.setAttribute("environment", "env");
            projectRoot.appendChild(environment);

            //Set up the taskdef for the salesforce antlib
            String pathToAntLib = jenkinsPluginHome + "/WEB-INF/lib/ant-salesforce.jar";
            Element salesforceAntLib = build.createElement("taskdef");
            salesforceAntLib.setAttribute("resource", "com/salesforce/antlib.xml");
            salesforceAntLib.setAttribute("classpath", pathToAntLib);
            salesforceAntLib.setAttribute("uri", "antlib:com.salesforce");
            projectRoot.appendChild(salesforceAntLib);

            //Create the target
            Comment targetComment = build.createComment("SMA Generated target");
            Element target = build.createElement("target");
            target.setAttribute("name", "sma");
            projectRoot.appendChild(targetComment);
            projectRoot.appendChild(target);

            //Create the sf deploy
            Element sfDeploy = build.createElement("sf:deploy");
            sfDeploy.setAttribute("username", "${sf.username}");
            sfDeploy.setAttribute("password", "${sf.password}");
            sfDeploy.setAttribute("serverurl", "${sf.serverurl}");
            sfDeploy.setAttribute("maxPoll", "20");
            sfDeploy.setAttribute("pollWaitMillis", "30000");
            sfDeploy.setAttribute("deployRoot", deployRoot);
            if(validate){
                sfDeploy.setAttribute("checkOnly", "true");
            }else {
                sfDeploy.setAttribute("checkOnly", "false");
            }

            //If indicated, create the test suite
            if(generateUnitTests){
                //String testKey = new String("(Test|test)");
                SMAManifestGenerator.SMAMetadataXMLDocument.initDocument();
                String testPattern = ".*[T|t]est.*";
                for (String file : repoContents){
                    if(file.matches(testPattern)){
                        SMAMetadata testClass = SMAManifestGenerator.SMAMetadataXMLDocument.createMetadataObject(file);
                        if(testClass.hasMetaxml()){
                            Element runTest = build.createElement("runTest");
                            runTest.setTextContent(testClass.getMember());
                            sfDeploy.appendChild(runTest);
                        }
                    }
                }
            }
            target.appendChild(sfDeploy);

            //Write the build file
            SMAUtility.writeXML(buildLocation, build);

            SMAUtility.removeFirstLine(buildLocation);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
