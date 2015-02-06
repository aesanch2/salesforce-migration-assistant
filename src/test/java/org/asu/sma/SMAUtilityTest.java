package org.asu.sma;

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

public class SMAUtilityTest {

    private Repository repository;
    private SMAGit git;
    private File addition, addXml, modification, modXml, deletion, delXml, localPath;
    private String oldSha, newSha, gitDir;
    private ArrayList<SMAMetadata> expectedList;

    @Before
    public void setUp() throws Exception{
        //Setup the fake repository
        localPath = File.createTempFile("TestGitRepository", "");
        localPath.delete();
        repository = FileRepositoryBuilder.create(new File(localPath, ".git"));
        repository.create();

        File classesPath = new File(repository.getDirectory().getParent() + "/src/classes");
        classesPath.mkdirs();
        File pagesPath = new File(repository.getDirectory().getParent() + "/src/pages");
        pagesPath.mkdirs();
        File triggersPath = new File(repository.getDirectory().getParent() + "/src/triggers");
        triggersPath.mkdirs();


        //Add the first collection of files
        deletion = new File(classesPath, "deleteThis.cls");
        delXml = new File(classesPath, "deleteThis.cls-meta.xml");
        modification = new File(pagesPath, "modifyThis.page");
        modXml = new File(pagesPath, "modifyThis.page-meta.xml");
        deletion.createNewFile();
        delXml.createNewFile();
        modification.createNewFile();
        modXml.createNewFile();
        PrintWriter print = new PrintWriter(deletion);
        print.println("This is the deleteThis file contents.");
        print.close();
        print = new PrintWriter(modification);
        print.println("This is the modifyThis file contents.");
        print.close();
        new Git(repository).add().addFilepattern("src/classes/deleteThis.cls").call();
        new Git(repository).add().addFilepattern("src/classes/deleteThis.cls-meta.xml").call();
        new Git(repository).add().addFilepattern("src/pages/modifyThis.page").call();
        new Git(repository).add().addFilepattern("src/pages/modifyThis.page-meta.xml").call();

        //Create the first commit
        RevCommit firstCommit = new Git(repository).commit().setMessage("Add deleteThis and modifyThis").call();
        oldSha = firstCommit.getName();


        //Delete the deletion file, modify the modification file, and add the addition file
        new Git(repository).rm().addFilepattern("src/class/deleteThis.cls").call();
        new Git(repository).rm().addFilepattern("src/class/deleteThis.cls-meta.xml").call();
        modification.setExecutable(true);
        addition = new File(triggersPath, "addThis.trigger");
        addXml = new File(triggersPath, "addThis.trigger-meta.xml");
        addition.createNewFile();
        addXml.createNewFile();
        print = new PrintWriter(addition);
        print.println("This is the addThis file contents.");
        print.close();
        new Git(repository).add().addFilepattern("src/pages/modifyThis.page").call();
        new Git(repository).add().addFilepattern("src/triggers/addThis.trigger").call();
        new Git(repository).add().addFilepattern("src/triggers/addThis.trigger-meta.xml").call();
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
        git = new SMAGit(gitDir, newSha, oldSha);

        ArrayList<String> destructiveChanges = git.getDeletions();
        ArrayList<String> changes = git.getNewChangeSet();
        String destination = localPath.getPath() + "/deployStage";

        ArrayList<SMAMetadata> members = SMAUtility.generate(destructiveChanges, changes,
                destination);

        SMAUtility.replicateMembers(members, localPath.getPath(), destination);

        for (SMAMetadata member : members){
            File memberFile = new File(destination + "/" + member.getPath() + member.getFullName());
            assertTrue(memberFile.exists());
        }
    }


    @Test
    public void generateManifestsTest() throws Exception{
        //Setup the metadata xml document
        SMAManifestGenerator.SMAMetadataXMLDocument.initDocument();

        //Create our expected members list
        expectedList = new ArrayList<SMAMetadata>();
        expectedList.add(SMAManifestGenerator.SMAMetadataXMLDocument.createMetadataObject("src/triggers/addThis.trigger"));
        expectedList.add(SMAManifestGenerator.SMAMetadataXMLDocument.
                createMetadataObject("src/triggers/addThis.trigger-meta.xml"));
        expectedList.add(SMAManifestGenerator.SMAMetadataXMLDocument.createMetadataObject("src/pages/modifyThis.page"));

        git = new SMAGit(gitDir, newSha, oldSha);

        ArrayList<String> destructiveChanges = git.getDeletions();
        ArrayList<String> changes = git.getNewChangeSet();
        String destination = localPath.getPath() + "/rollback";

        ArrayList<SMAMetadata> results = SMAUtility.generate(destructiveChanges, changes,
                destination);

        assertEquals(expectedList.size(), results.size());

        for(int i = 0; i< expectedList.size(); i++){
            assertEquals(expectedList.get(i).getFullName(), results.get(i).getFullName());
        }
    }

    @Test
    public void generateManifestsTestRollback() throws Exception{
        //Setup the metadata xml document
        SMAManifestGenerator.SMAMetadataXMLDocument.initDocument();

        //Create our expected members list
        expectedList = new ArrayList<SMAMetadata>();
        expectedList.add(SMAManifestGenerator.SMAMetadataXMLDocument.createMetadataObject("src/pages/modifyThis.page"));

        git = new SMAGit(gitDir, newSha, oldSha);

        ArrayList<String> destructiveChanges = git.getAdditions();
        ArrayList<String> changes = git.getOldChangeSet();
        String destination = localPath.getPath() + "/rollback";

        ArrayList<SMAMetadata> results = SMAUtility.generate(destructiveChanges, changes,
                destination);

        assertEquals(expectedList.size(), results.size());

        for(int i = 0; i< expectedList.size(); i++){
            assertEquals(expectedList.get(i).getFullName(), results.get(i).getFullName());
        }
    }

    @Test
    public void zipRollbackPackageTest() throws Exception{
        git = new SMAGit(gitDir, newSha, oldSha);

        ArrayList<String> destructiveChanges = git.getAdditions();
        ArrayList<String> changes = git.getOldChangeSet();
        String destination = localPath + "/rollback";

        ArrayList<SMAMetadata> members = SMAUtility.generate(destructiveChanges, changes,
                destination);

        git.getPrevCommitFiles(members, destination);

        String jobName = "TestJob";
        String buildNumber = "TestBuildNumber";
        String buildTag = jobName + "-" + buildNumber;

        File dest = new File(destination);
        SMAUtility.zipRollbackPackage(dest, buildTag);

        File zipTest = new File(localPath.getPath() + "/" + buildTag + "-APMGrollback.zip");
        assertTrue(zipTest.exists());
    }

}
