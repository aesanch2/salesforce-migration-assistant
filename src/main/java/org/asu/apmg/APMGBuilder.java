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

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public APMGBuilder() {  }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        String newCommit;
        String prevCommit;
        String workspaceDirectory;
        String jobName;
        String buildNumber;
        String buildURL;
        ArrayList<String> listOfDestructions;
        ArrayList<String> listOfUpdates;
        ArrayList<APMGMetadataObject> members;

        try{
            //Load our environment variables for the job
            EnvVars envVars = build.getEnvironment(listener);

            newCommit = envVars.get("GIT_COMMIT");
            workspaceDirectory = envVars.get("WORKSPACE");
            jobName = envVars.get("JOB_NAME");
            buildNumber = envVars.get("BUILD_NUMBER");
            buildURL = envVars.get("BUILD_URL");

            //Create a deployment space for this job within the workspace
            File deployStage = new File(workspaceDirectory + "/deployStage");
            if(deployStage.exists()){
                FileUtils.deleteDirectory(deployStage);
            }deployStage.mkdirs();

            //Set up our repository connection
            String pathToRepo = workspaceDirectory + "/.git";


            //Read this job's property file and setup the appropriate APMGGit wrapper
            String jobRoot = "/Users/anthony/Documents/IdeaProjects/apmg/work/jobs/";
            File lastSuccess = new File(jobRoot + jobName + "/lastSuccessful/apmgBuilder.properties");
            File newSuccess = new File(jobRoot + jobName + "/builds/" + buildNumber + "/apmgBuilder.properties");
            OutputStream output = new FileOutputStream(newSuccess);
            Properties jobProperties = new Properties();
            APMGGenerator generator = new APMGGenerator();
            if (lastSuccess.exists() && !lastSuccess.isDirectory()){
                jobProperties.load(new FileInputStream(lastSuccess));
                prevCommit = jobProperties.getProperty("LAST_SUCCESSFUL_COMMIT");

                //Interact with Git to get the changes made to the repository
                git = new APMGGit(pathToRepo, prevCommit, newCommit);
            }
            //This was the initial commit to the repo or the initial job
            else{
                //Interact with Git to get the changes made to the repository
                git = new APMGGit(pathToRepo, newCommit);
            }

            //Get our lists
            listOfDestructions = git.getListOfDestructions();
            listOfUpdates = git.getListOfUpdates();

            //Generate the manifests
            if (!listOfDestructions.isEmpty()) {
                //Generate the destructiveChanges.xml file
                String destructiveChanges = deployStage + "/src/destructiveChanges.xml";
                generator.setDestructiveChange(true);
                generator.generate(listOfDestructions, destructiveChanges);
                listener.getLogger().println("Created destructiveChanges.xml");
            }

            //Generate the package.xml file
            String packageManifest = deployStage + "/src/package.xml";
            generator.setDestructiveChange(false);
            members = generator.generate(listOfUpdates, packageManifest);
            listener.getLogger().println("Created package.xml");

            //Copy the files to the deployStage
            for(APMGMetadataObject file : members){
                File source = new File(workspaceDirectory + "/" + file.getPath() + file.getFullName());
                File destination = new File(deployStage + "/" + file.getPath());
                if(!destination.exists()){
                    destination.mkdirs();
                }
                FileUtils.copyFileToDirectory(source, destination);
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
}

