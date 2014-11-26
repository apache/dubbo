Dubbox now means Dubbo eXtensions. If you know java, javax and dubbo, you know what dubbox is :)

Dubbox adds features like RESTful remoting, Kyro/FST serialization, etc to the popular [dubbo service framework](http://github.com/alibaba/dubbo). It's been used by several projects of [dangdang.com](http://www.dangdang.com), which is one of the major e-commerce companies in China.

## 主要贡献者

* 沈理 [当当网](http://www.dangdang.com/) shenli@dangdang.com
* 王宇轩 [当当网](http://www.dangdang.com/) wangyuxuan@dangdang.com
* 马金凯 [韩都衣舍](http://www.handu.com/) majinkai@handu.com
* Dylan 独立开发者 dinguangx@163.com

**讨论QQ群**：305896472

## Dubbox当前的主要功能

* **支持REST风格远程调用（HTTP + JSON/XML)**：基于非常成熟的JBoss [RestEasy](http://resteasy.jboss.org/)框架，在dubbo中实现了REST风格（HTTP + JSON/XML）的远程调用，以显著简化企业内部的跨语言交互，同时显著简化企业对外的Open API、无线API甚至AJAX服务端等等的开发。事实上，这个REST调用也使得Dubbo可以对当今特别流行的“微服务”架构提供基础性支持。 另外，REST调用也达到了比较高的性能，在基准测试下，HTTP + JSON与Dubbo 2.x默认的RPC协议（即TCP + Hessian2二进制序列化）之间只有1.5倍左右的差距，详见文档中的基准测试报告。

* **支持基于Kryo和FST的Java高效序列化实现**：基于当今比较知名的[Kryo](https://github.com/EsotericSoftware/kryo)和[FST](https://github.com/RuedigerMoeller/fast-serialization)高性能序列化库，为Dubbo默认的RPC协议添加新的序列化实现，并优化调整了其序列化体系，比较显著的提高了Dubbo RPC的性能，详见文档中的基准测试报告。

* **支持基于Jackson的JSON序列化**：基于业界应用最广泛的[Jackson](http://jackson.codehaus.org/)序列化库，为Dubbo默认的RPC协议添加新的JSON序列化实现。

* **支持基于嵌入式Tomcat的HTTP remoting体系**：基于嵌入式tomcat实现dubbo的HTTP remoting体系（即dubbo-remoting-http），用以逐步取代Dubbo中旧版本的嵌入式Jetty，可以显著的提高REST等的远程调用性能，并将Servlet API的支持从2.5升级到3.1。（注：除了REST，dubbo中的WebServices、Hessian、HTTP Invoker等协议都基于这个HTTP remoting体系）。

* **升级Spring**：将dubbo中Spring由2.x升级到目前最常用的3.x版本，减少版本冲突带来的麻烦。

* **升级ZooKeeper客户端**：将dubbo中的zookeeper客户端升级到最新的版本，以修正老版本中包含的bug。

* **支持完全基于Java代码的Dubbo配置**：基于Spring的Java Config，实现完全无XML的纯Java代码方式来配置dubbo

* **调整Demo应用**：暂时将dubbo的demo应用调整并改写以主要演示REST功能、Dubbo协议的新序列化方式、基于Java代码的Spring配置等等。

* **修正了dubbo的bug** 包括配置、序列化、管理界面等等的bug。

**注：dubbox和dubbo 2.x是兼容的，没有改变dubbo的任何已有的功能和配置方式（除了升级了spring之类的版本）**

## Dubbox文档

[在Dubbo中开发REST风格的远程调用（RESTful Remoting）](http://dangdangdotcom.github.io/dubbox/rest.html)

[在Dubbo中使用高效的Java序列化（Kryo和FST）](http://dangdangdotcom.github.io/dubbox/serialization.html)

[使用JavaConfig方式配置dubbox](http://dangdangdotcom.github.io/dubbox/java-config.html)

[Dubbo Jackson序列化使用说明](http://dangdangdotcom.github.io/dubbox/jackson.html)

[Demo应用简单运行指南](http://dangdangdotcom.github.io/dubbox/demo.html)

## 版本

详见：https://github.com/dangdangdotcom/dubbox/releases

* **dubbox-2.8.0**：该版本已经在生产环境中使用，主要支持REST风格远程调用、支持Kryo和FST序列化、升级了Spring和Zookeeper客户端、调整了demo应用等等
* **dubbox-2.8.1**：主要支持基于嵌入式tomcat的http-remoting，优化了REST客户端性能，在REST中支持限制服务端接纳的最大HTTP连接数等等
* **dubbox-2.8.2**：
    * 支持REST中的HTTP logging，包括HTTP header的字段和HTTP body中的消息体，方便调试、日志纪录等等
    * 提供辅助类便于REST的中文处理
    * 改变使用`@Reference` annotation配置时的异常处理方式，即当用annotation配置时，过去dubbo在启动期间不抛出依赖服务找不到的异常，而是在具体调用时抛出NPE，这与用XML配置时的行为不一致。
    * 较大的充实了Dubbo REST的文档
* **dubbox-2.8.3**：
    * 在REST中支持dubbo统一的方式用bean validation annotation作参数校验（沈理）
    * 在RpcContext上支持获取底层协议的Request/Response（沈理）
    * 支持采用Spring的Java Config方式配置dubbo（马金凯）
    * 在Dubbo协议中支持基于Jackson的json序列化（Dylan）
    * 在Spring AOP代理过的对象上支持dubbo annotation配置（Dylan）
    * 修正Dubbo管理界面中没有consumer时出现空指针异常（马金凯）
    * 修正@Reference annotation中protocol设置不起作用的bug（沈理）
    * 修正@Reference annotation放在setter方法上即会出错的bug（Dylan）

## FAQ（暂存）

### Dubbo REST的服务能和Dubbo注册中心、监控中心集成吗？

可以的，而且是自动集成的，也就是你在dubbo中开发的所有REST服务都会自动注册到服务册中心和监控中心，可以通过它们做管理。

但是，只有当REST的消费端也是基于dubbo的时候，注册中心中的许多服务治理操作才能完全起作用。而如果消费端是非dubbo的，自然不受注册中心管理，所以其中很多操作是不会对消费端起作用的。

### Dubbo REST中如何实现负载均衡和容错（failover）？

如果dubbo REST的消费端也是dubbo的，则Dubbo REST和其他dubbo远程调用协议基本完全一样，由dubbo框架透明的在消费端做load balance、failover等等。

如果dubbo REST的消费端是非dubbo的，甚至是非java的，则最好配置服务提供端的软负载均衡机制，目前可考虑用LVS、HAProxy、 Nginx等等对HTTP请求做负载均衡。

### JAX-RS中重载的方法能够映射到同一URL地址吗？

http://stackoverflow.com/questions/17196766/can-resteasy-choose-method-based-on-query-params

### JAX-RS中作POST的方法能够接收多个参数吗？

http://stackoverflow.com/questions/5553218/jax-rs-post-multiple-objects