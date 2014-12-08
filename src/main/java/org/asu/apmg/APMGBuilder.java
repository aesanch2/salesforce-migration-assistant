package org.asu.apmg;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Properties;

/**
 * @author aesanch2
 */
public class APMGBuilder extends Builder {

    private APMGGit git;
    private boolean rollbackEnabled;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public APMGBuilder(Boolean rollbackEnabled) { this.rollbackEnabled = rollbackEnabled;  }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        String newCommit;
        String prevCommit;
        String workspaceDirectory;
        String jobName;
        String buildNumber;
        ArrayList<String> listOfDestructions, listOfUpdates;
        ArrayList<APMGMetadataObject> members;

        try{
            //Load our environment variables for the job
            EnvVars envVars = build.getEnvironment(listener);

            newCommit = envVars.get("GIT_COMMIT");
            workspaceDirectory = envVars.get("WORKSPACE");
            jobName = envVars.get("JOB_NAME");
            buildNumber = envVars.get("BUILD_NUMBER");

            //Create a deployment space for this job within the workspace
            File deployStage = new File(workspaceDirectory + "/deployStage");
            if(deployStage.exists()){
                FileUtils.deleteDirectory(deployStage);
            }deployStage.mkdirs();

            //Set up our repository connection
            String pathToRepo = workspaceDirectory + "/.git";


            //Read this job's property file and setup the appropriate APMGGit wrapper
            //TODO: Rename the job root when we are ready to release
            //String jobRoot = "/var/lib/jenkins/jobs/";
            String jobRoot = "/Users/anthony/Documents/IdeaProjects/apmg/work/jobs/";
            File lastSuccess = new File(jobRoot + jobName + "/lastSuccessful/apmgBuilder.properties");
            File newSuccess = new File(jobRoot + jobName + "/builds/" + buildNumber + "/apmgBuilder.properties");
            OutputStream output = new FileOutputStream(newSuccess);
            Properties jobProperties = new Properties();
            if (lastSuccess.exists() && !lastSuccess.isDirectory()){
                jobProperties.load(new FileInputStream(lastSuccess));
                prevCommit = jobProperties.getProperty("LAST_SUCCESSFUL_COMMIT");
                git = new APMGGit(pathToRepo, prevCommit, newCommit);
            }
            //This was the initial commit to the repo or the first build
            else{
                prevCommit = null;
                git = new APMGGit(pathToRepo, newCommit);
            }

            //Get our lists
            listOfDestructions = git.getDeletions();
            listOfUpdates = git.getNewChangeSet();

            //Generate the manifests
            members = APMGUtility.generateManifests(listOfDestructions, listOfUpdates, deployStage.getPath());

            //Copy the files to the deployStage
            APMGUtility.replicateMembers(members, workspaceDirectory, deployStage.getPath());

            //Check for rollback
            if (getRollbackEnabled() && prevCommit != null){
                String rollbackDirectory = newSuccess.getParent() + "/rollback";
                File rollbackStage = new File(rollbackDirectory);
                if(rollbackStage.exists()){
                    FileUtils.deleteDirectory(rollbackStage);
                }rollbackStage.mkdirs();

                //Get our lists
                ArrayList<String> listOfOldItems = git.getOldChangeSet();
                ArrayList<String> listOfAdditions = git.getAdditions();

                //Generate the manifests for the rollback package
                ArrayList<APMGMetadataObject> rollbackMembers =
                        APMGUtility.generateManifests(listOfAdditions, listOfOldItems, rollbackDirectory);

                //Copy the files to the rollbackStage and zip up the rollback stage
                git.getPrevCommitFiles(rollbackMembers, rollbackDirectory);
                APMGUtility.zipRollbackPackage(rollbackDirectory, jobName, buildNumber);
            }

            //Store the commit
            jobProperties.setProperty("LAST_SUCCESSFUL_COMMIT", newCommit);
            jobProperties.store(output, null);
        }catch(Exception e){
            e.printStackTrace(listener.getLogger());
            return false;
        }

        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link APMGBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "APMG";
        }
    }

    public boolean getRollbackEnabled() { return rollbackEnabled;}
}

