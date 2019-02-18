# Release Notes

## 2.7.0

Requirements: **Java 8+** required

Please check [here](https://github.com/apache/incubator-dubbo/blob/2.7.0-release/CHANGES.md#upgrading-and-compatibility-notifications) for notes and possible compatibility issues for upgrading from 2.6.x or lower to 2.7.0.

### New Features

- Enhancement of service governance rules.
  - Enriched Routing Rules.
    1. Conditional Routing. Supports both application-level and service-level conditions.
    2. Tag Routing. Newly introduced to better support traffic isolation, such as grey deployment.
  - Decoupling governance rules with the registry, making it easier to extend. Apollo and Zookeeper are available in this version. Nacos support is on the way...
  - Application-level Dynamic Configuration support.
  - Use YAML as the configuration language, which is more friendly to read and use.

- Externalized Configuration. Supports reading `dubbo.properties` hosted in remote centralized configuration center - centralized configuration.

- Simplified registry URL. With lower Registry memory use and less notification pressure from Service Directory, separates Configuration notification from Service Discovery.

- Metadata Center. A totally new concept since 2.7.0,  used to store service metadata including static configuration, service definition, method signature, etc.. By default, Zookeeper and Redis are supported as the backend storage. Will work as the basis of service testing, mock and other service governance features going to be supported in OPS.

- Asynchronous Programming Model (only works for Dubbo protocol now)
  - Built-in support for the method with CompletableFuture<T> signature.
  - Server-side asynchronous support, with an AsyncContext API works like Servlet 3.0.
  - Asynchronous filter chain callback.

- Serialization Extension: Protobuf.

- Caching Policy Extension: Expiring Cache.

### Enhancements / Bugfixes

- Load Balancing strategy enhancement: ConsitentHash #2190, LeastActive #2171, Random #2597, RoundRobin #2650.

- Third-party dependency upgrading.
  - Switch default remoting to Netty 4.
  - Switch default Zookeeper client to Curator.
  - Upgrade Jetty to 9.x.

- IPV6 support #2079.

- Performance tuning, check hanging requests on a closed channel, make them return directly #2185.

- Fixed the serialization problem of JDK primitive types in Kryo #2178.

- Fixed the problem of failing to notify Consumer as early as possible after the Provider side deserialization failed #1903.

### Upgrading and Compatibility Notifications

We have always keep compatibility in mind during the whole process of 2.7.0. We even want old users to upgrade with only on pom version upgrade, but it's hard to achieve that, especially when considering that we have the package renamed in this version, so we had some tradeoffs. If you only used the Dubbo's most basic features, you may have little problems of upgrading, but if you have used some advanced features or have some SPI extensions inside, you'd better read the upgrade notifications carefully. The compatibility issues can be classified into the following 5 categories, for each part, there will have detailed dos and don'ts published later in the official website.

1. Interoperability between 2.7.0 and lower versions

2. Package renaming

   com.alibaba.dubbo -> org.apache.dubbo

3. Simplification of registered URLs

4. Service Governance Rules

5. Configuration


## 2.6.5

Enhancements / Features：    

- Reactor the generation rule for @Service Bean name [#2235](https://github.com/apache/incubator-dubbo/issues/2235) 
- Introduce a new Spring ApplicationEvent for ServiceBean exporting [#2251](https://github.com/apache/incubator-dubbo/issues/2251) 
- [Enhancement] the algorithm of load issue on Windows. [#1641](https://github.com/apache/incubator-dubbo/issues/1641)
- add javadoc to dubbo-all module good first issue. [#2600](https://github.com/apache/incubator-dubbo/issues/2600) 
- [Enhancement] Reactor the generation rule for @Service Bean name type/enhancement [#2235](https://github.com/apache/incubator-dubbo/issues/2235) 
- Optimize LeastActiveLoadBalance and add weight test case. [#2540](https://github.com/apache/incubator-dubbo/issues/2540) 
- Smooth Round Robin selection. [#2578](https://github.com/apache/incubator-dubbo/issues/2578) [#2647](https://github.com/apache/incubator-dubbo/pull/2647) 
- [Enhancement] Resolve the placeholders for sub-properties. [#2297](https://github.com/apache/incubator-dubbo/issues/2297) 
- Add ability to turn off SPI auto injection, special support for generic Object type injection. [#2681](https://github.com/apache/incubator-dubbo/pull/2681)


Bugfixes：    

- @Service(register=false) is not work. [#2063](https://github.com/apache/incubator-dubbo/issues/2063) 
- Our customized serialization id exceeds the maximum limit, now it cannot work on 2.6.2 anymore. [#1903](https://github.com/apache/incubator-dubbo/issues/1903) 
- Consumer throws RpcException after RegistryDirectory notify in high QPS. [#2016](https://github.com/apache/incubator-dubbo/issues/2016)   
- Annotation @Reference can't support to export a service with a sync one and an async one . [#2194](https://github.com/apache/incubator-dubbo/issues/2194) 
- `org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor#generateReferenceBeanCacheKey` has a bug. [#2522](https://github.com/apache/incubator-dubbo/issues/2522) 
- 2.6.x Spring Event & Bugfix. [#2256](https://github.com/apache/incubator-dubbo/issues/2256) 
- Fix incorrect descriptions for dubbo-serialization module. [#2665](https://github.com/apache/incubator-dubbo/issues/2665) 
- A empty directory dubbo-config/dubbo-config-spring/src/test/resources/work after package source tgz. [#2560](https://github.com/apache/incubator-dubbo/issues/2560)
- Fixed 2.6.x branch a minor issue with doConnect not using getConnectTimeout() in NettyClient.  (*No issue*). [#2622](https://github.com/apache/incubator-dubbo/pull/2622) 
- Bean name of @service annotated class does not resolve placeholder. [#1755](https://github.com/apache/incubator-dubbo/issues/1755) 



Issues and Pull Requests, check [milestone-2.6.5](https://github.com/apache/incubator-dubbo/milestone/21).

## 2.6.4

Enhancements / Features

- Support access Redis with password, [#2146](https://github.com/apache/incubator-dubbo/pull/2146)
- Support char array for GenericService, [#2137](https://github.com/apache/incubator-dubbo/pull/2137)
- Direct return when the server goes down abnormally, [#2451](https://github.com/apache/incubator-dubbo/pull/2451)
- Add log for trouble-shooting when qos start failed, [#2455](https://github.com/apache/incubator-dubbo/pull/2455)
- PojoUtil support subclasses of java.util.Date, [#2502](https://github.com/apache/incubator-dubbo/pull/2502)
- Add ip and application name for MonitorService, [#2166](https://github.com/apache/incubator-dubbo/pull/2166)
- New ASCII logo, [#2402](https://github.com/apache/incubator-dubbo/pull/2402)

Bugfixes

- Change consumer retries default value from 0 to 2, [#2303](https://github.com/apache/incubator-dubbo/pull/2303)
- Fix the problem that attachment is lost when retry, [#2024](https://github.com/apache/incubator-dubbo/pull/2024)
- Fix NPE when telnet get a null parameter, [#2453](https://github.com/apache/incubator-dubbo/pull/2453)

UT stability

- Improve the stability by changing different port, setting timeout to 3000ms, [#2501](https://github.com/apache/incubator-dubbo/pull/2501)

Issues and Pull Requests, check [milestone-2.6.4](https://github.com/apache/incubator-dubbo/milestone/19).

## 2.6.3

Enhancements / Features

- Support implicit delivery of attachments from provider to consumer, #889
- Support inject Spring bean to SPI by bean type, #1837
- Add generic invoke and attachments support for http&hessian protocol, #1827
- Get the real methodname to support consistenthash for generic invoke, #1872
- Remove validation key from provider url on Consumer side, config depedently, #1386
- Introducing the Bootstrap module as a unified entry for Dubbo startup and resource destruction, #1820
- Open TCP_NODELAY on Netty 3, #1746
- Support specify proxy type on provider side, #1873
- Support dbindex in redis, #1831
- Upgrade tomcat to 8.5.31, #1781

Bugfixes

- ExecutionDispatcher meet with user docs, #1089
- Remove side effects of Dubbo custom loggers on Netty logger, #1717
- Fix isShutdown() judge of Dubbo biz threadpool always return true, #1426
- Selection of invoker node under the critical condition of only two nodes, #1759
- Listener cann't be removed during unsubscribe when use ZK as registry, #1792
- URL parsing problem when user filed contains '@',  #1808
- Check null in CacheFilter to avoid NPE, #1828
- Fix potential deadlock in DubboProtocol, #1836
- Restore the bug that attachment has not been updated in the RpcContext when the Dubbo built-in retry mechanism is triggered, #1453
- Some other small bugfixes

Performance Tuning

- ChannelState branch prediction optimization. #1643
- Optimize AtomicPositiveInteger, less memory and compute cost, #348
- Introduce embedded Threadlocal to replace the JDK implementation, #1745

Hessian-lite
