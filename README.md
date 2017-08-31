# ddmframework

它是一款SOA的轻量级分布式服务框架,TCP/IP长链家高性能连接
远程通讯: 提供对多种基于长连接的NIO框架抽象封装，包括多种线程模型，序列化，以及“请求-响应”模式的信息交换方式。
集群容错: 提供基于接口方法的透明远程过程调用，包括多协议支持，以及软负载均衡，失败容错，地址路由，动态配置等集群支持
自动发现: 基于注册中心目录服务，使服务消费方能动态的查找服务提供方，使地址透明，使服务提供方可以平滑增加或减少机器。

It contains three key parts, which include:

* **Remoting**: a network communication framework providing sync-over-async and request-response messaging.
* **Clustering**: a remote procedure call abstraction with load-balancing/failover/clustering capabilities.
* **Registration**: a service directory framework for service registration and service event publish/subscription

For more details, please refer to [dubbo.io](http://dubbo.io).

## Documentation

* [User's Guide](http://dubbo.io/user-guide/)
* [Developer's Guide](http://dubbo.io/developer-guide/)
* [Admin's Guide](http://dubbo.io/admin-guide/)

## Quick Start
This guide gets you started with dubbo with a simple working example.
#### Download the sources(examples)
You’ll need a local copy of the example code to work through this quickstart. Download the demo code from our [Github repository](https://github.com/alibaba/dubbo) (the following command clones the entire repository, but you just need the `dubbo-demo` for this quickstart and other tutorials):

```sh
$ cd ~
$ # Clone the repository to get the source code.
$ git clone https://github.com/alibaba/dubbo.git dubbo
$ git checkout master
$ # or: git checkout -b dubbo-2.4.x
```
#### Build & Run
1. Build the whole sources use the following maven command

```sh
$ cd ~/dubbo
$ mvn clean install -Dmaven.test.skip
$ # The demo code for this quickstart all stay in the `dubbo-demo` folder
$ cd ./dubbo-demo
$ ls
```
2. Run demo-provider. Start the provider and export service  
```sh
$ # Navigate to the provider part
$ cd ~/dubbo/demo-demo/dubbo-demo-provider/target
$ # unpack
$ tar zxvf dubbo-demo-provider-2.5.4-SNAPSHOT-assembly.tar.gz
$ cd dubbo-demo-provider-2.5.4-SNAPSHOT/bin
$ ls
```

```sh
$ # Start the provider
$ ./start.sh
```
3. Run demo-consumer. Start the consumer and consume service provided by _the provider_ above

```sh
$ # Navigate to the consumer part
$ cd ~/dubbo/demo-demo/dubbo-demo-consumer/target
$ # unpack
$ tar zxvf dubbo-demo-consumer-2.5.4-SNAPSHOT-assembly.tar.gz
$ cd dubbo-demo-consumer-2.5.4-SNAPSHOT/bin
$ ls
```

```sh
$ ./start.sh
```
For a more detailed tutorial of this demo, click [here](http://dubbo.io/#quickstart)

## Getting Help
* Community
* Releases
* Contributors
* Q&A

