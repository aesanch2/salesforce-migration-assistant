# Automated Package Manifest Generator

This Jenkins plugin generates a Salesforce package manifest file (``package.xml``) based on the differences
between two commits in Git, copies the added/modified files to a 'deployment stage' in the Jenkins' project workspace,
generates an appropriate ``destructiveChanges.xml`` file for deleted items, and allows ANT to deploy only these
changes to a Salesforce environment.

The plugin also has support for creating a "rollback package" that contains all the necessary metadata required for
removing the changes made in the build from the Salesforce environment. This rollback package is a zip file located in
the job's build directory for that build (e.g. ``/var/lib/jenkins/build/buildnumber`` on Linux).

The plugin also has support for keeping your remote repository's version of ``package.xml`` up to date with the latest
changes. See Project Configuration for more details.

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

### Required Jenkins Plugins:
* git plugin (https://wiki.jenkins-ci.org/display/JENKINS/Git+Plugin)

### Pre-installation:
* You should have some kind of Git repository that Jenkins has access to retrieve the supported Salesforce Metadata from.

### Installation:
* Install the plugin.
* There are no global configurations to be made with this plugin. All configuration is done on a per project basis.

### Project Configuration:
* Create a new job.
* Setup your Git SCM of choice.
* Add ``APMG`` as a Build step.
    * Check whether you want to let APMG generate rollback packages.
    * Check whether you want to let APMG update your repository's package manifest.
* Add ``Invoke Ant`` as a Build step.
    * You can specify anything you want for your ANT job, using the ANT migration tool from Salesforce.
    * You should point your deployRoot to ``(path)/apmg/src``.
* Add ``Git Publisher`` as a Post-build Action.
    * Check ``Push Only If Build Succeeds``.
    * You can setup any other configuration items in this action as you see fit.

### Changelog

#### -> 1.0
* Initial Release
* Generate manifest package file based on git-diffs
* Store the previous successfully deployed git commit in each job
* Generate a rollback package based on what was deployed/deleted/modified
* Create a deployment package in the job's workspace
* Create an updated version of package.xml if necessary

### Licensing

This software is licensed under the terms you may find in the file name "LICENSE.txt" in this directory.