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

* **[RPC protocol](https://cn.dubbo.apache.org/zh-cn/overview/core-features/protocols/):** triple, tcp, rest and more.
* **[Service Discovery](https://cn.dubbo.apache.org/zh-cn/overview/core-features/service-discovery/):** Nacos, Zookeeper, Kubernetes, etc.
* **[Microservice solution](https://cn.dubbo.apache.org/zh-cn/overview/core-features/):** traffic routing, configuration, observability, tracing, deploying to Kubernetes, service mesh, etc.

## Getting started
Follow the instructions below to learn how to:

### Programming with lightweight RPC API
[5 minutes step-by-step guide](https://dubbo.apache.org/zh-cn/overview/quickstart/rpc/java)

Dubbo supports building RPC services with only a few lines of code while depending only on a lightweight SDK (<10MB). The protocol on the wire can be [Triple](https://cn.dubbo.apache.org/zh-cn/overview/reference/protocols/triple/)(fully gRPC compatible and HTTP-friendly), Dubbo2(TCP), REST, or any protocol of your choice.


### Building a microservice application with Spring Boot
[5 minutes step-by-step guide](https://dubbo.apache.org/zh-cn/overview/quickstart/microservice)

It's highly recommended to start your microservice application with the Spring Boot Starter `dubbo-spring-boot-starter` provided by Dubbo. With only a single dependency and yaml file, and optionally a bunch of other useful spring boot starters, you can enable all of the Dubo features like service discovery, observability, tracing, etc.

Next, learn how to [deploy](https://cn.dubbo.apache.org/zh-cn/overview/tasks/deploy/), [monitor](https://cn.dubbo.apache.org/zh-cn/overview/tasks/observability/), and [manage the traffic](https://cn.dubbo.apache.org/zh-cn/overview/tasks/traffic-management/) of your Dubbo application and cluster.

## Which Dubbo version should I use?
| **dubbo3** | **jdk** | **组件版本** | **说明** |
| --- | --- | --- | --- |
| 3.3.0 | 1.8 ～ 17 | dependencies list |  |
| 3.2. | 1.8 ～ 17 | dependencies list |  |
| 3.1. | 1.8 ～ 11 | dependencies list |  |

| **dubbo2** | **jdk** | **组件版本** | **说明** |
| --- | --- | --- | --- |
| 2.7.23 | 1.8 | dependencies list |  |
| 2.6.x, 2.5.x | 1.6 ～ 1.7 | dependencies list |  |


## More Features
Get more details by visiting [the official website](https://cn.dubbo.apache.org/zh-cn/overview/tasks/) to get your hands dirty with some well-designed tasks.

* [Launch a Dubbo project](https://cn.dubbo.apache.org/zh-cn/overview/tasks/develop/template/)
* [RPC protocols](https://cn.dubbo.apache.org/zh-cn/overview/core-features/protocols/)
* [Traffic routing](https://cn.dubbo.apache.org/zh-cn/overview/core-features/traffic/)
* [Service discovery](https://cn.dubbo.apache.org/zh-cn/overview/core-features/service-discovery/)
* [Observability](https://cn.dubbo.apache.org/zh-cn/overview/core-features/observability/)
* [Extensibility](https://cn.dubbo.apache.org/zh-cn/overview/core-features/extensibility/)
* [Security](https://cn.dubbo.apache.org/zh-cn/overview/core-features/security/)
* [Visualized console and control plane](https://cn.dubbo.apache.org/zh-cn/overview/reference/admin/)
* [Kubernetes and Service mesh](https://cn.dubbo.apache.org/zh-cn/overview/core-features/service-mesh/)

## Contributing
Add developer's guide, how to build from source.

See [CONTRIBUTING](https://github.com/apache/dubbo/blob/master/CONTRIBUTING.md) for details on submitting patches and the contribution workflow.

## Contact
* WeChat: apachedubbo
* DingTalk group: 37290003945
* Mailing list: [guide](https://cn.dubbo.apache.org/zh-cn/contact/)
* Twitter: [@ApacheDubbo](https://twitter.com/ApacheDubbo)
* Security issues: please mail to [us](mailto:security@dubbo.apache.org) privately.

## License
Apache Dubbo is licensed under the Apache License Version 2.0. See the [LICENSE](https://github.com/apache/dubbo/blob/master/LICENSE) file for details.
