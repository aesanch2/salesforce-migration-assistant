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

public class APMGUtility {

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
        ArrayList<APMGMetadataObject> members = APMGGenerator.generate(changes, packageManifest, !isDestructiveChange);

        return members;
    }

    public static void zipRollbackPackage(String rollbackDirectory,
                                          String jobName,
                                          String buildNumber) throws Exception{
        String zipFile = "/"+ FilenameUtils.getPath(rollbackDirectory) + jobName + "_" + buildNumber + "_rollback.zip";
        String srcDir = rollbackDirectory;

        FileOutputStream fop = new FileOutputStream(zipFile);
        ZipOutputStream zop = new ZipOutputStream(fop);

        File srcFile = new File(srcDir);

        addDirToArchive(zop, srcFile);

        zop.close();
        fop.close();
    }

    private static void addDirToArchive(ZipOutputStream zop, File srcFile) throws Exception{
        File[] files = srcFile.listFiles();

        for (int i = 0; i < files.length; i++){
            if (files[i].isDirectory()){
                addDirToArchive(zop, files[i]);
                continue;
            }

            byte[] buffer = new byte[1024];

            FileInputStream fis = new FileInputStream(files[i]);
            zop.putNextEntry(new ZipEntry(files[i].getName()));

            int length;
            while((length = fis.read(buffer)) > 0){
                zop.write(buffer, 0, length);
            }

            zop.closeEntry();
            fis.close();
        }
    }
}
