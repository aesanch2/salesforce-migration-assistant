package org.asu.apmg;

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
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class APMGGitTest {

    private Repository repository;
    private APMGGit git;
    private File addition, modification, deletion;
    private String oldSha, newSha, gitDir;

    @Before
    public void setUp() throws Exception{
        //Setup the fake repository
        File localPath = File.createTempFile("TestGitRepository", "");
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
        deletion = new File(classesPath, "deleteThis");
        modification = new File(pagesPath, "modifyThis");
        deletion.createNewFile();
        modification.createNewFile();
        new Git(repository).add().addFilepattern("src/class/deleteThis").call();
        new Git(repository).add().addFilepattern("src/pages/modifyThis").call();

        //Create the first commit
        RevCommit firstCommit = new Git(repository).commit().setMessage("Add deleteThis and modifyThis").call();
        oldSha = firstCommit.getName();


        //Delete the deletion file, modify the modification file, and add the addition file
        new Git(repository).rm().addFilepattern("src/class/deleteThis").call();
        modification.setExecutable(true);
        addition = new File(triggersPath, "addThis");
        addition.createNewFile();
        new Git(repository).add().addFilepattern("src/pages/modifyThis").call();
        new Git(repository).add().addFilepattern("src/triggers/addThis").call();
        new Git(repository).add().addFilepattern("src/class/deleteThis").call();

        //Create the second commit
        RevCommit secondCommit = new Git(repository).commit().setMessage("Remove deleteThis. Modify " +
                "modifyThis. Add addThis").call();
        newSha = secondCommit.getName();

        gitDir = localPath.getPath() + "/.git";
    }

    @After
    public void tearDown() throws Exception{
        repository.close();
    }

    @Test
    public void testDiff() throws Exception {
        ArrayList<String> expectedDelete = new ArrayList<String>();
        expectedDelete.add("src/class/deleteThis");

        ArrayList<String> expectedContents = new ArrayList<String>();
        expectedContents.add("src/pages/modifyThis");
        expectedContents.add("src/triggers/addThis");

        //Get the trees
        ObjectReader reader = repository.newObjectReader();
        CanonicalTreeParser oldTree = new CanonicalTreeParser();
        CanonicalTreeParser newTree = new CanonicalTreeParser();
        ObjectId oldHead = repository.resolve(oldSha + "^{tree}");
        ObjectId newHead = repository.resolve(newSha + "^{tree}");
        oldTree.reset(reader, oldHead);
        newTree.reset(reader, newHead);

        git = new APMGGit(gitDir, oldSha, newSha);

        ArrayList<String> deletedContents = git.getListOfDestructions();
        ArrayList<String> newContents = git.getListOfUpdates();

        assertEquals(expectedDelete, deletedContents);
        assertEquals(expectedContents, newContents);
    }

    @Test
    public void testListPackageContents() throws Exception{
        ArrayList<String> expectedContents = new ArrayList<String>();
        expectedContents.add("src/pages/modifyThis");
        expectedContents.add("src/triggers/addThis");

        git = new APMGGit(gitDir, newSha);

        ArrayList<String> contents = git.getListOfUpdates();

        assertEquals(expectedContents, contents);
    }
}