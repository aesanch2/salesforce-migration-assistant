package org.asu.sma;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author aesanch2
 */
public class SMABuilder extends Builder {

    private SMAGit git;
    private boolean rollbackEnabled, updatePackageEnabled,
            forceInitialBuild, runUnitTests, validateEnabled;
    private JSONObject generateManifests, generateAntEnabled;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public SMABuilder(JSONObject generateManifests,
                      JSONObject generateAntEnabled) {
        this.generateManifests = generateManifests;
        if(generateManifests != null) {
            rollbackEnabled = Boolean.valueOf(generateManifests.get("rollbackEnabled").toString());
            updatePackageEnabled = Boolean.valueOf(generateManifests.get("updatePackageEnabled").toString());
            forceInitialBuild = Boolean.valueOf(generateManifests.get("forceInitialBuild").toString());
        }

        this.generateAntEnabled = generateAntEnabled;
        if(generateAntEnabled != null) {
            validateEnabled = Boolean.valueOf(generateAntEnabled.get("validateEnabled").toString());
            runUnitTests = Boolean.valueOf(generateAntEnabled.get("runUnitTests").toString());
        }
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        String newCommit;
        String prevCommit;
        String jenkinsGitUserName;
        String jenkinsGitEmail;
        String workspaceDirectory;
        String jobName;
        String buildTag;
        String buildNumber;
        String jenkinsHome;
        ArrayList<String> listOfDestructions, listOfUpdates;
        ArrayList<SMAMetadata> members;
        EnvVars envVars;
        List<ParameterValue> parameterValues;

        try{
            //Load our environment variables for the job
            envVars = build.getEnvironment(listener);

            newCommit = envVars.get("GIT_COMMIT");
            prevCommit = envVars.get("GIT_PREVIOUS_SUCCESSFUL_COMMIT");
            jenkinsGitUserName = envVars.get("GIT_COMMITTER_NAME");
            jenkinsGitEmail = envVars.get("GIT_COMMITTER_EMAIL");
            workspaceDirectory = envVars.get("WORKSPACE");
            jobName = envVars.get("JOB_NAME");
            buildTag = envVars.get("BUILD_TAG");
            buildNumber = envVars.get("BUILD_NUMBER");
            jenkinsHome = envVars.get("JENKINS_HOME");

            //Create a deployment space for this job within the workspace
            File deployStage = new File(workspaceDirectory + "/sma");
            if(deployStage.exists()){
                FileUtils.deleteDirectory(deployStage);
            }deployStage.mkdirs();

            //Put the deployment stage location into the environment as a variable
            parameterValues = new ArrayList<ParameterValue>();
            parameterValues.add(new StringParameterValue("SMA_DEPLOY", deployStage.getPath()+"/src"));
            String pathToRepo = workspaceDirectory + "/.git";

            //This was the initial commit to the repo or the first build
            if (prevCommit == null || getForceInitialBuild()){
                prevCommit = null;
                git = new SMAGit(pathToRepo, newCommit);
            }
            //If we have a previous successful commit from the git plugin
            else{
                git = new SMAGit(pathToRepo, newCommit, prevCommit);
            }

            //Check to see if we need to generateManifest the manifest files
            if(getGenerateManifests()){
                //Get our change sets
                listOfDestructions = git.getDeletions();
                listOfUpdates = git.getNewChangeSet();

                //Generate the manifests
                members = SMAUtility.generate(listOfDestructions, listOfUpdates, deployStage.getPath());
                listener.getLogger().println("[SMA] - Created deployment package.");

                //Copy the files to the deployStage
                SMAUtility.replicateMembers(members, workspaceDirectory, deployStage.getPath());

                //Check for rollback
                if (getRollbackEnabled() && prevCommit != null){
                    String rollbackDirectory = jenkinsHome + "/jobs/" + jobName + "/builds/" + buildNumber + "/rollback";
                    File rollbackStage = new File(rollbackDirectory);
                    if(rollbackStage.exists()){
                        FileUtils.deleteDirectory(rollbackStage);
                    }rollbackStage.mkdirs();

                    //Get our lists
                    ArrayList<String> listOfOldItems = git.getOldChangeSet();
                    ArrayList<String> listOfAdditions = git.getAdditions();

                    //Generate the manifests for the rollback package
                    ArrayList<SMAMetadata> rollbackMembers =
                            SMAUtility.generate(listOfAdditions, listOfOldItems, rollbackDirectory);

                    //Copy the files to the rollbackStage and zip up the rollback stage
                    git.getPrevCommitFiles(rollbackMembers, rollbackDirectory);
                    String zipFile = SMAUtility.zipRollbackPackage(rollbackStage, buildTag);
                    FileUtils.deleteDirectory(rollbackStage);
                    parameterValues.add(new StringParameterValue("SMA_ROLLBACK", zipFile));
                    listener.getLogger().println("[SMA] - Created rollback package.");
                }

                //Check to see if we need to update the repository's package.xml file
                if(getUpdatePackageEnabled()){
                    boolean updateRequired = git.updatePackageXML(workspaceDirectory + "/src/package.xml",
                            jenkinsGitUserName, jenkinsGitEmail);
                    if (updateRequired)
                        listener.getLogger().println("[SMA] - Updated repository package.xml file.");
                }
            }

            //Check to see if we need to generateManifest the build file
            if(getGenerateAntEnabled()){
                String buildFile = SMAUtility.generate(deployStage.getPath(), getRunUnitTests(),
                        getValidateEnabled(), git.getContents(), jenkinsHome);
                listener.getLogger().println("[SMA] - Created build file.");
                parameterValues.add(new StringParameterValue("SMA_BUILD", buildFile));
            }

            build.addAction(new ParametersAction(parameterValues));
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
     * Descriptor for {@link SMABuilder}. Used as a singleton.
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
            return "Salesforce Migration Assistant";
        }
    }

    public boolean getGenerateManifests() {
        if(generateManifests == null){
            return false;
        }else{
            return true;
        }
    }

    public boolean getGenerateAntEnabled() {
        if(generateAntEnabled == null){
            return false;
        }else{
            return true;
        }
    }

    public boolean getRollbackEnabled() { return rollbackEnabled; }

    public boolean getUpdatePackageEnabled() { return updatePackageEnabled; }

    public boolean getForceInitialBuild() { return forceInitialBuild; }

    public boolean getRunUnitTests() { return runUnitTests; }

    public boolean getValidateEnabled() { return validateEnabled; }
}

