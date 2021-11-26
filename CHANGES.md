# Release Notes

## 2.7.6 

### Features
* Support Service Authentication https://github.com/apache/dubbo/issues/5461

### Enhancement
* Removing the internal JDK API from FileSystemDynamicConfiguration
* Refactor the APT test-cases implementation of dubbo-metadata-processor in Java 9+
* Remove feature envy
* JsonRpcProtocol support Generalization 
* Reduce object allocation for ProtocolUtils.serviceKey
* Reduce object allocation for ContextFilter.invoke

### Bugfixes
* Fixed bugs reported from 2.7.5 or lower versions, check [2.7.6 milestone](https://github.com/apache/dubbo/milestone/30) for details.

### Compatibility
1. Filter refactor, the callback method `onResponse` annotated as @Deprecated has been removed, users of lower versions that 
have extended Filter implementations and enabled Filter callbacks should be careful of this change.
2. RpcContext added some experimental APIs to support generic Object transmission.

## 2.7.5

### Features
* Support HTTP/2 through gRPC, offers all features supported by HTTP/2 and gRPC
    * Stream communication: client stream, server stream and bi-stream.
    * Reactive stream style RPC call.
    * Back pressure based on HTTP/2 flow-control mechanism.
    * TLS secure transport layer.
    * Define service using IDL
* Protobuf support for native Dubbo
    * Define service using IDL
    * Protobuf serialization
* TLS for netty4 server
* New SPI for dynamically adding extra parameters into provider URL, especially env parameters.
* **[BETA]** Brand new Service Discovery mechanism: Service Reflection - instance (application) level service discovery.
* **[BETA]** Brand new API for bootstraping Dubbo projects

### Performance Tuning
* Overall performance improved by nearly 30% compared to v2.7.3 (by QPS in certain circumstances)
* Improved consumer side thread model to avoid thread allocation and context switch, especially useful for services serving big traffic.

### Enhancement
* Load balance strategy among multiple registries:
    * Preferred
    * Same zone first
    * Weighted LB
    * The first one available
* New callback SPI for receiving address change notifications
* Refactoring of config module

### Bugfixes
check 2.7.5 milestone for details.

## 2.7.4.1

### Enhancement

* Enhance ProtobufTypeBuilder support generate type definition which contains Bytes List or Bytes Map. #5083
* Using the ID of Dubbo Config as the alias of Bean. #5094
* tag router supports anyhost. #4431
* optimize generic invoke. #4076
* dubbo zookeeper registry too slow #4828
* use consul with group and version. #4755
* qos support host config. #4720
* migrate http protocol #4781
* Some unit test optimization. #5026 #4803 #4687

### Bugfixes

* Apollo namespace optimization.  #5105
* Simplify dubbo-common transitive dependencies. #5107 
* Delete 'config.' prefix for url generated from ConfigCenterConfig. #5001
* fix set generic method error. #5079
* Add support for overriding Map properties in AbstractConfig.refresh. #4882
* Fix travis javax.ex dependency issue. (unit test)
* Fix: ExtensionLoader load duplicate filter，no log or exception. #4340 
* When the provider interrupts abnormally, the consumer cannot return quickly and still waits for the timeout to end. #4694
* Fix register config not take effect because of url simplified。 #4397
* Don't support metadata for generic service. #4641 
* Avoid resize in ClassUtils.java. #5009 
* default attribute in <dubbo:registry> doesn't work as expected. #4412
* make RegistryDirectory can refresh the invokers when providers number become 0 when using nacos registry. #4793
* Multiple @Reference annotations only have one effect #4674
* Fix RpcContext.getContext().getRemoteApplicationName() returns null #4351
* Security issue: upgrade fastjson version to 1.2.60. #5018
* nacos-registry:serviceName split error #4974
* AbstractConfig.java-getMetaData set default depend on getmethod sequence #4678
* fix protocol register set false not work. #4776 
* Fix: In Rest protocol, the limitation of Dubbo-Attachments. #4898
* The logic of org.apache.dubbo.config.MonitorConfig#isValid is incorrect #4892
* protostuff return stackoverflow and other error msg #4861
* fix method parameter bean generation. #3796 
* replace hardcode with regex pattern #4810
* Fix warm up issue when provider's timestamp is bigger than local machine's timestamp. #4870
* Fix use generic invocation via API , lost #4238 ion" value #4784
* In consumer side the app cannot catch the exception from provider that is configured serialization="kryo". #4238
* fix StringUtils#isBlank #4725
* when the interfaceName of the Reference annotation has duplicated,the exception is puzzled #4160
* when anonymity  bean is defined in spirng context，dubbo throw npe #
* add Thread ContextClassLoader #4712
* Fix judgment ipv4 address #4729
* The compilation of static methods should be excluded when generating the proxy. #4647
* check EOF of inputstream in IOUtils.write #4648


## 2.7.3

### Change List

1. Asynchronous support
    * Unified asynchronous and synchronous callback process, exception scenario triggers onError callback, #4401
    * Performance degradation caused by CompletableFuture.get() in JDK1.8 environment, #4279

2. Configuration Center
    * ConfigCenter custom namespace does not take effect, #4411
    * Unify the models implemented by several configuration centers such as Zookeeper, Nacos, and Etcd. Please refer to the description for possible incompatibility issues, #4388
    * Adjust Override Coverage Rule Center Priority: Service Level > Application Level, #4175

3. 2.6.x compatibility
    * Support Zipkin tracing feature provided by Zipkin officially, #3728, #4471
    * DubboComponentScan supports simultaneous scanning of annotations under the `com.alibaba.*` and `org.apache.*` packages, #4330

4. The Nacos Registration Center only subscribes to the address list and no longer subscribes to configuration information, #4454.

5. Support to read the environment configuration from the specified location, which can be specified by -D or OS VARIABLE. Please refer to [automatically loading environment variables](http://dubbo.apache.org/en-us/docs/user/configuration/environment-variables.html)

6. Fix consumer cannot downgrade to providers with no tags when there's no tagged providers can match, #4525

7. Some other bugfixes, #4346 #4338 #4349 #4377

### Change List

1. 异步支持相关

    - 统一异步和同步的回调流程，异常场景触发onError回调 #4401
    - CompletableFuture.get()在JDK1.8环境下带来的性能下降问题 #4279

2. 配置中心相关

    - ConfigCenter自定义namespace不生效的问题 #4411
    - 统一Zookeeper、Nacos、Etcd等几个配置中心实现的模型，可能带来的不兼容性问题请参见说明。相关修改：#4388
    - 调整Override覆盖规则中心优先级：服务级别 > 应用级别 #4175

3. 2.6.x兼容性

    - 兼容zipkin官方提供的基于Dubbo-2.6 API的集成 #3728, #4471
    - DubboComponentScan支持同时扫描 `com.alibaba.*` 和 `org.apache.*` 两个包下的注解 #4330

4. Nacos注册中心只订阅地址列表，不再订阅配置信息 #4454

5. 支持从指定位置读取环境配置，可通过-D或OS VARIABLE指定，具体请参见[使用说明](http://dubbo.apache.org/zh-cn/docs/user/configuration/environment-variables.html)

6. 标签路由在消费端使用静态打标方式时，无法实现自动降级以消费无标签提供者 #4525

7. 其他一些bugfix，#4346 #4338 #4349 #4377 

## 2.7.2

### New Features

- nacos config center / metadata center support. [#3846](https://github.com/apache/dubbo/issues/3846)
- Etcd support as config center and metadata center [#3653](https://github.com/apache/dubbo/issues/3653)
- Support Redis cluster in Metadata Report. [#3817](https://github.com/apache/dubbo/issues/3817)
- add new module for Dubbo Event. [#4096](https://github.com/apache/dubbo/issues/4096)
- Support multiple registry that including some effective registry, such as zk, redis [#3599](https://github.com/apache/dubbo/issues/3599)
- support nacos metadata [#4025](https://github.com/apache/dubbo/issues/4025)
- Dubbo support Google Protobuf generic reference [#3829](https://github.com/apache/dubbo/issues/3829)
- Merge serialization-native-hessian-for-apache-dubbo into incubator-dubbo [#3961](https://github.com/apache/dubbo/issues/3961)
- Merge rpc-native-thrift-for-apache-dubbo into incubator-dubbo [#3960](https://github.com/apache/dubbo/issues/3960)
- add socks5 proxy support [#3624](https://github.com/apache/dubbo/issues/3624)
- Integrate with SOFARegistry [#3874](https://github.com/apache/dubbo/issues/3874)
- Introduce CompletableFuture $invokeAsync for GenericService, now, for generic call, you can use:  
  $invoke for sync method call with normal return type.
  $invokeAsync for async method call with CompletableFuture<T> signature. [#3163](https://github.com/apache/dubbo/issues/3163)

### Enhancement

- Performance tuning for TimeoutTask in DefaultFuture. [#4129](https://github.com/apache/dubbo/issues/4129)
- Add a script to check dependencies license. [#3840](https://github.com/apache/dubbo/issues/3840)
- Change DynamicConfiguration definition to better adapt to Apollo's namespace storage model.[#3266](https://github.com/apache/dubbo/issues/3266)
- use equal explicit class to replace anonymous class [#4027](https://github.com/apache/dubbo/issues/4027)
- Seperate Constants.java into some SubConstants Class [#3137](https://github.com/apache/dubbo/issues/3137)
- Need to enhance DecodeableRpcResult error message [#3994](https://github.com/apache/dubbo/issues/3994)
- Provide more meaningful binary releases. [#2491](https://github.com/apache/dubbo/issues/2491)
- remove useless module-dubbo-test-integration [#3573](https://github.com/apache/dubbo/issues/3573)
- complete lookup method of consul registry and add integration test [#3890](https://github.com/apache/dubbo/issues/3890)
- Metrics Service [#3702](https://github.com/apache/dubbo/issues/3702)
- Update nacos-client to 1.0.0 [#3804](https://github.com/apache/dubbo/issues/3804)
- Fluent style builder API support [#3431](https://github.com/apache/dubbo/issues/3431)
- Update readme to remove the incubator prefix [#4159](https://github.com/apache/dubbo/issues/4159)
- update erlang link [#4100](https://github.com/apache/dubbo/issues/4100)
- optimize array code style [#4031](https://github.com/apache/dubbo/issues/4031)
- optimize some code style [#4006](https://github.com/apache/dubbo/issues/4006)
- remove useless module-dubbo-test-integration [#3989](https://github.com/apache/dubbo/issues/3989)
- optimize constant naming style [#3970](https://github.com/apache/dubbo/issues/3970)
- Use maven CI friendly versions: revision. [#3851](https://github.com/apache/dubbo/issues/3851)
- remove-parse-error-log [#3862](https://github.com/apache/dubbo/issues/3862)
- Complete xsd definition for ConfigCenterConfig. [#3854](https://github.com/apache/dubbo/issues/3854)
- add remoteApplicationName field in RpcContext [#3816](https://github.com/apache/dubbo/issues/3816)

### Bugfixes

- @Reference can't match the local @Service beans. [#4071](https://github.com/apache/dubbo/issues/4071)
- remove some illegal licence: jcip-annotations, jsr173_api. [#3790](https://github.com/apache/dubbo/issues/3790)
- Qos port can't be disabled by externalized property. [#3958](https://github.com/apache/dubbo/issues/3958)
- Fix consumer will generate wrong stackTrace. [#4137](https://github.com/apache/dubbo/issues/4137)
- nacos registry serviceName may conflict. [#4111](https://github.com/apache/dubbo/issues/4111)
- The client loses the listener when the network is reconnected. [#4115](https://github.com/apache/dubbo/issues/4115)
- fix registery urls increase forever when recreate reference proxy. [#4109](https://github.com/apache/dubbo/issues/4109)
- In dubbo 2.7.1，the watcher processor of zookeeper client throw Nullpointexception. [#3866](https://github.com/apache/dubbo/issues/3866)
- ReferenceConfig initialized not changed to false once subscribe throws exception [#4068](https://github.com/apache/dubbo/issues/4068)
- dubbo registry extension compatibility with dubbo 2.6.x. [#3882](https://github.com/apache/dubbo/issues/3882)
- Annotation mode cannot set service parameters in 2.7.0. [#3778](https://github.com/apache/dubbo/issues/3778)
- compatibility with Zipkin. [#3728](https://github.com/apache/dubbo/issues/3728)
- do local export before register any listener. [#3669](https://github.com/apache/dubbo/issues/3669)
- Cannot recognize 2.6.x compatible rules from dubbo-admin. [#4059](https://github.com/apache/dubbo/issues/4059)
- In Dubbo 2.7.0, the provider can't be configured to async [#3650](https://github.com/apache/dubbo/issues/3650)
- dubbox compatibility [#3991](https://github.com/apache/dubbo/issues/3991)
- dubbo-2.7.1 providers repeat register [#3785](https://github.com/apache/dubbo/issues/3785)
- consul registry: NullPointerException [#3923](https://github.com/apache/dubbo/issues/3923)
- cannot publish local ip address when local ip and public ip exist at the same time [#3802](https://github.com/apache/dubbo/issues/3802)
- roll back change made by 3520. [#3935](https://github.com/apache/dubbo/issues/3935)
- dubbo-registry-nacos module is not bundled into Apache Dubbo 2.7.1 [#3797](https://github.com/apache/dubbo/issues/3797)
- switch from CopyOnWriteArrayList to regular list in order to avoid potential UnsupportedOperationException [#3242](https://github.com/apache/dubbo/issues/3242)
- Serialization ContentTypeId conflict between avro protocol and protocoluff protocol [#3926](https://github.com/apache/dubbo/issues/3926)
- delay export function doesn't work. [#3952](https://github.com/apache/dubbo/issues/3952)
- org.apache.dubbo.rpc.support.MockInvoker#getInterface should not return null [#3713](https://github.com/apache/dubbo/issues/3713)
- dubbo TagRouter does not work with dubbo:parameter [#3875](https://github.com/apache/dubbo/issues/3875)
- make protocols a mutable list (a concrete ArrayList) [#3841](https://github.com/apache/dubbo/issues/3841)
- javadoc lint issue [#3646](https://github.com/apache/dubbo/issues/3646)
- The etcd3 lease should be recycled correctly [#3684](https://github.com/apache/dubbo/issues/3684)
- telnet can't work when parameter has no nullary constructor and some fields is primitive [#4007](https://github.com/apache/dubbo/issues/4007)
- Sort added router list before set the 'routers' field of the RouterChain [#3969](https://github.com/apache/dubbo/issues/3969)
- fix injvm and local call [#3638](https://github.com/apache/dubbo/issues/3638)
- spelling error in org.apache.dubbo.common.extension.AdaptiveClassCodeGenerator#generateReturnAndInovation [#3933](https://github.com/apache/dubbo/issues/3933)
- metadata report doesn't support redis with password [#3826](https://github.com/apache/dubbo/issues/3826)
- The dubbo protostuff protocol serializes the bug of java.sql.Timestamp [#3914](https://github.com/apache/dubbo/issues/3914)
- do not filter thread pool by port [#3919](https://github.com/apache/dubbo/issues/3919)
- 'dubbo-serialization-gson' maven package error [#3903](https://github.com/apache/dubbo/issues/3903)
- AbstractRegistry will be endless loop, when doSaveProperties method have no permission to save the file [#3746](https://github.com/apache/dubbo/issues/3746)
- fix fastjson serialization with generic return type [#3771](https://github.com/apache/dubbo/issues/3771)
- The dubbo-serialization -api modules should not dependency on third-party jar packages [#3762](https://github.com/apache/dubbo/issues/3762)
- when using protostuff to serialize, there is not to check whether the data is null [#3727](https://github.com/apache/dubbo/issues/3727)
- bugfix and enhancement for async [#3287](https://github.com/apache/dubbo/issues/3287)

## 2.7.1

### Notice

'zkclient' extension for 'org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter' is removed from Dubbo 2.7.1, and 'curator' extension becomes the default extension. If you happen to config your application to use 'zkclient' explicitly, pls. switch to use 'curator' instead.

### New Features

- service register support on nacos [#3582](https://github.com/apache/dubbo/issues/3582)
- support consul as registry center, config center and metadata center [#983](https://github.com/apache/dubbo/issues/983)
- service registry support/config center support on etcd [#808](https://github.com/apache/dubbo/issues/808)
- metrics support in dubbo 2.7.1 [#3598](https://github.com/apache/dubbo/issues/3598)
- @Argument @Method support [#2405](https://github.com/apache/dubbo/issues/2045)

### Enhancement

- [Enhancement] @EnableDubboConfigBinding annotates @Repeatable [#1770](https://github.com/apache/dubbo/issues/1770)
- [Enhancement] Change the default behavior of @EnableDubboConfig.multiple() [#3193](https://github.com/apache/dubbo/issues/3193)
- Should make annotation easier to use in multiple items circumstance [#3039](https://github.com/apache/dubbo/issues/3039)
- NoSuchMethodError are thrown when add custom Filter using dubbo2.6.5 and JDK1.6 and upgrade to dubbo2.7.0 [#3570](https://github.com/apache/dubbo/issues/3570)
- introduce dubbo-dependencies-zookeeper [#3607](https://github.com/apache/dubbo/pull/3607)
- Zookeeper ConfigCenter reuse the client abstraction and connection session [#3288](https://github.com/apache/dubbo/issues/3288)
- [Survey] Is it necessary to continue to maintain zkclient in dubbo project? [#3569](https://github.com/apache/dubbo/issues/3569)
- Start to use IdleStateHandler in Netty4 [#3341](https://github.com/apache/dubbo/pull/3341)
- Support multiple shared links [#2457](https://github.com/apache/dubbo/pull/2457)
- Optimize heartbeat [#3299](https://github.com/apache/dubbo/pull/3299)
- AccessLogFilter simple date format reduce instance creation [#3026](https://github.com/apache/dubbo/issues/3026)
- Support wildcard ip for tag router rule. [#3289](https://github.com/apache/dubbo/issues/3289)
- ScriptRouter should cache CompiledScript [#390](https://github.com/apache/dubbo/issues/390)
- Optimize compareTo in Router to guarantee consistent behaviour. [#3302](https://github.com/apache/dubbo/issues/3302)
- RMI protocol doesn't support generic invocation [#2779](https://github.com/apache/dubbo/issues/2779)
- a more elegant way to enhance HashedWheelTimer [#3567](https://github.com/apache/dubbo/pull/3567)
- obtain local address incorrectly sometimes in dubbo [#538](https://github.com/apache/dubbo/issues/538)
- implement pull request #3412 on master branch [#3418](https://github.com/apache/dubbo/pull/3418)
- enhancement for event of response (follow up for pull request #3043) [#3244](https://github.com/apache/dubbo/issues/3244)
- bump up hessian-lite version #3423 [#3513](https://github.com/apache/dubbo/pull/3513)
- [Dubbo-3610]make snakeyaml transitive, should we do this? [#3659](https://github.com/apache/dubbo/pull/3659)

### Bugfixes

- cannot register REST service in 2.7 due to the changes in RestProtoco#getContextPath [#3445](https://github.com/apache/dubbo/issues/3445)
- Conflict between curator client and dubbo [#3574](https://github.com/apache/dubbo/issues/3574)
- is there a problem in NettyBackedChannelBuffer.setBytes(...)? [#2619](https://github.com/apache/dubbo/issues/2619)
- [Dubbo - client always reconnect offline provider] Dubbo client bug [#3158](https://github.com/apache/dubbo/issues/3158)
- fix heartbeat internal [#3579](https://github.com/apache/dubbo/pull/3579)
- logic issue in RedisRegistry leads to services cannot be discovered. [#3291](https://github.com/apache/dubbo/pull/3291)
- Multicast demo fails with message "Can't assign requested address" [#2423](https://github.com/apache/dubbo/issues/2423)
- Fix thrift protocol, use path to locate exporter. [#3331](https://github.com/apache/dubbo/pull/3331)
- cannot use override to modify provider's configuration when hessian protocol is used [#900](https://github.com/apache/dubbo/issues/900)
- Condition is not properly used ? [#1917](https://github.com/apache/dubbo/issues/1917)
- connectionMonitor in RestProtocol seems not work [#3237](https://github.com/apache/dubbo/issues/3237)
- fail to parse config text with white space [#3367](https://github.com/apache/dubbo/issues/3367)
- @Reference check=false doesn't take effect [#195](https://github.com/apache/dubbo/issues/195)
- [Issue] SpringStatusChecker execute errors on non-XML Spring configuration [#3615](https://github.com/apache/dubbo/issues/3615)
- monitor's cluster config is set to failsafe and set to failsafe only [#274](https://github.com/apache/dubbo/issues/274)
- A question for ReferenceConfigCache. [#1293](https://github.com/apache/dubbo/issues/1293)
- referenceconfig#destroy never invoke unregister [#3294](https://github.com/apache/dubbo/issues/3294)
- Fix when qos is disable,log will print every time [#3397](https://github.com/apache/dubbo/pull/3397)
- service group is not supported in generic direct invocation [#3555](https://github.com/apache/dubbo/issues/3555)
- setOnreturn doesn't take effect in async generic invocation [#208](https://github.com/apache/dubbo/issues/208)
- Fix timeout filter not work in async way [#3174](https://github.com/apache/dubbo/pull/3174)
- java.lang.NumberFormatException: For input string: "" [#3069](https://github.com/apache/dubbo/issues/3069)
- NPE occurred when the configuration was deleted [#3533](https://github.com/apache/dubbo/issues/3533)
- NPE when package of interface is empty [#3556](https://github.com/apache/dubbo/issues/3556)
- NPE when exporting rest service using a given path. [#3477](https://github.com/apache/dubbo/issues/3477)
- NullPointerException happened when using SpringContainer.getContext() [#3476](https://github.com/apache/dubbo/issues/3476)
- Why does not tomcat throw an exception when `server.start` failed with a socket binding error.  [#3236](https://github.com/apache/dubbo/issues/3236)
- No such extension org.apache.dubbo.metadata.store.MetadataReportFactory by name redis [#3514](https://github.com/apache/dubbo/issues/3514)
- dubbo 2.7.1-SNAPSHOT NoClassDefFoundError when use springboot [#3426](https://github.com/apache/dubbo/issues/3426)
- NPE occurs when use @Reference in junit in spring boot application [#3429](https://github.com/apache/dubbo/issues/3429)
- When refer the same service with more than one @References(with different configs) on consumer side, only one take effect [#1306](https://github.com/apache/dubbo/issues/1306)
- consumer always catch java.lang.reflect.UndeclaredThrowableException for the exception thrown from provider [#3386](https://github.com/apache/dubbo/issues/3386)
- dubbo2.7.0 com.alibaba.com.caucho.hessian.io.HessianProtocolException: 'com.alibaba.dubbo.common.URL' could not be instantiated [#3342](https://github.com/apache/dubbo/issues/3342)
- Close Resources Properly [#3473](https://github.com/apache/dubbo/issues/3473)
- SPI entires dup by 3 times. [#2842](https://github.com/apache/dubbo/issues/2842)
- provider gets wrong interface name from attachment when use generic invocation in 2.6.3 [#2981](https://github.com/apache/dubbo/issues/2981)
- HashedWheelTimer's queue gets full [#3449](https://github.com/apache/dubbo/issues/3449)
- Modify MetadataReportRetry ThreadName [#3550](https://github.com/apache/dubbo/pull/3550)
- Keep interface key in the URL in simplify mode when it's different from path. [#3478](https://github.com/apache/dubbo/issues/3478)
- nc is not stable in dubbo's bootstrap script [#936](https://github.com/apache/dubbo/issues/936)

## 2.7.0

Requirements: **Java 8+** required

Please check [here](https://github.com/apache/dubbo/blob/2.7.0-release/CHANGES.md#upgrading-and-compatibility-notifications) for notes and possible compatibility issues for upgrading from 2.6.x or lower to 2.7.0.

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

- Metadata Center. A totally new concept since 2.7.0,  used to store service metadata including static configuration, service definition, method signature, etc.. By default, Zookeeper and Redis are supported as the backend storage. Will work as the basis of service testing, mock and other service governance features going to be supported in [Dubbo-Admin](https://github.com/apache/dubbo-admin).

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

## 2.6.6

Enhancement / New feature：

* tag route.  #3065 
* Use Netty4 as default Netty version. #3029 
* upporting Java 8 Date/Time type when serializing with Kryo #3519 
* supoort config telnet  #3511
* add annotation driven in MethodConfig and ArgumentConfig #2603
* add nacos-registry module #3296  
* add `protocol` attribute in `@Rerefence` #3555 
*support the hierarchical interface in @Service  #3251  
* change the default behavior in `@EnableDubboConfig.multiple()` #3193 
* inline source code of `spring-context-support` #3192 
* Simplify externalized configuration of Dubbo Protocol name  #3189 

BugFix：
 
* update hessian-lite to 2.3.5, fix unnecessary class load #3538 
* Fix unregister when client destroyed（referenceconfig#destroy) #3502 
* SPI entires dup by 3 times #3315 
* fix Consumer throws RpcException after RegistryDirectory notify in high QPS #2016 
* fix NPE in @Reference when using Junit to test dubbo service #3429 
* fix consuer always catch java.lang.reflect.UndeclaredThrowableException for any exception throws in provider  #3386 
* fix the priority of `DubboConfigConfigurationSelector ` #2897 
* fix `@Rerefence#parameters()` not work #2301 

## 2.6.5

Enhancements / Features：    

- Reactor the generation rule for @Service Bean name [#2235](https://github.com/apache/dubbo/issues/2235) 
- Introduce a new Spring ApplicationEvent for ServiceBean exporting [#2251](https://github.com/apache/dubbo/issues/2251) 
- [Enhancement] the algorithm of load issue on Windows. [#1641](https://github.com/apache/dubbo/issues/1641)
- add javadoc to dubbo-all module good first issue. [#2600](https://github.com/apache/dubbo/issues/2600) 
- [Enhancement] Reactor the generation rule for @Service Bean name type/enhancement [#2235](https://github.com/apache/dubbo/issues/2235) 
- Optimize LeastActiveLoadBalance and add weight test case. [#2540](https://github.com/apache/dubbo/issues/2540) 
- Smooth Round Robin selection. [#2578](https://github.com/apache/dubbo/issues/2578) [#2647](https://github.com/apache/dubbo/pull/2647) 
- [Enhancement] Resolve the placeholders for sub-properties. [#2297](https://github.com/apache/dubbo/issues/2297) 
- Add ability to turn off SPI auto injection, special support for generic Object type injection. [#2681](https://github.com/apache/dubbo/pull/2681)


Bugfixes：    

- @Service(register=false) is not work. [#2063](https://github.com/apache/dubbo/issues/2063) 
- Our customized serialization id exceeds the maximum limit, now it cannot work on 2.6.2 anymore. [#1903](https://github.com/apache/dubbo/issues/1903) 
- Consumer throws RpcException after RegistryDirectory notify in high QPS. [#2016](https://github.com/apache/dubbo/issues/2016)   
- Annotation @Reference can't support to export a service with a sync one and an async one . [#2194](https://github.com/apache/dubbo/issues/2194) 
- `org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor#generateReferenceBeanCacheKey` has a bug. [#2522](https://github.com/apache/dubbo/issues/2522) 
- 2.6.x Spring Event & Bugfix. [#2256](https://github.com/apache/dubbo/issues/2256) 
- Fix incorrect descriptions for dubbo-serialization module. [#2665](https://github.com/apache/dubbo/issues/2665) 
- A empty directory dubbo-config/dubbo-config-spring/src/test/resources/work after package source tgz. [#2560](https://github.com/apache/dubbo/issues/2560)
- Fixed 2.6.x branch a minor issue with doConnect not using getConnectTimeout() in NettyClient.  (*No issue*). [#2622](https://github.com/apache/dubbo/pull/2622) 
- Bean name of @service annotated class does not resolve placeholder. [#1755](https://github.com/apache/dubbo/issues/1755) 



Issues and Pull Requests, check [milestone-2.6.5](https://github.com/apache/dubbo/milestone/21).

## 2.6.4

Enhancements / Features

- Support access Redis with password, [#2146](https://github.com/apache/dubbo/pull/2146)
- Support char array for GenericService, [#2137](https://github.com/apache/dubbo/pull/2137)
- Direct return when the server goes down abnormally, [#2451](https://github.com/apache/dubbo/pull/2451)
- Add log for trouble-shooting when qos start failed, [#2455](https://github.com/apache/dubbo/pull/2455)
- PojoUtil support subclasses of java.util.Date, [#2502](https://github.com/apache/dubbo/pull/2502)
- Add ip and application name for MonitorService, [#2166](https://github.com/apache/dubbo/pull/2166)
- New ASCII logo, [#2402](https://github.com/apache/dubbo/pull/2402)

Bugfixes

- Change consumer retries default value from 0 to 2, [#2303](https://github.com/apache/dubbo/pull/2303)
- Fix the problem that attachment is lost when retry, [#2024](https://github.com/apache/dubbo/pull/2024)
- Fix NPE when telnet get a null parameter, [#2453](https://github.com/apache/dubbo/pull/2453)

UT stability

- Improve the stability by changing different port, setting timeout to 3000ms, [#2501](https://github.com/apache/dubbo/pull/2501)

Issues and Pull Requests, check [milestone-2.6.4](https://github.com/apache/dubbo/milestone/19).

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
