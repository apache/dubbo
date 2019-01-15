# Release Notes

## 2.7.0
环境要求：需要Java 8及以上版本支持

请在[这里]()了解关于升级2.7.x版本的注意事项和兼容性问题

## New Features

- 服务治理规则增强。
  - 更丰富的服务治理规则，新增应用级别条件路由、Tag路由等
  - 治理规则与注册中心解耦，增加对Apollo等第三方专业配置中心的支持，更易于扩展
  -  新增应用级别的动态配置规则
  -  规则体使用更易读、易用YAML格式

- 外部化配置。支持读取托管在远程的集中式配置中心的dubbo.properties，实现应用配置的集中式管控。

- 更精炼的注册中心URL，进一步减轻注册中心存储和同步压力，初步实现地址和配置的职责分离。

- 新增服务元数据中心，负责存储包括服务静态化配置、服务定义（如方法签名）等数据，默认提供Zookeeper, Redis支持。此功能也是OPS实现服务测试、Mock等治理能力的基础。

- 异步编程模式增强（限定于Dubbo协议）
  - 原生CompletableFuture<T>签名接口支持
  - 服务端异步支持
  - 异步Filter链

- 新增Protobuf序列化协议扩展

- 新增ExpiringCache缓存策略扩展

## Enhancements / Bugfixes

- 负载均衡策略优化，包括ConsitentHash(#2190) 、LeastActive(#2171)、Random(#2597) 、RoundRobin(#2586) (#2650)

- 升级第三方依赖：默认通信框架为netty 4、默认ZK客户端为Curator、Jetty 9k

- 增加地址读取时对IPV6的支持(#2079)

- 性能优化，链接关闭的情况下使得Consumer快速返回  (#2185)

- 修复Jdk原生类型在kryo中的序列化问题 (#2178)

- 修复Provider端反序列化失败后，没有及早通知Consumer端的问题 (#1903)


## 升级与兼容性

此次版本发布我们遵循了保持和老版本兼容的原则，尤其是在一些可能会破坏2.7版本与低版本互操作性的问题上，我们增加了一些兼容性代码，典型如服务治理规则、Package重命名、注册URL简化等。

1. Package重命名

com.alibaba.dubbo -> org.apache.dubbo

2. 注册URL简化

3. 服务治理规则

4. 配置


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
