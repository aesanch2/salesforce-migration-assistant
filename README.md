# Salesforce Migration Assistant

This Jenkins plugin generates a Salesforce package manifest file (``package.xml``) based on the differences
between two commits in Git, copies the added/modified files to a 'deployment stage' in the Jenkins' project workspace,
generates an appropriate ``destructiveChanges.xml`` file for deleted items, and allows ANT to deploy only these
changes to a Salesforce environment.

The plugin also has support for creating a "rollback package" that contains all the necessary metadata required for
removing the changes made in the build from the Salesforce environment. This rollback package is a zip file located in
the job's build directory for that build (e.g. ``/var/lib/jenkins/build/buildnumber`` on Linux).

The plugin also has support for keeping your remote repository's version of ``package.xml`` up to date with the latest
changes. See Project Configuration for more details.

The plugin also has support for dynamically generating the ant build.xml file associated with the Force.com Migration
Tool. The plugin can also add runTest descriptors in build.xml to run the unit tests associated with the code in your
repository i.e. no managed package unit tests will be run in non-production environments. See Project Configuration for
more details.

This plugin supports v29.0 of the Salesforce Metadata API. The ant-salesforce.jar is included with this plugin.

### Supported Metadata
The following metadata types are supported in this release:

* CustomApplication
* AppMenu
* ApprovalProcess
* AssignmentRules
* AuthProvider
* CallCenter
* Community
* ApexClass
* CustomPermission
* ApexComponent
* ConnectedApp
* CustomApplicationComponent
* Dashboard
* DataCategoryGroups
* Document
* EmailTemplate
* EntitlementProcess
* EscalationRules
* FlexiPage
* Flow
* ExternalDataSource
* Group
* HomePageComponent
* HomePageLayout
* CustomLabels
* Layout
* Letterhead
* LiveChatAgentConfig
* LiveChatButton
* LiveChatDeployment
* Milestonetype
* Network
* CustomObject
* CustomObjectTranslation
* ApexPage
* PermissionSet
* Portal
* PostTemplate
* Profile
* Queue
* QuickAction
* RemoteSite
* Reports
* ReportType
* Role
* SamlSsoConfig
* Settings
* SharingSet
* CustomSite
* Skill
* Territory
* Translation
* ApexTrigger
* CustomTab
* Staticresource
* CustomPageWeblink
* Workflow

It should be noted that any problems that are traditionally encountered with the ANT migration tool and the migration of
certain metadata types (e.g. CustomObjects, Profiles, etc.) will not be solved with this tool. My suggestion would be to
plan the contents of your repository accordingly.

### Requirements
* This plugin was built on Linux, so it should work correctly with any popular distro. Windows hasn't been tested.
* Your SCM should be Git. This plugin will not work with any other SCMs.

### Required Jenkins Plugins
* git plugin (https://wiki.jenkins-ci.org/display/JENKINS/Git+Plugin)

### Pre-installation
* You should have some kind of Git repository that Jenkins has access to retrieve the supported Salesforce Metadata.

### Installation
* Install the plugin.
* There are some global configuration items for the build.xml portion of SMA.
    * Enter a Java regular expression to filter your repository for all your unmanaged package unit tests.
    * Enter the Poll Wait value.
    * Enter the Max Poll value.

### Project Configuration
* Create a new job.
* Setup your Git SCM of choice.
* Add ``Salesforce Migration Assistant`` as a Build step.
    * Check whether you want SMA to generate a package manifest file.
        * Check whether you want to let SMA generate rollback packages.
        * Check whether you want to let SMA update your repository's package manifest.
        * Check whether you want SMA to deploy the entire contents of your repository.
    * Check whether you want SMA to generate a build file.
        * Enter the username for the environment that you are deploying to.
        * Enter the password for the user entered above.
            * Note that the password is stored in plaintext in the generated build.xml file. If you do not want
            the password to be stored this way, you can specify the password in the traditional Ant properties manner.
            e.g. ``${sf.password}=xxxxxxx``
        * Select the Salesforce instance type.
            * Support for Sandbox and Production only
        * Check whether you want SMA validate this build only.
        * Check whether you want SMA to generate and run unit tests for code in your default namespace.
    * If you would like all deployments to build against a particular commit, you can specify that commit in the `SHA Override` 
    field.
* Add ``Invoke Ant`` as a Build step.
    * If you enable SMA to generate a build file, set the following properties:
        * Set ``sma`` as the Ant Target.
        * Set build file to ``$SMA_BUILD``.
        * If necessary, set the following ant properties:
            * ``sf.password='the API password for the user listed in Salesforce Migration Assistant build step'``
* Add ``Git Publisher`` as a Post-build Action.
    * Check ``Push Only If Build Succeeds``.
    * You can setup any other configuration items in this action as you see fit.
    
### Parameterized Builds
If you would like to manually override some of the aspects of builds, particularly on manual builds, you can set them via
specific parameters.
* `SMA_FORCE_INITIAL_BUILD`: A boolean parameter that forces manually triggered builds to deploy the entire repository contents.
* `SMA_SHA_OVERRIDE`: A String parameter that allows manually triggered builds to be generated against a particular commit
in your repository.

### Changelog

#### -> 1.1.2
* Add support for overriding the previous commit sha and to force an initial build.
* Add support for parameter-izing said overrides.
    * `SMA_FORCE_INITIAL_BUILD` -> Boolean
    * `SMA_SHA_OVERRIDE` -> String
* Bug fixes.

#### -> 1.1.1
* Add global configuration for max poll, poll wait, and test regex.
* Add help text
* Add username, password, and server url to dynamically generated build.xml

#### -> 1.1
* Rename plugin
* Generate an Ant build.xml file
* Run unit tests for default namespace code i.e. no managed package tests

#### -> 1.0
* Initial Release
* Generate manifest package file based on git-diffs
* Generate a rollback package based on what was deployed/deleted/modified
* Create a deployment package in the job's workspace
* Create an updated version of package.xml if necessary
* Allow user to force initial build/commit behavior for a job at will
* Support for Metadata API v29.0

### Licensing

This software is licensed under the terms you may find in the file name "LICENSE.txt" in this directory.