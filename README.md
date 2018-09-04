# Apache Dubbo (incubating) Project

[![Build Status](https://travis-ci.org/apache/incubator-dubbo.svg?branch=master)](https://travis-ci.org/apache/incubator-dubbo)
[![codecov](https://codecov.io/gh/apache/incubator-dubbo/branch/master/graph/badge.svg)](https://codecov.io/gh/apache/incubator-dubbo)
[![Gitter](https://badges.gitter.im/alibaba/dubbo.svg)](https://gitter.im/alibaba/dubbo?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
![license](https://img.shields.io/github/license/alibaba/dubbo.svg)
![maven](https://img.shields.io/maven-central/v/com.alibaba/dubbo.svg)

Apache Dubbo (incubating) is a high-performance, java based open source RPC framework. Please visit [official site ](http://dubbo.incubator.apache.org) for quick start and documentations, as well as [Wiki](https://github.com/apache/incubator-dubbo/wiki) for news, FAQ, and release notes.

We are now collecting dubbo user info in order to help us to improve dubbo better, pls. kindly help us by providing yours on [issue#1012: Wanted: who's using dubbo](https://github.com/apache/incubator-dubbo/issues/1012), thanks :)

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

### Maven dependency

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>dubbo</artifactId>
    <version>2.6.2</version>
</dependency>
```

### Defining service interfaces

```java
package org.apache.dubbo.demo;

public interface GreetingService {
    String sayHello(String name);
}
```

### Implement interface in service provider

```java
package org.apache.dubbo.demo.provider;
 
import org.apache.dubbo.demo.GreetingService;
 
public class GreetingServiceImpl implements GreetingService {
    public String sayHello(String name) {
        return "Hello " + name;
    }
}
```

### Starting service provider

```java
package org.apache.dubbo.demo.provider;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import org.apache.dubbo.demo.GreetingService;

import java.io.IOException;
 
public class Provider {

    public static void main(String[] args) throws IOException {
        ServiceConfig<GreetingService> serviceConfig = new ServiceConfig<GreetingService>();
        serviceConfig.setApplication(new ApplicationConfig("first-dubbo-provider"));
        serviceConfig.setRegistry(new RegistryConfig("multicast://224.5.6.7:1234"));
        serviceConfig.setInterface(GreetingService.class);
        serviceConfig.setRef(new GreetingServiceImpl());
        serviceConfig.export();
        System.in.read();
    }
}
```

### Call remote service in consumer

```java
package org.apache.dubbo.demo.consumer;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import org.apache.dubbo.demo.GreetingService;

public class Consumer {
    public static void main(String[] args) {
        ReferenceConfig<GreetingService> referenceConfig = new ReferenceConfig<GreetingService>();
        referenceConfig.setApplication(new ApplicationConfig("first-dubbo-consumer"));
        referenceConfig.setRegistry(new RegistryConfig("multicast://224.5.6.7:1234"));
        referenceConfig.setInterface(GreetingService.class);
        GreetingService greetingService = referenceConfig.get();
        System.out.println(greetingService.sayHello("world"));
    }
}
```

### Next steps

* [Dubbo user manual](http://dubbo.apache.org/en-us/docs/user/preface/background.html) - How to use Dubbo and all its features.
* [Dubbo developer guide](http://dubbo.apache.org/en-us/docs/dev/build.html) - How to invovle in Dubbo development.
* [Dubbo admin manual](http://dubbo.apache.org/en-us/docs/admin/install/provider-demo.html) - How to admin and manage Dubbo services.

## Contact

* Mailing list: [dev@dubbo.incubator.apache.org](mailto:[dev-subscribe@dubbo.incubator.apache.org]) (subscription [guide](https://github.com/apache/incubator-dubbo/wiki/Mailing-list-subscription-guide))
* Bugs: [Issues](https://github.com/apache/incubator-dubbo/issues/new?template=dubbo-issue-report-template.md)
* Gitter: [Gitter channel](https://gitter.im/alibaba/dubbo) 
* Twitter: [@ApacheDubbo](https://twitter.com/ApacheDubbo)

## Contributing

See [CONTRIBUTING](https://github.com/apache/incubator-dubbo/blob/master/CONTRIBUTING.md) for details on submitting patches and the contribution workflow.

### How can I contribute?

* Take a look at issues with tag called [`Good first issue`](https://github.com/apache/incubator-dubbo/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22) or [`Help wanted`](https://github.com/apache/incubator-dubbo/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22).
* Join the discussion on mailing list, subscription [guide](https://github.com/apache/incubator-dubbo/wiki/Mailing-list-subscription-guide).
* Answer questions on [issues](https://github.com/apache/incubator-dubbo/issues).
* Fix bugs reported on [issues](https://github.com/apache/incubator-dubbo/issues), and send us pull request.
* Review the existing [pull request](https://github.com/apache/incubator-dubbo/pulls).
* Improve the [website](https://github.com/apache/incubator-dubbo-website), typically we need
  * blog post
  * translation on documentation
  * use cases about how Dubbo is being used in enterprise system.
* Improve the [dubbo-ops/dubbo-monitor](https://github.com/apache/incubator-dubbo-ops).
* Contribute to the projects listed in [ecosystem](https://github.com/dubbo).
* Any form of contribution that is not mentioned above.
* If you would like to contribute, please send an email to dev@dubbo.incubator.apache.org to let us know!

## Reporting bugs

Please follow the [template](https://github.com/apache/incubator-dubbo/issues/new?template=dubbo-issue-report-template.md) for reporting any issues.

## Reporting a security vulnerability

Please report security vulnerability to security@dubbo.incubator.apache.org (private mailing list).

## [Ecosystem](https://github.com/dubbo)

* [Dubbo Website](https://github.com/apache/incubator-dubbo-website) - Apache Dubbo (incubating) official website
* [Dubbo Samples](https://github.com/dubbo/dubbo-samples) - samples for Apache Dubbo (incubating)
* [Dubbo Spring Boot](https://github.com/apache/incubator-dubbo-spring-boot-project) - Spring Boot Project for Dubbo
* [Dubbo OPS](https://github.com/apache/incubator-dubbo-ops) - The reference implementation for dubbo ops (dubbo-admin, dubbo-monitor, dubbo-registry-simple, etc.)

#### Language

* [Node.js](https://github.com/dubbo/dubbo2.js)
* [Python](https://github.com/dubbo/dubbo-client-py)
* [Php](https://github.com/dubbo/dubbo-php-framework)

## License

Apache Dubbo is under the Apache 2.0 license. See the [LICENSE](https://github.com/apache/incubator-dubbo/blob/master/LICENSE) file for details.
