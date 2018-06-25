
## Contributing to dubbo
Dubbo is released under the non-restrictive Apache 2.0 license, and follows a very standard Github development process, using Github tracker for issues and merging pull requests into master. If you want to contribute even something trivial please do not hesitate, but follow the guidelines below.
### Sign the Contributor License Agreement
Before we accept a non-trivial patch or pull request we will need you to sign the Contributor License Agreement. Signing the contributor’s agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do. Active contributors might be asked to join the core team, and given the ability to merge pull requests.
### Code Conventions
Our code style is almost in line with the standard java conventions(Popular IDE's default setting satisfy this), only changed the following two restricts:  
1. Classes under 'org.apache.dubbo.*' and 'com.taobao.*' package are grouped separately, and put on top of all other 'imports'.  
2. If there are more than 120 characters in current line, start a new line.

We provide a template file [dubbo_codestyle_for_idea.xml](https://github.com/apache/incubator-dubbo/tree/master/codestyle/dubbo_codestyle_for_idea.xml) for IntelliJ idea, you can import it to you IDE. If you use Eclipse you can config manually by referencing the same file.  

* Make sure all new .java files to have a simple Javadoc class comment with at least a @date tag identifying birth, and preferably at least a paragraph on what the class is for.

* Add the ASF license header comment to all new .java files (copy from existing files in the project)

* Make sure no @author tag added to the file you contribute since @author tag is not used at Apache, other ways such as cvs will record all your contributions fairly.

* Add some Javadocs and, if you change the namespace, some XSD doc elements.

* A few unit tests should be added for a new feature or an important bugfix.

* If no-one else is using your branch, please rebase it against the current master (or other target branch in the main project).

* When writing a commit message please follow these conventions, if you are fixing an existing issue please add Fixes #XXX at the end of the commit message (where XXX is the issue number).
