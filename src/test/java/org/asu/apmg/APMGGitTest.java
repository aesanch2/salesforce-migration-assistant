package org.asu.apmg;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class APMGGitTest {

    private Repository repository;
    private APMGGit git;
    private File addition, modification, deletion, localPath;
    private String oldSha, newSha, gitDir;

    /**
     * Before to setup the test.
     * @throws Exception
     */
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

    /**
     * After to tear down the test.
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception{
        repository.close();
        FileUtils.deleteDirectory(localPath);
    }

    /**
     * Test the diff capability of the wrapper.
     * @throws Exception
     */
    @Test
    public void testDiff() throws Exception {
        ArrayList<String> expectedDelete = new ArrayList<String>();
        expectedDelete.add("src/class/deleteThis.cls");

        ArrayList<String> expectedContents = new ArrayList<String>();
        expectedContents.add("src/triggers/addThis.trigger");
        expectedContents.add("src/pages/modifyThis.page");

        //Get the trees
        ObjectReader reader = repository.newObjectReader();
        CanonicalTreeParser oldTree = new CanonicalTreeParser();
        CanonicalTreeParser newTree = new CanonicalTreeParser();
        ObjectId oldHead = repository.resolve(oldSha + "^{tree}");
        ObjectId newHead = repository.resolve(newSha + "^{tree}");
        oldTree.reset(reader, oldHead);
        newTree.reset(reader, newHead);

        git = new APMGGit(gitDir, oldSha, newSha);

        ArrayList<String> deletedContents = git.getDeletions();
        ArrayList<String> newContents = git.getNewChangeSet();

        assertEquals(expectedDelete, deletedContents);
        assertEquals(expectedContents, newContents);
    }

    /**
     * Test the overloaded constructors.
     * @throws Exception
     */
    @Test
    public void testInitialCommit() throws Exception{
        ArrayList<String> expectedContents = new ArrayList<String>();
        expectedContents.add("src/pages/modifyThis.page");
        expectedContents.add("src/triggers/addThis.trigger");

        git = new APMGGit(gitDir, newSha);

        ArrayList<String> contents = git.getNewChangeSet();

        assertEquals(expectedContents, contents);
    }

    /**
     * Test the rollback capability.
     * @throws Exception
     */
    @Test
    public void testRollback() throws Exception{
        ArrayList<String> expectedContents = new ArrayList<String>();
        expectedContents.add("src/class/deleteThis.cls");
        expectedContents.add("src/pages/modifyThis.page");

        git = new APMGGit(gitDir, oldSha, newSha);

        ArrayList<String> contents = git.getOldChangeSet();

        assertEquals(expectedContents, contents);
    }

    /**
     * Test the retrieval of the previous commit's files.
     * @throws Exception
     */
    @Test
    public void testGetPrevCommitFiles() throws Exception{
        git = new APMGGit(gitDir, oldSha, newSha);

        String rollbackStage = localPath.getPath() + "/rollback";
    }
}