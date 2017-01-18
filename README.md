
#dubbo

>1. 增加 springmvc,jsonrpc,avro,grpc,jersey，原生thrift rpc组件.

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
###example

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

###更好的支持springboot
```
springboot 与dubbo结合使用,由于springboot已经提供了相关的rest服务,这时候再用dubbo提供相关的rest服务显得有点多余.
但是 我们可以使用dubbo的注册中心,把相关提供服务的机器注册进去.这样dubbo消费端就可以消费springboot rest提供的相关服务了
<dubbo:protocol name="springmvc" server="none" />

spring-boot-starter-dubbo git: https://git.oschina.net/wuyu15255872976/spring-boot-starter-dubbo.git
```

###未来

1. 支持eureka,consul,etcd 达到与springcloud 相互兼容,服务发现,服务调用