openTracing filter 使用说明

openTracing 是一个规范，类似于 sl4j,但是 openTracing 是应用于分布式追踪方面

所以我们需要有自已的一个实现

1、实现 com.alibaba.dubbo.trace.filter.support.TacerFactory 接口

2、创建 META-IN/services/com.alibaba.dubbo.trace.filter.support.TacerFactory
文件，内容为你的实现类

具体示例参照 BraveTracerFactory 类