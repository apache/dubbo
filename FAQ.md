### Where is dubbo-admin?

dubbo-admin has been moved from core repository to https://github.com/apache/incubator-dubbo-ops since 2.6.1

### Which version should I choose?

Currently, dubbo keeps 3 versions evolve in parallel:

* 2.7.x (master): requires Java 1.8, major feature branch.

* 2.6.x: requires Java 1.6, minor feature & bugfix branch, GA, production ready.

* 2.5.x: requires Java 1.6, maintenance branch, only accept security vulnerability and critical bugfix, expected to be EOL soon.

check [this](https://github.com/apache/incubator-dubbo/issues/1208) for detailed version management plan.

For contributors, please make sure all changes on the right branch, that is, most of the pull request should go to 2.7.x, and be backported to 2.6.x and 2.5.x if necessary. If the fix is specific to a branch, please make sure your pull request goes to the right branch.

For committers, make sure select the right label and target branch for every PR, and don't forget to back port the fix to lower version is necessary.

####  How to register ip correctly in docker?  

[Example question](https://github.com/alibaba/dubbo/issues/742)  

Dubbo supports specifying ip/port via system environment variables, examples can be found [here](https://github.com/dubbo/dubbo-samples/tree/master/dubbo-samples-docker).
