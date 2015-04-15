package org.asu.sma;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.logging.Logger;

/**
 * Utility class that generates the build XML file
 * @author aesanch2
 */
public class SMABuildGenerator {

    private static final Logger LOG = Logger.getLogger(SMABuildGenerator.class.getName());


    /**
     * Generates an xml file for salesforce deployments. Can also generate unit tests for the default namespace.
     * @param buildPackage
     */
    public static String generateBuildFile(SMAPackage buildPackage, Boolean apexChangePresent){
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
            String pathToAntLib = buildPackage.getPluginHome() + "/WEB-INF/lib/ant-salesforce.jar";
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
            sfDeploy.setAttribute("username", buildPackage.getUsername());
            sfDeploy.setAttribute("password", buildPackage.getPassword());
            sfDeploy.setAttribute("serverurl", buildPackage.getServer());
            sfDeploy.setAttribute("maxPoll", buildPackage.getMaxPoll());
            sfDeploy.setAttribute("pollWaitMillis", buildPackage.getPollWait());
            sfDeploy.setAttribute("deployRoot", buildPackage.getWorkspace());
            if(buildPackage.isValidateOnly()){
                sfDeploy.setAttribute("checkOnly", "true");
            }else {
                sfDeploy.setAttribute("checkOnly", "false");
            }

            //If indicated and there are .cls or .trigger changes, create the test suite
            if(buildPackage.isGenerateUnitTests() && apexChangePresent){
                SMAManifestGenerator.SMAMetadataXMLDocument.initDocument();
                String testPattern = buildPackage.getRunTestRegex();
                for (String file : buildPackage.getContents()){
                    if(file.matches(testPattern)){
                        SMAMetadata testClass = SMAManifestGenerator.SMAMetadataXMLDocument.
                                createMetadataObject(file);
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
            SMAUtility.writeXML(buildPackage.getDestination(), build);
            SMAUtility.removeFirstLine(buildPackage.getDestination());
        }catch(Exception e){
            e.printStackTrace();
        }
        return buildPackage.getDestination();
    }
}
