## dubbo-remoting

> 远程通信模块：提供通用的客户端和服务端的通讯功能

- dubbo-remoting-zookeeper ，相当于 Zookeeper Client ，和 Zookeeper Server 通信。
- dubbo-remoting-api ， 定义了 Dubbo Client 和 Dubbo Server 的接口。

### 实现 dubbo-remoting-api
- dubbo-remoting-grizzly ，基于 Grizzly 实现。
- dubbo-remoting-http ，基于 Jetty 或 Tomcat 实现。
- dubbo-remoting-mina ，基于 Mina 实现。
- dubbo-remoting-netty ，基于 Netty 3 实现。
- dubbo-remoting-netty4 ，基于 Netty 4 实现。
- dubbo-remoting-p2p ，P2P 服务器。注册中心 dubbo-registry-multicast 项目的使用该项目。

### 从最小化的角度来看，我们只需要看：
dubbo-remoting-api + dubbo-remoting-netty4
dubbo-remoting-zookeeper