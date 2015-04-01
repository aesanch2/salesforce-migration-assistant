package org.asu.sma;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author aesanch2
 */
public class SMABuilder extends Builder {

    private SMAGit git;
    private boolean rollbackEnabled;
    private boolean updatePackageEnabled;
    private boolean forceInitialBuild;
    private boolean runUnitTests;
    private boolean validateEnabled;
    private String sfUsername;
    private String sfPassword;
    private String sfServer;
    private JSONObject generateManifests;
    private JSONObject generateAntEnabled;


    @DataBoundConstructor
    public SMABuilder(JSONObject generateManifests, JSONObject generateAntEnabled) {
        this.generateManifests = generateManifests;
        if(generateManifests != null) {
            rollbackEnabled = Boolean.valueOf(generateManifests.get("rollbackEnabled").toString());
            updatePackageEnabled = Boolean.valueOf(generateManifests.get("updatePackageEnabled").toString());
            forceInitialBuild = Boolean.valueOf(generateManifests.get("forceInitialBuild").toString());
        }

        this.generateAntEnabled = generateAntEnabled;
        if(generateAntEnabled != null) {
            sfUsername = generateAntEnabled.get("sfUsername").toString();
            sfPassword = generateAntEnabled.get("sfPassword").toString();
            sfServer = generateAntEnabled.get("sfServer").toString();
            validateEnabled = Boolean.valueOf(generateAntEnabled.get("validateEnabled").toString());
            runUnitTests = Boolean.valueOf(generateAntEnabled.get("runUnitTests").toString());
        }
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        Boolean apexChangePresent;
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

        apexChangePresent = true;

        try {
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
            if (deployStage.exists()) {
                FileUtils.deleteDirectory(deployStage);
            }
            deployStage.mkdirs();

            //Put the deployment stage location into the environment as a variable
            parameterValues = new ArrayList<ParameterValue>();
            parameterValues.add(new StringParameterValue("SMA_DEPLOY", deployStage.getPath() + "/src"));
            String pathToRepo = workspaceDirectory + "/.git";

            //This was the initial commit to the repo, a manual job trigger, or the first build, deploy the entire repo
            if (getForceInitialBuild() || prevCommit == null || newCommit.equals(prevCommit)) {
                prevCommit = null;
                git = new SMAGit(pathToRepo, newCommit);
            }
            //If we have a previous successful commit from the git plugin
            else {
                git = new SMAGit(pathToRepo, newCommit, prevCommit);
            }

            //Check to see if we need to generateManifest the manifest files
            if (getGenerateManifests()) {
                //Get our change sets
                listOfDestructions = git.getDeletions();
                listOfUpdates = git.getNewChangeSet();

                //Generate the manifests
                members = SMAUtility.generate(listOfDestructions, listOfUpdates, deployStage.getPath());

                apexChangePresent = SMAUtility.apexChangesPresent(members);

                listener.getLogger().println("[SMA] - Created deployment package.");

                //Copy the files to the deployStage
                SMAUtility.replicateMembers(members, workspaceDirectory, deployStage.getPath());

                //Check for rollback
                if (getRollbackEnabled() && prevCommit != null) {
                    String rollbackDirectory = jenkinsHome + "/jobs/" + jobName + "/builds/" + buildNumber + "/sma/rollback";
                    File rollbackStage = new File(rollbackDirectory);
                    if (rollbackStage.exists()) {
                        FileUtils.deleteDirectory(rollbackStage);
                    }
                    rollbackStage.mkdirs();

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
                if (getUpdatePackageEnabled()) {
                    boolean updateRequired = git.updatePackageXML(workspaceDirectory,
                            jenkinsGitUserName, jenkinsGitEmail);
                    if (updateRequired)
                        listener.getLogger().println("[SMA] - Updated repository package.xml file.");
                }
            }

            //Check to see if we need to generateManifest the build file
            if (getGenerateAntEnabled()) {
                SMAPackage buildPackage = new SMAPackage(deployStage.getPath(), git.getContents(),
                        jenkinsHome, getDescriptor().getRunTestRegex(), getDescriptor().getPollWait(),
                        getDescriptor().getMaxPoll(), getRunUnitTests(), getValidateEnabled(),
                        getSfUsername(), getSfPassword(), getSfServer());
                String buildFile = SMABuildGenerator.generateBuildFile(buildPackage, apexChangePresent);
                listener.getLogger().println("[SMA] - Created build file.");
                parameterValues.add(new StringParameterValue("SMA_BUILD", buildFile));
            }

            build.addAction(new ParametersAction(parameterValues));
        } catch (Exception e) {
            e.printStackTrace(listener.getLogger());
            return false;
        }

        return true;
    }

    public boolean getGenerateManifests() {
        if(generateManifests == null)
            return false;
        else
            return true;
    }

    public boolean getGenerateAntEnabled() {
        if(generateAntEnabled == null)
            return false;
        else
            return true;
    }

    public boolean getRollbackEnabled() { return rollbackEnabled; }

    public boolean getUpdatePackageEnabled() { return updatePackageEnabled; }

    public boolean getForceInitialBuild() { return forceInitialBuild; }

    public boolean getRunUnitTests() { return runUnitTests; }

    public boolean getValidateEnabled() { return validateEnabled; }

    public String getSfUsername() { return sfUsername; }

    public String getSfServer() { return sfServer; }

    public String getSfPassword() { return sfPassword; }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private String runTestRegex = ".*[T|t]est.*";
        private String maxPoll = "20";
        private String pollWait = "30000";

        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        public String getDisplayName() {
            return "Salesforce Migration Assistant";
        }

        public String getRunTestRegex() {
            return runTestRegex;
        }

        public String getMaxPoll() {
            return maxPoll;
        }

        public String getPollWait() {
            return pollWait;
        }

        public ListBoxModel doFillSfServerItems(){
            return new ListBoxModel(
                    new ListBoxModel.Option("Production (https://login.salesforce.com)", "https://login.salesforce.com"),
                    new ListBoxModel.Option("Sandbox (https://test.salesforce.com)", "https://test.salesforce.com")
            );
        }

        public boolean configure(StaplerRequest request, JSONObject formData) throws FormException {
            runTestRegex = formData.getString("runTestRegex");
            maxPoll = formData.getString("maxPoll");
            pollWait = formData.getString("pollWait");

            save();
            return false;
        }
    }
}

