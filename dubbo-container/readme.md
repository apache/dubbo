### dubbo-container 
> 容器模块：是一个 Standlone 的容器，以简单的 Main 加载 Spring 启动，因为服务通常不需要 Tomcat/JBoss 等 Web 容器的特性，没必要用 Web 容器去加载服务。

- dubbo-container-api ：定义了 com.alibaba.dubbo.container.Container 接口，并提供 加载所有容器启动的 Main 类。
实现 dubbo-container-api 
- dubbo-container-spring ，提供了 com.alibaba.dubbo.container.spring.SpringContainer 。
- dubbo-container-log4j ，提供了 com.alibaba.dubbo.container.log4j.Log4jContainer 。
- dubbo-container-logback ，提供了 com.alibaba.dubbo.container.logback.LogbackContainer 。