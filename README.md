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
* **[More features]():** traffic routing, configuration, observability, tracing, deploying to Kubernetes, service mesh, etc.

## Getting started
Following the instructions below to learn how to:
* Programming with lightweight RPC API
* Start a microservice application with Spring Boot

### Lightweight RPC API
[5 minutes step-by-step guide](https://dubbo.apache.org/zh-cn/overview/quickstart/rpc/java)

Dubbo supports building RPC services with only a few lines of code while depending only on a lightweight SDK (<10MB). The protocol on the wire can be [Triple(fully gRPC compatible and HTTP-friendly)](https://cn.dubbo.apache.org/zh-cn/overview/reference/protocols/triple/), Dubbo2(TCP), REST, or any protocol of your choice.

```java
private void startServer() {
    DubboBootstrap.getInstance()
        .service(ServiceBuilder.newBuilder().ref(new GreetingsServiceImpl()).build())
        .start()
        .await();
}

private void startClient() {
    DubboBootstrap.getInstance()
        .service(ReferenceBuilder.newBuilder().interfaceClass(GreetingService.class).url("tri://localhost:50051").build())
        .start()
        .await();
}
```

For triple protocol, you can easily use cURL to test the service works as expected:

```shell
curl \
    --header "Content-Type: application/json" \
    --data '[{"name": "Dubbo"}]' \
    http://localhost:50052/org.apache.dubbo.samples.tri.unary.Greeter/greet/
```

### Start a microservice application with Spring Boot
[5 minutes step-by-step guide](https://dubbo.apache.org/zh-cn/overview/quickstart/microservice)

It's highly recommended to start your microservice application with the Spring Boot Starter provided by Dubbo. 

`application.yml` configuration file
```yaml
dubbo:
  application:
    name: dubbo-demo-provider
  protocol:
    name: tri
    port: -1
  registry:
    address: zookeeper://${zookeeper.address:127.0.0.1}:2181
```

Add `dubbo-spring-boot-starter` and optionally a bunch of useful spring boot starters ready to enable features like service discovery, observability, tracing, etc.
```xml
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-spring-boot-starter</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-spring-boot-zookeeper-starter</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-spring-boot-observability-starter</artifactId>
</dependency>
```

Next, learn how to [deploy](), [monitor](), and [manage]() your Dubbo application and cluster.

## Features
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
See [CONTRIBUTING](https://github.com/apache/dubbo/blob/master/CONTRIBUTING.md) for details on submitting patches and the contribution workflow.

## Contact
* WeChat: apachedubbo
* DingTalk group: 37290003945
* Mailing list: [guide](https://cn.dubbo.apache.org/zh-cn/contact/)
* Twitter: [@ApacheDubbo](https://twitter.com/ApacheDubbo)
* Security issues: please mail to [us](mailto:security@dubbo.apache.org) privately.

## License
Apache Dubbo is licensed under the Apache License Version 2.0. See the [LICENSE](https://github.com/apache/dubbo/blob/master/LICENSE) file for details.
