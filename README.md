# Dubbo序列化协议扩充
1. 新增kryo以及protostuff两个高性能的二进制序列化协议。kryo的代码为dubbox中的代码，protostuff的代码为自己实现。
2. 完成dubbo内部实现后的java、fastjson、kryo、pb、hessian2几个协议的性能benchmark test。在该测试下，pb的性能表现非常突出。压缩后的大小仅为h2的1/4，整体时间也仅为h2的一半。
3. kryo的benchmark表现和其宣传的速度差距非常大。落后于h2和pb几十倍。
4. 想使用kryo或者pb协议进行二进制传输的同学，直接按照dubbo序列化协议的用法去声明即可。如都不声明，暂时还走h2的默认协议。

# 原dubbo项目的README
Dubbo is a distributed service framework enpowers applications with service import/export capability with high performance RPC.

It's composed of three kernel parts:

Remoting: a network communication framework provides sync-over-async and request-response messaging.

Clustering: a remote procedure call abstraction with load-balancing/failover/clustering capabilities.

Registry: a service directory framework for service registration and service event publish/subscription

For more, please refer to:

    http://dubbo.io

================================================================
Quick Start
================================================================

Export remote service:

    <bean id="barService" class="com.foo.BarServiceImpl" />
	
    <dubbo:service interface="com.foo.BarService" ref="barService" />

Refer remote service:

    <dubbo:reference id="barService" interface="com.foo.BarService" />
	
    <bean id="barAction" class="com.foo.BarAction">
        <property name="barService" ref="barService" />
    </bean>

================================================================
Source Building
================================================================

0. Install the git and maven command line:

    yum install git
    or: apt-get install git

    cd ~
    wget http://www.apache.org/dist//maven/binaries/apache-maven-2.2.1-bin.tar.gz
    tar zxvf apache-maven-2.2.1-bin.tar.gz
    vi .bash_profile
       - append: export PATH=$PATH:~/apache-maven-2.2.1/bin
    source .bash_profile

1. Checkout the dubbo source code:

    cd ~
    git clone https://github.com/alibaba/dubbo.git dubbo

    git checkout master
    or: git checkout -b dubbo-2.4.0

2. Import the dubbo source code to eclipse project:

    cd ~/dubbo
    mvn eclipse:eclipse
    Eclipse -> Menu -> File -> Import -> Exsiting Projects to Workspace -> Browse -> Finish

    Context Menu -> Run As -> Java Application:
    dubbo-demo-provider/src/test/java/com.alibaba.dubbo.demo.provider.DemoProvider
    dubbo-demo-consumer/src/test/java/com.alibaba.dubbo.demo.consumer.DemoConsumer
    dubbo-monitor-simple/src/test/java/com.alibaba.dubbo.monitor.simple.SimpleMonitor
    dubbo-registry-simple/src/test/java/com.alibaba.dubbo.registry.simple.SimpleRegistry

    Edit Config:
    dubbo-demo-provider/src/test/resources/dubbo.properties
    dubbo-demo-consumer/src/test/resources/dubbo.properties
    dubbo-monitor-simple/src/test/resources/dubbo.properties
    dubbo-registry-simple/src/test/resources/dubbo.properties

3. Build the dubbo binary package:

    cd ~/dubbo
    mvn clean install -Dmaven.test.skip
    cd dubbo/target
    ls

4. Install the demo provider:

    cd ~/dubbo/dubbo-demo-provider/target
    tar zxvf dubbo-demo-provider-2.4.0-assembly.tar.gz
    cd dubbo-demo-provider-2.4.0/bin
    ./start.sh

5. Install the demo consumer:

    cd ~/dubbo/dubbo-demo-consumer/target
    tar zxvf dubbo-demo-consumer-2.4.0-assembly.tar.gz
    cd dubbo-demo-consumer-2.4.0/bin
    ./start.sh
    cd ../logs
    tail -f stdout.log

6. Install the simple monitor:

    cd ~/dubbo/dubbo-simple-monitor/target
    tar zxvf dubbo-simple-monitor-2.4.0-assembly.tar.gz
    cd dubbo-simple-monitor-2.4.0/bin
    ./start.sh
    http://127.0.0.1:8080

7. Install the simple registry:

    cd ~/dubbo/dubbo-simple-registry/target
    tar zxvf dubbo-simple-registry-2.4.0-assembly.tar.gz
    cd dubbo-simple-registry-2.4.0/bin
    ./start.sh

    cd ~/dubbo/dubbo-demo-provider/conf
    vi dubbo.properties
       - edit: dubbo.registry.adddress=dubbo://127.0.0.1:9090
    cd ../bin
    ./restart.sh

    cd ~/dubbo/dubbo-demo-consumer/conf
    vi dubbo.properties
       - edit: dubbo.registry.adddress=dubbo://127.0.0.1:9090
    cd ../bin
    ./restart.sh

    cd ~/dubbo/dubbo-simple-monitor/conf
    vi dubbo.properties
       - edit: dubbo.registry.adddress=dubbo://127.0.0.1:9090
    cd ../bin
    ./restart.sh

8. Install the zookeeper registry:

    cd ~
    wget http://www.apache.org/dist//zookeeper/zookeeper-3.3.3/zookeeper-3.3.3.tar.gz
    tar zxvf zookeeper-3.3.3.tar.gz
    cd zookeeper-3.3.3/conf
    cp zoo_sample.cfg zoo.cfg
    vi zoo.cfg
       - edit: dataDir=/home/xxx/data
    cd ../bin
    ./zkServer.sh start

    cd ~/dubbo/dubbo-demo-provider/conf
    vi dubbo.properties
       - edit: dubbo.registry.adddress=zookeeper://127.0.0.1:2181
    cd ../bin
    ./restart.sh

    cd ~/dubbo/dubbo-demo-consumer/conf
    vi dubbo.properties
       - edit: dubbo.registry.adddress=zookeeper://127.0.0.1:2181
    cd ../bin
    ./restart.sh

    cd ~/dubbo/dubbo-simple-monitor/conf
    vi dubbo.properties
       - edit: dubbo.registry.adddress=zookeeper://127.0.0.1:2181
    cd ../bin
    ./restart.sh

9. Install the redis registry:

    cd ~
    wget http://redis.googlecode.com/files/redis-2.4.8.tar.gz
    tar xzf redis-2.4.8.tar.gz
    cd redis-2.4.8
    make
    nohup ./src/redis-server redis.conf &

    cd ~/dubbo/dubbo-demo-provider/conf
    vi dubbo.properties
       - edit: dubbo.registry.adddress=redis://127.0.0.1:6379
    cd ../bin
    ./restart.sh

    cd ~/dubbo/dubbo-demo-consumer/conf
    vi dubbo.properties
       - edit: dubbo.registry.adddress=redis://127.0.0.1:6379
    cd ../bin
    ./restart.sh

    cd ~/dubbo/dubbo-simple-monitor/conf
    vi dubbo.properties
       - edit: dubbo.registry.adddress=redis://127.0.0.1:6379
    cd ../bin
    ./restart.sh

10. Install the admin console:

    cd ~/dubbo/dubbo-admin
    mvn jetty:run -Ddubbo.registry.address=zookeeper://127.0.0.1:2181
    http://root:root@127.0.0.1:8080

