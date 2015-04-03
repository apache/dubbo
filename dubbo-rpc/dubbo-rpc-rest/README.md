# 在Dubbo中开发REST风格的远程调用（RESTful Remoting）

**作者：沈理**

**文档版权：[Creative Commons 3.0许可证 署名-禁止演绎](https://creativecommons.org/licenses/by-nd/3.0/deed.zh)**

完善中……

> 本文篇幅较长，因为REST本身涉及面较多。另外，本文参照Spring等的文档风格，不仅仅局限于框架用法的阐述，同时也努力呈现框架的设计理念和优良应用的架构思想。

> 对于想粗略了解dubbo和REST的人，只需浏览 `概述` 至 `标准Java REST API：JAX-RS简介` 几节即可。

TODO 生成可点击的目录

## 目录

* 概述
* REST的优点
* 应用场景
* 快速入门
* 标准Java REST API：JAX-RS简介
* REST服务提供端详解
    * HTTP POST/GET的实现
    * Annotation放在接口类还是实现类
    * JSON、XML等多数据格式的支持
    * 中文字符支持
    * XML数据格式的额外要求
    * 定制序列化
    * 配置REST Server的实现
    * 获取上下文（Context）信息
    * 配置端口号和Context Path	
    * 配置线程数和IO线程数	
    * 配置长连接	
    * 配置最大的HTTP连接数
    * 配置每个消费端的超时时间和HTTP连接数	
    * GZIP数据压缩	
    * 用Annotation取代部分Spring XML配置	
    * 添加自定义的Filter、Interceptor等
    * 添加自定义的Exception处理	
    * 配置HTTP日志输出
    * 输入参数的校验
    * 是否应该透明发布REST服务		
* REST服务消费端详解	
    * 场景1：非dubbo的消费端调用dubbo的REST服务	
    * 场景2：dubbo消费端调用dubbo的REST服务	
    * 场景3：dubbo的消费端调用非dubbo的REST服务	
* Dubbo中JAX-RS的限制	
* REST常见问题解答（REST FAQ）
    * Dubbo REST的服务能和Dubbo注册中心、监控中心集成吗？
    * Dubbo REST中如何实现负载均衡和容错（failover）？
    * JAX-RS中重载的方法能够映射到同一URL地址吗？
    * JAX-RS中作POST的方法能够接收多个参数吗？
* REST最佳实践	
* 性能基准测试	
    * 测试环境	
    * 测试脚本	
    * 测试结果
* 扩展讨论
    * REST与Thrift、Protobuf等的对比	
    * REST与传统WebServices的对比	
    * JAX-RS与Spring MVC的对比	

## 概述

dubbo支持多种远程调用方式，例如dubbo RPC（二进制序列化 + tcp协议）、http invoker（二进制序列化 + http协议，至少在开源版本没发现对文本序列化的支持）、hessian（二进制序列化 + http协议）、WebServices （文本序列化 + http协议）等等，但缺乏对当今特别流行的REST风格远程调用（文本序列化 + http协议）的支持。

有鉴于此，我们基于标准的Java REST API——JAX-RS 2.0（Java API for RESTful Web Services的简写），为dubbo提供了接近透明的REST调用支持。由于完全兼容Java标准API，所以为dubbo开发的所有REST服务，未来脱离dubbo或者任何特定的REST底层实现一般也可以正常运行。

特别值得指出的是，我们并不需要完全严格遵守REST的原始定义和架构风格。即使著名的Twitter REST API也会根据情况做适度调整，而不是机械的遵守原始的REST风格。

> 附注：我们将这个功能称之为REST风格的远程调用，即RESTful Remoting（抽象的远程处理或者调用），而不是叫RESTful RPC（具体的远程“过程”调用），是因为REST和RPC本身可以被认为是两种不同的风格。在dubbo的REST实现中，可以说有两个面向，其一是提供或消费正常的REST服务，其二是将REST作为dubbo RPC体系中一种协议实现，而RESTful Remoting同时涵盖了这个面向。

## REST的优点

以下摘自维基百科：

* 可更高效利用缓存来提高响应速度
* 通讯本身的无状态性可以让不同的服务器的处理一系列请求中的不同请求，提高服务器的扩展性
* 浏览器即可作为客户端，简化软件需求
* 相对于其他叠加在HTTP协议之上的机制，REST的软件依赖性更小
* 不需要额外的资源发现机制
* 在软件技术演进中的长期的兼容性更好

这里我还想特别补充REST的显著优点：基于简单的文本格式消息和通用的HTTP协议，使它具备极广的适用性，几乎所有语言和平台都对它提供支持，同时其学习和使用的门槛也较低。

## 应用场景

正是由于REST在适用性方面的优点，所以在dubbo中支持REST，可以为当今多数主流的远程调用场景都带来（显著）好处：
 
1. 显著简化企业内部的异构系统之间的（跨语言）调用。此处主要针对这种场景：dubbo的系统做服务提供端，其他语言的系统（也包括某些不基于dubbo的java系统）做服务消费端，两者通过HTTP和文本消息进行通信。即使相比Thrift、ProtoBuf等二进制跨语言调用方案，REST也有自己独特的优势（详见后面讨论）

2. 显著简化对外Open API（开放平台）的开发。既可以用dubbo来开发专门的Open API应用，也可以将原内部使用的dubbo service直接“透明”发布为对外的Open REST API（当然dubbo本身未来最好可以较透明的提供诸如权限控制、频次控制、计费等诸多功能）

3. 显著简化手机（平板）APP或者PC桌面客户端开发。类似于2，既可以用dubbo来开发专门针对无线或者桌面的服务器端，也可以将原内部使用的dubbo service直接”透明“的暴露给手机APP或桌面程序。当然在有些项目中，手机或桌面程序也可以直接访问以上场景2中所述的Open API。

4. 显著简化浏览器AJAX应用的开发。类似于2，既可以用dubbo来开发专门的AJAX服务器端，也可以将原内部使用的dubbo service直接”透明“的暴露给浏览器中JavaScript。当然，很多AJAX应用更适合与web框架协同工作，所以直接访问dubbo service在很多web项目中未必是一种非常优雅的架构。

5. 为企业内部的dubbo系统之间（即服务提供端和消费端都是基于dubbo的系统）提供一种基于文本的、易读的远程调用方式。

6. 一定程度简化dubbo系统对其它异构系统的调用。可以用类似dubbo的简便方式“透明”的调用非dubbo系统提供的REST服务（不管服务提供端是在企业内部还是外部）

需要指出的是，我认为1～3是dubbo的REST调用最有价值的三种应用场景，并且我们为dubbo添加REST调用，其最主要到目的也是面向服务的提供端，即开发REST服务来提供给非dubbo的（异构）消费端。

归纳起来，所有应用场景如下图所示：
![no image found](images/rest.jpg)

借用Java过去最流行的宣传语，为dubbo添加REST调用后，可以实现服务的”一次编写，到处访问“，理论上可以面向全世界开放，从而真正实现比较理想化的面向服务架构（SOA）。

当然，传统的WebServices（WSDL/SOAP）也基本同样能满足以上场景（除了场景4）的要求（甚至还能满足那些需要企业级特性的场景），但由于其复杂性等问题，现在已经越来越少被实际采用了。

## 快速入门

在dubbo中开发一个REST风格的服务会比较简单，下面以一个注册用户的简单服务为例说明。

这个服务要实现的功能是提供如下URL（注：这个URL不是完全符合REST的风格，但是更简单实用）：

```
http://localhost:8080/users/register
```

而任何客户端都可以将包含用户信息的JSON字符串POST到以上URL来完成用户注册。

首先，开发服务的接口：

```java
public class UserService {    
   void registerUser(User user);
}
```

然后，开发服务的实现：

```java
@Path("users")
public class UserServiceImpl implements UserService {
       
    @POST
    @Path("register")
    @Consumes({MediaType.APPLICATION_JSON})
    public void registerUser(User user) {
        // save the user...
    }
}
```
上面的服务实现代码非常简单，但是由于REST服务是要被发布到特定HTTP URL，供任意语言客户端甚至浏览器来访问，所以这里要额外添加了几个JAX-RS的标准annotation来做相关的配置：

@Path("users")：指定访问UserService的URL相对路径是/users，即http://localhost:8080/users

@Path("register")：指定访问registerUser()方法的URL相对路径是/register，再结合上一个@Path为UserService指定的路径，则调用UserService.register()的完整路径为http://localhost:8080/users/register

@POST：指定访问registerUser()用HTTP POST方法

@Consumes({MediaType.APPLICATION_JSON})：指定registerUser()接收JSON格式的数据。REST框架会自动将JSON数据反序列化为User对象

最后，在spring配置文件中添加此服务，即完成所有服务开发工作：

 ```xml
<!-- 用rest协议在8080端口暴露服务 -->
<dubbo:protocol name="rest" port="8080"/>
 
<!-- 声明需要暴露的服务接口 -->
<dubbo:service interface="xxx.UserService" ref="userService"/>
 
<!-- 和本地bean一样实现服务 -->
<bean id="userService" class="xxx.UserServiceImpl" />
``` 

## 标准Java REST API：JAX-RS简介

JAX-RS是标准的Java REST API，得到了业界的广泛支持和应用，其著名的开源实现就有很多，包括Oracle的Jersey，RedHat的RestEasy，Apache的CXF和Wink，以及restlet等等。另外，所有支持JavaEE 6.0以上规范的商用JavaEE应用服务器都对JAX-RS提供了支持。因此，JAX-RS是一种已经非常成熟的解决方案，并且采用它没有任何所谓vendor lock-in的问题。

JAX-RS在网上的资料非常丰富，例如下面的入门教程：

* Oracle官方的tutorial：http://docs.oracle.com/javaee/7/tutorial/doc/jaxrs.htm
* IBM developerWorks中国站文章：http://www.ibm.com/developerworks/cn/java/j-lo-jaxrs/

更多的资料请自行google或者百度一下。就学习JAX-RS来说，一般主要掌握其各种annotation的用法即可。

> 注意：dubbo是基于JAX-RS 2.0版本的，有时候需要注意一下资料或REST实现所涉及的版本。

## REST服务提供端详解

下面我们扩充“快速入门”中的UserService，进一步展示在dubbo中REST服务提供端的开发要点。

### HTTP POST/GET的实现

REST服务中虽然建议使用HTTP协议中四种标准方法POST、DELETE、PUT、GET来分别实现常见的“增删改查”，但实际中，我们一般情况直接用POST来实现“增改”，GET来实现“删查”即可（DELETE和PUT甚至会被一些防火墙阻挡）。

前面已经简单演示了POST的实现，在此，我们为UserService添加一个获取注册用户资料的功能，来演示GET的实现。

这个功能就是要实现客户端通过访问如下不同URL来获取不同ID的用户资料：

```
http://localhost:8080/users/1001
http://localhost:8080/users/1002
http://localhost:8080/users/1003
```

当然，也可以通过其他形式的URL来访问不同ID的用户资料，例如：

```
http://localhost:8080/users/load?id=1001
```

JAX-RS本身可以支持所有这些形式。但是上面那种在URL路径中包含查询参数的形式（http://localhost:8080/users/1001） 更符合REST的一般习惯，所以更推荐大家来使用。下面我们就为UserService添加一个getUser()方法来实现这种形式的URL访问：

```java
@GET
@Path("{id : \\d+}")
@Produces({MediaType.APPLICATION_JSON})
public User getUser(@PathParam("id") Long id) {
    // ...
}
```

@GET：指定用HTTP GET方法访问

@Path("{id : \\d+}")：根据上面的功能需求，访问getUser()的URL应当是“http://localhost:8080/users/ + 任意数字"，并且这个数字要被做为参数传入getUser()方法。 这里的annotation配置中，@Path中间的{id: xxx}指定URL相对路径中包含了名为id参数，而它的值也将被自动传递给下面用@PathParam("id")修饰的方法参数id。{id:后面紧跟的\\d+是一个正则表达式，指定了id参数必须是数字。

@Produces({MediaType.APPLICATION_JSON})：指定getUser()输出JSON格式的数据。框架会自动将User对象序列化为JSON数据。

### Annotation放在接口类还是实现类

在Dubbo中开发REST服务主要都是通过JAX-RS的annotation来完成配置的，在上面的示例中，我们都是将annotation放在服务的实现类中。但其实，我们完全也可以将annotation放到服务的接口上，这两种方式是完全等价的，例如：

```java
@Path("users")
public interface UserService {
    
    @GET
    @Path("{id : \\d+}")
    @Produces({MediaType.APPLICATION_JSON})
    User getUser(@PathParam("id") Long id);
}
```

在一般应用中，我们建议将annotation放到服务实现类，这样annotation和java实现代码位置更接近，更便于开发和维护。另外更重要的是，我们一般倾向于避免对接口的污染，保持接口的纯净性和广泛适用性。

但是，如后文所述，如果我们要用dubbo直接开发的消费端来访问此服务，则annotation必须放到接口上。

如果接口和实现类都同时添加了annotation，则实现类的annotation配置会生效，接口上的annotation被直接忽略。

### JSON、XML等多数据格式的支持

在dubbo中开发的REST服务可以同时支持传输多种格式的数据，以给客户端提供最大的灵活性。其中我们目前对最常用的JSON和XML格式特别添加了额外的功能。

比如，我们要让上例中的getUser()方法支持分别返回JSON和XML格式的数据，只需要在annotation中同时包含两种格式即可：

```java
@Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
User getUser(@PathParam("id") Long id);
```
	
或者也可以直接用字符串（还支持通配符）表示MediaType：	

```java
@Produces({"application/json", "text/xml"})
User getUser(@PathParam("id") Long id);
```

如果所有方法都支持同样类型的输入输出数据格式，则我们无需在每个方法上做配置，只需要在服务类上添加annotation即可：

```java
@Path("users")
@Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
public class UserServiceImpl implements UserService {
    // ...
}

```

在一个REST服务同时对多种数据格式支持的情况下，根据JAX-RS标准，一般是通过HTTP中的MIME header（content-type和accept）来指定当前想用的是哪种格式的数据。

但是在dubbo中，我们还自动支持目前业界普遍使用的方式，即用一个URL后缀（.json和.xml）来指定想用的数据格式。例如，在添加上述annotation后，直接访问http://localhost:8888/users/1001.json则表示用json格式，直接访问http://localhost:8888/users/1002.xml则表示用xml格式，比用HTTP Header更简单直观。Twitter、微博等的REST API都是采用这种方式。

如果你既不加HTTP header，也不加后缀，则dubbo的REST会优先启用在以上annotation定义中排位最靠前的那种数据格式。

> 注意：这里要支持XML格式数据，在annotation中既可以用MediaType.TEXT_XML，也可以用MediaType.APPLICATION_XML，但是TEXT_XML是更常用的，并且如果要利用上述的URL后缀方式来指定数据格式，只能配置为TEXT_XML才能生效。

### 中文字符支持

为了在dubbo REST中正常输出中文字符，和通常的Java web应用一样，我们需要将HTTP响应的contentType设置为UTF-8编码。

基于JAX-RS的标准用法，我们只需要做如下annotation配置即可：

```java
@Produces({"application/json; charset=UTF-8", "text/xml; charset=UTF-8"})
User getUser(@PathParam("id") Long id);
```

为了方便用户，我们在dubbo REST中直接添加了一个支持类，来定义以上的常量，可以直接使用，减少出错的可能性。

```java
@Produces({ContentType.APPLICATION_JSON_UTF_8, ContentType.TEXT_XML_UTF_8})
User getUser(@PathParam("id") Long id);
```

### XML数据格式的额外要求

由于JAX-RS的实现一般都用标准的JAXB（Java API for XML Binding）来序列化和反序列化XML格式数据，所以我们需要为每一个要用XML传输的对象添加一个类级别的JAXB annotation，否则序列化将报错。例如为getUser()中返回的User添加如下：

```java
@XmlRootElement
public class User implements Serializable {
    // ...
}
```	

此外，如果service方法中的返回值是Java的 primitive类型（如int，long，float，double等），最好为它们添加一层wrapper对象，因为JAXB不能直接序列化primitive类型。

例如，我们想让前述的registerUser()方法返回服务器端为用户生成的ID号：

```java
long registerUser(User user);
```
	
由于primitive类型不被JAXB序列化支持，所以添加一个wrapper对象：

```java
@XmlRootElement
public class RegistrationResult implements Serializable {
    
    private Long id;
    
    public RegistrationResult() {
    }
    
    public RegistrationResult(Long id) {
        this.id = id;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
}
```

并修改service方法：

```java
RegistrationResult registerUser(User user);
```

这样不但能够解决XML序列化的问题，而且使得返回的数据都符合XML和JSON的规范。例如，在JSON中，返回的将是如下形式：

```javascript
{"id": 1001}
```

如果不加wrapper，JSON返回值将直接是

```
1001 	
```

而在XML中，加wrapper后返回值将是：

```xml
<registrationResult>
    <id>1002</id>
</registrationResult>
```
	
这种wrapper对象其实利用所谓Data Transfer Object（DTO）模式，采用DTO还能对传输数据做更多有用的定制。	
	
### 定制序列化

如上所述，REST的底层实现会在service的对象和JSON/XML数据格式之间自动做序列化/反序列化。但有些场景下，如果觉得这种自动转换不满足要求，可以对其做定制。

Dubbo中的REST实现是用JAXB做XML序列化，用Jackson做JSON序列化，所以在对象上添加JAXB或Jackson的annotation即可以定制映射。

例如，定制对象属性映射到XML元素的名字：

```java
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class User implements Serializable {
    
    @XmlElement(name="username") 
    private String name;  
}
```

定制对象属性映射到JSON字段的名字：

```java
public class User implements Serializable {
    
    @JsonProperty("username")
    private String name;
}
```

更多资料请参考JAXB和Jackson的官方文档，或自行google。

### 配置REST Server的实现

目前在dubbo中，我们支持5种嵌入式rest server的实现，并同时支持采用外部应用服务器来做rest server的实现。rest server的实现是通过如下server这个XML属性来选择的：

```xml
<dubbo:protocol name="rest" server="jetty"/>
```

以上配置选用了嵌入式的jetty来做rest server，同时，如果不配置server属性，rest协议默认也是选用jetty。jetty是非常成熟的java servlet容器，并和dubbo已经有较好的集成（目前5种嵌入式server中只有jetty和后面所述的tomcat、tjws，与dubbo监控系统等完成了无缝的集成），所以，如果你的dubbo系统是单独启动的进程，你可以直接默认采用jetty即可。


```xml
<dubbo:protocol name="rest" server="tomcat"/>
```

以上配置选用了嵌入式的tomcat来做rest server。在嵌入式tomcat上，REST的性能比jetty上要好得多（参见后面的基准测试），建议在需要高性能的场景下采用tomcat。

```xml
<dubbo:protocol name="rest" server="netty"/>
```
	
以上配置选用嵌入式的netty来做rest server。（TODO more contents to add）

```xml
<dubbo:protocol name="rest" server="tjws"/> (tjws is now deprecated)
<dubbo:protocol name="rest" server="sunhttp"/>
```

以上配置选用嵌入式的tjws或Sun HTTP server来做rest server。这两个server实现非常轻量级，非常方便在集成测试中快速启动使用，当然也可以在负荷不高的生产环境中使用。	注：tjws目前已经被deprecated掉了，因为它不能很好的和servlet 3.1 API工作。

如果你的dubbo系统不是单独启动的进程，而是部署到了Java应用服务器中，则建议你采用以下配置：

```xml
<dubbo:protocol name="rest" server="servlet"/>
```
	
通过将server设置为servlet，dubbo将采用外部应用服务器的servlet容器来做rest server。同时，还要在dubbo系统的web.xml中添加如下配置：

```xml
<web-app>
    <context-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>/WEB-INF/classes/META-INF/spring/dubbo-demo-provider.xml</param-value>
    </context-param>
    
    <listener>
        <listener-class>com.alibaba.dubbo.remoting.http.servlet.BootstrapListener</listener-class>
    </listener>
    
    <listener>
        <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
    </listener>
    
    <servlet>
        <servlet-name>dispatcher</servlet-name>
        <servlet-class>com.alibaba.dubbo.remoting.http.servlet.DispatcherServlet</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>
    
    <servlet-mapping>
        <servlet-name>dispatcher</servlet-name>
        <url-pattern>/*</url-pattern>
    </servlet-mapping>
</web-app>
```

即必须将dubbo的BootstrapListener和DispatherServlet添加到web.xml，以完成dubbo的REST功能与外部servlet容器的集成。

> 注意：如果你是用spring的ContextLoaderListener来加载spring，则必须保证BootstrapListener配置在ContextLoaderListener之前，否则dubbo初始化会出错。

其实，这种场景下你依然可以坚持用嵌入式server，但外部应用服务器的servlet容器往往比嵌入式server更加强大（特别是如果你是部署到更健壮更可伸缩的WebLogic，WebSphere等），另外有时也便于在应用服务器做统一管理、监控等等。

### 获取上下文（Context）信息

在远程调用中，值得获取的上下文信息可能有很多种，这里特别以获取客户端IP为例。

在dubbo的REST中，我们有两种方式获取客户端IP。

第一种方式，用JAX-RS标准的@Context annotation：

```java
public User getUser(@PathParam("id") Long id, @Context HttpServletRequest request) {
    System.out.println("Client address is " + request.getRemoteAddr());
} 
```
	
用Context修饰getUser()的一个方法参数后，就可以将当前的HttpServletRequest注入进来，然后直接调用servlet api获取IP。

> 注意：这种方式只能在设置server="tjws"或者server="tomcat"或者server="jetty"或者server="servlet"的时候才能工作，因为只有这几种REST server的实现才提供了servlet容器。另外，标准的JAX-RS还支持用@Context修饰service类的一个实例字段来获取HttpServletRequest，但在dubbo中我们没有对此作出支持。

第二种方式，用dubbo中常用的RpcContext：

```java
public User getUser(@PathParam("id") Long id) {
    System.out.println("Client address is " + RpcContext.getContext().getRemoteAddressString());
} 
```

> 注意：这种方式只能在设置server="jetty"或者server="tomcat"或者server="servlet"或者server="tjws"的时候才能工作。另外，目前dubbo的RpcContext是一种比较有侵入性的用法，未来我们很可能会做出重构。

如果你想保持你的项目对JAX-RS的兼容性，未来脱离dubbo也可以运行，请选择第一种方式。如果你想要更优雅的服务接口定义，请选用第二种方式。

此外，在最新的dubbo rest中，还支持通过RpcContext来获取HttpServletRequest和HttpServletResponse，以提供更大的灵活性来方便用户实现某些复杂功能，比如在dubbo标准的filter中访问HTTP Header。用法示例如下：

```java
if (RpcContext.getContext().getRequest() != null && RpcContext.getContext().getRequest() instanceof HttpServletRequest) {
    System.out.println("Client address is " + ((HttpServletRequest) RpcContext.getContext().getRequest()).getRemoteAddr());
}

if (RpcContext.getContext().getResponse() != null && RpcContext.getContext().getResponse() instanceof HttpServletResponse) {
    System.out.println("Response object from RpcContext: " + RpcContext.getContext().getResponse());
}
```

> 注意：为了保持协议的中立性，RpcContext.getRequest()和RpcContext.getResponse()返回的仅仅是一个Object类，而且可能为null。所以，你必须自己做null和类型的检查。

> 注意：只有在设置server="jetty"或者server="tomcat"或者server="servlet"的时候，你才能通过以上方法正确的得到HttpServletRequest和HttpServletResponse，因为只有这几种server实现了servlet容器。

为了简化编程，在此你也可以用泛型的方式来直接获取特定类型的request/response：

```java
if (RpcContext.getContext().getRequest(HttpServletRequest.class) != null) {
    System.out.println("Client address is " + RpcContext.getContext().getRequest(HttpServletRequest.class).getRemoteAddr());
}

if (RpcContext.getContext().getResponse(HttpServletResponse.class) != null) {
    System.out.println("Response object from RpcContext: " + RpcContext.getContext().getResponse(HttpServletResponse.class));
}
```

如果request/response不符合指定的类型，这里也会返回null。

### 配置端口号和Context Path

dubbo中的rest协议默认将采用80端口，如果想修改端口，直接配置：

```xml
<dubbo:protocol name="rest" port="8888"/>
```

另外，如前所述，我们可以用@Path来配置单个rest服务的URL相对路径。但其实，我们还可以设置一个所有rest服务都适用的基础相对路径，即java web应用中常说的context path。

只需要添加如下contextpath属性即可：

```xml
<dubbo:protocol name="rest" port="8888" contextpath="services"/>
```
	
以前面代码为例：

```java
@Path("users")
public class UserServiceImpl implements UserService {
       
    @POST
    @Path("register")
    @Consumes({MediaType.APPLICATION_JSON})
    public void registerUser(User user) {
        // save the user...
    }	
}
```

现在registerUser()的完整访问路径为：

```
http://localhost:8888/services/users/register
```

注意：如果你是选用外部应用服务器做rest server，即配置:

```xml
<dubbo:protocol name="rest" port="8888" contextpath="services" server="servlet"/>
```

则必须保证这里设置的port、contextpath，与外部应用服务器的端口、DispatcherServlet的上下文路径（即webapp path加上servlet url pattern）保持一致。例如，对于部署为tomcat ROOT路径的应用，这里的contextpath必须与web.xml中DispacherServlet的`<url-pattern/>`完全一致：

```xml
<servlet-mapping>
     <servlet-name>dispatcher</servlet-name>
     <url-pattern>/services/*</url-pattern>
</servlet-mapping>
```

### 配置线程数和IO线程数

可以为rest服务配置线程池大小：

```xml
<dubbo:protocol name="rest" threads="500"/>
```

> 注意：目前线程池的设置只有当server="netty"或者server="jetty"或者server="tomcat"的时候才能生效。另外，如果server="servlet"，由于这时候启用的是外部应用服务器做rest server，不受dubbo控制，所以这里的线程池设置也无效。

如果是选用netty server，还可以配置Netty的IO worker线程数：

```xml
<dubbo:protocol name="rest" iothreads="5" threads="100"/>
```

### 配置长连接

Dubbo中的rest服务默认都是采用http长连接来访问，如果想切换为短连接，直接配置：

```xml
<dubbo:protocol name="rest" keepalive="false"/>
```

> 注意：这个配置目前只对server="netty"和server="tomcat"才能生效。

### 配置最大的HTTP连接数

可以配置服务器提供端所能同时接收的最大HTTP连接数，防止REST server被过多连接撑爆，以作为一种最基本的自我保护机制：

```xml
<dubbo:protocol name="rest" accepts="500" server="tomcat/>
```

> 注意：这个配置目前只对server="tomcat"才能生效。

### 配置每个消费端的超时时间和HTTP连接数

如果rest服务的消费端也是dubbo系统，可以像其他dubbo RPC机制一样，配置消费端调用此rest服务的最大超时时间以及每个消费端所能启动的最大HTTP连接数。

```xml
<dubbo:service interface="xxx" ref="xxx" protocol="rest" timeout="2000" connections="10"/>
```

当然，由于这个配置针对消费端生效的，所以也可以在消费端配置：

```xml
<dubbo:reference id="xxx" interface="xxx" timeout="2000" connections="10"/>
```

但是，通常我们建议配置在服务提供端提供此类配置。按照dubbo官方文档的说法：“Provider上尽量多配置Consumer端的属性，让Provider实现者一开始就思考Provider服务特点、服务质量的问题。”

> 注意：如果dubbo的REST服务是发布给非dubbo的客户端使用，则这里`<dubbo:service/>`上的配置完全无效，因为这种客户端不受dubbo控制。

### GZIP数据压缩

Dubbo的REST支持用GZIP压缩请求和响应的数据，以减少网络传输时间和带宽占用，但这种方式会也增加CPU开销。

TODO more contents to add

### 用Annotation取代部分Spring XML配置

以上所有的讨论都是基于dubbo在spring中的xml配置。但是，dubbo/spring本身也支持用annotation来作配置，所以我们也可以按dubbo官方文档中的步骤，把相关annotation加到REST服务的实现中，取代一些xml配置，例如：

```java
@Service(protocol = "rest")
@Path("users")
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
       
    @POST
    @Path("register")
    @Consumes({MediaType.APPLICATION_JSON})
    public void registerUser(User user) {
        // save the user
        userRepository.save(user);
    }	
}
```

annotation的配置更简单更精确，经常也更便于维护（当然现代IDE都可以在xml中支持比如类名重构，所以就这里的特定用例而言，xml的维护性也很好）。而xml对代码对侵入性更小一些，尤其有利于动态修改配置，特别是比如你要针对单个服务配置连接超时时间、每客户端最大连接数、集群策略、权重等等。另外，特别对复杂应用或者模块来说，xml提供了一个中心点来涵盖的所有组件和配置，更一目了然，一般更便于项目长时期的维护。

当然，选择哪种配置方式没有绝对的优劣，和个人的偏好也不无关系。

### 添加自定义的Filter、Interceptor等

Dubbo的REST也支持JAX-RS标准的Filter和Interceptor，以方便对REST的请求与响应过程做定制化的拦截处理。

其中，Filter主要用于访问和设置HTTP请求和响应的参数、URI等等。例如，设置HTTP响应的cache header：

```java
public class CacheControlFilter implements ContainerResponseFilter {

    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        if (req.getMethod().equals("GET")) {
            res.getHeaders().add("Cache-Control", "someValue");
        }
    }
}
```

Interceptor主要用于访问和修改输入与输出字节流，例如，手动添加GZIP压缩：

```java
public class GZIPWriterInterceptor implements WriterInterceptor {
 
    @Override
    public void aroundWriteTo(WriterInterceptorContext context)
                    throws IOException, WebApplicationException {
        OutputStream outputStream = context.getOutputStream();
        context.setOutputStream(new GZIPOutputStream(outputStream));
        context.proceed();
    }
}
```

在标准JAX-RS应用中，我们一般是为Filter和Interceptor添加@Provider annotation，然后JAX-RS runtime会自动发现并启用它们。而在dubbo中，我们是通过添加XML配置的方式来注册Filter和Interceptor：

```xml
<dubbo:protocol name="rest" port="8888" extension="xxx.TraceInterceptor, xxx.TraceFilter"/>
```

在此，我们可以将Filter、Interceptor和DynamicFuture这三种类型的对象都添加到extension属性上，多个之间用逗号分隔。（DynamicFuture是另一个接口，可以方便我们更动态的启用Filter和Interceptor，感兴趣请自行google。）

当然，dubbo自身也支持Filter的概念，但我们这里讨论的Filter和Interceptor更加接近协议实现的底层，相比dubbo的filter，可以做更底层的定制化。

> 注：这里的XML属性叫extension，而不是叫interceptor或者filter，是因为除了Interceptor和Filter，未来我们还会添加更多的扩展类型。

如果REST的消费端也是dubbo系统（参见下文的讨论），则也可以用类似方式为消费端配置Interceptor和Filter。但注意，JAX-RS中消费端的Filter和提供端的Filter是两种不同的接口。例如前面例子中服务端是ContainerResponseFilter接口，而消费端对应的是ClientResponseFilter:

```java
public class LoggingFilter implements ClientResponseFilter {
 
    public void filter(ClientRequestContext reqCtx, ClientResponseContext resCtx) throws IOException {
        System.out.println("status: " + resCtx.getStatus());
	    System.out.println("date: " + resCtx.getDate());
	    System.out.println("last-modified: " + resCtx.getLastModified());
	    System.out.println("location: " + resCtx.getLocation());
	    System.out.println("headers:");
	    for (Entry<String, List<String>> header : resCtx.getHeaders().entrySet()) {
     	    System.out.print("\t" + header.getKey() + " :");
	        for (String value : header.getValue()) {
	            System.out.print(value + ", ");
	        }
	        System.out.print("\n");
	    }
	    System.out.println("media-type: " + resCtx.getMediaType().getType());
    } 
}
```

### 添加自定义的Exception处理

Dubbo的REST也支持JAX-RS标准的ExceptionMapper，可以用来定制特定exception发生后应该返回的HTTP响应。

```java
public class CustomExceptionMapper implements ExceptionMapper<NotFoundException> {

    public Response toResponse(NotFoundException e) {     
        return Response.status(Response.Status.NOT_FOUND).entity("Oops! the requested resource is not found!").type("text/plain").build();
    }
}
```

和Interceptor、Filter类似，将其添加到XML配置文件中即可启用：

```xml
<dubbo:protocol name="rest" port="8888" extension="xxx.CustomExceptionMapper"/>
```

### 配置HTTP日志输出

Dubbo rest支持输出所有HTTP请求/响应中的header字段和body消息体。

在XML配置中添加如下自带的REST filter：

```xml
<dubbo:protocol name="rest" port="8888" extension="com.alibaba.dubbo.rpc.protocol.rest.support.LoggingFilter"/>
```

然后配置在logging配置中至少为com.alibaba.dubbo.rpc.protocol.rest.support打开INFO级别日志输出，例如，在log4j.xml中配置：

```xml
<logger name="com.alibaba.dubbo.rpc.protocol.rest.support">
    <level value="INFO"/>
    <appender-ref ref="CONSOLE"/>
</logger>
```

当然，你也可以直接在ROOT logger打开INFO级别日志输出：

```xml
<root>
	<level value="INFO" />
	<appender-ref ref="CONSOLE"/>
</root>
```

然后在日志中会有类似如下的内容输出：

```
The HTTP headers are: 
accept: application/json;charset=UTF-8
accept-encoding: gzip, deflate
connection: Keep-Alive
content-length: 22
content-type: application/json
host: 192.168.1.100:8888
user-agent: Apache-HttpClient/4.2.1 (java 1.5)
```

```
The contents of request body is: 
{"id":1,"name":"dang"}
```

打开HTTP日志输出后，除了正常日志输出的性能开销外，也会在比如HTTP请求解析时产生额外的开销，因为需要建立额外的内存缓冲区来为日志的输出做数据准备。

### 输入参数的校验

dubbo的rest支持采用Java标准的bean validation annotation（JSR 303)来做输入校验http://beanvalidation.org/

为了和其他dubbo远程调用协议保持一致，在rest中作校验的annotation必须放在服务的接口上，例如：

```java
public interface UserService {
   
    User getUser(@Min(value=1L, message="User ID must be greater than 1") Long id);
}

```

当然，在很多其他的bean validation的应用场景都是将annotation放到实现类而不是接口上。把annotation放在接口上至少有一个好处是，dubbo的客户端可以共享这个接口的信息，dubbo甚至不需要做远程调用，在本地就可以完成输入校验。

然后按照dubbo的标准方式在XML配置中打开验证：

```xml
<dubbo:service interface=xxx.UserService" ref="userService" protocol="rest" validation="true"/>
```

在dubbo的其他很多远程调用协议中，如果输入验证出错，是直接将`RpcException`抛向客户端，而在rest中由于客户端经常是非dubbo，甚至非java的系统，所以不便直接抛出Java异常。因此，目前我们将校验错误以XML的格式返回：

```xml
<violationReport>
    <constraintViolations>
        <path>getUserArgument0</path>
        <message>User ID must be greater than 1</message>
        <value>0</value>
    </constraintViolations>
</violationReport>
```

稍后也会支持其他数据格式的返回值。至于如何对验证错误消息作国际化处理，直接参考bean validation的相关文档即可。

如果你认为默认的校验错误返回格式不符合你的要求，可以如上面章节所述，添加自定义的ExceptionMapper来自由的定制错误返回格式。需要注意的是，这个ExceptionMapper必须用泛型声明来捕获dubbo的RpcException，才能成功覆盖dubbo rest默认的异常处理策略。为了简化操作，其实这里最简单的方式是直接继承dubbo rest的RpcExceptionMapper，并覆盖其中处理校验异常的方法即可：

```java
public class MyValidationExceptionMapper extends RpcExceptionMapper {

    protected Response handleConstraintViolationException(ConstraintViolationException cve) {
        ViolationReport report = new ViolationReport();
        for (ConstraintViolation cv : cve.getConstraintViolations()) {
            report.addConstraintViolation(new RestConstraintViolation(
                    cv.getPropertyPath().toString(),
                    cv.getMessage(),
                    cv.getInvalidValue() == null ? "null" : cv.getInvalidValue().toString()));
        }
        // 采用json输出代替xml输出
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(report).type(ContentType.APPLICATION_JSON_UTF_8).build();
    }
}
```

然后将这个ExceptionMapper添加到XML配置中即可：

```xml
<dubbo:protocol name="rest" port="8888" extension="xxx.MyValidationExceptionMapper"/>
```

### 是否应该透明发布REST服务

Dubbo的REST调用和dubbo中其它某些RPC不同的是，需要在服务代码中添加JAX-RS的annotation（以及JAXB、Jackson的annotation），如果你觉得这些annotation一定程度“污染”了你的服务代码，你可以考虑编写额外的Facade和DTO类，在Facade和DTO上添加annotation，而Facade将调用转发给真正的服务实现类。当然事实上，直接在服务代码中添加annotation基本没有任何负面作用，而且这本身是Java EE的标准用法，另外JAX-RS和JAXB的annotation是属于java标准，比我们经常使用的spring、dubbo等等annotation更没有vendor lock-in的问题，所以一般没有必要因此而引入额外对象。

另外，如果你想用前述的@Context annotation，通过方法参数注入HttpServletRequest（如`public User getUser(@PathParam("id") Long id, @Context HttpServletRequest request)`），这时候由于改变了服务的方法签名，并且HttpServletRequest是REST特有的参数，如果你的服务要支持多种RPC机制的话，则引入额外的Facade类是比较适当的。

当然，在没有添加REST调用之前，你的服务代码可能本身已经就充当了Facade和DTO的角色（至于为什么有些场景需要这些角色，有兴趣可参考[微观SOA：服务设计原则及其实践方式](http://www.infoq.com/cn/articles/micro-soa-1)）。这种情况下，在添加REST之后，如果你再额外添加与REST相关的Facade和DTO，就相当于对原有代码对再一次包装，即形成如下调用链：

`RestFacade/RestDTO -> Facade/DTO -> Service`

这种体系比较繁琐，数据转换之类的工作量也不小，所以一般应尽量避免如此。

## REST服务消费端详解

这里我们用三种场景来分别讨论：

1. 非dubbo的消费端调用dubbo的REST服务（non-dubbo --> dubbo）
2. dubbo消费端调用dubbo的REST服务 （dubbo --> dubbo）
3. dubbo的消费端调用非dubbo的REST服务 （dubbo --> non-dubbo）

### 场景1：非dubbo的消费端调用dubbo的REST服务

这种场景的客户端与dubbo本身无关，直接选用相应语言和框架中合适的方式即可。

如果是还是java的客户端（但没用dubbo），可以考虑直接使用标准的JAX-RS Client API或者特定REST实现的Client API来调用REST服务。下面是用JAX-RS Client API来访问上述的UserService的registerUser()：

```java
User user = new User();
user.setName("Larry");

Client client = ClientBuilder.newClient();
WebTarget target = client.target("http://localhost:8080/services/users/register.json");
Response response = target.request().post(Entity.entity(user, MediaType.APPLICATION_JSON_TYPE));

try {
    if (response.getStatus() != 200) {
        throw new RuntimeException("Failed with HTTP error code : " + response.getStatus());
    }
    System.out.println("The generated id is " + response.readEntity(RegistrationResult.class).getId());
} finally {
    response.close();
    client.close(); // 在真正开发中不要每次关闭client，比如HTTP长连接是由client持有的
}
```

上面代码片段中的User和RegistrationResult类都是消费端自己编写的，JAX-RS Client API会自动对它们做序列化/反序列化。

当然，在java中也可以直接用自己熟悉的比如HttpClient，FastJson，XStream等等各种不同技术来实现REST客户端，在此不再详述。

### 场景2：dubbo消费端调用dubbo的REST服务

这种场景下，和使用其他dubbo的远程调用方式一样，直接在服务提供端和服务消费端共享Java服务接口，并添加spring xml配置（当然也可以用spring/dubbo的annotation配置），即可透明的调用远程REST服务：

```xml
<dubbo:reference id="userService" interface="xxx.UserService"/>
```

如前所述，这种场景下必须把JAX-RS的annotation添加到服务接口上，这样在dubbo在消费端才能共享相应的REST配置信息，并据之做远程调用:

```java
@Path("users")
public interface UserService {
    
    @GET
    @Path("{id : \\d+}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    User getUser(@PathParam("id") Long id);
}
```

如果服务接口的annotation中配置了多种数据格式，这里由于两端都是dubbo系统，REST的大量细节被屏蔽了，所以不存在用前述URL后缀之类选择数据格式的可能。目前在这种情况下，排名最靠前的数据格式将直接被使用。

因此，我们建议你在定义annotation的时候最好把最合适的数据格式放到前面，比如以上我们是把json放在xml前面，因为json的传输性能优于xml。	

### 场景3：dubbo的消费端调用非dubbo的REST服务

这种场景下，可以直接用场景1中描述的Java的方式来调用REST服务。但其实也可以采用场景2中描述的方式，即更透明的调用REST服务，即使这个服务并不是dubbo提供的。

如果用场景2的方式，由于这里REST服务并非dubbo提供，一般也就没有前述的共享的Java服务接口，所以在此我们需要根据外部REST服务的情况，自己来编写Java接口以及相应参数类，并添加JAX-RS、JAXB、Jackson等的annotation，dubbo的REST底层实现会据此去自动生成请求消息，自动解析响应消息等等，从而透明的做远程调用。或者这种方式也可以理解为，我们尝试用JAX-RS的方式去仿造实现一遍外部的REST服务提供端，然后把写成服务接口放到客户端来直接使用，dubbo的REST底层实现就能像调用dubbo的REST服务一样调用其他REST服务。

例如，我们要调用如下的外部服务

```
http://api.foo.com/services/users/1001
http://api.foo.com/services/users/1002
```

获取不同ID的用户资料，返回格式是JSON

```javascript
{
    "id": 1001,
    "name": "Larry"
}
```

我们可根据这些信息，编写服务接口和参数类即可：

```java
@Path("users")
public interface UserService {
    
    @GET
    @Path("{id : \\d+}")
    @Produces({MediaType.APPLICATION_JSON})
    User getUser(@PathParam("id") Long id);
}
```

```java
public class User implements Serializable {

    private Long id;

    private String name;

    // …
}
```

对于spring中的配置，因为这里的REST服务不是dubbo提供的，所以无法使用dubbo的注册中心，直接配置外部REST服务的url地址即可（如多个地址用逗号分隔）：

```xml
<dubbo:reference id="userService" interface="xxx.UserService" url="rest://api.foo.com/services/"/>
```

> 注意：这里协议必须用rest://而不是http://之类。如果外部的REST服务有context path，则在url中也必须添加上（除非你在每个服务接口的@Path annotation中都带上context path），例如上面的/services/。同时这里的services后面必须带上/，这样才能使dubbo正常工作。

另外，这里依然可以配置客户端可启动的最大连接数和超时时间：

```xml
<dubbo:reference id="userService" interface="xxx.UserService" url="rest://api.foo.com/services/" timeout="2000" connections="10"/>
```

## Dubbo中JAX-RS的限制

Dubbo中的REST开发是完全兼容标准JAX-RS的，但其支持的功能目前是完整JAX-RS的一个子集，部分因为它要受限于dubbo和spring的特定体系。

在dubbo中使用的JAX-RS的局限包括但不限于：

1. 服务实现只能是singleton的，不能支持per-request scope和per-lookup scope
2. 不支持用@Context annotation对服务的实例字段注入 ServletConfig、ServletContext、HttpServletRequest、HttpServletResponse等等，但可以支持对服务方法参数的注入。但对某些特定REST server实现，（祥见前面的叙述），也不支持对服务方法参数的注入。

## REST常见问题解答（REST FAQ）

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

## REST最佳实践

TODO

## 性能基准测试

### 测试环境
 
粗略如下：

* 两台独立服务器
* 4核Intel(R) Xeon(R) CPU E5-2603 0 @ 1.80GHz
* 8G内存
* 服务器之间网络通过百兆交换机
* CentOS 5
* JDK 7
* Tomcat 7
* JVM参数-server -Xms1g -Xmx1g -XX:PermSize=64M -XX:+UseConcMarkSweepGC

### 测试脚本

和dubbo自身的基准测试保持接近：

10个并发客户端持续不断发出请求：

* 传入嵌套复杂对象（但单个数据量很小），不做任何处理，原样返回
* 传入50K字符串，不做任何处理，原样返回（TODO：结果尚未列出）

进行5分钟性能测试。（引用dubbo自身测试的考虑：“主要考察序列化和网络IO的性能，因此服务端无任何业务逻辑。取10并发是考虑到http协议在高并发下对CPU的使用率较高可能会先打到瓶颈。”）

### 测试结果

下面的结果主要对比的是REST和dubbo RPC两种远程调用方式，并对它们作不同的配置，例如：

* “REST: Jetty + XML + GZIP”的意思是：测试REST，并采用jetty server，XML数据格式，启用GZIP压缩。
* “Dubbo: hessian2”的意思是：测试dubbo RPC，并采用hessian2序列化方式。

针对复杂对象的结果如下（响应时间越小越好，TPS越大越好）：

| 远程调用方式 | 平均响应时间 | 平均TPS（每秒事务数） | 
| ----------- | ------------- | ------------- |
| REST: Jetty + JSON | 7.806 | 1280 |
| REST: Jetty + JSON + GZIP | TODO | TODO |
| REST: Jetty + XML | TODO | TODO |
| REST: Jetty + XML + GZIP | TODO | TODO |
| REST: Tomcat + JSON | 2.082 | 4796 |
| REST: Netty + JSON | 2.182 | 4576 |
| Dubbo: FST | 1.211 | 8244 |
| Dubbo: kyro | 1.182 | 8444 |
| Dubbo: dubbo serialization | 1.43 | 6982 |
| Dubbo: hessian2 | 1.49 | 6701 |
| Dubbo: fastjson | 1.572 | 6352 |

![no image found](images/rt.png)

![no image found](images/tps.png)


仅就目前的结果，一点简单总结：

* dubbo RPC（特别是基于高效java序列化方式如kryo，fst）比REST的响应时间和吞吐量都有较显著优势，内网的dubbo系统之间优先选择dubbo RPC。
* 在REST的实现选择上，仅就性能而言，目前tomcat7和netty最优（当然目前使用的jetty和netty版本都较低）。tjws和sun http server在性能测试中表现极差，平均响应时间超过200ms，平均tps只有50左右（为了避免影响图片效果，没在上面列出）。
* 在REST中JSON数据格式性能优于XML（数据暂未在以上列出）。
* 在REST中启用GZIP对企业内网中的小数据量复杂对象帮助不大，性能反而有下降（数据暂未在以上列出）。

## 性能优化建议

如果将dubbo REST部署到外部Tomcat上，并配置server="servlet"，即启用外部的tomcat来做为rest server的底层实现，则最好在tomcat上添加如下配置：

```xml
<Connector port="8080" protocol="org.apache.coyote.http11.Http11NioProtocol"
               connectionTimeout="20000"
               redirectPort="8443"
               minSpareThreads="20"
               enableLookups="false"
               maxThreads="100"
               maxKeepAliveRequests="-1"
               keepAliveTimeout="60000"/>
```

特别是maxKeepAliveRequests="-1"，这个配置主要是保证tomcat一直启用http长连接，以提高REST调用性能。但是请注意，如果REST消费端不是持续的调用REST服务，则一直启用长连接未必是最好的做法。另外，一直启用长连接的方式一般不适合针对普通webapp，更适合这种类似rpc的场景。所以为了高性能，在tomcat中，dubbo REST应用和普通web应用最好不要混合部署，而应该用单独的实例。

TODO more contents to add

## 扩展讨论

### REST与Thrift、Protobuf等的对比

TODO

### REST与传统WebServices的对比

TODO

### JAX-RS与Spring MVC的对比

初步看法，摘自http://www.infoq.com/cn/news/2014/10/dubbox-open-source?utm_source=infoq&utm_medium=popular_links_homepage#theCommentsSection

> 谢谢，对于jax-rs和spring mvc，其实我对spring mvc的rest支持还没有太深入的看过，说点初步想法，请大家指正：

> spring mvc也支持annotation的配置，其实和jax-rs看起来是非常非常类似的。

> 我个人认为spring mvc相对更适合于面向web应用的restful服务，比如被AJAX调用，也可能输出HTML之类的，应用中还有页面跳转流程之类，spring mvc既可以做好正常的web页面请求也可以同时处理rest请求。但总的来说这个restful服务是在展现层或者叫web层之类实现的

> 而jax-rs相对更适合纯粹的服务化应用，也就是传统Java EE中所说的中间层服务，比如它可以把传统的EJB发布成restful服务。在spring应用中，也就把spring中充当service之类的bean直接发布成restful服务。总的来说这个restful服务是在业务、应用层或者facade层。而MVC层次和概念在这种做比如（后台）服务化的应用中通常是没有多大价值的。

> 当然jax-rs的有些实现比如jersey，也试图提供mvc支持，以更好的适应上面所说的web应用，但应该是不如spring mvc。

> 在dubbo应用中，我想很多人都比较喜欢直接将一个本地的spring service bean（或者叫manager之类的）完全透明的发布成远程服务，则这里用JAX-RS是更自然更直接的，不必额外的引入MVC概念。当然，先不讨论透明发布远程服务是不是最佳实践，要不要添加facade之类。

> 当然，我知道在dubbo不支持rest的情况下，很多朋友采用的架构是spring mvc restful调用dubbo (spring) service来发布restful服务的。这种方式我觉得也非常好，只是如果不修改spring mvc并将其与dubbo深度集成，restful服务不能像dubbo中的其他远程调用协议比如webservices、dubbo rpc、hessian等等那样，享受诸多高级的服务治理的功能，比如：注册到dubbo的服务注册中心，通过dubbo监控中心监控其调用次数、TPS、响应时间之类，通过dubbo的统一的配置方式控制其比如线程池大小、最大连接数等等，通过dubbo统一方式做服务流量控制、权限控制、频次控制。另外spring mvc仅仅负责服务端，而在消费端，通常是用spring restTemplate，如果restTemplate不和dubbo集成，有可能像dubbo服务客户端那样自动或者人工干预做服务降级。如果服务端消费端都是dubbo系统，通过spring的rest交互，如果spring rest不深度整合dubbo，则不能用dubbo统一的路由分流等功能。

> 当然，其实我个人认为这些东西不必要非此即彼的。我听说spring创始人rod johnson总是爱说一句话，the customer is always right，其实与其非要探讨哪种方式更好，不如同时支持两种方式就是了，所以原来在文档中也写过计划支持spring rest annoation，只是不知道具体可行性有多高。
