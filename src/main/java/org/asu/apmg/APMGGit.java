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
 * Created by aesanch2 on 10/30/14.
 * Wrapper for git interactions using jGit
 */
public class APMGGit {
    private Git git;
    private Repository repository;
    private List<DiffEntry> diffs;
    private ArrayList<String> listOfDestructions, listOfPackageContents, listOfUpdates;
    private String prevCommit, curCommit;

    //Constructor
    public APMGGit(String pathToRepo, String prevCommit, String curCommit) throws Exception{
        File repoDir = new File(pathToRepo);
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder.setGitDir(repoDir).readEnvironment().build();
        git = new Git(repository);
        this.prevCommit = prevCommit;
        this.curCommit = curCommit;
        getDiffs();
    }

    public APMGGit(String pathToRepo, String curCommit) throws Exception{
        File repoDir = new File(pathToRepo);
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder.setGitDir(repoDir).readEnvironment().build();
        git = new Git(repository);
        this.curCommit = curCommit;
    }

    public ArrayList<String> getListOfUpdates() throws IOException{
        listOfUpdates = new ArrayList<String>();
        if(diffs == null){
            listOfUpdates = getRepoContents();
            return listOfUpdates;
        }else{
            for (DiffEntry diff:diffs){
                if (diff.getChangeType().toString().equals("ADD") || diff.getChangeType().toString().equals("MODIFY")){
                    listOfUpdates.add(diff.getNewPath());
                }
            }
        }
        return listOfUpdates;
    }

    public ArrayList<String> getListOfDestructions(){
        listOfDestructions = new ArrayList<String>();
        if(diffs == null) {
            return listOfDestructions;
        }else{
            for (DiffEntry diff:diffs){
                if (diff.getChangeType().toString().equals("DELETE")){
                    listOfDestructions.add(diff.getOldPath());
                }
            }
        }
        return listOfDestructions;
    }

    private ArrayList<String> getRepoContents() throws IOException{
        listOfPackageContents = new ArrayList<String>();
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
                listOfPackageContents.add(member);
            }
        }

        return listOfPackageContents;
    }

    private void getDiffs() throws Exception{
        CanonicalTreeParser previousTree = getTree(prevCommit);
        CanonicalTreeParser newTree = getTree(curCommit);
        diffs = git.diff().setOldTree(previousTree).setNewTree(newTree).call();
    }

    private CanonicalTreeParser getTree(String commit) throws IOException{
        CanonicalTreeParser tree = new CanonicalTreeParser();
        ObjectReader reader = repository.newObjectReader();
        ObjectId head = repository.resolve(commit + "^{tree}");
        tree.reset(reader, head);
        return tree;
    }
}
