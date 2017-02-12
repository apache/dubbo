
#dubbo

>1. 增加 springmvc,jersey,rest,jsonrpc,xmlrpc,avro,grpc，websocket rpc,hprose,原生thrift,jms,redis rpc.

>2. 增加none http容器,只注册服务,不导出服务.使其更好的支持springboot.

>3. 增加hystrix 熔断支持。

>4. 增加spring oauth2 客户端支持.

>5. 增加 安卓消费dubbo服务的支持.




###安装

```
mvn install -Dmaven.test.skip=true
```

###增加了3个http容器 tomcat8,jetty9,none
```
<dubbo:protocol name="springmvc" server="jetty9" port="8080" />

<dubbo:protocol name="springmvc" server="tomcat" port="8080" />

 <!-- 如果 server 值为none,只注册服务,不导出服务,由第三方提供rest服务.dubbo消费 第三方rest服务 -->
<dubbo:protocol name="springmvc" server="none" />
```
###springmvc depenecies
```
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
</dependency>

<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-core</artifactId>
</dependency>
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-hystrix</artifactId>
</dependency>
<dependency>
    <groupId>org.hibernate</groupId>
    <artifactId>hibernate-validator</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
</dependency>

<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
</dependency>

<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

```
###springmvc example
```
建议使用高版本的springmvc



接口定义,用来指定相关方法地址,请求方法/参数.
@RequestMapping("/comment")
public interface CommentService {

    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Comment add(@RequestBody Comment comment);

    @RequestMapping(value = "/{id}", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public Comment get(@PathVariable("id") Integer id);

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public Comment delete(@PathVariable("id")Integer id);

    @RequestMapping(value = "/", method = RequestMethod.PUT,consumes = MediaType.APPLICATION_JSON_VALUE)
    public Comment update(@RequestBody Comment comment);

}
```

###支持消费非dubbo提供的接口
```
<!-- 生成远程服务代理，可以像使用本地bean一样使用userService -->
 <!-- 如果已经服务中心注册了服务, 可以不用url直连,url直连的是未在服务中心注册的-->
<dubbo:reference id="userService" interface="com.vcg.UserService" protocol="springmvc" url="springmvc://提供服务的server,可以非dubbo服务端" />
```

###更好的支持springmvc
```
由于dubbo-springmvc提供的rest服务,有诸多限制.例如不能使用相关拦截器 .这时候可以由自己提供相关 springmvc 的rest服务,由dubbo消费端负责消费
只需配置提供的协议是springmvc 服务为none即可,只注册相关服务,但不导出相关服务.dubbo消费端可以通过提供的注册地址 ,即可消费自定义的rest服务.
<dubbo:protocol name="springmvc" server="none" />
```
###Avro
```
<dependency>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro-ipc</artifactId>
    <version>1.8.1</version>
</dependency>

```
###Avro example
```
<!--avro-->
<dubbo:protocol port="8084" name="avro"/>
<bean id="avroService" class="com.alibaba.dubbo.demo.provider.AvroServiceImpl"/>
<dubbo:service interface="com.alibaba.dubbo.demo.AvroService" ref="avroService" protocol="avro"/>
<!--avro-->

```


###Thrift9
```
<dependency>
    <groupId>org.apache.thrift</groupId>
    <artifactId>libthrift</artifactId>
    <version>0.9.3</version>
    <exclusions>
        <exclusion>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpcore</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
     <version>2.4.2</version>
</dependency>
```
###Thrift example
```
<!--Thrift9-->
<dubbo:protocol port="8083" name="thrift9"/>
<bean id="fooService" class="com.alibaba.dubbo.demo.provider.FooServiceImpl"/>
<dubbo:service interface="com.alibaba.dubbo.demo.FooService$Iface" ref="fooService" protocol="thrift9"/>
<!--Thrift9-->
```

###Grpc
```
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-all</artifactId>
    <version>1.0.3</version>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
    <version>2.4.2</version>
</dependency>
```

###Grpc example
```
<!--Grpc-->
<dubbo:protocol port="8082" name="grpc"/>
<bean id="helloWorldService" class="com.alibaba.dubbo.demo.provider.grpc.HelloWorldServiceImpl"/>
<dubbo:service interface="io.grpc.examples.helloworld.GreeterGrpc$Greeter" ref="helloWorldService" protocol="grpc"/>
<!--Grpc-->
```

###Jersey

```
<dependency>
    <groupId>org.glassfish.jersey.containers</groupId>
    <artifactId>jersey-container-netty-http</artifactId>
</dependency>
<dependency>
    <groupId>org.glassfish.jersey.media</groupId>
    <artifactId>jersey-media-moxy</artifactId>
</dependency>
<dependency>
    <groupId>org.glassfish.jersey.media</groupId>
    <artifactId>jersey-media-json-jackson</artifactId>
</dependency>
<dependency>
    <groupId>org.jboss.resteasy</groupId>
    <artifactId>resteasy-client</artifactId>
</dependency>

<!--Jersey-->
<dubbo:protocol port="8081" name="jersey"/>
<bean id="commentService" class="com.alibaba.dubbo.demo.provider.CommentServiceImpl"/>
<dubbo:service interface="com.alibaba.dubbo.demo.CommentService" ref="commentService" protocol="jersey"/>
<!--Jersey-->
```



###Dubbo springmvc Rest Proxy
```
/**
 * http://localhost:8080/
 * POST,PUT,DELETE
 * 调用示例
 * {
 * "jsonrpc":2.0 ,//兼容jsonrpc， 如果携带次参数 将以jsonrpc 格式返回
 * "service":"com.alibaba.dubbo.demo.DemoService",
 * "method":"sayHello", //可以以 com.alibaba.dubbo.demo.DemoService.sayHello 来省略 service
 * "group":"defaultGroup",//可以不写
 * "version":"1.0" ,//可以不写
 * "paramsType":["java.lang.String"], //可以不写 如果有方法重载必须填写
 * "params":["wuyu"]
 * }
 *
 * @param config
 * @return
 */
 
<!--DubboProxy:start-->
<!--代理 Dubbo,并转化为Rest服务 可通过http方式调用dubbo服务-->
<bean class="com.alibaba.dubbo.rpc.protocol.springmvc.proxy.ProxyServiceImpl" id="proxyService"/>

<!--如果本身是web服务,可以省略这一步.该步骤是为了初始化springmvc容器-->
<dubbo:service interface="com.alibaba.dubbo.rpc.protocol.springmvc.proxy.ProxyService" ref="proxyService"
               protocol="springmvc"/>
<!--DubboProxy:end-->

```


###RestProxy
```
/**
 * http://localhost:8080/
 * POST,PUT,DELETE
 * 调用示例
 * {
 * "jsonrpc":2.0 ,//兼容jsonrpc， 如果携带次参数 将以jsonrpc 格式返回
 * "service":"com.alibaba.dubbo.demo.DemoService",
 * "method":"sayHello", //可以以 com.alibaba.dubbo.demo.DemoService.sayHello 来省略 service
 * "group":"defaultGroup",//可以不写
 * "version":"1.0" ,//可以不写
 * "paramsType":["java.lang.String"], //可以不写 如果有方法重载必须填写
 * "params":["wuyu"]
 * }
 *
 * @param config
 * @return
 */
 
 <dependency>
     <groupId>com.alibaba</groupId>
     <artifactId>fastjson</artifactId>
 </dependency>
 <dependency>
     <groupId>com.fasterxml.jackson.core</groupId>
     <artifactId>jackson-databind</artifactId>
 </dependency>
 <dependency>
     <groupId>org.apache.httpcomponents</groupId>
     <artifactId>httpclient</artifactId>
 </dependency>
 
 
Server:

<!--代理 Dubbo,并转化为Rest服务 可通过http方式调用dubbo服务-->
<dubbo:protocol port="8087" name="restproxy" server="tomcat"/>
<bean class="com.alibaba.dubbo.rpc.protocol.proxy.ProxyServiceImpl" id="restProxy"/>

<!--发布多个注册中心,代理服务端跨注册中心调用-->
<dubbo:service interface="com.alibaba.dubbo.rpc.protocol.proxy.ProxyService" ref="restProxy" protocol="restproxy"/>

Client:
<dubbo:reference interface="com.alibaba.dubbo.rpc.protocol.proxy.ProxyService" id="proxyService"/>
DemoService demoservice = proxyService.target(DemoService.class);
```


###WebSocket
```
<dependency>
    <groupId>com.corundumstudio.socketio</groupId>
    <artifactId>netty-socketio</artifactId>
</dependency>
<dependency>
    <groupId>io.socket</groupId>
    <artifactId>socket.io-client</artifactId>
    <version>0.8.3</version>
</dependency>
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>3.5.0</version>
</dependency>
<dependency>
    <groupId>com.github.briandilley.jsonrpc4j</groupId>
    <artifactId>jsonrpc4j</artifactId>
    <version>1.2.0</version>
</dependency>
<dependency>
    <groupId>io.reactivex</groupId>
    <artifactId>rxjava</artifactId>
    <version>1.1.10</version>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
    <version>2.4.2</version>
</dependency>
```
###websocket example
```
<!--websocket-->
<dubbo:protocol port="8086" name="ws"/>
<bean id="webSocketService" class="com.alibaba.dubbo.demo.provider.WebSocketServiceImpl"/>
<dubbo:service interface="com.alibaba.dubbo.demo.WebSocketService" ref="webSocketService" protocol="ws"/>
<!--websocket-->
```

###rest
```
<!--rest:start-->
<dubbo:protocol port="8088" name="rest"/>
<bean id="restService" class="com.alibaba.dubbo.demo.provider.RestServiceImpl"/>
<dubbo:service interface="com.alibaba.dubbo.demo.RestService" ref="restService" protocol="rest"/>
<!--rest:end-->
```
###hprose
```
<!--hprose:start  hprose 支持两种模式,http,tcp.-->
<dubbo:protocol port="8089" name="hprose" server="tomcat"/>
<dubbo:protocol port="4321" name="hprose_tcp"/>
<bean id="hproseService" class="com.alibaba.dubbo.demo.provider.HproseServiceImpl"/>
<dubbo:service interface="com.alibaba.dubbo.demo.HproseService" ref="hproseService" protocol="hprose,hprose_tcp"/>
<!--hprose:end-->
```

###更好的支持springboot
```
springboot 与dubbo结合使用,由于springboot已经提供了相关的rest服务,这时候再用dubbo提供相关的rest服务显得有点多余.
但是 我们可以使用dubbo的注册中心,把相关提供服务的机器注册进去.这样dubbo消费端就可以消费springboot rest提供的相关服务了
<dubbo:protocol name="springmvc" server="none" />

spring-boot-starter-dubbo git: https://git.oschina.net/wuyu15255872976/spring-boot-starter-dubbo.git
```

###未来

1. 支持eureka,consul,etcd 达到与springcloud 相互兼容,服务发现,服务调用