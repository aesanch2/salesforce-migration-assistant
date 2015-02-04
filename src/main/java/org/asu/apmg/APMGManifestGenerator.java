package org.asu.apmg;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Utility class that generates the package manifest XML file.
 * @author aesanch2
 */
public class APMGManifestGenerator {

    private static final Logger LOG = Logger.getLogger(APMGManifestGenerator.class.getName());

    /**
     * Generates an xml file that describes the metadata that is to be deployed in this job.
     * @param memberList
     * @param manifestLocation
     * @param isDestructiveChange
     * @return The ArrayList of APMGMetadataObjects that are to be deployed in this job.
     */
    public static ArrayList<APMGMetadataObject> generateManifest(ArrayList<String> memberList,
                                                                 String manifestLocation,
                                                                 Boolean isDestructiveChange){
        ArrayList<APMGMetadataObject> contents = new ArrayList<APMGMetadataObject>();

        try {
            //Create the manifest
            DocumentBuilderFactory manifestFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder manifestBuilder = manifestFactory.newDocumentBuilder();
            Document manifest = manifestBuilder.newDocument();

            //Set the root element and the namespace for the manifest
            Element rootElement = manifest.createElement("Package");
            rootElement.setAttribute("xmlns", "http://soap.sforce.com/2006/04/metadata");
            manifest.appendChild(rootElement);

            //Setup xpath querying
            XPathFactory pathFactory = XPathFactory.newInstance();
            XPath xpath = pathFactory.newXPath();
            XPathExpression query;
            
            APMGMetadataObject metadata;
            Boolean typeExists;
            String xpathExpr;
            APMGMetadataXmlDocument.initDocument();
            for(String repoItem : memberList){
                metadata = APMGMetadataXmlDocument.createMetadataObject(repoItem);

                //Handle unknown members
                if(metadata.getMetadataType().equals("Invalid")) {
                    if (metadata.getFullName().contains("-meta")){
                        contents.add(metadata);
                        LOG.fine(metadata.getFullName() + " is a valid member, but unnecessary for manifest");
                        continue;
                    }else {
                        LOG.warning(metadata.getFullName() + " is not a valid member of the API");
                        continue;
                    }
                }

                //Logging for debugging purposes
                LOG.finest("Member is " + metadata.getMember());
                LOG.finest("Extension is " + metadata.getExtension());
                LOG.finest("Container is " + metadata.getContainer());
                LOG.finest("Path is " + metadata.getPath());

                //Check to make sure the metadata can be deleted if this is a destructiveChange
                if (!metadata.isDestructible() && isDestructiveChange){
                    LOG.warning(metadata.getFullName() + " cannot be deleted via the API");
                }else{
                    //Query the document to see if the metadataType node already exists for this metadata
                    xpathExpr = "//name[text()='" + metadata.getMetadataType() + "']";
                    query = xpath.compile(xpathExpr);
                    typeExists = (Boolean) query.evaluate(manifest, XPathConstants.BOOLEAN);
                    LOG.fine("Xpath query returned " + typeExists);

                    //Generate the new member element
                    LOG.fine("Generating new member for " + metadata.getMember());
                    Element newMember = manifest.createElement("members");
                    newMember.setTextContent(metadata.getMember());

                    //This sections is where the member and/or typename is added to the package manifest
                    if (typeExists){
                        //Find the type node that this member should be appended to
                        NodeList nameNodes = manifest.getElementsByTagName("name");
                        for (int iterator = 0; iterator < nameNodes.getLength(); iterator++) {
                            Element name = (Element) nameNodes.item(iterator);

                            if(name.getTextContent().equals(metadata.getMetadataType())){
                                Node parentType = name.getParentNode();
                                parentType.appendChild(newMember);
                                break;
                            }
                        }
                        contents.add(metadata);
                    }else{
                        //Generate a new type and name node
                        LOG.fine("Generating new type and name for " + metadata.getMetadataType());
                        Element newType = manifest.createElement("types");
                        Element newName = manifest.createElement("name");
                        newName.setTextContent(metadata.getMetadataType());
                        newType.appendChild(newName);
                        newType.appendChild(newMember);
                        rootElement.appendChild(newType);
                        contents.add(metadata);
                    }
                }
            }



            //Add the version element to the manifest
            String version = APMGMetadataXmlDocument.getAPIVersion();
            Element verElement = manifest.createElement("version");
            verElement.setTextContent(version);
            rootElement.appendChild(verElement);

            //Write the manifest
            APMGUtility.writeXML(manifestLocation, manifest);
        }catch(Exception e){
            e.printStackTrace();
        }

        return contents;
    }

    /**
     * Sub-class for the salesforceMetadata.xml document that contains Salesforce Metadata API information.
     * @author aesanch2
     */
    public static final class APMGMetadataXmlDocument {
        private static final ClassLoader loader = APMGManifestGenerator.APMGMetadataXmlDocument.class.getClassLoader();
        private static String pathToResource = loader.getResource("org/asu/apmg/salesforceMetadata.xml").toString();
        private static Document doc;

        /**
         * Initializes the Document representation of the salesforceMetadata.xml file
         * @throws Exception
         */
        public static void initDocument() throws Exception {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dbBuilder = dbFactory.newDocumentBuilder();
            doc = dbBuilder.parse(pathToResource);
        }

        /**
         * Returns the path to the salesforceMetadata.xml resource.
         * @return The path to the salesforceMetadata.xml resource.
         */
        public static String getPathToResource() {
            return pathToResource;
        }

        /**
         * Returns the Document object representation of the salesforceMetadata.xml file.
         * @return The Document object representation of the salesforceMetadata.xml file.
         */
        public static Document getDoc() {
            return doc;
        }

        /**
         * Returns the Salesforce Metadata API Version
         * @return A string representing the API version number.
         */
        public static String getAPIVersion() {
            String version = null;

            doc.getDocumentElement().normalize();

            NodeList verNodes = doc.getElementsByTagName("version");

            //There should only be one node in this list
            for (int iterator = 0; iterator < verNodes.getLength(); iterator++) {
                Node curNode = verNodes.item(iterator);
                Element verElement = (Element)curNode;
                //If for some reason there is more than one, get the first one
                version = verElement.getAttribute("API");
            }

            return version;
        }

        /**
         * Creates an APMGMetadataObject from a string representation of a file's path and filename.
         * @param filename
         * @return An APMGMetadataObject representation of a file.
         * @throws Exception
         */
        public static APMGMetadataObject createMetadataObject(String filename) throws Exception{
            String container = "empty";
            String metadataType = "Invalid";
            boolean destructible = false;
            boolean valid = false;
            boolean metaxml = false;

            File file = new File(filename);
            String object = file.getName();
            LOG.fine("Analyzing " + filename);
            String member = FilenameUtils.removeExtension(object);
            String extension = FilenameUtils.getExtension(filename);
            String path = FilenameUtils.getFullPath(filename);


            //Normalize the document
            doc.getDocumentElement().normalize();

            NodeList extNodes = doc.getElementsByTagName("extension");

            //Get the node with the corresponding extension and get the relevant information for
            //creating the APMGMetadataObject
            for (int iterator = 0; iterator < extNodes.getLength(); iterator++) {
                Node curNode = extNodes.item(iterator);

                Element element = (Element)curNode;
                if(element.getAttribute("name").equals(extension)){
                    container =  element.getElementsByTagName("container").item(0).getTextContent();
                    metadataType = element.getElementsByTagName("metadata").item(0).getTextContent();
                    destructible = Boolean.parseBoolean(element.getElementsByTagName("destructible").item(0).
                            getTextContent());
                    valid = true;
                    metaxml = Boolean.parseBoolean(element.getElementsByTagName("metaxml").item(0).getTextContent());
                    break;
                }
            }

            return new APMGMetadataObject(extension, container, member, metadataType,
                    path, destructible, valid, metaxml);
        }
    }
}
