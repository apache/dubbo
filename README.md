# Apache Dubbo (incubating) Project


Apache Dubbo(孵化)是一种基于Java的高性能开源RPC框架。请访问[官方网站](http://dubbo.incubator.apache.org)以获取快速入门和文档，以及[Wiki](https://github.com/apache/incubator-dubbo/wiki)获取新闻，常见问题和发行说明。我们现在正在收集dubbo用户信息，以帮助我们更好地改进Dubbo，请参阅。亲切地帮助我们提供你的[问题＃1012：通缉：谁在使用dubbo](https://github.com/apache/incubator-dubbo/issues/1012)，谢谢:)
## Architecture

![Architecture](http://dubbo.apache.org/img/architecture.png)

## Features

* 基于透明接口的RPC
* 智能负载平衡
* 自动服务注册和发现
* 高扩展性
* 运行时流量路由
* 可视化的服务治理

## Getting started

以下代码段来自[Dubbo Samples](https://github.com/apache/incubator-dubbo-samples/tree/master/dubbo-samples-api)。在继续阅读之前，您可以克隆示例项目并进入`dubbo-samples-api`子目录。
```bash
# git clone https://github.com/apache/incubator-dubbo-samples.git
# cd incubator-dubbo-samples/dubbo-samples-api
```

There's a [README](https://github.com/apache/incubator-dubbo-samples/tree/master/dubbo-samples-api/README.md) file under `dubbo-samples-api` directory. 阅读并按照说明尝试此示例。

### Maven dependency

[master分支的版本是 2.7.x]()

```xml
<properties>
    <dubbo.version>2.7.1</dubbo.version>
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

public interface GreetingService {
    String sayHello(String name);
}
```

*See [api/GreetingService.java](https://github.com/apache/incubator-dubbo-samples/blob/master/dubbo-samples-api/src/main/java/org/apache/dubbo/samples/api/GreetingsService.java) on GitHub.*

### Implement service interface for the provider

```java
package org.apache.dubbo.samples.provider;
 
import org.apache.dubbo.samples.api.GreetingService;
 
public class GreetingServiceImpl implements GreetingService {
    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }
}
```

*See [provider/GreetingServiceImpl.java](https://github.com/apache/incubator-dubbo-samples/blob/master/dubbo-samples-api/src/main/java/org/apache/dubbo/samples/server/GreetingsServiceImpl.java) on GitHub.*

### Start service provider

```java
package org.apache.dubbo.demo.provider;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.samples.api.GreetingService;

import java.io.IOException;
 
public class Application {

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

*See [provider/Application.java](https://github.com/apache/incubator-dubbo-samples/blob/master/dubbo-samples-api/src/main/java/org/apache/dubbo/samples/server/Application.java) on GitHub.*

### Build and run the provider

```bash
# mvn clean package
# mvn -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.apache.dubbo.demo.provider.Application exec:java
```

### Call remote service in consumer

```java
package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.samples.api.GreetingService;

public class Application {
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

### Build and run the consumer

```bash
# mvn clean package
# mvn -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.apache.dubbo.demo.consumer.Application exec:java
```

The consumer will print out `Hello world` on the screen.

*See [consumer/Application.java](https://github.com/apache/incubator-dubbo-samples/blob/master/dubbo-samples-api/src/main/java/org/apache/dubbo/samples/client/Application.java) on GitHub.*

### Next steps

* [Your first Dubbo application](http://dubbo.apache.org/en-us/blog/dubbo-101.html) - 这是一个101教程，用于显示更多详细信息，使用相同的代码。
* [Dubbo user manual](http://dubbo.apache.org/en-us/docs/user/preface/background.html) - How to use Dubbo and all its features.
* [Dubbo developer guide](http://dubbo.apache.org/en-us/docs/dev/build.html) - 如何参与dubbo开发。
* [Dubbo admin manual](http://dubbo.apache.org/en-us/docs/admin/install/provider-demo.html) - How to admin and manage Dubbo services.

## Building

If you want to try out the cutting-edge features, you can built with the following commands. (Java 1.8 is required to build the master branch)

```
  mvn clean install
```

## Contact(联系)

* Mailing(邮件) list: 
  * dev list: for dev/user discussion(讨论). [subscribe(订阅)](mailto:dev-subscribe@dubbo.incubator.apache.org), [unsubscribe](mailto:dev-unsubscribe@dubbo.incubator.apache.org), [archive(档案)](https://lists.apache.org/list.html?dev@dubbo.apache.org),  [guide](https://github.com/apache/incubator-dubbo/wiki/Mailing-list-subscription-guide)
  
* Bugs: [Issues](https://github.com/apache/incubator-dubbo/issues/new?template=dubbo-issue-report-template.md)
* Gitter: [Gitter channel](https://gitter.im/alibaba/dubbo) 
* Twitter: [@ApacheDubbo](https://twitter.com/ApacheDubbo)

## Contributing

有关提交补丁和贡献工作流程的详细信息，请参阅[贡献](https://github.com/apache/incubator-dubbo/blob/master/CONTRIBUTING.md)。

### How can I contribute?

* 看一下名为[`Good first issue`](https://github.com/apache/incubator-dubbo/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue％22) 或[`Help wanted`](https://github.com/apache/incubator-dubbo/issues?q=is%3Aopen+is%3Aissue+label%3A%22help+wanted%22)的标签问题。
* 加入关于邮件列表的讨论，订阅[指南](https://github.com/apache/incubator-dubbo/wiki/Mailing-list-subscription-guide)。
* 回答有关[问题](https://github.com/apache/incubator-dubbo/issues)的问题。
* 修复[问题](https://github.com/apache/incubator-dubbo/issues)上报告的错误，并向我们发送拉取请求。
* 查看现有的[pull request](https://github.com/apache/incubator-dubbo/pulls)。
* 改进[网站](https://github.com/apache/incubator-dubbo-website)，通常我们需要
   * 博文
   * 文件翻译
   * 使用有关如何在企业系统中使用Dubbo的案例。
* 改进[dubbo-admin / dubbo-monitor](https://github.com/apache/incubator-dubbo-admin)。
* 参与[生态系统](https://github.com/dubbo)中列出的项目。
* 上述未提及的任何形式的贡献。
* 如果您想贡献，请发送电子邮件至dev@dubbo.incubator.apache.org告诉我们！
## Reporting bugs

Please follow the [template](https://github.com/apache/incubator-dubbo/issues/new?template=dubbo-issue-report-template.md) for reporting any issues.

## Reporting a security vulnerability(漏洞)

Please report security vulnerability to [us](mailto:security@dubbo.incubator.apache.org) privately.

## Dubbo ecosystem(生态系统)

* [Dubbo Ecosystem Entry](https://github.com/dubbo) - A GitHub group `dubbo` to gather all Dubbo relevant projects not appropriate in [apache](https://github.com/apache) group yet
* [Dubbo Website](https://github.com/apache/incubator-dubbo-website) - Apache Dubbo（孵化）官方网站
* [Dubbo Samples](https://github.com/apache/incubator-dubbo-samples) - samples for Apache Dubbo (incubating)
* [Dubbo Spring Boot](https://github.com/apache/incubator-dubbo-spring-boot-project) - Spring Boot Project for Dubbo
* [Dubbo Admin](https://github.com/apache/incubator-dubbo-admin) - The reference(参考) implementation(实现) for Dubbo admin

#### Language

* [Node.js](https://github.com/dubbo/dubbo2.js)
* [Python](https://github.com/dubbo/dubbo-client-py)
* [PHP](https://github.com/dubbo/dubbo-php-framework)
* [Go](https://github.com/dubbo/dubbo-go)

## License

Apache Dubbo is under the Apache 2.0 license. See the [LICENSE](https://github.com/apache/incubator-dubbo/blob/master/LICENSE) file for details.
