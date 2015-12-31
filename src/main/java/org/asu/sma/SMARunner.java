package org.asu.sma;

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
 * @author aesanch2
 */
public class SMARunner
{

    private static final Logger LOG = Logger.getLogger(SMARunner.class.getName());

    private Boolean deployAll;
    private String currentCommit;
    private String previousSuccessfulCommit;
    private SMAGit git;
    private SMAPackage manifest;
    private File rollbackPath;
    private List<SMAMetadata> deployMetadata = new ArrayList<SMAMetadata>();
    private List<SMAMetadata> deleteMetadata = new ArrayList<SMAMetadata>();
    private List<SMAMetadata> rollbackMetadata = new ArrayList<SMAMetadata>();

    public SMARunner(EnvVars jobVariables) throws Exception
    {
        // Get envvars to initialize SMAGit
        deployAll = Boolean.valueOf(jobVariables.get("SMA_DEPLOY_ALL_METADATA"));
        previousSuccessfulCommit = jobVariables.get("GIT_PREVIOUS_SUCCESSFUL_COMMIT");
        currentCommit = jobVariables.get("GIT_COMMIT");
        String pathToWorkspace = jobVariables.get("WORKSPACE");
        String jenkinsHome = jobVariables.get("JENKINS_HOME");
        String jobName = jobVariables.get("JOB_NAME");
        String buildNumber = jobVariables.get("BUILD_NUMBER");
        String shaOverride = jobVariables.get("SMA_PREVIOUS_COMMIT_OVERRIDE");

        if (!shaOverride.isEmpty())
        {
            previousSuccessfulCommit = shaOverride;
        }

        if (deployAll || previousSuccessfulCommit == null)
        {
            deployAll = true;
            git = new SMAGit(pathToWorkspace, currentCommit);
        }
        else
        {
            git = new SMAGit(pathToWorkspace, currentCommit, previousSuccessfulCommit);
        }
    }

    public Boolean getDeployAll()
    {
        return deployAll;
    }

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

    public List<SMAMetadata> getDestructionMembers() throws Exception
    {
        if (deleteMetadata.isEmpty())
        {
            Map<String, byte[]> negativeChanges = git.getDeletedMetadata();

            deleteMetadata = buildMetadataList(negativeChanges);
        }

        return deleteMetadata;
    }

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
        if (deleteMetadata.isEmpty())
        {
            getDestructionMembers();
        }

        rollbackMetadata = new ArrayList<SMAMetadata>();
        rollbackMetadata.addAll(deleteMetadata);
        rollbackMetadata.addAll(buildMetadataList(git.getOriginalMetadata()));

        return getData(rollbackMetadata, previousSuccessfulCommit);
    }

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
}