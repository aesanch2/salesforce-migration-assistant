package org.asu.sma;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for performing a variety of tasks in SMA.
 * @author aesanch2
 */
public class SMAUtility {

    private static final Logger LOG = Logger.getLogger(SMAUtility.class.getName());

    /**
     * Writes an XML Document to disk.
     * @param destination
     * @param xmlToWrite
     */
    public static void writeXML(String destination, Document xmlToWrite){
        try {
            //Prepare the workspace for the manifest
            File directory = new File(FilenameUtils.getFullPath(destination));
            directory.mkdirs();

            //Write the manifest
            DOMSource source = new DOMSource(xmlToWrite);
            StreamResult result = new StreamResult(new File(destination));
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(source, result);

            LOG.info("Saved xml file to " + destination);
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Removes the first line from a file.
     * @param fileName
     * @throws IOException
     */
    public static void removeFirstLine(String fileName) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
        //Initial write position
        long writePosition = raf.getFilePointer();
        raf.readLine();
        // Shift the next lines upwards.
        long readPosition = raf.getFilePointer();

        byte[] buff = new byte[1024];
        int n;
        while (-1 != (n = raf.read(buff))) {
            raf.seek(writePosition);
            raf.write(buff, 0, n);
            readPosition += n;
            writePosition += n;
            raf.seek(readPosition);
        }
        raf.setLength(writePosition);
        raf.close();
    }

    /**
     * Copies all necessary files to the deployment stage.
     * @param members The list of metadata members to replicate.
     * @param sourceDir The directory where the members are located.
     * @param destDir The destination to copy the members to.
     * @throws IOException
     */
    public static void replicateMembers(ArrayList<SMAMetadata> members,
                                 String sourceDir, String destDir) throws IOException{
        for(SMAMetadata file : members){
            File source = new File(sourceDir + "/" + file.getPath() + file.getFullName());
            File destination = new File(destDir + "/" + file.getPath());
            if(!destination.exists()){
                destination.mkdirs();
            }
            FileUtils.copyFileToDirectory(source, destination);

            //Copy the accompanying -meta.xml file if appropriate. Throw exception if not found.
            if(file.hasMetaxml()){
                File metaXML = new File(sourceDir + "/" + file.getPath() + file.getFullName() + "-meta.xml");
                if (metaXML.exists()) {
                    FileUtils.copyFileToDirectory(metaXML, destination);
                }
                else{
                    throw new FileNotFoundException("Could not locate the metadata file for '" +
                    file.getFullName() + "'. Perhaps you forgot to commit it?");
                }
            }
        }
    }

    /**
     * Helper method that generates the salesforce manifest files.
     * @param destructiveChanges The list of items that were deleted from the repository in this commit.
     * @param changes The list of items that were added or modified from the repository in this commit.
     * @param deployStage The sma deployment directory.
     * @return An ArrayList of the SMAMetadata objects that were included in this commit.
     */
    public static ArrayList<SMAMetadata> generate(ArrayList<String> destructiveChanges,
                                                                  ArrayList<String> changes,
                                                                  String deployStage){
        Boolean isDestructiveChange = true;
        //Generate the destructiveChanges.xml file
        if(!destructiveChanges.isEmpty()){
            SMAPackage destructiveManifest = new SMAPackage(deployStage, destructiveChanges, isDestructiveChange);
            SMAManifestGenerator.generateManifest(destructiveManifest);
        }
        //Generate the package.xml file
        SMAPackage packageManifest = new SMAPackage(deployStage, changes, !isDestructiveChange);
        return SMAManifestGenerator.generateManifest(packageManifest);
    }

    /**
     * Helper method that generates the rollback package zip file.
     * @param rollbackDirectory The location of the directory where the rollback items are located.
     * @param buildTag The build tag created by Jenkins for this job.
     * @throws Exception
     */
    public static String zipRollbackPackage(File rollbackDirectory, String buildTag) throws Exception{
        String zipFile = "/"+ FilenameUtils.getPath(rollbackDirectory.getPath()) + buildTag
                + "-SMArollback.zip";
        String srcDir = rollbackDirectory.getPath();

        FileOutputStream fop = new FileOutputStream(zipFile);
        ZipOutputStream zop = new ZipOutputStream(fop);

        File srcFile = new File(srcDir);

        addDirToArchive(zop, srcFile);

        zop.close();
        fop.close();

        return zipFile;
    }

    /**
     * Helper method that aids zipRollbackPackage in recursively creating a zip file from a directory's contents.
     * @param zop ZipOutputStream from zipRollbackPackage.
     * @param srcFile The files in this directory to be zipped.
     * @throws Exception
     */
    private static void addDirToArchive(ZipOutputStream zop, File srcFile) throws Exception{
        File[] files = srcFile.listFiles();

        for (File file : files){
            if (file.isDirectory()){
                addDirToArchive(zop, file);
                continue;
            }

            byte[] buffer = new byte[1024];

            FileInputStream fis = new FileInputStream(file);
            zop.putNextEntry(new ZipEntry(file.getName()));

            int length;
            while((length = fis.read(buffer)) > 0){
                zop.write(buffer, 0, length);
            }

            zop.closeEntry();
            fis.close();
        }
    }
}
