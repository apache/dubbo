# Apache Dubbo Project

[![Build and Test For PR](https://github.com/apache/dubbo/actions/workflows/build-and-test-pr.yml/badge.svg)](https://github.com/apache/dubbo/actions/workflows/build-and-test-pr.yml)
[![Codecov](https://codecov.io/gh/apache/dubbo/branch/3.2/graph/badge.svg)](https://codecov.io/gh/apache/dubbo)
![Maven](https://img.shields.io/maven-central/v/org.apache.dubbo/dubbo.svg)
![License](https://img.shields.io/github/license/alibaba/dubbo.svg)
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/apache/dubbo.svg)](http://isitmaintained.com/project/apache/dubbo "Average time to resolve an issue")
[![Percentage of issues still open](http://isitmaintained.com/badge/open/apache/dubbo.svg)](http://isitmaintained.com/project/apache/dubbo "Percentage of issues still open")
[![Tweet](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?text=Apache%20Dubbo%20is%20a%20high-performance%2C%20java%20based%2C%20open%20source%20RPC%20framework.&url=http://dubbo.apache.org/&via=ApacheDubbo&hashtags=rpc,java,dubbo,micro-service)
[![Twitter Follow](https://img.shields.io/twitter/follow/ApacheDubbo.svg?label=Follow&style=social&logoWidth=0)](https://twitter.com/intent/follow?screen_name=ApacheDubbo)
[![Gitter](https://badges.gitter.im/alibaba/dubbo.svg)](https://gitter.im/alibaba/dubbo?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

Apache Dubbo is an easy-to-use Web and RPC framework that provides multiple
language implementations(Java, [Go](https://github.com/apache/dubbo-go), [Rust](https://github.com/apache/dubbo-rust), [Node.js](https://github.com/apache/dubbo-js), [Web](https://github.com/apache/dubbo-js)) for communication, service discovery, traffic management,
observability, security, tools, and best practices for building enterprise-ready microservices.

Visit [the official website](https://dubbo.apache.org/) for more information.

## Architecture
![Architecture](https://dubbo.apache.org/imgs/architecture.png)

* Consumer and provider communicate with each other using RPC protocol like triple, tcp, rest, etc.
* Consumers automatically trace provider instances registered in registries(Zookeeper, Nacos) and distribute traffic among them by following traffic strategies.
* Rich features for monitoring and managing the cluster with dynamic configuration, metrics, tracing, security, and visualized console.

## Getting started
Follow the instructions below to learn how to:

### Programming with lightweight RPC API
[5 minutes step-by-step guide](https://dubbo.apache.org/zh-cn/overview/quickstart/rpc/java)

Dubbo supports building RPC services with only a few lines of code while depending only on a lightweight SDK (<10MB). The protocol on the wire can be [Triple](https://dubbo.apache.org/zh-cn/overview/reference/protocols/triple/)(fully gRPC compatible and HTTP-friendly), Dubbo2(TCP), REST, or any protocol of your choice.


### Building a microservice application with Spring Boot
[5 minutes step-by-step guide](https://dubbo.apache.org/zh-cn/overview/quickstart/microservice)

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
| **Dubbo3** | **JDK** | **Dependencies** | **Description** |
| --- | --- | --- | --- |
| 3.3.0-beta| 1.8 ～ 17 | [dependency list](https://github.com/apache/dubbo/blob/3.3/dubbo-dependencies-bom/pom.xml#L94)  | **- Unstable version** <br/> **- Features** <br/> &nbsp;&nbsp;  - Triple - gRPC and cURL compatible.<br/>  &nbsp;&nbsp;  - Rest-style programming support.<br/>  &nbsp;&nbsp;  - Spring Boot Starters. |
| 3.2.5 | 1.8 ～ 17 | [dependency list](https://github.com/apache/dubbo/blob/dubbo-3.2.5/dubbo-dependencies-bom/pom.xml#L94) | **- Stable version (active)** <br/> **- Features** <br/> &nbsp;&nbsp;- Out-of-box metrics and tracing support.<br/> &nbsp;&nbsp;- Threadpool Isolation<br/> &nbsp;&nbsp;- 30% performance<br/> &nbsp;&nbsp;- Native Image|
| 3.1.11 | 1.8 ～ 11 | [dependency list](https://github.com/apache/dubbo/blob/dubbo-3.2.11/dubbo-dependencies-bom/pom.xml#L94) | **Stable version (not active)** |

| **Dubbo2** | **JDK** | **Dependencies** | **Description** |
| --- | --- | --- | --- |
| 2.7.23 | 1.8 | [dependency list](https://github.com/apache/dubbo/blob/dubbo-2.7.23/dubbo-dependencies-bom/pom.xml#L92) | EOL |
| 2.6.x, 2.5.x | 1.6 ～ 1.7 |  | EOL |

## Contributing
See [CONTRIBUTING](https://github.com/apache/dubbo/blob/master/CONTRIBUTING.md) for details on submitting patches and the contribution workflow.

## Contact
* WeChat: apachedubbo
* DingTalk group: 37290003945
* Mailing list: [guide](https://dubbo.apache.org/zh-cn/contact/)
* Twitter: [@ApacheDubbo](https://twitter.com/ApacheDubbo)
* Security issues: please mail to [us](mailto:security@dubbo.apache.org) privately.

## Contributing

See [CONTRIBUTING](https://github.com/apache/dubbo/blob/master/CONTRIBUTING.md) for details on submitting patches and the contribution workflow.

### How can I contribute?

* Take a look at issues with tags marked [`Good first issue`](https://github.com/apache/dubbo/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) or [`Help wanted`](https://github.com/apache/dubbo/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22).
* Join the discussion on the mailing list, subscription [guide](https://github.com/apache/dubbo/wiki/Mailing-list-subscription-guide).
* Answer questions on [issues](https://github.com/apache/dubbo/issues).
* Fix bugs reported on [issues](https://github.com/apache/dubbo/issues), and send us a pull request.
* Review the existing [pull request](https://github.com/apache/dubbo/pulls).
* Improve the [website](https://github.com/apache/dubbo-website), typically we need
  * blog post
  * translation on documentation
  * use cases around the integration of Dubbo in enterprise systems.
* Improve the [dubbo-admin/dubbo-monitor](https://github.com/apache/dubbo-admin).
* Contribute to the projects listed in [ecosystem](https://github.com/dubbo).
* Other forms of contribution not explicitly enumerated above.
* If you would like to contribute, please send an email to dev@dubbo.apache.org to let us know!

## Reporting bugs

Please follow the [template](https://github.com/apache/dubbo/issues/new?template=dubbo-issue-report-template.md) for reporting any issues.

## Reporting a security vulnerability

Please report security vulnerabilities to [us](mailto:security@dubbo.apache.org) privately.

## Dubbo ecosystem

* [Dubbo Ecosystem Entry](https://github.com/apache?utf8=%E2%9C%93&q=dubbo&type=&language=) - A GitHub group `dubbo` to gather all Dubbo relevant projects not appropriate in [apache](https://github.com/apache) group yet
* [Dubbo Website](https://github.com/apache/dubbo-website) - Apache Dubbo official website
* [Dubbo Samples](https://github.com/apache/dubbo-samples) - samples for Apache Dubbo
* [Dubbo Spring Boot](https://github.com/apache/dubbo-spring-boot-project) - Spring Boot Project for Dubbo
* [Dubbo Admin](https://github.com/apache/dubbo-admin) - The reference implementation for Dubbo admin
* [Dubbo Awesome](https://github.com/apache/dubbo-awesome) - Dubbo's slides and video links in Meetup

#### Language

* [Go](https://github.com/apache/dubbo-go) (recommended)
* [Rust](https://github.com/apache/dubbo-rust)
* [Node.js](https://github.com/apache/dubbo-js)
* [Python](https://github.com/dubbo/py-client-for-apache-dubbo)
* [PHP](https://github.com/apache/dubbo-php-framework)
* [Erlang](https://github.com/apache/dubbo-erlang)

## License
Apache Dubbo is licensed under the Apache License Version 2.0. See the [LICENSE](https://github.com/apache/dubbo/blob/master/LICENSE) file for details.
