# Apache Dubbo Project

[![Build and Test For PR](https://github.com/apache/dubbo/actions/workflows/build-and-test-pr.yml/badge.svg)](https://github.com/apache/dubbo/actions/workflows/build-and-test-pr.yml)
[![Codecov](https://codecov.io/gh/apache/dubbo/branch/3.3/graph/badge.svg)](https://codecov.io/gh/apache/dubbo)
![Maven](https://img.shields.io/maven-central/v/org.apache.dubbo/dubbo.svg)
![License](https://img.shields.io/github/license/alibaba/dubbo.svg)
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/apache/dubbo.svg)](http://isitmaintained.com/project/apache/dubbo "Average time to resolve an issue")
[![Percentage of issues still open](http://isitmaintained.com/badge/open/apache/dubbo.svg)](http://isitmaintained.com/project/apache/dubbo "Percentage of issues still open")

Apache Dubbo is an easy-to-use Web and RPC framework that provides multiple
language implementations(Java, [Go](https://github.com/apache/dubbo-go), [Python](https://github.com/dubbo/py-client-for-apache-dubbo), [PHP](https://github.com/apache/dubbo-php-framework), [Erlang](https://github.com/apache/dubbo-erlang), [Rust](https://github.com/apache/dubbo-rust), [Node.js](https://github.com/apache/dubbo-js), [Web](https://github.com/apache/dubbo-js)) for communication, service discovery, traffic management,
observability, security, tools, and best practices for building enterprise-ready microservices.

We are now collecting Dubbo user info to help us to improve Dubbo further. Kindly support us by providing your usage information on [Wanted: who's using dubbo](https://github.com/apache/dubbo/discussions/13842), thanks :)

## Architecture
![Architecture](https://dubbo.apache.org/imgs/architecture.png)

* Consumer and provider communicate with each other using RPC protocol like triple, tcp, rest, etc.
* Consumers automatically trace provider instances registered in registries(Zookeeper, Nacos) and distribute traffic among them by following traffic strategies.
* Rich features for monitoring and managing the cluster with dynamic configuration, metrics, tracing, security, and visualized console.

## Getting started
Follow the instructions below to learn how to:

### Programming with lightweight RPC API
[5 minutes step-by-step guide](https://cn.dubbo.apache.org/zh-cn/overview/mannual/java-sdk/tasks/framework/lightweight-rpc/)

Dubbo supports building RPC services with only a few lines of code while depending only on a lightweight SDK. The protocol on the wire can be [Triple](https://dubbo.apache.org/zh-cn/overview/reference/protocols/triple/)(fully gRPC compatible and HTTP-friendly), Dubbo2(TCP), REST, or any protocol of your choice.


### Building a microservice application with Spring Boot
[5 minutes step-by-step guide](https://cn.dubbo.apache.org/zh-cn/overview/mannual/java-sdk/tasks/develop/springboot/)

It's highly recommended to start your microservice application with the Spring Boot Starter `dubbo-spring-boot-starter` provided by Dubbo. With only a single dependency and yaml file, and optionally a bunch of other useful spring boot starters, you can enable all of the Dubo features like service discovery, observability, tracing, etc.

Next, learn how to [deploy](https://dubbo.apache.org/zh-cn/overview/tasks/deploy/), [monitor](https://dubbo.apache.org/zh-cn/overview/tasks/observability/), and [manage the traffic](https://dubbo.apache.org/zh-cn/overview/tasks/traffic-management/) of your Dubbo application and cluster.

## More Features
Get more details by visiting the links below to get your hands dirty with some well-designed tasks on our website.

* [Launch a Dubbo project](https://dubbo.apache.org/zh-cn/overview/tasks/develop/template/)
* [RPC protocols](https://dubbo.apache.org/zh-cn/overview/core-features/protocols/)
* [Traffic management](https://dubbo.apache.org/zh-cn/overview/core-features/traffic/)
* [Service discovery](https://dubbo.apache.org/zh-cn/overview/core-features/service-discovery/)
* [Observability](https://dubbo.apache.org/zh-cn/overview/core-features/observability/)
* [Extensibility](https://dubbo.apache.org/zh-cn/overview/core-features/extensibility/)
* [Security](https://dubbo.apache.org/zh-cn/overview/core-features/security/)
* [Visualized console and control plane](https://dubbo.apache.org/zh-cn/overview/reference/admin/)
* [Kubernetes and Service mesh](https://dubbo.apache.org/zh-cn/overview/core-features/service-mesh/)

## Which Dubbo version should I use?
| **Dubbo3** | **JDK**  | **Dependencies**                                                                                        | **Description**                                                                                                                                                                                                            |
|------------|----------|---------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 3.3.2      | 1.8 ～ 21 | [dependency list](https://github.com/apache/dubbo/blob/dubbo-3.3.2/dubbo-dependencies-bom/pom.xml#L92)  | **- Stable version (active)** <br/> **- Features** <br/> &nbsp;&nbsp;  - Triple - gRPC and cURL compatible.<br/>  &nbsp;&nbsp;  - Rest-style programming support.<br/>  &nbsp;&nbsp;  - Spring Boot Starters.              |
| 3.2.16     | 1.8 ～ 17 | [dependency list](https://github.com/apache/dubbo/blob/dubbo-3.2.5/dubbo-dependencies-bom/pom.xml#L94)  | **- Stable version (active)** <br/> **- Features** <br/> &nbsp;&nbsp;- Out-of-box metrics and tracing support.<br/> &nbsp;&nbsp;- Threadpool Isolation<br/> &nbsp;&nbsp;- 30% performance<br/> &nbsp;&nbsp;- Native Image  |
| 3.1.11     | 1.8 ～ 17 | [dependency list](https://github.com/apache/dubbo/blob/dubbo-3.2.11/dubbo-dependencies-bom/pom.xml#L90) | **Stable version (not active)**                                                                                                                                                                                            |

| **Dubbo2** | **JDK** | **Dependencies** | **Description** |
| --- | --- | --- | --- |
| 2.7.23 | 1.8 | [dependency list](https://github.com/apache/dubbo/blob/dubbo-2.7.23/dubbo-dependencies-bom/pom.xml#L92) | EOL |
| 2.6.x, 2.5.x | 1.6 ～ 1.7 |  | EOL |

## Contributing
See [CONTRIBUTING](https://github.com/apache/dubbo/blob/master/CONTRIBUTING.md) for details on submitting patches and the contribution workflow.

### How does the Dubbo Community collaborate?

The Dubbo Community primarily communicates on GitHub through issues, discussions, and pull requests.

- Issues: We use issues to track bugs and tasks. Any **work-related** item is associated with an issue.
- Discussions: We use discussions for questions, early proposals, and announcements. Any **idea-related** item is associated with a discussion.
- Pull Requests: We use pull requests to merge a set of changes from contributors into Dubbo.

We have also implemented [a project board](https://github.com/orgs/apache/projects/337) to monitor all the items.

Any essential changes should be discussed on the mailing list before they happen.

### Seeking help

If you have questions such as:

- What is Dubbo?
- How do I use Dubbo?
- Why did an unexpected result occur?

Please start a discussion at https://github.com/apache/dubbo/discussions.

However, if you encounter the following situations:

- You're certain there's a bug that Dubbo needs to fix,
- You believe a feature could be enhanced,
- You have a detailed proposal for improving Dubbo,

Please open an issue at https://github.com/apache/dubbo/issues.

To ask effective questions, we recommend reading **[How To Ask Questions The Smart Way](https://github.com/selfteaching/How-To-Ask-Questions-The-Smart-Way/blob/master/How-To-Ask-Questions-The-Smart-Way.md)** first.

### Make a Contribution

- Browse the "help wanted" tasks in the [Dubbo project board](https://github.com/orgs/apache/projects/337).
- Participate in discussions on the mailing list. See the subscription [guide](https://github.com/apache/dubbo/wiki/Mailing-list-subscription-guide).
- Respond to queries in the [discussions](https://github.com/apache/dubbo/issues).
- Resolve bugs reported in [issues](https://github.com/apache/dubbo/issues) and send us a pull request.
- Review existing [pull requests](https://github.com/apache/dubbo/pulls).
- Enhance the [website](https://github.com/apache/dubbo-website). We typically need:
    - Blog posts
    - Translations for documentation
    - Use cases showcasing Dubbo integration in enterprise systems.
- Improve the [dubbo-admin](https://github.com/apache/dubbo-admin).
- Contribute to the projects listed in the [ecosystem](https://github.com/apache/?q=dubbo&type=all&language=&sort=).
- Any other forms of contribution not listed above are also welcome.
- If you're interested in contributing, please send an email to [dev@dubbo.apache.org](mailto:dev@dubbo.apache.org) to let us know!

For more details, please take a look at our [guide](https://github.com/apache/dubbo/blob/master/CONTRIBUTING.md) about how to contribute to Dubbo.

## Reporting bugs

Please follow the [template](https://github.com/apache/dubbo/issues/new?template=dubbo-issue-report-template.md) for reporting any issues.

## Reporting a security vulnerability

Please report security vulnerabilities to [us](mailto:security@dubbo.apache.org) privately.

## Contact
* WeChat: apachedubbo
* DingTalk group: 37290003945
* Mailing list: [guide](https://dubbo.apache.org/zh-cn/contact/)
* Twitter: [@ApacheDubbo](https://twitter.com/ApacheDubbo)
* Security issues: please mail to [us](mailto:security@dubbo.apache.org) privately.

## License
Apache Dubbo is licensed under the Apache License Version 2.0. See the [LICENSE](https://github.com/apache/dubbo/blob/3.3/LICENSE) file for details.
