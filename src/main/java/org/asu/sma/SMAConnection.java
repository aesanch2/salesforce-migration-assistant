package org.asu.sma;

import com.sforce.soap.metadata.*;
import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectorConfig;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;

/**
 * @author aesanch2
 */
public class SMAConnection
{
    private final ConnectorConfig initConfig = new ConnectorConfig();
    private final ConnectorConfig metadataConfig = new ConnectorConfig();
    private final ConnectorConfig toolingConfig = new ConnectorConfig();

    private final MetadataConnection metadataConnection;
    private final PartnerConnection partnerConnection;

    private final String pollWaitString;
    private final String maxPollString;

    private DeployResult deployResult;
    private DeployDetails deployDetails;
    private double API_VERSION;

    public SMAConnection(String username,
                         String password,
                         String securityToken,
                         String server,
                         String pollWaitString,
                         String maxPollString) throws Exception
    {
        API_VERSION = Double.valueOf(SMAMetadataTypes.getAPIVersion());
        this.pollWaitString = pollWaitString;
        this.maxPollString = maxPollString;

        String endpoint = server + "/services/Soap/u/" + String.valueOf(API_VERSION);

        initConfig.setUsername(username);
        initConfig.setPassword(password + securityToken);
        initConfig.setAuthEndpoint(endpoint);
        initConfig.setServiceEndpoint(endpoint);
        initConfig.setManualLogin(true);

        partnerConnection = Connector.newConnection(initConfig);

        LoginResult loginResult = new LoginResult();

        loginResult = partnerConnection.login(initConfig.getUsername(), initConfig.getPassword());
        metadataConfig.setServiceEndpoint(loginResult.getMetadataServerUrl());
        metadataConfig.setSessionId(loginResult.getSessionId());

        metadataConnection = new MetadataConnection(metadataConfig);
    }

    public boolean deployToServer(ByteArrayOutputStream bytes,
                                  boolean validateOnly,
                                  TestLevel testLevel,
                                  String[] specifiedTests) throws Exception
    {
        DeployOptions deployOptions = new DeployOptions();
        deployOptions.setPerformRetrieve(false);
        deployOptions.setRollbackOnError(true);
        deployOptions.setSinglePackage(true);
        deployOptions.setCheckOnly(validateOnly);
        deployOptions.setTestLevel(testLevel);
        if (testLevel.equals(TestLevel.RunSpecifiedTests))
        {
            deployOptions.setRunTests(specifiedTests);
        }

        AsyncResult asyncResult = metadataConnection.deploy(bytes.toByteArray(), deployOptions);
        String asyncResultId = asyncResult.getId();

        int poll = 0;
        int maxPoll = Integer.valueOf(maxPollString);
        long pollWait = Long.valueOf(pollWaitString);
        boolean fetchDetails;
        do
        {
            Thread.sleep(pollWait);

            if (poll++ > maxPoll)
            {
                throw new Exception("[SMA] Request timed out. You can check the results later by using this AsyncResult Id: " + asyncResultId);
            }

            // Only fetch the details every three poll attempts
            fetchDetails = (poll % 3 == 0);
            deployResult = metadataConnection.checkDeployStatus(asyncResultId, fetchDetails);
        }
        while (!deployResult.isDone());

        // This is more to do with errors related to Salesforce. Actual deployment failures are not returned as error codes.
        if (!deployResult.isSuccess() && deployResult.getErrorStatusCode() != null)
        {
            throw new Exception(deployResult.getErrorStatusCode() + " msg:" + deployResult.getErrorMessage());
        }

        if (!fetchDetails)
        {
            // Get the final result with details if we didn't do it in the last attempt.
            deployResult = metadataConnection.checkDeployStatus(asyncResultId, true);
        }

        deployDetails = deployResult.getDetails();

        return deployResult.isSuccess();
    }

    public String getTestFailures()
    {
        RunTestsResult rtr = deployDetails.getRunTestResult();
        StringBuilder buf = new StringBuilder();
        if (rtr.getFailures() != null)
        {
            buf.append("[SMA] Test Failures\n");
            for (RunTestFailure failure : rtr.getFailures())
            {
                String n = (failure.getNamespace() == null ? "" :
                        (failure.getNamespace() + ".")) + failure.getName();
                buf.append("Test failure, method: " + n + "." +
                        failure.getMethodName() + " -- " +
                        failure.getMessage() + " stack " +
                        failure.getStackTrace() + "\n\n");
            }
        }

        return buf.toString();
    }

    public String getComponentFailures()
    {
        DeployMessage messages[] = deployDetails.getComponentFailures();
        StringBuilder buf = new StringBuilder();
        for (DeployMessage message : messages)
        {
            if (!message.isSuccess())
            {
                buf.append("[SMA] Component Failures\n");
                if (buf.length() == 0)
                {
                    buf = new StringBuilder("\nFailures:\n");
                }

                String loc = (message.getLineNumber() == 0 ? "" :
                        ("(" + message.getLineNumber() + "," +
                                message.getColumnNumber() + ")"));

                if (loc.length() == 0
                        && !message.getFileName().equals(message.getFullName()))
                {
                    loc = "(" + message.getFullName() + ")";
                }
                buf.append(message.getFileName() + loc + ":" +
                        message.getProblem()).append('\n');
            }
        }

        return buf.toString();
    }

    public String getCodeCoverage()
    {
        RunTestsResult rtr = deployDetails.getRunTestResult();
        StringBuilder buf = new StringBuilder();
        DecimalFormat df = new DecimalFormat("#.##");

        //Get the individual coverage results
        CodeCoverageResult[] ccresult = rtr.getCodeCoverage();
        if (ccresult.length > 0);
        {
            buf.append("[SMA] Code Coverage Results\n");

            double loc = 0;
            double locUncovered = 0;
            for (CodeCoverageResult ccr : ccresult)
            {
                buf.append(ccr.getName() + ".cls");
                buf.append(" -- ");
                loc = ccr.getNumLocations();
                locUncovered = ccr.getNumLocationsNotCovered();

                double coverage = 0;
                if (loc > 0)
                {
                    coverage = calculateCoverage(locUncovered, loc);
                }

                buf.append(df.format(coverage) + "%\n");
            }

            // Get the total code coverage for this deployment
            double totalCoverage = getTotalCodeCoverage(ccresult);
            buf.append("\nTotal code coverage for this deployment -- ");
            buf.append(df.format(totalCoverage) + "%\n");
        }

        return buf.toString();
    }

    public String getCodeCoverageWarnings()
    {
        RunTestsResult rtr = deployDetails.getRunTestResult();
        StringBuilder buf = new StringBuilder();
        CodeCoverageWarning[] ccwarn = rtr.getCodeCoverageWarnings();
        if (ccwarn.length > 0);
        {
            boolean generateHeader = true;
            for (CodeCoverageWarning ccw : ccwarn)
            {
                // Weird handling for warnings
                if (generateHeader)
                {
                    buf.append("[SMA] Code Coverage Warnings\n");
                    generateHeader = false;
                }

                buf.append("Code coverage issue");
                if (ccw.getName() != null)
                {
                    String n = (ccw.getNamespace() == null ? "" :
                            (ccw.getNamespace() + ".")) + ccw.getName();
                    buf.append(", class: " + n);
                }
                buf.append(" -- " + ccw.getMessage() + "\n");
            }
        }

        return buf.toString();
    }

    public DeployDetails getDeployDetails()
    {
        return deployDetails;
    }

    public void setDeployDetails(DeployDetails deployDetails)
    {
        this.deployDetails = deployDetails;
    }

    private Double getTotalCodeCoverage(CodeCoverageResult[] ccresult)
    {
        double totalLoc = 0;
        double totalLocUncovered = 0;

        if (ccresult.length > 0);
        {
            for (CodeCoverageResult ccr : ccresult)
            {
                totalLoc += ccr.getNumLocations();
                totalLocUncovered += ccr.getNumLocationsNotCovered();
            }
        }

        // Determine the coverage
        double coverage = 0;
        if (totalLoc > 0)
        {
            coverage = calculateCoverage(totalLocUncovered, totalLoc);
        }

        return coverage;
    }

    private double calculateCoverage(double totalLocUncovered, double totalLoc)
    {
        return (1 - (totalLocUncovered / totalLoc)) * 100;
    }
}