
## Contributing to Dubbo
Dubbo is released under the non-restrictive Apache 2.0 licenses and follows a very standard Github development process, using Github tracker for issues and merging pull requests into master. Contributions of all form to this repository is acceptable, as long as it follows the prescribed community guidelines enumerated below.

### Sign the Contributor License Agreement
Before we accept a non-trivial patch or pull request (PRs), we will need you to sign the Contributor License Agreement. Signing the contributors' agreement does not grant anyone commits rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do. Active contributors may get invited to join the core team that will grant them privileges to merge existing PRs. 

### Contact

#### Mailing list

The mailing list is the recommended way of pursuing a discussion on almost anything related to Dubbo. Please refer to this [guide](https://github.com/apache/dubbo/wiki/Mailing-list-subscription-guide) for detailed documentation on how to subscribe.

- [dev@dubbo.apache.org](mailto:dev-subscribe@dubbo.apache.org): the developer mailing list where you can ask questions about an issue you may have encountered while working with Dubbo. 
- [commits@dubbo.apache.org](mailto:commits-subscribe@dubbo.apache.org): the commits update will get broadcasted on this mailing list. You can subscribe to it, should you be interested in following Dubbo's development.
- [notifications@dubbo.apache.org](mailto:notifications-subscribe@dubbo.apache.org): all the Github [issue](https://github.com/apache/dubbo/issues) updates and [pull request](https://github.com/apache/dubbo/pulls) updates will be sent to this mailing list.

### Reporting issue

Please follow the [template](https://github.com/apache/dubbo/issues/new?template=dubbo-issue-report-template.md) for reporting any issues.

### Code Conventions
Our code style is almost in line with the standard java conventions (Popular IDE's default setting satisfy this), with the following additional restricts:  
* If there are more than 120 characters in the current line, begin a new line.

* Make sure all new .java files to have a simple Javadoc class comment with at least a @date tag identifying birth, and preferably at least a paragraph on the intended purpose of the class.

* Add the ASF license header comment to all new .java files (copy from existing files in the project)

* Make sure no @author tag gets appended to the file you contribute to as the @author tag is incompatible with Apache. Rest assured, other ways, including CVS, will ensure transparency, fairness in recording your contributions. 

* Add some Javadocs and, if you change the namespace, some XSD doc elements.

* Sufficient unit-tests should accompany new feature development or non-trivial bug fixes. 

* If no-one else is using your branch, please rebase it against the current master (or another target branch in the main project).

* When writing a commit message, please follow the following conventions: should your commit address an open issue, please add Fixes #XXX at the end of the commit message (where XXX is the issue number).

### Contribution flow

A rough outline of an ideal contributors' workflow is as follows:

* Fork the current repository
* Create a topic branch from where to base the contribution. Mostly, it's the master branch.
* Make commits of logical units.
* Make sure the commit messages are in the proper format (see below).
* Push changes in a topic branch to your forked repository.
* Follow the checklist in the [pull request template](https://github.com/apache/dubbo/blob/master/PULL_REQUEST_TEMPLATE.md)
* Before sending out the pull request, please sync your forked repository with the remote repository to ensure that your PR is elegant, concise. Reference the guide below:
```
git remote add upstream git@github.com:apache/dubbo.git
git fetch upstream
git rebase upstream/master
git checkout -b your_awesome_patch
... add some work
git push origin your_awesome_patch
```
* Submit a pull request to apache/dubbo and wait for the reply.

Thanks for contributing!

### Code style

We provide a template file [dubbo_codestyle_for_idea.xml](https://github.com/apache/dubbo/tree/master/codestyle/dubbo_codestyle_for_idea.xml) for IntelliJ idea that you can import it to your workplace. 
If you use Eclipse, you can use the IntelliJ Idea template for manually configuring your file.

**NOTICE**

It's critical to set the dubbo_codestyle_for_idea.xml to avoid the failure of your Travis CI builds. Steps to configure the code styles are as follows:

1. Enter `Editor > Code Style`
2. To manage a code style scheme, in the Code Style page, select the desired scheme from the drop-down list, and click on ![manage profiles](codestyle/manage_profiles.png).
From the drop-down list, select `Import Scheme`, then choose the option `IntelliJ IDEA code style XML` to import the scheme. 
3. In the Scheme field, type the name of the new scheme and press ⏎ to save the changes.

