# Apache Dubbo Project

[![Build and Test For PR](https://github.com/apache/dubbo/actions/workflows/build-and-test-pr.yml/badge.svg)](https://github.com/apache/dubbo/actions/workflows/build-and-test-pr.yml)
[![Codecov](https://codecov.io/gh/apache/dubbo/branch/3.2/graph/badge.svg)](https://codecov.io/gh/apache/dubbo)
![Maven](https://img.shields.io/maven-central/v/org.apache.dubbo/dubbo.svg)
![License](https://img.shields.io/github/license/alibaba/dubbo.svg)
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/apache/dubbo.svg)](http://isitmaintained.com/project/apache/dubbo "Average time to resolve an issue")
[![Percentage of issues still open](http://isitmaintained.com/badge/open/apache/dubbo.svg)](http://isitmaintained.com/project/apache/dubbo "Percentage of issues still open")

Apache Dubbo is a high-performance, Java-based open-source RPC framework. Please visit the [official site](http://dubbo.apache.org) for the quick start guide and documentation, as well as the [wiki](https://github.com/apache/dubbo/wiki) for news, FAQ, and release notes.

We are now collecting Dubbo user info to help us to improve Dubbo further. Kindly support us by providing your usage information on [Wanted: who's using dubbo](https://github.com/apache/dubbo/discussions/13842), thanks :)

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
cd dubbo-samples/1-basic/dubbo-samples-api
```

There's a [README](https://github.com/apache/dubbo-samples/blob/389cd612f1ea57ee6e575005b32f195c442c35a2/1-basic/dubbo-samples-api/README.md) file under `dubbo-samples-api` directory. We recommend referencing the samples in that directory by following the below-mentioned instructions:

### Maven dependency

```xml
<properties>
    <dubbo.version>3.2.13-SNAPSHOT</dubbo.version>
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

## Recommended Test Environment
To avoid intermittent test failures (i.e., flaky tests), it is recommended to have a machine or virtual machine with the following specifications:

* Minimum of 2CPUs.
* Minimum of 2Gb of RAM.

### How does the Dubbo Community collaborate?

The Dubbo Community primarily communicates on GitHub through issues, discussions, and pull requests.

- Issues: We use issues to track bugs and tasks. Any **work-related** item is associated with an issue.
- Discussions: We use discussions for questions, early proposals, and announcements. Any **idea-related** item is associated with a discussion.
- Pull Requests: We use pull requests to merge a set of changes from contributors into Dubbo.

We have also implemented [a project board](https://github.com/orgs/apache/projects/337) to monitor all the items.

Any essential changes should be discussed on the mailing list before they happen.

### Seeking for help

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

### Contribution

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

For further details, please refer our [guide](https://github.com/apache/dubbo/blob/master/CONTRIBUTING.md) about how to contribute Dubbo.

## Reporting bugs

Please follow the [template](https://github.com/apache/dubbo/issues/new?template=dubbo-issue-report-template.md) for reporting any issues.

## Reporting a security vulnerability

Please report security vulnerabilities to [us](mailto:security@dubbo.apache.org) privately.

## Contact

* Mailing list:
    * dev list: for dev/user discussion. [subscribe](mailto:dev-subscribe@dubbo.apache.org), [unsubscribe](mailto:dev-unsubscribe@dubbo.apache.org), [archive](https://lists.apache.org/list.html?dev@dubbo.apache.org),  [guide](https://github.com/apache/dubbo/wiki/Mailing-list-subscription-guide)

* Bugs: [Issues](https://github.com/apache/dubbo/issues/new?template=dubbo-issue-report-template.md)
* Gitter: [Gitter channel](https://gitter.im/alibaba/dubbo)
* Twitter: [@ApacheDubbo](https://twitter.com/ApacheDubbo)

## Dubbo ecosystem

* [Dubbo Ecosystem Entry](https://github.com/apache?utf8=%E2%9C%93&q=dubbo&type=&language=) - A GitHub group `dubbo` to gather all Dubbo relevant projects not appropriate in [apache](https://github.com/apache) group yet
* [Dubbo Website](https://github.com/apache/dubbo-website) - Apache Dubbo official website
* [Dubbo Samples](https://github.com/apache/dubbo-samples) - samples for Apache Dubbo
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

Apache Dubbo software is licensed under the Apache License Version 2.0. See the [LICENSE](https://github.com/apache/dubbo/blob/master/LICENSE) file for details.
