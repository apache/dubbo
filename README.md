Dubbox now means Dubbo eXtensions. If you know java, javax and dubbo, you know what dubbox is :)

Dubbox adds features like RESTful remoting, Kyro/FST serialization, etc to the popular [dubbo service framework](http://github.com/alibaba/dubbo). It’s now internally used by several projects of [dangdang.com](http://www.dangdang.com), which is one of the major e-commerce companies in China.

### Dubbox当前的主要功能：

* **支持REST风格远程调用（HTTP + JSON/XML)**：基于非常成熟的JBoss [RestEasy](http://resteasy.jboss.org/)框架，在dubbo中实现了REST风格（HTTP + JSON/XML）的远程调用，以显著简化企业内部的跨语言交互，同时显著简化企业对外的Open API、无线API甚至AJAX服务端等等的开发。事实上，这个REST调用也使得Dubbo可以对当今特别流行的“微服务”架构提供基础性支持。 另外，REST调用也达到了比较高的性能，在基准测试下，HTTP + JSON与Dubbo 2.x默认的RPC协议（即TCP + Hessian2二进制序列化）之间只有1.5倍左右的差距，详见文档中的基准测试报告。

* **支持基于Kryo和FST的Java高效序列化实现**：基于当今比较知名的[Kryo](https://github.com/EsotericSoftware/kryo)和[FST](https://github.com/RuedigerMoeller/fast-serialization)高性能序列化库，为Dubbo 默认的RPC协议添加新的序列化实现，并优化调整了其序列化体系，比较显著的提高了Dubbo RPC的性能，详见文档中的基准测试报告。

* **支持基于嵌入式Tomcat的HTTP remoting体系**：基于嵌入式tomcat实现dubbo的HTTP remoting体系（即dubbo-remoting-http），用以逐步取代Dubbo中旧版本的嵌入式Jetty，可以显著的提高REST等的远程调用性能，并将Servlet API的支持从2.5升级到3.1。（注：除了REST，dubbo中的WebServices、Hessian、HTTP Invoker等协议都基于这个HTTP remoting体系）。

* **升级Spring**：将dubbo中Spring由2.x升级到目前最常用的3.x版本，减少版本冲突带来的麻烦

* **升级ZooKeeper客户端**：将dubbo中的zookeeper客户端升级到最新的版本，以修正老版本中包含的bug。

* **调整Demo应用**：暂时将dubbo的demo应用调整并改写以主要演示REST功能和新的Java高效序列化等等。

* **修正了在JDK1.7上dubbo的部分bug**：修正了比如dubbo协议中json序列化的问题。但是还没有修正所有发现的bug。

**注：dubbox和dubbo 2.x是兼容的，没有改变dubbo的任何已有的功能和配置方式（除了升级了spring之类的版本）**

### Dubbox文档

[在Dubbo中开发REST风格的远程调用（RESTful Remoting）](http://dangdangdotcom.github.io/dubbox/rest.html)

[在Dubbo中使用高效的Java序列化（Kryo和FST）](http://dangdangdotcom.github.io/dubbox/serialization.html)

[Demo应用简单运行指南](http://dangdangdotcom.github.io/dubbox/demo.html)

### 版本

详见：https://github.com/dangdangdotcom/dubbox/releases

* **dubbox-2.8.0**：该版本已经在生产环境中使用，主要支持REST风格远程调用、支持Kryo和FST序列化、升级了Spring和Zookeeper客户端、调整了demo应用等等
* **dubbox-2.8.1**：主要支持基于嵌入式tomcat的http-remoting，优化了REST客户端性能等等（以上关于REST的文档针对2.8.1）


**联系方式**：沈理（shenli@dangdang.com）, 王宇轩（wangyuxuan@dangdang.com）
**QQ群**：305896472



