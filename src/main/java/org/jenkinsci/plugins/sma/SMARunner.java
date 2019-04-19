package org.jenkinsci.plugins.sma;

import hudson.EnvVars;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class that contains all of the configuration pertinent to the running job
 *
 */
public class SMARunner
{
    private static final Logger LOG = Logger.getLogger(SMARunner.class.getName());

    private Boolean deployAll = false;
    private String currentCommit;
    private String previousCommit;
    private String rollbackLocation;
    private SMAGit git;
    private List<SMAMetadata> deployMetadata = new ArrayList<SMAMetadata>();
    private List<SMAMetadata> deleteMetadata = new ArrayList<SMAMetadata>();
    private List<SMAMetadata> rollbackMetadata = new ArrayList<SMAMetadata>();
    private List<SMAMetadata> rollbackAdditions = new ArrayList<SMAMetadata>();

    /**
     * Wrapper for coordinating the configuration of the running job
     *
     * @param jobVariables
     * @param prTargetBranch
     * @throws Exception
     */
    public SMARunner(EnvVars jobVariables, String prTargetBranch) throws Exception
    {
        // Get envvars to initialize SMAGit
        Boolean shaOverride = false;
        currentCommit = jobVariables.get("GIT_COMMIT");
        String pathToWorkspace = jobVariables.get("WORKSPACE");
        String jobName = jobVariables.get("JOB_NAME");
        String buildNumber = jobVariables.get("BUILD_NUMBER");

        if (jobVariables.containsKey("GIT_PREVIOUS_SUCCESSFUL_COMMIT"))
        {
            previousCommit = jobVariables.get("GIT_PREVIOUS_SUCCESSFUL_COMMIT");
        }
        else
        {
            deployAll = true;
        }

        if (jobVariables.containsKey("SMA_DEPLOY_ALL_METADATA"))
        {
            deployAll = Boolean.valueOf(jobVariables.get("SMA_DEPLOY_ALL_METADATA"));
        }

        if (jobVariables.containsKey("SMA_PREVIOUS_COMMIT_OVERRIDE"))
        {
            if (!jobVariables.get("SMA_PREVIOUS_COMMIT_OVERRIDE").isEmpty())
            {
                shaOverride = true;
                previousCommit = jobVariables.get("SMA_PREVIOUS_COMMIT_OVERRIDE");
            }
        }

        // Configure using pull request logic
        if (!prTargetBranch.isEmpty() && !shaOverride)
        {
            deployAll = false;
            git = new SMAGit(pathToWorkspace, currentCommit, prTargetBranch, SMAGit.Mode.PRB);
        }
        // Configure for all the metadata
        else if (deployAll)
        {
            git = new SMAGit(pathToWorkspace, currentCommit, null, SMAGit.Mode.INI);
        }
        // Configure using the previous successful commit for this job
        else
        {
            git = new SMAGit(pathToWorkspace, currentCommit, previousCommit, SMAGit.Mode.STD);
        }

        rollbackLocation = pathToWorkspace + "/sma/rollback" + jobName + buildNumber + ".zip";
    }

    /**
     * Returns whether the current job is set to deploy all the metadata in the repository
     *
     * @return deployAll
     */
    public Boolean getDeployAll()
    {
        return deployAll;
    }

    /**
     * Returns the SMAMetadata that is going to be deployed in this job
     *
     * @return
     * @throws Exception
     */
    public List<SMAMetadata> getPackageMembers() throws Exception
    {
        if (deployAll)
        {
            deployMetadata = buildMetadataList(git.getAllMetadata());
        }
        else if (deployMetadata.isEmpty())
        {
            Map<String, byte[]> positiveChanges = git.getNewMetadata();
            positiveChanges.putAll(git.getUpdatedMetadata());

            deployMetadata = buildMetadataList(positiveChanges);
        }

        return deployMetadata;
    }

    /**
     * Returns the SMAMetadata that is going to be deleted in this job
     *
     * @return deleteMetadata
     * @throws Exception
     */
    public List<SMAMetadata> getDestructionMembers() throws Exception
    {
        if (deleteMetadata.isEmpty())
        {
            Map<String, byte[]> negativeChanges = git.getDeletedMetadata();

            deleteMetadata = buildMetadataList(negativeChanges);
        }

        return deleteMetadata;
    }

    public List<SMAMetadata> getRollbackMetadata() throws Exception
    {
        if (deleteMetadata.isEmpty())
        {
            getDestructionMembers();
        }

        rollbackMetadata = new ArrayList<SMAMetadata>();
        rollbackMetadata.addAll(deleteMetadata);
        rollbackMetadata.addAll(buildMetadataList(git.getOriginalMetadata()));

        return rollbackMetadata;
    }

    public List<SMAMetadata> getRollbackAdditions() throws Exception
    {
        rollbackAdditions = new ArrayList<SMAMetadata>();
        rollbackAdditions.addAll(buildMetadataList(git.getNewMetadata()));

        return rollbackAdditions;
    }

    /**
     * Returns a map with the file name mapped to the byte contents of the metadata
     *
     * @return deploymentData
     * @throws Exception
     */
    public Map<String, byte[]> getDeploymentData() throws Exception
    {
        if (deployMetadata.isEmpty())
        {
            getPackageMembers();
        }

        return getData(deployMetadata, currentCommit);
    }

    public Map<String, byte[]> getRollbackData() throws Exception
    {
        if (rollbackMetadata.isEmpty())
        {
            getRollbackMetadata();
        }

        return getData(rollbackMetadata, previousCommit);
    }

    /**
     * Helper method to find the byte[] contents of given metadata
     *
     * @param metadatas
     * @param commit
     * @return
     * @throws Exception
     */
    private Map<String, byte[]> getData(List<SMAMetadata> metadatas, String commit) throws Exception
    {
        Map<String, byte[]> data = new HashMap<String, byte[]>();

        for (SMAMetadata metadata : metadatas)
        {
            data.put(metadata.toString(), metadata.getBody());

            if (metadata.hasMetaxml())
            {
                String metaXml = metadata.toString() + "-meta.xml";
                String pathToXml = metadata.getPath() + metadata.getFullName() + "-meta.xml";
                data.put(metaXml, git.getBlob(pathToXml, commit));
            }
        }

        return data;
    }

    /**
     * Constructs a list of SMAMetadata objects from a Map of files and their byte[] contents
     *
     * @param repoItems
     * @return
     * @throws Exception
     */
    private List<SMAMetadata> buildMetadataList(Map<String, byte[]> repoItems) throws Exception
    {
        List<SMAMetadata> thisMetadata = new ArrayList<SMAMetadata>();

        for (String repoItem : repoItems.keySet())
        {
            SMAMetadata mdObject = SMAMetadataTypes.createMetadataObject(repoItem, repoItems.get(repoItem));
            if (mdObject.isValid())
            {
                thisMetadata.add(mdObject);
            }
        }

        return thisMetadata;
    }

    /**
     * Returns a String array of all the unit tests that should be run in this job
     *
     * @param testRegex
     * @return
     * @throws Exception
     */
    public String[] getSpecifiedTests(String testRegex) throws Exception
    {
        List<String> specifiedTestsList = new ArrayList<String>();
        List<String> deployApex = SMAMetadata.getApexClasses(deployMetadata);
        List<String> allApex = SMAMetadata.getApexClasses(
                buildMetadataList(
                        git.getAllMetadata()
                )
        );

        for (String md : deployApex)
        {
            if (md.matches(testRegex))
            {
                if (!specifiedTestsList.contains(md))
                {
                    specifiedTestsList.add(md);
                }
            }
            else
            {
                // Go find the test class we need to add
                String testClass = SMAUtility.searchForTestClass(allApex, md + testRegex);

                if (testClass.equals("noneFound"))
                {
                    testClass = SMAUtility.searchForTestClass(allApex, testRegex + md);

                    if (testClass.equals("noneFound"))
                    {
                        LOG.warning("No test class for " + md + " found");
                        continue;
                    }
                }

                if (!specifiedTestsList.contains(testClass))
                {
                    specifiedTestsList.add(testClass);
                }

            }
        }

        String[] specifiedTests = new String[specifiedTestsList.size()];

        specifiedTestsList.toArray(specifiedTests);

        return specifiedTests;
    }

    public String getRollbackLocation()
    {
        File rollbackLocationFile = new File(rollbackLocation);

        if (!rollbackLocationFile.getParentFile().exists())
        {
            rollbackLocationFile.getParentFile().mkdirs();
        }

        return rollbackLocation;
    }
}