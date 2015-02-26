package org.asu.sma;

import java.util.ArrayList;

/**
 * Helper class for generating both build.xml and package.xml files.
 * @author aesanch2
 */
public class SMAPackage {
    private String destination;
    private String workspace;
    private ArrayList<String> contents;
    private boolean destructiveChange;
    //Project configuration variables
    private boolean validateOnly;
    private boolean generateUnitTests;
    private String username;
    private String password;
    private String server;
    //Global configuration variables
    private String pluginHome;
    private String runTestRegex;
    private String pollWait;
    private String maxPoll;

    /**
     * Constructor for manifest SMAPackage
     * @param destination
     * @param contents
     * @param destructiveChange
     */
    public SMAPackage(String destination, ArrayList<String> contents,
                      boolean destructiveChange){
        this.destination = destination;
        this.contents = contents;
        this.destructiveChange = destructiveChange;

        //Modify the location of the SMAPackage based on what xml file we're representing.
        if(destructiveChange){
            this.destination = destination + "/src/destructiveChanges.xml";
        } else {
            this.destination = destination + "/src/package.xml";
        }
    }

    /**
     * Constructor for build SMAPackage
     * @param destination
     * @param contents
     * @param jenkinsHome
     * @param runTestRegex
     * @param pollWait
     * @param generateUnitTests
     * @param validateOnly
     */
    public SMAPackage(String destination, ArrayList<String> contents, String jenkinsHome,
                      String runTestRegex, String pollWait, String maxPoll,
                      boolean generateUnitTests, boolean validateOnly,
                      String username, String password, String server){
        this.contents = contents;
        this.runTestRegex = runTestRegex;
        this.pollWait = pollWait;
        this.maxPoll = maxPoll;
        this.generateUnitTests = generateUnitTests;
        this.validateOnly = validateOnly;
        this.username = username;
        this.server = server;

        if (password.isEmpty()){
            this.password = "{$sf.password}";
        }else{
            this.password = password;
        }

        //SMA's plugin directory for locating the ant-salesforce.jar
        pluginHome = jenkinsHome + "/plugins/sma";

        //Modify the location of the SMAPackage for the build file
        this.destination = destination + "/build/build.xml";
        this.workspace = destination + "/src";
    }

    public String getDestination() { return destination; }

    public String getWorkspace() { return workspace; }

    public ArrayList<String> getContents() { return contents; }

    public boolean isDestructiveChange() { return destructiveChange; }

    public boolean isValidateOnly() { return validateOnly; }

    public boolean isGenerateUnitTests() { return generateUnitTests; }

    public String getPluginHome() { return pluginHome; }

    public String getRunTestRegex() { return runTestRegex; }

    public String getPollWait() { return pollWait; }

    public String getMaxPoll() { return maxPoll; }

    public String getUsername() { return username; }

    public String getServer() { return server; }

    public String getPassword() { return password; }

}
