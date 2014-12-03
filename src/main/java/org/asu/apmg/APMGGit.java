package org.asu.apmg;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for git interactions using jGit
 * @author aesanch2
 */
public class APMGGit {

    private Git git;
    private Repository repository;
    private ArrayList<String> additions, deletions, modificationsOld, modificationsNew, contents;
    private String prevCommit, curCommit;

    /**
     * Creates an APMGGit instance for the initial commit and/or initial build.
     * @param pathToRepo The path to the git repository.
     * @param curCommit The current commit.
     * @throws Exception
     */
    public APMGGit(String pathToRepo, String curCommit) throws Exception{
        File repoDir = new File(pathToRepo);
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder.setGitDir(repoDir).readEnvironment().build();
        git = new Git(repository);
        this.curCommit = curCommit;
    }

    /**
     * Creates an APMGGit instance for all other builds.
     * @param pathToRepo The path to the git repository.
     * @param prevCommit The previous commit.
     * @param curCommit The current commit.
     * @throws Exception
     */
    public APMGGit(String pathToRepo, String prevCommit, String curCommit) throws Exception{
        File repoDir = new File(pathToRepo);
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder.setGitDir(repoDir).readEnvironment().build();
        git = new Git(repository);
        this.prevCommit = prevCommit;
        this.curCommit = curCommit;
        determineChanges();
    }

    /**
     * Returns all of the items that were added in the current commit.
     * @return The ArrayList containing all of the additions in the current commit.
     * @throws IOException
     */
    public ArrayList<String> getAdditions() throws IOException{
        if (additions == null){
            additions = new ArrayList<String>();
        }
        return additions;
    }

    /**
     * Returns all of the items that were deleted in the current commit.
     * @return The ArrayList containing all of the items that were deleted in the current commit.
     */
    public ArrayList<String> getDeletions(){
        if (deletions == null){
            deletions = new ArrayList<String>();
        }
        return deletions;
    }

    /**
     * Returns all of the updated changes in the current commit.
     * @return The ArrayList containing the items that were modified (new paths) and added to the repository.
     * @throws IOException
     */
    public ArrayList<String> getNewChangeSet() throws IOException{
        ArrayList<String> newChangeSet = new ArrayList<String>();

        if (prevCommit == null){
            newChangeSet = getContents();
        }else{
            newChangeSet.addAll(additions);
            newChangeSet.addAll(modificationsNew);
        }

        return newChangeSet;
    }

    /**
     * Returns all of the deleted or modified (old paths) changes in the current commit.
     * @return ArrayList containing the items that were modified (old paths) and deleted from the repository.
     */
    public ArrayList<String> getOldChangeSet(){
        ArrayList<String> oldChangeSet = new ArrayList<String>();
        oldChangeSet.addAll(deletions);
        oldChangeSet.addAll(modificationsOld);

        return oldChangeSet;
    }

    /**
     * Replicates ls-tree for the current commit.
     * @return ArrayList containing the full path for all items in the repository.
     * @throws IOException
     */
    private ArrayList<String> getContents() throws IOException{
        contents = new ArrayList<String>();
        ObjectId commitId = repository.resolve(curCommit);
        RevWalk revWalk = new RevWalk(repository);
        RevCommit commit = revWalk.parseCommit(commitId);
        RevTree tree = commit.getTree();
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(false);

        while(treeWalk.next()){
            if(treeWalk.isSubtree()){
                treeWalk.enterSubtree();
            }
            else {
                String member = treeWalk.getPathString();
                contents.add(member);
            }
        }

        return contents;
    }

    /**
     * Parses the diff between previous commit and current commit and sorts the changes into
     * lists that correspond to the change made.
     * @throws Exception
     */
    private void determineChanges() throws Exception{
        deletions = new ArrayList<String>();
        additions = new ArrayList<String>();
        modificationsNew = new ArrayList<String>();
        modificationsOld = new ArrayList<String>();

        for (DiffEntry diff : getDiffs()){
            if (diff.getChangeType().toString().equals("DELETE")){
                deletions.add(diff.getOldPath());
            }else if (diff.getChangeType().toString().equals("ADD")){
                additions.add(diff.getNewPath());
            }else if (diff.getChangeType().toString().equals("MODIFY")){
                modificationsNew.add(diff.getNewPath());
                modificationsOld.add(diff.getOldPath());
            }
        }
    }

    /**
     * Returns the diff between two commits.
     * @return List that contains DiffEntry objects of the changes made between the previous and current commits.
     * @throws Exception
     */
    private List<DiffEntry> getDiffs() throws Exception{
        CanonicalTreeParser previousTree = getTree(prevCommit);
        CanonicalTreeParser newTree = getTree(curCommit);
        return git.diff().setOldTree(previousTree).setNewTree(newTree).call();
    }

    /**
     * Returns the Canonical Tree Parser  representation of a commit.
     * @param commit Commit in the repository.
     * @return CanonicalTreeParser representing the tree for the commit.
     * @throws IOException
     */
    private CanonicalTreeParser getTree(String commit) throws IOException{
        CanonicalTreeParser tree = new CanonicalTreeParser();
        ObjectReader reader = repository.newObjectReader();
        ObjectId head = repository.resolve(commit + "^{tree}");
        tree.reset(reader, head);
        return tree;
    }
}
