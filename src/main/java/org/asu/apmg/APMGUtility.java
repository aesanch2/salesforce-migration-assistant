package org.asu.apmg;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for performing a variety of tasks in APMG.
 * @author aesanch2
 */
public class APMGUtility {

    /**
     * Copies all necessary files to the deployment stage.
     * @param members The list of metadata members to replicate.
     * @param sourceDir The directory where the members are located.
     * @param destDir The destination to copy the members to.
     * @throws IOException
     */
    public static void replicateMembers(ArrayList<APMGMetadataObject> members,
                                 String sourceDir, String destDir) throws IOException{
        for(APMGMetadataObject file : members){
            File source = new File(sourceDir + "/" + file.getPath() + file.getFullName());
            File destination = new File(destDir + "/" + file.getPath());
            if(!destination.exists()){
                destination.mkdirs();
            }
            FileUtils.copyFileToDirectory(source, destination);
        }
    }

    /**
     * Helper method that generates the package manifest files.
     * @param destructiveChanges The list of items that were deleted from the repository in this commit.
     * @param changes The list of items that were added or modified from the repository in this commit.
     * @param destination The destination of the package manifest file.
     * @return An ArrayList of the APMGMetadataObjects that were included in this commit.
     */
    public static ArrayList<APMGMetadataObject> generateManifests(ArrayList<String> destructiveChanges,
                                                                  ArrayList<String> changes,
                                                                  String destination){
        Boolean isDestructiveChange = true;
        //Generate the destructiveChanges.xml file
        if(!destructiveChanges.isEmpty()){
            String destructiveChangesFile = destination + "/src/destructiveChanges.xml";
            APMGGenerator.generate(destructiveChanges, destructiveChangesFile, isDestructiveChange);
        }
        String packageManifest= destination + "/src/package.xml";

        return APMGGenerator.generate(changes, packageManifest, !isDestructiveChange);
    }

    /**
     * Helper method that generates the rollback package zip file.
     * @param rollbackDirectory The location of the directory where the rollback items are located.
     * @param buildTag The build tag created by Jenkins for this job.
     * @throws Exception
     */
    public static String zipRollbackPackage(File rollbackDirectory,
                                          String buildTag) throws Exception{
        String zipFile = "/"+ FilenameUtils.getPath(rollbackDirectory.getPath()) + buildTag + "-APMGrollback.zip";
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
