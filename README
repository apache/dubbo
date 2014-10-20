Dubbox now means Dubbo eXtensions. If you know java, javax and dubbo, you know what dubbox is :)
 
Dubbox adds features like RESTful remoting, Kyro/FST serialization, etc to the popular [dubbo service framework](http://github.com/alibaba/dubbo). It’s now internally used by several projects of [dangdang.com](http://www.dangdang.com), which is one of the major e-commerce companies in China.

Contacts: shenli@dangdang.com, wangyuxuan@dangdang.com

### Dubbox当前的主要功能：

* **REST风格远程调用支持 （HTTP + JSON/XML)**：在dubbo中支持基于HTTP + JSON/XML的远程调用，以显著简化企业内部跨语言调用，同时显著简化对外Open API、无线API甚至浏览器AJAX应用等的开发。

* **基于Kryo和FST的Java高效序列化实现**：基于开源的kryo和fst（fast serialization）序列化库，为dubbo协议添加新的序列化实现，并优化调整了其序列化体系，比较显著的提高了远程调用性能。

* **基于嵌入式Tomcat的HTTP体系（To be pushed）**：基于嵌入式tomcat实现dubbo http体系（即dubbo-remoting-http），用以逐步取代旧的嵌入式jetty，可以显著的提高REST等调用的性能，并将servlet API的支持从2.5升级到3.1。（注：除了REST，dubbo中的WebServices、Hessian、HTTP Invoker等协议都基于此http体系）。

* **Spring的升级**：将dubbo中spring由2.x升级到目前最常用的3.x版本，减少版本冲突带来的麻烦

* **ZooKeeper客户端的升级**：将dubbo中的zookeeper客户端升级到最新的版本，以修正老版本中包含的bug。

* **Demo应用**：暂时将dubbo的demo应用调整并改写以主要演示REST功能和新的Java高效序列化等等。

* **修正了在JDK1.7上dubbo的部分bug**：修正了比如dubbo协议中json序列化的问题。但是还没有修正所有发现的bug。

**注：dubbox和dubbo 2.x是兼容的，没有改变dubbo的任何已有的功能和配置方式（除了升级了spring之类的版本）**

### Dubbox文档

[在Dubbo中开发REST风格的远程调用（RESTful Remoting）](http://dangdangdotcom.github.io/dubbox/rest.html)

[在Dubbo中使用高效的Java序列化（Kryo和FST）](http://dangdangdotcom.github.io/dubbox/serialization.html)

[Demo应用简单运行指南](http://dangdangdotcom.github.io/dubbox/demo.html)


