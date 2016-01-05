package org.asu.sma;

import com.sforce.soap.metadata.DeployResult;
import com.sforce.soap.metadata.TestLevel;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author aesanch2
 */
public class SMABuilder extends Builder
{
    private boolean validateEnabled;
    private String username;
    private String password;
    private String securityToken;
    private String serverType;
    private String testLevel;

    @DataBoundConstructor
    public SMABuilder(Boolean validateEnabled,
                      String username,
                      String password,
                      String securityToken,
                      String serverType,
                      String testLevel)
    {
        this.username = username;
        this.password = password;
        this.securityToken = securityToken;
        this.serverType = serverType;
        this.validateEnabled = validateEnabled;
        this.testLevel = testLevel;
    }

    @Override
    public boolean perform(AbstractBuild build,
                           Launcher launcher,
                           BuildListener listener)
    {
        List<SMAMetadata> deployMetadata;
        List<SMAMetadata> deleteMetadata;
        DeployResult deploymentResult;
        boolean JOB_SUCCESS = false;

        PrintStream writeToConsole = listener.getLogger();

        try
        {
            // Initialize the runner for this job
            SMARunner currentJob = new SMARunner(build.getEnvironment(listener));

            // Build the package and destructiveChanges manifests
            SMAPackage packageXml = new SMAPackage(currentJob.getPackageMembers(), false);
            writeToConsole.println("[SMA] Deploying the following metadata:");
            SMAUtility.printMetadataToConsole(listener, currentJob.getPackageMembers());
            SMAPackage destructiveChanges;

            if (currentJob.getDeployAll() || currentJob.getDestructionMembers().isEmpty())
            {
                destructiveChanges = new SMAPackage(new ArrayList<SMAMetadata>(), true);
            }
            else
            {
                destructiveChanges = new SMAPackage(currentJob.getDestructionMembers(), true);
                writeToConsole.println("[SMA] Deleting the following metadata:");
                SMAUtility.printMetadataToConsole(listener, currentJob.getDestructionMembers());
            }

            // Build the zipped deployment package
            ByteArrayOutputStream deploymentPackage = SMAUtility.zipPackage(
                    currentJob.getDeploymentData(),
                    packageXml,
                    destructiveChanges
            );

            // Initialize the connection to Salesforce for this job
            SMAConnection sfConnection = new SMAConnection(
                    getUsername(),
                    getPassword(),
                    getSecurityToken(),
                    getServerType(),
                    getDescriptor().getPollWait(),
                    getDescriptor().getMaxPoll()
            );

            // Deploy to the server
            String[] specifiedTests = null;
            if (TestLevel.valueOf(getTestLevel()).equals(TestLevel.RunSpecifiedTests))
            {
                specifiedTests = currentJob.getSpecifiedTests(getDescriptor().getRunTestRegex());
            }
            JOB_SUCCESS = sfConnection.deployToServer(
                    deploymentPackage,
                    getValidateEnabled(),
                    TestLevel.valueOf(
                            getTestLevel()
                    ),
                    specifiedTests
            );

            if (JOB_SUCCESS)
            {
                writeToConsole.println(sfConnection.getCodeCoverageWarnings());
                writeToConsole.println(sfConnection.getCodeCoverage());
                writeToConsole.println("[SMA] Deployment Succeeded");

                SMAPackage rollbackPackageXml = new SMAPackage(
                        currentJob.getRollbackMetadata(),
                        false
                );

                SMAPackage rollbackDestructiveChange = new SMAPackage(
                        currentJob.getRollbackAdditions(),
                        true
                );

                ByteArrayOutputStream rollbackPackage = SMAUtility.zipPackage(
                        currentJob.getRollbackData(),
                        rollbackPackageXml,
                        rollbackDestructiveChange
                );

                SMAUtility.writeZip(rollbackPackage, currentJob.getRollbackLocation());
            }
            else
            {
                writeToConsole.println(sfConnection.getComponentFailures());
                writeToConsole.println(sfConnection.getTestFailures());
                writeToConsole.println(sfConnection.getCodeCoverageWarnings());
                writeToConsole.println(sfConnection.getCodeCoverage());
                writeToConsole.println("[SMA] Deployment Failed");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(writeToConsole);
        }

        return JOB_SUCCESS;
    }

    public boolean getValidateEnabled()
    {
        return validateEnabled;
    }

    public String getUsername()
    {
        return username;
    }

    public String getSecurityToken()
    {
        return securityToken;
    }

    public String getPassword()
    {
        return password;
    }

    public String getServerType()
    {
        return serverType;
    }

    public String getTestLevel()
    {
        return testLevel;
    }

    @Override
    public DescriptorImpl getDescriptor()
    {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder>
    {

        private String maxPoll = "20";
        private String pollWait = "30000";
        private String runTestRegex = ".*[T|t]est.*";

        public DescriptorImpl()
        {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass)
        {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        public String getDisplayName()
        {
            return "Salesforce Migration Assistant";
        }

        public String getMaxPoll()
        {
            return maxPoll;
        }

        public String getPollWait()
        {
            return pollWait;
        }

        public String getRunTestRegex()
        {
            return runTestRegex;
        }

        public ListBoxModel doFillServerTypeItems()
        {
            return new ListBoxModel(
                    new ListBoxModel.Option("Production (https://login.salesforce.com)", "https://login.salesforce.com"),
                    new ListBoxModel.Option("Sandbox (https://test.salesforce.com)", "https://test.salesforce.com")
            );
        }

        public ListBoxModel doFillTestLevelItems()
        {
            return new ListBoxModel(
                    new ListBoxModel.Option("None", "NoTestRun"),
                    new ListBoxModel.Option("Relevant", "RunSpecifiedTests"),
                    new ListBoxModel.Option("Local", "RunLocalTests"),
                    new ListBoxModel.Option("All", "RunAllTestsInOrg")
            );
        }

        public boolean configure(StaplerRequest request, JSONObject formData) throws FormException
        {
            maxPoll = formData.getString("maxPoll");
            pollWait = formData.getString("pollWait");

            save();
            return false;
        }
    }
}

