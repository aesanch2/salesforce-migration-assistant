package org.asu.sma;

import java.util.ArrayList;

/**
 * Helper class for generating both build.xml and package.xml files.
 * @author aesanch2
 */
public class SMAPackage {
    private String destination;
    private ArrayList<String> memberList;
    private boolean destructiveChange;
    private boolean validateOnly;
    private boolean generateUnitTests;
    //Global configuration variables
    private String pluginHome;
    private String runTestRegex;
    private String pollWait;

    /**
     * Constructor for manifest SMAPackage
     * @param destination
     * @param memberList
     * @param destructiveChange
     * @param validateOnly
     */
    public SMAPackage(String destination, ArrayList<String> memberList,
                      boolean destructiveChange, boolean validateOnly){
        this.destination = destination;
        this.memberList = memberList;
        this.destructiveChange = destructiveChange;
        this.validateOnly = validateOnly;
    }

    /**
     * Constructor for build SMAPackage
     * @param destination
     * @param memberList
     * @param jenkinsHome
     * @param runTestRegex
     * @param pollWait
     * @param generateUnitTests
     */
    public SMAPackage(String destination, ArrayList<String> memberList, String jenkinsHome,
                      String runTestRegex, String pollWait, boolean generateUnitTests){
        this.destination = destination;
        this.memberList = memberList;
        this.runTestRegex = runTestRegex;
        this.pollWait = pollWait;
        this.generateUnitTests = generateUnitTests;

        //SMA's plugin directory for locating the ant-salesforce.jar
        pluginHome = jenkinsHome + "/plugins/sma";
    }

    public ArrayList<String> getMemberList() { return memberList; }

    public String getDestination() { return destination; }

    public boolean isDestructiveChange() { return destructiveChange; }

    public boolean isValidateOnly() { return validateOnly; }

    public boolean isGenerateUnitTests() { return generateUnitTests; }

    public String getPluginHome() { return pluginHome; }

    public String getRunTestRegex() { return runTestRegex; }

    public String getPollWait() { return pollWait; }
}
