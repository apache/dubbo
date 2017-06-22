## 支持预热的Hash负载均衡策略

### 用户手册

该Feathure在是在Hash负载均衡策略的基础上增加了权重与预热的特性。

使用方式很简单，只需要配置即可，很明显的负载均衡策略是在消费者一端生效，所以在消费者一端配置是能生效的。但是在Dubbo的配置体系中消费者一端的配置是会继承提供者的一些配置的。这里的“loadbalance”就会有继承效应。所以在提供者一端配置同样生效。需要明确一点的是如果配置在提供者一端对应的所有消费者都会继承配置。

还有一点需要明确的是既然两端都生效就有一个优先级的问题，Dubbo中的约定是消费者一端的配置优先级要高于提供者一端的配置。以下是在spring配置文件中的配置示例：

```
#配置在消费者端
<dubbo:reference id="demoService" loadbalance="weightedconsistenthash" interface="com.alibaba.dubbo.demo.DemoService" />

#配置在提供者端
<dubbo:service  ref="demoService" loadbalance="weightedconsistenthash" interface="com.alibaba.dubbo.demo.DemoService" />
```

同时还有另外一种配置方式：

```
#配置在消费者端
<dubbo:consumer loadbalance="weightedconsistenthash" />

#配置在提供者端
<dubbo:provider loadbalance="weightedconsistenthash" />
```

Dubbo的配置体系中dubbo:reference与dubbo:service代表的是一个引用和一个服务。而dubbo:consumer标签与dubbo:provider代表的是所有的引用与服务，这两个标签中的配置是公共配置，对所有的应用与服务都生效。这里的优先级顺序是dubbo:reference比dubbo:service优先。

在Dubbo的配置体系中的通用的原则是：

```
dubbo:reference > dubbo:service > dubbo:consumer > dubbo:provider
```

这里其实会碰到一个有问题的场景，如果服务提供端有多个，并且每个提供者配置了不同的参数，在消费者一方没有配置相应的参数，那么到底哪一个提供者的参数会生效呢，这个是不确定的。**所以强烈建议在消费者一端生效的参数尽量不要配置在提供者端。**

另外请注意，对于哈希方式的负载均衡策略，默认情况下，是根据方法的第一个参数作为哈希的基础进行分片的，如果使用其它参数可以进行配置，以下是一个例子，表示取前三个参数作为哈希的基础：

```
<dubbo:reference id="demoService" interface="com.alibaba.dubbo.demo.DemoService">
    <dubbo:method name="hello">
        <dubbo:parameter key="hash.arguments" value="0,1,2"/>
    </dubbo:method >
</dubbo:reference>
```


### 实现方式

该Feature的实现类是WeightedConsistentHashLoadBalance，这是一个基于一致性哈希实现的负载均衡策略，一致性哈希对比与简单哈希的好处是当增加或者减少服务提供者时，只会破坏一部分请求与服务提供者的映射关系，而简单哈希方式则会完全破坏之前的映射关系，所以选择了一致性哈希。

对于权重特性的支持的实现思路是，构建一致性哈希环的时候，根据每个提供者的权重的大小，计算出相应的虚拟节点的数量，然后构建哈希环。

![weight and virtual-node](http://image.cnthrowable.com/upload/throwable_blog/itbroblog/blog/1499047504152_217.png)

对于服务预热特性的支持的实现思路是，在预热期间（默认是服务启动的10分钟）内，每分钟计算一次该服务的的动态权重，然后根据动态权重比例设置虚拟节点的数量

```
dynamicWeight=uptime/warmupTime*configuredWeight
uptime : 服务启动的时间
warmupTime : 预热期间，默认10分钟
configuredWeight : 服务配置的权重，默认100
```

接下来分析预热功能对哈希Sticky特性的破坏。因为只有增加节点的场景服务预热功能才会生效，所以我们看增加节点的场景：

![预热功能对Sticky特性的影响](http://image.cnthrowable.com/upload/throwable_blog/itbroblog/blog/1499065245188_649.png)

通过观察以上的流程可以看到对于老的节点，在哈希环上对应的虚拟节点的数量和哈希值是不变的，整个预热期间变化的只要新增加服务的虚拟节点的数量。

有没有预热功能对于哈希环的影响只是：一次性添加所有虚拟节点与缓慢添加虚拟节点的区别，但是两者最终哈希环的形态是一样的。所以我们认为服务预热功能是不会破坏Sticky特性的。
