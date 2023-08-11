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

* **[RPC protocol]():** triple, tcp, rest and more.
* **[Service Discovery]():** Nacos, Zookeeper, Kubernetes, etc.
* **[Microservice solution]():** traffic routing, configuration, observability, tracing, deploying to Kubernetes, service mesh, etc.

## Getting started
Following the instructions below to learn how to:
* Programming with lightweight RPC API
* Start a microservice application with Spring Boot

### Lightweight RPC API
[5 minutes step-by-step guide](https://dubbo.apache.org/zh-cn/overview/quickstart/rpc/java)

Dubbo supports building RPC services with only a few lines of code while depending only on a lightweight SDK (<10MB). The protocol on the wire can be [Triple(fully gRPC compatible and HTTP-friendly)](https://cn.dubbo.apache.org/zh-cn/overview/reference/protocols/triple/), Dubbo2(TCP), REST, or any protocol of your choice.


### Start a microservice application with Spring Boot
[5 minutes step-by-step guide](https://dubbo.apache.org/zh-cn/overview/quickstart/microservice)

It's highly recommended to start your microservice application with the Spring Boot Starter `dubbo-spring-boot-starter` provided by Dubbo. With only a single dependency and yaml file, and optionally a bunch of other useful spring boot starters, you can enable all of the Dubo features like service discovery, observability, tracing, etc.

Next, learn how to [deploy](), [monitor](), and [manage]() your Dubbo application and cluster.

### Rlease guide
* 3.3  jdk
* 3.2  jdk
* 3.1  jdk
* 2.7.23  jdk

## More Features
Get more details by visiting [the official website](https://cn.dubbo.apache.org/zh-cn/overview/tasks/) to get your hands dirty with some well-designed tasks.

* gRPC compatible and http-friendly RPC protocol
* IDL and non-IDL programming API
* Traffic routing
* Service discovery
* Observability
* Extensibility
* Security
* Visualized console and control plane
* Kubernetes and Service mesh

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
