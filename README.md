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

Apache Dubbo is an easy-to-use Web and RPC framework that provides different
language implementations([Java](https://github.com/apache/dubbo), [Go](https://github.com/apache/dubbo-go), [Rust](https://github.com/apache/dubbo-rust), [Node.js](https://github.com/apache/dubbo-js), [Web](https://github.com/apache/dubbo-js)) for communication, service discovery, traffic management,
observability, security, tools, and best practices for building enterprise-ready microservices.

## Architecture
![Architecture](https://dubbo.apache.org/imgs/architecture.png)

## Getting started
Following the instructions below to learn how to build [RPC]() and [Microservices]() using Dubbo.

### RPC
Starting from Dubbo3, lightweight , fully compatible with gRPC,
```xml

```

using curl to

```shell
curl
```

### Microservices


Spring Boot

yaml file
```yaml

```

Spring boot starters
```xml
dubbo-spring-boot-starter
dubbo-spring-boot-zookeeper-starter
dubbo-spring-boot-nacos-starter
dubbo-spring-boot-observability-starter
dubbo-spring-boot-tracing-starter
```

## Features
* gRPC compatible and http friendly rpc protocol
* IDL and non-IDL programming api
* Traffic routing
* Service discovery
* Observability
* Extensibility
* Security
* Visualized console and control plane
* Kubernetes and Service mesh

## Building

If you want to try out the cutting-edge features, you can build with the following commands. (Java 1.8 is needed to build the master branch)

```
  mvn clean install
```

## Contributing
See [CONTRIBUTING](https://github.com/apache/dubbo/blob/master/CONTRIBUTING.md) for details on submitting patches and the contribution workflow.

## Contact
* Wechat: Apache Dubbo
* Dingtalk group:
* Mailing list for dev/user discussion: [guide](https://cn.dubbo.apache.org/zh-cn/contact/)
* Twitter: [@ApacheDubbo](https://twitter.com/ApacheDubbo)

## Reporting a security vulnerability
Please report security vulnerabilities to [us](mailto:security@dubbo.apache.org) privately.

## License
Apache Dubbo is licenced under the Apache License Version 2.0. See the [LICENSE](https://github.com/apache/dubbo/blob/master/LICENSE) file for details.
