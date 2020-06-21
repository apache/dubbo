# Apache Dubbo Project

[![Build Status](https://travis-ci.org/apache/dubbo.svg?branch=master)](https://travis-ci.org/apache/dubbo)
[![codecov](https://codecov.io/gh/apache/dubbo/branch/master/graph/badge.svg)](https://codecov.io/gh/apache/dubbo)
![maven](https://img.shields.io/maven-central/v/org.apache.dubbo/dubbo.svg)
![license](https://img.shields.io/github/license/alibaba/dubbo.svg)
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/apache/dubbo.svg)](http://isitmaintained.com/project/apache/dubbo "Average time to resolve an issue")
[![Percentage of issues still open](http://isitmaintained.com/badge/open/apache/dubbo.svg)](http://isitmaintained.com/project/apache/dubbo "Percentage of issues still open")
[![Tweet](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?text=Apache%20Dubbo%20is%20a%20high-performance%2C%20java%20based%2C%20open%20source%20RPC%20framework.&url=http://dubbo.apache.org/&via=ApacheDubbo&hashtags=rpc,java,dubbo,micro-service)
[![](https://img.shields.io/twitter/follow/ApacheDubbo.svg?label=Follow&style=social&logoWidth=0)](https://twitter.com/intent/follow?screen_name=ApacheDubbo)
[![Gitter](https://badges.gitter.im/alibaba/dubbo.svg)](https://gitter.im/alibaba/dubbo?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

Apache Dubbo is a high-performance, Java based open source RPC framework. Please visit [official site](http://dubbo.apache.org) for quick start and documentations, as well as [Wiki](https://github.com/apache/dubbo/wiki) for news, FAQ, and release notes.

We are now collecting dubbo user info in order to help us to improve Dubbo better, pls. kindly help us by providing yours on [issue#1012: Wanted: who's using dubbo](https://github.com/apache/dubbo/issues/1012), thanks :)

## Architecture

![Architecture](http://dubbo.apache.org/img/architecture.png)

## Features

* Transparent interface based RPC
* Intelligent load balancing
* Automatic service registration and discovery
* High extensibility
* Runtime traffic routing
* Visualized service governance

## Getting started

The following code snippet comes from [Dubbo Samples](https://github.com/apache/dubbo-samples/tree/master/java/dubbo-samples-api). You may clone the sample project and step into `dubbo-samples-api` sub directory before read on.

```bash
# git clone https://github.com/apache/dubbo-samples.git
# cd dubbo-samples/java/dubbo-samples-api
```

There's a [README](https://github.com/apache/dubbo-samples/tree/master/java/dubbo-samples-api/README.md) file under `dubbo-samples-api` directory. Read it and try this sample out by following the instructions.

### Maven dependency

```xml
<properties>
    <dubbo.version>2.7.7</dubbo.version>
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

*See [api/GreetingsService.java](https://github.com/apache/dubbo-samples/blob/master/java/dubbo-samples-api/src/main/java/org/apache/dubbo/samples/api/GreetingsService.java) on GitHub.*

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

*See [provider/GreetingsServiceImpl.java](https://github.com/apache/dubbo-samples/blob/master/java/dubbo-samples-api/src/main/java/org/apache/dubbo/samples/provider/GreetingsServiceImpl.java) on GitHub.*

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

*See [provider/Application.java](https://github.com/apache/dubbo-samples/blob/master/java/dubbo-samples-api/src/main/java/org/apache/dubbo/samples/provider/Application.java) on GitHub.*

### Build and run the provider

```bash
# mvn clean package
# mvn -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.apache.dubbo.samples.provider.Application exec:java
```

### Call remote service in consumer

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
*See [consumer/Application.java](https://github.com/apache/dubbo-samples/blob/master/java/dubbo-samples-api/src/main/java/org/apache/dubbo/samples/client/Application.java) on GitHub.*

### Build and run the consumer

```bash
# mvn clean package
# mvn -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.apache.dubbo.samples.client.Application exec:java
```

The consumer will print out `hi, dubbo` on the screen.


### Next steps

* [Your first Dubbo application](http://dubbo.apache.org/en-us/blog/dubbo-101.html) - A 101 tutorial to reveal more details, with the same code above.
* [Dubbo user manual](http://dubbo.apache.org/en-us/docs/user/preface/background.html) - How to use Dubbo and all its features.
* [Dubbo developer guide](http://dubbo.apache.org/en-us/docs/dev/build.html) - How to involve in Dubbo development.
* [Dubbo admin manual](http://dubbo.apache.org/en-us/docs/admin/install/provider-demo.html) - How to admin and manage Dubbo services.

## Building

If you want to try out the cutting-edge features, you can build with the following commands. (Java 1.8 is required to build the master branch)

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

* Take a look at issues with tag called [`Good first issue`](https://github.com/apache/dubbo/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) or [`Help wanted`](https://github.com/apache/dubbo/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22).
* Join the discussion on mailing list, subscription [guide](https://github.com/apache/dubbo/wiki/Mailing-list-subscription-guide).
* Answer questions on [issues](https://github.com/apache/dubbo/issues).
* Fix bugs reported on [issues](https://github.com/apache/dubbo/issues), and send us pull request.
* Review the existing [pull request](https://github.com/apache/dubbo/pulls).
* Improve the [website](https://github.com/apache/dubbo-website), typically we need
  * blog post
  * translation on documentation
  * use cases about how Dubbo is being used in enterprise system.
* Improve the [dubbo-admin/dubbo-monitor](https://github.com/apache/dubbo-admin).
* Contribute to the projects listed in [ecosystem](https://github.com/dubbo).
* Any form of contribution that is not mentioned above.
* If you would like to contribute, please send an email to dev@dubbo.apache.org to let us know!

## Reporting bugs

Please follow the [template](https://github.com/apache/dubbo/issues/new?template=dubbo-issue-report-template.md) for reporting any issues.

## Reporting a security vulnerability

Please report security vulnerability to [us](mailto:security@dubbo.apache.org) privately.

## Dubbo ecosystem

* [Dubbo Ecosystem Entry](https://github.com/apache?utf8=%E2%9C%93&q=dubbo&type=&language=) - A GitHub group `dubbo` to gather all Dubbo relevant projects not appropriate in [apache](https://github.com/apache) group yet
* [Dubbo Website](https://github.com/apache/dubbo-website) - Apache Dubbo official website
* [Dubbo Samples](https://github.com/apache/dubbo-samples) - samples for Apache Dubbo
* [Dubbo Spring Boot](https://github.com/apache/dubbo-spring-boot-project) - Spring Boot Project for Dubbo
* [Dubbo Admin](https://github.com/apache/dubbo-admin) - The reference implementation for Dubbo admin
* [Dubbo Awesome](https://github.com/apache/dubbo-awesome) - Dubbo's slides and video links in Meetup

#### Language

* [Node.js](https://github.com/apache/dubbo-js)
* [Python](https://github.com/dubbo/py-client-for-apache-dubbo)
* [PHP](https://github.com/apache/dubbo-php-framework)
* [Go](https://github.com/dubbo/dubbo-go)
* [Erlang](https://github.com/apache/dubbo-erlang)

## License

Apache Dubbo is under the Apache 2.0 license. See the [LICENSE](https://github.com/apache/dubbo/blob/master/LICENSE) file for details.
