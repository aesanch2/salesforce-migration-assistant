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
import java.util.ArrayList;

/**
 * @author aesanch2
 */
public class APMGBuilder extends Builder {

    private APMGGit git;
    private boolean rollbackEnabled, updatePackageEnabled, forceInitialBuild;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public APMGBuilder(Boolean rollbackEnabled,
                       Boolean updatePackageEnabled,
                       Boolean forceInitialBuild) {
        this.rollbackEnabled = rollbackEnabled;
        this.updatePackageEnabled = updatePackageEnabled;
        this.forceInitialBuild = forceInitialBuild;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        String newCommit;
        String prevCommit;
        String workspaceDirectory;
        String jobName;
        String buildTag;
        String jenkinsHome;
        ArrayList<String> listOfDestructions, listOfUpdates;
        ArrayList<APMGMetadataObject> members;
        EnvVars envVars;

        try{
            //Load our environment variables for the job
            envVars = build.getEnvironment(listener);

            newCommit = envVars.get("GIT_COMMIT");
            prevCommit = envVars.get("GIT_PREVIOUS_SUCCESSFUL_COMMIT");
            workspaceDirectory = envVars.get("WORKSPACE");
            jobName = envVars.get("JOB_NAME");
            buildTag = envVars.get("BUILD_TAG");
            jenkinsHome = envVars.get("JENKINS_HOME");

            //Create a deployment space for this job within the workspace
            File deployStage = new File(workspaceDirectory + "/apmg");
            if(deployStage.exists()){
                FileUtils.deleteDirectory(deployStage);
            }deployStage.mkdirs();

            //Put the deployment stage location into the environment as a variable
            build.getEnvironment(listener).put("APMG_DEPLOYSTG", deployStage.getPath());

            String pathToRepo = workspaceDirectory + "/.git";

            //This was the initial commit to the repo or the first build
            if (prevCommit == null || getForceInitialBuild()){
                prevCommit = null;
                git = new APMGGit(pathToRepo, prevCommit);
            }
            //If we have a previous successful commit from the git plugin
            else{
                git = new APMGGit(pathToRepo, newCommit, newCommit);
            }

            //Get our change sets
            listOfDestructions = git.getDeletions();
            listOfUpdates = git.getNewChangeSet();

            //Generate the manifests
            members = APMGUtility.generateManifests(listOfDestructions, listOfUpdates, deployStage.getPath());
            listener.getLogger().println("[APMG] - Created deployment package.");

            //Copy the files to the deployStage
            APMGUtility.replicateMembers(members, workspaceDirectory, deployStage.getPath());

            //Check for rollback
            if (getRollbackEnabled() && prevCommit != null){
                String rollbackDirectory = jenkinsHome + "/jobs/" + jobName;
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
                String zipFile = APMGUtility.zipRollbackPackage(rollbackDirectory, buildTag);
                build.getEnvironment(listener).put("APMG_ROLLBACK", zipFile);
                listener.getLogger().println("[APMG] - Created rollback package.");
            }

            //Check to see if we need to update the repository's package.xml file
            if(getUpdatePackageEnabled()){
                boolean updateRequired = git.updatePackageXML(workspaceDirectory + "/src/package.xml");
                if (updateRequired)
                    listener.getLogger().println("[APMG] - Updated repository package.xml file.");
            }

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

    public boolean getUpdatePackageEnabled() { return updatePackageEnabled; }

    public boolean getForceInitialBuild() { return forceInitialBuild; }
}

