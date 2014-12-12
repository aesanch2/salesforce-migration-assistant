package org.asu.apmg;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class APMGUtilityTest {

    private Repository repository;
    private APMGGit git;
    private File addition, modification, deletion, localPath;
    private String oldSha, newSha, gitDir;
    private ArrayList<APMGMetadataObject> expectedList;

    @Before
    public void setUp() throws Exception{
        //Setup the fake repository
        localPath = File.createTempFile("TestGitRepository", "");
        localPath.delete();
        repository = FileRepositoryBuilder.create(new File(localPath, ".git"));
        repository.create();

        File classesPath = new File(repository.getDirectory().getParent() + "/src/class");
        classesPath.mkdirs();
        File pagesPath = new File(repository.getDirectory().getParent() + "/src/pages");
        pagesPath.mkdirs();
        File triggersPath = new File(repository.getDirectory().getParent() + "/src/triggers");
        triggersPath.mkdirs();


        //Add the first collection of files
        deletion = new File(classesPath, "deleteThis.cls");
        modification = new File(pagesPath, "modifyThis.page");
        deletion.createNewFile();
        modification.createNewFile();
        PrintWriter print = new PrintWriter(deletion);
        print.println("This is the deleteThis file contents.");
        print.close();
        print = new PrintWriter(modification);
        print.println("This is the modifyThis file contents.");
        print.close();
        new Git(repository).add().addFilepattern("src/class/deleteThis.cls").call();
        new Git(repository).add().addFilepattern("src/pages/modifyThis.page").call();

        //Create the first commit
        RevCommit firstCommit = new Git(repository).commit().setMessage("Add deleteThis and modifyThis").call();
        oldSha = firstCommit.getName();


        //Delete the deletion file, modify the modification file, and add the addition file
        new Git(repository).rm().addFilepattern("src/class/deleteThis.cls").call();
        modification.setExecutable(true);
        addition = new File(triggersPath, "addThis.trigger");
        addition.createNewFile();
        print = new PrintWriter(addition);
        print.println("This is the addThis file contents.");
        print.close();
        new Git(repository).add().addFilepattern("src/pages/modifyThis.page").call();
        new Git(repository).add().addFilepattern("src/triggers/addThis.trigger").call();
        new Git(repository).add().addFilepattern("src/class/deleteThis.cls").call();

        //Create the second commit
        RevCommit secondCommit = new Git(repository).commit().setMessage("Remove deleteThis. Modify " +
                "modifyThis. Add addThis").call();
        newSha = secondCommit.getName();

        gitDir = localPath.getPath() + "/.git";
    }

    @After
    public void tearDown() throws Exception{
        repository.close();
        FileUtils.deleteDirectory(localPath);
    }

    @Test
    public void replicateMembersTest() throws Exception{
        git = new APMGGit(gitDir, newSha, oldSha);

        ArrayList<String> destructiveChanges = git.getDeletions();
        ArrayList<String> changes = git.getNewChangeSet();
        String destination = localPath.getPath() + "/deployStage";

        ArrayList<APMGMetadataObject> members = APMGUtility.generateManifests(destructiveChanges, changes,
                destination);

        APMGUtility.replicateMembers(members, localPath.getPath(), destination);

        for (APMGMetadataObject member : members){
            File memberFile = new File(destination + "/" + member.getPath() + member.getFullName());
            assertTrue(memberFile.exists());
        }
    }


    @Test
    public void generateManifestsTest() throws Exception{
        //Setup the metadata xml document
        APMGGenerator.APMGMetadataXmlDocument.initDocument();

        //Create our expected members list
        expectedList = new ArrayList<APMGMetadataObject>();
        expectedList.add(APMGGenerator.APMGMetadataXmlDocument.createMetadataObject("src/triggers/addThis.trigger"));
        expectedList.add(APMGGenerator.APMGMetadataXmlDocument.createMetadataObject("src/pages/modifyThis.page"));

        git = new APMGGit(gitDir, newSha, oldSha);

        ArrayList<String> destructiveChanges = git.getDeletions();
        ArrayList<String> changes = git.getNewChangeSet();
        String destination = localPath.getPath() + "/rollback";

        ArrayList<APMGMetadataObject> results = APMGUtility.generateManifests(destructiveChanges, changes,
                destination);

        assertEquals(expectedList.size(), results.size());

        for(int i = 0; i< expectedList.size(); i++){
            assertEquals(expectedList.get(i).getFullName(), results.get(i).getFullName());
        }
    }

    @Test
    public void generateManifestsTestRollback() throws Exception{
        //Setup the metadata xml document
        APMGGenerator.APMGMetadataXmlDocument.initDocument();

        //Create our expected members list
        expectedList = new ArrayList<APMGMetadataObject>();
        expectedList.add(APMGGenerator.APMGMetadataXmlDocument.createMetadataObject("src/classes/deleteThis.cls"));
        expectedList.add(APMGGenerator.APMGMetadataXmlDocument.createMetadataObject("src/pages/modifyThis.page"));

        git = new APMGGit(gitDir, newSha, oldSha);

        ArrayList<String> destructiveChanges = git.getAdditions();
        ArrayList<String> changes = git.getOldChangeSet();
        String destination = localPath.getPath() + "/rollback";

        ArrayList<APMGMetadataObject> results = APMGUtility.generateManifests(destructiveChanges, changes,
                destination);

        assertEquals(expectedList.size(), results.size());

        for(int i = 0; i< expectedList.size(); i++){
            assertEquals(expectedList.get(i).getFullName(), results.get(i).getFullName());
        }
    }

    @Test
    public void zipRollbackPackageTest() throws Exception{
        git = new APMGGit(gitDir, newSha, oldSha);

        ArrayList<String> destructiveChanges = git.getAdditions();
        ArrayList<String> changes = git.getOldChangeSet();
        String destination = localPath.getPath() + "/rollback";

        ArrayList<APMGMetadataObject> members = APMGUtility.generateManifests(destructiveChanges, changes,
                destination);

        git.getPrevCommitFiles(members, destination);

        String jobName = "TestJob";
        String buildNumber = "TestBuildNumber";
        String buildTag = jobName + "-" + buildNumber;

        APMGUtility.zipRollbackPackage(destination, buildTag);

        File zipTest = new File(localPath.getPath() + "/" + buildTag + "-rollback.zip");
        assertTrue(zipTest.exists());
    }

}
