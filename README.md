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

Apache Dubbo is a high-performance, Java-based open-source RPC framework. Please visit the [official site](http://dubbo.apache.org) for the quick start guide and documentation, as well as the [wiki](https://github.com/apache/dubbo/wiki) for news, FAQ, and release notes.

We are now collecting Dubbo user info to help us to improve Dubbo further. Kindly support us by providing your usage information on [issue#9436: Wanted: who's using dubbo](https://github.com/apache/dubbo/issues/9436), thanks :)

## Architecture

![Architecture](https://dubbo.apache.org/imgs/architecture.png)

## Features

* Transparent interface based RPC
* Intelligent load balancing
* Automatic service registration and discovery
* High extensibility
* Runtime traffic routing
* Visualized service governance

## Getting started

The following code snippet comes from [Dubbo Samples](https://github.com/apache/dubbo-samples.git). You may clone the sample project and step into the `dubbo-samples-api` subdirectory before proceeding.

```bash
git clone https://github.com/apache/dubbo-samples.git
cd dubbo-samples/dubbo-samples-api
```

There's a [README](https://github.com/apache/dubbo-samples/blob/389cd612f1ea57ee6e575005b32f195c442c35a2/1-basic/dubbo-samples-api/README.md) file under `dubbo-samples-api` directory. We recommend referencing the samples in that directory by following the below-mentioned instructions: 

### Maven dependency

```xml
<properties>
    <dubbo.version>3.0.10</dubbo.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo</artifactId>
        <version>${dubbo.version}</version>
    </dependency>
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-dependencies-zookeeper</artifactId>
        <version>${dubbo.version}</version>
        <type>pom</type>
    </dependency>
</dependencies>
```

### Define service interfaces

```java
package org.apache.dubbo.samples.api;

public interface GreetingsService {
    String sayHi(String name);
}
```

*See [api/GreetingsService.java](https://github.com/apache/dubbo-samples/blob/389cd612f1ea57ee6e575005b32f195c442c35a2/1-basic/dubbo-samples-api/src/main/java/org/apache/dubbo/samples/api/GreetingsService.java) on GitHub.*

### Implement service interface for the provider

```java
package org.apache.dubbo.samples.provider;

import org.apache.dubbo.samples.api.GreetingsService;

public class GreetingsServiceImpl implements GreetingsService {
    @Override
    public String sayHi(String name) {
        return "hi, " + name;
    }
}
```

*See [provider/GreetingsServiceImpl.java](https://github.com/apache/dubbo-samples/blob/389cd612f1ea57ee6e575005b32f195c442c35a2/1-basic/dubbo-samples-api/src/main/java/org/apache/dubbo/samples/provider/GreetingsServiceImpl.java) on GitHub.*

### Start service provider

```java
package org.apache.dubbo.samples.provider;


import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.samples.api.GreetingsService;

import java.util.concurrent.CountDownLatch;

public class Application {
    private static String zookeeperHost = System.getProperty("zookeeper.address", "127.0.0.1");

    public static void main(String[] args) throws Exception {
        ServiceConfig<GreetingsService> service = new ServiceConfig<>();
        service.setApplication(new ApplicationConfig("first-dubbo-provider"));
        service.setRegistry(new RegistryConfig("zookeeper://" + zookeeperHost + ":2181"));
        service.setInterface(GreetingsService.class);
        service.setRef(new GreetingsServiceImpl());
        service.export();

        System.out.println("dubbo service started");
        new CountDownLatch(1).await();
    }
}
```

*See [provider/Application.java](https://github.com/apache/dubbo-samples/blob/389cd612f1ea57ee6e575005b32f195c442c35a2/1-basic/dubbo-samples-spring-xml/src/main/java/org/apache/dubbo/samples/provider/Application.java) on GitHub.*

### Build and run the provider

```bash
mvn clean package
mvn -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.apache.dubbo.samples.provider.Application exec:java
```

### Call remote service in the consumer

```java
package org.apache.dubbo.samples.client;


import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.samples.api.GreetingsService;

public class Application {
    private static String zookeeperHost = System.getProperty("zookeeper.address", "127.0.0.1");

    public static void main(String[] args) {
        ReferenceConfig<GreetingsService> reference = new ReferenceConfig<>();
        reference.setApplication(new ApplicationConfig("first-dubbo-consumer"));
        reference.setRegistry(new RegistryConfig("zookeeper://" + zookeeperHost + ":2181"));
        reference.setInterface(GreetingsService.class);
        GreetingsService service = reference.get();
        String message = service.sayHi("dubbo");
        System.out.println(message);
    }
}
```
*See [client/Application.java](https://github.com/apache/dubbo-samples/blob/389cd612f1ea57ee6e575005b32f195c442c35a2/1-basic/dubbo-samples-api/src/main/java/org/apache/dubbo/samples/client/Application.java) on GitHub.*

### Build and run the consumer

```bash
mvn clean package
mvn -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.apache.dubbo.samples.client.Application exec:java
```

The consumer will print out `hi, dubbo` on the screen.


### Next steps

* [Your first Dubbo application](https://dubbo.apache.org/en/blog/2018/08/07/dubbo-101/) - A 101 tutorial to reveal more details, with the same code above.
* [Dubbo user manual](https://dubbo.apache.org/en/overview/what/) - How to use Dubbo and all its features.
* [Dubbo developer guide](https://dubbo.apache.org/en/docs3-v2/java-sdk/) - How to involve in Dubbo development.
* [Dubbo admin manual](https://dubbo.apache.org/en/docs/v2.7/admin/ops/) - How to admin and manage Dubbo services.

## Building

If you want to try out the cutting-edge features, you can build with the following commands. (Java 1.8 is needed to build the master branch)

```
  mvn clean install
```

## Contact

* Mailing list:
  * dev list: for dev/user discussion. [subscribe](mailto:dev-subscribe@dubbo.apache.org), [unsubscribe](mailto:dev-unsubscribe@dubbo.apache.org), [archive](https://lists.apache.org/list.html?dev@dubbo.apache.org),  [guide](https://github.com/apache/dubbo/wiki/Mailing-list-subscription-guide)

* Bugs: [Issues](https://github.com/apache/dubbo/issues/new?template=dubbo-issue-report-template.md)
* Gitter: [Gitter channel](https://gitter.im/alibaba/dubbo)
* Twitter: [@ApacheDubbo](https://twitter.com/ApacheDubbo)

## Contributing

See [CONTRIBUTING](https://github.com/apache/dubbo/blob/master/CONTRIBUTING.md) for details on submitting patches and the contribution workflow.

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

Please follow the [template](https://github.com/apache/dubbo/issues/new?template=dubbo-issue-report-template.md) for reporting any issues.

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

* [Go](https://github.com/dubbo/dubbo-go) (recommended)
* [Node.js](https://github.com/apache/dubbo-js)
* [Python](https://github.com/dubbo/py-client-for-apache-dubbo)
* [PHP](https://github.com/apache/dubbo-php-framework)
* [Erlang](https://github.com/apache/dubbo-erlang)

## License

Apache Dubbo software is licenced under the Apache License Version 2.0. See the [LICENSE](https://github.com/apache/dubbo/blob/master/LICENSE) file for details.
