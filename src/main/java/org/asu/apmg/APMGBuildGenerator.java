package org.asu.apmg;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by anthony on 2/3/15.
 */
public class APMGBuildGenerator {

    private static final Logger LOG = Logger.getLogger(APMGBuildGenerator.class.getName());
    private static final ClassLoader loader = APMGBuildGenerator.class.getClassLoader();


    public static void generateBuildFile(String buildLocation, Boolean generateUnitTests, ArrayList<String> repoContents){
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
            projectRoot.setAttribute("name", "APMG Build File");
            build.appendChild(projectRoot);

            //Set the properties
            Element environment = build.createElement("property");
            environment.setAttribute("environment", "env");
            projectRoot.appendChild(environment);

            //Set up the taskdef for the salesforce antlib
            String pathToAntLib = loader.getResource("org/asu/apmg/ant-salesforce.jar").getPath();
            Element salesforceAntLib = build.createElement("taskdef");
            salesforceAntLib.setAttribute("resource", "com/salesforce/antlib.xml");
            salesforceAntLib.setAttribute("classpath", pathToAntLib);
            salesforceAntLib.setAttribute("uri", "antlib:com.salesforce");
            projectRoot.appendChild(salesforceAntLib);

            //Create the target
            Comment targetComment = build.createComment("APMG Generated target");
            Element target = build.createElement("target");
            target.setAttribute("name", "apmg");
            projectRoot.appendChild(targetComment);
            projectRoot.appendChild(target);

            //Create the sf deploy
            Element sfDeploy = build.createElement("sf:deploy");
            sfDeploy.setAttribute("username", "${sf.username}");
            sfDeploy.setAttribute("password", "${sf.password}");
            sfDeploy.setAttribute("serverurl", "${sf.serverurl}");
            sfDeploy.setAttribute("maxPoll", "20");
            sfDeploy.setAttribute("pollWaitMillis", "30000");
            sfDeploy.setAttribute("deployRoot", buildLocation);
            sfDeploy.setAttribute("checkOnly", "true");

            //If indicated, create the test suite
            if(generateUnitTests){
                //String testKey = new String("(Test|test)");
                APMGManifestGenerator.APMGMetadataXmlDocument.initDocument();
                String testPattern = ".*[T|t]est.*";
                for (String file : repoContents){
                    if(file.matches(testPattern)){
                        APMGMetadataObject testClass = APMGManifestGenerator.
                                APMGMetadataXmlDocument.createMetadataObject(file);
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
            APMGUtility.writeXML(buildLocation, build);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
